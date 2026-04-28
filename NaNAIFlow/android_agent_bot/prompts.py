from __future__ import annotations

import json
from pathlib import Path


def _json(data: object) -> str:
    return json.dumps(data, ensure_ascii=True, indent=2)


def idea_prompt(job: dict) -> str:
    return f"""
You are the product ideation agent for an autonomous Android Kotlin app builder.
Return strict JSON only. No markdown fences.

Output schema:
{{
  "app_name": "...",
  "tagline": "...",
  "problem": "...",
  "target_users": ["..."],
  "core_value": "...",
  "mvp_features": ["..."],
  "differentiators": ["..."],
  "success_metrics": ["..."],
  "risks": ["..."]
}}

User request: {job['request_text']}
Target users hint: {job['target_users']}
Constraints: {job['constraints_text']}

Rules:
- Build a practical Android MVP, not a vague concept.
- Assume Kotlin + Jetpack Compose + Material 3.
- Keep feature scope tight enough for one agentic build loop.
- Treat any style, font, and feature hints in the user request as high-priority product direction.
- Treat this baseline app system flow as mandatory unless the user explicitly opts out:
  Splash -> Intro/Onboarding -> Main App.
- Include language selection (at least EN + VI), a settings area, and an explicit exit action/path in the app shell.
""".strip()


def plan_prompt(job: dict, context: dict) -> str:
    active_task = ""
    if isinstance(context, dict):
        active_task = str(context.get("active_task") or "").strip()

    if active_task and active_task.lower().startswith("[fixbug]"):
        return f"""
You are the planning agent for an Android Kotlin MVP.
Return strict JSON only. No markdown fences.

Output schema:
{{
  "summary": "...",
  "screens": [],
  "navigation": [],
  "data_models": [],
  "architecture": {{
    "ui": "Jetpack Compose",
    "pattern": "MVVM",
    "storage": "unchanged"
  }},
  "milestones": ["apply bug fix directly in code"],
  "test_strategy": ["regression check around bug scope"]
}}

Follow-up task to implement now:
{active_task}

Rules:
- This task is tagged as FIXBUG.
- Do NOT re-plan product scope or UX redesign.
- Keep plan minimal and focused on direct bug-fix implementation.
- Preserve existing app architecture and flows.
""".strip()

    if active_task:
        return f"""
You are the planning agent for an Android Kotlin MVP.
Return strict JSON only. No markdown fences.

Output schema:
{{
  "summary": "...",
  "screens": [
    {{
      "name": "...",
      "purpose": "...",
      "key_elements": ["..."]
    }}
  ],
  "navigation": ["..."],
  "data_models": [
    {{
      "name": "...",
      "fields": ["..."]
    }}
  ],
  "architecture": {{
    "ui": "Jetpack Compose",
    "pattern": "MVVM",
    "storage": "..."
  }},
  "milestones": ["..."],
  "test_strategy": ["..."]
}}

Current app context (already implemented baseline):
{_json(context.get('stages', {}).get('code', {}))}

Previous plan (for reference only, do not re-run old work):
{_json(context.get('stages', {}).get('plan', {}))}

Follow-up task to implement now:
{active_task}

Rules:
- This is a FOLLOW-UP task mode.
- Plan ONLY the incremental work needed for the follow-up task.
- Do not re-plan onboarding/system flow unless follow-up task explicitly requires it.
- Keep existing architecture and app behavior stable.
- Focus on integration points, modified screens/components, and verification steps for the new task.
""".strip()

    return f"""
You are the planning agent for an Android Kotlin MVP.
Return strict JSON only. No markdown fences.

Output schema:
{{
  "summary": "...",
  "screens": [
    {{
      "name": "...",
      "purpose": "...",
      "key_elements": ["..."]
    }}
  ],
  "navigation": ["..."],
  "data_models": [
    {{
      "name": "...",
      "fields": ["..."]
    }}
  ],
  "architecture": {{
    "ui": "Jetpack Compose",
    "pattern": "MVVM",
    "storage": "..."
  }},
  "milestones": ["..."],
  "test_strategy": ["..."]
}}

User request: {job['request_text']}
Idea output:
{_json(context['stages']['idea'])}

Rules:
- Stay within a 5-7 screen MVP.
- Prefer local data or one simple remote API.
- Keep milestones incremental and implementation-ready.
- Carry forward any explicit style, font, and feature priorities from the brief.
- Mandatory navigation/system flow:
  Splash -> Intro -> Main App, plus Settings and Language selection.
- Ensure the plan includes an explicit user-facing exit action/path.
""".strip()


