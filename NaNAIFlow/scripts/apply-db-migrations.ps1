param(
  [Parameter(Mandatory = $false)]
  [string]$DatabaseUrl = "",

  [Parameter(Mandatory = $false)]
  [string]$SchemaPath = "D:\Code\NaNAIFlow\db\schema.sql"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path $SchemaPath)) {
  throw "Schema file not found: $SchemaPath"
}

$dbUrl = $DatabaseUrl
if ([string]::IsNullOrWhiteSpace($dbUrl)) {
  $dbUrl = $env:BOT_DB_CONNECTION
}

if ([string]::IsNullOrWhiteSpace($dbUrl)) {
  throw "Database URL missing. Pass -DatabaseUrl or set BOT_DB_CONNECTION."
}

$psql = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psql) {
  throw "psql not found in PATH. Install PostgreSQL client tools."
}

& psql "$dbUrl" -v ON_ERROR_STOP=1 -f "$SchemaPath"
if ($LASTEXITCODE -ne 0) {
  throw "Failed to apply schema migrations"
}

Write-Output "Database migration applied successfully from $SchemaPath"
