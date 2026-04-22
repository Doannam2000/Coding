# Setup Guide

## 1) Prerequisites

- n8n instance (desktop, docker, or server)
- Postgres database
- Git, Node.js, and test/build tooling per target repo
- OpenCode CLI available in PATH (`opencode`)
- Optional: GitHub CLI (`gh`) for PR creation

## 2) Environment Variables

Copy `.env.example` values into your n8n environment and adjust:

- `ALLOWED_TELEGRAM_ID`: the only user that can run commands
- `ALLOWED_TELEGRAM_CHAT_ID`: optional strict chat lock
- `DEFAULT_REPO_ROOT`: parent folder containing project repos
- `OPEN_CODE_BINARY`: CLI name/path if not `opencode`

## 3) Database

Run schema:

```sql
\i db/schema.sql
```

## 4) Import Workflows in n8n

Import the following files:

- `n8n/workflows/telegram-gateway.json`
- `n8n/workflows/scheduler.json`
- `n8n/workflows/worker.json`
- `n8n/workflows/notifier.json`

Then set credential IDs in each workflow:

- `POSTGRES_CREDENTIAL_ID`
- `TELEGRAM_CREDENTIAL_ID`

This setup intentionally keeps n8n as the orchestrator and OpenCode CLI as the executor.
If you remove n8n, you lose queue durability, schedule orchestration, and workflow-level retries.

## 5) Script Paths

The example workflows call scripts from this template repo:

- `scripts/plan-task.ps1`
- `scripts/run-task.ps1`

If your n8n host path differs, update command paths in `Execute Command` nodes.

## 6) Risk and Production Guardrails

- Enable protected branch (`main`) with required checks
- Keep worker concurrency at 1 for deterministic behavior
- Never store secrets in task prompts
- Add path deny-list in your worker script if needed
- Use `/approve <taskId>` before sensitive changes

## 7) Telegram Usage

Examples:

```text
/task NaNAIFlow|Build responsive landing page|Create hero, feature grid, pricing and CTA from docs/ai-spec.md
/status 12
/pause 12
/resume 12
/schedule 12|*/30 * * * *
/config NaNAIFlow|docs/ai-spec.md
```

## 8) Expected Lifecycle

1. `/task` creates queue item
2. Scheduler calls planner and generates step list
3. Worker runs one step, tests, build, commit
4. Notify after each step
5. Final step marks task completed and shares PR URL
