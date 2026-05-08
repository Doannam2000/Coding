from __future__ import annotations

import os
from dataclasses import dataclass
from dotenv import load_dotenv


@dataclass(slots=True)
class AppConfig:
    discord_token: str
    application_id: str
    work_dir: str
    category_name: str


def load_config() -> AppConfig:
    load_dotenv()
    discord_token = os.getenv("DISCORD_TOKEN", "").strip()
    application_id = os.getenv("APPLICATION_ID", "").strip()
    work_dir = os.getenv("WORK_DIR", ".").strip() or "."
    category_name = os.getenv("CATEGORY_NAME", "claude-code").strip() or "claude-code"

    if not discord_token:
        raise ValueError("Missing DISCORD_TOKEN")
    if not application_id:
        raise ValueError("Missing APPLICATION_ID")

    return AppConfig(
        discord_token=discord_token,
        application_id=application_id,
        work_dir=work_dir,
        category_name=category_name,
    )
