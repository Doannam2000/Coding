from __future__ import annotations

import time

from .config import load_settings
from .db import BotDatabase
from .runner import CommandRouter, JobRunner
from .telegram_api import TelegramClient, TelegramNetworkError


def main() -> None:
    settings = load_settings()
    settings.projects_root.mkdir(parents=True, exist_ok=True)
    db = BotDatabase(settings.database_path)
    telegram = TelegramClient(settings.telegram_bot_token)
    telegram.set_commands(
        [
            {"command": "start", "description": "Open bot menu"},
            {"command": "help", "description": "Show usage and templates"},
            {"command": "buildapp", "description": "Build app from brief"},
            {"command": "newandroid", "description": "Create basic Android job"},
            {"command": "jobs", "description": "List recent jobs and states"},
            {"command": "status", "description": "Check job status"},
            {"command": "progress", "description": "Show live job progress"},
            {"command": "tail", "description": "Read detailed progress tail"},
            {"command": "logs", "description": "Read recent job logs"},
            {"command": "models", "description": "List OpenCode models with picker"},
            {"command": "model", "description": "Set or view selected OpenCode model"},
            {"command": "cli", "description": "Switch agent CLI between opencode and codex"},
            {"command": "setrepo", "description": "Link remote repository URL for a job"},
            {"command": "pushgit", "description": "Commit and push local changes for a job"},
            {"command": "review", "description": "Run manual review on a job"},
            {"command": "runandroid", "description": "Install and launch Android app for a job"},
            {"command": "syncandroid", "description": "Run Gradle sync-style check for a job"},
            {"command": "currentlog", "description": "Show CLI log mode and enable live CLI logs"},
            {"command": "endlog", "description": "Disable live CLI logs"},
            {"command": "pause", "description": "Pause a running or queued job"},
            {"command": "resume", "description": "Resume a paused job"},
            {"command": "addfeature", "description": "Add extra feature request to a job"},
            {"command": "addtask", "description": "Queue a follow-up task for a job"},
            {"command": "tasks", "description": "Show active/pending tasks for a job"},
            {"command": "fixbug", "description": "Queue a follow-up bug-fix task"},
            {"command": "approve", "description": "Approve current stage"},
            {"command": "reject", "description": "Reject stage with feedback"},
            {"command": "cancel", "description": "Cancel a job"},
        ]
    )
    worker = JobRunner(settings, db, telegram)
    router = CommandRouter(settings, db, telegram, worker)

    for existing_job in db.list_jobs_for_workspace_docs():
        try:
            worker._ensure_workspace_docs(existing_job, existing_job.get("context", {}))
        except Exception as exc:  # noqa: BLE001
            print(f"workspace docs sync warning for job #{existing_job.get('id')}: {exc}")

    worker.start()

    offset: int | None = None
    network_failures = 0
    while True:
        try:
            updates = telegram.get_updates(offset=offset, timeout=settings.poll_timeout_seconds)
            network_failures = 0
            for update in updates:
                offset = int(update["update_id"]) + 1
                router.handle_update(update)
        except KeyboardInterrupt:
            worker.stop()
            return
        except TelegramNetworkError as exc:
            network_failures += 1
            if network_failures in {1, 5} or network_failures % 20 == 0:
                print(f"telegram polling warning ({network_failures}): {exc}")
            time.sleep(1)
        except Exception as exc:  # noqa: BLE001
            time.sleep(3)
            print(f"telegram polling error: {exc}")


if __name__ == "__main__":
    main()
