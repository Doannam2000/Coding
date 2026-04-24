from __future__ import annotations

import json
import socket
import time
import urllib.parse
import urllib.error
import urllib.request
from typing import Any


class TelegramClientError(RuntimeError):
    pass


class TelegramNetworkError(TelegramClientError):
    pass


class TelegramClient:
    TELEGRAM_TEXT_LIMIT = 4096

    def __init__(self, token: str) -> None:
        self.base_url = f"https://api.telegram.org/bot{token}"

    def get_updates(self, *, offset: int | None, timeout: int) -> list[dict[str, Any]]:
        payload = {"timeout": timeout}
        if offset is not None:
            payload["offset"] = offset
        data = self._post("getUpdates", payload, retries=5)
        return data if isinstance(data, list) else []

    def answer_callback_query(self, callback_query_id: str, text: str) -> None:
        try:
            self._post("answerCallbackQuery", {"callback_query_id": callback_query_id, "text": text})
        except (TelegramClientError, TelegramNetworkError) as exc:
            detail = str(exc).lower()
            if (
                "query is too old" in detail
                or "query id is invalid" in detail
                or "response timeout expired" in detail
                or "status=400" in detail
            ):
                return
            return

    def send_message(self, chat_id: int, text: str) -> None:
        self.send_message_with_markup(chat_id, text)

    def send_message_many(self, chat_id: int, text: str) -> None:
        for chunk in self._split_text_chunks(text, self.TELEGRAM_TEXT_LIMIT):
            self.send_message_with_markup(chat_id, chunk)

    def send_message_with_markup(self, chat_id: int, text: str, reply_markup: dict[str, Any] | None = None) -> None:
        chunks = self._split_text_chunks(text, self.TELEGRAM_TEXT_LIMIT)
        if not chunks:
            return
        if reply_markup is None and len(chunks) > 1:
            for chunk in chunks:
                payload: dict[str, Any] = {
                    "chat_id": chat_id,
                    "text": chunk,
                    "disable_web_page_preview": True,
                }
                self._safe_post("sendMessage", payload)
            return

        if len(chunks) > 1:
            for chunk in chunks[:-1]:
                payload_chunk: dict[str, Any] = {
                    "chat_id": chat_id,
                    "text": chunk,
                    "disable_web_page_preview": True,
                }
                self._safe_post("sendMessage", payload_chunk)
        text = chunks[-1]
        payload: dict[str, Any] = {
            "chat_id": chat_id,
            "text": text,
            "disable_web_page_preview": True,
        }
        if reply_markup is not None:
            payload["reply_markup"] = json.dumps(reply_markup, ensure_ascii=True)
        self._safe_post("sendMessage", payload)

    def set_commands(self, commands: list[dict[str, str]]) -> None:
        self._safe_post("setMyCommands", {"commands": json.dumps(commands, ensure_ascii=True)})

    def _safe_post(self, method: str, payload: dict[str, Any], *, retries: int = 0) -> Any | None:
        try:
            return self._post(method, payload, retries=retries)
        except (TelegramClientError, TelegramNetworkError):
            return None

    def _post(self, method: str, payload: dict[str, Any], *, retries: int = 0) -> Any:
        body = urllib.parse.urlencode(payload).encode("utf-8")
        request = urllib.request.Request(f"{self.base_url}/{method}", data=body, method="POST")
        for attempt in range(retries + 1):
            try:
                with urllib.request.urlopen(request, timeout=90) as response:
                    data = json.loads(response.read().decode("utf-8"))
                break
            except urllib.error.HTTPError as exc:
                raw_body = exc.read().decode("utf-8", errors="ignore")
                detail = raw_body[:1200] if raw_body else str(exc)
                raise TelegramClientError(f"Telegram HTTP error for {method}: status={exc.code} body={detail}") from exc
            except (urllib.error.URLError, TimeoutError, ConnectionError, socket.timeout, OSError) as exc:
                is_last_attempt = attempt >= retries
                if is_last_attempt:
                    reason = getattr(exc, "reason", exc)
                    raise TelegramNetworkError(f"Telegram network error for {method}: {reason}") from exc
                time.sleep(min(2 * (attempt + 1), 10))
        if not data.get("ok"):
            raise TelegramClientError(f"Telegram API error for {method}: {data}")
        return data.get("result")

    def _split_text_chunks(self, text: str, limit: int) -> list[str]:
        clean = (text or "").strip()
        if not clean:
            return []
        if len(clean) <= limit:
            return [clean]

        chunks: list[str] = []
        remaining = clean
        while len(remaining) > limit:
            split_idx = remaining.rfind("\n", 0, limit)
            if split_idx < int(limit * 0.4):
                split_idx = remaining.rfind(" ", 0, limit)
            if split_idx < int(limit * 0.4):
                split_idx = limit
            chunk = remaining[:split_idx].strip()
            if not chunk:
                chunk = remaining[:limit].strip()
                split_idx = limit
            chunks.append(chunk)
            remaining = remaining[split_idx:].lstrip()
        if remaining:
            chunks.append(remaining)
        return chunks
