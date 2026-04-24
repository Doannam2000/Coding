# Architecture Decision

## Recommended Model

Use a hybrid model:

- Telegram bot is the control plane
- n8n is the orchestrator and state manager
- OpenCode CLI is the execution engine

Flow:

```text
Telegram -> n8n gateway -> Postgres queue/state -> n8n worker -> OpenCode CLI -> tests/build -> GitHub PR -> Telegram notify
```

## Why This Is Better Than Telegram -> CLI Directly

### Direct Telegram -> CLI

Pros:

- Very fast to build
- Fewer moving parts
- Good for solo experiments

Cons:

- No durable queue by default
- Hard to retry safely
- Hard to schedule recurring work
- Weak visibility into step-by-step progress
- Harder to recover from crashes or host restarts
- Approval gates and notifications become custom code

### Telegram -> n8n -> CLI

Pros:

- Durable task queue with status history
- Easy cron and recurring schedules
- Step-by-step workflow orchestration
- Built-in integration with Telegram, Postgres, GitHub, Slack
- Easier retries, timeouts, and stale task recovery
- Cleaner separation of concerns

Cons:

- More setup work
- More components to maintain

## Recommended Responsibility Split

### Telegram

- Accept commands from exactly one allowed user ID
- Show status, failures, approvals, and PR links

### n8n

- Authorize Telegram commands
- Persist tasks and steps
- Trigger planner and worker scripts
- Schedule recurring work
- Route notifications
- Enforce workflow state transitions

### OpenCode CLI

- Read step instruction and markdown spec
- Modify code in the target repo
- Respect repo conventions
- Let shell scripts run test/build/commit/push/PR around it

## Production Recommendation

For your use case (24/7 autonomous coding with planning, test gates, UI generation from markdown, and Telegram reports), keep n8n in the loop.

Best practical deployment:

- One VPS or Windows server
- n8n service always on
- Postgres always on
- Repos cloned locally under `DEFAULT_REPO_ROOT`
- OpenCode CLI and GitHub CLI installed on the same host as n8n worker execution

## Minimal Lifecycle

1. Send `/task` from Telegram
2. n8n stores task
3. Planner creates sequential steps
4. Worker runs one step with OpenCode CLI
5. Tests/build validate output
6. n8n marks step state and sends Telegram update
7. Final completion creates or updates PR

## When You Can Skip n8n

You can skip n8n only if all of these are true:

- single repo
- low task volume
- no recurring schedules
- no durable audit/history needed
- okay with manual restart/recovery

That is not your current goal, so hybrid is the right call.
