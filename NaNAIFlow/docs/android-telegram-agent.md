# Android Telegram Agent

This direct bot path is the lightweight alternative to the existing `n8n` setup.

Flow:

```text
Telegram -> Python polling bot -> SQLite job store -> OpenCode CLI -> Android project workspace
```

## What It Does

- receives Android app requests from Telegram
- runs staged agent work: `idea -> plan -> design -> code -> verify -> review`
- stores state in SQLite
- reports stage-by-stage progress back to Telegram
- uses OpenCode CLI for ideation, planning, design, coding, verification repair, and review-driven iteration loops
- scaffolds a Compose Android project with `android create empty-activity` before coding when needed
- runs Gradle verification commands locally in the generated Android project
- reports the debug APK path when verification succeeds
- initializes a local git repository in each generated Android workspace and commits scaffold, code, and repair changes

## Commands

- `/newandroid <slug>|<idea>|<target users>|<constraints>`
- `/buildapp <slug>|<idea>|<style>|<font>|<features>|<target users>|<constraints>`
- `Build App Template` button to send a ready-to-fill multiline brief
- `Start Build Wizard` button for step-by-step guided input
- `/status <job_id>`
- `/logs <job_id>`
- `/approve <job_id>`
- `/reject <job_id>|<feedback>`
- `/cancel <job_id>`

## Environment Variables

- `TELEGRAM_BOT_TOKEN`
- `ALLOWED_TELEGRAM_ID`
- `ALLOWED_TELEGRAM_CHAT_ID`
- `OPEN_CODE_BINARY`
- `OPEN_CODE_MIN_REQUEST_INTERVAL_SECONDS`
- `ANDROID_CLI_BINARY`
- `ANDROID_PROJECTS_ROOT`
- `ANDROID_AGENT_DB_PATH`
- `ANDROID_AGENT_REQUIRE_STAGE_APPROVAL`
- `ANDROID_AGENT_APPROVAL_STAGES`
- `ANDROID_AGENT_MAX_VERIFY_REPAIRS`
- `ANDROID_AGENT_MAX_REVIEW_LOOPS`
- `ANDROID_AGENT_PACKAGE_PREFIX`
- `ANDROID_AGENT_MIN_SDK`
- `ANDROID_AGENT_VERIFY_COMMANDS`

Defaults assume Windows and Gradle wrapper commands:

```text
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

CLI notes:

- `OPEN_CODE_BINARY` can be `opencode` (default behavior) or `codex`.
- When using `codex`, the bot runs non-interactive `codex exec` with JSON events and automatic session resume/fallback.

## Run

```text
python -m android_agent_bot.main
```

## Fast Brief Command

Use `/buildapp` when you want to give one compact brief and let the bot do the rest.

Format:

```text
/buildapp <slug>|<idea>|<style>|<font>|<features>|<target users>|<constraints>
```

Optional multiline-only field:

```text
app_id: com.example.yourapp
```

Multiline format:

```text
/buildapp
slug: coffee-social
idea: social app for coffee lovers
style: warm modern editorial
font: minimal geometric sans
features:
- post cafe reviews
- map nearby cafes
- save favorites
target_users: young city coffee drinkers
constraints: android only, MVP first, offline-friendly where possible
app_id: com.example.coffeesocial
```

Example:

```text
/buildapp coffee-social|social app for coffee lovers|warm modern editorial|minimal geometric sans|post cafe reviews, map nearby cafes, save favorites|young city coffee drinkers|android only, MVP first, offline-friendly where possible
```

This command packages your idea, visual direction, font direction, and feature priorities into the agent context, then runs:

```text
idea -> plan -> design -> code -> verify -> review -> iterate until complete
```

When the bot starts, it also shows a persistent Telegram keyboard with common actions and a `Build App Template` button.

## Guided Wizard

You can tap `Start Build Wizard` and the bot will ask for these fields one by one:

```text
slug
idea
style
font
features
target_users
constraints
```

After the last answer, the bot automatically creates the job and starts the full loop.

## Inline Job Buttons

After a job is queued, the bot sends inline buttons for quick actions:

```text
Status | Logs | Progress | Pause | Resume | Approve | Reject Help | Cancel
```

Notes:

- `Approve` only works when the current stage is waiting for approval
- `Reject Help` sends the exact `/reject <job_id>|<feedback>` format to use

## Progress And Control

Use these commands while a job is running:

```text
/jobs
/status <job_id>
/progress <job_id>
/tail <job_id>
/logs <job_id>
/pause <job_id>
/resume <job_id>
```

Behavior:

- `/jobs` lists recent jobs, highlights `running` and `paused`, and gives quick inline buttons to inspect or control them
- the bot now sends progress messages during long stages such as `idea`, `plan`, `design`, `code`, `verify`, and `review`
- `/progress` shows current status and the latest live detail if the worker is still active
- `/tail` shows a longer recent event stream than `/logs`
- `/pause` pauses queued jobs immediately and marks running jobs as paused for the next worker cycle boundary
- `/resume` puts a paused job back into `queued` or `waiting_approval`, depending on where it was paused

## Notes

- this is intentionally single-worker and single-user
- it is best suited to Kotlin Android MVPs built with Jetpack Compose
- approval gates are optional and disabled by default
- after verification, a review stage can route the job back to `plan`, `design`, `code`, or `verify` for another loop, or mark it complete
- review iteration count is capped to avoid infinite loops
- the generated Android project is created under `D:\Code\<slug>` unless overridden
- if the workspace is empty, the bot scaffolds an Android project first using the installed `android` CLI template
- the final Kotlin package name is read from the scaffolded Gradle project so later code and repair stages stay aligned with the generated template
- generated Android workspaces use a local git branch named `job-<id>` with incremental commits after scaffold, code, and repair steps
