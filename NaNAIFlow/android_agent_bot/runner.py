from __future__ import annotations

import json
import re
import subprocess
import threading
import time
from pathlib import Path
from typing import Any

from .config import Settings
from .db import BotDatabase, STAGES, WIZARD_FIELDS
from .opencode import OpenCodeClient, run_powershell
from .prompts import code_prompt, design_prompt, idea_prompt, plan_prompt, repair_prompt, review_prompt
from .telegram_api import TelegramClient


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


def human_app_name(text: str) -> str:
    words = re.sub(r"[^a-zA-Z0-9]+", " ", text).split()
    if not words:
        return "Android App"
    return " ".join(word.capitalize() for word in words[:4])


class JobRunner:
    RUNTIME_MODEL_KEY = "worker.selected_model"

    def __init__(self, settings: Settings, db: BotDatabase, telegram: TelegramClient) -> None:
        self.settings = settings
        self.db = db
        self.telegram = telegram
        self.opencode = OpenCodeClient(
            settings.open_code_binary,
            timeout_seconds=settings.open_code_timeout_seconds,
            restart_attempts=settings.open_code_restart_attempts,
        )
        self._stop = threading.Event()
        self._active_jobs: dict[int, dict[str, Any]] = {}
        self._live_cli_log_chats: set[int] = set()
        persisted_model = self.db.get_runtime_setting(self.RUNTIME_MODEL_KEY, None)
        self._selected_model: str | None = str(persisted_model).strip() if isinstance(persisted_model, str) and persisted_model.strip() else None

    def start(self) -> threading.Thread:
        thread = threading.Thread(target=self.run_forever, name="android-agent-worker", daemon=True)
        thread.start()
        return thread

    def stop(self) -> None:
        self._stop.set()

    def set_live_cli_logs(self, chat_id: int, enabled: bool) -> bool:
        if enabled:
            already = chat_id in self._live_cli_log_chats
            self._live_cli_log_chats.add(chat_id)
            return not already
        already = chat_id in self._live_cli_log_chats
        self._live_cli_log_chats.discard(chat_id)
        return already

    def live_cli_logs_enabled(self, chat_id: int) -> bool:
        return chat_id in self._live_cli_log_chats

    def set_model(self, model: str | None) -> None:
        cleaned = (model or "").strip()
        self._selected_model = cleaned or None
        self.db.set_runtime_setting(self.RUNTIME_MODEL_KEY, self._selected_model)

    def selected_model(self) -> str | None:
        return self._selected_model

    def _effective_model_for_job(self, job: dict[str, Any]) -> str | None:
        context = job.get("context", {}) if isinstance(job.get("context"), dict) else {}
        pinned = context.get("selected_model")
        if isinstance(pinned, str) and pinned.strip():
            return pinned.strip()

        selected = self.selected_model()
        if selected:
            context["selected_model"] = selected
            self.db.set_job_context(job["id"], context)
            job["context"] = context
        return selected

    def log_mode_snapshot(self, chat_id: int) -> dict[str, Any]:
        return {
            "enabled": self.live_cli_logs_enabled(chat_id),
            "cli": "opencode",
            "binary": self.opencode.binary,
            "model": self._selected_model or "default",
            "timeout_seconds": self.settings.open_code_timeout_seconds,
            "restart_attempts": self.settings.open_code_restart_attempts,
        }

    def _report_cli_output(self, job: dict[str, Any], stage_name: str, stream_name: str, line: str) -> None:
        detail = self._humanize_cli_line(stream_name, (line or "").strip())
        if not detail:
            return
        preview = detail[:700]
        self.db.add_event(
            job["id"],
            stage_name,
            "info",
            f"CLI {stream_name}: {preview}",
            {"stream": stream_name, "line": preview},
        )
        if self.live_cli_logs_enabled(job["chat_id"]):
            short = self._compact_text(preview, 320)
            self.telegram.send_message(job["chat_id"], f"Job #{job['id']} `{stage_name}` cli {stream_name}: {short}")

    def _humanize_cli_line(self, stream_name: str, line: str) -> str:
        if not line:
            return ""
        cleaned = re.sub(r"\x1b\[[0-9;]*m", "", line).strip()
        if not cleaned:
            return ""

        try:
            event = json.loads(cleaned)
        except json.JSONDecodeError:
            return self._compact_text(cleaned)

        event_type = str(event.get("type", "")).strip().lower()
        part = event.get("part") if isinstance(event.get("part"), dict) else {}
        part_type = str(part.get("type", "")).strip().lower()

        if event_type == "reasoning":
            text = self._compact_text(str(part.get("text") or part.get("content") or ""))
            return f"reasoning: {text}" if text else "reasoning"

        if event_type == "text":
            text = self._compact_text(str(part.get("text") or part.get("content") or ""))
            return f"text: {text}" if text else "text"

        if event_type == "tool_call":
            tool_name = self._compact_text(str(part.get("toolName") or part.get("name") or event.get("tool") or "tool"), 60)
            status = self._compact_text(str(part.get("status") or event.get("status") or "started"), 40)
            return f"tool call: {tool_name} ({status})"

        if event_type in {"tool_result", "tool_output"}:
            tool_name = self._compact_text(str(part.get("toolName") or part.get("name") or event.get("tool") or "tool"), 60)
            summary = self._compact_text(str(part.get("summary") or part.get("text") or part.get("content") or "result"), 180)
            return f"tool result: {tool_name} - {summary}"

        if event_type in {"message", "progress", "status"}:
            text = self._compact_text(str(part.get("text") or part.get("content") or event.get("message") or ""), 200)
            return f"{event_type}: {text}" if text else event_type

        if event_type == "step_finish" or part_type == "step-finish":
            reason = self._compact_text(str(part.get("reason") or event.get("reason") or "done"))
            tokens = part.get("tokens") if isinstance(part.get("tokens"), dict) else {}
            if tokens:
                in_tokens = tokens.get("input", "?")
                out_tokens = tokens.get("output", "?")
                total_tokens = tokens.get("total", "?")
                return f"finish: {reason} | tokens in/out/total={in_tokens}/{out_tokens}/{total_tokens}"
            return f"finish: {reason}"

        if part_type:
            text = self._compact_text(str(part.get("text") or part.get("content") or ""))
            return f"{part_type}: {text}" if text else part_type

        if event_type:
            return event_type
        return self._compact_text(cleaned)

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
                self._active_jobs.pop(job["id"], None)
                current = self.db.get_job(job["id"])
                if current is not None and current["status"] == "paused":
                    self.telegram.send_message(job["chat_id"], f"Job #{job['id']} paused at `{current['current_stage']}`")
                    continue
                stage_name = job.get("current_stage", "unknown")
                self.db.mark_job_failed(job["id"], str(exc), stage_name)
                self.telegram.send_message(job["chat_id"], f"Job #{job['id']} failed at `{stage_name}`\n{exc}")

    def _run_job(self, job: dict[str, Any]) -> None:
        current = self.db.get_job(job["id"])
        if current is None or current["status"] == "paused":
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
                on_progress=lambda message: self._report_progress(job, stage_name, message),
                on_watchdog=lambda message: self._report_watchdog(job, stage_name, message),
                on_cli_output=lambda stream, line: self._report_cli_output(job, stage_name, stream, line),
            )
        elif stage_name == "plan":
            payload = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=plan_prompt(job, context),
                model=model_for_job,
                on_progress=lambda message: self._report_progress(job, stage_name, message),
                on_watchdog=lambda message: self._report_watchdog(job, stage_name, message),
                on_cli_output=lambda stream, line: self._report_cli_output(job, stage_name, stream, line),
            )
        elif stage_name == "design":
            payload = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=design_prompt(job, context),
                model=model_for_job,
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
            payload = self.opencode.run_json_prompt_with_progress(
                workdir=workspace,
                prompt=code_prompt(job, context, package_name, workspace, job.get("rejection_feedback")),
                model=model_for_job,
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
        gradle_wrapper = workspace / "gradlew.bat"
        app_build = workspace / "app" / "build.gradle.kts"
        if gradle_wrapper.exists() and app_build.exists():
            return None

        if any(workspace.iterdir()):
            raise RuntimeError(
                f"Workspace {workspace} is not empty but does not contain a valid Android Gradle project. Clean it or use a new slug."
            )

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
        existing = run_git(workspace, "rev-parse", "--is-inside-work-tree")
        if existing.returncode == 0:
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


class CommandRouter:
    def __init__(self, settings: Settings, db: BotDatabase, telegram: TelegramClient, worker: JobRunner | None = None) -> None:
        self.settings = settings
        self.db = db
        self.telegram = telegram
        self.worker = worker
        self._model_picker_cache: dict[int, dict[str, Any]] = {}
        self._manual_review_threads: dict[int, threading.Thread] = {}
        self._manual_review_lock = threading.Lock()

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

        if text.startswith("/start") or text.startswith("/help"):
            self._send_help(chat_id)
        elif text.startswith("/buildapp"):
            self._build_app(chat_id, user_id, text)
        elif text.startswith("/newandroid"):
            self._create_job(chat_id, user_id, text)
        elif text.startswith("/jobs"):
            self._jobs(chat_id)
        elif text.startswith("/status"):
            self._status(chat_id, text)
        elif text.startswith("/progress"):
            self._progress(chat_id, text)
        elif text.startswith("/tail"):
            self._tail(chat_id, text)
        elif text.startswith("/approve"):
            self._approve(chat_id, text)
        elif text.startswith("/reject"):
            self._reject(chat_id, text)
        elif text.startswith("/pause"):
            self._pause(chat_id, text)
        elif text.startswith("/resume"):
            self._resume(chat_id, text)
        elif text.startswith("/cancel"):
            self._cancel(chat_id, text)
        elif text.startswith("/logs"):
            self._logs(chat_id, text)
        elif text.startswith("/models"):
            self._models(chat_id, text)
        elif text.startswith("/model"):
            self._model(chat_id, text)
        elif text.startswith("/review"):
            self._review(chat_id, text)
        elif text.startswith("/runandroid"):
            self._run_android(chat_id, text)
        elif text.startswith("/syncandroid"):
            self._sync_android(chat_id, text)
        elif text.lower().startswith("/currentlog"):
            self._current_log(chat_id)
        elif text.lower().startswith("/endlog"):
            self._end_log(chat_id)
        else:
            self.telegram.send_message(chat_id, "Unknown command. Use /help")

    def _send_help(self, chat_id: int) -> None:
        self.telegram.send_message_with_markup(
            chat_id,
            "Commands:\n"
            "/buildapp with one line or multiline brief\n"
            "/newandroid <slug>|<idea>|<target users>|<constraints>\n"
            "/jobs\n"
            "/status <job_id>\n"
            "/progress <job_id>\n"
            "/tail <job_id>\n"
            "/logs <job_id>\n"
            "/models [provider]\n"
            "/model <provider/model>  (or /model default)\n"
            "/review <job_id> [--strict] [--max <iterations>]\n"
            "/runandroid <job_id> [device_id optional]\n"
            "/syncandroid <job_id>\n"
            "/currentLog\n"
            "/endLog\n"
            "/pause <job_id>\n"
            "/resume <job_id>\n"
            "/approve <job_id>\n"
            "/reject <job_id>|<feedback>\n"
            "/cancel <job_id>\n\n"
            "Tap 'Start Build Wizard' for guided input, or 'Build App Template' for a multiline form.",
            reply_markup=self._menu_keyboard(),
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
        pinned_jobs = 0
        for candidate in self.db.list_jobs(limit=100):
            if candidate["status"] in {"completed", "cancelled", "failed"}:
                continue
            context = candidate.get("context", {})
            context["selected_model"] = model
            self.db.set_job_context(candidate["id"], context)
            pinned_jobs += 1
        self.telegram.send_message(chat_id, f"Model set to `{model}`\nPinned on active jobs: {pinned_jobs}")

    def _models(self, chat_id: int, text: str) -> None:
        if self.worker is None:
            self.telegram.send_message(chat_id, "Worker is not available")
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
                label = f"{label} ✅"
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

        workspace = Path(job["workspace_path"])
        if not (workspace / "gradlew.bat").exists():
            self.telegram.send_message(chat_id, f"Workspace `{workspace}` is not an Android Gradle project")
            return

        device_arg = f" -Pandroid.injected.invoked.from.ide=true -Pandroid.testInstrumentationRunnerArguments.class={device_id}" if False else ""
        install_command = ".\\gradlew.bat installDebug"
        if device_id:
            install_command += f" -Pandroid.testInstrumentationRunnerArguments.notAnnotation={device_id}"
        self.telegram.send_message(chat_id, f"Running install for job #{job_id}\n`{install_command}`")
        install_result = run_powershell(install_command, workspace)
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
            launch_result = run_powershell(launch_cmd, workspace)
            if launch_result.returncode == 0:
                launched = f"Installed and launched `{package_name}`"
            else:
                launched = f"Installed, but launch failed for `{package_name}`"

        self.telegram.send_message(chat_id, f"runandroid done for job #{job_id}\n{launched}")

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
        result = run_powershell(sync_command, workspace)
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

        workspace = self.settings.projects_root / slug
        workspace.mkdir(parents=True, exist_ok=True)
        job_id = self.db.create_job(
            slug=slug,
            request_text=request_text,
            target_users=target_users,
            constraints_text=constraints_text,
            workspace_path=str(workspace),
            chat_id=chat_id,
            created_by=user_id,
        )
        if self.worker is not None:
            selected = self.worker.selected_model()
            if selected:
                created_job = self.db.get_job(job_id)
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

    def _create_job(self, chat_id: int, user_id: int, text: str) -> None:
        _, _, raw_args = text.partition(" ")
        parts = [part.strip() for part in raw_args.split("|")]
        if len(parts) < 4 or not all(parts[:3]):
            self.telegram.send_message(chat_id, "Usage: /newandroid <slug>|<idea>|<target users>|<constraints>")
            return
        slug = slugify(parts[0])
        workspace = self.settings.projects_root / slug
        workspace.mkdir(parents=True, exist_ok=True)
        job_id = self.db.create_job(
            slug=slug,
            request_text=parts[1],
            target_users=parts[2],
            constraints_text=parts[3],
            workspace_path=str(workspace),
            chat_id=chat_id,
            created_by=user_id,
        )
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
            self.telegram.send_message(chat_id, "Usage: /resume <job_id>")
            return
        updated = self.db.resume_job(job["id"])
        if updated is None:
            self.telegram.send_message(chat_id, "Job not found")
            return
        self.telegram.send_message(chat_id, f"Job #{job['id']} resumed with status `{updated['status']}` at stage `{updated['current_stage']}`")

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
