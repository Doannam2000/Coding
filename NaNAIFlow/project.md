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
- Theo doi/quan ly job: `/jobs`, `/status`, `/progress`, `/tail`, `/logs`, `/pause`, `/resume`, `/addfeature`, `/addtask`, `/fixbug`, `/approve`, `/reject`, `/cancel`.
- Verify bang Gradle va bao duong dan APK debug khi thanh cong.
- Tao git repo local cho moi workspace job va commit theo tung buoc scaffold/code/repair.

## Change Log

### 2026-04-22

- `fixed`: Tang do ben polling Telegram trong `getUpdates` (retry + backoff cho loi mang, gom reset ket noi WinError 10054).
- `changed`: Tach nhom loi mang thanh `TelegramNetworkError` de xu ly rieng.
- `changed`: Giam nhieu log polling bang co che dem loi lien tiep, chi in canh bao theo moc.
- `changed`: `KeyboardInterrupt` thoat bot gon gon (khong con in traceback khong can thiet).
- `added`: Them lenh `/addfeature <job_id>|<feature request>` de bo sung tinh nang cho job dang co va dua job quay lai stage phu hop de build tiep.
- `fixed`: `/addfeature` nay luon re-queue job ve stage `plan` voi status `queued` de job chay lai tu dong.
- `added`: Them lenh `/addtask <job_id>|<task>` de xep hang task tiep theo sau task hien tai.
- `added`: Them lenh `/fixbug <job_id>|<bug description>` de queue follow-up task co tag `[fixbug]`.
- `added`: Them lenh `/tasks <job_id>` de xem active task, pending queue, va recent done tasks cua job.
- `added`: Khi job hoan thanh va con `pending_tasks`, bot tu dong kich hoat task tiep theo va restart tu stage `plan`.
- `fixed`: Follow-up task mode chi plan/design/code phan mo rong moi, tranh lap lai toan bo baseline task da xong.
- `fixed`: `runandroid` va `syncandroid` khong con treo vo han; da them timeout cho lenh PowerShell/Gradle va tra ve loi timeout ro rang.
- `fixed`: `runandroid` duoc chay trong background thread, tranh block vong xu ly Telegram polling khi lenh cai dat/lauch Android treo lau.
- `fixed`: Bo qua loi callback Telegram het han (`query is too old` / `query ID is invalid`) de tranh lam poll loop bao loi gia.
- `fixed`: Mo rong bo loc callback Telegram het han (`response timeout expired`, HTTP 400 cho callback) de chan spam polling error.
- `changed`: Mac dinh bat live CLI logs cho chat hop le ngay khi bat dau su dung bot.
- `changed`: `/resume` co fallback replay lai lenh gan nhat (khong phai `/resume`) de thu chay lai thao tac truoc do neu gap loi.
- `fixed`: Gioi han lenh duoc `/resume` replay (chi lenh tac vu), tranh replay `/models` hoac lenh menu gay nham luong.
- `added`: Tu dong chia nho tin nhan dai thanh nhieu message de vuot gioi han 4096 ky tu cua Telegram.
- `changed`: Retry OpenCode CLI uu tien tiep tuc session cu (`--session ... --continue`) thay vi tao session moi neu lay duoc session id.
- `fixed`: Tang timeout theo tung stage OpenCode (code/repair/review) de giam fail `timed out after 600 seconds` cho task lon.
- `fixed`: `/resume` ho tro hoi sinh job tu `failed/cancelled/completed/waiting_approval` ve `queued` de worker chay lai that su.
- `changed`: Tang them timeout cho stage `idea/plan/design` va bo sung huong dan code theo 2 pass de giam xac suat timeout.
- `changed`: Khi OpenCode CLI timeout/hung, bot tu dong retry vo han va tiep tuc lai session cu den khi chay duoc (khong dung o gioi han restart_attempts).
- `changed`: Neu resume session OpenCode that bai (session invalid/expired), bot tu dong fallback tao session moi va tiep tuc retry.
- `fixed`: Neu Telegram gui message/callback bi loi, bot bo qua va tiep tuc chay workflow (khong lam dung poll loop/job).
- `fixed`: Timeout OpenCode doi tu timeout tong runtime sang timeout theo idle output (co output thi khong timeout), giam timeout gia khi task dang chay.
- `changed`: Giu default `OPEN_CODE_TIMEOUT_SECONDS` la 600; da chuyen sang idle-timeout nen van giam timeout gia.
- `changed`: Ho tro 2 CLI cho tac vu agent (`opencode` hoac `codex`) thong qua `OPEN_CODE_BINARY`, co resume session va fallback session moi cho ca hai.
- `added`: Them lenh `/cli [opencode|codex]` de doi CLI runtime truc tiep trong Telegram (luu runtime setting, khong can restart).
- `fixed`: `/models` khi dang dung Codex CLI se khong hien danh sach gia; bot huong dan dung `/model <provider/model>` truc tiep.
- `changed`: `/models` ho tro Codex CLI bang cach goi truc tiep `codex models` (stdin newline) de lay danh sach model thuc te.
- `changed`: Them hardcoded model picker cho Codex CLI de `/models` luon dung duoc trong bot (danh sach model do user cung cap).
- `fixed`: Voi OpenCode CLI, bot giu nguyen final text/JSON tu stdout event stream thay vi de co che output-file cua Codex ghi de.
- `changed`: Cap nhat template `agent.md` mac dinh voi CODE QUALITY RULES day du (component separation, strings.xml, DataStore-only, multi-language strings, localization safety).
- `fixed`: Giam 429 cho CLI bang request pacing (mac dinh cach nhau 2s) + backoff theo `retry_after` khi co, retry ratelimit khong gioi han den khi qua duoc.
- `changed`: 429 retry duoc nang len toi da 100 lan, wait random vai giay giua cac lan retry, uu tien continue session de model giu context.
- `changed`: Giam timeout toi thieu stage `code` con 900s de restart nhanh hon khi bi treo; van giu recovery continue prompt de ep tra final JSON.
