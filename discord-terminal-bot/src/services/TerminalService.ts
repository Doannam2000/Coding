import { spawn, ChildProcess } from 'child_process';
import { EventEmitter } from 'events';
import config from '../config';
import { ProcessInfo, TerminalSession } from '../types';
import { decodeProcessChunk, getProcessEnv, normalizeWindowsShellCommand, truncateOutput, wrapWindowsUtf8Command } from '../utils';
import { loggerService } from './LoggerService';
import { runtimeStateService } from './RuntimeStateService';

export class TerminalService extends EventEmitter {
  private sessions: Map<string, TerminalSession> = new Map();
  private activeProcesses: Map<string, ProcessInfo> = new Map();
  private processTimeouts: Map<string, NodeJS.Timeout> = new Map();

  static readonly Events = {
    ProcessError: 'processError',
    ProcessExit: 'processExit',
    SessionError: 'sessionError',
  } as const;

  constructor() {
    super();
    this.loadSessions();
  }

  private loadSessions(): void {
    try {
      const parsed = runtimeStateService.getTerminalSessions();

      for (const item of parsed) {
        if (!item || typeof item.channelId !== 'string' || typeof item.cwd !== 'string') continue;
        const session: TerminalSession = {
          channelId: item.channelId,
          cwd: item.cwd,
          history: Array.isArray(item.history) ? item.history.filter((entry: unknown) => typeof entry === 'string') : [],
          activeProcess: null,
          createdAt: item.createdAt ? new Date(item.createdAt) : new Date(),
          selectedDeviceId: typeof item.selectedDeviceId === 'string' ? item.selectedDeviceId : undefined,
        };
        this.sessions.set(session.channelId, session);
      }

      loggerService.info('Restored terminal sessions', { count: this.sessions.size });
    } catch (error) {
      loggerService.warn('Failed to restore terminal sessions', { error: String(error) });
    }
  }

  private saveSessions(): void {
    try {
      runtimeStateService.setTerminalSessions(Array.from(this.sessions.values()).map((session) => ({
        channelId: session.channelId,
        cwd: session.cwd,
        history: session.history.slice(-config.maxHistoryPerChannel),
        createdAt: session.createdAt.toISOString(),
        selectedDeviceId: session.selectedDeviceId,
      })));
    } catch (error) {
      loggerService.error('Failed to persist terminal sessions', { error: String(error) });
    }
  }

  createSession(channelId: string): TerminalSession {
    if (this.sessions.has(channelId)) {
      return this.sessions.get(channelId)!;
    }

    const session: TerminalSession = {
      channelId,
      cwd: process.cwd(),
      history: [],
      activeProcess: null,
      createdAt: new Date(),
      selectedDeviceId: undefined,
    };

    this.sessions.set(channelId, session);
    this.saveSessions();
    return session;
  }

  getSession(channelId: string): TerminalSession | undefined {
    return this.sessions.get(channelId);
  }

  getOrCreateSession(channelId: string): TerminalSession {
    return this.getSession(channelId) || this.createSession(channelId);
  }

  async executeCommand(
    channelId: string,
    userId: string,
    command: string,
    onOutput: (data: string, type: 'stdout' | 'stderr') => void,
    onComplete: (exitCode: number | null, signal: string | null) => void | Promise<void>,
    timeoutMinutes?: number
  ): Promise<string> {
    const session = this.getOrCreateSession(channelId);
    
    if (session.history.length >= config.maxHistoryPerChannel) {
      session.history = session.history.slice(-config.maxHistoryPerChannel + 1);
    }
    session.history.push(command);
    this.saveSessions();

    if (this.activeProcesses.has(channelId)) {
      throw new Error('A process is already running in this channel. Stop it first.');
    }

    const isWindows = process.platform === 'win32';
    
    let proc: ChildProcess;
    if (isWindows) {
      const normalizedCommand = normalizeWindowsShellCommand(command);
      const fullCommand = wrapWindowsUtf8Command(normalizedCommand);
      proc = spawn('cmd.exe', ['/d', '/c', fullCommand], {
        cwd: session.cwd,
        env: getProcessEnv(),
        windowsHide: true,
        shell: false,
      });
    } else {
      proc = spawn('/bin/bash', ['-c', command], {
        cwd: session.cwd,
        env: getProcessEnv(),
        shell: false,
      });
    }

    const processInfo: ProcessInfo = {
      process: proc as any,
      startTime: new Date(),
      command,
      channelId,
      userId,
    };

    this.activeProcesses.set(channelId, processInfo);
    session.activeProcess = processInfo;

    const timeoutMs = timeoutMinutes ? timeoutMinutes * 60 * 1000 : config.processTimeout;

    let outputBuffer = '';

    proc.stdout?.on('data', (data: Buffer) => {
      const text = decodeProcessChunk(data);
      outputBuffer += text;
      onOutput(text, 'stdout');
    });

    proc.stderr?.on('data', (data: Buffer) => {
      const text = decodeProcessChunk(data);
      outputBuffer += text;
      onOutput(text, 'stderr');
    });

    let completionNotified = false;
    let promiseResolved = false;
    let resolvePromise: ((value: string) => void) | null = null;
    let completionPromise: Promise<void> | null = null;

    const notifyCompleteOnce = (code: number | null, signal: string | null): Promise<void> => {
      if (completionPromise) return completionPromise;
      completionNotified = true;
      completionPromise = Promise.resolve(onComplete(code, signal)).catch((error) => {
        loggerService.error('Process completion callback failed', {
          channelId,
          command,
          code,
          signal,
          error: error instanceof Error ? error.message : String(error),
        });
      });
      return completionPromise;
    };

    const resolveOnce = (value: string): void => {
      if (promiseResolved) return;
      promiseResolved = true;
      resolvePromise?.(value);
    };

    const timeout = setTimeout(() => {
      this.killProcess(channelId);

      setTimeout(() => {
        if (completionNotified) return;
        loggerService.warn('Process timeout fallback triggered without close event', {
          channelId,
          command,
          timeoutMs,
        });
        this.clearProcess(channelId);
        void notifyCompleteOnce(null, 'SIGTERM').finally(() => {
          resolveOnce(truncateOutput(outputBuffer, config.maxOutputLength));
        });
      }, 8000);
    }, timeoutMs);

    this.processTimeouts.set(channelId, timeout);

    return new Promise((resolve) => {
      resolvePromise = resolve;

      proc.on('close', (code, signal) => {
        const duration = Date.now() - processInfo.startTime.getTime();
        this.clearProcess(channelId);
        const finalize = notifyCompleteOnce(code, signal);
        if (code === 0 || signal) {
          loggerService.debug('Process completed', { channelId, code, signal, duration });
        } else {
          loggerService.warn('Process failed with non-zero exit code', { channelId, code, duration });
        }
        const output = truncateOutput(outputBuffer, config.maxOutputLength);
        void Promise.race([
          finalize,
          new Promise<void>((resolve) => setTimeout(resolve, 15000)),
        ]).finally(() => {
          resolveOnce(output);
        });
      });

      proc.on('error', (error) => {
        this.clearProcess(channelId);
        loggerService.error('Process error', { 
          channelId, 
          command,
          error: error.message 
        });
        onOutput(`Error: ${error.message}`, 'stderr');
        void notifyCompleteOnce(-1, null).finally(() => {
          resolveOnce(`Error: ${error.message}`);
        });
      });
    });
  }

