# Auto Code Bot Blueprint (n8n + Telegram + OpenCode CLI)

## Goal

Run a 24/7 coding loop that can:

- Accept tasks from Telegram
- Plan and split tasks into sequential steps
- Call OpenCode CLI to implement each step
- Run tests/build gates automatically
- Create PRs and notify step-by-step status on Telegram

## Security Model

- Only one Telegram account can control the bot (`ALLOWED_TELEGRAM_ID`)
- Optionally enforce one chat (`ALLOWED_TELEGRAM_CHAT_ID`)
- Bot can create branches and PRs, never direct push to protected `main`
- Branch protection and required checks must be enabled in GitHub
- Add a risk gate for sensitive paths (`.env`, infra, auth core)

## Command Protocol

Command payload uses `|` delimiters.

- `/task <repo>|<title>|<brief>`
  - Creates task in `queued`, then planner generates steps
- `/schedule <taskId>|<cron>`
  - Sets recurring run cadence for an existing task
- `/status <taskId>`
  - Returns task status, current step, retries, PR link
- `/pause <taskId>`
  - Sets task status to `paused`
- `/resume <taskId>`
  - Returns paused task to `ready`
- `/cancel <taskId>`
  - Sets task to `cancelled`
- `/approve <taskId>`
  - Unblocks tasks waiting for human approval
- `/config <repo>|<mdPath>`
  - Overrides markdown spec path per repo
- `/autoapp <repo>|<idea>|<target users>|<constraints>`
  - Auto-generates product/design spec markdown, creates an execution prompt, and queues a build task

## State Machine

Task status:

- `queued`
- `planning`
- `ready`
- `running_step`
- `blocked_needs_approval`
- `failed_retryable`
- `failed_terminal`
- `paused`
- `cancelled`
- `completed`

Step status:

- `pending`
- `running`
- `passed`
- `failed`
- `skipped`

## Planner Contract

Planner receives:

- task title and prompt
- repo name
- markdown config (`docs/ai-spec.md` by default)

Planner returns strict JSON:

```json
{
  "steps": [
    {
      "step_no": 1,
      "title": "Create page shell",
      "instruction": "Implement route and base layout",
      "test_command": "npm test",
      "build_command": "npm run build"
    }
  ]
}
```

## Worker Loop

1. Pull a single `ready` task (FIFO)
2. Lock task row and mark `running_step`
3. Fetch first `pending` step
4. Call `scripts/run-task.ps1`
5. If pass:
   - mark step `passed`
   - notify Telegram
   - continue with next pending step
6. If fail and retries available:
   - increment retries
   - set step `pending`
   - task `failed_retryable`
7. If fail terminal:
   - set task `failed_terminal`
   - notify Telegram with short error digest
8. When all steps passed:
   - create PR (or keep draft)
   - set task `completed`
   - notify Telegram with PR URL

Note: current template runs worker with `-CreatePr`, so PR is updated continuously as steps are committed.

## n8n Workflow Set

- `telegram-gateway.json`
  - Parses commands, authorizes user, writes DB
- `scheduler.json`
  - Cron poller moves due work to `ready`
- `worker.json`
  - Claims and executes one step at a time
- `notifier.json`
  - Reusable Telegram formatting and delivery
- `recovery.json`
  - Requeues stale running steps after timeout and alerts Telegram

## Suggested Telegram Notifications

- Task accepted: `Task #ID queued`
- Step start: `Task #ID Step N started`
- Step pass: `Task #ID Step N passed`
- Step fail: `Task #ID Step N failed (retry X/Y)`
- Completed: `Task #ID completed. PR: <url>`
- Include elapsed time, branch, commit hash, changed files, and PR URL when available

## Operational Guardrails

- Worker concurrency = 1
- Step timeout = 30 minutes
- Max retries per step = 2
- Daily token budget enforced by bot setting
- Quiet hours for non-critical alerts
- Recovery cron requeues stale `running_step` tasks after timeout
- Risk gate blocks sensitive path edits before commit (`RISK_DENY_PATH_REGEX`)
