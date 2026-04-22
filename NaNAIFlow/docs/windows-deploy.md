# Windows Deployment Guide

## Target

Run the full stack on Windows:

- n8n always-on
- Postgres always-on
- OpenCode CLI callable from n8n `Execute Command`
- Telegram control + step notifications

## 1) Install Dependencies

- Install Node.js LTS
- Install Git
- Install PostgreSQL
- Install n8n globally:

```powershell
npm install -g n8n
```

- Install OpenCode CLI and verify:

```powershell
opencode --help
```

- Optional (for PR automation):

```powershell
winget install GitHub.cli
gh auth login
```

## 2) Create Runtime Folder

```powershell
New-Item -ItemType Directory -Path "C:\n8n-bot" -Force
```

Copy these files there (or keep repo path and adjust env):

- `db/schema.sql`
- `n8n/workflows/*.json`
- `scripts/plan-task.ps1`
- `scripts/run-task.ps1`
- `scripts/install-n8n-service.ps1`

## 3) Set Environment Variables (Machine Scope)

Run in elevated PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("ALLOWED_TELEGRAM_ID", "123456789", "Machine")
[Environment]::SetEnvironmentVariable("ALLOWED_TELEGRAM_CHAT_ID", "123456789", "Machine")
[Environment]::SetEnvironmentVariable("BOT_DB_CONNECTION", "postgres://postgres:postgres@localhost:5432/opencode_bot", "Machine")
[Environment]::SetEnvironmentVariable("DEFAULT_REPO_ROOT", "D:\Code", "Machine")
[Environment]::SetEnvironmentVariable("DEFAULT_TEST_COMMAND", "npm test", "Machine")
[Environment]::SetEnvironmentVariable("DEFAULT_BUILD_COMMAND", "npm run build", "Machine")
[Environment]::SetEnvironmentVariable("OPEN_CODE_BINARY", "opencode", "Machine")
[Environment]::SetEnvironmentVariable("GITHUB_BASE_BRANCH", "main", "Machine")
[Environment]::SetEnvironmentVariable("RISK_DENY_PATH_REGEX", "^\.env($|\.)|(^|/)\.github/workflows/|(^|/)infra/|(^|/)terraform/|(^|/)k8s/|(^|/)auth/", "Machine")
```

For n8n itself:

```powershell
[Environment]::SetEnvironmentVariable("N8N_HOST", "127.0.0.1", "Machine")
[Environment]::SetEnvironmentVariable("N8N_PORT", "5678", "Machine")
[Environment]::SetEnvironmentVariable("N8N_PROTOCOL", "http", "Machine")
[Environment]::SetEnvironmentVariable("N8N_ENCRYPTION_KEY", "replace_with_long_random_key", "Machine")
```

Restart terminal/session after setting machine env vars.

## 4) Initialize Database

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Code\NaNAIFlow\scripts\apply-db-migrations.ps1"
```

Or explicit connection string:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Code\NaNAIFlow\scripts\apply-db-migrations.ps1" -DatabaseUrl "postgres://postgres:postgres@localhost:5432/opencode_bot"
```

## 5) Run n8n Once (First Boot)

```powershell
n8n
```

Open `http://127.0.0.1:5678`, create owner account, then import workflows from `n8n/workflows/`.

Import all workflows including recovery:

- `telegram-gateway.json`
- `scheduler.json`
- `worker.json`
- `notifier.json`
- `recovery.json`

Configure credentials in n8n UI:

- Telegram credential
- Postgres credential

## 6) Fix Script Paths in Workflows

This template already hardcodes:

- `D:\Code\NaNAIFlow\scripts\plan-task.ps1`
- `D:\Code\NaNAIFlow\scripts\run-task.ps1`

If your repo path is different, update the Execute Command node strings in:

- `n8n/workflows/scheduler.json`
- `n8n/workflows/worker.json`

## 7) Run n8n as Windows Service

Use NSSM:

```powershell
winget install NSSM.NSSM
powershell -ExecutionPolicy Bypass -File "D:\Code\NaNAIFlow\scripts\install-n8n-service.ps1" -ServiceName "n8n-opencode-bot" -AppDirectory "D:\Code\NaNAIFlow" -StartAfterInstall
```

If service already exists and you want to replace it:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Code\NaNAIFlow\scripts\install-n8n-service.ps1" -ServiceName "n8n-opencode-bot" -AppDirectory "D:\Code\NaNAIFlow" -Reinstall -StartAfterInstall
```

## 8) Telegram Smoke Test

From your allowed account:

```text
/task NaNAIFlow|Windows smoke test|Create tiny docs update and run test/build
/status 1
/autoapp NaNAIFlow|Build a school management SaaS|School admins and teachers|Use React + Node, responsive UI, MVP in 1 week
```

## 9) Windows-Specific Notes

- Use escaped backslashes in command paths (`D:\\Code\\...`) where needed.
- Keep PowerShell execution policy compatible with script execution.
- Ensure n8n service account has access to repos, git credentials, and PATH containing `opencode`, `git`, `node`, `npm`, `gh`.
- If `gh` cannot authenticate in service context, create PR in CI instead of local host.
- Recovery workflow should be active in production to auto-heal timed-out steps.
