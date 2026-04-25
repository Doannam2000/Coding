from __future__ import annotations

import json
import random
import re
import subprocess
import threading
import time
from collections.abc import Callable
from datetime import datetime
from pathlib import Path
from typing import Any

from .config import Settings
from .db import BotDatabase, STAGES, WIZARD_FIELDS
from .opencode import OpenCodeClient, run_powershell
from .prompts import code_prompt, design_prompt, idea_prompt, plan_prompt, repair_prompt, review_prompt
from .telegram_api import TelegramClient


AGENT_MD_TEMPLATE = """# agent.md (ULTRA BUILDER + GENERATOR MODE)

## ROLE
You are an elite Android Architect + UI/UX Designer + Product Builder.

You generate complete, production-ready Android apps using Kotlin + Jetpack Compose.

You NEVER:
- generate demo apps
- generate incomplete code
- generate broken UI

You ALWAYS:
- build real apps
- ensure UI is clean and balanced
- ensure full navigation flow
- ensure all states handled

---

## CORE STACK (MANDATORY)
- Kotlin
- Jetpack Compose (Material 3)
- MVVM
- Navigation Compose
- StateFlow
- Hilt
- Coroutines
- Retrofit
- DataStore
- Coil

---

## BUILD MODE

You support 2 modes:

### MODE 1 - FULL BUILD
Generate full Android project with:
- all screens
- navigation
- UI components
- sample data
- state handling

### MODE 2 - FEATURE BUILD
Generate:
- 1 feature module
- with ViewModel + UI + state

---

## UI RULE ENGINE (STRICT)

You must enforce:

### Layout safety
- No overlapping UI
- No clipped text
- No hardcoded widths (unless justified)
- Always responsive

### Text rules
- maxLines applied
- ellipsis when needed
- avoid long unwrapped text

### Spacing system
4 / 8 / 12 / 16 / 20 / 24 dp

### Button rules
- proper height (48-56dp)
- balanced padding
- no edge collision
- loading + disabled states

---

## CODE QUALITY RULES (STRICT)

You must enforce:

### Component separation
- Never write large UI directly inside one screen.
- Each screen must be split into reusable composables.
- Extract repeated UI into components.
- Keep Screen composables clean and readable.
- Use clear naming for components, state, events, and models.

### No hardcoded values
- Do not hardcode user-facing text.
- Do not hardcode colors directly inside screens.
- Do not hardcode dimensions randomly.
- Use:
  - `strings.xml` for text
  - theme colors for colors
  - spacing constants when repeated
  - typed models instead of raw maps

### DataStore only
- Use DataStore Preferences for local settings.
- Never use SharedPreferences.
- Store:
  - first launch state
  - theme mode
  - notification settings
  - onboarding completed state
  - lightweight user preferences

### String resource rules
- All user-facing text must be placed in `res/values/strings.xml`.
- Default language must be English.
- Do not write visible text directly in Kotlin composables.
- Use `stringResource(R.string.xxx)` in Compose.

### Multi-language generation
After creating English strings, automatically generate translated `strings.xml` files for:

`af, am, ar, be, bg, bn, bs, ca, co, cs, da, de, el, es, et, eu, fa, fi, fr, fy, ga, gl, gu, haw, hi, hr, ht, hu, hy, id, in, is, it, iw, ja, ka, ko, ky, lb, lo, lt, lv, mg, mk, mn, ms, nl, no, pl, pt, ro, ru, sk, sl, sm, sq, sr, sv, tg, th, tl, tr, uk, uz, vi, zh`

For each language:
- Create proper folder format:
  - `values-af/strings.xml`
  - `values-am/strings.xml`
  - `values-ar/strings.xml`
  - ...
  - `values-vi/strings.xml`
  - `values-zh/strings.xml`
- Keep string keys identical across all languages.
- Escape special XML characters.
- Do not remove or rename string keys.
- Do not leave untranslated English text unless translation is unsafe or brand-specific.
- Preserve app name, package name, brand names, and technical terms when needed.

### Localization safety
- All strings must be short enough to avoid UI overflow.
- Buttons must support long translated text.
- Use `maxLines`, `softWrap`, and `TextOverflow.Ellipsis` where needed.
- Layouts must handle RTL languages such as Arabic, Persian, Hebrew, and Urdu-like scripts.
- Do not rely on fixed text width.

----------

## REQUIRED SCREENS

Always include:

- Splash
- Intro
- Home
- Search
- Detail
- Favorites
- Notifications
- Profile
- Settings
- About
- Exit Dialog
- Empty / Error / Loading states

---

## NAVIGATION FLOW

Splash ->
  first time -> Intro -> Home
  else -> Home

Home:
- entry to all features

Settings:
- theme
- notifications
- about
- logout
- exit

Exit:
- must confirm

---

## STATE SYSTEM

Each screen:
- Loading
- Success
- Empty
- Error

Use sealed class UiState

---

## COMPONENT LIBRARY (REQUIRED)

- AppTopBar
- PrimaryButton
- SecondaryButton
- AppCard
- SearchBar
- SectionHeader
- EmptyStateView
- ErrorStateView
- LoadingView
- SettingItem
- ConfirmationDialog

---

## FILE STRUCTURE

data/
domain/
ui/
viewmodel/
di/

---

## DESIGN STYLE

- modern
- minimal
- premium
- clean
- soft UI
- strong hierarchy

---

## OUTPUT RULE

When building:

- always full runnable code
- no TODO
- no pseudo
- include navigation
- include theme
- include preview where useful

---

## FINAL CHECK

Before output:

Check:
- small screen safe
- text not overflow
- buttons not broken
- spacing consistent

Fix everything before finalizing.

---

## INPUT FORMAT (FROM GENERATOR)

You will receive input like:

{
  "app_name": "...",
  "idea": "...",
  "target_users": "...",
  "features": [...],
  "style": "...",
  "complexity": "simple | medium | advanced"
}

You must:
- interpret it
- expand into full Android app
- generate clean architecture code
"""

PROJECT_CORE_START = "<!-- AUTO-GENERATED:CORE_START -->"
PROJECT_CORE_END = "<!-- AUTO-GENERATED:CORE_END -->"
CODEX_HARDCODED_MODELS = [
    "gpt-5.5",
    "gpt-5.4",
    "gpt-5.3-codex",
    "gpt-5.1-codex-mini",
    "gpt-5.2",
    "gpt-5.3-codex-high",
    "gpt-5.3-codex-xhigh",
    "gpt-5.3-codex-low",
    "gpt-5.1",
    "gpt-5.3-codex-none",
    "gpt-5.1-codex-mini-high",
    "gpt-5-codex",
    "gpt-5.3-codex-spark",
    "gpt-5.2-codex",
    "gpt-5.1-codex-max",
    "gpt-5.1-codex",
    "gpt-5-codex-mini",
]
WORKSPACE_BOT_DOC_FILES = {"agent.md", "project.md"}


def slugify(text: str) -> str:
    clean = re.sub(r"[^a-zA-Z0-9]+", "-", text.strip().lower()).strip("-")
    return clean or "android-app"


def compact_json_preview(payload: dict[str, Any]) -> str:
    pieces: list[str] = []
    for key, value in payload.items():
        if isinstance(value, list):
            items = ", ".join(str(item) for item in value[:3])
            suffix = "..." if len(value) > 3 else ""
            pieces.append(f"{key}: {items}{suffix}")
        elif isinstance(value, dict):
            pieces.append(f"{key}: {', '.join(value.keys())}")
        else:
            pieces.append(f"{key}: {value}")
        if len(pieces) == 4:
            break
    return "\n".join(f"- {piece}" for piece in pieces)


