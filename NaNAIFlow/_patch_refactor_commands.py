import pathlib

root = pathlib.Path(r"D:\Code\NaNAIFlow")
runner_path = root / "android_agent_bot" / "runner.py"
main_path = root / "android_agent_bot" / "main.py"
prompts_path = root / "android_agent_bot" / "prompts.py"

# prompts.py cleanup
prompts = prompts_path.read_text(encoding="utf-8")
prompts = prompts.replace(
    '- You MUST read and follow the project agent.md file for ALL code quality rules including component separation, string resources, auto-translation, ViewModel/Model separation, and no hardcoded values. This is not optional.\n- Every user-facing string MUST be in res/values/strings.xml and auto-translated into all 65 locales listed in agent.md.\n- Every screen MUST have a dedicated ViewModel and use sealed class UiState.\n- Read and follow the project agent.md file for all code quality rules.',
    '- You MUST read and follow the project agent.md file for ALL code quality rules including component separation, string resources, auto-translation, ViewModel/Model separation, and no hardcoded values. This is not optional.\n- Every user-facing string MUST be in res/values/strings.xml and auto-translated into all 65 locales listed in agent.md.\n- Every screen MUST have a dedicated ViewModel and use sealed class UiState.'
)
prompts_path.write_text(prompts, encoding="utf-8")

runner = runner_path.read_text(encoding="utf-8")
runner = runner.replace(
    'from .prompts import code_prompt, design_prompt, idea_prompt, plan_prompt, repair_prompt, review_prompt',
    'from .prompts import code_prompt, design_prompt, idea_prompt, plan_prompt, refactor_prompt, repair_prompt, review_prompt'
)
runner = runner.replace(
    '        "/buildbyprompt",\n        "/fixbug",\n    )',
    '        "/buildbyprompt",\n        "/fixbug",\n        "/refactor",\n    )'
)

anchor = '''    def _ensure_workspace_git(self, job: dict[str, Any], workspace: Path) -> dict[str, Any] | None:\n        if (workspace / ".git").exists():\n            return None\n\n        init_result = run_git(workspace, "init", "-b", f"job-{job['id']}")\n        if init_result.returncode != 0:\n            raise RuntimeError((init_result.stderr or init_result.stdout).strip() or "git init failed")\n\n        run_git(workspace, "config", "user.name", "Android Agent Bot")\n        run_git(workspace, "config", "user.email", "android-agent@example.local")\n        return {\n            "branch": f"job-{job['id']}",\n            "summary": "Initialized git repository for generated Android project",\n        }\n'''
insert = anchor + '''\n    def _refactor_instruction(self, extra_request: str | None = None) -> str:\n        base = (\n            "Refactor the existing Android project to comply with agent.md. "\n            "Mandatory scope: split large screens into reusable components; separate component/model/function/viewmodel responsibilities; "\n            "move all user-facing strings out of Kotlin into strings.xml; auto-translate all new and extracted strings into locales af, am, ar, be, bg, bn, bs, ca, co, cs, da, de, el, es, et, eu, fa, fi, fr, fy, ga, gl, gu, haw, hi, hr, ht, hu, hy, id, in, is, it, iw, ja, ka, ko, ky, lb, lo, lt, lv, mg, mk, mn, ms, nl, no, pl, pt, ro, ru, sk, sl, sm, sq, sr, sv, tg, th, tl, tr, uk, uz, vi, zh; "\n            "do not hardcode user-facing strings; use dedicated ViewModel per screen; use sealed UiState; replace SharedPreferences with DataStore where present; "\n            "replace hardcoded colors/dimensions with theme/design tokens; preserve app behavior while improving architecture and localization safety."
        )\n        extra = (extra_request or "").strip()\n        return f"{base} Additional request: {extra}" if extra else base\n\n    def _resolve_code_prompt(self, job: dict[str, Any], context: dict[str, Any], package_name: str, workspace: Path) -> str:\n        active_task = str(context.get("active_task") or "").strip() if isinstance(context, dict) else ""\n        if active_task.lower().startswith("[refactor]"):\n            return refactor_prompt(job, context, package_name, workspace)\n        return code_prompt(job, context, package_name, workspace, job.get("rejection_feedback"))\n'''
if 'def _resolve_code_prompt(' not in runner:
    runner = runner.replace(anchor, insert)

runner = runner.replace(
    '                prompt=code_prompt(job, context, package_name, workspace, job.get("rejection_feedback")),',
    '                prompt=self._resolve_code_prompt(job, context, package_name, workspace),'
)

runner = runner.replace(
    '        is_fix_bug_task = lowered_task.startswith("[fixbug]")\n        next_stage = "code" if is_fix_bug_task else "plan"',
    '        is_fix_bug_task = lowered_task.startswith("[fixbug]")\n        is_refactor_task = lowered_task.startswith("[refactor]")\n        next_stage = "code" if (is_fix_bug_task or is_refactor_task) else "plan"'
)

runner = runner.replace(
    '        elif normalized.startswith("/fixbug"):\n            self._fix_bug(chat_id, normalized)\n        elif normalized.startswith("/deletejob"):',
    '        elif normalized.startswith("/fixbug"):\n            self._fix_bug(chat_id, normalized)\n        elif normalized.startswith("/refactorprompt"):\n            self._refactor_prompt(chat_id, normalized)\n        elif normalized.startswith("/refactor"):\n            self._refactor(chat_id, normalized)\n        elif normalized.startswith("/deletejob"):'
)

runner = runner.replace(
    '            "/fixbug <job_id>|<bug description>\\n"\n            "/deletejob <job_id>\\n"',
    '            "/fixbug <job_id>|<bug description>\\n"\n            "/refactor <job_id> [|extra instruction]\\n"\n            "/refactorprompt\\n"\n            "/deletejob <job_id>\\n"'
)

