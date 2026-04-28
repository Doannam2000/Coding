import pathlib

root = pathlib.Path(r"D:\Code\NaNAIFlow")
prompts_path = root / "android_agent_bot" / "prompts.py"
agent_path = root / "agent.md"

prompts = prompts_path.read_text(encoding="utf-8")
prompts = prompts.replace(
    '- You MUST read and follow the project agent.md file for ALL code quality rules including component separation, string resources, auto-translation, ViewModel/Model separation, and no hardcoded values. This is not optional.\n- Every user-facing string MUST be in res/values/strings.xml and auto-translated into all 65 locales listed in agent.md.\n- Every screen MUST have a dedicated ViewModel and use sealed class UiState.\n- Read and follow the project agent.md file for all code quality rules.',
    '- You MUST read and follow the project agent.md file for ALL code quality rules including component separation, string resources, auto-translation, ViewModel/Model separation, and no hardcoded values. This is not optional.\n- Every user-facing string MUST be in res/values/strings.xml and auto-translated into all 65 locales listed in agent.md.\n- Every screen MUST have a dedicated ViewModel and use sealed class UiState.'
)
prompts_path.write_text(prompts, encoding="utf-8")

agent = agent_path.read_text(encoding="utf-8")
entry = '''- `added`: Them `/refactor <job_id> [|extra instruction]` de bot tu dong refactor project cu theo agent.md: tach component/model/function/viewmodel, dua string vao `strings.xml`, va sinh full locale translations.
- `added`: Them `/refactorprompt` de tra ve prompt refactor mau khi can dung thu cong.
- `changed`: Stage `code` nay tu dong chuyen sang `refactor_prompt` khi active task duoc tag `[refactor]`.
'''
if '/refactor <job_id>' not in agent:
    marker = '### 2026-04-25\n\n'
    agent = agent.replace(marker, marker + entry)
    agent_path.write_text(agent, encoding="utf-8")

print('cleanup and changelog done')
