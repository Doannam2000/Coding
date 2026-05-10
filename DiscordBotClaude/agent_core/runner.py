import subprocess
from dataclasses import dataclass


@dataclass
class CmdResult:
    code: int
    stdout: str
    stderr: str


def run_cmd(command: list[str], timeout: int, cwd: str | None = None) -> CmdResult:
    r = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=timeout,
        check=False,
        cwd=cwd,
    )
    return CmdResult(r.returncode, (r.stdout or "").strip(), (r.stderr or "").strip())


def parse_gradle_output(text: str) -> str:
    lines = text.splitlines()
    errs = [l for l in lines if "error" in l.lower() or "failed" in l.lower()]
    return "\n".join(errs[:30]) if errs else ("\n".join(lines[-40:]) if lines else "")


def classify_errors(text: str) -> dict[str, list[str]]:
    buckets = {
        "kotlin": [],
        "gradle": [],
        "manifest": [],
        "compose": [],
        "hilt": [],
        "other": [],
    }
    for line in text.splitlines():
        l = line.lower()
        if not l.strip():
            continue
        if any(k in l for k in ["unresolved reference", "type mismatch", "cannot find symbol", "import"]):
            buckets["kotlin"].append(line)
        elif any(k in l for k in ["dependency", "could not resolve", "gradle", "version conflict"]):
            buckets["gradle"].append(line)
        elif any(k in l for k in ["manifest", "permission", "activity", "service", "receiver"]):
            buckets["manifest"].append(line)
        elif any(k in l for k in ["composable", "@composable", "compose", "state", "remember"]):
            buckets["compose"].append(line)
        elif any(k in l for k in ["hilt", "@inject", "dagger", "module", "provides"]):
            buckets["hilt"].append(line)
        else:
            if "error" in l or "failed" in l or "exception" in l:
                buckets["other"].append(line)

    return {k: v[:20] for k, v in buckets.items() if v}


def format_error_report(buckets: dict[str, list[str]]) -> str:
    if not buckets:
        return "No categorized errors found."
    parts: list[str] = []
    for k, items in buckets.items():
        parts.append(f"[{k}]\n" + "\n".join(items))
    return "\n\n".join(parts)