def run_git(workspace: Path, *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(["git", *args], cwd=workspace, capture_output=True, text=True, check=False)


def git_repo_root(workspace: Path) -> Path | None:
    result = run_git(workspace, "rev-parse", "--show-toplevel")
    if result.returncode != 0:
        return None
    root_text = (result.stdout or "").strip()
    if not root_text:
        return None
    return Path(root_text)


def human_app_name(text: str) -> str:
    words = re.sub(r"[^a-zA-Z0-9]+", " ", text).split()
    if not words:
        return "Android App"
    return " ".join(word.capitalize() for word in words[:4])


class JobRunner:
    RUNTIME_MODEL_KEY = "worker.selected_model"
    RUNTIME_CLI_BINARY_KEY = "worker.cli_binary"
    RUNTIME_STAGE_RETRY_LIMIT_KEY = "worker.stage_retry_limit"
    CODE_STAGE_MIN_TIMEOUT_SECONDS = 900
    REPAIR_STAGE_MIN_TIMEOUT_SECONDS = 1200
    REVIEW_STAGE_MIN_TIMEOUT_SECONDS = 900
    DEFAULT_STAGE_RETRY_LIMIT = 100
    RETRY_DELAY_MIN_SECONDS = 3.0
    RETRY_DELAY_MAX_SECONDS = 12.0
    OPENCODE_MODEL_ALIAS_MAP = {
        "bigpickle": "opencode/big-pickle",
        "big-pickle": "opencode/big-pickle",
        "minimax": "opencode/minimax-m2.5-free",
        "nemotron": "opencode/nemotron-3-super-free",
    }
    MODEL_EFFORT_SUFFIXES = {"low", "medium", "high", "xhigh", "none"}

    def __init__(self, settings: Settings, db: BotDatabase, telegram: TelegramClient) -> None:
        self.settings = settings
        self.db = db
        self.telegram = telegram
        persisted_binary = self.db.get_runtime_setting(self.RUNTIME_CLI_BINARY_KEY, None)
        runtime_binary = str(persisted_binary).strip() if isinstance(persisted_binary, str) and persisted_binary.strip() else settings.open_code_binary
        self.opencode = OpenCodeClient(
            runtime_binary,
            timeout_seconds=settings.open_code_timeout_seconds,
            restart_attempts=settings.open_code_restart_attempts,
            min_request_interval_seconds=settings.open_code_min_request_interval_seconds,
        )
        self._stop = threading.Event()
        self._active_jobs: dict[int, dict[str, Any]] = {}
        self._live_cli_log_chats: set[int] = set()
        self._cli_log_default_seen_chats: set[int] = set()
        configured_retry_limit = int(
            self.db.get_runtime_setting(self.RUNTIME_STAGE_RETRY_LIMIT_KEY, self.DEFAULT_STAGE_RETRY_LIMIT)
            or self.DEFAULT_STAGE_RETRY_LIMIT
        )
        self._stage_retry_limit = max(self.DEFAULT_STAGE_RETRY_LIMIT, configured_retry_limit)
        persisted_model = self.db.get_runtime_setting(self.RUNTIME_MODEL_KEY, None)
        self._selected_model: str | None = str(persisted_model).strip() if isinstance(persisted_model, str) and persisted_model.strip() else None
        if self.settings.allowed_telegram_chat_id is not None:
            self._live_cli_log_chats.add(self.settings.allowed_telegram_chat_id)
            self._cli_log_default_seen_chats.add(self.settings.allowed_telegram_chat_id)

    def start(self) -> threading.Thread:
        thread = threading.Thread(target=self.run_forever, name="android-agent-worker", daemon=True)
        thread.start()
        return thread

    def stop(self) -> None:
        self._stop.set()

    def set_live_cli_logs(self, chat_id: int, enabled: bool) -> bool:
        self._cli_log_default_seen_chats.add(chat_id)
        if enabled:
            already = chat_id in self._live_cli_log_chats
            self._live_cli_log_chats.add(chat_id)
            return not already
        already = chat_id in self._live_cli_log_chats
        self._live_cli_log_chats.discard(chat_id)
        return already

    def ensure_default_live_cli_logs(self, chat_id: int) -> None:
        if chat_id in self._cli_log_default_seen_chats:
            return
        self._cli_log_default_seen_chats.add(chat_id)
        self._live_cli_log_chats.add(chat_id)

    def live_cli_logs_enabled(self, chat_id: int) -> bool:
        return chat_id in self._live_cli_log_chats

    def set_model(self, model: str | None) -> None:
        cleaned = self._normalize_model_for_cli(model, self._current_cli_name())
        self._selected_model = cleaned or None
        self.db.set_runtime_setting(self.RUNTIME_MODEL_KEY, self._selected_model)

    def selected_model(self) -> str | None:
        return self._selected_model

    def cli_binary(self) -> str:
        return self.opencode.binary

    def set_cli_binary(self, binary: str) -> str:
        cleaned = (binary or "").strip().lower()
        if cleaned not in {"opencode", "codex"}:
            raise ValueError("CLI must be 'opencode' or 'codex'")
        self.opencode = OpenCodeClient(
            cleaned,
            timeout_seconds=self.settings.open_code_timeout_seconds,
            restart_attempts=self.settings.open_code_restart_attempts,
            min_request_interval_seconds=self.settings.open_code_min_request_interval_seconds,
        )
        self.db.set_runtime_setting(self.RUNTIME_CLI_BINARY_KEY, cleaned)
        self._reconcile_models_for_current_cli()
        return self.opencode.binary

    def _current_cli_name(self) -> str:
        return Path(self.opencode.binary).stem.lower() or "opencode"

    def _default_model_for_cli(self, cli_name: str | None = None) -> str | None:
        resolved_cli = (cli_name or self._current_cli_name()).strip().lower()
        if resolved_cli == "codex":
            return CODEX_HARDCODED_MODELS[0]
        return None

    def _normalize_model_for_cli(self, model: str | None, cli_name: str | None = None) -> str | None:
        raw = str(model or "").strip().strip("`").strip('"').strip("'")
        if not raw or raw.lower() in {"default", "reset", "clear"}:
            return None

        resolved_cli = (cli_name or self._current_cli_name()).strip().lower()
        normalized = re.sub(r"\s+", " ", raw).strip()

        if resolved_cli == "codex":
            if "/" in normalized:
                tail = normalized.split("/")[-1].strip()
                if tail:
                    normalized = tail

            parts = normalized.split()
            if len(parts) > 1 and all(part.lower() in self.MODEL_EFFORT_SUFFIXES for part in parts[1:]):
                normalized = parts[0]
            return normalized

        if resolved_cli == "opencode":
            parts = normalized.split()
            if len(parts) > 1 and all(part.lower() in self.MODEL_EFFORT_SUFFIXES for part in parts[1:]):
                normalized = parts[0]

            lowered = normalized.lower()
            if lowered in self.OPENCODE_MODEL_ALIAS_MAP:
                return self.OPENCODE_MODEL_ALIAS_MAP[lowered]
            if "/" in normalized:
                return normalized
            if re.match(r"^(gpt-|o[1-9]|codex|claude)", lowered):
                return None
            return normalized

        return normalized

    def _reconcile_models_for_current_cli(self) -> None:
        cli_name = self._current_cli_name()
        default_model = self._default_model_for_cli(cli_name)

        normalized_runtime = self._normalize_model_for_cli(self._selected_model, cli_name)
        if normalized_runtime != self._selected_model:
            self._selected_model = normalized_runtime
            self.db.set_runtime_setting(self.RUNTIME_MODEL_KEY, self._selected_model)

        for candidate in self.db.list_jobs(limit=200):
            if candidate["status"] in {"completed", "cancelled", "failed"}:
                continue
            context = candidate.get("context", {})
            if not isinstance(context, dict):
                continue

            pinned = context.get("selected_model")
            if not isinstance(pinned, str) or not pinned.strip():
                continue

            normalized = self._normalize_model_for_cli(pinned, cli_name)
            replacement = normalized or self._selected_model or default_model
            current = pinned.strip()
            if replacement == current:
                continue

            if replacement:
                context["selected_model"] = replacement
            else:
                context.pop("selected_model", None)
            self.db.set_job_context(candidate["id"], context)

    def _current_stage_retry_count(self, job: dict[str, Any], stage_name: str) -> int:
        latest = self.db.get_job(job["id"])
        context = latest.get("context", {}) if latest is not None and isinstance(latest.get("context"), dict) else {}
        retry_counts = context.get("stage_retry_counts")
        if not isinstance(retry_counts, dict):
            return 0
        try:
            return int(retry_counts.get(stage_name, 0) or 0)
        except Exception:  # noqa: BLE001
            return 0

    def _set_stage_retry_count(self, job: dict[str, Any], stage_name: str, retry_count: int) -> None:
        latest = self.db.get_job(job["id"])
        context = latest.get("context", {}) if latest is not None and isinstance(latest.get("context"), dict) else {}
        retry_counts = context.get("stage_retry_counts")
        if not isinstance(retry_counts, dict):
            retry_counts = {}
        retry_counts[stage_name] = max(0, int(retry_count))
        context["stage_retry_counts"] = retry_counts
        updated = self.db.set_job_context(job["id"], context)
        if updated:
            job["context"] = updated.get("context", context)

    def _reset_stage_retry_count(self, job: dict[str, Any], stage_name: str) -> None:
        if self._current_stage_retry_count(job, stage_name) == 0:
            return
        self._set_stage_retry_count(job, stage_name, 0)

    def _handle_stage_failure(self, job: dict[str, Any], stage_name: str, exc: Exception) -> bool:
        current = self.db.get_job(job["id"])
        if current is not None and current["status"] in {"paused", "cancelled"}:
            return False

        error_text = str(exc).strip() or exc.__class__.__name__
        if self._is_non_retryable_stage_error(stage_name, error_text):
            return False

        retry_count = self._current_stage_retry_count(job, stage_name)
        if retry_count >= self._stage_retry_limit:
            return False

        next_retry = retry_count + 1
        delay_seconds = round(random.uniform(self.RETRY_DELAY_MIN_SECONDS, self.RETRY_DELAY_MAX_SECONDS), 1)
        self._set_stage_retry_count(job, stage_name, next_retry)
        self.db.add_event(
            job["id"],
            stage_name,
            "warning",
            f"Stage failed, queued retry {next_retry}/{self._stage_retry_limit} after {delay_seconds:.1f}s",
            {
                "error": error_text[:1000],
                "retry": next_retry,
                "retry_limit": self._stage_retry_limit,
                "delay_seconds": delay_seconds,
            },
        )
        self.telegram.send_message(
            job["chat_id"],
            f"Job #{job['id']} `{stage_name}` failed. Auto retry {next_retry}/{self._stage_retry_limit} in {delay_seconds:.1f}s.\n{error_text[:1200]}",
        )
        self._stop.wait(delay_seconds)
        self.db.requeue_running_job(job["id"])
        refreshed = self.db.get_job(job["id"])
        if refreshed is not None:
            job.update(refreshed)
        return True

    def _effective_model_for_job(self, job: dict[str, Any]) -> str | None:
        cli_name = self._current_cli_name()
        default_model = self._default_model_for_cli(cli_name)
        latest = self.db.get_job(job["id"])
        context = latest.get("context", {}) if latest is not None and isinstance(latest.get("context"), dict) else {}
        pinned = context.get("selected_model")
        if isinstance(pinned, str) and pinned.strip():
            normalized_pinned = self._normalize_model_for_cli(pinned, cli_name)
            if normalized_pinned != pinned.strip():
                if normalized_pinned:
                    context["selected_model"] = normalized_pinned
                else:
                    context.pop("selected_model", None)
                self.db.set_job_context(job["id"], context)
                job["context"] = context
            if normalized_pinned:
                return normalized_pinned

        selected = self.selected_model()
        if selected:
            normalized_selected = self._normalize_model_for_cli(selected, cli_name)
            if normalized_selected != selected:
                self._selected_model = normalized_selected or None
                self.db.set_runtime_setting(self.RUNTIME_MODEL_KEY, self._selected_model)
            if normalized_selected:
                context["selected_model"] = normalized_selected
                self.db.set_job_context(job["id"], context)
                job["context"] = context
                return normalized_selected

        if default_model:
            context["selected_model"] = default_model
            self.db.set_job_context(job["id"], context)
            job["context"] = context
            return default_model
        return None

    def log_mode_snapshot(self, chat_id: int) -> dict[str, Any]:
        cli_name = Path(self.opencode.binary).stem or "opencode"
        return {
            "enabled": self.live_cli_logs_enabled(chat_id),
            "cli": cli_name,
            "binary": self.opencode.binary,
            "model": self._selected_model or "default",
            "timeout_seconds": self.settings.open_code_timeout_seconds,
            "restart_attempts": self.settings.open_code_restart_attempts,
            "min_request_interval_seconds": self.settings.open_code_min_request_interval_seconds,
        }

    def _report_cli_output(self, job: dict[str, Any], stage_name: str, stream_name: str, line: str) -> None:
        detail = self._humanize_cli_line(stream_name, (line or "").strip())
        if not detail:
            return
        preview = self._compact_text(detail, 1200)
        self.db.add_event(
            job["id"],
            stage_name,
            "info",
            f"CLI {stream_name}: {preview}",
            {"stream": stream_name, "line": preview},
        )
        if self.live_cli_logs_enabled(job["chat_id"]):
            prefix = f"Job #{job['id']} `{stage_name}` cli {stream_name}: "
            limit = max(512, TelegramClient.TELEGRAM_TEXT_LIMIT - len(prefix) - 32)
            payload = self._compact_text(detail, limit)
            self.telegram.send_message_many(job["chat_id"], f"{prefix}{payload}")

    def _humanize_cli_line(self, stream_name: str, line: str) -> str:
        if not line:
            return ""
        cleaned = re.sub(r"\x1b\[[0-9;]*m", "", line).strip()
        if not cleaned:
            return ""

        try:
            event = json.loads(cleaned)
        except json.JSONDecodeError:
            return self._compact_text(cleaned, 6000)

        if not isinstance(event, dict):
            return self._compact_text(cleaned, 6000)

        event_type = str(event.get("type", "")).strip().lower()
        part = event.get("part") if isinstance(event.get("part"), dict) else {}
        part_type = str(part.get("type", "")).strip().lower()

        if event_type == "reasoning":
            return ""

        if event_type == "text":
            text = self._extract_cli_value(part.get("text") or part.get("content"), 6000)
            if not text or self._looks_like_json_blob(text):
                return ""
            return text

        if event_type in {"tool_use", "tool_call"} or part_type == "tool":
            detail = self._humanize_tool_event(event, part)
            if detail:
                return detail

        if event_type in {"tool_result", "tool_output"}:
            detail = self._humanize_tool_event(event, part)
            if detail:
                return detail

        if event_type in {"message", "progress", "status"}:
            text = self._extract_cli_value(part.get("text") or part.get("content") or event.get("message"), 6000)
            return text

        if event_type == "step_finish" or part_type == "step-finish":
            reason = self._compact_text(str(part.get("reason") or event.get("reason") or "done"))
            tokens = part.get("tokens") if isinstance(part.get("tokens"), dict) else {}
            if reason.lower() in {"done", "completed", "complete", "success", "stop"} and not tokens:
                return ""
            if tokens:
                in_tokens = tokens.get("input", "?")
                out_tokens = tokens.get("output", "?")
                total_tokens = tokens.get("total", "?")
                return f"finish: {reason} | tokens in/out/total={in_tokens}/{out_tokens}/{total_tokens}"
            return f"finish: {reason}"

        if part_type:
            text = self._extract_cli_value(part.get("text") or part.get("content"), 6000)
            if text:
                return f"{part_type}: {text}"
            if part_type in {"tool", "step-finish"}:
                return ""
            return part_type

        if event_type:
            return ""
        return self._compact_text(cleaned, 6000)

    def _humanize_tool_event(self, event: dict[str, Any], part: dict[str, Any]) -> str:
        state = part.get("state") if isinstance(part.get("state"), dict) else {}
        tool_name = self._compact_text(str(part.get("tool") or part.get("toolName") or part.get("name") or event.get("tool") or "tool"), 60)
        status = self._compact_text(str(state.get("status") or part.get("status") or event.get("status") or "running"), 40)
        header = f"tool {tool_name} {status}".strip()
        exit_code = state.get("exit_code")
        if exit_code is None:
            exit_code = state.get("exitCode")
        if exit_code is not None and str(exit_code).strip():
            header = f"{header} (exit {exit_code})"

        pieces = [header]
        command = self._extract_cli_command(state.get("input"))
        if command:
            pieces.append(f"command: {command}")
        elif state.get("input") is not None:
            input_text = self._extract_cli_value(state.get("input"), 2400)
            if input_text:
                pieces.append(f"input: {input_text}")

        output_value = None
        for candidate in (
            state.get("summary"),
            state.get("output"),
            state.get("result"),
            state.get("error"),
            part.get("summary"),
            part.get("text"),
            part.get("content"),
            event.get("message"),
        ):
            if candidate not in (None, "", [], {}):
                output_value = candidate
                break
        output_text = self._extract_cli_value(output_value, 2800)
        if output_text and output_text != command and not self._looks_like_json_blob(output_text):
            label = "error" if state.get("error") not in (None, "") else "output"
            pieces.append(f"{label}: {output_text}")
        return " | ".join(piece for piece in pieces if piece)

    def _extract_cli_command(self, payload: Any) -> str:
        if isinstance(payload, str):
            return self._compact_text(payload, 2800)
        if not isinstance(payload, dict):
            return ""
        for key in ("command", "cmd", "script", "arguments", "args", "prompt"):
            value = payload.get(key)
            if isinstance(value, str) and value.strip():
                return self._compact_text(value, 2800)
            if isinstance(value, list):
                parts = [str(item).strip() for item in value if str(item).strip()]
                if parts:
                    return self._compact_text(" ".join(parts), 2800)
        return ""

    def _extract_cli_value(self, value: Any, limit: int = 260) -> str:
        if value is None:
            return ""
        if isinstance(value, str):
            return self._compact_text(value, limit)
        if isinstance(value, bool | int | float):
            return str(value)
        if isinstance(value, list):
            parts: list[str] = []
            for item in value[:8]:
                text = self._extract_cli_value(item, min(limit, 400))
                if text:
                    parts.append(text)
            return self._compact_text(", ".join(parts), limit)
        if isinstance(value, dict):
            for key in ("summary", "text", "content", "message", "stdout", "stderr", "result", "error", "command"):
                text = self._extract_cli_value(value.get(key), limit)
                if text:
                    return text
            return self._compact_text(json.dumps(value, ensure_ascii=True), limit)
        return self._compact_text(str(value), limit)

    def _looks_like_json_blob(self, text: str) -> bool:
        stripped = (text or "").strip()
        if not stripped or stripped[0] not in "{[":
            return False
        try:
            json.loads(stripped)
        except json.JSONDecodeError:
            return False
        return True

    def _compact_text(self, text: str, limit: int = 260) -> str:
        compact = re.sub(r"\s+", " ", (text or "").strip())
        if len(compact) <= limit:
            return compact
        return compact[: limit - 3] + "..."

    def run_forever(self) -> None:
        while not self._stop.is_set():
            job = self.db.claim_next_job()
            if job is None:
                self._stop.wait(self.settings.worker_poll_seconds)
                continue

            try:
                self._run_job(job)
            except Exception as exc:  # noqa: BLE001
                stage_name = job.get("current_stage", "unknown")
                self._active_jobs.pop(job["id"], None)
                current = self.db.get_job(job["id"])
                if current is not None and current["status"] == "paused":
                    self.telegram.send_message(job["chat_id"], f"Job #{job['id']} paused at `{current['current_stage']}`")
                    continue
                if current is not None and current["status"] == "cancelled":
                    self.telegram.send_message(job["chat_id"], f"Job #{job['id']} cancelled and stopped")
                    continue
                if self._handle_stage_failure(job, stage_name, exc):
                    continue
                self.db.mark_job_failed(job["id"], str(exc), stage_name)
                self.telegram.send_message(job["chat_id"], f"Job #{job['id']} failed at `{stage_name}`\n{exc}")

    def _run_job(self, job: dict[str, Any]) -> None:
        current = self.db.get_job(job["id"])
        if current is None or current["status"] in {"paused", "cancelled"}:
            return
        stage_name = STAGES[job["stage_index"]]
        self._active_jobs[job["id"]] = {"stage": stage_name, "started_at": time.time(), "detail": "Stage bootstrapped"}
        self.db.add_event(job["id"], stage_name, "info", "Stage started", None)
        self.telegram.send_message(job["chat_id"], f"Job #{job['id']} stage `{stage_name}` started")

        workspace = Path(job["workspace_path"])
        context = job["context"]
        model_for_job = self._effective_model_for_job(job)
        if stage_name == "idea":
            payload = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=idea_prompt(job),
                model=model_for_job,
                timeout_seconds=max(self.settings.open_code_timeout_seconds, 900),
                on_progress=lambda message: self._report_progress(job, stage_name, message),
                on_watchdog=lambda message: self._report_watchdog(job, stage_name, message),
                on_cli_output=lambda stream, line: self._report_cli_output(job, stage_name, stream, line),
            )
        elif stage_name == "plan":
            payload = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=plan_prompt(job, context),
                model=model_for_job,
                timeout_seconds=max(self.settings.open_code_timeout_seconds, 900),
                on_progress=lambda message: self._report_progress(job, stage_name, message),
                on_watchdog=lambda message: self._report_watchdog(job, stage_name, message),
                on_cli_output=lambda stream, line: self._report_cli_output(job, stage_name, stream, line),
            )
        elif stage_name == "design":
            payload = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=design_prompt(job, context),
                model=model_for_job,
                timeout_seconds=max(self.settings.open_code_timeout_seconds, 1200),
                on_progress=lambda message: self._report_progress(job, stage_name, message),
                on_watchdog=lambda message: self._report_watchdog(job, stage_name, message),
                on_cli_output=lambda stream, line: self._report_cli_output(job, stage_name, stream, line),
            )
        elif stage_name == "code":
            self._report_progress(job, stage_name, "Checking scaffold and package setup")
            package_name = self._package_name(job)
            scaffold_result = self._ensure_android_scaffold(job, package_name)
            if scaffold_result is not None:
                self.db.add_event(job["id"], stage_name, "info", "Android scaffold prepared", scaffold_result)
                self.telegram.send_message(job["chat_id"], f"Job #{job['id']} scaffold ready\n{compact_json_preview(scaffold_result)}")
                package_name = scaffold_result.get("package_name", package_name)
            else:
                package_name = self._determine_existing_package_name(workspace) or package_name
            self._ensure_workspace_docs(job, context)
            payload = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=code_prompt(job, context, package_name, workspace, job.get("rejection_feedback")),
                model=model_for_job,
                timeout_seconds=max(self.settings.open_code_timeout_seconds, self.CODE_STAGE_MIN_TIMEOUT_SECONDS),
                on_progress=lambda message: self._report_progress(job, stage_name, message),
                on_watchdog=lambda message: self._report_watchdog(job, stage_name, message),
                on_cli_output=lambda stream, line: self._report_cli_output(job, stage_name, stream, line),
            )
            self._report_progress(job, stage_name, "Committing generated code changes")
            git_result = self._commit_workspace_changes(job, workspace, "code", f"agent(job-{job['id']}): implement Android app")
            if git_result is not None:
                payload["git"] = git_result
        elif stage_name == "verify":
            payload = self._verify_and_repair(job, model_for_job)
        elif stage_name == "review":
            payload = self._review_and_route(job, model_for_job)
        else:
            raise RuntimeError(f"Unsupported stage: {stage_name}")

        self._reset_stage_retry_count(job, stage_name)
        waiting = self.settings.require_stage_approval and stage_name in self.settings.approval_stages
        next_stage_index = self._next_stage_index(job, stage_name, payload)
        updated_job = self.db.save_stage_result(job["id"], stage_name, payload, next_stage_index, waiting)
        self.db.add_event(job["id"], stage_name, "info", "Stage completed", payload)
        summary = compact_json_preview(payload)

        if waiting:
            self.telegram.send_message(
                job["chat_id"],
                f"Job #{job['id']} stage `{stage_name}` completed and is waiting approval.\n{summary}\nUse /approve {job['id']} or /reject {job['id']}|feedback",
            )
            self._active_jobs.pop(job["id"], None)
            return

        if updated_job["status"] == "completed":
            self.telegram.send_message(
                job["chat_id"],
                f"Job #{job['id']} completed.\nWorkspace: `{job['workspace_path']}`\n{summary}",
            )
            activated = self.db.activate_next_task(job["id"])
            if activated is not None and activated.get("status") == "queued":
                active_task = ""
                context = activated.get("context", {})
                if isinstance(context, dict):
                    active_task = str(context.get("active_task") or "").strip()
                if active_task:
                    self.telegram.send_message(
                        job["chat_id"],
                        f"Job #{job['id']} auto-started next queued task:\n{active_task}\n"
                        f"Restarting from stage `{activated['current_stage']}`.",
                    )
            self._active_jobs.pop(job["id"], None)
            return

        self.telegram.send_message(job["chat_id"], f"Job #{job['id']} stage `{stage_name}` completed\n{summary}")
        self._active_jobs.pop(job["id"], None)

    def _next_stage_index(self, job: dict[str, Any], stage_name: str, payload: dict[str, Any]) -> int:
        if stage_name != "review":
            return job["stage_index"] + 1

        next_stage = str(payload.get("next_stage", "complete")).strip().lower()
        if payload.get("decision") == "iterate":
            if next_stage not in STAGES:
                raise RuntimeError(f"Review requested unsupported next stage: {next_stage}")
            return STAGES.index(next_stage)

        return len(STAGES)

    def _verify_and_repair(self, job: dict[str, Any], model_for_job: str | None) -> dict[str, Any]:
        workspace = Path(job["workspace_path"])
        attempts: list[dict[str, Any]] = []
        for repair_attempt in range(self.settings.max_verify_repairs + 1):
            self._report_progress(job, "verify", f"Running verification pass {repair_attempt + 1}")
            command_results = []
            failed_log = None
            for command in self.settings.gradle_verify_commands:
                self._report_progress(job, "verify", f"Executing `{command}`")
                result = run_powershell(command, workspace)
                command_results.append(
                    {
                        "command": command,
                        "return_code": result.returncode,
                        "stdout": (result.stdout or "")[-4000:],
                        "stderr": (result.stderr or "")[-4000:],
                    }
                )
                if result.returncode != 0:
                    failed_log = f"Command: {command}\nSTDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
                    break

            attempts.append({"repair_attempt": repair_attempt, "commands": command_results})
            if failed_log is None:
                artifact_path = self._find_debug_apk(workspace)
                return {
                    "summary": "Verification commands passed",
                    "repair_attempts": repair_attempt,
                    "commands": [item["command"] for item in command_results],
                    "apk_path": str(artifact_path) if artifact_path else "",
                    "details": attempts,
                }

            if repair_attempt >= self.settings.max_verify_repairs:
                raise RuntimeError(f"Verification failed after {repair_attempt} repair attempts\n{failed_log[-3000:]}")

            repair_result = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=repair_prompt(job, job["context"], self._determine_existing_package_name(workspace) or self._package_name(job), failed_log[-6000:]),
                model=model_for_job,
                timeout_seconds=max(self.settings.open_code_timeout_seconds, self.REPAIR_STAGE_MIN_TIMEOUT_SECONDS),
                on_progress=lambda message: self._report_progress(job, "verify", message),
                on_watchdog=lambda message: self._report_watchdog(job, "verify", message),
                on_cli_output=lambda stream, line: self._report_cli_output(job, "verify", stream, line),
            )
            git_result = self._commit_workspace_changes(
                job,
                workspace,
                "verify",
                f"agent(job-{job['id']}): repair Android verification attempt {repair_attempt + 1}",
            )
            if git_result is not None:
                repair_result["git"] = git_result
            self.db.add_event(job["id"], "verify", "warning", "Repair attempt applied", repair_result)
            self.telegram.send_message(
                job["chat_id"],
                f"Job #{job['id']} verify failed. Repair attempt {repair_attempt + 1} applied.\n{compact_json_preview(repair_result)}",
            )

        raise RuntimeError("Verification loop reached unexpected state")

    def _review_and_route(self, job: dict[str, Any], model_for_job: str | None) -> dict[str, Any]:
        context = job["context"]
        review_count = int(context.get("review_count", 0)) + 1
        if review_count > self.settings.max_review_loops:
            return {
                "summary": f"Review loop limit reached after {self.settings.max_review_loops} iterations",
                "decision": "complete",
                "next_stage": "complete",
                "reasoning": ["Maximum review loops reached, stopping to avoid infinite iteration."],
                "strengths": [],
                "issues": ["Potential unresolved follow-up work remains."],
                "action_items": [],
                "review_focus": "loop-limit",
                "review_count": review_count,
            }

        self._report_progress(job, "review", f"Running review iteration {review_count}")
        payload = self.opencode.run_json_prompt_with_progress(
            workdir=Path(job["workspace_path"]),
            prompt=review_prompt(job, context),
            model=model_for_job,
            timeout_seconds=max(self.settings.open_code_timeout_seconds, self.REVIEW_STAGE_MIN_TIMEOUT_SECONDS),
            on_progress=lambda message: self._report_progress(job, "review", message),
            on_watchdog=lambda message: self._report_watchdog(job, "review", message),
            on_cli_output=lambda stream, line: self._report_cli_output(job, "review", stream, line),
        )
        decision = str(payload.get("decision", "")).strip().lower()
        next_stage = str(payload.get("next_stage", "")).strip().lower()
        if decision not in {"complete", "iterate"}:
            raise RuntimeError(f"Review returned invalid decision: {decision}")
        if decision == "complete":
            next_stage = "complete"
        elif next_stage not in {"plan", "design", "code", "verify"}:
            raise RuntimeError(f"Review returned invalid next stage: {next_stage}")

        payload["decision"] = decision
        payload["next_stage"] = next_stage
        payload["review_count"] = review_count
        return payload

    def _report_progress(self, job: dict[str, Any], stage_name: str, detail: str) -> None:
        self._ensure_not_paused(job["id"])
        active = self._active_jobs.setdefault(
            job["id"],
            {"stage": stage_name, "started_at": time.time(), "detail": detail},
        )
        active["stage"] = stage_name
        active["detail"] = detail
        active["updated_at"] = time.time()
        self.db.add_event(job["id"], stage_name, "info", detail, None)
        self.telegram.send_message(job["chat_id"], f"Job #{job['id']} `{stage_name}`: {detail}")

    def active_progress(self, job_id: int) -> dict[str, Any] | None:
        return self._active_jobs.get(job_id)

    def _report_watchdog(self, job: dict[str, Any], stage_name: str, detail: str) -> None:
        self.db.add_event(job["id"], stage_name, "warning", f"WATCHDOG: {detail}", None)
        self.telegram.send_message(job["chat_id"], f"Job #{job['id']} `{stage_name}` watchdog: {detail}")

    def _ensure_not_paused(self, job_id: int) -> None:
        current = self.db.get_job(job_id)
        if current is not None and current["status"] == "paused":
            raise RuntimeError("Job paused by user")
        if current is not None and current["status"] == "cancelled":
            raise RuntimeError("Job cancelled by user")

    def _is_non_retryable_stage_error(self, stage_name: str, error_text: str) -> bool:
        normalized = (error_text or "").strip().lower()
        if not normalized:
            return False
        if "does not contain a valid android gradle project" in normalized:
            return True
        if stage_name == "code" and "clean it or use a new slug" in normalized:
            return True
        return False

    def _prepare_workspace_for_scaffold(self, workspace: Path) -> dict[str, str]:
        preserved: dict[str, str] = {}
        unexpected_entries: list[str] = []

        for entry in workspace.iterdir():
            if entry.name in WORKSPACE_BOT_DOC_FILES and entry.is_file():
                preserved[entry.name] = entry.read_text(encoding="utf-8", errors="ignore")
                continue
            unexpected_entries.append(entry.name)

        if unexpected_entries:
            sample = ", ".join(sorted(unexpected_entries)[:8])
            suffix = "" if len(unexpected_entries) <= 8 else f" and {len(unexpected_entries) - 8} more"
            raise RuntimeError(
                f"Workspace {workspace} is not empty and contains unexpected files: {sample}{suffix}. "
                "Clean it or use a new slug."
            )

        for filename in preserved:
            (workspace / filename).unlink(missing_ok=True)
        return preserved

    def _restore_preserved_workspace_docs(self, workspace: Path, preserved: dict[str, str]) -> None:
        for filename, content in preserved.items():
            (workspace / filename).write_text(content, encoding="utf-8")

    def _package_name(self, job: dict[str, Any]) -> str:
        requested_app_id = self._requested_app_id(job)
        if requested_app_id:
            return requested_app_id
        return f"{self.settings.kotlin_package_prefix}.{job['slug'].replace('-', '')}"

    def _requested_app_id(self, job: dict[str, Any]) -> str | None:
        texts = [job.get("request_text", ""), job.get("constraints_text", "")]
        patterns = [
            r"Requested Android applicationId:\s*([^\s]+)",
            r"Use Android applicationId/package name:\s*([^\s]+)",
        ]
        for text in texts:
            for pattern in patterns:
                match = re.search(pattern, text)
                if match:
                    return match.group(1).strip()
        return None

    def _ensure_android_scaffold(self, job: dict[str, Any], package_name: str) -> dict[str, Any] | None:
        workspace = Path(job["workspace_path"])
        workspace.mkdir(parents=True, exist_ok=True)
        gradle_wrapper = workspace / "gradlew.bat"
        app_build = workspace / "app" / "build.gradle.kts"
        if gradle_wrapper.exists() and app_build.exists():
            return None

        preserved_docs = self._prepare_workspace_for_scaffold(workspace)

        idea = job["context"].get("stages", {}).get("idea", {})
        app_name = idea.get("app_name") or human_app_name(job["slug"])
        command = [
            self.settings.android_cli_binary,
            "create",
            "empty-activity",
            f"--name={app_name}",
            f"--output={workspace}",
            f"--minSdk={self.settings.android_min_sdk}",
        ]
        result = subprocess.run(command, cwd=workspace.parent, capture_output=True, text=True, check=False)
        if result.returncode != 0:
            self._restore_preserved_workspace_docs(workspace, preserved_docs)
            error_text = (result.stderr or result.stdout or "").strip()
            raise RuntimeError(f"Android scaffold failed: {error_text}")

        init_result = self._ensure_workspace_git(job, workspace)
        scaffold_commit = self._commit_workspace_changes(job, workspace, "code", f"agent(job-{job['id']}): scaffold Android project")

        detected_package = self._determine_existing_package_name(workspace) or package_name

        payload = {
            "summary": "Android Compose scaffold created",
            "app_name": app_name,
            "package_name": detected_package,
            "workspace": str(workspace),
        }
        if init_result is not None:
            payload["git_initialized"] = init_result
        if scaffold_commit is not None:
            payload["git"] = scaffold_commit
        return payload

    def _find_debug_apk(self, workspace: Path) -> Path | None:
        matches = sorted(workspace.glob("app/build/outputs/apk/debug/*.apk"), key=lambda path: path.stat().st_mtime, reverse=True)
        return matches[0] if matches else None

    def _determine_existing_package_name(self, workspace: Path) -> str | None:
        build_file = workspace / "app" / "build.gradle.kts"
        if build_file.exists():
            text = build_file.read_text(encoding="utf-8", errors="ignore")
            namespace_match = re.search(r'namespace\s*=\s*"([^"]+)"', text)
            if namespace_match:
                return namespace_match.group(1)
            app_id_match = re.search(r'applicationId\s*=\s*"([^"]+)"', text)
            if app_id_match:
                return app_id_match.group(1)

        manifest_file = workspace / "app" / "src" / "main" / "AndroidManifest.xml"
        if manifest_file.exists():
            manifest = manifest_file.read_text(encoding="utf-8", errors="ignore")
            package_match = re.search(r'package\s*=\s*"([^"]+)"', manifest)
            if package_match:
                return package_match.group(1)
        return None

    def _ensure_workspace_git(self, job: dict[str, Any], workspace: Path) -> dict[str, Any] | None:
        if (workspace / ".git").exists():
            return None

        init_result = run_git(workspace, "init", "-b", f"job-{job['id']}")
        if init_result.returncode != 0:
            raise RuntimeError((init_result.stderr or init_result.stdout).strip() or "git init failed")

        run_git(workspace, "config", "user.name", "Android Agent Bot")
        run_git(workspace, "config", "user.email", "android-agent@example.local")
        return {
            "branch": f"job-{job['id']}",
            "summary": "Initialized git repository for generated Android project",
        }

    def _resolve_git_target_root(self, job: dict[str, Any], workspace: Path) -> Path:
        repo_root = git_repo_root(workspace)
        if repo_root is not None:
            return repo_root
        self._ensure_workspace_git(job, workspace)
        return workspace

    def _commit_workspace_changes(
        self,
        job: dict[str, Any],
        workspace: Path,
        stage_name: str,
        message: str,
    ) -> dict[str, Any] | None:
        self._ensure_workspace_git(job, workspace)
        status_result = run_git(workspace, "status", "--short")
        if status_result.returncode != 0:
            raise RuntimeError((status_result.stderr or status_result.stdout).strip() or "git status failed")

        changed_lines = [line.strip() for line in status_result.stdout.splitlines() if line.strip()]
        if not changed_lines:
            return None

        add_result = run_git(workspace, "add", ".")
        if add_result.returncode != 0:
            raise RuntimeError((add_result.stderr or add_result.stdout).strip() or "git add failed")

        commit_result = run_git(workspace, "commit", "-m", message)
        if commit_result.returncode != 0:
            raise RuntimeError((commit_result.stderr or commit_result.stdout).strip() or "git commit failed")

        branch_result = run_git(workspace, "branch", "--show-current")
        head_result = run_git(workspace, "rev-parse", "HEAD")
        branch = (branch_result.stdout or "").strip()
        commit = (head_result.stdout or "").strip()
        payload = {
            "branch": branch,
            "commit": commit,
            "changed_files": [self._extract_status_path(line) for line in changed_lines],
        }
        self.db.add_event(job["id"], stage_name, "info", "Committed workspace changes", payload)
        return payload

    def _extract_status_path(self, status_line: str) -> str:
        trimmed = status_line[3:].strip() if len(status_line) > 3 else status_line
        if " -> " in trimmed:
            return trimmed.split(" -> ")[-1].strip()
        return trimmed

    def _ensure_workspace_docs(self, job: dict[str, Any], context: dict[str, Any]) -> None:
        workspace = Path(job["workspace_path"])
        workspace.mkdir(parents=True, exist_ok=True)

        agent_path = workspace / "agent.md"
        if not agent_path.exists():
            agent_path.write_text(AGENT_MD_TEMPLATE.strip() + "\n", encoding="utf-8")

        project_path = workspace / "project.md"
        project_section = self._render_project_core_section(job, context)
        if project_path.exists():
            existing = project_path.read_text(encoding="utf-8", errors="ignore")
        else:
            existing = "# Project Notes\n"

        if PROJECT_CORE_START in existing and PROJECT_CORE_END in existing:
            before, remainder = existing.split(PROJECT_CORE_START, 1)
            _, after = remainder.split(PROJECT_CORE_END, 1)
            merged = before.rstrip() + "\n\n" + project_section + "\n" + after.lstrip("\n")
        else:
            merged = existing.rstrip() + "\n\n" + project_section + "\n"
        project_path.write_text(merged.strip() + "\n", encoding="utf-8")

    def _render_project_core_section(self, job: dict[str, Any], context: dict[str, Any]) -> str:
        stages = context.get("stages", {}) if isinstance(context.get("stages"), dict) else {}
        idea = stages.get("idea") if isinstance(stages.get("idea"), dict) else {}
        plan = stages.get("plan") if isinstance(stages.get("plan"), dict) else {}
        design = stages.get("design") if isinstance(stages.get("design"), dict) else {}

        app_name = str(idea.get("app_name") or human_app_name(job.get("slug", ""))).strip()
        tagline = str(idea.get("tagline") or "").strip()
        target_users = str(job.get("target_users") or "").strip()
        constraints = str(job.get("constraints_text") or "").strip()

        core_features: list[str] = []
        for item in idea.get("mvp_features", []) if isinstance(idea.get("mvp_features"), list) else []:
            text = str(item).strip()
            if text and text not in core_features:
                core_features.append(text)
        if not core_features:
            match = re.search(r"Requested features:\s*(.+)", str(job.get("request_text") or ""))
            if match:
                for raw in match.group(1).split(","):
                    text = raw.strip()
                    if text and text not in core_features:
                        core_features.append(text)

        screen_names: list[str] = []
        for screen in plan.get("screens", []) if isinstance(plan.get("screens"), list) else []:
            if not isinstance(screen, dict):
                continue
            name = str(screen.get("name") or "").strip()
            if name and name not in screen_names:
                screen_names.append(name)

        architecture_items: list[str] = []
        if isinstance(plan.get("architecture"), dict):
            architecture = plan["architecture"]
            for key in ("ui", "pattern", "storage"):
                value = str(architecture.get(key) or "").strip()
                if value:
                    architecture_items.append(f"{key}: {value}")
        if not architecture_items:
            architecture_items = [
                "ui: Jetpack Compose",
                "pattern: MVVM",
                "state: StateFlow",
                "navigation: Navigation Compose",
            ]

        visual_direction = str(design.get("visual_direction") or "").strip()
        updated_at = datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")

        lines = [
            PROJECT_CORE_START,
            "## Core App Snapshot (Auto)",
            f"- Last updated: {updated_at}",
            f"- App: {app_name}",
            f"- Slug: {job.get('slug', '')}",
        ]
        if tagline:
            lines.append(f"- Tagline: {tagline}")
        if target_users:
            lines.append(f"- Target users: {target_users}")
        if visual_direction:
            lines.append(f"- Design direction: {visual_direction}")
        if constraints:
            lines.append(f"- Core constraints: {constraints}")

        lines.append("\n### Core Features")
        if core_features:
            lines.extend(f"- {item}" for item in core_features)
        else:
            lines.append("- (pending feature extraction)")

        lines.append("\n### Main Screens")
        if screen_names:
            lines.extend(f"- {name}" for name in screen_names)
        else:
            lines.append("- (pending screen planning)")

        lines.append("\n### Architecture Core")
        lines.extend(f"- {item}" for item in architecture_items)
        lines.append(PROJECT_CORE_END)
        return "\n".join(lines)


