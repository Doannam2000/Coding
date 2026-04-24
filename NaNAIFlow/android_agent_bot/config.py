from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


def _load_dotenv(dotenv_path: Path) -> None:
    if not dotenv_path.exists():
        return
    for raw_line in dotenv_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        if not key or key in os.environ:
            continue
        os.environ[key] = value.strip()


def _env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def _env_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None or not raw.strip():
        return default
    return int(raw)


def _env_list(name: str, default: list[str]) -> list[str]:
    raw = os.getenv(name)
    if raw is None or not raw.strip():
        return default
    return [item.strip() for item in raw.split(";") if item.strip()]


@dataclass(slots=True)
class Settings:
    telegram_bot_token: str
    allowed_telegram_id: int
    allowed_telegram_chat_id: int | None
    open_code_binary: str
    open_code_timeout_seconds: int
    open_code_restart_attempts: int
    open_code_min_request_interval_seconds: float
    android_cli_binary: str
    projects_root: Path
    database_path: Path
    poll_timeout_seconds: int
    worker_poll_seconds: int
    require_stage_approval: bool
    approval_stages: list[str]
    max_verify_repairs: int
    max_review_loops: int
    kotlin_package_prefix: str
    android_min_sdk: int
    gradle_verify_commands: list[str]


def load_settings() -> Settings:
    _load_dotenv(Path.cwd() / ".env")
    workspace_root = Path(os.getenv("WORKSPACE_ROOT", Path.cwd()))
    projects_root = Path(os.getenv("ANDROID_PROJECTS_ROOT", Path("D:/Code")))
    database_path = Path(os.getenv("ANDROID_AGENT_DB_PATH", workspace_root / "data" / "android-agent.sqlite3"))
    allowed_chat = os.getenv("ALLOWED_TELEGRAM_CHAT_ID")
    gradle_defaults = [
        ".\\gradlew.bat testDebugUnitTest",
        ".\\gradlew.bat lintDebug",
        ".\\gradlew.bat assembleDebug",
    ]

    token = os.getenv("TELEGRAM_BOT_TOKEN", "").strip()
    allowed_id = os.getenv("ALLOWED_TELEGRAM_ID", "").strip()
    if not token:
        raise RuntimeError("Missing TELEGRAM_BOT_TOKEN")
    if not allowed_id:
        raise RuntimeError("Missing ALLOWED_TELEGRAM_ID")

    return Settings(
        telegram_bot_token=token,
        allowed_telegram_id=int(allowed_id),
        allowed_telegram_chat_id=int(allowed_chat) if allowed_chat and allowed_chat.strip() else None,
        open_code_binary=os.getenv("OPEN_CODE_BINARY", "opencode"),
        open_code_timeout_seconds=_env_int("OPEN_CODE_TIMEOUT_SECONDS", 600),
        open_code_restart_attempts=_env_int("OPEN_CODE_RESTART_ATTEMPTS", 2),
        open_code_min_request_interval_seconds=float(os.getenv("OPEN_CODE_MIN_REQUEST_INTERVAL_SECONDS", "2.0")),
        android_cli_binary=os.getenv("ANDROID_CLI_BINARY", "android"),
        projects_root=projects_root,
        database_path=database_path,
        poll_timeout_seconds=_env_int("TELEGRAM_POLL_TIMEOUT_SECONDS", 30),
        worker_poll_seconds=_env_int("ANDROID_AGENT_WORKER_POLL_SECONDS", 5),
        require_stage_approval=_env_bool("ANDROID_AGENT_REQUIRE_STAGE_APPROVAL", False),
        approval_stages=_env_list("ANDROID_AGENT_APPROVAL_STAGES", ["idea", "plan", "design"]),
        max_verify_repairs=_env_int("ANDROID_AGENT_MAX_VERIFY_REPAIRS", 2),
        max_review_loops=_env_int("ANDROID_AGENT_MAX_REVIEW_LOOPS", 3),
        kotlin_package_prefix=os.getenv("ANDROID_AGENT_PACKAGE_PREFIX", "com.nanai.generated"),
        android_min_sdk=_env_int("ANDROID_AGENT_MIN_SDK", 26),
        gradle_verify_commands=_env_list("ANDROID_AGENT_VERIFY_COMMANDS", gradle_defaults),
    )