def design_prompt(job: dict, context: dict) -> str:
    active_task = ""
    if isinstance(context, dict):
        active_task = str(context.get("active_task") or "").strip()

    if active_task and active_task.lower().startswith("[fixbug]"):
        return f"""
You are the Android design agent.
Return strict JSON only. No markdown fences.

Output schema:
{{
  "visual_direction": "unchanged",
  "color_palette": {{
    "primary": "unchanged",
    "secondary": "unchanged",
    "background": "unchanged",
    "surface": "unchanged",
    "accent": "unchanged"
  }},
  "typography": {{
    "headline": "unchanged",
    "body": "unchanged"
  }},
  "component_guidelines": ["only minimal UI adjustments needed for bug fix"],
  "screen_briefs": [],
  "accessibility": ["preserve current accessibility behavior"]
}}

Follow-up task:
{active_task}

Rules:
- This task is tagged as FIXBUG.
- Do NOT redesign unrelated UX/UI.
- Keep design output minimal and focused only on visual constraints needed for bug fix validation.
""".strip()

    if active_task:
        return f"""
You are the Android design agent.
Return strict JSON only. No markdown fences.

Output schema:
{{
  "visual_direction": "...",
  "color_palette": {{
    "primary": "#...",
    "secondary": "#...",
    "background": "#...",
    "surface": "#...",
    "accent": "#..."
  }},
  "typography": {{
    "headline": "...",
    "body": "..."
  }},
  "component_guidelines": ["..."],
  "screen_briefs": [
    {{
      "screen": "...",
      "layout": "...",
      "interactions": ["..."]
    }}
  ],
  "accessibility": ["..."]
}}

Current app context:
{_json(context.get('stages', {}).get('code', {}))}

Follow-up task:
{active_task}

Rules:
- Follow-up mode: provide only incremental design updates for the follow-up task.
- Do not redesign unrelated screens/components.
- Keep existing visual language and architecture stable.
- Focus on updated interactions/states needed for this task.
""".strip()

    return f"""
You are the Android design agent.
Return strict JSON only. No markdown fences.

Output schema:
{{
  "visual_direction": "...",
  "color_palette": {{
    "primary": "#...",
    "secondary": "#...",
    "background": "#...",
    "surface": "#...",
    "accent": "#..."
  }},
  "typography": {{
    "headline": "...",
    "body": "..."
  }},
  "component_guidelines": ["..."],
  "screen_briefs": [
    {{
      "screen": "...",
      "layout": "...",
      "interactions": ["..."]
    }}
  ],
  "accessibility": ["..."]
}}

Idea output:
{_json(context['stages']['idea'])}

Plan output:
{_json(context['stages']['plan'])}

Rules:
- Keep the design specific enough for direct implementation in Jetpack Compose.
- Use Material 3 patterns unless the product clearly needs otherwise.
- Avoid decorative complexity that increases implementation risk.
- Respect the requested style and font direction from the user brief whenever possible.
- Make screen briefs concrete enough that the coding stage can implement them without inventing a new visual language.
- Include concrete design briefs for Splash, Intro/Onboarding, Main App shell, Settings, and Language selection.
- Include an explicit exit action affordance in the design flow.
""".strip()


