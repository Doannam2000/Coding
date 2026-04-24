# n8n OpenCode 24/7 Bot

This repository contains a production-ready template for running an autonomous coding bot:

- Telegram as the control plane (single allowed user ID)
- n8n as orchestrator (gateway, scheduler, worker, notifier)
- OpenCode CLI as coding engine
- Postgres as queue/state machine
- GitHub PR flow with test/build gates

It also now includes a lightweight direct-run path for Android app generation:

- Telegram polling bot in Python
- SQLite job/state store
- staged Android agent flow: idea, plan, design, code, verify, review, iterate
- deterministic Android Compose scaffolding before coding
- Kotlin Android output aimed at Jetpack Compose projects

Architecture model used here: `Telegram -> n8n -> OpenCode CLI`.

## Quick Start

1. Create and configure a Telegram bot with BotFather.
2. Import workflows from `n8n/workflows/*.json` into n8n.
3. Create the database schema from `db/schema.sql`.
4. Set environment variables from `.env.example`.
5. Put your product/design intent in `docs/ai-spec.md` (or another markdown file).
6. Run task commands from Telegram.

## Included Assets

- `db/schema.sql`: Postgres queue/state schema
- `docs/ai-spec.md`: markdown spec for UI/product intent
- `docs/architecture.md`: why hybrid orchestration is recommended
- `docs/auto-code-bot.md`: architecture and state machine
- `docs/android-telegram-agent.md`: lightweight direct Telegram bot for Android Kotlin app generation
- `docs/setup.md`: install and import checklist
- `docs/windows-deploy.md`: Windows-specific deployment playbook
- `scripts/plan-task.ps1`: planner wrapper for OpenCode CLI
- `scripts/run-task.ps1`: step executor with test/build/commit/PR flow
- `scripts/install-n8n-service.ps1`: one-command Windows service installer
- `scripts/apply-db-migrations.ps1`: one-command schema apply/update
- `scripts/autoapp-bootstrap.ps1`: generates autoapp idea/design package and execution prompt
- `n8n/workflows/*.json`: importable n8n workflows

Hardening included: stale-step recovery workflow, risk gate for sensitive file paths, richer Telegram status payload.

## Telegram Commands

- `/task <repo>|<title>|<brief>`
- `/schedule <taskId>|<cron>`
- `/status <taskId>`
- `/pause <taskId>`
- `/resume <taskId>`
- `/cancel <taskId>`
- `/approve <taskId>`
- `/config <repo>|<mdPath>`
- `/autoapp <repo>|<idea>|<target users>|<constraints>`

Full behavior and SQL details are documented in `docs/auto-code-bot.md`.

Setup instructions are in `docs/setup.md`.

Architecture decision details are in `docs/architecture.md`.

Windows deployment steps are in `docs/windows-deploy.md`.

## Direct Android Agent

Run the direct Telegram bot with:

```text
python -m android_agent_bot.main
```

Setup details and commands are documented in `docs/android-telegram-agent.md`.
