# Agent Notes

Muc dich: ghi lai cac chuc nang va thay doi quan trong cua Android Telegram Agent.

## Quy uoc cap nhat

- Moi khi them/xoa/chinh sua chuc nang quan trong, phai cap nhat file nay.
- Moi muc thay doi nen co ngay, phan loai (`added`, `changed`, `removed`, `fixed`) va mo ta ngan gon.
- Neu thay doi anh huong den lenh Telegram, env vars, hoac luong xu ly stage, phai ghi ro.

## Chuc nang hien tai (tom tat)

- Telegram polling bot + SQLite job store + OpenCode CLI + Android workspace.
- Stage pipeline: `idea -> plan -> design -> code -> verify -> review`.
- Ho tro lenh tao job: `/newandroid`, `/buildapp`, wizard huong dan, va mau brief.
- Theo doi/quan ly job: `/jobs`, `/status`, `/progress`, `/tail`, `/logs`, `/pause`, `/resume`, `/approve`, `/reject`, `/cancel`.
- Verify bang Gradle va bao duong dan APK debug khi thanh cong.
- Tao git repo local cho moi workspace job va commit theo tung buoc scaffold/code/repair.

## Change Log

### 2026-04-25

- `added`: Them `/refactor <job_id> [|extra instruction]` de bot tu dong refactor project cu theo agent.md: tach component/model/function/viewmodel, dua string vao `strings.xml`, va sinh full locale translations.
- `added`: Them `/refactorprompt` de tra ve prompt refactor mau khi can dung thu cong.
- `changed`: Stage `code` nay tu dong chuyen sang `refactor_prompt` khi active task duoc tag `[refactor]`.
- `fixed`: Live CLI log khong con cat JSON event qua som truoc khi format, nen Telegram khong con hien raw `tool_use`/`tool_result` metadata.
- `changed`: Live CLI log cho `codex`/`opencode` nay chi tom tat phan can thiet (tool, status, command, output/error) va bo qua noise nhu `sessionID`, `timestamp`, `callID`, reasoning.
- `changed`: Live CLI log gui gan het gioi han 4096 ky tu cua Telegram thay vi tu cat ngan quanh 320 ky tu.
- `changed`: `/setrepo` va `/pushgit` nay uu tien repo root hien co cua workspace; voi setup hien tai co the set remote va push toan bo `D:\Code`.

### 2026-04-22

- `fixed`: Tang do ben polling Telegram trong `getUpdates` (retry + backoff cho loi mang, gom reset ket noi WinError 10054).
- `changed`: Tach nhom loi mang thanh `TelegramNetworkError` de xu ly rieng.
- `changed`: Giam nhieu log polling bang co che dem loi lien tiep, chi in canh bao theo moc.
- `changed`: `KeyboardInterrupt` thoat bot gon gon (khong con in traceback khong can thiet).
