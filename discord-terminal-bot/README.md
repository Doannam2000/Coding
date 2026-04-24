# Discord Terminal Bot

Remote terminal bot for Discord with advanced CLI features. Execute commands on your server directly from Discord.

## Features

- Execute shell commands via Discord slash commands
- Real-time output streaming
- Multiple terminal sessions (one per channel)
- Command history per channel
- Process management (stop, view status)
- Security features (owner-only, command blacklist, cooldown)
- Cross-platform support (Windows, Linux, macOS)
- Beautiful Discord embeds with buttons
- Auto-reconnect on disconnect
- PM2 ready for production
- Telegram command menu and autonomous coding loop
- Heartbeat file + watchdog restart support for 24/7 uptime

## Requirements

- Node.js >= 18.0.0
- npm or yarn
- PM2 (optional, for production)

## Quick Start

### 1. Create a Discord Bot

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click "New Application" and give it a name
3. Go to "Bot" section and click "Add Bot"
4. Copy the **Bot Token** (keep it secret!)
5. Enable **Message Content Intent** in Bot > Privileged Gateway Intents
6. Go to "OAuth2" > "URL Generator"
   - Select scopes: `bot`, `application.commands`
   - Select permissions: `Send Messages`, `Embed Links`, `Attach Files`, `Use Slash Commands`
7. Use the generated URL to invite the bot

### 2. Configuration

```bash
# Clone or download the project
cd discord-terminal-bot

# Install dependencies
npm install

# Copy example env file
cp .env.example .env
```

Edit `.env` file:

```env
DISCORD_TOKEN=your_bot_token_here
CLIENT_ID=your_client_id_here
GUILD_ID=your_guild_id_here
OWNER_IDS=your_discord_user_id
```

To get your Discord user ID:
1. Enable Developer Mode in Discord
2. Right-click on your profile > "Copy User ID"

### 3. Run the Bot

#### Development Mode
```bash
npm run dev
```

#### Production Mode (PM2)
```bash
npm install -g pm2
npm run build
pm2 start ecosystem.config.js
pm2 save
pm2 startup
```

#### Watchdog Check
```bash
npm run watchdog:check
```

Recommended: run the watchdog from cron every minute so the bot restarts if heartbeat goes stale.

## Commands

| Command | Description |
|---------|-------------|
| `/ai <prompt>` | Ask OpenCode AI to help with coding |
| `/run <command>` | Execute a terminal command |
| `/autocode <goal>` | Run an autonomous multi-step coding loop |
| `/autocodestatus` | Show autonomous coding status |
| `/autocodestop` | Stop autonomous coding loop |
| `/status [type]` | Check status (session/process/system) |
| `/stop` | Stop the running process |
| `/cd <directory>` | Change working directory |
| `/history [lines] [--clear]` | View or clear command history |
| `/logs [lines] [--file]` | View command logs |
| `/sessions` | List all terminal sessions |
| `/upload <file> [path]` | Upload file to server |
| `/ping` | Check bot latency |
| `/help` | Show help message |

### AI Command Examples

```
/ai viết cho tôi 1 login form bằng React
/ai tạo API endpoint cho user authentication
/ai sửa bug trong file src/utils.ts
/ai viết unit test cho function calculateTotal
/ai refactor code trong thư mục src/components
```

### Autonomous Coding Examples

```text
/autocode thêm healthcheck endpoint và cập nhật README --steps 8
/autocode cải thiện telegram output handling --path C:\discord-terminal-bot --steps 12
/autocodestatus
/autocodestop
```

## AI Integration (OpenCode CLI)

The `/ai` command integrates with OpenCode CLI to provide AI-powered coding assistance.

### Installation

```bash
# Install OpenCode CLI globally
npm install -g opencode-ai

# Or use npx
npx opencode-ai
```

### How It Works

1. You send `/ai <prompt>` with your coding request
2. Bot calls OpenCode CLI with your prompt
3. OpenCode analyzes and performs the task (creates files, writes code, etc.)
4. Bot streams the output back to Discord
5. You can click **Stop** to cancel the task

### Features

- Real-time output streaming
- File creation and modification
- Code generation from natural language
- Multi-file project scaffolding
- Works with any programming language

## Security Features

- **Owner-only access**: Only users in `OWNER_IDS` can use commands
- **Dangerous command blacklist**: Commands like `rm -rf /` are blocked
- **Cooldown**: Prevents command spam (configurable in `.env`)
- **Command logging**: All commands are logged to file

### Blacklisted Commands

The following patterns are automatically blocked:
- `rm -rf /`, `rm -rf *`
- `shutdown`, `reboot`, `format`
- `del /f /q`, `mkfs`
- Dangerous redirects and pipes

## Project Structure

