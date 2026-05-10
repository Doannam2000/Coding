from dataclasses import dataclass
from pathlib import Path

from .policy import validate_user_prompt
from .runner import classify_errors, format_error_report, parse_gradle_output, run_cmd
from .skills import collect_skills


@dataclass
class AgentConfig:
    claude_cli: str
    timeout_seconds: int
    safe_mode: bool
    bot_skills_dir: Path
    host_skills_dir: Path
    max_skill_text_chars: int
    max_iters: int


@dataclass
class AgentReport:
    final_output: str
    summary: str


def run_android_agent(user_prompt: str, cfg: AgentConfig, workspace: Path | None = None) -> AgentReport:
    v = validate_user_prompt(user_prompt, safe_mode=cfg.safe_mode)
    if not v.ok:
        return AgentReport(final_output=f"[BLOCKED] {v.reason}", summary="Blocked by policy")

    skill_context = collect_skills(cfg.bot_skills_dir, cfg.host_skills_dir, cfg.max_skill_text_chars)
    system_block = (
        "Bạn là Android coding agent. Luôn theo loop: PLAN -> CODE -> BUILD -> ANALYZE -> FIX -> REBUILD -> REVIEW. "
        "Dùng Kotlin, Jetpack Compose Material3, MVVM, StateFlow, Coroutines. "
        "Luôn đảm bảo UI states: loading/error/empty/success. "
        "Không sửa file không liên quan. Luôn liệt kê changed files."
    )
    prompt = f"{system_block}\n\n{skill_context}\n\nYêu cầu:\n{user_prompt}" if skill_context else f"{system_block}\n\nYêu cầu:\n{user_prompt}"

    last_output = ""
    last_err = ""
    for _ in range(cfg.max_iters):
        r = run_cmd([cfg.claude_cli, "-p", prompt], timeout=cfg.timeout_seconds, cwd=str(workspace) if workspace else None)
        if r.code != 0:
            return AgentReport(final_output=f"[Claude CLI error] {r.stderr or r.stdout}", summary="CLI failed")

        last_output = r.stdout
        build = run_cmd(["./gradlew", "assembleDebug"], timeout=cfg.timeout_seconds, cwd=str(workspace) if workspace else None)
        if build.code == 0:
            review_prompt = "Thực hiện REVIEW ngắn: kiểm tra UI states loading/error/empty/success, không đụng file không liên quan, liệt kê changed files, kết luận DONE."
            rv = run_cmd([cfg.claude_cli, "-p", review_prompt], timeout=cfg.timeout_seconds, cwd=str(workspace) if workspace else None)
            merged = (last_output + "\n\n" + (rv.stdout or "")).strip()
            return AgentReport(final_output=merged, summary="Build passed and review completed")

        err_text = (build.stdout + "\n" + build.stderr).strip()
        last_err = parse_gradle_output(err_text)
        categorized = format_error_report(classify_errors(err_text))
        prompt = (
            "Build failed. Hãy FIX theo lỗi đã phân loại sau, sửa tối thiểu, rồi chuẩn bị rebuild:\n\n"
            f"{categorized}\n\n"
            "Nhắc lại: giữ MVVM + Compose Material3 + đủ UI states + không sửa file không liên quan."
        )

    return AgentReport(
        final_output=(last_output + "\n\nLast build errors:\n" + last_err).strip() or "[No output]",
        summary="Agent loop finished but build may still fail",
    )
