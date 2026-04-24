$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Starting Android Telegram agent from $scriptRoot"
python -m android_agent_bot.main
