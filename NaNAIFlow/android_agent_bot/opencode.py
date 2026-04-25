from __future__ import annotations

import json
import os
import queue
import random
import re
import shutil
import subprocess
import tempfile
import threading
import time
from collections.abc import Callable
from pathlib import Path
from typing import Any


class OpenCodeClient:
    MAX_429_RETRIES = 100

    def __init__(
        self,
        binary: str,
        timeout_seconds: int = 600,
        restart_attempts: int = 2,
        min_request_interval_seconds: float = 2.0,
    ) -> None:
        self.binary = self._resolve_binary(binary)
        self.timeout_seconds = timeout_seconds
        self.restart_attempts = restart_attempts
        self.min_request_interval_seconds = max(0.0, float(min_request_interval_seconds))
        self._request_gate_lock = threading.Lock()
        self._last_request_started_at = 0.0

    def run_json_prompt(self, *, workdir: Path, prompt: str) -> dict[str, Any]:
        stdout = self._run_prompt(workdir=workdir, prompt=prompt)
        return self._parse_json(stdout)

    def run_json_prompt_with_progress(
        self,
        *,
        workdir: Path,
        prompt: str,
        model: str | None = None,
        timeout_seconds: int | None = None,
        on_progress: Callable[[str], None] | None = None,
        on_watchdog: Callable[[str], None] | None = None,
        on_cli_output: Callable[[str, str], None] | None = None,
    ) -> dict[str, Any]:
        stdout = self._run_prompt(
            workdir=workdir,
            prompt=prompt,
            model=model,
            timeout_seconds=timeout_seconds,
            on_progress=on_progress,
            on_watchdog=on_watchdog,
            on_cli_output=on_cli_output,
        )
        return self._parse_json(stdout)

    def _run_prompt(
        self,
        *,
        workdir: Path,
        prompt: str,
        model: str | None = None,
        timeout_seconds: int | None = None,
        on_progress: Callable[[str], None] | None = None,
        on_watchdog: Callable[[str], None] | None = None,
        on_cli_output: Callable[[str, str], None] | None = None,
    ) -> str:
        workdir.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as handle:
            handle.write(prompt)
            prompt_path = Path(handle.name)

        try:
            result: subprocess.CompletedProcess[str] | None = None
            hung_attempts = 0
            session_id: str | None = None
            session_fallback_count = 0
            recovered_empty_output = False
            while True:
                rate_attempt = 0
                while rate_attempt < self.MAX_429_RETRIES:
                    rate_attempt += 1
                    self._wait_for_request_slot()
                    self._stream_callback = on_cli_output
                    result, observed_session_id = self._run_once(
                        workdir,
                        prompt_path,
                        model=model,
                        session_id=session_id,
                        timeout_seconds=timeout_seconds,
                    )
                    self._stream_callback = None
                    if observed_session_id:
                        session_id = observed_session_id
                    if result.returncode == 0:
                        break
                    error_text = (result.stderr or result.stdout or "").strip()
                    if session_id and self._is_session_resume_error(error_text):
                        session_fallback_count += 1
                        if on_progress is not None:
                            on_progress(
                                f"Session resume failed for {session_id}, starting a new OpenCode session (fallback {session_fallback_count})"
                            )
                        session_id = None
                        continue
                    if not self._is_rate_limited(error_text):
                        break
                    wait_seconds = self._rate_limit_wait_seconds(error_text, rate_attempt)
                    if on_progress is not None:
                        if session_id:
                            on_progress(
                                f"Rate limited by model provider, retry {rate_attempt}/{self.MAX_429_RETRIES} after {wait_seconds:.1f}s (continue session {session_id})"
                            )
                        else:
                            on_progress(
                                f"Rate limited by model provider, retry {rate_attempt}/{self.MAX_429_RETRIES} after {wait_seconds:.1f}s"
                            )
                    time.sleep(wait_seconds)

                if (
                    result is not None
                    and result.returncode != 0
                    and self._is_rate_limited((result.stderr or result.stdout or "").strip())
                    and rate_attempt >= self.MAX_429_RETRIES
                ):
                    raise RuntimeError(
                        f"Rate limited after {self.MAX_429_RETRIES} retries. "
                        f"Last error: {(result.stderr or result.stdout or '').strip()[:400]}"
                    )

                if result is not None and result.returncode == 0:
                    candidate_stdout = (result.stdout or "").strip()
                    if not candidate_stdout and session_id:
                        recovered_empty_output = True
                        if on_progress is not None:
                            on_progress(
                                f"CLI returned empty final output, sending lightweight continue prompt on session {session_id}"
                            )
                        continue_prompt = self._write_recovery_prompt()
                        try:
                            self._wait_for_request_slot()
                            self._stream_callback = on_cli_output
                            resume_result, observed_session_id = self._run_once(
                                workdir,
                                continue_prompt,
                                model=model,
                                session_id=session_id,
                                timeout_seconds=min(max(300, int(timeout_seconds or self.timeout_seconds)), 900),
                            )
                            result = resume_result
                            if observed_session_id:
                                session_id = observed_session_id
                        finally:
                            self._stream_callback = None
                            continue_prompt.unlink(missing_ok=True)
                        if result.returncode == 0 and (result.stdout or "").strip():
                            break
                        continue
                    break
                error_text = (result.stderr or result.stdout or "").strip() if result is not None else ""
                if not self._is_cli_hung_error(error_text):
                    break
                hung_attempts += 1
                if session_id:
                    message = (
                        f"OpenCode CLI hung or timed out, restarting attempt {hung_attempts} "
                        f"and resuming session {session_id}"
                    )
                else:
                    message = f"OpenCode CLI hung or timed out, restarting attempt {hung_attempts}"
                if on_progress is not None:
                    on_progress(message)
                if on_watchdog is not None:
                    on_watchdog(message)
                time.sleep(min(10 + hung_attempts, 30))
        finally:
            self._stream_callback = None
            prompt_path.unlink(missing_ok=True)

        if result is None:
            raise RuntimeError("OpenCode CLI did not run")
        if result.returncode != 0:
            raise RuntimeError((result.stderr or result.stdout).strip() or "OpenCode CLI failed")
        if recovered_empty_output and not (result.stdout or "").strip():
            raise RuntimeError("OpenCode CLI completed but returned empty output after recovery continue")
        return (result.stdout or "").strip()

    def _run_once(
        self,
        workdir: Path,
        prompt_path: Path,
        *,
        model: str | None = None,
        session_id: str | None = None,
        timeout_seconds: int | None = None,
    ) -> tuple[subprocess.CompletedProcess[str], str | None]:
        output_file = tempfile.NamedTemporaryFile("w", suffix=".out.txt", delete=False, encoding="utf-8")
        output_file.close()
        output_path = Path(output_file.name)

        command: list[str] = []
        stdin_payload: bytes | None = None
        if self._is_codex_binary():
            command = [
                self.binary,
                "exec",
            ]
            if session_id and session_id.strip():
                command.extend(["resume", session_id.strip()])
            command.extend(
                [
                    "-",
                    "--json",
                    "--skip-git-repo-check",
                    "--full-auto",
                    "--cd",
                    str(workdir),
                    "--output-last-message",
                    str(output_path),
                ]
            )
            if model and model.strip():
                command.extend(["--model", model.strip()])
            stdin_payload = prompt_path.read_bytes()
        else:
            command = [
                self.binary,
                "run",
                "--format",
                "json",
                "--dir",
                str(workdir),
            ]
            if session_id and session_id.strip():
                command.extend(["--session", session_id.strip(), "--continue"])
            else:
                command.extend(
                    [
                        "Read the attached file as the full task prompt. Follow it exactly and return only the final answer.",
                        "--file",
                        str(prompt_path),
                    ]
                )
            if model and model.strip():
                command.extend(["--model", model.strip()])

        callback = getattr(self, "_stream_callback", None)
        timeout_seconds = max(30, int(timeout_seconds or self.timeout_seconds))

        process = subprocess.Popen(
            command,
            cwd=workdir,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            stdin=subprocess.PIPE if stdin_payload is not None else None,
            text=False,
            bufsize=0,
        )
        if stdin_payload is not None and process.stdin is not None:
            try:
                process.stdin.write(stdin_payload)
                process.stdin.close()
            except Exception:  # noqa: BLE001
                pass

        stdout_lines: list[str] = []
        stderr_lines: list[str] = []
        events: queue.Queue[tuple[str, str]] = queue.Queue()
        session_holder: list[str | None] = [session_id.strip() if session_id else None]
        session_lock = threading.Lock()

        def capture_session_id(raw_line: str) -> None:
            parsed = self._extract_session_id(raw_line)
            if not parsed:
                return
            with session_lock:
                session_holder[0] = parsed

        def reader(stream_name: str, stream: Any, sink: list[str]) -> None:
            buffer = b""
            try:
                while True:
                    chunk = stream.read(4096)
                    if not chunk:
                        break
                    if isinstance(chunk, str):
                        chunk = chunk.encode("utf-8", errors="replace")
                    buffer += chunk
                    while b"\n" in buffer:
                        raw_line, buffer = buffer.split(b"\n", 1)
                        line = raw_line.decode("utf-8", errors="replace").rstrip("\r")
                        sink.append(line + "\n")
                        capture_session_id(line)
                        events.put((stream_name, line))
                if buffer:
                    line = buffer.decode("utf-8", errors="replace").rstrip("\r")
                    sink.append(line)
                    capture_session_id(line)
                    events.put((stream_name, line))
            finally:
                try:
                    stream.close()
                except Exception:  # noqa: BLE001
                    pass

        stdout_thread = threading.Thread(target=reader, args=("stdout", process.stdout, stdout_lines), daemon=True)
        stderr_thread = threading.Thread(target=reader, args=("stderr", process.stderr, stderr_lines), daemon=True)
        stdout_thread.start()
        stderr_thread.start()

        start = time.time()
        last_activity = start
        timed_out = False
        while True:
            now = time.time()
            if now - last_activity > timeout_seconds:
                timed_out = True
                break
            if process.poll() is not None:
                break

            try:
                stream_name, line = events.get(timeout=0.2)
            except queue.Empty:
                continue
            if callback is not None and line.strip():
                callback(stream_name, line.strip())
            last_activity = time.time()

        if timed_out:
            try:
                if process.poll() is None:
                    if process.pid and os.name == "nt":
                        subprocess.run(["taskkill", "/pid", str(process.pid), "/f", "/t"], capture_output=True, text=True, check=False)
                    else:
                        process.kill()
            except Exception:  # noqa: BLE001
                pass

        try:
            process.wait(timeout=5)
        except Exception:  # noqa: BLE001
            pass

        while True:
            try:
                stream_name, line = events.get_nowait()
            except queue.Empty:
                break
            if callback is not None and line.strip():
                callback(stream_name, line.strip())

        stdout_thread.join(timeout=1)
        stderr_thread.join(timeout=1)

        stdout_text = "".join(stdout_lines)
        stderr_text = "".join(stderr_lines)
        if session_holder[0] is None:
            session_holder[0] = self._extract_session_id(f"{stdout_text}\n{stderr_text}")

        final_stdout = stdout_text
        try:
            if self._is_codex_binary() and output_path.exists():
                output_text = output_path.read_text(encoding="utf-8", errors="ignore").strip()
                if output_text:
                    final_stdout = output_text
        except Exception:  # noqa: BLE001
            pass
        finally:
            output_path.unlink(missing_ok=True)

        if timed_out:
            return (
                subprocess.CompletedProcess(
                    args=command,
                    returncode=124,
                    stdout=final_stdout,
                    stderr=(stderr_text or "") + f"\nOpenCode CLI idle timed out after {timeout_seconds} seconds",
                ),
                session_holder[0],
            )

        return (
            subprocess.CompletedProcess(
                args=command,
                returncode=process.returncode,
                stdout=final_stdout,
                stderr=stderr_text,
            ),
            session_holder[0],
        )

    def list_models(self, provider: str | None = None, timeout_seconds: int = 30) -> subprocess.CompletedProcess[str]:
        if self._is_codex_binary():
            command = [self.binary, "models"]
            return subprocess.run(
                command,
                input="\n",
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                check=False,
                timeout=max(5, timeout_seconds),
            )

        command = [self.binary, "models"]
        if provider and provider.strip():
            command.append(provider.strip())
        return subprocess.run(
            command,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
            timeout=max(5, timeout_seconds),
        )

    def _resolve_binary(self, binary: str) -> str:
        candidate = binary.strip()
        if not candidate:
            return binary
        direct_path = Path(candidate)
        if direct_path.exists():
            return str(direct_path)
        resolved = shutil.which(candidate)
        if resolved:
            return resolved
        if not candidate.lower().endswith(".cmd"):
            resolved = shutil.which(f"{candidate}.cmd")
            if resolved:
                return resolved
        return candidate

    def _parse_json(self, output: str) -> dict[str, Any]:
        text = output.strip()
        if not text:
            raise RuntimeError("OpenCode returned empty output")

        if "\n" in text:
            parsed_from_events = self._parse_json_event_stream(text)
            if parsed_from_events is not None:
                return parsed_from_events

        try:
            return json.loads(text)
        except json.JSONDecodeError:
            start = text.find("{")
            end = text.rfind("}")
            if start >= 0 and end > start:
                return json.loads(text[start : end + 1])
            raise RuntimeError(f"OpenCode did not return valid JSON: {text[:400]}")

    def _parse_json_event_stream(self, output: str) -> dict[str, Any] | None:
        last_text_payload: str | None = None
        for line in output.splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            if event.get("type") != "text":
                continue
            part = event.get("part")
            if not isinstance(part, dict):
                continue
            payload = part.get("text")
            if isinstance(payload, str) and payload.strip():
                last_text_payload = payload.strip()

        if not last_text_payload:
            return None
        return json.loads(last_text_payload)

    def _is_rate_limited(self, error_text: str) -> bool:
        normalized = error_text.lower()
        return "429" in normalized or "rate limit" in normalized or "too many requests" in normalized

    def _rate_limit_wait_seconds(self, error_text: str, attempt: int) -> float:
        retry_after = self._extract_retry_after_seconds(error_text)
        if retry_after is not None:
            return float(max(1, min(retry_after, 120)))
        upper = min(8.0, 3.0 + (attempt * 0.05))
        return random.uniform(2.0, max(2.5, upper))

    def _extract_retry_after_seconds(self, error_text: str) -> int | None:
        text = (error_text or "").lower()
        match = re.search(r"retry\s*after\s*(\d+)", text)
        if match:
            return int(match.group(1))
        match = re.search(r"\"retry_after\"\s*:\s*(\d+)", text)
        if match:
            return int(match.group(1))
        return None

    def _wait_for_request_slot(self) -> None:
        if self.min_request_interval_seconds <= 0:
            return
        while True:
            with self._request_gate_lock:
                now = time.monotonic()
                elapsed = now - self._last_request_started_at
                if elapsed >= self.min_request_interval_seconds:
                    self._last_request_started_at = now
                    return
                wait_for = self.min_request_interval_seconds - elapsed
            time.sleep(min(wait_for, 1.0))

    def _write_recovery_prompt(self) -> Path:
        recovery_text = (
            "Continue from the existing session context.\n"
            "The previous step completed but did not return the required final JSON.\n"
            "Return ONLY the final strict JSON output for the original task now.\n"
            "No markdown, no explanation.\n"
        )
        with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as handle:
            handle.write(recovery_text)
            return Path(handle.name)

    def _is_cli_hung_error(self, error_text: str) -> bool:
        normalized = error_text.lower()
        return "timed out" in normalized or "timeout" in normalized or "hung" in normalized

    def _is_codex_binary(self) -> bool:
        name = Path(self.binary).name.lower()
        stem = Path(self.binary).stem.lower()
        return stem == "codex" or name == "codex.exe"

    def _is_session_resume_error(self, error_text: str) -> bool:
        normalized = (error_text or "").lower()
        if "session" not in normalized:
            return False
        markers = [
            "session not found",
            "invalid session",
            "unknown session",
            "session expired",
            "cannot continue",
            "failed to continue",
            "no such session",
            "session id is invalid",
        ]
        return any(marker in normalized for marker in markers)

    def _extract_session_id(self, raw_text: str) -> str | None:
        text = (raw_text or "").strip()
        if not text:
            return None

        for line in text.splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                payload = json.loads(line)
            except json.JSONDecodeError:
                payload = None
            session = self._extract_session_id_from_payload(payload)
            if session:
                return session

            direct = self._extract_session_id_from_string(line)
            if direct:
                return direct

        return None

    def _extract_session_id_from_payload(self, payload: Any) -> str | None:
        if isinstance(payload, dict):
            for key in ("session_id", "sessionId", "session", "conversation_id", "conversationId", "thread_id", "threadId"):
                value = payload.get(key)
                if isinstance(value, str):
                    candidate = value.strip()
                    if candidate:
                        return candidate
                if isinstance(value, dict):
                    nested = self._extract_session_id_from_payload(value)
                    if nested:
                        return nested
            for value in payload.values():
                nested = self._extract_session_id_from_payload(value)
                if nested:
                    return nested
        elif isinstance(payload, list):
            for item in payload:
                nested = self._extract_session_id_from_payload(item)
                if nested:
                    return nested
        return None

    def _extract_session_id_from_string(self, text: str) -> str | None:
        quoted = re.search(r'"session(?:_id|Id)?"\s*:\s*"([^"]+)"', text)
        if quoted:
            return quoted.group(1).strip()
        uuid_match = re.search(
            r"\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b",
            text,
        )
        if uuid_match:
            return uuid_match.group(0)
        return None


