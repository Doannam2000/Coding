param(
  [Parameter(Mandatory = $true)]
  [string]$TaskId,

  [Parameter(Mandatory = $true)]
  [string]$RepoPath,

  [Parameter(Mandatory = $true)]
  [string]$TaskTitle,

  [Parameter(Mandatory = $true)]
  [string]$TaskPrompt,

  [Parameter(Mandatory = $false)]
  [string]$ConfigPath = "docs/ai-spec.md",

  [Parameter(Mandatory = $false)]
  [string]$DefaultTestCommand = "npm test",

  [Parameter(Mandatory = $false)]
  [string]$DefaultBuildCommand = "npm run build"
)

$ErrorActionPreference = "Stop"

function Out-Json {
  param(
    [string]$Status,
    [string]$Message,
    [string]$Steps = "[]"
  )

  return [PSCustomObject]@{
    status = $Status
    message = $Message
    steps = if ([string]::IsNullOrWhiteSpace($Steps)) { @() } else { $Steps | ConvertFrom-Json }
  } | ConvertTo-Json -Compress
}

try {
  if (-not (Test-Path -Path $RepoPath)) {
    throw "RepoPath not found: $RepoPath"
  }

  $openCodeBinary = if ($env:OPEN_CODE_BINARY) { $env:OPEN_CODE_BINARY } else { "opencode" }
  if (-not (Get-Command $openCodeBinary -ErrorAction SilentlyContinue)) {
    throw "OpenCode binary not found in PATH: $openCodeBinary"
  }

  $specText = ""
  $specFullPath = Join-Path $RepoPath $ConfigPath
  if (Test-Path -Path $specFullPath) {
    $specText = Get-Content -Path $specFullPath -Raw
  }

  $plannerPrompt = @"
You are a task planner for autonomous coding.
Create a step-by-step plan with 2-8 steps.
Output strict JSON only, with this shape:

{
  "steps": [
    {
      "step_no": 1,
      "title": "...",
      "instruction": "...",
      "test_command": "$DefaultTestCommand",
      "build_command": "$DefaultBuildCommand"
    }
  ]
}

Task title: $TaskTitle
Task prompt: $TaskPrompt

Project specification markdown:
$specText

Rules:
- Make each step independently testable.
- Keep steps ordered and incremental.
- Favor practical implementation order.
- No markdown fences in output.
"@

  $tempPlannerFile = Join-Path $env:TEMP "opencode-plan-task-$TaskId.txt"
  Set-Content -Path $tempPlannerFile -Value $plannerPrompt -Encoding UTF8

  $raw = & $openCodeBinary --non-interactive --prompt-file "$tempPlannerFile"
  if ($LASTEXITCODE -ne 0) {
    throw "OpenCode planning command failed"
  }

  $jsonText = ($raw | Out-String).Trim()
  $parsed = $jsonText | ConvertFrom-Json
  if (-not $parsed.steps -or $parsed.steps.Count -lt 1) {
    throw "Planner returned no steps"
  }

  foreach ($s in $parsed.steps) {
    if (-not $s.test_command) {
      $s | Add-Member -NotePropertyName test_command -NotePropertyValue $DefaultTestCommand
    }
    if (-not $s.build_command) {
      $s | Add-Member -NotePropertyName build_command -NotePropertyValue $DefaultBuildCommand
    }
  }

  $stepsJson = ($parsed.steps | ConvertTo-Json -Compress)
  Write-Output (Out-Json -Status "success" -Message "Planner created steps" -Steps $stepsJson)
  exit 0
}
catch {
  $msg = $_.Exception.Message.Replace('"', "'")
  Write-Output (Out-Json -Status "failed" -Message $msg)
  exit 1
}