def code_prompt(job: dict, context: dict, package_name: str, workspace_path: Path, rejection_feedback: str | None) -> str:
    feedback_block = f"Previous review feedback to address before coding: {rejection_feedback}\n" if rejection_feedback else ""
    active_task = ""
    if isinstance(context, dict):
        active_task = str(context.get("active_task") or "").strip()
    follow_up_block = ""
    if active_task:
        is_fixbug = active_task.lower().startswith("[fixbug]")
        if is_fixbug:
            follow_up_block = (
                "Follow-up task mode:\n"
                f"- Active follow-up task: {active_task}\n"
                "- FIXBUG mode: apply direct bug fix only.\n"
                "- Do not rework unrelated features, architecture, or broad UX redesign.\n"
                "- Prefer targeted changes with regression-safe scope.\n"
                "- Keep behavior changes limited to bug area and immediate dependencies.\n\n"
            )
        else:
            follow_up_block = (
                "Follow-up task mode:\n"
                f"- Active follow-up task: {active_task}\n"
                "- Implement only this incremental task on top of current app.\n"
                "- Keep existing features working; avoid re-implementing unrelated baseline flows.\n"
                "- Touch the smallest set of files/screens needed.\n"
                "- If task mentions UX/UI, explicitly verify spacing, typography hierarchy, touch targets, empty/error/loading states, and small-screen behavior before finishing.\n\n"
            )
    return f"""
You are the coding agent for an Android Kotlin app project.
You are working inside this project folder: {workspace_path}

Goal:
- Create or update a buildable Android app using Kotlin, Jetpack Compose, Material 3, and MVVM.
- If the project is empty, scaffold an Android Studio-compatible project first.
- Favor a minimal single-module app unless the plan explicitly requires more.

Architecture constraints:
- Package name: {package_name}
- Kotlin only
- Jetpack Compose UI
- Navigation Compose for navigation
- ViewModel + StateFlow for screen state
- Local persistence with Room or in-memory fake repository if storage is not essential

Product brief:
{_json(context['stages']['idea'])}

Implementation plan:
{_json(context['stages']['plan'])}

Design brief:
{_json(context['stages']['design'])}

{follow_up_block}
{feedback_block}Return strict JSON only after applying edits. No markdown fences.

Output schema:
{{
  "summary": "...",
  "files_touched": ["..."],
  "features_completed": ["..."],
  "follow_up_notes": ["..."]
}}

Rules:
- Keep the app buildable on Windows with Gradle wrapper.
- Before making any code changes, you MUST read `{workspace_path / 'agent.md'}` inside this job workspace and treat it as the source of truth for project rules.
- Immediately after that, you MUST read `{workspace_path / 'project.md'}` inside this job workspace and use it as the source of truth for project-specific scope, context, and implementation direction.
- Do not rely on any other agent.md outside this workspace unless the workspace agent.md explicitly tells you to.
- Do not rely on any other project.md outside this workspace unless the workspace project.md explicitly tells you to.
- You MUST follow the workspace agent.md file for ALL code quality rules including component separation, string resources, auto-translation, ViewModel/Model separation, and no hardcoded values. This is not optional.
- You MUST keep the implementation aligned with the workspace project.md file for the current app scope, user needs, and planned structure.
- Every user-facing string MUST be in res/values/strings.xml and auto-translated into all 65 locales listed in agent.md.
- Every screen MUST have a dedicated ViewModel and use sealed class UiState.
- Add concise comments only where logic would otherwise be unclear.
- Do not edit secrets or environment files.
- Implement the requested features first before adding extras.
- Match the requested visual style and font direction as closely as practical with available Android resources.
- Mandatory app system implementation unless explicitly disabled in the brief:
  Splash -> Intro -> Main App, with Settings, Language selection, and an explicit exit action/path.
- If the task is large, work in two passes in this same run:
  1) establish compilable architecture/screen wiring,
  2) implement advanced interactions and polishing.
- Prefer finishing with a valid minimal complete implementation over timing out while polishing details.
""".strip()


def repair_prompt(job: dict, context: dict, package_name: str, verify_log: str, workspace_path: Path) -> str:
    return f"""
You are repairing an Android Kotlin app after verification failed.
Work only in the existing project.
Project folder: {workspace_path}

Product brief:
{_json(context['stages']['idea'])}

Plan:
{_json(context['stages']['plan'])}

Design:
{_json(context['stages']['design'])}

Latest code summary:
{_json(context['stages']['code'])}

Verification failures:
{verify_log}

Return strict JSON only after edits. No markdown fences.
{{
  "summary": "...",
  "files_touched": ["..."],
  "fixes_applied": ["..."],
  "remaining_risks": ["..."]
}}

    Rules:
    - Focus only on the reported failures.
    - Preserve the chosen architecture and design direction.
    - Keep the app buildable with Gradle wrapper commands.
    - Before making any code changes, you MUST read `{workspace_path / 'agent.md'}` inside this job workspace and treat it as the source of truth for project rules.
    - Immediately after that, you MUST read `{workspace_path / 'project.md'}` inside this job workspace and use it as the source of truth for project-specific scope, context, and implementation direction.
    - Do not rely on any other agent.md outside this workspace unless the workspace agent.md explicitly tells you to.
    - Do not rely on any other project.md outside this workspace unless the workspace project.md explicitly tells you to.
    - You MUST follow the workspace agent.md code quality rules: no hardcoded strings, component separation, ViewModel per screen, auto-translate new strings into all 65 locales.
    - You MUST keep the repair aligned with the workspace project.md file and preserve the intended app scope and behavior.
    - Preserve mandatory app system flow: Splash -> Intro -> Main App, plus Settings/Language/Exit paths.
    """.strip()


