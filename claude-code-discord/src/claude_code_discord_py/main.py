from __future__ import annotations

import asyncio
import discord
from discord.ext import commands

from .commands import register_commands
from .core import load_config


async def run() -> None:
    cfg = load_config()
    intents = discord.Intents.default()
    intents.message_content = True

    bot = commands.Bot(command_prefix="!", intents=intents)

    @bot.event
    async def on_ready() -> None:
        await bot.tree.sync()
        print(f"Logged in as {bot.user}")

    register_commands(bot, cfg.work_dir)
    await bot.start(cfg.discord_token)


if __name__ == "__main__":
    asyncio.run(run())
