from __future__ import annotations

import discord
from discord import app_commands
from discord.ext import commands

from ..core import run_claude_prompt


def register_commands(bot: commands.Bot, work_dir: str) -> None:
    @bot.tree.command(name="claude", description="Send a prompt to Claude Code")
    @app_commands.describe(prompt="Prompt for Claude Code")
    async def claude(interaction: discord.Interaction, prompt: str) -> None:
        await interaction.response.defer(thinking=True)
        code, out, err = await run_claude_prompt(work_dir, prompt)
        if code == 0:
            content = out.strip() or "(no output)"
        else:
            content = f"Command failed (exit {code})\n{err.strip() or out.strip() or '(no output)'}"

        for chunk in _chunk_text(content, 1800):
            await interaction.followup.send(f"```\n{chunk}\n```")

    @bot.tree.command(name="status", description="Show bot status")
    async def status(interaction: discord.Interaction) -> None:
        await interaction.response.send_message("Python bot is running.", ephemeral=True)


def _chunk_text(text: str, size: int):
    if not text:
        yield ""
        return
    start = 0
    while start < len(text):
        yield text[start:start + size]
        start += size
