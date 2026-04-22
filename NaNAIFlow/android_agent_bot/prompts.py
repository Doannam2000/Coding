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
- Add concise comments only where logic would otherwise be unclear.
- Do not edit secrets or environment files.
- Implement the requested features first before adding extras.
- Match the requested visual style and font direction as closely as practical with available Android resources.
- Mandatory app system implementation unless explicitly disabled in the brief:
  Splash -> Intro -> Main App, with Settings, Language selection, and an explicit exit action/path.
""".strip()


def repair_prompt(job: dict, context: dict, package_name: str, verify_log: str) -> str:
    return f"""
You are repairing an Android Kotlin app after verification failed.
Work only in the existing project.

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
    - Preserve mandatory app system flow: Splash -> Intro -> Main App, plus Settings/Language/Exit paths.
    """.strip()


def review_prompt(job: dict, context: dict) -> str:
    return f"""
You are the review agent for an autonomous Android Kotlin app builder.
Review the current output and decide whether the agent should stop or loop back for more work.

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
- Explicitly check whether the final output still matches the requested app idea, style direction, font direction, and feature priorities.
- Explicitly verify mandatory app system flow: Splash -> Intro -> Main App and the presence/quality of Settings, Language selection, and Exit path.
""".strip()
