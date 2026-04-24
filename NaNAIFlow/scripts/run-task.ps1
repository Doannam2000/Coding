param(
  [Parameter(Mandatory = $true)]
  [string]$TaskId,

  [Parameter(Mandatory = $true)]
  [string]$RepoPath,

  [Parameter(Mandatory = $true)]
  [string]$StepNo,

  [Parameter(Mandatory = $true)]
  [string]$StepInstruction,

  [Parameter(Mandatory = $false)]
  [string]$TaskTitle = "",

  [Parameter(Mandatory = $false)]
  [string]$TaskPrompt = "",

  [Parameter(Mandatory = $false)]
  [string]$ConfigPath = "docs/ai-spec.md",

  [Parameter(Mandatory = $false)]
  [string]$BaseBranch = "main",

  [Parameter(Mandatory = $false)]
  [string]$TestCommand = "npm test",

  [Parameter(Mandatory = $false)]
  [string]$BuildCommand = "npm run build",

  [Parameter(Mandatory = $false)]
  [switch]$CreatePr
)

$ErrorActionPreference = "Stop"

function Invoke-StrictCommand {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Command,
    [Parameter(Mandatory = $true)]
    [string]$DisplayName
  )

  Invoke-Expression $Command
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed: $DisplayName"
  }
}

function Get-Output {
  param(
    [string]$TaskId,
    [string]$Status,
    [string]$Message,
    [string]$Branch = "",
    [string]$Commit = "",
    [string]$PrUrl = "",
    [string]$ChangedFiles = ""
  )

  return [PSCustomObject]@{
    taskId = $TaskId
    status = $Status
    message = $Message
    branch = $Branch
    commit = $Commit
    prUrl = $PrUrl
    changedFiles = $ChangedFiles
  } | ConvertTo-Json -Compress
}

function Get-ChangedFiles {
  $lines = git status --porcelain
  $files = @()
  foreach ($line in $lines) {
    if ([string]::IsNullOrWhiteSpace($line)) {
      continue
    }

    if ($line.Length -ge 4) {
      $pathPart = $line.Substring(3).Trim()
      if ($pathPart -match "->") {
        $pathPart = ($pathPart -split "->")[-1].Trim()
      }
      if (-not [string]::IsNullOrWhiteSpace($pathPart)) {
        $files += $pathPart.Replace('\\', '/')
      }
    }
  }
  return $files
}

function Assert-NoRiskyChanges {
  param(
    [string[]]$ChangedFiles
  )

  $defaultPatterns = @(
    '^\.env($|\.)',
    '(^|/)\.github/workflows/',
    '(^|/)infra/',
    '(^|/)terraform/',
    '(^|/)k8s/',
    '(^|/)auth/'
  )

  $raw = $env:RISK_DENY_PATH_REGEX
  $patterns = @()
  if (-not [string]::IsNullOrWhiteSpace($raw)) {
    $patterns = $raw.Split(';') | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
  }
  if ($patterns.Count -eq 0) {
    $patterns = $defaultPatterns
  }

  $risky = @()
  foreach ($file in $ChangedFiles) {
    foreach ($pattern in $patterns) {
      if ([regex]::IsMatch($file, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
        $risky += $file
        break
      }
    }
  }

  if ($risky.Count -gt 0) {
    $joined = ($risky | Select-Object -Unique) -join ", "
    throw "Risk gate blocked sensitive file changes: $joined"
  }
}

try {
  if (-not (Test-Path -Path $RepoPath)) {
    throw "RepoPath not found: $RepoPath"
  }

  Set-Location $RepoPath

  Invoke-StrictCommand -Command "git rev-parse --is-inside-work-tree" -DisplayName "git repo check"

  $branch = "bot/task-$TaskId"
  $openCodeBinary = if ($env:OPEN_CODE_BINARY) { $env:OPEN_CODE_BINARY } else { "opencode" }

  git fetch origin | Out-Null
  git checkout $BaseBranch | Out-Null
  git pull origin $BaseBranch | Out-Null

  $existing = git branch --list $branch
  if ([string]::IsNullOrWhiteSpace($existing)) {
    git checkout -b $branch | Out-Null
  } else {
    git checkout $branch | Out-Null
  }

  $specFullPath = Join-Path $RepoPath $ConfigPath
  $specContent = ""
  if (Test-Path -Path $specFullPath) {
    $specContent = Get-Content -Path $specFullPath -Raw
  }

  $systemPrompt = @"
You are implementing one task step in a repository.
Return only code edits through CLI behavior.

Task title: $TaskTitle
Task prompt: $TaskPrompt
Step number: $StepNo
Step instruction: $StepInstruction

Project specification markdown:
$specContent

Rules:
- Make focused changes only for this step.
- Keep style consistent with repo.
- Avoid editing secrets, CI credentials, or .env files.
"@

  $tempPromptFile = Join-Path $env:TEMP "opencode-task-$TaskId-step-$StepNo.txt"
  Set-Content -Path $tempPromptFile -Value $systemPrompt -Encoding UTF8

  if (Get-Command $openCodeBinary -ErrorAction SilentlyContinue) {
    & $openCodeBinary --non-interactive --prompt-file "$tempPromptFile"
    if ($LASTEXITCODE -ne 0) {
      throw "OpenCode CLI execution failed"
    }
  } else {
    throw "OpenCode binary not found in PATH: $openCodeBinary"
  }

  if (-not [string]::IsNullOrWhiteSpace($TestCommand)) {
    Invoke-StrictCommand -Command $TestCommand -DisplayName "test command"
  }

  if (-not [string]::IsNullOrWhiteSpace($BuildCommand)) {
    Invoke-StrictCommand -Command $BuildCommand -DisplayName "build command"
  }

  $changedFileList = Get-ChangedFiles
  $changedFiles = $changedFileList -join ","

  if ([string]::IsNullOrWhiteSpace($changedFiles)) {
    Write-Output (Get-Output -TaskId $TaskId -Status "success" -Message "No file changes generated" -Branch $branch)
    exit 0
  }

  Assert-NoRiskyChanges -ChangedFiles $changedFileList

  git add .
  git commit -m "bot(task-$TaskId): complete step $StepNo" | Out-Null
  git push -u origin $branch | Out-Null

  $commit = (git rev-parse HEAD).Trim()
  $prUrl = ""

  if ($CreatePr.IsPresent) {
    if (Get-Command gh -ErrorAction SilentlyContinue) {
      $prTitle = "bot: task $TaskId"
      $prBody = "Automated task progress for step $StepNo.`n`nTask: $TaskTitle"
      $existingPr = gh pr list --head $branch --json url --jq ".[] | .url"
      if ([string]::IsNullOrWhiteSpace($existingPr)) {
        $prUrl = gh pr create --title "$prTitle" --body "$prBody" --base $BaseBranch --head $branch
      } else {
        $prUrl = $existingPr
      }
    }
  }

  Write-Output (Get-Output -TaskId $TaskId -Status "success" -Message "Step execution succeeded" -Branch $branch -Commit $commit -PrUrl $prUrl -ChangedFiles $changedFiles)
  exit 0
}
catch {
  $errorText = $_.Exception.Message.Replace('"', "'")
  Write-Output (Get-Output -TaskId $TaskId -Status "failed" -Message $errorText)
  exit 1
}
