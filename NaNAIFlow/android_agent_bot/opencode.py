from __future__ import annotations

import json
import os
import queue
import shutil
import subprocess
import tempfile
import threading
import time
from collections.abc import Callable
from pathlib import Path
from typing import Any


class OpenCodeClient:
    MAX_429_RETRIES = 5

    def __init__(self, binary: str, timeout_seconds: int = 600, restart_attempts: int = 2) -> None:
        self.binary = self._resolve_binary(binary)
        self.timeout_seconds = timeout_seconds
        self.restart_attempts = restart_attempts

    def run_json_prompt(self, *, workdir: Path, prompt: str) -> dict[str, Any]:
        stdout = self._run_prompt(workdir=workdir, prompt=prompt)
        return self._parse_json(stdout)

    def run_json_prompt_with_progress(
        self,
        *,
        workdir: Path,
        prompt: str,
        model: str | None = None,
        on_progress: Callable[[str], None] | None = None,
        on_watchdog: Callable[[str], None] | None = None,
        on_cli_output: Callable[[str, str], None] | None = None,
    ) -> dict[str, Any]:
        stdout = self._run_prompt(
            workdir=workdir,
            prompt=prompt,
            model=model,
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
            while True:
                for attempt in range(1, self.MAX_429_RETRIES + 1):
                    self._stream_callback = on_cli_output
                    result = self._run_once(workdir, prompt_path, model=model)
                    self._stream_callback = None
                    if result.returncode == 0:
                        break
                    error_text = (result.stderr or result.stdout or "").strip()
                    if attempt >= self.MAX_429_RETRIES or not self._is_rate_limited(error_text):
                        break
                    if on_progress is not None:
                        on_progress(f"Rate limited by model provider, retry {attempt}/{self.MAX_429_RETRIES}")
                    time.sleep(min(30, attempt * 5))

                if result is not None and result.returncode == 0:
                    break
                error_text = (result.stderr or result.stdout or "").strip() if result is not None else ""
                if not self._is_cli_hung_error(error_text) or hung_attempts >= self.restart_attempts:
                    break
                hung_attempts += 1
                message = f"OpenCode CLI hung or timed out, restarting {hung_attempts}/{self.restart_attempts}"
                if on_progress is not None:
                    on_progress(message)
                if on_watchdog is not None:
                    on_watchdog(message)
        finally:
            self._stream_callback = None
            prompt_path.unlink(missing_ok=True)

        if result is None:
            raise RuntimeError("OpenCode CLI did not run")
        if result.returncode != 0:
            raise RuntimeError((result.stderr or result.stdout).strip() or "OpenCode CLI failed")
        return (result.stdout or "").strip()

    def _run_once(self, workdir: Path, prompt_path: Path, *, model: str | None = None) -> subprocess.CompletedProcess[str]:
        command = [
            self.binary,
            "run",
            "--format",
            "json",
            "--dir",
            str(workdir),
            "Read the attached file as the full task prompt. Follow it exactly and return only the final answer.",
            "--file",
            str(prompt_path),
        ]
        if model and model.strip():
            command.extend(["--model", model.strip()])

        callback = getattr(self, "_stream_callback", None)
        timeout_seconds = self.timeout_seconds

        process = subprocess.Popen(
            command,
            cwd=workdir,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=False,
            bufsize=0,
        )

        stdout_lines: list[str] = []
        stderr_lines: list[str] = []
        events: queue.Queue[tuple[str, str]] = queue.Queue()

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
                        events.put((stream_name, line))
                if buffer:
                    line = buffer.decode("utf-8", errors="replace").rstrip("\r")
                    sink.append(line)
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
        timed_out = False
        while True:
            now = time.time()
            if now - start > timeout_seconds:
                timed_out = True
                break
            if process.poll() is not None:
                break

            try:
                stream_name, line = events.get(timeout=0.2)
            except queue.Empty:
                continue
            if callback is not None and line.strip():
                callback(stream_name, line.strip()[:1000])

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
                callback(stream_name, line.strip()[:1000])

        stdout_thread.join(timeout=1)
        stderr_thread.join(timeout=1)

        stdout_text = "".join(stdout_lines)
        stderr_text = "".join(stderr_lines)

        if timed_out:
            return subprocess.CompletedProcess(
                args=command,
                returncode=124,
                stdout=stdout_text,
                stderr=(stderr_text or "") + f"\nOpenCode CLI timed out after {timeout_seconds} seconds",
            )

        return subprocess.CompletedProcess(
            args=command,
            returncode=process.returncode,
            stdout=stdout_text,
            stderr=stderr_text,
        )

    def list_models(self, provider: str | None = None, timeout_seconds: int = 30) -> subprocess.CompletedProcess[str]:
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

    def _is_cli_hung_error(self, error_text: str) -> bool:
        normalized = error_text.lower()
        return "timed out" in normalized or "timeout" in normalized or "hung" in normalized


def run_powershell(command: str, workdir: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            command,
        ],
        cwd=workdir,
        capture_output=True,
        text=True,
        check=False,
    )
