from __future__ import annotations

import json
import sqlite3
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


STAGES = ["idea", "plan", "design", "code", "verify", "review"]

WIZARD_FIELDS = ["slug", "idea", "style", "font", "features", "requirements", "target_users", "constraints", "app_id"]


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


class BotDatabase:
    def __init__(self, database_path: Path) -> None:
        self.database_path = database_path
        self.database_path.parent.mkdir(parents=True, exist_ok=True)
        self._initialize()

    @contextmanager
    def _connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.database_path)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        finally:
            conn.close()

    def _initialize(self) -> None:
        with self._connect() as conn:
            conn.executescript(
                """
                create table if not exists jobs (
                  id integer primary key autoincrement,
                  slug text not null,
                  request_text text not null,
                  target_users text not null,
                  constraints_text text not null,
                  status text not null default 'queued',
                  current_stage text not null default 'idea',
                  stage_index integer not null default 0,
                  waiting_for_approval integer not null default 0,
                  rejection_feedback text,
                  workspace_path text not null,
                  chat_id integer not null,
                  created_by integer not null,
                  last_error text,
                  context_json text not null default '{}',
                  created_at text not null,
                  updated_at text not null
                );

                create table if not exists job_events (
                  id integer primary key autoincrement,
                  job_id integer not null references jobs(id) on delete cascade,
                  stage_name text,
                  level text not null,
                  message text not null,
                  payload_json text,
                  created_at text not null
                );

                create table if not exists chat_wizards (
                  chat_id integer primary key,
                  created_by integer not null,
                  state_json text not null,
                  updated_at text not null
                );

                create table if not exists runtime_settings (
                  key text primary key,
                  value_json text not null,
                  updated_at text not null
                );
                """
            )

    def create_job(
        self,
        *,
        slug: str,
        request_text: str,
        target_users: str,
        constraints_text: str,
        workspace_path: str,
        chat_id: int,
        created_by: int,
    ) -> int:
        now = utc_now()
        with self._connect() as conn:
            cursor = conn.execute(
                """
                insert into jobs (
                  slug, request_text, target_users, constraints_text, status, current_stage,
                  stage_index, waiting_for_approval, workspace_path, chat_id, created_by,
                  context_json, created_at, updated_at
                ) values (?, ?, ?, ?, 'queued', 'idea', 0, 0, ?, ?, ?, '{}', ?, ?)
                """,
                (slug, request_text, target_users, constraints_text, workspace_path, chat_id, created_by, now, now),
            )
            job_id = int(cursor.lastrowid)
        self.add_event(job_id, None, "info", "Job created", None)
        return job_id

    def add_event(
        self,
        job_id: int,
        stage_name: str | None,
        level: str,
        message: str,
        payload: dict[str, Any] | None,
    ) -> None:
        with self._connect() as conn:
            conn.execute(
                "insert into job_events (job_id, stage_name, level, message, payload_json, created_at) values (?, ?, ?, ?, ?, ?)",
                (job_id, stage_name, level, message, json.dumps(payload, ensure_ascii=True) if payload is not None else None, utc_now()),
            )

    def get_job(self, job_id: int) -> dict[str, Any] | None:
        with self._connect() as conn:
            row = conn.execute("select * from jobs where id = ?", (job_id,)).fetchone()
        return self._row_to_job(row) if row else None

    def list_events(self, job_id: int, limit: int = 10) -> list[dict[str, Any]]:
        with self._connect() as conn:
            rows = conn.execute(
                "select * from job_events where job_id = ? order by id desc limit ?",
                (job_id, limit),
            ).fetchall()
        return [self._row_to_event(row) for row in rows]

    def list_jobs(self, limit: int = 10) -> list[dict[str, Any]]:
        with self._connect() as conn:
            rows = conn.execute("select * from jobs order by id desc limit ?", (limit,)).fetchall()
        return [self._row_to_job(row) for row in rows]

    def claim_next_job(self) -> dict[str, Any] | None:
        with self._connect() as conn:
            row = conn.execute(
                """
                select * from jobs
                where status = 'queued' and waiting_for_approval = 0
                order by id asc
                limit 1
                """
            ).fetchone()
            if row is None:
                return None

            conn.execute(
                "update jobs set status = 'running', updated_at = ? where id = ?",
                (utc_now(), row["id"]),
            )
            claimed = conn.execute("select * from jobs where id = ?", (row["id"],)).fetchone()
        return self._row_to_job(claimed) if claimed else None

    def save_stage_result(
        self,
        job_id: int,
        stage_name: str,
        payload: dict[str, Any],
        next_stage_index: int,
        waiting_for_approval: bool,
    ) -> dict[str, Any]:
        job = self.get_job(job_id)
        if job is None:
            raise RuntimeError(f"Job {job_id} not found")

        context = job["context"]
        context.setdefault("stages", {})[stage_name] = payload
        if stage_name == "review":
            context["review_count"] = int(payload.get("review_count", context.get("review_count", 0)))
        next_stage = STAGES[next_stage_index] if next_stage_index < len(STAGES) else "completed"
        status = "waiting_approval" if waiting_for_approval else ("completed" if next_stage == "completed" else "queued")

        with self._connect() as conn:
            conn.execute(
                """
                update jobs
                set context_json = ?, current_stage = ?, stage_index = ?, status = ?,
                    waiting_for_approval = ?, rejection_feedback = null, updated_at = ?
                where id = ?
                """,
                (
                    json.dumps(context, ensure_ascii=True),
                    stage_name if waiting_for_approval else next_stage,
                    job["stage_index"] if waiting_for_approval else next_stage_index,
                    status,
                    1 if waiting_for_approval else 0,
                    utc_now(),
                    job_id,
                ),
            )
        return self.get_job(job_id) or {}

    def upsert_stage_context(self, job_id: int, stage_name: str, payload: dict[str, Any]) -> dict[str, Any]:
        job = self.get_job(job_id)
        if job is None:
            raise RuntimeError(f"Job {job_id} not found")

        context = job["context"]
        context.setdefault("stages", {})[stage_name] = payload
        if stage_name == "review":
            context["review_count"] = int(payload.get("review_count", context.get("review_count", 0)))

        with self._connect() as conn:
            conn.execute(
                "update jobs set context_json = ?, updated_at = ? where id = ?",
                (json.dumps(context, ensure_ascii=True), utc_now(), job_id),
            )
        return self.get_job(job_id) or {}

    def set_job_context(self, job_id: int, context: dict[str, Any]) -> dict[str, Any]:
        with self._connect() as conn:
            conn.execute(
                "update jobs set context_json = ?, updated_at = ? where id = ?",
                (json.dumps(context, ensure_ascii=True), utc_now(), job_id),
            )
        return self.get_job(job_id) or {}

    def mark_job_failed(self, job_id: int, error_message: str, stage_name: str) -> None:
        with self._connect() as conn:
            conn.execute(
                "update jobs set status = 'failed', last_error = ?, waiting_for_approval = 0, updated_at = ? where id = ?",
                (error_message, utc_now(), job_id),
            )
        self.add_event(job_id, stage_name, "error", error_message, None)

    def mark_job_cancelled(self, job_id: int) -> bool:
        with self._connect() as conn:
            cursor = conn.execute(
                "update jobs set status = 'cancelled', waiting_for_approval = 0, updated_at = ? where id = ? and status not in ('completed', 'cancelled')",
                (utc_now(), job_id),
            )
        return cursor.rowcount > 0

    def pause_job(self, job_id: int) -> dict[str, Any] | None:
        with self._connect() as conn:
            conn.execute(
                "update jobs set status = 'paused', updated_at = ? where id = ? and status in ('queued', 'running', 'waiting_approval')",
                (utc_now(), job_id),
            )
        job = self.get_job(job_id)
        if job is not None:
            self.add_event(job_id, job["current_stage"], "warning", "Job paused", None)
        return job

    def resume_job(self, job_id: int) -> dict[str, Any] | None:
        job = self.get_job(job_id)
        if job is None:
            return None
        if job["status"] != "paused":
            return job
        next_status = "waiting_approval" if job["waiting_for_approval"] else "queued"
        with self._connect() as conn:
            conn.execute(
                "update jobs set status = ?, updated_at = ? where id = ?",
                (next_status, utc_now(), job_id),
            )
        self.add_event(job_id, job["current_stage"], "info", "Job resumed", None)
        return self.get_job(job_id)

    def approve_job(self, job_id: int) -> dict[str, Any] | None:
        job = self.get_job(job_id)
        if job is None or not job["waiting_for_approval"]:
            return job
        next_stage_index = job["stage_index"] + 1
        next_stage = STAGES[next_stage_index] if next_stage_index < len(STAGES) else "completed"
        status = "completed" if next_stage == "completed" else "queued"
        with self._connect() as conn:
            conn.execute(
                """
                update jobs
                set waiting_for_approval = 0, stage_index = ?, current_stage = ?, status = ?, updated_at = ?
                where id = ?
                """,
                (next_stage_index, next_stage, status, utc_now(), job_id),
            )
        self.add_event(job_id, job["current_stage"], "info", "Stage approved", None)
        return self.get_job(job_id)

    def reject_job(self, job_id: int, feedback: str) -> dict[str, Any] | None:
        job = self.get_job(job_id)
        if job is None or not job["waiting_for_approval"]:
            return job
        with self._connect() as conn:
            conn.execute(
                """
                update jobs
                set waiting_for_approval = 0, status = 'queued', rejection_feedback = ?, updated_at = ?
                where id = ?
                """,
                (feedback, utc_now(), job_id),
            )
        self.add_event(job_id, job["current_stage"], "warning", "Stage rejected", {"feedback": feedback})
        return self.get_job(job_id)

    def save_wizard_state(self, chat_id: int, created_by: int, state: dict[str, Any]) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                insert into chat_wizards (chat_id, created_by, state_json, updated_at)
                values (?, ?, ?, ?)
                on conflict(chat_id) do update set
                  created_by = excluded.created_by,
                  state_json = excluded.state_json,
                  updated_at = excluded.updated_at
                """,
                (chat_id, created_by, json.dumps(state, ensure_ascii=True), utc_now()),
            )

    def get_wizard_state(self, chat_id: int) -> dict[str, Any] | None:
        with self._connect() as conn:
            row = conn.execute("select state_json from chat_wizards where chat_id = ?", (chat_id,)).fetchone()
        if row is None:
            return None
        return json.loads(row["state_json"])

    def clear_wizard_state(self, chat_id: int) -> None:
        with self._connect() as conn:
            conn.execute("delete from chat_wizards where chat_id = ?", (chat_id,))

    def set_runtime_setting(self, key: str, value: Any) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                insert into runtime_settings (key, value_json, updated_at)
                values (?, ?, ?)
                on conflict(key) do update set
                  value_json = excluded.value_json,
                  updated_at = excluded.updated_at
                """,
                (key, json.dumps(value, ensure_ascii=True), utc_now()),
            )

    def get_runtime_setting(self, key: str, default: Any = None) -> Any:
        with self._connect() as conn:
            row = conn.execute("select value_json from runtime_settings where key = ?", (key,)).fetchone()
        if row is None:
            return default
        try:
            return json.loads(row["value_json"])
        except Exception:  # noqa: BLE001
            return default

    def _row_to_job(self, row: sqlite3.Row) -> dict[str, Any]:
        context = json.loads(row["context_json"]) if row["context_json"] else {}
        return {
            "id": row["id"],
            "slug": row["slug"],
            "request_text": row["request_text"],
            "target_users": row["target_users"],
            "constraints_text": row["constraints_text"],
            "status": row["status"],
            "current_stage": row["current_stage"],
            "stage_index": row["stage_index"],
            "waiting_for_approval": bool(row["waiting_for_approval"]),
            "rejection_feedback": row["rejection_feedback"],
            "workspace_path": row["workspace_path"],
            "chat_id": row["chat_id"],
            "created_by": row["created_by"],
            "last_error": row["last_error"],
            "context": context,
            "created_at": row["created_at"],
            "updated_at": row["updated_at"],
        }

    def _row_to_event(self, row: sqlite3.Row) -> dict[str, Any]:
        return {
            "id": row["id"],
            "job_id": row["job_id"],
            "stage_name": row["stage_name"],
            "level": row["level"],
            "message": row["message"],
            "payload": json.loads(row["payload_json"]) if row["payload_json"] else None,
            "created_at": row["created_at"],
        }
