import { EmbedBuilder, Colors } from 'discord.js';
import config from '../config';
import { TerminalSession, ProcessInfo } from '../types';
import { formatUptime, formatBytes } from '../utils';
import os from 'os';
import fs from 'fs';
import path from 'path';

export class LoggerService {
  private logStream: fs.WriteStream | null = null;
  private isShuttingDown = false;

  initialize(): void {
    this.isShuttingDown = false;
    if (config.logCommands) {
      try {
        const logDir = path.dirname(config.logFile);
        if (!fs.existsSync(logDir)) {
          fs.mkdirSync(logDir, { recursive: true });
        }
        this.logStream = fs.createWriteStream(config.logFile, { flags: 'a' });
        this.logStream.on('error', (err) => {
          console.error('Log stream error:', err.message);
          this.logStream = null;
        });
      } catch (error) {
        console.error('Failed to initialize log stream:', error);
      }
    }
  }

  log(level: 'info' | 'warn' | 'error' | 'debug', message: string, meta?: any): void {
    const timestamp = new Date().toISOString();
    const logMessage = `[${timestamp}] [${level.toUpperCase()}] ${message}${meta ? ' ' + JSON.stringify(meta) : ''}`;
    
    switch (level) {
      case 'error':
        console.error(logMessage);
        break;
      case 'warn':
        console.warn(logMessage);
        break;
      default:
        console.log(logMessage);
    }

    if (this.logStream && !this.isShuttingDown) {
      try {
        this.logStream.write(logMessage + '\n');
      } catch (error) {
        console.error('Failed to write to log file:', error);
      }
    }
  }

  info(message: string, meta?: any): void {
    this.log('info', message, meta);
  }

  warn(message: string, meta?: any): void {
    this.log('warn', message, meta);
  }

  error(message: string, meta?: any): void {
    this.log('error', message, meta);
  }

  debug(message: string, meta?: any): void {
    this.log('debug', message, meta);
  }

  createStatusEmbed(): EmbedBuilder {
    const osInfo = {
      platform: os.platform(),
      arch: os.arch(),
      cpus: os.cpus().length,
      totalMem: formatBytes(os.totalmem()),
      freeMem: formatBytes(os.freemem()),
      uptime: formatUptime(new Date(Date.now() - os.uptime() * 1000)),
      hostname: os.hostname(),
    };

    return new EmbedBuilder()
      .setTitle('System Status')
      .setColor(Colors.Green)
      .addFields(
        { name: 'Platform', value: `${osInfo.platform} (${osInfo.arch})`, inline: true },
        { name: 'CPU Cores', value: osInfo.cpus.toString(), inline: true },
        { name: 'Hostname', value: osInfo.hostname, inline: true },
        { name: 'Total RAM', value: osInfo.totalMem, inline: true },
        { name: 'Free RAM', value: osInfo.freeMem, inline: true },
        { name: 'System Uptime', value: osInfo.uptime, inline: true }
      )
      .setTimestamp();
  }

  createProcessEmbed(processInfo: ProcessInfo): EmbedBuilder {
    const duration = Date.now() - processInfo.startTime.getTime();
    
    return new EmbedBuilder()
      .setTitle('Running Process')
      .setColor(Colors.Blue)
      .addFields(
        { name: 'Command', value: `\`${processInfo.command}\``, inline: false },
        { name: 'PID', value: processInfo.process.pid?.toString() || 'N/A', inline: true },
        { name: 'Duration', value: `${duration}ms`, inline: true },
        { name: 'Start Time', value: processInfo.startTime.toLocaleTimeString(), inline: true }
      )
      .setTimestamp();
  }

  createSessionEmbed(session: TerminalSession): EmbedBuilder {
    return new EmbedBuilder()
      .setTitle('Terminal Session')
      .setColor(Colors.Purple)
      .addFields(
        { name: 'Channel ID', value: session.channelId, inline: true },
        { name: 'Current Directory', value: `\`${session.cwd}\``, inline: false },
        { name: 'History Count', value: session.history.length.toString(), inline: true },
        { name: 'Created At', value: session.createdAt.toLocaleString(), inline: true },
        { name: 'Active Process', value: session.activeProcess ? 'Yes' : 'No', inline: true }
      )
      .setTimestamp();
  }

  shutdown(): void {
    this.isShuttingDown = true;
    if (this.logStream) {
      try {
        this.logStream.end();
      } catch (error) {
        console.error('Error closing log stream:', error);
      }
      this.logStream = null;
    }
  }
}

export const loggerService = new LoggerService();
