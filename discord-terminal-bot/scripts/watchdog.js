const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const ROOT = process.cwd();
const HEARTBEAT_FILE = path.join(ROOT, 'runtime', 'heartbeat.json');
const LOG_FILE = path.join(ROOT, 'logs', 'watchdog.log');
const APP_NAME = process.env.WATCHDOG_APP_NAME || 'discord-terminal-bot';
const STALE_SECONDS = Number(process.env.WATCHDOG_STALE_SECONDS || 90);

function log(message, meta) {
  const line = `[${new Date().toISOString()}] ${message}${meta ? ` ${JSON.stringify(meta)}` : ''}`;
  try {
    const dir = path.dirname(LOG_FILE);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.appendFileSync(LOG_FILE, line + '\n', 'utf-8');
  } catch {}
  console.log(line);
}

function readHeartbeat() {
  if (!fs.existsSync(HEARTBEAT_FILE)) return null;
  try {
    return JSON.parse(fs.readFileSync(HEARTBEAT_FILE, 'utf-8'));
  } catch {
    return null;
  }
}

function restartApp(reason, meta) {
  log(`Restarting app: ${reason}`, meta);
  execFileSync('pm2', ['restart', APP_NAME], { stdio: 'inherit' });
}

function main() {
  const heartbeat = readHeartbeat();
  if (!heartbeat) {
    restartApp('missing-heartbeat');
    return;
  }

  const ts = Date.parse(String(heartbeat.timestamp || ''));
  if (!Number.isFinite(ts)) {
    restartApp('invalid-heartbeat', { heartbeat });
    return;
  }

  const ageSeconds = Math.floor((Date.now() - ts) / 1000);
  if (ageSeconds > STALE_SECONDS) {
    restartApp('stale-heartbeat', { ageSeconds, staleAfter: STALE_SECONDS });
    return;
  }

  if (heartbeat.status === 'degraded') {
    restartApp('degraded-heartbeat', { heartbeat });
    return;
  }

  log('Healthy heartbeat', { ageSeconds, status: heartbeat.status });
}

try {
  main();
} catch (error) {
  log('Watchdog crashed', { error: String(error) });
  process.exitCode = 1;
}