def review_prompt(job: dict, context: dict, workspace_path: Path) -> str:
    return f"""
You are the review agent for an autonomous Android Kotlin app builder.
Review the current output and decide whether the agent should stop or loop back for more work.
Project folder: {workspace_path}

User request:
{job['request_text']}

Target users:
{job['target_users']}

Constraints:
{job['constraints_text']}

Idea:
{_json(context['stages']['idea'])}

Plan:
{_json(context['stages']['plan'])}

Design:
{_json(context['stages']['design'])}

Latest code result:
{_json(context['stages']['code'])}

Latest verify result:
{_json(context['stages']['verify'])}

Return strict JSON only. No markdown fences.

Output schema:
{{
  "summary": "...",
  "decision": "complete" | "iterate",
  "next_stage": "plan" | "design" | "code" | "verify" | "complete",
  "reasoning": ["..."],
  "strengths": ["..."],
  "issues": ["..."],
  "action_items": ["..."],
  "review_focus": "..."
}}

Rules:
- Choose `complete` only if the app is coherent, scoped correctly, and sufficiently implemented for the request.
- Choose `iterate` if there are meaningful gaps, quality issues, or scope misses.
- Set `next_stage` to the earliest stage that should be revisited.
- Use `plan` for scope/flow changes, `design` for UX/UI direction changes, `code` for implementation gaps, `verify` only if more verification is needed without code changes.
- Keep action items concrete so the next stage can use them directly.
- Before reviewing, you MUST read `{workspace_path / 'agent.md'}` inside this job workspace and treat it as the source of truth for project rules.
- Immediately after that, you MUST read `{workspace_path / 'project.md'}` inside this job workspace and use it as the source of truth for project-specific scope, context, and implementation direction.
- Do not rely on any other agent.md outside this workspace unless the workspace agent.md explicitly tells you to.
- Do not rely on any other project.md outside this workspace unless the workspace project.md explicitly tells you to.
- Explicitly check whether the final output still matches the requested app idea, style direction, font direction, and feature priorities.
- Explicitly check whether the final output remains aligned with the workspace project.md summary, feature scope, screens, and constraints.
- Explicitly verify mandatory app system flow: Splash -> Intro -> Main App and the presence/quality of Settings, Language selection, and Exit path.
- Explicitly verify workspace agent.md compliance: no hardcoded strings, component separation, ViewModel per screen, auto-translated strings in all 65 locales, no SharedPreferences, proper file structure (data/, domain/, ui/, viewmodel/, di/).
""".strip()

def refactor_prompt(job: dict, context: dict, package_name: str, workspace_path: Path) -> str:
    return f"""
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
- Before making any code changes, you MUST read `{workspace_path / 'agent.md'}` inside this job workspace and treat it as the source of truth for project rules.
- Immediately after that, you MUST read `{workspace_path / 'project.md'}` inside this job workspace and use it as the source of truth for project-specific scope, context, and implementation direction.
- Do not rely on any other agent.md outside this workspace unless the workspace agent.md explicitly tells you to.
- Do not rely on any other project.md outside this workspace unless the workspace project.md explicitly tells you to.
- You MUST follow the workspace agent.md file for ALL code quality rules including component separation, string resources, auto-translation, ViewModel/Model separation, and no hardcoded values. This is not optional.
- You MUST preserve the app scope and intended behavior described by the workspace project.md file while refactoring.
- Every user-facing string MUST be in res/values/strings.xml and auto-translated into all 65 locales listed in agent.md.
- Every screen MUST have a dedicated ViewModel and use sealed class UiState.
""".strip()

