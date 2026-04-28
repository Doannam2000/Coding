import pathlib

p = pathlib.Path(r"D:\Code\NaNAIFlow\android_agent_bot\prompts.py")
content = p.read_text(encoding="utf-8")

refactor = """

def refactor_prompt(job: dict, context: dict, package_name: str, workspace_path: Path) -> str:
    return f\"\"\"
You are the refactoring agent for an existing Android Kotlin app.
Your job is to improve code quality and enforce architectural standards WITHOUT changing app behavior.

Project folder: {workspace_path}
Package name: {package_name}

MANDATORY REFACTORING SCOPE:

1. Component separation
- Split large Screen composables into smaller reusable composables under ui/components/.
- Extract repeated UI patterns into shared components.
- Keep Screen composables clean: only layout + ViewModel wiring.
- Name components clearly: purpose-based (e.g. AppTopBar, PrimaryButton, EmptyStateView).

2. ViewModel + Model separation
- Every screen MUST have a dedicated ViewModel.
- Move business logic out of composables into ViewModels.
- Define typed data models (data class) in a model/ package - never use Map or raw JSON as state.
- Use sealed class UiState for screen state: Loading / Success / Empty / Error.

3. String resources - NO HARDCODED TEXT
- Find ALL hardcoded user-facing strings in Kotlin files.
- Move every single one to res/values/strings.xml with a descriptive key.
- Replace in Compose with stringResource(R.string.key_name).
- Replace in non-Compose code with context.getString(R.string.key_name).

4. Auto-translate all new strings into ALL these locales:
af, am, ar, be, bg, bn, bs, ca, co, cs, da, de, el, es, et, eu, fa, fi, fr, fy, ga, gl, gu, haw, hi, hr, ht, hu, hy, id, in, is, it, iw, ja, ka, ko, ky, lb, lo, lt, lv, mg, mk, mn, ms, nl, no, pl, pt, ro, ru, sk, sl, sm, sq, sr, sv, tg, th, tl, tr, uk, uz, vi, zh
- Create values-{locale}/strings.xml for each.
- Keep string keys identical across all locales.
- Escape XML special characters.
- Preserve brand names and technical terms when needed.

5. Theme and design tokens
- Replace hardcoded colors with theme color references.
- Replace hardcoded dimensions with spacing constants or dimension resources.

6. Architecture cleanup
- Remove SharedPreferences usage; migrate to DataStore Preferences.
- Ensure Hilt dependency injection is used.
- Ensure proper file structure: data/, domain/, ui/, viewmodel/, di/.

7. Localization safety
- Verify layouts handle RTL (Arabic, Persian, Hebrew).
- Add maxLines, softWrap, TextOverflow.Ellipsis where text might overflow.
- Do not rely on fixed text width.

Return strict JSON only. No markdown fences.
Output schema:
{{
  "summary": "...",
  "files_touched": ["..."],
  "hardcoded_strings_extracted": ["R.string.xxx = ..."],
  "locales_generated": ["af", "am", "..."],
  "components_created": ["..."],
  "viewmodels_created": ["..."],
  "models_created": ["..."],
  "remaining_risks": ["..."]
}}

Rules:
- Do NOT change app behavior or add new features.
- Focus only on refactoring for quality, maintainability, and i18n readiness.
- Keep the app buildable on Windows with Gradle wrapper.
- Read and follow the project agent.md file for all code quality rules.
\"\"\".strip()
"""

content = content.rstrip() + refactor + "\n"

code_anchor = "- Keep the app buildable on Windows with Gradle wrapper."
code_extra = """- You MUST read and follow the project agent.md file for ALL code quality rules including component separation, string resources, auto-translation, ViewModel/Model separation, and no hardcoded values. This is not optional.
- Every user-facing string MUST be in res/values/strings.xml and auto-translated into all 65 locales listed in agent.md.
- Every screen MUST have a dedicated ViewModel and use sealed class UiState."""
if code_extra not in content:
    content = content.replace(code_anchor, code_anchor + "\n" + code_extra)

repair_anchor = "- Keep the app buildable with Gradle wrapper commands."
repair_extra = "- You MUST follow agent.md code quality rules: no hardcoded strings, component separation, ViewModel per screen, auto-translate new strings into all 65 locales."
if repair_extra not in content:
    content = content.replace(repair_anchor, repair_anchor + "\n" + repair_extra)

review_anchor = "- Explicitly verify mandatory app system flow: Splash -> Intro -> Main App and the presence/quality of Settings, Language selection, and Exit path."
review_extra = "- Explicitly verify agent.md compliance: no hardcoded strings, component separation, ViewModel per screen, auto-translated strings in all 65 locales, no SharedPreferences, proper file structure (data/, domain/, ui/, viewmodel/, di/)."
if review_extra not in content:
    content = content.replace(review_anchor, review_anchor + "\n" + review_extra)

p.write_text(content, encoding="utf-8")
print("prompts.py updated ok")
