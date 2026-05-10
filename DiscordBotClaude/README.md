# Discord Bot -> Claude CLI (Python)

Bot nhận tin nhắn Discord, gửi prompt vào `claude` CLI, rồi trả kết quả về Discord. Có thêm chế độ Android agent loop.

## 1) Cài đặt

```bash
python -m venv .venv
source .venv/Scripts/activate
pip install -r requirements.txt
```

## 2) Cấu hình env

```bash
cp .env.example .env
```

Điền `DISCORD_TOKEN` và tùy chỉnh:

- `BOT_SKILLS_DIR=./skills` (skill đóng gói cùng bot)
- `HOST_SKILLS_DIR=.../.agents/skills` (skill máy host, nếu có)
- `SAFE_MODE=true` (bật policy an toàn)
- `AGENT_MAX_ITERS=3` (số vòng agent loop)

## 3) Chạy bot

```bash
python bot.py
```

## 4) Lệnh Discord

- `!chat <prompt>`: gọi Claude CLI thường
- `!android_agent <task>`: chạy Android coding agent loop
- `!selfcheck`: chạy `./gradlew lint` và `./gradlew test`

Ví dụ:

```text
!android_agent sửa lỗi crash khi mở HomeActivity và chạy test liên quan
```

## 5) Policy an toàn

Bot chặn mặc định:

- `rm -rf`
- `git reset --hard`
- `git push --force` / `git push -f`

Với `SAFE_MODE=true`, các lệnh Android/adb/git trực tiếp chỉ cho phép nếu nằm trong allowlist đã định nghĩa.
