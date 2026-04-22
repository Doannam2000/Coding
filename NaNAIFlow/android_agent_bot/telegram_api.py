from __future__ import annotations

import json
import urllib.parse
import urllib.error
import urllib.request
from typing import Any


class TelegramClient:
    def __init__(self, token: str) -> None:
        self.base_url = f"https://api.telegram.org/bot{token}"

    def get_updates(self, *, offset: int | None, timeout: int) -> list[dict[str, Any]]:
        payload = {"timeout": timeout}
        if offset is not None:
            payload["offset"] = offset
        data = self._post("getUpdates", payload)
        return data if isinstance(data, list) else []

    def answer_callback_query(self, callback_query_id: str, text: str) -> None:
        self._post("answerCallbackQuery", {"callback_query_id": callback_query_id, "text": text})

    def send_message(self, chat_id: int, text: str) -> None:
        self.send_message_with_markup(chat_id, text)

    def send_message_with_markup(self, chat_id: int, text: str, reply_markup: dict[str, Any] | None = None) -> None:
        payload: dict[str, Any] = {
            "chat_id": chat_id,
            "text": text,
            "disable_web_page_preview": True,
        }
        if reply_markup is not None:
            payload["reply_markup"] = json.dumps(reply_markup, ensure_ascii=True)
        self._post("sendMessage", payload)

    def set_commands(self, commands: list[dict[str, str]]) -> None:
        self._post("setMyCommands", {"commands": json.dumps(commands, ensure_ascii=True)})

    def _post(self, method: str, payload: dict[str, Any]) -> Any:
        body = urllib.parse.urlencode(payload).encode("utf-8")
        request = urllib.request.Request(f"{self.base_url}/{method}", data=body, method="POST")
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                data = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            raw_body = exc.read().decode("utf-8", errors="ignore")
            detail = raw_body[:1200] if raw_body else str(exc)
            raise RuntimeError(f"Telegram HTTP error for {method}: status={exc.code} body={detail}") from exc
        except urllib.error.URLError as exc:
            raise RuntimeError(f"Telegram network error for {method}: {exc.reason}") from exc
        if not data.get("ok"):
            raise RuntimeError(f"Telegram API error for {method}: {data}")
        return data.get("result")
