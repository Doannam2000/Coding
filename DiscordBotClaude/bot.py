import asyncio
import contextlib
import json
import os
import re
import subprocess
import threading
import time
import uuid
from collections import defaultdict
from pathlib import Path
from typing import Final

import shutil

CURRENT_WORKSPACE: Path = Path.cwd()
BASE_DIR: Path = Path(__file__).resolve().parent
DATA_DIR: Path = BASE_DIR / ".data"
CHANNEL_PROJECTS_FILE: Path = DATA_DIR / "channel_projects.json"
CHANNEL_LAST_TASKS_FILE: Path = DATA_DIR / "channel_last_tasks.json"
CHANNEL_SESSION_TOKENS_FILE: Path = DATA_DIR / "channel_session_tokens.json"
MAX_PARALLEL_REQUESTS: Final[int] = int(os.getenv("MAX_PARALLEL_REQUESTS", "5"))
GLOBAL_SEMAPHORE = asyncio.Semaphore(MAX_PARALLEL_REQUESTS)
CHANNEL_LOCKS: dict[int, asyncio.Lock] = defaultdict(asyncio.Lock)
CHANNEL_PENDING: dict[int, int] = defaultdict(int)
CHANNEL_ACTIVE: dict[int, bool] = defaultdict(bool)

import discord
from discord import app_commands
from discord.ext import commands
from discord.errors import HTTPException
from dotenv import load_dotenv

from agent_core.loop import AgentConfig, run_android_agent
from agent_core.policy import validate_user_prompt
from agent_core.runner import parse_gradle_output, run_cmd
from agent_core.skills import collect_skills
from agent_core.workspace import create_workspace

load_dotenv()

DISCORD_TOKEN: Final[str | None] = os.getenv("DISCORD_TOKEN")
CLAUDE_CLI: Final[str] = os.getenv("CLAUDE_CLI", "claude")
MAX_OUTPUT_CHARS: Final[int] = int(os.getenv("MAX_OUTPUT_CHARS", "1800"))
CLI_TIMEOUT_SECONDS: Final[int] = int(os.getenv("CLI_TIMEOUT_SECONDS", "0"))
CLI_IDLE_TIMEOUT_SECONDS: Final[int] = int(os.getenv("CLI_IDLE_TIMEOUT_SECONDS", "0"))
CLI_RETRY_ATTEMPTS: Final[int] = int(os.getenv("CLI_RETRY_ATTEMPTS", "4"))
CLI_RETRY_BASE_SECONDS: Final[int] = int(os.getenv("CLI_RETRY_BASE_SECONDS", "2"))
CLI_RETRY_MAX_SECONDS: Final[int] = int(os.getenv("CLI_RETRY_MAX_SECONDS", "15"))
CLI_BUSY_WAIT_SECONDS: Final[int] = int(os.getenv("CLI_BUSY_WAIT_SECONDS", "10"))
CLI_BUSY_MAX_WAIT_SECONDS: Final[int] = int(os.getenv("CLI_BUSY_MAX_WAIT_SECONDS", "300"))
CLI_BUSY_MAX_RETRIES: Final[int] = int(os.getenv("CLI_BUSY_MAX_RETRIES", "5"))
HEARTBEAT_SECONDS: Final[int] = int(os.getenv("HEARTBEAT_SECONDS", "15"))
QUEUE_HEARTBEAT_SECONDS: Final[int] = int(os.getenv("QUEUE_HEARTBEAT_SECONDS", "10"))
MAX_QUEUE_PER_CHANNEL: Final[int] = int(os.getenv("MAX_QUEUE_PER_CHANNEL", "10"))
BOT_SKILLS_DIR: Final[Path] = Path(os.getenv("BOT_SKILLS_DIR", "./skills")).resolve()
HOST_SKILLS_DIR: Final[Path] = Path(os.getenv("HOST_SKILLS_DIR", str(Path.home() / ".agents" / "skills"))).resolve()
MAX_SKILL_TEXT_CHARS: Final[int] = int(os.getenv("MAX_SKILL_TEXT_CHARS", "8000"))
SAFE_MODE: Final[bool] = os.getenv("SAFE_MODE", "true").lower() == "true"
AGENT_MAX_ITERS: Final[int] = int(os.getenv("AGENT_MAX_ITERS", "3"))
ALLOWLIST_REQUIRED: Final[bool] = os.getenv("ALLOWLIST_REQUIRED", "true").lower() == "true"
ALLOWED_USER_IDS: Final[set[int]] = {
    int(x.strip())
    for x in os.getenv("ALLOWED_USER_IDS", "").split(",")
    if x.strip().isdigit()
}

if not DISCORD_TOKEN:
    raise RuntimeError("Missing DISCORD_TOKEN in environment")

intents = discord.Intents.default()
bot = commands.Bot(command_prefix="!", intents=intents, help_command=None)


def is_user_allowed(user_id: int) -> bool:
    if not ALLOWLIST_REQUIRED:
        return True
    return user_id in ALLOWED_USER_IDS


def build_allowed_mentions() -> str:
    if not ALLOWED_USER_IDS:
        return ""
    ids = sorted(ALLOWED_USER_IDS)
    return " ".join(f"<@{user_id}>" for user_id in ids)


def _looks_like_choice_prompt(text: str) -> bool:
    lowered = text.lower()
    return (
        "please pick one option" in lowered
        or "choose \"other\"" in lowered
        or "i need your selection" in lowered
        or "pick one option above" in lowered
        or "chọn mục bạn muốn" in lowered
        or "chọn một option" in lowered
        or "chọn option" in lowered
        or "chọn lựa chọn" in lowered
        or "chọn 1 trong 3" in lowered
        or "select one of" in lowered
    )


