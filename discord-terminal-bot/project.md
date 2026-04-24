# project.md - Core App Notes

## App Identity
- Multi-platform terminal bot for Discord + Telegram.
- Executes terminal commands remotely and supports AI-assisted chat in project context.

## Core Features
- Secure command execution with authorization, command validation, and cooldown.
- Persistent terminal sessions per chat/channel (cwd + history aware).
- AI chat and single-shot AI query modes (`/chat`, `/ai`) with CLI backends (`opencode`, `claude`, `codex`).
- Project memory and runtime persistence (`memory.json`, `runtime-state.json`).
- Queue and recovery flow for in-flight AI requests to avoid stuck states.
- Session selection for AI conversations, persisted per Telegram chat (`chatId`, `cli`, `workdir`, `sessionId`) to preserve context after restart/retry (including Telegram 429 scenarios).

## Main Command Surface
- Execute and manage terminal: `/run`, `/stop`, `/cd`, `/sessions`, `/status`, `/history`, `/logs`.
- AI and model/session controls: `/chat`, `/ai`, CLI/model selection, session selection.
- Project and memory management: `/project`, `/memory`.

## Service Architecture (Important)
- `SecurityService`: auth + command safety + cooldown.
- `TerminalService`: process/session lifecycle.
- `AIService`: model/CLI routing, streaming, retries, session handling.
- `MemoryService`: command/project/stats snapshots.
- `RuntimeStateService`: unified persisted runtime state.
- `ProcessQueueService`: queued execution flow.

## Reliability Behaviors
- Handles rate-limit/transient AI provider errors with retries.
- Tracks active AI thinking requests and recovers stale request states.
- Uses heartbeat + watchdog signals for runtime health monitoring.
