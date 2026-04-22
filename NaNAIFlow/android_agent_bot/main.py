from __future__ import annotations

import time

from .config import load_settings
from .db import BotDatabase
from .runner import CommandRouter, JobRunner
from .telegram_api import TelegramClient


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
            {"command": "review", "description": "Run manual review on a job"},
            {"command": "runandroid", "description": "Install and launch Android app for a job"},
            {"command": "syncandroid", "description": "Run Gradle sync-style check for a job"},
            {"command": "currentlog", "description": "Show CLI log mode and enable live CLI logs"},
            {"command": "endlog", "description": "Disable live CLI logs"},
            {"command": "pause", "description": "Pause a running or queued job"},
            {"command": "resume", "description": "Resume a paused job"},
            {"command": "approve", "description": "Approve current stage"},
            {"command": "reject", "description": "Reject stage with feedback"},
            {"command": "cancel", "description": "Cancel a job"},
        ]
    )
    worker = JobRunner(settings, db, telegram)
    router = CommandRouter(settings, db, telegram, worker)
    worker.start()

    offset: int | None = None
    while True:
        try:
            updates = telegram.get_updates(offset=offset, timeout=settings.poll_timeout_seconds)
            for update in updates:
                offset = int(update["update_id"]) + 1
                router.handle_update(update)
        except KeyboardInterrupt:
            worker.stop()
            raise
        except Exception as exc:  # noqa: BLE001
            time.sleep(3)
            print(f"telegram polling error: {exc}")


if __name__ == "__main__":
    main()
