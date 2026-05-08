from __future__ import annotations

import asyncio
from asyncio.subprocess import PIPE


async def run_claude_prompt(work_dir: str, prompt: str) -> tuple[int, str, str]:
    proc = await asyncio.create_subprocess_exec(
        "claude",
        "-p",
        prompt,
        cwd=work_dir,
        stdout=PIPE,
        stderr=PIPE,
    )
    out, err = await proc.communicate()
    return proc.returncode or 0, out.decode("utf-8", errors="replace"), err.decode("utf-8", errors="replace")
