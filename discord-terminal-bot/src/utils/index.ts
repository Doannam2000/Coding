import fs from 'fs';
import os from 'os';
import path from 'path';

export function getOS(): 'windows' | 'linux' | 'mac' {
  const platform = os.platform();
  if (platform === 'win32') return 'windows';
  if (platform === 'darwin') return 'mac';
  return 'linux';
}

export function getShell(): { shell: string; args: string[] } {
  const osType = getOS();
  
  if (osType === 'windows') {
    const systemRoot = process.env.SystemRoot || 'C:\\Windows';
    return { shell: process.env.ComSpec || path.join(systemRoot, 'System32', 'cmd.exe'), args: ['/d', '/c'] };
  }
  
  return { shell: '/bin/bash', args: ['-c'] };
}

export function getOpenCodeLauncher(): { command: string; args: string[] } {
  if (process.platform !== 'win32') {
    return { command: 'opencode', args: [] };
  }

  const appData = process.env.OPENCODE_BIN_PATH
    ? null
    : (process.env.APPDATA || path.join(process.env.USERPROFILE || 'C:\\Users\\Default', 'AppData', 'Roaming'));

  const candidates = [
    process.env.OPENCODE_BIN_PATH,
    appData ? path.join(appData, 'npm', 'node_modules', 'opencode-ai', 'bin', 'opencode') : null,
    appData ? path.join(appData, 'npm', 'opencode.cmd') : null,
    appData ? path.join(appData, 'npm', 'opencode') : null,
  ].filter((candidate): candidate is string => !!candidate);

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return { command: process.execPath, args: [candidate] };
    }
  }

  return { command: 'opencode', args: [] };
}

export function getProcessEnv(extraEnv: NodeJS.ProcessEnv = {} as NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  const env = { ...process.env, ...extraEnv } as NodeJS.ProcessEnv;

  if (process.platform === 'win32') {
    env.PYTHONUTF8 = env.PYTHONUTF8 || '1';
    env.PYTHONIOENCODING = env.PYTHONIOENCODING || 'utf-8';
  }

  return env;
}

export function wrapWindowsUtf8Command(command: string): string {
  if (process.platform !== 'win32') return command;
  return `chcp 65001>nul & ${command}`;
}

export function normalizeWindowsShellCommand(command: string): string {
  if (process.platform !== 'win32') return command;

  const trimmed = command.trim();
  if (!trimmed) return command;

  const aliasRules: Array<{ pattern: RegExp; replace: string | ((match: RegExpMatchArray) => string) }> = [
    { pattern: /^ls(\s+-a)?$/i, replace: (match) => match[1] ? 'dir /a' : 'dir' },
    { pattern: /^pwd$/i, replace: 'cd' },
    { pattern: /^cat\s+(.+)$/i, replace: (match) => `type ${match[1]}` },
    { pattern: /^rm\s+(-rf|-fr|-r|-f)?\s*(.+)$/i, replace: (match) => {
      const flags = (match[1] || '').toLowerCase();
      const target = match[2];
      if (flags.includes('r')) return `rmdir /s /q ${target}`;
      return `del /f /q ${target}`;
    } },
    { pattern: /^cp\s+(.+?)\s+(.+)$/i, replace: (match) => `copy ${match[1]} ${match[2]}` },
    { pattern: /^mv\s+(.+?)\s+(.+)$/i, replace: (match) => `move ${match[1]} ${match[2]}` },
    { pattern: /^touch\s+(.+)$/i, replace: (match) => `powershell -NoProfile -Command "New-Item -ItemType File -Path ${match[1]} -Force | Out-Null"` },
    { pattern: /^mkdir\s+-p\s+(.+)$/i, replace: (match) => `powershell -NoProfile -Command "New-Item -ItemType Directory -Path ${match[1]} -Force | Out-Null"` },
    { pattern: /^which\s+(.+)$/i, replace: (match) => `where ${match[1]}` },
    { pattern: /^clear$/i, replace: 'cls' },
  ];

  for (const rule of aliasRules) {
    const matched = trimmed.match(rule.pattern);
    if (!matched) continue;
    return typeof rule.replace === 'function' ? rule.replace(matched) : rule.replace;
  }

  return command;
}

export function decodeProcessChunk(data: Buffer | string): string {
  if (typeof data === 'string') return data;
  return data.toString('utf8');
}

export function getCwd(): string {
  return process.cwd();
}

