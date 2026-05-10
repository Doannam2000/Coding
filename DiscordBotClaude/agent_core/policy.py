from dataclasses import dataclass

BLOCKED_PATTERNS = ["rm -rf", "git reset --hard", "git push --force", "git push -f"]
ALLOWED_ANDROID_COMMANDS = {
    "./gradlew tasks",
    "./gradlew assembleDebug",
    "./gradlew test",
    "./gradlew connectedAndroidTest",
    "./gradlew lint",
    "adb devices",
    "adb logcat",
    "adb shell pm list packages",
    "git status",
    "git diff",
}


@dataclass
class PolicyResult:
    ok: bool
    reason: str = ""


def validate_user_prompt(prompt: str, safe_mode: bool = True) -> PolicyResult:
    lowered = prompt.lower()
    for p in BLOCKED_PATTERNS:
        if p in lowered:
            return PolicyResult(False, f"Blocked pattern: {p}")

    if safe_mode and prompt.strip().startswith(("./gradlew", "adb", "git")):
        normalized = " ".join(prompt.strip().split())
        if normalized not in ALLOWED_ANDROID_COMMANDS:
            return PolicyResult(False, "Command is outside Android allowlist")

    return PolicyResult(True)
