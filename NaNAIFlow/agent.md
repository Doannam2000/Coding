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

### 2026-04-22

- `fixed`: Tang do ben polling Telegram trong `getUpdates` (retry + backoff cho loi mang, gom reset ket noi WinError 10054).
- `changed`: Tach nhom loi mang thanh `TelegramNetworkError` de xu ly rieng.
- `changed`: Giam nhieu log polling bang co che dem loi lien tiep, chi in canh bao theo moc.
- `changed`: `KeyboardInterrupt` thoat bot gon gon (khong con in traceback khong can thiet).