def _looks_like_choice_signal(text: str) -> bool:
    return _looks_like_choice_prompt(text)


def _choice_retry_prompt(final_prompt: str) -> str:
    return (
        f"{final_prompt}\n\n"
        "Hệ thống Discord bot không có UI chọn lựa. "
        "Hãy tự chọn phương án được đánh dấu 'Recommended' (nếu có), "
        "nếu không có thì chọn phương án mặc định hợp lý nhất. "
        "Không hỏi lại lựa chọn; hãy thực thi tiếp và trả kết quả cuối cùng."
    )


def _sanitize_user_output(text: str) -> str:
    blocked_markers = [
        "please pick one option",
        "pick one option above",
        "i need your selection",
        "choose \"other\"",
        "mình đã gửi lựa chọn nhanh",
        "chọn mục bạn muốn",
        "chọn một option",
        "chọn 1 trong 3",
        "chọn 1 hướng",
        "chọn option",
        "chọn lựa chọn",
        "nhập “other”",
        'nhập "other"',
    ]
    kept_lines: list[str] = []
    for line in text.splitlines():
        lowered = line.lower().strip()
        if lowered.startswith("[progress]"):
            continue
        if any(marker in lowered for marker in blocked_markers):
            continue
        kept_lines.append(line)
    return "\n".join(kept_lines).strip()


def _strip_internal_output(text: str) -> str:
    return _sanitize_user_output(text)


def _looks_like_transient_cli_error(text: str) -> bool:
    lowered = text.lower()
    return (
        "empty or malformed response" in lowered
        or "malformed response" in lowered
        or "api error" in lowered
        or "http 200" in lowered
        or "proxy or gateway" in lowered
        or "timed out" in lowered
        or "timeout" in lowered
        or "connection reset" in lowered
        or "temporarily unavailable" in lowered
        or "econnreset" in lowered
        or "econnrefused" in lowered
    )


def _retry_wait_seconds(attempt: int) -> int:
    wait = CLI_RETRY_BASE_SECONDS * (2 ** max(0, attempt - 1))
    return min(wait, CLI_RETRY_MAX_SECONDS)


def _is_session_in_use_error(text: str) -> bool:
    lowered = text.lower()
    return "session id" in lowered and "already in use" in lowered


def _run_claude_with_retries(final_prompt: str, cwd: str, phase: str, session_id: str) -> tuple[int, str, str]:
    code = 1
    output = ""
    error = ""
    busy_started = None
    busy_retries = 0

    for attempt in range(1, CLI_RETRY_ATTEMPTS + 1):
        code, output, error = _run_claude_process(final_prompt, cwd, session_id)
        detail = "\n".join(part for part in (error.strip(), output.strip()) if part)
        empty_success = code == 0 and not detail
        if empty_success:
            detail = "[ClaudeCLI] empty success response"

        if _is_session_in_use_error(detail):
            now = time.monotonic()
            busy_retries += 1
            if busy_started is None:
                busy_started = now
            busy_elapsed = int(now - busy_started)

            if CLI_BUSY_MAX_RETRIES > 0 and busy_retries >= CLI_BUSY_MAX_RETRIES:
                print(
                    f"[ClaudeCLI] SESSION_CONFLICT_RETRY_LIMIT phase={phase} retries={busy_retries} session={session_id}"
                )
                break

            if CLI_BUSY_MAX_WAIT_SECONDS > 0 and busy_elapsed >= CLI_BUSY_MAX_WAIT_SECONDS:
                print(
                    f"[ClaudeCLI] SESSION_CONFLICT_TIMEOUT phase={phase} waited={busy_elapsed}s session={session_id}"
                )
                break

            wait = max(1, CLI_BUSY_WAIT_SECONDS)
            print(
                f"[ClaudeCLI] SESSION_CONFLICT phase={phase} keep_session wait={wait}s waited={busy_elapsed}s retries={busy_retries} session={session_id}"
            )
            time.sleep(wait)
            continue

        busy_retries = 0
        busy_started = None
'}+''}】【：】【“】【commentary to=functions.Edit  天天彩票中大奖json file_path=
        if empty_success or _looks_like_transient_cli_error(detail):
            if attempt < CLI_RETRY_ATTEMPTS:
                wait = _retry_wait_seconds(attempt)
                print(
                    f"[ClaudeCLI] RETRY phase={phase} attempt={attempt + 1}/{CLI_RETRY_ATTEMPTS} after={wait}s reason={detail[:200]}"
                )
                time.sleep(wait)
                continue
            break

        return code, output, error

    return code, output, error


