import { Client, Collection, Guild, GuildBasedChannel, TextChannel, Channel, ThreadChannel } from 'discord.js';

export interface ProcessInfo {
  process: ReturnType<typeof import('child_process').spawn>;
  startTime: Date;
  command: string;
  channelId: string;
  userId: string;
  messageId?: string;
}

export interface TerminalSession {
  channelId: string;
  cwd: string;
  history: string[];
  activeProcess: ProcessInfo | null;
  createdAt: Date;
  selectedDeviceId?: string;
}

export interface CommandLog {
  id: string;
  userId: string;
  channelId: string;
  command: string;
  timestamp: Date;
  status: 'success' | 'failed' | 'timeout' | 'stopped';
  output?: string;
  duration?: number;
}

export interface CooldownInfo {
  userId: string;
  lastCommand: Date;
  commandCount: number;
}

export interface BotConfig {
  discordToken: string;
  clientId: string;
  guildId: string;
  telegramToken: string;
  ownerIds: string[];
  enableBlacklist: boolean;
  enableWhitelist: boolean;
  whitelistedCommands: string[];
  processTimeout: number;
  maxOutputLength: number;
  maxQueueSize: number;
  cooldownMs: number;
  defaultShell: string;
  maxHistoryPerChannel: number;
  logCommands: boolean;
  logFile: string;
  streamFlushIntervalMs: number;
  streamFlushMaxChars: number;
  eventLoopWatchdogIntervalMs: number;
  eventLoopLagWarnMs: number;
}

declare global {
  namespace NodeJS {
    interface ProcessEnv {
      DISCORD_TOKEN: string;
      CLIENT_ID: string;
      GUILD_ID: string;
      TELEGRAM_BOT_TOKEN: string;
      OWNER_IDS: string;
      ENABLE_BLACKLIST: string;
      ENABLE_WHITELIST: string;
      WHITELISTED_COMMANDS: string;
      PROCESS_TIMEOUT: string;
      MAX_OUTPUT_LENGTH: string;
      MAX_QUEUE_SIZE: string;
      COOLDOWN_MS: string;
      DEFAULT_SHELL: string;
      MAX_HISTORY_PER_CHANNEL: string;
      LOG_COMMANDS: string;
      LOG_FILE: string;
      STREAM_FLUSH_INTERVAL_MS: string;
      STREAM_FLUSH_MAX_CHARS: string;
      EVENT_LOOP_WATCHDOG_INTERVAL_MS: string;
      EVENT_LOOP_LAG_WARN_MS: string;
    }
  }
}

export interface ExtendedClient extends Client {
  commands: Collection<string, any>;
  sessions: Map<string, TerminalSession>;
  processQueue: Map<string, ProcessInfo[]>;
  commandLogs: CommandLog[];
  cooldowns: Map<string, CooldownInfo>;
  config: BotConfig;
}