export function parseArgs(input: string): { command: string; args: string[] } {
  const parts = input.match(/(?:[^\s"]+|"[^"]*")+/g) || [];
  const command = parts[0] || '';
  const args = parts.slice(1).map(arg => arg.replace(/^"|"$/g, ''));
  
  return { command, args };
}

export function truncateOutput(output: string, maxLength: number): string {
  if (output.length <= maxLength) return output;
  return output.substring(0, maxLength - 100) + '\n\n... (Output truncated)';
}

export function splitMessage(text: string, maxLength: number = 4000): string[] {
  if (maxLength <= 0) return [text];
  const messages: string[] = [];
  const lines = text.split('\n');
  let currentMessage = '';

  for (const line of lines) {
    if (line.length > maxLength) {
      if (currentMessage) {
        messages.push(currentMessage);
        currentMessage = '';
      }

      let remaining = line;
      while (remaining.length > maxLength) {
        messages.push(remaining.slice(0, maxLength));
        remaining = remaining.slice(maxLength);
      }
      currentMessage = remaining;
      continue;
    }

    if (currentMessage.length + line.length + 1 > maxLength) {
      if (currentMessage) messages.push(currentMessage);
      currentMessage = line;
    } else {
      currentMessage += (currentMessage ? '\n' : '') + line;
    }
  }

  if (currentMessage) messages.push(currentMessage);
  return messages.length > 0 ? messages : [''];
}

export function buildCodeBlockMessages(text: string, header: string, maxLength: number): string[] {
  const reserve = header.length + 32;
  const bodyMaxLength = Math.max(200, maxLength - reserve);
  const parts = splitMessage(text || 'No response', bodyMaxLength);

  return parts.map((part, index) => {
    const suffix = parts.length > 1 ? ` (Part ${index + 1}/${parts.length})` : '';
    return `${header}${suffix}\n\`\`\`\n${part}\n\`\`\``;
  });
}

export function escapeHtml(text: string): string {
  return (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function escapedHtmlCharLength(char: string): number {
  if (char === '&') return 5;
  if (char === '<' || char === '>') return 4;
  return 1;
}

function splitTextByEscapedHtmlLength(text: string, maxEscapedLength: number): string[] {
  const source = text || '';
  if (!source) return [''];

  const chunks: string[] = [];
  let current = '';
  let escapedLength = 0;

  for (const char of source) {
    const charLen = escapedHtmlCharLength(char);
    if (escapedLength + charLen > maxEscapedLength && current.length > 0) {
      chunks.push(current);
      current = char;
      escapedLength = charLen;
      continue;
    }

    current += char;
    escapedLength += charLen;
  }

  if (current.length > 0) chunks.push(current);
  return chunks.length > 0 ? chunks : [''];
}

export function buildTelegramHtmlCodeBlockMessages(text: string, header: string, maxLength: number): string[] {
  const plainHeader = (header || '').replace(/[*_`~]/g, '').trim() || 'Output';
  const maxEscapedBodyLength = Math.max(200, Math.min(3000, maxLength - plainHeader.length - 128));
  const parts = splitTextByEscapedHtmlLength(text || 'No response', maxEscapedBodyLength);

  return parts.map((part, index) => {
    const suffix = parts.length > 1 ? ` (Part ${index + 1}/${parts.length})` : '';
    const headerHtml = `<b>${escapeHtml(plainHeader + suffix)}</b>`;
    const bodyHtml = `<pre>${escapeHtml(part)}</pre>`;
    return `${headerHtml}\n${bodyHtml}`;
  });
}

export interface CoalescedAsyncRenderer<T> {
  schedule: (value: T) => void;
  flush: () => Promise<void>;
}

export function createCoalescedAsyncRenderer<T>(
  render: (value: T) => Promise<void>,
  onError?: (error: unknown, value: T) => void,
): CoalescedAsyncRenderer<T> {
  let hasPending = false;
  let pendingValue: T | undefined;
  let runner: Promise<void> | null = null;

  const reportError = (error: unknown, value: T): void => {
    if (!onError) return;
    try {
      onError(error, value);
    } catch {}
  };

  const drain = async (): Promise<void> => {
    while (hasPending) {
      const nextValue = pendingValue as T;
      hasPending = false;

      try {
        await render(nextValue);
      } catch (error) {
        reportError(error, nextValue);
      }
    }
  };

  const ensureRunner = (): Promise<void> => {
    if (!runner) {
      runner = drain().finally(() => {
        runner = null;
        if (hasPending) {
          void ensureRunner();
        }
      });
    }

    return runner;
  };

  return {
    schedule(value: T): void {
      pendingValue = value;
      hasPending = true;
      void ensureRunner();
    },
    async flush(): Promise<void> {
      while (hasPending || runner) {
        await (runner || ensureRunner());
      }
    },
  };
}

export function formatUptime(startTime: Date): string {
  const seconds = Math.floor((Date.now() - startTime.getTime()) / 1000);
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;

  const parts: string[] = [];
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0) parts.push(`${minutes}m`);
  if (secs > 0 || parts.length === 0) parts.push(`${secs}s`);

  return parts.join(' ');
}

export function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export function sanitizeInput(input: string): string {
  return input
    .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '')
    .trim();
}

export function toDisplayText(value: unknown): string {
  if (typeof value === 'string') return value;
  if (value === null || value === undefined) return '';
  if (value instanceof Error) return value.message || String(value);

  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

export function normalizeErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    const maybeAny = error as any;
    if (typeof maybeAny.message === 'string' && maybeAny.message.trim()) {
      const msg = maybeAny.message.trim();
      if (msg !== '[object Object]' && msg.toLowerCase() !== 'object object') {
        return msg;
      }
    }

    const detailCandidates = [maybeAny.cause, maybeAny.error, maybeAny.details, maybeAny.response?.data, maybeAny.data];
    for (const detail of detailCandidates) {
      const detailText = toDisplayText(detail).trim();
      if (detailText && detailText !== '[object Object]') {
        return detailText;
      }
    }

    if (maybeAny.cause) {
      const causeText = toDisplayText(maybeAny.cause);
      if (causeText.trim()) return causeText;
    }

    const fullError = toDisplayText(maybeAny).trim();
    if (fullError && fullError !== '[object Object]') {
      return fullError;
    }
    return 'Unknown error';
  }

  const text = toDisplayText(error);
  return text.trim() || 'Unknown error';
}

export function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substring(2);
}