def run_powershell(command: str, workdir: Path, timeout_seconds: int | None = None) -> subprocess.CompletedProcess[str]:
    args = [
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-Command",
        command,
    ]
    process = subprocess.Popen(
        args,
        cwd=workdir,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    try:
        stdout_text, stderr_text = process.communicate(timeout=timeout_seconds)
        return subprocess.CompletedProcess(
            args=args,
            returncode=process.returncode,
            stdout=stdout_text or "",
            stderr=stderr_text or "",
        )
    except subprocess.TimeoutExpired as exc:
        try:
            if process.poll() is None:
                if process.pid and os.name == "nt":
                    subprocess.run(["taskkill", "/pid", str(process.pid), "/f", "/t"], capture_output=True, text=True, check=False)
                else:
                    process.kill()
        except Exception:  # noqa: BLE001
            pass
        try:
            stdout_tail, stderr_tail = process.communicate(timeout=5)
        except Exception:  # noqa: BLE001
            stdout_tail = exc.stdout or ""
            stderr_tail = exc.stderr or ""
        return subprocess.CompletedProcess(
            args=args,
            returncode=124,
            stdout=(stdout_tail or ""),
            stderr=((stderr_tail or "") + f"\nCommand timed out after {timeout_seconds} seconds").strip(),
        )

