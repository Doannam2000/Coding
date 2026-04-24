import config from '../config';
import { CommandLog, CooldownInfo } from '../types';
import { generateId } from '../utils';
import fs from 'fs';
import path from 'path';
import { loggerService } from './LoggerService';

const DANGEROUS_PATTERNS = [
  /rm\s+-rf\s+\//i,
  /rm\s+-rf\s+\*/i,
  /shutdown/i,
  /reboot/i,
  /format/i,
  /del\s+\/f\s+\/q/i,
  /del\s+\/s\s+\/q/i,
  /mkfs/i,
  /dd\s+if=/i,
  />:.*\/etc\/passwd/i,
  /chmod\s+-R\s+777\s+\//i,
  /eval\s*\(/i,
  /exec\s*\(/i,
  /;.*rm\s+-rf/i,
  /\|\s*rm\s+-rf/i,
  /&&\s*rm\s+-rf/i,
  /\&\&rm\s+-rf/i,
  /\|\|rm\s+-rf/i,
];

const COMMAND_ALIASES: Record<string, string> = {
  'll': 'ls -la',
  'la': 'ls -la',
  'dir': 'ls',
  'cls': 'clear',
  'copy': 'cp',
  'move': 'mv',
  'del': 'rm',
  'md': 'mkdir',
  'rd': 'rmdir',
  'type': 'cat',
  'ren': 'mv',
};

export class SecurityService {
  private cooldownMap: Map<string, CooldownInfo> = new Map();
  private failedAttempts: Map<string, number> = new Map();
  private commandLogs: CommandLog[] = [];

  isOwner(userId: string): boolean {
    const ownerIds = config.ownerIds.length > 0 ? config.ownerIds : ['503916839638925332'];
    return ownerIds.includes(userId);
  }

  validateCommand(command: string): { valid: boolean; reason?: string } {
    const trimmedCommand = command.trim();
    
    if (!trimmedCommand) {
      return { valid: false, reason: 'Command cannot be empty' };
    }

    for (const pattern of DANGEROUS_PATTERNS) {
      if (pattern.test(trimmedCommand)) {
        return { valid: false, reason: `Command blocked: potentially dangerous pattern detected` };
      }
    }

    if (config.enableBlacklist) {
      const lowerCommand = trimmedCommand.toLowerCase();
      for (const pattern of DANGEROUS_PATTERNS) {
        if (pattern.test(lowerCommand)) {
          return { valid: false, reason: 'Command is in the blacklist' };
        }
      }
    }

    if (config.enableWhitelist && config.whitelistedCommands.length > 0) {
      const cmdName = trimmedCommand.split(/\s+/)[0].toLowerCase();
      if (!config.whitelistedCommands.includes(cmdName)) {
        return { valid: false, reason: 'Command not in whitelist' };
      }
    }

    return { valid: true };
  }

  resolveAlias(command: string): string {
    const cmdName = command.trim().split(/\s+/)[0].toLowerCase();
    if (COMMAND_ALIASES[cmdName]) {
      return command.replace(cmdName, COMMAND_ALIASES[cmdName]);
    }
    return command;
  }

  checkCooldown(userId: string): { allowed: boolean; remainingMs?: number } {
    const info = this.cooldownMap.get(userId);
    if (!info) {
      this.cooldownMap.set(userId, { userId, lastCommand: new Date(), commandCount: 1 });
      return { allowed: true };
    }

    const elapsed = Date.now() - info.lastCommand.getTime();
    if (elapsed < config.cooldownMs) {
      return { allowed: false, remainingMs: config.cooldownMs - elapsed };
    }

    info.lastCommand = new Date();
    info.commandCount++;
    return { allowed: true };
  }

  getCommandCount(userId: string): number {
    return this.cooldownMap.get(userId)?.commandCount || 0;
  }

  logCommand(log: Omit<CommandLog, 'id' | 'timestamp'>): CommandLog {
    const fullLog: CommandLog = {
      ...log,
      id: generateId(),
      timestamp: new Date(),
    };
    
    this.commandLogs.push(fullLog);
    
    if (this.commandLogs.length > 1000) {
      this.commandLogs = this.commandLogs.slice(-500);
    }

    if (config.logCommands) {
      this.writeLogToFile(fullLog);
    }

    return fullLog;
  }

  private writeLogToFile(log: CommandLog): void {
    try {
      const logDir = path.dirname(config.logFile);
      if (!fs.existsSync(logDir)) {
        fs.mkdirSync(logDir, { recursive: true });
      }
      
      const logLine = `[${log.timestamp.toISOString()}] [${log.status.toUpperCase()}] User: ${log.userId} | Channel: ${log.channelId} | Command: ${log.command}${log.output ? ` | Output: ${log.output.substring(0, 200)}` : ''}\n`;
      
      fs.appendFileSync(config.logFile, logLine);
    } catch (error) {
      loggerService.error('Failed to write command log to file', { 
        error: error instanceof Error ? error.message : String(error) 
      });
    }
  }

  getRecentLogs(limit: number = 50): CommandLog[] {
    return this.commandLogs.slice(-limit).reverse();
  }

  recordFailedAttempt(userId: string): number {
    const attempts = (this.failedAttempts.get(userId) || 0) + 1;
    this.failedAttempts.set(userId, attempts);
    
    if (attempts > 10) {
      setTimeout(() => {
        this.failedAttempts.delete(userId);
      }, 60000);
    }
    
    return attempts;
  }

  getFailedAttempts(userId: string): number {
    return this.failedAttempts.get(userId) || 0;
  }

  clearFailedAttempts(userId: string): void {
    this.failedAttempts.delete(userId);
  }
}

export const securityService = new SecurityService();