class CommandRouter:
    REPLAYABLE_COMMAND_PREFIXES = (
        "/runandroid",
        "/syncandroid",
        "/review",
        "/setrepo",
        "/pushgit",
        "/addfeature",
        "/addtask",
        "/editapp",
        "/buildbyprompt",
        "/fixbug",
    )

    def __init__(self, settings: Settings, db: BotDatabase, telegram: TelegramClient, worker: JobRunner | None = None) -> None:
        self.settings = settings
        self.db = db
        self.telegram = telegram
        self.worker = worker
        self._model_picker_cache: dict[int, dict[str, Any]] = {}
        self._manual_review_threads: dict[int, threading.Thread] = {}
        self._manual_review_lock = threading.Lock()
        self._runandroid_threads: dict[int, threading.Thread] = {}
        self._runandroid_lock = threading.Lock()
        self._last_command_by_chat: dict[int, str] = {}

    def _menu_keyboard(self) -> dict[str, Any]:
        return {
            "keyboard": [
                [{"text": "Start Build Wizard"}, {"text": "Build App Template"}],
                [{"text": "/help"}, {"text": "/jobs"}],
                [{"text": "/progress"}, {"text": "/logs"}],
                [{"text": "/models"}, {"text": "/model"}],
                [{"text": "/currentLog"}, {"text": "/endLog"}],
                [{"text": "/tail"}, {"text": "/pause"}],
                [{"text": "/resume"}, {"text": "/cancel"}],
            ],
            "resize_keyboard": True,
            "is_persistent": True,
        }

    def _job_action_markup(self, job_id: int, include_approval: bool) -> dict[str, Any]:
        rows: list[list[dict[str, str]]] = [
            [
                {"text": "Status", "callback_data": f"job:status:{job_id}"},
                {"text": "Logs", "callback_data": f"job:logs:{job_id}"},
            ]
        ]
        rows.append(
            [
                {"text": "Progress", "callback_data": f"job:progress:{job_id}"},
                {"text": "Pause", "callback_data": f"job:pause:{job_id}"},
                {"text": "Resume", "callback_data": f"job:resume:{job_id}"},
            ]
        )
        rows.append(
            [
                {"text": "Enable CLI Log", "callback_data": f"log:enable:{job_id}"},
                {"text": "Disable CLI Log", "callback_data": f"log:disable:{job_id}"},
            ]
        )
        if include_approval:
            rows.append(
                [
                    {"text": "Approve", "callback_data": f"job:approve:{job_id}"},
                    {"text": "Reject Help", "callback_data": f"job:rejecthelp:{job_id}"},
                ]
            )
        rows.append([{"text": "Cancel", "callback_data": f"job:cancel:{job_id}"}])
        return {"inline_keyboard": rows}

    def _wizard_questions(self) -> dict[str, str]:
        return {
            "slug": "Nhap slug app. Vi du: `coffee-social`",
            "idea": "Nhap y tuong app chinh.",
            "style": "Nhap style giao dien mong muon. Vi du: `warm modern editorial`",
            "font": "Nhap huong font chu mong muon. Vi du: `minimal geometric sans`",
            "features": "Nhap cac chuc nang uu tien, cach nhau boi dau phay.",
            "requirements": "Nhap yeu cau bo sung bat buoc (flow, ky thuat, UX, data, ...).",
            "target_users": "App nay danh cho ai?",
            "constraints": "Nhap rang buoc. Vi du: `android only, MVP first, offline-friendly`",
            "app_id": "Nhap Android app id. Vi du: `com.nantcompany.daily.quote`",
        }

    def _start_wizard(self, chat_id: int, user_id: int) -> None:
        state = {"mode": "buildapp", "step_index": 0, "values": {}}
        self.db.save_wizard_state(chat_id, user_id, state)
        self.telegram.send_message_with_markup(
            chat_id,
            "Build wizard started.\n" + self._wizard_questions()[WIZARD_FIELDS[0]],
            reply_markup=self._menu_keyboard(),
        )

    def _continue_wizard(self, chat_id: int, user_id: int, text: str, state: dict[str, Any]) -> bool:
        step_index = int(state.get("step_index", 0))
        if step_index < 0 or step_index >= len(WIZARD_FIELDS):
            self.db.clear_wizard_state(chat_id)
            self.telegram.send_message(chat_id, "Wizard state was invalid and has been reset. Send 'Start Build Wizard' again.")
            return True

        field = WIZARD_FIELDS[step_index]
        value = text.strip()
        if not value:
            self.telegram.send_message(chat_id, "Gia tri khong duoc de trong. Thu gui lai.")
            return True

        state.setdefault("values", {})[field] = value
        step_index += 1
        if step_index >= len(WIZARD_FIELDS):
            self.db.clear_wizard_state(chat_id)
            self._queue_build_job(chat_id, user_id, state["values"])
            return True

        state["step_index"] = step_index
        self.db.save_wizard_state(chat_id, user_id, state)
        self.telegram.send_message_with_markup(
            chat_id,
            self._wizard_questions()[WIZARD_FIELDS[step_index]],
            reply_markup=self._menu_keyboard(),
        )
        return True

    def handle_update(self, update: dict[str, Any]) -> None:
        callback_query = update.get("callback_query")
        if callback_query:
            self._handle_callback_query(callback_query)
            return

        message = update.get("message") or update.get("edited_message")
        if not message:
            return
        chat_id = int(message["chat"]["id"])
        from_user = message.get("from") or {}
        user_id = int(from_user.get("id", 0))
        text = (message.get("text") or "").strip()
        if user_id != self.settings.allowed_telegram_id:
            self.telegram.send_message(chat_id, "Unauthorized user")
            return
        if self.settings.allowed_telegram_chat_id is not None and chat_id != self.settings.allowed_telegram_chat_id:
            self.telegram.send_message(chat_id, "Unauthorized chat")
            return
        if self.worker is not None:
            self.worker.ensure_default_live_cli_logs(chat_id)

        wizard_state = self.db.get_wizard_state(chat_id)
        if wizard_state and not text.startswith("/") and text not in {"Build App Template", "Start Build Wizard"}:
            if self._continue_wizard(chat_id, user_id, text, wizard_state):
                return

        if text == "Start Build Wizard":
            self._start_wizard(chat_id, user_id)
            return
        if text == "Build App Template":
            self._send_buildapp_template(chat_id)
            return
        if not text.startswith("/"):
            return
        self._dispatch_command(chat_id, user_id, text)

    def _dispatch_command(self, chat_id: int, user_id: int, text: str, *, record_last: bool = True) -> None:
        normalized = text.strip()
        if (
            record_last
            and normalized.startswith("/")
            and not normalized.lower().startswith("/resume")
            and self._is_replayable_command(normalized)
        ):
            self._last_command_by_chat[chat_id] = normalized

        if normalized.startswith("/start") or normalized.startswith("/help"):
            self._send_help(chat_id)
        elif normalized.startswith("/buildbyprompt"):
            self._build_by_prompt(chat_id, user_id, normalized)
        elif normalized.startswith("/buildapp"):
            self._build_app(chat_id, user_id, normalized)
        elif normalized.startswith("/newandroid"):
            self._create_job(chat_id, user_id, normalized)
        elif normalized.startswith("/jobs"):
            self._jobs(chat_id)
        elif normalized.startswith("/status"):
            self._status(chat_id, normalized)
        elif normalized.startswith("/progress"):
            self._progress(chat_id, normalized)
        elif normalized.startswith("/tail"):
            self._tail(chat_id, normalized)
        elif normalized.startswith("/approve"):
            self._approve(chat_id, normalized)
        elif normalized.startswith("/reject"):
            self._reject(chat_id, normalized)
        elif normalized.startswith("/pause"):
            self._pause(chat_id, normalized)
        elif normalized.startswith("/resume"):
            self._resume(chat_id, normalized)
        elif normalized.startswith("/addfeature"):
            self._add_feature(chat_id, normalized)
        elif normalized.startswith("/addtask"):
            self._add_task(chat_id, normalized)
        elif normalized.startswith("/editapp"):
            self._edit_app(chat_id, normalized)
        elif normalized.startswith("/tasks"):
            self._tasks(chat_id, normalized)
        elif normalized.startswith("/cleartask"):
            self._clear_task(chat_id, normalized)
        elif normalized.startswith("/fixbug"):
            self._fix_bug(chat_id, normalized)
        elif normalized.startswith("/deletejob"):
            self._delete_job(chat_id, normalized)
        elif normalized.startswith("/cancel"):
            self._cancel(chat_id, normalized)
        elif normalized.startswith("/logs"):
            self._logs(chat_id, normalized)
        elif normalized.startswith("/models"):
            self._models(chat_id, normalized)
        elif normalized.startswith("/model"):
            self._model(chat_id, normalized)
        elif normalized.startswith("/cli"):
            self._cli(chat_id, normalized)
        elif normalized.startswith("/setrepo"):
            self._set_repo(chat_id, normalized)
        elif normalized.startswith("/pushgit"):
            self._push_git(chat_id, normalized)
        elif normalized.startswith("/review"):
            self._review(chat_id, normalized)
        elif normalized.startswith("/runandroid"):
            self._run_android(chat_id, normalized)
        elif normalized.startswith("/syncandroid"):
            self._sync_android(chat_id, normalized)
        elif normalized.lower().startswith("/currentlog"):
            self._current_log(chat_id)
        elif normalized.lower().startswith("/endlog"):
            self._end_log(chat_id)
        else:
            self.telegram.send_message(chat_id, "Unknown command. Use /help")

    def _is_replayable_command(self, command: str) -> bool:
        lowered = command.strip().lower()
        return any(lowered.startswith(prefix) for prefix in self.REPLAYABLE_COMMAND_PREFIXES)

    def _send_help(self, chat_id: int) -> None:
        self.telegram.send_message_with_markup(
            chat_id,
            "Commands:\n"
            "/buildbyprompt <appid>|<prompt>\n"
            "/buildapp with one line or multiline brief\n"
            "/newandroid <slug>|<idea>|<target users>|<constraints>\n"
            "/jobs\n"
            "/status <job_id>\n"
            "/progress <job_id>\n"
            "/tail <job_id>\n"
            "/logs <job_id>\n"
            "/models [provider]\n"
            "/model <provider/model>  (or /model default)\n"
            "/cli [opencode|codex]\n"
            "/setrepo <job_id>|<repo_url>\n"
            "/pushgit <job_id>|<summary>\n"
            "/review <job_id> [--strict] [--max <iterations>]\n"
            "/runandroid <job_id> [device_id optional]\n"
            "/syncandroid <job_id>\n"
            "/currentLog\n"
            "/endLog\n"
            "/pause <job_id>\n"
            "/resume <job_id>\n"
            "/addfeature <job_id>|<feature request>\n"
            "/addtask <job_id>|<task to run after current one>\n"
            "/editapp <job_id>|<change request>\n"
            "/tasks <job_id>\n"
            "/cleartask <job_id>\n"
            "/fixbug <job_id>|<bug description>\n"
            "/deletejob <job_id>\n"
            "/approve <job_id>\n"
            "/reject <job_id>|<feedback>\n"
            "/cancel <job_id>\n\n"
            "Tap 'Start Build Wizard' for guided input, or 'Build App Template' for a multiline form.",
            reply_markup=self._menu_keyboard(),
        )

    def _add_feature(self, chat_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|", 1)]
        if len(parts) != 2 or not parts[0] or not parts[1] or not parts[0].isdigit():
            self.telegram.send_message(chat_id, "Usage: /addfeature <job_id>|<feature request>")
            return

        job_id = int(parts[0])
        feature = parts[1]
        updated = self.db.append_job_feature(job_id, feature)
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        self.telegram.send_message_with_markup(
            chat_id,
            f"Added feature to job #{job_id}: {feature}\n"
            f"Job moved to stage `{updated['current_stage']}` with status `{updated['status']}`.",
            reply_markup=self._job_action_markup(job_id, include_approval=False),
        )

    def _add_task(self, chat_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|", 1)]
        if len(parts) != 2 or not parts[0] or not parts[1] or not parts[0].isdigit():
            self.telegram.send_message(chat_id, "Usage: /addtask <job_id>|<task to run after current one>")
            return

        job_id = int(parts[0])
        task_text = parts[1]
        updated, should_activate_now = self.db.add_follow_up_task(job_id, task_text)
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        if should_activate_now:
            activated = self.db.activate_next_task(job_id)
            if activated is None:
                self.telegram.send_message(chat_id, "Job not found")
                return
            self.telegram.send_message_with_markup(
                chat_id,
                f"Task queued and activated for job #{job_id}: {task_text}\n"
                f"Job moved to stage `{activated['current_stage']}` with status `{activated['status']}`.",
                reply_markup=self._job_action_markup(job_id, include_approval=False),
            )
            return

        self.telegram.send_message_with_markup(
            chat_id,
            f"Task queued for job #{job_id}: {task_text}\n"
            "Bot will run it automatically after current task completes.",
            reply_markup=self._job_action_markup(job_id, include_approval=False),
        )

    def _edit_app(self, chat_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|", 1)]
        if len(parts) != 2 or not parts[0] or not parts[1] or not parts[0].isdigit():
            self.telegram.send_message(chat_id, "Usage: /editapp <job_id>|<change request>")
            return

        job_id = int(parts[0])
        task_text = parts[1]
        updated, should_activate_now = self.db.add_follow_up_task(job_id, task_text)
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        if should_activate_now:
            activated = self.db.activate_next_task(job_id)
            if activated is None:
                self.telegram.send_message(chat_id, "Job not found")
                return
            self.telegram.send_message_with_markup(
                chat_id,
                f"App edit task queued and activated for job #{job_id}: {task_text}\n"
                f"Job moved to stage `{activated['current_stage']}` with status `{activated['status']}`.",
                reply_markup=self._job_action_markup(job_id, include_approval=False),
            )
            return

        self.telegram.send_message_with_markup(
            chat_id,
            f"App edit task queued for job #{job_id}: {task_text}\n"
            "Bot will run it automatically after current task completes.",
            reply_markup=self._job_action_markup(job_id, include_approval=False),
        )

    def _clear_task(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /cleartask <job_id>")
            return

        updated = self.db.clear_follow_up_tasks(job["id"])
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        self.telegram.send_message_with_markup(
            chat_id,
            f"Cleared active and pending follow-up tasks for job #{job['id']}.",
            reply_markup=self._job_action_markup(job["id"], include_approval=updated["waiting_for_approval"]),
        )

    def _fix_bug(self, chat_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|", 1)]
        if len(parts) != 2 or not parts[0] or not parts[1] or not parts[0].isdigit():
            self.telegram.send_message(chat_id, "Usage: /fixbug <job_id>|<bug description>")
            return

        job_id = int(parts[0])
        bug_text = parts[1]
        updated, should_activate_now = self.db.add_follow_up_task(job_id, bug_text, tag="fixbug")
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        if should_activate_now:
            activated = self.db.activate_next_task(job_id)
            if activated is None:
                self.telegram.send_message(chat_id, "Job not found")
                return
            self.telegram.send_message_with_markup(
                chat_id,
                f"Bug-fix task queued and activated for job #{job_id}: {bug_text}\n"
                f"Job moved to stage `{activated['current_stage']}` with status `{activated['status']}`.",
                reply_markup=self._job_action_markup(job_id, include_approval=False),
            )
            return

        self.telegram.send_message_with_markup(
            chat_id,
            f"Bug-fix task queued for job #{job_id}: {bug_text}\n"
            "Bot will run it automatically after current task completes.",
            reply_markup=self._job_action_markup(job_id, include_approval=False),
        )

    def _delete_job(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /deletejob <job_id>")
            return

        if job["status"] == "running" or (self.worker is not None and self.worker.active_progress(job["id"]) is not None):
            self.telegram.send_message(
                chat_id,
                f"Job #{job['id']} is still active. Cancel it first, wait until it stops, then run /deletejob {job['id']}.",
            )
            return

        deleted = self.db.delete_job(job["id"])
        if deleted is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        self.telegram.send_message(
            chat_id,
            f"Job #{deleted['id']} deleted from database.\nWorkspace kept: `{deleted['workspace_path']}`",
        )

    def _tasks(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /tasks <job_id>")
            return

        context = job.get("context", {})
        active = ""
        pending: list[str] = []
        history: list[str] = []
        if isinstance(context, dict):
            active = str(context.get("active_task") or "").strip()
            raw_pending = context.get("pending_tasks")
            raw_history = context.get("task_history")
            if isinstance(raw_pending, list):
                pending = [str(item).strip() for item in raw_pending if str(item).strip()]
            if isinstance(raw_history, list):
                history = [str(item).strip() for item in raw_history if str(item).strip()]

        lines = [f"Job #{job['id']} task queue"]
        lines.append(f"Status: {job['status']} | Stage: {job['current_stage']}")
        lines.append(f"Active: {active if active else '(none)'}")
        if pending:
            lines.append("Pending:")
            lines.extend(f"- {item}" for item in pending[:10])
            if len(pending) > 10:
                lines.append(f"- ... and {len(pending) - 10} more")
        else:
            lines.append("Pending: (none)")
        if history:
            lines.append("Recent done:")
            lines.extend(f"- {item}" for item in history[-5:])

        self.telegram.send_message_with_markup(
            chat_id,
            "\n".join(lines),
            reply_markup=self._job_action_markup(job["id"], include_approval=job["waiting_for_approval"]),
        )

    def _current_log(self, chat_id: int) -> None:
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return
        self.worker.set_live_cli_logs(chat_id, True)
        snapshot = self.worker.log_mode_snapshot(chat_id)
        self.telegram.send_message(
            chat_id,
            "Current log mode\n"
            f"Live CLI logs: {'ON' if snapshot['enabled'] else 'OFF'}\n"
            f"CLI: `{snapshot['cli']}`\n"
            f"Binary: `{snapshot['binary']}`\n"
            f"Timeout: {snapshot['timeout_seconds']}s\n"
            f"Restart attempts: {snapshot['restart_attempts']}\n"
            f"Min request interval: {snapshot['min_request_interval_seconds']}s\n"
            "Use /endLog to stop live CLI logs.",
        )

    def _model(self, chat_id: int, text: str) -> None:
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return
        _, _, raw = text.partition(" ")
        model = raw.strip()
        if not model:
            current = self.worker.selected_model() or "default"
            self.telegram.send_message(chat_id, f"Current model: `{current}`\nUse /models to pick or /model <provider/model> to set.")
            return
        if model.lower() in {"default", "reset", "clear"}:
            self.worker.set_model(None)
            self.telegram.send_message(chat_id, "Model reset to default.")
            return
        self.worker.set_model(model)
        stored_model = self.worker.selected_model()
        pinned_jobs = 0
        for candidate in self.db.list_jobs(limit=100):
            if candidate["status"] in {"completed", "cancelled", "failed"}:
                continue
            context = candidate.get("context", {})
            if stored_model:
                context["selected_model"] = stored_model
            else:
                context.pop("selected_model", None)
            self.db.set_job_context(candidate["id"], context)
            pinned_jobs += 1
        self.telegram.send_message(chat_id, f"Model set to `{stored_model or 'default'}`\nPinned on active jobs: {pinned_jobs}")

    def _cli(self, chat_id: int, text: str) -> None:
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return
        _, _, raw = text.partition(" ")
        choice = raw.strip().lower()
        current = Path(self.worker.cli_binary()).stem.lower()
        if not choice:
            self.telegram.send_message(
                chat_id,
                f"Current CLI: `{current}`\n"
                "Use `/cli opencode` or `/cli codex` to switch.",
            )
            return
        if choice not in {"opencode", "codex"}:
            self.telegram.send_message(chat_id, "Usage: /cli [opencode|codex]")
            return
        try:
            resolved = self.worker.set_cli_binary(choice)
        except Exception as exc:  # noqa: BLE001
            self.telegram.send_message(chat_id, f"Failed to switch CLI: {exc}")
            return
        self.telegram.send_message(
            chat_id,
            f"CLI switched to `{Path(resolved).stem.lower()}`\nBinary: `{resolved}`\nModel: `{self.worker.selected_model() or self.worker._default_model_for_cli() or 'default'}`",
        )

    def _set_repo(self, chat_id: int, text: str) -> None:
        parts = text.split(" ", 1)
        if len(parts) < 2:
            self.telegram.send_message(chat_id, "Usage: /setrepo <job_id>|<repo_url>")
            return
        raw = parts[1].strip()
        if "|" in raw:
            job_raw, repo_url = [part.strip() for part in raw.split("|", 1)]
        else:
            split = raw.split(maxsplit=1)
            if len(split) < 2:
                self.telegram.send_message(chat_id, "Usage: /setrepo <job_id>|<repo_url>")
                return
            job_raw, repo_url = split[0].strip(), split[1].strip()

        if not job_raw.isdigit() or not repo_url:
            self.telegram.send_message(chat_id, "Usage: /setrepo <job_id>|<repo_url>")
            return

        job = self.db.get_job(int(job_raw))
        if job is None:
            self.telegram.send_message(chat_id, "Job not found")
            return
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return

        workspace = Path(job["workspace_path"])
        try:
            git_root = self.worker._resolve_git_target_root(job, workspace)
            existing = run_git(git_root, "remote", "get-url", "origin")
            if existing.returncode == 0:
                result = run_git(git_root, "remote", "set-url", "origin", repo_url)
            else:
                result = run_git(git_root, "remote", "add", "origin", repo_url)
            if result.returncode != 0:
                raise RuntimeError((result.stderr or result.stdout).strip() or "git remote update failed")
        except Exception as exc:  # noqa: BLE001
            self.telegram.send_message(chat_id, f"setrepo failed for job #{job['id']}\n{exc}")
            return

        self.telegram.send_message(chat_id, f"Repo linked for job #{job['id']}\nroot: `{git_root}`\norigin: `{repo_url}`")

    def _push_git(self, chat_id: int, text: str) -> None:
        parts = text.split(" ", 1)
        if len(parts) < 2:
            self.telegram.send_message(chat_id, "Usage: /pushgit <job_id>|<summary>")
            return
        raw = parts[1].strip()
        if "|" in raw:
            job_raw, summary = [part.strip() for part in raw.split("|", 1)]
        else:
            split = raw.split(maxsplit=1)
            if len(split) < 2:
                self.telegram.send_message(chat_id, "Usage: /pushgit <job_id>|<summary>")
                return
            job_raw, summary = split[0].strip(), split[1].strip()

        if not job_raw.isdigit() or not summary:
            self.telegram.send_message(chat_id, "Usage: /pushgit <job_id>|<summary>")
            return

        job = self.db.get_job(int(job_raw))
        if job is None:
            self.telegram.send_message(chat_id, "Job not found")
            return
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return

        workspace = Path(job["workspace_path"])
        try:
            git_root = self.worker._resolve_git_target_root(job, workspace)
            remote = run_git(git_root, "remote", "get-url", "origin")
            if remote.returncode != 0:
                self.telegram.send_message(chat_id, "No remote origin found. Run /setrepo <job_id>|<repo_url> first.")
                return

            status = run_git(git_root, "status", "--short")
            if status.returncode != 0:
                raise RuntimeError((status.stderr or status.stdout).strip() or "git status failed")
            changed_lines = [line.strip() for line in (status.stdout or "").splitlines() if line.strip()]
            if not changed_lines:
                self.telegram.send_message(chat_id, f"No local changes to commit for job #{job['id']}\nroot: `{git_root}`")
                return

            add_result = run_git(git_root, "add", ".")
            if add_result.returncode != 0:
                raise RuntimeError((add_result.stderr or add_result.stdout).strip() or "git add failed")

            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            message = f"{timestamp} | {summary}"
            commit_result = run_git(git_root, "commit", "-m", message)
            if commit_result.returncode != 0:
                raise RuntimeError((commit_result.stderr or commit_result.stdout).strip() or "git commit failed")

            branch_result = run_git(git_root, "branch", "--show-current")
            if branch_result.returncode != 0:
                raise RuntimeError((branch_result.stderr or branch_result.stdout).strip() or "git branch failed")
            branch = (branch_result.stdout or "").strip() or f"job-{job['id']}"

            push_result = run_git(git_root, "push", "-u", "origin", branch)
            if push_result.returncode != 0:
                raise RuntimeError((push_result.stderr or push_result.stdout).strip() or "git push failed")

            head_result = run_git(git_root, "rev-parse", "HEAD")
            commit_hash = (head_result.stdout or "").strip() if head_result.returncode == 0 else ""
        except Exception as exc:  # noqa: BLE001
            self.telegram.send_message(chat_id, f"pushgit failed for job #{job['id']}\n{exc}")
            return

        self.telegram.send_message(
            chat_id,
            f"pushgit done for job #{job['id']}\n"
            f"Root: `{git_root}`\n"
            f"Branch: `{branch}`\n"
            f"Commit: `{commit_hash or 'created'}`\n"
            f"Message: `{message}`",
        )

    def _models(self, chat_id: int, text: str) -> None:
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return
        cli_name = Path(self.worker.cli_binary()).stem.lower()
        if cli_name == "codex":
            self._model_picker_cache[chat_id] = {"models": CODEX_HARDCODED_MODELS, "provider": "codex(hardcoded)"}
            self._send_models_page(chat_id, page=0)
            return
        _, _, raw = text.partition(" ")
        provider = raw.strip() or None
        try:
            result = self.worker.opencode.list_models(provider=provider)
        except Exception as exc:  # noqa: BLE001
            self.telegram.send_message(chat_id, f"Failed to query models: {exc}")
            return

        output = (result.stdout or "").strip()
        model_ids = self._extract_model_ids(output)
        if result.returncode != 0 or not model_ids:
            err = (result.stderr or output or "No output").strip()
            self.telegram.send_message(chat_id, f"Could not load models from OpenCode CLI\n{err[:700]}")
            return

        self._model_picker_cache[chat_id] = {"models": model_ids, "provider": provider or "all"}
        self._send_models_page(chat_id, page=0)

    def _extract_model_ids(self, output: str) -> list[str]:
        cleaned = re.sub(r"\x1b\[[0-9;]*m", "", output or "")
        found: list[str] = []
        seen: set[str] = set()
        for match in re.finditer(r"\b[a-z0-9][a-z0-9_-]*/[a-z0-9][a-z0-9_.-]*\b", cleaned, flags=re.IGNORECASE):
            model = match.group(0).strip()
            key = model.lower()
            if key in seen:
                continue
            seen.add(key)
            found.append(model)
        return found

    def _send_models_page(self, chat_id: int, page: int) -> None:
        cache = self._model_picker_cache.get(chat_id)
        if not cache:
            self.telegram.send_message(chat_id, "Model picker is empty. Use /models first.")
            return
        models = list(cache.get("models") or [])
        if not models:
            self.telegram.send_message(chat_id, "No models available.")
            return

        per_page = 8
        total_pages = max(1, (len(models) + per_page - 1) // per_page)
        safe_page = max(0, min(page, total_pages - 1))
        start = safe_page * per_page
        end = start + per_page
        current = self.worker.selected_model() if self.worker is not None else None

        rows: list[list[dict[str, str]]] = []
        for idx in range(start, min(end, len(models))):
            model = models[idx]
            label = model if len(model) <= 36 else f"{model[:33]}..."
            if current and model.lower() == current.lower():
                label = f"{label} [current]"
            rows.append([{"text": label, "callback_data": f"model:pick:{idx}"}])

        if total_pages > 1:
            nav: list[dict[str, str]] = []
            if safe_page > 0:
                nav.append({"text": "Prev", "callback_data": f"model:page:{safe_page - 1}"})
            if safe_page < total_pages - 1:
                nav.append({"text": "Next", "callback_data": f"model:page:{safe_page + 1}"})
            if nav:
                rows.append(nav)

        provider = cache.get("provider", "all")
        self.telegram.send_message_with_markup(
            chat_id,
            f"OpenCode models ({provider})\nPage {safe_page + 1}/{total_pages}\nCurrent: `{current or 'default'}`",
            reply_markup={"inline_keyboard": rows},
        )

    def _end_log(self, chat_id: int) -> None:
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return
        was_enabled = self.worker.set_live_cli_logs(chat_id, False)
        if was_enabled:
            self.telegram.send_message(chat_id, "Live CLI logs are now OFF for this chat.")
            return
        self.telegram.send_message(chat_id, "Live CLI logs were already OFF for this chat.")

    def _run_android(self, chat_id: int, text: str) -> None:
        parts = text.split()
        if len(parts) < 2 or not parts[1].isdigit():
            self.telegram.send_message(chat_id, "Usage: /runandroid <job_id> [device_id]")
            return
        job_id = int(parts[1])
        device_id = parts[2].strip() if len(parts) >= 3 else ""
        job = self.db.get_job(job_id)
        if job is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        with self._runandroid_lock:
            active = self._runandroid_threads.get(job_id)
            if active is not None and active.is_alive():
                self.telegram.send_message(chat_id, f"runandroid already running for job #{job_id}")
                return
            thread = threading.Thread(
                target=self._run_android_background,
                args=(chat_id, job, device_id),
                name=f"runandroid-{job_id}",
                daemon=True,
            )
            self._runandroid_threads[job_id] = thread
            thread.start()

        self.telegram.send_message(chat_id, f"runandroid queued in background for job #{job_id}")

    def _run_android_background(self, chat_id: int, job: dict[str, Any], device_id: str) -> None:
        job_id = int(job["id"])
        try:
            workspace = Path(job["workspace_path"])
            if not (workspace / "gradlew.bat").exists():
                self.telegram.send_message(chat_id, f"Workspace `{workspace}` is not an Android Gradle project")
                return

            install_command = ".\\gradlew.bat installDebug"
            if device_id:
                install_command += f" -Pandroid.testInstrumentationRunnerArguments.notAnnotation={device_id}"
            self.telegram.send_message(chat_id, f"Running install for job #{job_id}\n`{install_command}`")
            install_result = run_powershell(install_command, workspace, timeout_seconds=900)
            install_out = (install_result.stdout or "").strip()
            install_err = (install_result.stderr or "").strip()
            if install_result.returncode != 0:
                self.telegram.send_message(
                    chat_id,
                    f"runandroid failed (installDebug)\nExit: {install_result.returncode}\nSTDERR:\n{install_err[:1500]}\nSTDOUT:\n{install_out[:1500]}",
                )
                return

            package_name = self.worker._determine_existing_package_name(workspace) if self.worker is not None else None
            launched = "Installed debug build."
            if package_name:
                launch_cmd = f"adb shell monkey -p {package_name} -c android.intent.category.LAUNCHER 1"
                launch_result = run_powershell(launch_cmd, workspace, timeout_seconds=45)
                if launch_result.returncode == 0:
                    launched = f"Installed and launched `{package_name}`"
                else:
                    launched = f"Installed, but launch failed for `{package_name}`"

            self.telegram.send_message(chat_id, f"runandroid done for job #{job_id}\n{launched}")
        finally:
            with self._runandroid_lock:
                current = self._runandroid_threads.get(job_id)
                if current is threading.current_thread():
                    self._runandroid_threads.pop(job_id, None)

    def _sync_android(self, chat_id: int, text: str) -> None:
        parts = text.split()
        if len(parts) < 2 or not parts[1].isdigit():
            self.telegram.send_message(chat_id, "Usage: /syncandroid <job_id>")
            return
        job_id = int(parts[1])
        job = self.db.get_job(job_id)
        if job is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        workspace = Path(job["workspace_path"])
        if not (workspace / "gradlew.bat").exists():
            self.telegram.send_message(chat_id, f"Workspace `{workspace}` is not an Android Gradle project")
            return

        sync_command = ".\\gradlew.bat help"
        self.telegram.send_message(chat_id, f"Running sync check for job #{job_id}\n`{sync_command}`")
        result = run_powershell(sync_command, workspace, timeout_seconds=300)
        out = (result.stdout or "").strip()
        err = (result.stderr or "").strip()
        if result.returncode != 0:
            self.telegram.send_message(
                chat_id,
                f"syncandroid failed\nExit: {result.returncode}\nSTDERR:\n{err[:1800]}\nSTDOUT:\n{out[:1800]}",
            )
            return
        self.telegram.send_message(chat_id, f"syncandroid done for job #{job_id}\n{out[-1200:] if out else 'Gradle command completed.'}")

    def _review(self, chat_id: int, text: str) -> None:
        parts = text.split()
        if len(parts) < 2 or not parts[1].isdigit():
            self.telegram.send_message(chat_id, "Usage: /review <job_id> [--strict] [--max <iterations>]")
            return
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
            return

        job_id = int(parts[1])
        strict_mode = any(part.strip().lower() == "--strict" for part in parts[2:])
        max_iterations_override: int | None = None
        for idx, part in enumerate(parts[2:], start=2):
            if part.strip().lower() != "--max":
                continue
            if idx + 1 >= len(parts):
                self.telegram.send_message(chat_id, "Usage: /review <job_id> [--strict] [--max <iterations>]")
                return
            if not parts[idx + 1].isdigit():
                self.telegram.send_message(chat_id, "`--max` must be a positive integer")
                return
            max_iterations_override = max(1, int(parts[idx + 1]))
            break

        job = self.db.get_job(job_id)
        if job is None:
            self.telegram.send_message(chat_id, "Job not found")
            return

        context = job.get("context", {})
        stages = context.get("stages", {})
        if not stages.get("idea") or not stages.get("plan") or not stages.get("design"):
            self.telegram.send_message(chat_id, f"Job #{job_id} does not have enough completed stages for review yet.")
            return

        with self._manual_review_lock:
            active = self._manual_review_threads.get(job_id)
            if active is not None and active.is_alive():
                self.telegram.send_message(chat_id, f"Manual review is already running for job #{job_id}.")
                return

            thread = threading.Thread(
                target=self._run_manual_review_background,
                args=(chat_id, job, strict_mode, max_iterations_override),
                name=f"manual-review-{job_id}",
                daemon=True,
            )
            self._manual_review_threads[job_id] = thread
            thread.start()

        self.telegram.send_message(
            chat_id,
            f"Running manual review loop for job #{job_id}...\n"
            f"strict: {'ON' if strict_mode else 'OFF'}\n"
            f"max iterations: {max_iterations_override or self.settings.max_review_loops}",
        )

    def _run_manual_review_background(
        self,
        chat_id: int,
        job: dict[str, Any],
        strict_mode: bool,
        max_iterations_override: int | None,
    ) -> None:
        job_id = int(job["id"])
        try:
            stage_result = self._run_manual_review_loop(
                chat_id,
                job,
                strict_mode=strict_mode,
                max_iterations_override=max_iterations_override,
            )
            if stage_result.get("status") == "failed":
                self.telegram.send_message(chat_id, f"Manual review loop failed for job #{job_id}\n{stage_result.get('error', 'unknown error')}")
                return

            final_review = stage_result.get("final_review", {})
            summary = compact_json_preview(final_review if isinstance(final_review, dict) else {"summary": "review finished"})
            self.telegram.send_message(
                chat_id,
                f"Manual review loop completed for job #{job_id}\n"
                f"Iterations: {stage_result.get('iterations', 0)}\n"
                f"Decision: {stage_result.get('decision', 'unknown')}\n"
                f"Strict mode: {'ON' if strict_mode else 'OFF'}\n"
                f"{summary}",
            )
        except Exception as exc:  # noqa: BLE001
            self.telegram.send_message(chat_id, f"Manual review thread crashed for job #{job_id}\n{exc}")
        finally:
            with self._manual_review_lock:
                current = self._manual_review_threads.get(job_id)
                if current is threading.current_thread():
                    self._manual_review_threads.pop(job_id, None)

    def _run_manual_review_loop(
        self,
        chat_id: int,
        job: dict[str, Any],
        *,
        strict_mode: bool,
        max_iterations_override: int | None,
    ) -> dict[str, Any]:
        if self.worker is None:
            return {"status": "failed", "error": "worker is not available"}

        workspace = Path(job["workspace_path"])
        max_iterations = max(1, max_iterations_override or self.settings.max_review_loops)
        iterations = 0
        final_review: dict[str, Any] = {}

        try:
            while iterations < max_iterations:
                iterations += 1
                self.telegram.send_message(chat_id, f"Review loop job #{job['id']}: iteration {iterations}/{max_iterations} started")
                current_job = self.db.get_job(job["id"])
                if current_job is None:
                    return {"status": "failed", "error": f"job {job['id']} not found"}

                context = current_job["context"]
                review_prompt_text = review_prompt(current_job, context)
                if strict_mode:
                    review_prompt_text += (
                        "\n\nSTRICT REVIEW MODE:\n"
                        "- Be conservative: choose `complete` only when there are no meaningful UX/flow/function gaps.\n"
                        "- Explicitly enforce system flow quality: Splash -> Intro -> Main App, plus Settings/Language/Exit.\n"
                        "- If any missing/inconsistent behavior remains, choose `iterate` and provide concrete action_items."
                    )

                model_for_job = self.worker._effective_model_for_job(current_job)
                review_payload = self.worker.opencode.run_json_prompt_with_progress(
                    workdir=workspace,
                    prompt=review_prompt_text,
                    model=model_for_job,
                    timeout_seconds=max(self.settings.open_code_timeout_seconds, self.worker.REVIEW_STAGE_MIN_TIMEOUT_SECONDS),
                    on_progress=lambda message: self.worker._report_progress(current_job, "manual-review", f"review: {message}"),
                    on_watchdog=lambda message: self.worker._report_watchdog(current_job, "manual-review", f"review: {message}"),
                    on_cli_output=lambda stream, line: self.worker._report_cli_output(current_job, "manual-review", stream, line),
                )

                decision = str(review_payload.get("decision", "iterate")).strip().lower()
                next_stage = str(review_payload.get("next_stage", "code")).strip().lower()
                if decision not in {"complete", "iterate"}:
                    decision = "iterate"
                if next_stage not in {"plan", "design", "code", "verify", "complete"}:
                    next_stage = "code"

                review_payload["review_count"] = iterations
                review_payload["decision"] = decision
                review_payload["next_stage"] = next_stage
                final_review = review_payload
                self.db.upsert_stage_context(current_job["id"], "review", review_payload)
                self.db.add_event(current_job["id"], "review", "info", f"Manual review iteration {iterations} completed", review_payload)
                self.telegram.send_message(
                    chat_id,
                    f"Review loop job #{job['id']}: iteration {iterations} decision `{decision}` -> next `{next_stage}`",
                )

                if decision == "complete" or next_stage == "complete":
                    self.telegram.send_message(chat_id, f"Review loop job #{job['id']}: accepted as complete at iteration {iterations}")
                    return {
                        "status": "completed",
                        "iterations": iterations,
                        "decision": "complete",
                        "final_review": final_review,
                    }

                if next_stage in {"plan", "design"}:
                    refine_prompt = (
                        f"Manual review action items:\n{json.dumps(review_payload.get('action_items', []), ensure_ascii=True)}\n"
                        f"Issues to fix:\n{json.dumps(review_payload.get('issues', []), ensure_ascii=True)}\n"
                        "Update the implementation by making concrete code changes so that these review items are resolved.\n"
                        "Return strict JSON only with schema: {\"summary\":\"...\",\"files_touched\":[\"...\"],\"features_completed\":[\"...\"],\"follow_up_notes\":[\"...\"]}"
                    )
                elif next_stage == "verify":
                    refine_prompt = (
                        f"Manual review requested verification-focused fixes.\n"
                        f"Issues to fix:\n{json.dumps(review_payload.get('issues', []), ensure_ascii=True)}\n"
                        "Apply code changes required to pass verification and align behavior/UI with requested flow.\n"
                        "Return strict JSON only with schema: {\"summary\":\"...\",\"files_touched\":[\"...\"],\"features_completed\":[\"...\"],\"follow_up_notes\":[\"...\"]}"
                    )
                else:
                    refine_prompt = (
                        "Manual review requested code refinements for app quality and flow compliance.\n"
                        f"Action items:\n{json.dumps(review_payload.get('action_items', []), ensure_ascii=True)}\n"
                        f"UI/flow issues:\n{json.dumps(review_payload.get('issues', []), ensure_ascii=True)}\n"
                        "Implement fixes now in Kotlin Compose code.\n"
                        "Return strict JSON only with schema: {\"summary\":\"...\",\"files_touched\":[\"...\"],\"features_completed\":[\"...\"],\"follow_up_notes\":[\"...\"]}"
                    )

                if strict_mode:
                    refine_prompt += (
                        "\nSTRICT MODE ACCEPTANCE:\n"
                        "- Keep iterating until app flow and UI are coherent and complete.\n"
                        "- Do not skip fixes for onboarding, settings, language selection, or exit path."
                    )

                code_payload = self.worker.opencode.run_json_prompt_with_progress(
                    workdir=workspace,
                    prompt=refine_prompt,
                    model=model_for_job,
                    timeout_seconds=max(self.settings.open_code_timeout_seconds, self.worker.CODE_STAGE_MIN_TIMEOUT_SECONDS),
                    on_progress=lambda message: self.worker._report_progress(current_job, "manual-review", f"fix: {message}"),
                    on_watchdog=lambda message: self.worker._report_watchdog(current_job, "manual-review", f"fix: {message}"),
                    on_cli_output=lambda stream, line: self.worker._report_cli_output(current_job, "manual-review", stream, line),
                )

                git_result = self.worker._commit_workspace_changes(
                    current_job,
                    workspace,
                    "manual-review",
                    f"agent(job-{current_job['id']}): apply manual review fixes iteration {iterations}",
                )
                if git_result is not None:
                    code_payload["git"] = git_result

                self.db.upsert_stage_context(current_job["id"], "code", code_payload)
                self.db.add_event(current_job["id"], "manual-review", "warning", f"Manual review fixes applied iteration {iterations}", code_payload)
                self.telegram.send_message(chat_id, f"Review loop job #{job['id']}: fixes applied for iteration {iterations}, continuing...")

            return {
                "status": "completed",
                "iterations": iterations,
                "decision": "iterate",
                "final_review": final_review,
            }
        except Exception as exc:  # noqa: BLE001
            return {"status": "failed", "error": str(exc), "iterations": iterations, "final_review": final_review}

    def _send_buildapp_template(self, chat_id: int) -> None:
        self.telegram.send_message_with_markup(
            chat_id,
            "/buildapp\n"
            "slug: coffee-social\n"
            "idea: social app for coffee lovers\n"
            "style: warm modern editorial\n"
            "font: minimal geometric sans\n"
            "features:\n"
            "- post cafe reviews\n"
            "- map nearby cafes\n"
            "- save favorites\n"
            "requirements: support offline cache, keep onboarding under 2 steps, prioritize fast startup\n"
            "target_users: young city coffee drinkers\n"
            "constraints: android only, MVP first, offline-friendly where possible\n"
            "app_id: com.example.coffeesocial",
            reply_markup=self._menu_keyboard(),
        )

    def _build_by_prompt(self, chat_id: int, user_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|", 1)]
        if len(parts) != 2 or not parts[0] or not parts[1]:
            self.telegram.send_message(chat_id, "Usage: /buildbyprompt <appid>|<prompt>")
            return

        raw_app_id = parts[0]
        prompt = parts[1]

        if not raw_app_id.startswith("com."):
            raw_app_id = f"com.app.{raw_app_id}"

        slug = slugify(raw_app_id.split(".")[-1])
        if not slug:
            slug = slugify(raw_app_id)

        parsed = {
            "slug": slug,
            "idea": prompt,
            "style": "modern minimal premium",
            "font": "clean geometric sans",
            "features": prompt,
            "target_users": "General users",
            "constraints": "android only, MVP first, offline-friendly where possible",
            "app_id": raw_app_id,
        }
        self._queue_build_job(chat_id, user_id, parsed)

    def _build_app(self, chat_id: int, user_id: int, text: str) -> None:
        parsed = self._parse_build_app_args(text)
        if parsed is None:
            self.telegram.send_message(
                chat_id,
                "Usage:\n"
                "/buildapp <slug>|<idea>|<style>|<font>|<features>|<requirements>|<target users>|<constraints>|<app_id optional>\n\n"
                "Or multiline:\n"
                "/buildapp\n"
                "slug: your-app\n"
                "idea: ...\n"
                "style: ...\n"
                "font: ...\n"
                "features: ...  or bullet list under features:\n"
                "- first feature\n"
                "- second feature\n"
                "requirements: ...\n"
                "target_users: ...\n"
                "constraints: ...\n"
                "app_id: com.example.yourapp  (optional)",
            )
            return

        self._queue_build_job(chat_id, user_id, parsed)

    def _queue_build_job(self, chat_id: int, user_id: int, parsed: dict[str, str]) -> None:
        self.db.clear_wizard_state(chat_id)

        slug = slugify(parsed["slug"])
        idea = parsed["idea"]
        style = parsed["style"]
        font = parsed["font"]
        features = parsed["features"]
        requirements = parsed.get("requirements", "").strip()
        target_users = parsed["target_users"]
        constraints = parsed["constraints"]
        app_id = parsed.get("app_id", "").strip()

        request_text = (
            f"App idea: {idea}\n"
            f"Preferred visual style: {style}\n"
            f"Preferred font direction: {font}\n"
            f"Requested features: {features}"
        )
        if requirements:
            request_text += f"\nAdditional product requirements: {requirements}"
        if app_id:
            request_text += f"\nRequested Android applicationId: {app_id}"
        constraints_text = (
            f"{constraints}\n"
            f"Design style must align with: {style}\n"
            f"Typography should align with: {font}\n"
            f"Feature priorities: {features}"
        )
        if requirements:
            constraints_text += f"\nMust-have requirements: {requirements}"
        if app_id:
            constraints_text += f"\nUse Android applicationId/package name: {app_id}"

        provisional_workspace = self.settings.projects_root / slug
        job_id = self.db.create_job(
            slug=slug,
            request_text=request_text,
            target_users=target_users,
            constraints_text=constraints_text,
            workspace_path=str(provisional_workspace),
            chat_id=chat_id,
            created_by=user_id,
        )
        workspace = self._allocate_workspace_for_job(slug, job_id)
        workspace.mkdir(parents=True, exist_ok=True)
        created_job = self.db.update_job_workspace(job_id, str(workspace))
        if self.worker is not None and created_job is not None:
            self.worker._ensure_workspace_docs(created_job, created_job.get("context", {}))
        if self.worker is not None:
            selected = self.worker.selected_model()
            if selected:
                if created_job is not None:
                    context = created_job.get("context", {})
                    context["selected_model"] = selected
                    self.db.set_job_context(job_id, context)
        self.telegram.send_message(
            chat_id,
            f"Job #{job_id} queued for autonomous build\n"
            f"Flow: idea -> plan -> design -> code -> verify -> review -> repeat until done\n"
            f"Workspace: `{workspace}`",
        )
        self.telegram.send_message_with_markup(
            chat_id,
            f"Job #{job_id} actions",
            reply_markup=self._job_action_markup(job_id, include_approval=False),
        )

    def _parse_build_app_args(self, text: str) -> dict[str, str] | None:
        _, _, raw_args = text.partition(" ")
        raw_args = raw_args.strip()
        if not raw_args:
            lines = text.splitlines()[1:]
            return self._parse_build_app_lines(lines)
        if "|" in raw_args:
            parts = [part.strip() for part in raw_args.split("|")]
            if len(parts) < 7 or not parts[0] or not parts[1]:
                return None
            if len(parts) >= 8:
                parsed = {
                    "slug": parts[0],
                    "idea": parts[1],
                    "style": parts[2],
                    "font": parts[3],
                    "features": parts[4],
                    "requirements": parts[5],
                    "target_users": parts[6],
                    "constraints": parts[7],
                }
                if len(parts) >= 9 and parts[8]:
                    parsed["app_id"] = parts[8]
                return parsed
            return {
                "slug": parts[0],
                "idea": parts[1],
                "style": parts[2],
                "font": parts[3],
                "features": parts[4],
                "target_users": parts[5],
                "constraints": parts[6],
            }

        lines = text.splitlines()[1:] if "\n" in text else []
        return self._parse_build_app_lines(lines)

    def _parse_build_app_lines(self, lines: list[str]) -> dict[str, str] | None:
        values: dict[str, str] = {}
        aliases = {
            "slug": "slug",
            "idea": "idea",
            "style": "style",
            "font": "font",
            "features": "features",
            "requirements": "requirements",
            "requirement": "requirements",
            "target_users": "target_users",
            "target users": "target_users",
            "users": "target_users",
            "constraints": "constraints",
            "app_id": "app_id",
            "appid": "app_id",
            "application_id": "app_id",
            "application id": "app_id",
            "package_name": "app_id",
            "package name": "app_id",
        }
        multiline_key: str | None = None
        multiline_items: list[str] = []

        def flush_multiline() -> None:
            nonlocal multiline_key, multiline_items
            if multiline_key and multiline_items:
                values[multiline_key] = ", ".join(multiline_items)
            multiline_key = None
            multiline_items = []

        for line in lines:
            line = line.strip()
            if not line:
                flush_multiline()
                continue
            if multiline_key and line.startswith(("- ", "* ")):
                item = line[2:].strip()
                if item:
                    multiline_items.append(item)
                continue
            if ":" not in line:
                flush_multiline()
                continue

            flush_multiline()
            key, value = line.split(":", 1)
            normalized = aliases.get(key.strip().lower())
            if normalized in {"features", "requirements"} and not value.strip():
                multiline_key = normalized
                multiline_items = []
                continue
            if normalized and value.strip():
                values[normalized] = value.strip()

        required = ["slug", "idea", "style", "font", "features", "target_users", "constraints"]
        flush_multiline()
        if any(not values.get(field) for field in required):
            return None
        return values

    def _allocate_workspace_for_job(self, slug: str, job_id: int) -> Path:
        base_workspace = self.settings.projects_root / slug
        if not base_workspace.exists():
            return base_workspace
        return self.settings.projects_root / f"{slug}-{job_id}"

    def _create_job(self, chat_id: int, user_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|")]
        if len(parts) < 4 or not all(parts[:3]):
            self.telegram.send_message(chat_id, "Usage: /newandroid <slug>|<idea>|<target users>|<constraints>")
            return
        slug = slugify(parts[0])
        provisional_workspace = self.settings.projects_root / slug
        job_id = self.db.create_job(
            slug=slug,
            request_text=parts[1],
            target_users=parts[2],
            constraints_text=parts[3],
            workspace_path=str(provisional_workspace),
            chat_id=chat_id,
            created_by=user_id,
        )
        workspace = self._allocate_workspace_for_job(slug, job_id)
        workspace.mkdir(parents=True, exist_ok=True)
        created_job = self.db.update_job_workspace(job_id, str(workspace))
        if self.worker is not None and created_job is not None:
            self.worker._ensure_workspace_docs(created_job, created_job.get("context", {}))
        self.telegram.send_message(chat_id, f"Job #{job_id} queued\nWorkspace: `{workspace}`")

    def _status(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /status <job_id>")
            return
        self._send_job_status(chat_id, job)

    def _jobs(self, chat_id: int) -> None:
        jobs = self.db.list_jobs(limit=12)
        if not jobs:
            self.telegram.send_message(chat_id, "No jobs found")
            return

        lines = ["Recent jobs"]
        for job in jobs:
            marker = ""
            if job["status"] == "running":
                marker = " [running]"
            elif job["status"] == "paused":
                marker = " [paused]"
            lines.append(f"#{job['id']} {job['slug']} | {job['status']} | {job['current_stage']}{marker}")

        inline_rows: list[list[dict[str, str]]] = []
        for job in jobs[:6]:
            inline_rows.append(
                [
                    {"text": f"#{job['id']} Progress", "callback_data": f"job:progress:{job['id']}"},
                    {"text": f"#{job['id']} Pause", "callback_data": f"job:pause:{job['id']}"},
                    {"text": f"#{job['id']} Resume", "callback_data": f"job:resume:{job['id']}"},
                ]
            )

        self.telegram.send_message_with_markup(
            chat_id,
            "\n".join(lines),
            reply_markup={"inline_keyboard": inline_rows} if inline_rows else None,
        )

    def _send_job_status(self, chat_id: int, job: dict[str, Any]) -> None:
        stages = ", ".join(job["context"].get("stages", {}).keys()) or "none"
        waiting = "yes" if job["waiting_for_approval"] else "no"
        active = self.worker.active_progress(job["id"]) if self.worker is not None else None
        message = (
            f"Job #{job['id']}\n"
            f"Status: {job['status']}\n"
            f"Current stage: {job['current_stage']}\n"
            f"Waiting approval: {waiting}\n"
            f"Completed stages: {stages}\n"
            f"Workspace: `{job['workspace_path']}`"
        )
        if active:
            message += f"\nLive progress: {active.get('detail', 'running')}"
        active_task = ""
        pending_tasks: list[str] = []
        context_payload = job.get("context", {})
        if isinstance(context_payload, dict):
            active_task = str(context_payload.get("active_task") or "").strip()
            pending = context_payload.get("pending_tasks")
            if isinstance(pending, list):
                pending_tasks = [str(item).strip() for item in pending if str(item).strip()]
        if active_task:
            message += f"\nActive follow-up task: {active_task}"
        if pending_tasks:
            message += f"\nPending follow-up tasks: {len(pending_tasks)}"
        if job["last_error"]:
            message += f"\nLast error: {job['last_error']}"
        self.telegram.send_message_with_markup(
            chat_id,
            message,
            reply_markup=self._job_action_markup(job["id"], include_approval=job["waiting_for_approval"]),
        )

    def _approve(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /approve <job_id>")
            return
        self._approve_job(chat_id, job)

    def _approve_job(self, chat_id: int, job: dict[str, Any]) -> None:
        updated = self.db.approve_job(job["id"])
        if not updated or updated["status"] == job["status"] and not job["waiting_for_approval"]:
            self.telegram.send_message(chat_id, f"Job #{job['id']} is not waiting for approval")
            return
        self.telegram.send_message_with_markup(
            chat_id,
            f"Job #{job['id']} approved. Next stage queued: {updated['current_stage']}",
            reply_markup=self._job_action_markup(job["id"], include_approval=False),
        )

    def _reject(self, chat_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|", 1)]
        if len(parts) != 2 or not parts[0] or not parts[1]:
            self.telegram.send_message(chat_id, "Usage: /reject <job_id>|<feedback>")
            return
        job = self.db.get_job(int(parts[0]))
        if job is None:
            self.telegram.send_message(chat_id, "Job not found")
            return
        updated = self.db.reject_job(job["id"], parts[1])
        if not updated or updated["status"] == job["status"] and not job["waiting_for_approval"]:
            self.telegram.send_message(chat_id, f"Job #{job['id']} is not waiting for approval")
            return
        self.telegram.send_message(chat_id, f"Job #{job['id']} rejected. Stage `{updated['current_stage']}` queued again with your feedback.")

    def _cancel(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /cancel <job_id>")
            return
        self._cancel_job(chat_id, job)

    def _cancel_job(self, chat_id: int, job: dict[str, Any]) -> None:
        cancelled = self.db.mark_job_cancelled(job["id"])
        if not cancelled:
            self.telegram.send_message(chat_id, f"Job #{job['id']} could not be cancelled")
            return
        self.telegram.send_message(chat_id, f"Job #{job['id']} cancelled")

    def _pause(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /pause <job_id>")
            return
        updated = self.db.pause_job(job["id"])
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return
        self.telegram.send_message(chat_id, f"Job #{job['id']} paused at `{updated['current_stage']}`")

    def _resume(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            last_command = self._last_command_by_chat.get(chat_id, "").strip()
            if last_command:
                self.telegram.send_message(chat_id, f"/resume fallback: re-running last command\n`{last_command}`")
                self._dispatch_command(chat_id, self.settings.allowed_telegram_id, last_command, record_last=False)
                return
            self.telegram.send_message(chat_id, "Usage: /resume <job_id>\nNo replayable previous command found.")
            return
        updated = self.db.resume_job(job["id"])
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return
        self.telegram.send_message(chat_id, f"Job #{job['id']} resume applied -> status `{updated['status']}` at stage `{updated['current_stage']}`")
        last_command = self._last_command_by_chat.get(chat_id, "").strip()
        if last_command and self._is_replayable_command(last_command) and not last_command.lower().startswith("/resume"):
            self.telegram.send_message(chat_id, f"/resume replaying last command\n`{last_command}`")
            self._dispatch_command(chat_id, self.settings.allowed_telegram_id, last_command, record_last=False)

    def _logs(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /logs <job_id>")
            return
        self._send_job_logs(chat_id, job)

    def _send_job_logs(self, chat_id: int, job: dict[str, Any]) -> None:
        events = self.db.list_events(job["id"], limit=6)
        if not events:
            self.telegram.send_message(chat_id, f"Job #{job['id']} has no logs")
            return
        lines = [f"[{event['level']}] {event['stage_name'] or 'job'}: {event['message']}" for event in reversed(events)]
        self.telegram.send_message_with_markup(
            chat_id,
            f"Job #{job['id']} recent logs\n" + "\n".join(lines),
            reply_markup=self._job_action_markup(job["id"], include_approval=job["waiting_for_approval"]),
        )

    def _progress(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /progress <job_id>")
            return
        active = self.worker.active_progress(job["id"]) if self.worker is not None else None
        events = self.db.list_events(job["id"], limit=20)
        watchdog_restarts = sum(1 for event in events if str(event["message"]).startswith("WATCHDOG:"))
        lines = [f"Job #{job['id']}", f"Status: {job['status']}", f"Stage: {job['current_stage']}"]
        lines.append(f"Watchdog restarts: {watchdog_restarts}")
        if active:
            lines.append(f"Live: {active.get('detail', 'running')}")
        if events:
            latest = events[0]
            lines.append(f"Latest event: [{latest['level']}] {latest['message']}")
        self.telegram.send_message_with_markup(
            chat_id,
            "\n".join(lines),
            reply_markup=self._job_action_markup(job["id"], include_approval=job["waiting_for_approval"]),
        )

    def _tail(self, chat_id: int, text: str) -> None:
        job = self._job_from_command(text)
        if job is None:
            self.telegram.send_message(chat_id, "Usage: /tail <job_id>")
            return
        events = self.db.list_events(job["id"], limit=12)
        if not events:
            self.telegram.send_message(chat_id, f"Job #{job['id']} has no logs")
            return
        lines = [f"[{event['level']}] {event['stage_name'] or 'job'}: {event['message']}" for event in reversed(events)]
        self.telegram.send_message_with_markup(
            chat_id,
            f"Job #{job['id']} progress tail\n" + "\n".join(lines),
            reply_markup=self._job_action_markup(job["id"], include_approval=job["waiting_for_approval"]),
        )

    def _handle_callback_query(self, callback_query: dict[str, Any]) -> None:
        callback_id = str(callback_query.get("id", ""))
        message = callback_query.get("message") or {}
        chat = message.get("chat") or {}
        from_user = callback_query.get("from") or {}
        chat_id = int(chat.get("id", 0))
        user_id = int(from_user.get("id", 0))
        data = str(callback_query.get("data", ""))
        if user_id != self.settings.allowed_telegram_id:
            self.telegram.answer_callback_query(callback_id, "Unauthorized user")
            return
        if self.settings.allowed_telegram_chat_id is not None and chat_id != self.settings.allowed_telegram_chat_id:
            self.telegram.answer_callback_query(callback_id, "Unauthorized chat")
            return

        parts = data.split(":")
        if len(parts) == 3 and parts[0] == "model" and parts[1] == "page":
            if not parts[2].isdigit():
                self.telegram.answer_callback_query(callback_id, "Invalid page")
                return
            self._send_models_page(chat_id, int(parts[2]))
            self.telegram.answer_callback_query(callback_id, "Opened models page")
            return

        if len(parts) == 3 and parts[0] == "model" and parts[1] == "pick":
            if not parts[2].isdigit():
                self.telegram.answer_callback_query(callback_id, "Invalid model selection")
                return
            if self.worker is None:
                self.telegram.answer_callback_query(callback_id, "Worker unavailable")
                return
            cache = self._model_picker_cache.get(chat_id)
            if not cache:
                self.telegram.answer_callback_query(callback_id, "Model picker expired")
                return
            models = list(cache.get("models") or [])
            idx = int(parts[2])
            if idx < 0 or idx >= len(models):
                self.telegram.answer_callback_query(callback_id, "Model not found")
                return
            model = models[idx]
            self.worker.set_model(model)
            self.telegram.answer_callback_query(callback_id, f"Model set: {model}")
            self.telegram.send_message(chat_id, f"Model set to `{model}`")
            return

        if len(parts) == 3 and parts[0] == "log" and parts[1] in {"enable", "disable"} and parts[2].isdigit():
            if self.worker is None:
                self.telegram.answer_callback_query(callback_id, "Worker unavailable")
                return
            enabled = parts[1] == "enable"
            changed = self.worker.set_live_cli_logs(chat_id, enabled)
            if enabled:
                self.telegram.answer_callback_query(callback_id, "Live CLI log enabled")
                if changed:
                    self.telegram.send_message(chat_id, "Live CLI logs are now ON for this chat.")
                return
            self.telegram.answer_callback_query(callback_id, "Live CLI log disabled")
            if changed:
                self.telegram.send_message(chat_id, "Live CLI logs are now OFF for this chat.")
            return

        if len(parts) != 3 or parts[0] != "job" or not parts[2].isdigit():
            self.telegram.answer_callback_query(callback_id, "Unknown action")
            return

        action = parts[1]
        job = self.db.get_job(int(parts[2]))
        if job is None:
            self.telegram.answer_callback_query(callback_id, "Job not found")
            return

        if action == "status":
            self._send_job_status(chat_id, job)
            self.telegram.answer_callback_query(callback_id, f"Opened status for job {job['id']}")
            return
        if action == "logs":
            self._send_job_logs(chat_id, job)
            self.telegram.answer_callback_query(callback_id, f"Opened logs for job {job['id']}")
            return
        if action == "progress":
            self._progress(chat_id, f"/progress {job['id']}")
            self.telegram.answer_callback_query(callback_id, f"Opened progress for job {job['id']}")
            return
        if action == "approve":
            self._approve_job(chat_id, job)
            self.telegram.answer_callback_query(callback_id, f"Approved job {job['id']}")
            return
        if action == "pause":
            self._pause(chat_id, f"/pause {job['id']}")
            self.telegram.answer_callback_query(callback_id, f"Paused job {job['id']}")
            return
        if action == "resume":
            self._resume(chat_id, f"/resume {job['id']}")
            self.telegram.answer_callback_query(callback_id, f"Resumed job {job['id']}")
            return
        if action == "cancel":
            self._cancel_job(chat_id, job)
            self.telegram.answer_callback_query(callback_id, f"Cancelled job {job['id']}")
            return
        if action == "rejecthelp":
            self.telegram.send_message(chat_id, f"To reject job #{job['id']}, send:\n/reject {job['id']}|your feedback")
            self.telegram.answer_callback_query(callback_id, "Sent reject instructions")
            return

        self.telegram.answer_callback_query(callback_id, "Unknown action")

    def _job_from_command(self, text: str) -> dict[str, Any] | None:
        _, _, raw = text.partition(" ")
        raw = raw.strip()
        if not raw.isdigit():
            return None
        return self.db.get_job(int(raw))

