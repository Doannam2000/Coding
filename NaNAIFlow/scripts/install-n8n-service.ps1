param(
  [Parameter(Mandatory = $false)]
  [string]$ServiceName = "n8n-opencode-bot",

  [Parameter(Mandatory = $false)]
  [string]$N8nCommand = "n8n",

  [Parameter(Mandatory = $false)]
  [string]$AppDirectory = "D:\Code\NaNAIFlow",

  [Parameter(Mandatory = $false)]
  [switch]$StartAfterInstall,

  [Parameter(Mandatory = $false)]
  [switch]$Reinstall
)

$ErrorActionPreference = "Stop"

function Resolve-NssmPath {
  $cmd = Get-Command nssm -ErrorAction SilentlyContinue
  if ($cmd) {
    return $cmd.Source
  }

  $commonPaths = @(
    "C:\Program Files\nssm\win64\nssm.exe",
    "C:\Program Files (x86)\nssm\win64\nssm.exe",
    "C:\Windows\System32\nssm.exe"
  )

  foreach ($p in $commonPaths) {
    if (Test-Path $p) { return $p }
  }

  throw "nssm not found. Install with: winget install NSSM.NSSM"
}

function Resolve-N8nExecutable {
  $cmd = Get-Command $N8nCommand -ErrorAction SilentlyContinue
  if ($cmd) {
    return $cmd.Source
  }

  $candidate = Join-Path $env:APPDATA "npm\n8n.cmd"
  if (Test-Path $candidate) {
    return $candidate
  }

  throw "n8n executable not found. Install with: npm install -g n8n"
}

$nssmExe = Resolve-NssmPath
$n8nExe = Resolve-N8nExecutable

if (-not (Test-Path $AppDirectory)) {
  throw "AppDirectory not found: $AppDirectory"
}

$serviceExists = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($serviceExists) {
  if (-not $Reinstall.IsPresent) {
    throw "Service '$ServiceName' already exists. Run with -Reinstall to replace it."
  }

  & $nssmExe stop $ServiceName | Out-Null
  & $nssmExe remove $ServiceName confirm | Out-Null
}

& $nssmExe install $ServiceName $n8nExe | Out-Null
& $nssmExe set $ServiceName AppDirectory $AppDirectory | Out-Null
& $nssmExe set $ServiceName AppParameters "" | Out-Null
& $nssmExe set $ServiceName Start SERVICE_AUTO_START | Out-Null
& $nssmExe set $ServiceName AppStdout "D:\Code\NaNAIFlow\logs\n8n-service-out.log" | Out-Null
& $nssmExe set $ServiceName AppStderr "D:\Code\NaNAIFlow\logs\n8n-service-err.log" | Out-Null
& $nssmExe set $ServiceName AppRotateFiles 1 | Out-Null
& $nssmExe set $ServiceName AppRotateOnline 1 | Out-Null

if (-not (Test-Path "D:\Code\NaNAIFlow\logs")) {
  New-Item -ItemType Directory -Path "D:\Code\NaNAIFlow\logs" -Force | Out-Null
}

if ($StartAfterInstall.IsPresent) {
  & $nssmExe start $ServiceName | Out-Null
}

Write-Output "Installed service: $ServiceName"
Write-Output "n8n executable: $n8nExe"
Write-Output "App directory: $AppDirectory"
if ($StartAfterInstall.IsPresent) {
  Write-Output "Service started."
}