def _run_claude_process(final_prompt: str, cwd: str, session_id: str) -> tuple[int, str, str]:
    start_ts = time.monotonic()
    print(f"[ClaudeCLI] START cwd={cwd}")

    process = subprocess.Popen(
        [CLAUDE_CLI, "--session-id", session_id, "-p", final_prompt],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
        cwd=cwd,
    )

    stdout_chunks: list[str] = []
    stderr_chunks: list[str] = []
    out_bytes = 0
    err_bytes = 0
    lock = threading.Lock()

    def read_stream(stream, collector: list[str], stream_name: str) -> None:
        nonlocal out_bytes, err_bytes
        if stream is None:
            return
        for line in iter(stream.readline, ""):
            collector.append(line)
            line_bytes = len(line.encode("utf-8", errors="replace"))
            with lock:
                if stream_name == "stdout":
                    out_bytes += line_bytes
                    total = out_bytes
                else:
                    err_bytes += line_bytes
                    total = err_bytes
            print(f"[ClaudeCLI] RECV {stream_name} +{line_bytes}B total={total}B")
        stream.close()

    t_out = threading.Thread(target=read_stream, args=(process.stdout, stdout_chunks, "stdout"), daemon=True)
    t_err = threading.Thread(target=read_stream, args=(process.stderr, stderr_chunks, "stderr"), daemon=True)
    t_out.start()
    t_err.start()

    last_activity_ts = start_ts
    last_seen_out = 0
    last_seen_err = 0

    while process.poll() is None:
        elapsed = int(time.monotonic() - start_ts)
        now = time.monotonic()
        with lock:
            current_out = out_bytes
            current_err = err_bytes

        if current_out != last_seen_out or current_err != last_seen_err:
            last_activity_ts = now
            last_seen_out = current_out
            last_seen_err = current_err

        idle_seconds = int(now - last_activity_ts)
        print(
            f"[ClaudeCLI] THINKING elapsed={elapsed}s idle={idle_seconds}s stdout={current_out}B stderr={current_err}B"
        )

        if CLI_IDLE_TIMEOUT_SECONDS > 0 and idle_seconds >= CLI_IDLE_TIMEOUT_SECONDS:
            process.kill()
            t_out.join(timeout=1)
            t_err.join(timeout=1)
            raise subprocess.TimeoutExpired([CLAUDE_CLI, "-p", final_prompt], CLI_IDLE_TIMEOUT_SECONDS)

        if CLI_TIMEOUT_SECONDS > 0 and elapsed >= CLI_TIMEOUT_SECONDS:
            process.kill()
            t_out.join(timeout=1)
            t_err.join(timeout=1)
            raise subprocess.TimeoutExpired([CLAUDE_CLI, "-p", final_prompt], CLI_TIMEOUT_SECONDS)

        time.sleep(10)

    t_out.join(timeout=2)
    t_err.join(timeout=2)

    elapsed = int(time.monotonic() - start_ts)
    print(f"[ClaudeCLI] END code={process.returncode} elapsed={elapsed}s")

    return process.returncode, "".join(stdout_chunks).strip(), "".join(stderr_chunks).strip()


def run_claude_sync(
    prompt: str,
    workspace: Path | None = None,
    session_scope: str | None = None,
    session_token: str = "default",
) -> str:
    policy = validate_user_prompt(prompt, safe_mode=SAFE_MODE)
    if not policy.ok:
        return f"[BLOCKED] {policy.reason}"

    skill_context = collect_skills(BOT_SKILLS_DIR, HOST_SKILLS_DIR, MAX_SKILL_TEXT_CHARS)
    final_prompt = f"{skill_context}\n\nYêu cầu người dùng:\n{prompt}" if skill_context else prompt
    cwd = str(workspace or CURRENT_WORKSPACE)

    progress_logs: list[str] = []
    progress_logs.append(f"[progress] phase=initial phase_runs=1/{CLI_RETRY_ATTEMPTS}")
    scope = session_scope or cwd
    session_seed = f"discordbot:{scope}:{session_token}"
    session_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, session_seed))
    code, output, error = _run_claude_with_retries(final_prompt, cwd, "initial", session_id)
    detail = error or output or "Unknown CLI error"
    if code != 0:
        return "\n".join(progress_logs) + f"\n[Claude CLI error] {detail}"
    if not output.strip() and not error.strip():
        return "\n".join(progress_logs) + "\n[Claude CLI error] Empty response from CLI after retries"

    auto_choice_passes = 3
    choice_resolved = False
    for pass_idx in range(1, auto_choice_passes + 1):
        if not _looks_like_choice_signal(output):
            choice_resolved = True
            break
        progress_logs.append(f"[progress] detected choice prompt, auto-select pass {pass_idx}/{auto_choice_passes}")
        print(f"[ClaudeCLI] Auto-resolving choice prompt with Recommended option (pass {pass_idx})")
        code, output, error = _run_claude_with_retries(
            _choice_retry_prompt(final_prompt),
            cwd,
            f"choice-{pass_idx}",
            session_id,
        )
        if code != 0:
            detail = error or output or "Unknown CLI error"
            progress_logs.append(f"[progress] auto-choice failed ({detail[:180]})")
            if _looks_like_transient_cli_error(detail):
                print(f"[ClaudeCLI] Choice-resolve transient error: {detail[:200]}")
            return "\n".join(progress_logs) + f"\n[Claude CLI error] {detail}"

    if _looks_like_choice_signal(output):
        progress_logs.append("[progress] auto-choice exhausted; returned best-effort output")
        output = _sanitize_user_output(output)
    elif choice_resolved:
        progress_logs.append("[progress] auto-choice resolved successfully")
    else:
        progress_logs.append("[progress] no choice prompt detected")

    output = _sanitize_user_output(output)

    final_output = _strip_internal_output(output) or "[Claude CLI] Không có output."
    return final_output


    return final_output


def chunk_text(text: str, max_len: int) -> list[str]:
    chunks: list[str] = []
    remaining = text
    while len(remaining) > max_len:
        split_at = remaining.rfind("\n", 0, max_len)
        if split_at <= 0:
            split_at = max_len
        chunks.append(remaining[:split_at])
        remaining = remaining[split_at:].lstrip("\n")
    if remaining:
        chunks.append(remaining)
    return chunks