```
discord-terminal-bot/
├── src/
│   ├── commands/        # Discord slash commands
│   │   ├── run.ts       # Execute command
│   │   ├── status.ts    # Check status
│   │   ├── stop.ts      # Stop process
│   │   ├── cd.ts        # Change directory
│   │   ├── history.ts   # Command history
│   │   ├── logs.ts      # View logs
│   │   ├── sessions.ts  # List sessions
│   │   └── ping.ts      # Latency check
│   ├── services/        # Core services
│   │   ├── SecurityService.ts
│   │   ├── TerminalService.ts
│   │   ├── ProcessQueueService.ts
│   │   └── LoggerService.ts
│   ├── utils/           # Utility functions
│   │   └── index.ts
│   ├── types/           # TypeScript types
│   │   └── index.ts
│   ├── config/          # Configuration
│   │   └── index.ts
│   └── index.ts         # Main entry point
├── config/
├── logs/                # Log files
├── .env.example         # Example environment file
├── ecosystem.config.js # PM2 configuration
├── package.json
├── tsconfig.json
└── README.md
```

## Configuration Options

| Variable | Default | Description |
|----------|--------|-------------|
| `DISCORD_TOKEN` | - | Your bot token |
| `CLIENT_ID` | - | Bot application ID |
| `GUILD_ID` | - | Guild ID for guild commands |
| `OWNER_IDS` | - | Comma-separated Discord user IDs |
| `ENABLE_BLACKLIST` | `true` | Enable dangerous command blocking |
| `ENABLE_WHITELIST` | `false` | Enable command whitelist |
| `WHITELISTED_COMMANDS` | - | Allowed commands (if whitelist enabled) |
| `PROCESS_TIMEOUT` | `300000` | Max command execution time (ms) |
| `MAX_OUTPUT_LENGTH` | `4000` | Max output buffer size |
| `MAX_QUEUE_SIZE` | `5` | Max queued commands per channel |
| `COOLDOWN_MS` | `3000` | Cooldown between commands (ms) |
| `MAX_HISTORY_PER_CHANNEL` | `50` | Max history entries per channel |
| `LOG_COMMANDS` | `true` | Enable command logging |
| `STREAM_FLUSH_INTERVAL_MS` | `800` | Output stream flush interval (ms) |
| `STREAM_FLUSH_MAX_CHARS` | `3000` | Output buffer size before immediate flush |
| `EVENT_LOOP_WATCHDOG_INTERVAL_MS` | `10000` | Event-loop watchdog check interval (ms) |
| `EVENT_LOOP_LAG_WARN_MS` | `1500` | Lag threshold to trigger watchdog warning (ms) |

## Deploy on VPS

### Ubuntu/Debian

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Node.js 20.x
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Install PM2
sudo npm install -g pm2

# Clone/create project
cd /opt
sudo git clone https://github.com/your/repo.git discord-terminal-bot
cd discord-terminal-bot

# Install dependencies
sudo npm install
sudo npm run build

# Setup environment
sudo cp .env.example .env
sudo nano .env  # Edit with your values

# Set proper permissions
sudo chown -R $USER:$USER /opt/discord-terminal-bot

# Start with PM2
pm2 start ecosystem.config.js
pm2 save
pm2 startup  # Follow instructions

# Setup log rotation
pm2 install pm2-logrotate
pm2 set pm2-logrotate:max_size 10M
pm2 set pm2-logrotate:compress true
```

### Systemd Service (Alternative)

Create `/etc/systemd/system/discord-bot.service`:

```ini
[Unit]
Description=Discord Terminal Bot
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/discord-terminal-bot
ExecStart=/usr/bin/node dist/index.js
Restart=on-failure
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

Then:
```bash
sudo systemctl daemon-reload
sudo systemctl enable discord-bot
sudo systemctl start discord-bot
```

## Run on Windows

### Method 1: Command Prompt

```cmd
cd C:\path\to\discord-terminal-bot
npm install
npm run build
npm start
```

### Method 2: PowerShell

```powershell
cd C:\path\to\discord-terminal-bot
npm install
npm run build
node dist\index.js
```

### Method 3: PM2 on Windows

```cmd
npm install -g pm2
pm2 start ecosystem.config.js
```

## Troubleshooting

### Bot not responding to commands

1. Check if bot is online (shows green dot)
2. Verify `DISCORD_TOKEN` is correct
3. Check `CLIENT_ID` matches your application
4. Ensure bot has necessary permissions

### Commands fail with "Not Authorized"

- Add your Discord user ID to `OWNER_IDS` in `.env`
- Multiple IDs can be comma-separated

### Process timeout issues

- Increase `PROCESS_TIMEOUT` in `.env`
- Long-running commands may need more time

### Windows-specific issues

- Make sure PowerShell is available
- Run as Administrator if permission errors occur

## License

MIT
