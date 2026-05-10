from pathlib import Path


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        return ""


def collect_skills(bot_dir: Path, host_dir: Path, max_chars: int) -> str:
    def collect(base: Path, source: str) -> list[str]:
        if not base.exists() or not base.is_dir():
            return []
        out: list[str] = []
        for d in sorted(base.iterdir()):
            if not d.is_dir():
                continue
            parts = []
            for name in ("README.md", "CLAUDE.md", "prompt.md", "instructions.md"):
                fp = d / name
                if fp.exists():
                    txt = _read_text(fp).strip()
                    if txt:
                        parts.append(f"[{name}]\n{txt}")
            if parts:
                out.append(f"## {source}:{d.name}\n" + "\n\n".join(parts))
        return out

    merged = "\n\n".join(collect(bot_dir, "bot") + collect(host_dir, "host"))
    if not merged:
        return ""
    if len(merged) > max_chars:
        merged = merged[:max_chars] + "\n... (truncated)"
    return "Skill context (bot + host):\n\n" + merged