def parse_models_from_help(text: str) -> list[str]:
    models: list[str] = []
    alias_match = re.search(r"--model <model>.*?\(e\.g\. '([^']+)' or '([^']+)'\)", text, flags=re.IGNORECASE)
    if alias_match:
        models.extend([alias_match.group(1).strip(), alias_match.group(2).strip()])

    for full_name in re.findall(r"'(claude-[^']+)'", text):
        value = full_name.strip()
        if value and value not in models:
            models.append(value)

    for m in ["haiku", "sonnet", "opus"]:
        if m in text.lower() and m not in models:
            models.append(m)

    return models


def get_available_models() -> list[str]:
    result = subprocess.run(
        [CLAUDE_CLI, "--help"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=CLI_TIMEOUT_SECONDS,
        check=False,
    )
    text = (result.stdout or "") + "\n" + (result.stderr or "")
    models = parse_models_from_help(text)
    if not models:
        return ["opus", "sonnet", "haiku"]
    return models


def set_project_model(model: str) -> tuple[bool, str]:
    config_dir = Path(__file__).resolve().parent / ".claude"
    config_path = config_dir / "settings.local.json"
    config_dir.mkdir(parents=True, exist_ok=True)

    data: dict = {}
    if config_path.exists():
        try:
            data = json.loads(config_path.read_text(encoding="utf-8"))
            if not isinstance(data, dict):
                data = {}
        except Exception:
            data = {}

    data["model"] = model
    config_path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return True, str(config_path)


def load_channel_projects() -> dict[str, str]:
    if not CHANNEL_PROJECTS_FILE.exists():
        return {}
    try:
        data = json.loads(CHANNEL_PROJECTS_FILE.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            return {}
        return {str(k): str(v) for k, v in data.items()}
    except Exception:
        return {}


def save_channel_projects(mapping: dict[str, str]) -> str:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    CHANNEL_PROJECTS_FILE.write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return str(CHANNEL_PROJECTS_FILE)


def load_channel_last_tasks() -> dict[str, str]:
    if not CHANNEL_LAST_TASKS_FILE.exists():
        return {}
    try:
        data = json.loads(CHANNEL_LAST_TASKS_FILE.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            return {}
        return {str(k): str(v) for k, v in data.items()}
    except Exception:
        return {}


def save_channel_last_tasks(mapping: dict[str, str]) -> str:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    CHANNEL_LAST_TASKS_FILE.write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return str(CHANNEL_LAST_TASKS_FILE)


def load_channel_session_tokens() -> dict[str, str]:
    if not CHANNEL_SESSION_TOKENS_FILE.exists():
        return {}
    try:
        data = json.loads(CHANNEL_SESSION_TOKENS_FILE.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            return {}
        return {str(k): str(v) for k, v in data.items()}
    except Exception:
        return {}


def save_channel_session_tokens(mapping: dict[str, str]) -> str:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    CHANNEL_SESSION_TOKENS_FILE.write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return str(CHANNEL_SESSION_TOKENS_FILE)


def get_channel_session_token(channel_id: int) -> str:
    mapping = load_channel_session_tokens()
    token = mapping.get(str(channel_id), "default")
    if not token:
        return "default"
    return token


def rotate_channel_session_token(channel_id: int) -> tuple[str, str]:
    mapping = load_channel_session_tokens()
    token = str(uuid.uuid4())
    mapping[str(channel_id)] = token
    store = save_channel_session_tokens(mapping)
    return token, store


def list_all_session_tokens() -> dict[str, str]:
    return load_channel_session_tokens()


def set_channel_session_token(channel_id: int, token: str) -> tuple[bool, str]:
    normalized = token.strip()
    if not normalized:
        return False, "Session token rỗng."
    mapping = load_channel_session_tokens()
    mapping[str(channel_id)] = normalized
    store = save_channel_session_tokens(mapping)
    return True, store


def clear_channel_session_token(channel_id: int) -> tuple[bool, str]:
    mapping = load_channel_session_tokens()
    key = str(channel_id)
    if key not in mapping:
        return False, "Channel này chưa có session token riêng."
    removed = mapping.pop(key)
    store = save_channel_session_tokens(mapping)
    return True, f"removed={removed[:8]}... saved={store}"


def set_channel_last_task(channel_id: int, prompt: str) -> str:
    mapping = load_channel_last_tasks()
    mapping[str(channel_id)] = prompt
    return save_channel_last_tasks(mapping)


def get_channel_last_task(channel_id: int) -> str | None:
    mapping = load_channel_last_tasks()
    value = mapping.get(str(channel_id))
    if not value:
        return None
    return value


def get_channel_workspace(channel_id: int) -> Path | None:
    mapping = load_channel_projects()
    value = mapping.get(str(channel_id))
    if not value:
        return None
    p = Path(value)
    if not p.exists() or not p.is_dir():
        return None
    return p


def set_channel_workspace(channel_id: int, path: str) -> tuple[bool, str]:
    raw_path = path.strip().strip('"').strip("'")
    if not raw_path:
        return False, "Path rỗng."
    p = Path(raw_path).expanduser()
    if not p.is_absolute():
        p = (BASE_DIR / p).resolve()
    else:
        p = p.resolve()
    if not p.exists() or not p.is_dir():
        return False, f"Path không tồn tại hoặc không phải thư mục: {p}"
    mapping = load_channel_projects()
    mapping[str(channel_id)] = str(p)
    store = save_channel_projects(mapping)
    return True, f"{p} (saved: {store})"


def clear_channel_workspace(channel_id: int) -> tuple[bool, str]:
    mapping = load_channel_projects()
    key = str(channel_id)
    if key not in mapping:
        return False, "Channel này chưa có workspace để xóa."
    removed = mapping.pop(key)
    store = save_channel_projects(mapping)
    return True, f"Đã xóa `{removed}` (saved: {store})"


async def check_interaction_permission(interaction: discord.Interaction) -> bool:
    if not interaction.user or not is_user_allowed(interaction.user.id):
        await interaction.response.send_message("Bạn không có quyền dùng bot này.", ephemeral=True)
        return False
    return True


async def safe_followup_send(interaction: discord.Interaction, content: str) -> None:
    try:
        await interaction.followup.send(content)
        return
    except HTTPException as exc:
        if exc.code != 50027:
            raise
    channel = interaction.channel
    if channel is None:
        raise RuntimeError("Không gửi được kết quả: channel không tồn tại.")
    await channel.send(content)


async def _queue_wait_heartbeat(interaction: discord.Interaction, channel_id: int, started_ts: float) -> None:
    while CHANNEL_PENDING[channel_id] > 0:
        waited = int(time.monotonic() - started_ts)
        await safe_followup_send(
            interaction,
            f"[queue] Channel đang bận, chờ task trước hoàn tất... waited={waited}s pending={CHANNEL_PENDING[channel_id]}",
        )
        await asyncio.sleep(max(3, QUEUE_HEARTBEAT_SECONDS))


async def _run_waiting_claude(
    interaction: discord.Interaction,
    channel_id: int,
    prompt: str,
    active_workspace: Path,
    session_scope: str,
) -> str:
    queue_started = time.monotonic()
    heartbeat_task: asyncio.Task | None = None

    if CHANNEL_ACTIVE[channel_id] or CHANNEL_PENDING[channel_id] > 0:
        CHANNEL_PENDING[channel_id] += 1
        heartbeat_task = asyncio.create_task(_queue_wait_heartbeat(interaction, channel_id, queue_started))

    try:
        async with GLOBAL_SEMAPHORE:
            async with CHANNEL_LOCKS[channel_id]:
                if CHANNEL_PENDING[channel_id] > 0:
                    CHANNEL_PENDING[channel_id] = max(0, CHANNEL_PENDING[channel_id] - 1)

                CHANNEL_ACTIVE[channel_id] = True
                await safe_followup_send(interaction, "[thinking] Đã vào session, đang xử lý bằng Claude CLI...")
                session_token = await asyncio.to_thread(get_channel_session_token, channel_id)
                output = await asyncio.to_thread(
                    run_claude_sync,
                    prompt,
                    active_workspace,
                    session_scope,
                    session_token,
                )
                return output
    finally:
        CHANNEL_ACTIVE[channel_id] = False
        if heartbeat_task is not None:
            heartbeat_task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await heartbeat_task


@bot.event
async def on_ready() -> None:
    synced_global = await bot.tree.sync()
    guild_count = 0
    for guild in bot.guilds:
        try:
            await bot.tree.sync(guild=guild)
            guild_count += 1
        except Exception:
            continue
    print(f"Logged in as {bot.user} (id={bot.user.id})")
    print(f"Slash commands synced. global={len(synced_global)} guilds={guild_count}")


@bot.tree.command(name="whoami", description="Hiện Discord user id của bạn")
async def whoami(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return
    await interaction.response.send_message(f"Your Discord user id: `{interaction.user.id}`", ephemeral=True)


@bot.tree.command(name="chat", description="Gửi prompt tới Claude CLI")
@app_commands.describe(prompt="Nội dung prompt")
async def chat(interaction: discord.Interaction, prompt: str) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    workspace = get_channel_workspace(channel_id)
    active_workspace = workspace or CURRENT_WORKSPACE

    await interaction.response.defer(thinking=True)
    _ = await asyncio.to_thread(set_channel_last_task, channel_id, prompt)
    try:
        output = await _run_waiting_claude(interaction, channel_id, prompt, active_workspace, f"channel:{channel_id}")
    except subprocess.TimeoutExpired:
        await safe_followup_send(interaction, f"Claude CLI timeout sau {CLI_TIMEOUT_SECONDS}s.")
        return
    except Exception as exc:
        await safe_followup_send(interaction, f"Lỗi khi gọi Claude CLI: {exc}")
        return

    if len(output) > MAX_OUTPUT_CHARS:
        output = output[:MAX_OUTPUT_CHARS] + "\n... (đã cắt ngắn)"

    message = f"```\n{output}\n```"
    mention_text = build_allowed_mentions()

    if len(message) <= 2000:
        if mention_text:
            await safe_followup_send(interaction, f"{mention_text}\n{message}")
        else:
            await safe_followup_send(interaction, message)
        return

    if len(message) > 2000:
        if mention_text:
            await safe_followup_send(interaction, f"{mention_text}\nKết quả dài, gửi theo nhiều phần:")
        else:
            await safe_followup_send(interaction, "Kết quả dài, gửi theo nhiều phần:")
        for idx, part in enumerate(chunk_text(output, 1800), start=1):
            await safe_followup_send(interaction, f"Phần {idx}\n```\n{part}\n```")


@bot.tree.command(name="continue", description="Tiếp tục task gần nhất của channel")
async def continue_task(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    last_prompt = await asyncio.to_thread(get_channel_last_task, channel_id)
    if not last_prompt:
        await interaction.response.send_message(
            "Channel này chưa có task trước đó. Hãy dùng `/chat` trước.",
            ephemeral=True,
        )
        return

    workspace = get_channel_workspace(channel_id)
    active_workspace = workspace or CURRENT_WORKSPACE
    continue_prompt = (
        "Tiếp tục task trước đó từ trạng thái mới nhất, không lặp lại phần đã xong.\n\n"
        f"Task trước đó:\n{last_prompt}"
    )

    await interaction.response.defer(thinking=True)
    try:
        output = await _run_waiting_claude(interaction, channel_id, continue_prompt, active_workspace, f"channel:{channel_id}")
    except subprocess.TimeoutExpired:
        await safe_followup_send(interaction, f"Claude CLI timeout sau {CLI_TIMEOUT_SECONDS}s.")
        return
    except Exception as exc:
        await safe_followup_send(interaction, f"Lỗi khi tiếp tục task: {exc}")
        return

    message = f"```\n{output}\n```"
    mention_text = build_allowed_mentions()

    if len(message) <= 2000:
        if mention_text:
            await safe_followup_send(interaction, f"{mention_text}\n{message}")
        else:
            await safe_followup_send(interaction, message)
        return

    if mention_text:
        await safe_followup_send(interaction, f"{mention_text}\nKết quả dài, gửi theo nhiều phần:")
    else:
        await safe_followup_send(interaction, "Kết quả dài, gửi theo nhiều phần:")
    for idx, part in enumerate(chunk_text(output, 1800), start=1):
        await safe_followup_send(interaction, f"Phần {idx}\n```\n{part}\n```")


@bot.tree.command(name="android_agent", description="Chạy Android coding agent loop")
@app_commands.describe(prompt="Task Android cần thực hiện")
async def android_agent(interaction: discord.Interaction, prompt: str) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    workspace = get_channel_workspace(channel_id)
    if workspace is None:
        await interaction.response.send_message(
            "Channel này chưa setup project. Dùng `/setup_project path:<đường_dẫn>` trước.",
            ephemeral=True,
        )
        return

    await interaction.response.defer(thinking=True)
    ws = create_workspace()
    cfg = AgentConfig(
        claude_cli=CLAUDE_CLI,
        timeout_seconds=CLI_TIMEOUT_SECONDS,
        safe_mode=SAFE_MODE,
        bot_skills_dir=BOT_SKILLS_DIR,
        host_skills_dir=HOST_SKILLS_DIR,
        max_skill_text_chars=MAX_SKILL_TEXT_CHARS,
        max_iters=AGENT_MAX_ITERS,
    )

    try:
        async with GLOBAL_SEMAPHORE:
            async with CHANNEL_LOCKS[channel_id]:
                report = await asyncio.to_thread(run_android_agent, prompt, cfg, workspace)
    except Exception as exc:
        await interaction.followup.send(f"Lỗi android agent: {exc}")
        return

    output = report.final_output or "[No output]"
    if len(output) > MAX_OUTPUT_CHARS:
        output = output[:MAX_OUTPUT_CHARS] + "\n... (đã cắt ngắn)"

    mention_text = build_allowed_mentions()
    body = f"Workspace: `{ws.root}`\nSummary: {report.summary}\n```\n{output}\n```"
    if mention_text:
        await interaction.followup.send(f"{mention_text}\n{body}")
    else:
        await interaction.followup.send(body)


@bot.tree.command(name="selfcheck", description="Chạy lint + test nhanh")
async def selfcheck(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    workspace = get_channel_workspace(channel_id)
    if workspace is None:
        await interaction.response.send_message(
            "Channel này chưa setup project. Dùng `/setup_project path:<đường_dẫn>` trước.",
            ephemeral=True,
        )
        return

    await interaction.response.defer(thinking=True)
    checks = [["./gradlew", "lint"], ["./gradlew", "test"]]
    lines: list[str] = []

    for cmd in checks:
        try:
            async with GLOBAL_SEMAPHORE:
                async with CHANNEL_LOCKS[channel_id]:
                    r = await asyncio.to_thread(run_cmd, cmd, CLI_TIMEOUT_SECONDS, str(workspace))
        except Exception as exc:
            lines.append(f"$ {' '.join(cmd)} => ERROR: {exc}")
            continue
        parsed = parse_gradle_output((r.stdout + "\n" + r.stderr).strip())
        status = "OK" if r.code == 0 else f"FAIL({r.code})"
        lines.append(f"$ {' '.join(cmd)} => {status}\n{parsed[:500]}")

    text = "\n\n".join(lines)
    if len(text) > MAX_OUTPUT_CHARS:
        text = text[:MAX_OUTPUT_CHARS] + "\n... (đã cắt ngắn)"
    await interaction.followup.send(f"```\n{text}\n```")


@bot.tree.command(name="setup_project", description="Gán project path cho channel hiện tại")
@app_commands.describe(path="Đường dẫn thư mục project (absolute path)")
async def setup_project(interaction: discord.Interaction, path: str) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    ok, msg = await asyncio.to_thread(set_channel_workspace, channel_id, path)
    if not ok:
        await interaction.response.send_message(f"Setup thất bại: {msg}", ephemeral=True)
        return

    await interaction.response.send_message(f"Đã setup project cho channel này: `{msg}`", ephemeral=True)


@bot.tree.command(name="workspace", description="Xem project hiện tại của channel")
async def workspace(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    ws = get_channel_workspace(channel_id)
    if ws is None:
        await interaction.response.send_message(
            "Channel này chưa có project. Dùng `/setup_project path:<đường_dẫn>` để gán.",
            ephemeral=True,
        )
        return

    await interaction.response.send_message(f"Workspace channel hiện tại: `{ws}`", ephemeral=True)


@bot.tree.command(name="clear_workspace", description="Xóa project của channel hiện tại")
async def clear_workspace(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    ok, msg = await asyncio.to_thread(clear_channel_workspace, channel_id)
    if not ok:
        await interaction.response.send_message(msg, ephemeral=True)
        return

    await interaction.response.send_message(msg, ephemeral=True)


@bot.tree.command(name="new_session", description="Tạo session mới cho channel hiện tại")
async def new_session(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    if CHANNEL_ACTIVE[channel_id] or CHANNEL_PENDING[channel_id] > 0:
        await interaction.response.send_message(
            "Channel đang chạy task hoặc còn task chờ. Hãy đợi queue trống rồi tạo session mới.",
            ephemeral=True,
        )
        return

    token, store = await asyncio.to_thread(rotate_channel_session_token, channel_id)
    await interaction.response.send_message(
        f"Đã tạo session mới cho channel này. token={token[:8]}... (saved: {store})",
        ephemeral=True,
    )


@bot.tree.command(name="clear_session", description="Xóa session hiện tại của channel (reset về mặc định)")
async def clear_session(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    if CHANNEL_ACTIVE[channel_id] or CHANNEL_PENDING[channel_id] > 0:
        await interaction.response.send_message(
            "Channel đang chạy task hoặc còn task chờ. Hãy đợi queue trống rồi xóa session.",
            ephemeral=True,
        )
        return

    ok, msg = await asyncio.to_thread(clear_channel_session_token, channel_id)
    if not ok:
        await interaction.response.send_message(msg, ephemeral=True)
        return

    await interaction.response.send_message(f"Đã reset session channel. {msg}", ephemeral=True)


@bot.tree.command(name="list_sessions", description="Liệt kê toàn bộ session token đã lưu")
async def list_sessions(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    data = await asyncio.to_thread(list_all_session_tokens)
    if not data:
        await interaction.response.send_message("Chưa có session token nào được lưu.", ephemeral=True)
        return

    lines = [f"channel={k} token={v[:8]}..." for k, v in sorted(data.items(), key=lambda x: x[0])]
    text = "\n".join(lines)
    if len(text) > 1800:
        text = text[:1800] + "\n... (đã cắt ngắn)"
    await interaction.response.send_message(f"```\n{text}\n```", ephemeral=True)


@bot.tree.command(name="current_session", description="Xem session token hiện tại của channel")
async def current_session(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    token = await asyncio.to_thread(get_channel_session_token, channel_id)
    await interaction.response.send_message(
        f"Session hiện tại của channel `{channel_id}`: token={token[:8]}...",
        ephemeral=True,
    )


@bot.tree.command(name="use_session", description="Chọn session token đã tồn tại cho channel hiện tại")
@app_commands.describe(token="Session token cần dùng")
async def use_session(interaction: discord.Interaction, token: str) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    if CHANNEL_ACTIVE[channel_id] or CHANNEL_PENDING[channel_id] > 0:
        await interaction.response.send_message(
            "Channel đang chạy task hoặc còn task chờ. Hãy đợi queue trống rồi đổi session.",
            ephemeral=True,
        )
        return

    ok, store = await asyncio.to_thread(set_channel_session_token, channel_id, token)
    if not ok:
        await interaction.response.send_message(store, ephemeral=True)
        return

    await interaction.response.send_message(
        f"Đã đổi session channel sang token={token[:8]}... (saved: {store})",
        ephemeral=True,
    )


@bot.tree.command(name="models", description="Lấy danh sách model từ Claude CLI")
async def models(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    await interaction.response.defer(ephemeral=True, thinking=True)
    try:
        items = await asyncio.to_thread(get_available_models)
    except Exception as exc:
        await interaction.followup.send(f"Không lấy được models: {exc}", ephemeral=True)
        return

    text = "\n".join(f"- `{m}`" for m in items)
    await interaction.followup.send(f"Models từ CLI:\n{text}\n\nDùng `/set_model model:<tên>` để đổi.", ephemeral=True)


@bot.tree.command(name="set_model", description="Đổi model mặc định cho project bot")
@app_commands.describe(model="Tên model từ /models")
async def set_model(interaction: discord.Interaction, model: str) -> None:
    if not await check_interaction_permission(interaction):
        return

    await interaction.response.defer(ephemeral=True, thinking=True)
    try:
        available = await asyncio.to_thread(get_available_models)
    except Exception as exc:
        await interaction.followup.send(f"Không kiểm tra được models: {exc}", ephemeral=True)
        return

    normalized = model.strip()
    if normalized not in available:
        choices = ", ".join(available)
        await interaction.followup.send(
            f"Model `{normalized}` không hợp lệ. Chọn một trong: {choices}",
            ephemeral=True,
        )
        return

    try:
        _, config_path = await asyncio.to_thread(set_project_model, normalized)
    except Exception as exc:
        await interaction.followup.send(f"Không set được model: {exc}", ephemeral=True)
        return

    await interaction.followup.send(
        f"Đã set model mặc định = `{normalized}`\nConfig: `{config_path}`",
        ephemeral=True,
    )


@bot.tree.command(name="cicd", description="Trigger Android Firebase Distribution workflow")
@app_commands.describe(ref="Branch/tag để chạy workflow (mặc định: current branch)")
async def cicd(interaction: discord.Interaction, ref: str | None = None) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    workspace = get_channel_workspace(channel_id)
    if workspace is None:
        await interaction.response.send_message(
            "Channel này chưa setup project. Dùng `/setup_project path:<đường_dẫn>` trước.",
            ephemeral=True,
        )
        return

    if shutil.which("gh") is None:
        await interaction.response.send_message("Thiếu GitHub CLI (`gh`). Cài `gh` rồi thử lại.", ephemeral=True)
        return

    await interaction.response.defer(ephemeral=True, thinking=True)

    cmd = [
        "gh",
        "workflow",
        "run",
        "android-firebase-distribute.yml",
    ]
    normalized_ref = (ref or "").strip()
    if normalized_ref:
        cmd.extend(["--ref", normalized_ref])

    try:
        async with GLOBAL_SEMAPHORE:
            async with CHANNEL_LOCKS[channel_id]:
                result = await asyncio.to_thread(run_cmd, cmd, CLI_TIMEOUT_SECONDS, str(workspace))
    except Exception as exc:
        await interaction.followup.send(f"Trigger CI/CD thất bại: {exc}", ephemeral=True)
        return

    if result.code != 0:
        detail = (result.stderr or result.stdout or "Unknown error").strip()
        await interaction.followup.send(f"Trigger CI/CD lỗi (code {result.code}):\n```\n{detail[:1500]}\n```", ephemeral=True)
        return

    repo_info = await asyncio.to_thread(run_cmd, ["gh", "repo", "view", "--json", "url", "-q", ".url"], 60, str(workspace))
    repo_url = (repo_info.stdout or "").strip()
    actions_url = f"{repo_url}/actions/workflows/android-firebase-distribute.yml" if repo_url else ""

    if actions_url:
        await interaction.followup.send(
            f"Đã trigger CI/CD thành công.\nWorkflow: {actions_url}",
            ephemeral=True,
        )
    else:
        await interaction.followup.send("Đã trigger CI/CD thành công.", ephemeral=True)


@bot.tree.command(name="cicd_status", description="Xem status workflow Android Firebase Distribution")
async def cicd_status(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    channel_id = interaction.channel_id
    if channel_id is None:
        await interaction.response.send_message("Không xác định được channel.", ephemeral=True)
        return

    workspace = get_channel_workspace(channel_id)
    if workspace is None:
        await interaction.response.send_message(
            "Channel này chưa setup project. Dùng `/setup_project path:<đường_dẫn>` trước.",
            ephemeral=True,
        )
        return

    if shutil.which("gh") is None:
        await interaction.response.send_message("Thiếu GitHub CLI (`gh`). Cài `gh` rồi thử lại.", ephemeral=True)
        return

    await interaction.response.defer(ephemeral=True, thinking=True)

    try:
        repo_info = await asyncio.to_thread(run_cmd, ["gh", "repo", "view", "--json", "nameWithOwner", "-q", ".nameWithOwner"], 60, str(workspace))
        repo_name = (repo_info.stdout or "").strip()
        if not repo_name:
            raise RuntimeError("Không lấy được repo name")

        list_cmd = [
            "gh",
            "run",
            "list",
            "--workflow",
            "android-firebase-distribute.yml",
            "--limit",
            "1",
            "--json",
            "databaseId,displayTitle,status,conclusion,createdAt,event,headBranch,url",
        ]
        runs = await asyncio.to_thread(run_cmd, list_cmd, CLI_TIMEOUT_SECONDS, str(workspace))
    except Exception as exc:
        await interaction.followup.send(f"Không lấy được CI/CD status: {exc}", ephemeral=True)
        return

    raw = (runs.stdout or "").strip()
    if not raw:
        await interaction.followup.send("Chưa có workflow run nào.", ephemeral=True)
        return

    try:
        payload = json.loads(raw)
        run = payload[0] if isinstance(payload, list) and payload else None
    except Exception:
        run = None

    if not run:
        await interaction.followup.send(f"Không parse được status:\n```\n{raw[:1500]}\n```", ephemeral=True)
        return

    status = str(run.get("status") or "unknown")
    conclusion = str(run.get("conclusion") or "running")
    branch = str(run.get("headBranch") or "-")
    title = str(run.get("displayTitle") or "android-firebase-distribute.yml")
    url = str(run.get("url") or "")
    body = (
        f"Workflow: `{title}`\n"
        f"Branch: `{branch}`\n"
        f"Status: `{status}`\n"
        f"Conclusion: `{conclusion}`\n"
        f"URL: {url or 'N/A'}"
    )
    await interaction.followup.send(body, ephemeral=True)


@bot.tree.command(name="helpme", description="Hiện các lệnh bot")
async def helpme(interaction: discord.Interaction) -> None:
    if not await check_interaction_permission(interaction):
        return

    await interaction.response.send_message(
        "Lệnh có sẵn:\n"
        "- `/whoami`\n"
        "- `/chat`\n"
        "- `/continue`\n"
        "- `/android_agent`\n"
        "- `/selfcheck`\n"
        "- `/setup_project`\n"
        "- `/workspace`\n"
        "- `/clear_workspace`\n"
        "- `/models`\n"
        "- `/set_model`\n"
        "- `/cicd`\n"
        "- `/cicd_status`\n"
        "Thiết lập quyền bằng env: `ALLOWED_USER_IDS=123,456`",
        ephemeral=True,
    )


if __name__ == "__main__":
    bot.run(DISCORD_TOKEN)
