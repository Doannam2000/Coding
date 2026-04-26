# AGENTS.md - BotDiscordAndTelegram

## Project Overview

Multi-platform terminal bot that provides remote terminal access via Discord and Telegram. Allows executing commands, chatting with AI, and managing terminal sessions.

## Maintenance Rule

- Every important change (add/remove feature, behavior change, architecture update, critical bug fix) must be documented in this file.
- Update this file in the same change set whenever possible.

## Architecture

### Entry Points
- `src/index.ts` - Discord bot only
- `src/combined.ts` - Discord + Telegram combined bot (main entry)

### Services (`src/services/`)
| Service | Purpose |
|---------|---------|
| `LoggerService` | Logging with file output |
| `SecurityService` | Authorization, command validation, cooldown |
| `TerminalService` | Execute shell commands, manage sessions |
| `AIService` | Connect to OpenCode server for AI chat |
| `MemoryService` | Track projects, commands history, bot stats |
| `ProcessQueueService` | Queue processing |

### Commands (`src/commands/`)
| Command | Description |
|---------|-------------|
| `/run` | Execute terminal command |
| `/chat` | Chat with AI (with context) |
| `/ai` | Single AI query |
| `/project` | List/switch projects |
| `/memory` | View bot memory/stats |
| `/status` | Check session/process/system |
| `/stop` | Stop running process |
| `/cd` | Change working directory |
| `/history` | View command history |
| `/logs` | View command logs |
| `/sessions` | List terminal sessions |
| `/help` | Help message |
| `/ping` | Check latency |

## Memory System

### Project Detection
Automatically detects project type from directory:
- `android` - Contains `build.gradle` or `build.gradle.kts`
- `node` - Contains `package.json`
- `python` - Contains `requirements.txt`, `pyproject.toml`, `setup.py`

### Android Projects
Extracts `applicationId`/`namespace` from `build.gradle`.

### Tracked Data
- All executed commands with exit code, duration, cwd
- Project paths, names, types
- Bot statistics (total commands, uptime)

### Files
- `memory.json` - Persistent storage in project root
- `runtime-state.json` - Unified runtime state (terminal sessions, combined bot state, write broker state)

### Runtime State Notes
- Combined runtime state now persists selected AI session per Telegram chat (`selectedAISessionsByChat`) with `chatId`, `cli`, `workdir`, `sessionId`.
- This keeps `/chat` and `/ai` using the previously selected OpenCode session after restart/retry scenarios (including Telegram 429 incidents), preventing context loss.

### Project Documentation Automation
- When a project is tracked/added/updated path via `MemoryService`, the bot now auto-ensures two docs exist in that project root:
  - `agent.md` (copied from workspace root template if available, otherwise fallback template)
  - `project.md` (core feature/architecture note template)
- This enforces documentation bootstrapping for new projects and backfills missing docs in existing tracked projects.
- Project doc/context scanning now only reads a small, prioritized set of top-level markdown files (`AGENTS.md`/`agent.md`, `README.md`, `project.md`, then others) and caps bytes read per file.
- This prevents `/project`, project switching, and AI context loading from hanging too long on projects with very large markdown files.

## Telegram Commands

| Command | Usage |
|---------|-------|
| `/run <cmd>` | Execute command |
| `/project [name]` | List/select project |
| `/project <name> --path <path>` | Update project path |
| `/memory [stats|projects|commands]` | View memory |
| `/memory clear` | Clear history |

## Discord Commands

Same as Telegram but with slash commands:
- `/project` - Project selection
- `/memory` - View stats

## Flow

1. User sends command (Discord/Telegram)
2. SecurityService validates authorization + command
3. TerminalService executes command
4. MemoryService tracks command + updates project
5. Output sent back to user

## Environment Variables

Required in `.env`:
```
DISCORD_TOKEN=...
DISCORD_CLIENT_ID=...
DISCORD_GUILD_ID=...
TELEGRAM_TOKEN=...
```

## Git Automation Notes

- `GitService.commit()` now executes `git commit -m <message>` via argument array spawn (no shell interpolation).
- This prevents `/pushgit` commit messages containing separators like `|`, `:` and spaces from being split into invalid pathspec arguments on Windows shells.