  killProcess(channelId: string): boolean {
    const processInfo = this.activeProcesses.get(channelId);
    if (!processInfo) return false;

    try {
      if (process.platform === 'win32') {
        spawn('taskkill', ['/pid', processInfo.process.pid!.toString(), '/f', '/t']);
      } else {
        processInfo.process.kill('SIGTERM');
        setTimeout(() => {
          if (!processInfo.process.killed) {
            processInfo.process.kill('SIGKILL');
          }
        }, 5000);
      }
    } catch (error) {
      loggerService.error('Failed to kill process', { 
        channelId, 
        error: error instanceof Error ? error.message : String(error),
        pid: processInfo.process.pid 
      });
      return false;
    }

    this.clearProcess(channelId);
    return true;
  }

  private clearProcess(channelId: string): void {
    const timeout = this.processTimeouts.get(channelId);
    if (timeout) {
      clearTimeout(timeout);
      this.processTimeouts.delete(channelId);
    }

    this.activeProcesses.delete(channelId);
    
    const session = this.sessions.get(channelId);
    if (session) {
      session.activeProcess = null;
      this.saveSessions();
    }
  }

  isProcessRunning(channelId: string): boolean {
    return this.activeProcesses.has(channelId);
  }

  getActiveProcess(channelId: string): ProcessInfo | undefined {
    return this.activeProcesses.get(channelId);
  }

  changeDirectory(channelId: string, newCwd: string): boolean {
    const session = this.getOrCreateSession(channelId);

    try {
      const fs = require('fs');
      if (fs.existsSync(newCwd) && fs.statSync(newCwd).isDirectory()) {
        session.cwd = newCwd;
        this.saveSessions();
        return true;
      } else {
        loggerService.warn('Directory does not exist or is not accessible', { channelId, newCwd });
      }
    } catch (error) {
      loggerService.error('Failed to change directory', { 
        channelId, 
        newCwd, 
        error: error instanceof Error ? error.message : String(error) 
      });
    }
    return false;
  }

  getHistory(channelId: string): string[] {
    return this.sessions.get(channelId)?.history || [];
  }

  getSelectedDevice(channelId: string): string | undefined {
    return this.sessions.get(channelId)?.selectedDeviceId;
  }

  setSelectedDevice(channelId: string, deviceId?: string): boolean {
    const session = this.getOrCreateSession(channelId);
    session.selectedDeviceId = deviceId || undefined;
    this.saveSessions();
    return true;
  }

  clearHistory(channelId: string): void {
    const session = this.sessions.get(channelId);
    if (session) {
      session.history = [];
      this.saveSessions();
    }
  }

  destroySession(channelId: string): void {
    if (this.isProcessRunning(channelId)) {
      this.killProcess(channelId);
    }
    this.sessions.delete(channelId);
    this.saveSessions();
  }

  clearAllSessions(): number {
    const sessionIds = Array.from(this.sessions.keys());
    for (const channelId of sessionIds) {
      this.destroySession(channelId);
    }
    return sessionIds.length;
  }

  getAllSessions(): TerminalSession[] {
    return Array.from(this.sessions.values());
  }
}

export const terminalService = new TerminalService();