method_anchor = '''    def _fix_bug(self, chat_id: int, text: str) -> None:\n        _, _, raw_args = text.partition(" ")\n        parts = [part.strip() for part in raw_args.split("|", 1)]\n        if len(parts) != 2 or not parts[0] or not parts[1] or not parts[0].isdigit():\n            self.telegram.send_message(chat_id, "Usage: /fixbug <job_id>|<bug description>")\n            return\n\n        job_id = int(parts[0])\n        bug_text = parts[1]\n        updated, should_activate_now = self.db.add_follow_up_task(job_id, bug_text, tag="fixbug")\n        if updated is None:\n            self.telegram.send_message(chat_id, "Job not found")\n            return\n\n        if should_activate_now:\n            activated = self.db.activate_next_task(job_id)\n            if activated is None:\n                self.telegram.send_message(chat_id, "Job not found")\n                return\n            self.telegram.send_message_with_markup(\n                chat_id,\n                f"Bug-fix task queued and activated for job #{job_id}: {bug_text}\\n"\n                f"Job moved to stage `{activated['current_stage']}` with status `{activated['status']}`.",\n                reply_markup=self._job_action_markup(job_id, include_approval=False),\n            )\n            return\n\n        self.telegram.send_message_with_markup(\n            chat_id,\n            f"Bug-fix task queued for job #{job_id}: {bug_text}\\n"\n            "Bot will run it automatically after current task completes.",\n            reply_markup=self._job_action_markup(job_id, include_approval=False),\n        )\n'''
methods_insert = method_anchor + '''\n    def _refactor(self, chat_id: int, text: str) -> None:\n        _, _, raw_args = text.partition(" ")\n        raw_args = raw_args.strip()\n        if not raw_args:\n            self.telegram.send_message(chat_id, "Usage: /refactor <job_id> [|extra instruction]")\n            return\n\n        if "|" in raw_args:\n            job_raw, extra_instruction = [part.strip() for part in raw_args.split("|", 1)]\n        else:\n            job_raw, extra_instruction = raw_args, ""\n\n        if not job_raw.isdigit():\n            self.telegram.send_message(chat_id, "Usage: /refactor <job_id> [|extra instruction]")\n            return\n\n        job_id = int(job_raw)\n        job = self.db.get_job(job_id)\n        if job is None:\n            self.telegram.send_message(chat_id, "Job not found")\n            return\n\n        instruction = self.worker._refactor_instruction(extra_instruction) if self.worker is not None else extra_instruction\n        updated, should_activate_now = self.db.add_follow_up_task(job_id, instruction, tag="refactor")\n        if updated is None:\n            self.telegram.send_message(chat_id, "Job not found")\n            return\n\n        if should_activate_now:\n            activated = self.db.activate_next_task(job_id)\n            if activated is None:\n                self.telegram.send_message(chat_id, "Job not found")\n                return\n            self.telegram.send_message_with_markup(\n                chat_id,\n                f"Refactor task queued and activated for job #{job_id}.\\n"\n                f"Job moved to stage `{activated['current_stage']}` with status `{activated['status']}`.\\n"\n                "Bot will enforce component/model/function/viewmodel separation, strings.xml extraction, and full locale translation from agent.md.",\n                reply_markup=self._job_action_markup(job_id, include_approval=False),\n            )\n            return\n\n        self.telegram.send_message_with_markup(\n            chat_id,\n            f"Refactor task queued for job #{job_id}.\\n"\n            "Bot will automatically enforce agent.md architecture, no hardcoded strings, and full locale translation after the current task completes.",\n            reply_markup=self._job_action_markup(job_id, include_approval=False),\n        )\n\n    def _refactor_prompt(self, chat_id: int, text: str) -> None:\n        self.telegram.send_message_many(\n            chat_id,\n            "Refactor prompt template:\\n"\n            "Refactor this existing Android project to fully comply with agent.md.\\n"\n            "Mandatory: split large screens into reusable components, separate component/model/function/viewmodel responsibilities, move all user-facing strings into strings.xml, auto-translate all new strings into locales af, am, ar, be, bg, bn, bs, ca, co, cs, da, de, el, es, et, eu, fa, fi, fr, fy, ga, gl, gu, haw, hi, hr, ht, hu, hy, id, in, is, it, iw, ja, ka, ko, ky, lb, lo, lt, lv, mg, mk, mn, ms, nl, no, pl, pt, ro, ru, sk, sl, sm, sq, sr, sv, tg, th, tl, tr, uk, uz, vi, zh, use a dedicated ViewModel per screen, sealed UiState, DataStore instead of SharedPreferences, theme/design tokens instead of hardcoded colors and dimensions, and preserve app behavior while improving architecture and localization safety."\n        )\n'''
if 'def _refactor(self, chat_id: int, text: str) -> None:' not in runner:
    runner = runner.replace(method_anchor, methods_insert)

runner_path.write_text(runner, encoding="utf-8")

main = main_path.read_text(encoding="utf-8")
main = main.replace(
    '{"command": "fixbug", "description": "Queue a follow-up bug-fix task"},\n            {"command": "deletejob", "description": "Delete job from database"},',
    '{"command": "fixbug", "description": "Queue a follow-up bug-fix task"},\n            {"command": "refactor", "description": "Auto-refactor project to comply with agent.md"},\n            {"command": "refactorprompt", "description": "Show reusable refactor prompt template"},\n            {"command": "deletejob", "description": "Delete job from database"},'
)
main_path.write_text(main, encoding="utf-8")

print('patched refactor command support')
