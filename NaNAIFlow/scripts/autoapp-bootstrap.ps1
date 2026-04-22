param(
  [Parameter(Mandatory = $true)]
  [string]$Repo,

  [Parameter(Mandatory = $true)]
  [string]$Idea,

  [Parameter(Mandatory = $false)]
  [string]$TargetUsers = "General users",

  [Parameter(Mandatory = $false)]
  [string]$Constraints = "",

  [Parameter(Mandatory = $false)]
  [string]$RepoRoot = "D:\Code"
)

$ErrorActionPreference = "Stop"

function Out-Result {
  param(
    [string]$Status,
    [string]$Message,
    [string]$Repo,
    [string]$Title = "",
    [string]$Prompt = "",
    [string]$ConfigMdPath = ""
  )

  return [PSCustomObject]@{
    status = $Status
    message = $Message
    repo = $Repo
    title = $Title
    prompt = $Prompt
    config_md_path = $ConfigMdPath
  } | ConvertTo-Json -Compress
}

try {
  $repoPath = Join-Path $RepoRoot $Repo
  if (-not (Test-Path -Path $repoPath)) {
    throw "Target repo not found: $repoPath"
  }

  $openCodeBinary = if ($env:OPEN_CODE_BINARY) { $env:OPEN_CODE_BINARY } else { "opencode" }
  if (-not (Get-Command $openCodeBinary -ErrorAction SilentlyContinue)) {
    throw "OpenCode binary not found in PATH: $openCodeBinary"
  }

  $docsDir = Join-Path $repoPath "docs"
  if (-not (Test-Path -Path $docsDir)) {
    New-Item -Path $docsDir -ItemType Directory -Force | Out-Null
  }

  $autoDir = Join-Path $docsDir "autoapp"
  if (-not (Test-Path -Path $autoDir)) {
    New-Item -Path $autoDir -ItemType Directory -Force | Out-Null
  }

  $timeTag = Get-Date -Format "yyyyMMdd-HHmmss"
  $specRelPath = "docs/autoapp/$timeTag-autoapp-spec.md"
  $specFullPath = Join-Path $repoPath $specRelPath

  $prompt = @"
You are generating a complete bootstrap package for autonomous app development.

Output strict JSON only with this shape:
{
  "title": "...",
  "execution_prompt": "...",
  "design_markdown": "..."
}

Input:
- Idea: $Idea
- Target users: $TargetUsers
- Constraints: $Constraints

Requirements:
- Create an MVP scope that can be built step-by-step.
- design_markdown must include:
  - Product intent
  - User personas
  - Core features (MVP)
  - UI direction (typography, colors, motion, layout)
  - Screen list and component map
  - Technical constraints
  - Definition of done
- execution_prompt must instruct an implementation agent to:
  - build from this design
  - create tests
  - keep commits incremental
  - prioritize responsiveness and accessibility
"@

  $promptFile = Join-Path $env:TEMP "opencode-autoapp-$timeTag.txt"
  Set-Content -Path $promptFile -Value $prompt -Encoding UTF8

  $raw = & $openCodeBinary --non-interactive --prompt-file "$promptFile"
  if ($LASTEXITCODE -ne 0) {
    throw "OpenCode autoapp bootstrap failed"
  }

  $jsonText = ($raw | Out-String).Trim()
  $parsed = $jsonText | ConvertFrom-Json

  if (-not $parsed.title -or -not $parsed.execution_prompt -or -not $parsed.design_markdown) {
    throw "Autoapp payload missing title/execution_prompt/design_markdown"
  }

  Set-Content -Path $specFullPath -Value $parsed.design_markdown -Encoding UTF8

  Write-Output (Out-Result -Status "success" -Message "Autoapp bootstrap generated" -Repo $Repo -Title $parsed.title -Prompt $parsed.execution_prompt -ConfigMdPath $specRelPath)
  exit 0
}
catch {
  $msg = $_.Exception.Message.Replace('"', "'")
  Write-Output (Out-Result -Status "failed" -Message $msg -Repo $Repo)
  exit 1
}
