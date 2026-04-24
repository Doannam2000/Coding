import dotenv from 'dotenv';
import { BotConfig } from '../types';

dotenv.config();

const config: BotConfig = {
  discordToken: process.env.DISCORD_TOKEN || '',
  clientId: process.env.CLIENT_ID || '',
  guildId: process.env.GUILD_ID || '',
  telegramToken: process.env.TELEGRAM_BOT_TOKEN || '',
  ownerIds: (process.env.OWNER_IDS || '503916839638925332').split(',').filter(id => id.trim() !== ''),
  enableBlacklist: process.env.ENABLE_BLACKLIST !== 'false',
  enableWhitelist: process.env.ENABLE_WHITELIST === 'true',
  whitelistedCommands: (process.env.WHITELISTED_COMMANDS || '').split(',').filter(cmd => cmd.trim() !== ''),
  processTimeout: parseInt(process.env.PROCESS_TIMEOUT || '600000', 10),
  maxOutputLength: parseInt(process.env.MAX_OUTPUT_LENGTH || '4000', 10),
  maxQueueSize: parseInt(process.env.MAX_QUEUE_SIZE || '5', 10),
  cooldownMs: parseInt(process.env.COOLDOWN_MS || '3000', 10),
  defaultShell: process.env.DEFAULT_SHELL || 'auto',
  maxHistoryPerChannel: parseInt(process.env.MAX_HISTORY_PER_CHANNEL || '50', 10),
  logCommands: process.env.LOG_COMMANDS !== 'false',
  logFile: process.env.LOG_FILE || './logs/commands.log',
  streamFlushIntervalMs: parseInt(process.env.STREAM_FLUSH_INTERVAL_MS || '800', 10),
  streamFlushMaxChars: parseInt(process.env.STREAM_FLUSH_MAX_CHARS || '3000', 10),
  eventLoopWatchdogIntervalMs: parseInt(process.env.EVENT_LOOP_WATCHDOG_INTERVAL_MS || '10000', 10),
  eventLoopLagWarnMs: parseInt(process.env.EVENT_LOOP_LAG_WARN_MS || '1500', 10),
};

export const token = config.discordToken;
export const clientId = config.clientId;
export const guildId = config.guildId;
export const telegramToken = config.telegramToken;

export default config;
