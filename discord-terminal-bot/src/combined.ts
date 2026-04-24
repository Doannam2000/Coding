import { Client, GatewayIntentBits, Collection, Events, REST, Routes } from 'discord.js';
import TelegramBot, { Message, CallbackQuery } from 'node-telegram-bot-api';
import config, { token, clientId, guildId, telegramToken } from './config';
import { loggerService, securityService, terminalService, aiService, memoryService, figmaService, gitService, fileService, deployService, testService, reviewService, databaseService, speechService, androidService, permissionBrokerService, runtimeStateService } from './services';
import type { SupportedCLI } from './services/AIService';
import { splitMessage, truncateOutput, getOS, normalizeErrorMessage, buildCodeBlockMessages, buildTelegramHtmlCodeBlockMessages, getOpenCodeLauncher } from './utils';
import { buildCurrentContext } from './utils/current';
import { execFileSync, execSync } from 'child_process';
import path from 'path';
import fs from 'fs';

const BOT_VERSION = '1.0.6';
const HEARTBEAT_DIR = path.join(process.cwd(), 'runtime');
const HEARTBEAT_FILE = path.join(HEARTBEAT_DIR, 'heartbeat.json');

let heartbeatTimer: NodeJS.Timeout | null = null;
let eventLoopWatchdogTimer: NodeJS.Timeout | null = null;

function writeHeartbeat(status: 'ok' | 'degraded' | 'stopping', meta: Record<string, unknown> = {}): void {
  try {
    if (!fs.existsSync(HEARTBEAT_DIR)) {
      fs.mkdirSync(HEARTBEAT_DIR, { recursive: true });
    }

    const payload = {
      status,
      pid: process.pid,
      timestamp: new Date().toISOString(),
      uptimeSeconds: Math.floor(process.uptime()),
      memoryMb: Math.round(process.memoryUsage().rss / 1024 / 1024),
      ...meta,
    };
    fs.writeFileSync(HEARTBEAT_FILE, JSON.stringify(payload, null, 2), 'utf-8');
  } catch (error) {
    loggerService.warn('Failed to write heartbeat', { error: String(error) });
  }
}

function startHeartbeatLoop(): void {
  writeHeartbeat('ok', { phase: 'startup' });
  heartbeatTimer = setInterval(() => {
    writeHeartbeat('ok', { phase: 'running' });
  }, 20000);
  heartbeatTimer.unref();
}

function stopHeartbeatLoop(): void {
  if (!heartbeatTimer) return;
  clearInterval(heartbeatTimer);
  heartbeatTimer = null;
}

function startEventLoopWatchdog(): void {
  const checkIntervalMs = Math.max(1000, config.eventLoopWatchdogIntervalMs || 10000);
  const warnThresholdMs = Math.max(100, config.eventLoopLagWarnMs || 1500);
  let expectedAt = Date.now() + checkIntervalMs;

  eventLoopWatchdogTimer = setInterval(() => {
    const now = Date.now();
    const lagMs = Math.max(0, now - expectedAt);
    expectedAt = now + checkIntervalMs;

    if (lagMs < warnThresholdMs) return;

    writeHeartbeat('degraded', {
      phase: 'event-loop-lag',
      lagMs,
      thresholdMs: warnThresholdMs,
    });

    loggerService.warn('Event loop lag detected', {
      lagMs,
      thresholdMs: warnThresholdMs,
      memoryMb: Math.round(process.memoryUsage().rss / 1024 / 1024),
    });
  }, checkIntervalMs);

  eventLoopWatchdogTimer.unref();
}

function stopEventLoopWatchdog(): void {
  if (!eventLoopWatchdogTimer) return;
  clearInterval(eventLoopWatchdogTimer);
  eventLoopWatchdogTimer = null;
}

interface ExtendedClient extends Client {
  commands: Collection<any, any>;
  runningProcesses: Map<string, string>;
}

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

interface ChatSession {
  messages: ChatMessage[];
  workdir: string;
}

interface ActiveRequest {
  abort: () => void;
}

interface CombinedRuntimeState {
  currentCLI?: 'opencode' | 'claude' | 'codex';
  chatSessions: Array<{
    key: string;
    workdir: string;
    messages: ChatMessage[];
  }>;
  projectContexts: Array<{
    chatId: string;
    content: string;
    timestamp: number;
    used?: boolean;
  }>;
  selectedAISessionsByChat?: Array<{
    chatId: string;
    cli: 'opencode' | 'claude' | 'codex';
    workdir: string;
    sessionId: string;
  }>;
}

interface QueuedTelegramChatRequest {
  userId: string;
  message: string;
  workdir: string;
  model: string;
  cli: SupportedCLI;
  enqueuedAt: number;
}

const discordClient: ExtendedClient = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent,
    GatewayIntentBits.DirectMessages,
  ],
}) as ExtendedClient;

const git = gitService;
const files = fileService;
const deploy = deployService;
const tests = testService;
const review = reviewService;
const db = databaseService;
const speech = speechService;
const figma = figmaService;

function extractTokenValue(output: string, patterns: RegExp[]): string | null {
  for (const pattern of patterns) {
    const found = output.match(pattern);
    if (found?.[1]) return found[1].trim();
  }
  return null;
}

function extractTokenPercent(value: string | null): number | null {
  if (!value) return null;
  const match = value.match(/(\d+(?:\.\d+)?)\s*%/);
  return match ? Number(match[1]) : null;
}

function buildTokenBar(percent: number, size: number = 10): string {
  const clamped = Math.max(0, Math.min(100, percent));
  const filled = Math.round((clamped / 100) * size);
  return `${'█'.repeat(filled)}${'░'.repeat(size - filled)} ${clamped.toFixed(1)}%`;
}

function buildTokenAlert(remainingPercent: number | null): string | null {
  if (remainingPercent === null) return null;
  if (remainingPercent <= 10) return '🚨 Remaining quota is critically low';
  if (remainingPercent <= 20) return '⚠️ Remaining quota is getting low';
  return null;
}

function extractTokenModelBreakdown(output: string): string[] {
  return output
    .split('\n')
    .map(line => line.trim())
    .filter(line => /[a-z0-9_-]+\/[a-z0-9._-]+/i.test(line) && /(token|cost|\$|%)/i.test(line))
    .slice(0, 6);
}

function getTelegramModelsPage<T>(items: T[], page: number, pageSize: number = 8): T[] {
  const start = page * pageSize;
  return items.slice(start, start + pageSize);
}

function modelDisplayName(modelId: string): string {
  const raw = (modelId || '').trim();
  const idx = raw.indexOf('/');
  return idx >= 0 ? raw.slice(idx + 1) : raw;
}

function formatViDateTime(value: any): string {
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp) || timestamp <= 0) return 'N/A';
  return new Date(timestamp).toLocaleString('vi-VN');
}

function buildAIArtifactsDump(response: any): string {
  const stripAnsi = (value: string): string => value.replace(/\x1b\[[0-9;]*m/g, '');
  const decodeHtmlEntities = (value: string): string => value
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&nbsp;/g, ' ')
    .replace(/&#x([0-9a-fA-F]+);/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
    .replace(/&#(\d+);/g, (_, num) => String.fromCharCode(parseInt(num, 10)));
  const htmlToText = (value: string): string => {
    const normalized = decodeHtmlEntities(value)
      .replace(/<\s*br\s*\/?>/gi, '\n')
      .replace(/<\s*\/p\s*>/gi, '\n\n')
      .replace(/<\s*p\b[^>]*>/gi, '')
      .replace(/<\s*\/div\s*>/gi, '\n')
      .replace(/<\s*div\b[^>]*>/gi, '')
      .replace(/<\s*li\b[^>]*>/gi, '- ')
      .replace(/<\s*\/li\s*>/gi, '\n')
      .replace(/<\s*\/h[1-6]\s*>/gi, '\n\n')
      .replace(/<\s*h[1-6]\b[^>]*>/gi, '')
      .replace(/<\s*\/tr\s*>/gi, '\n')
      .replace(/<\s*t[dh]\b[^>]*>/gi, '')
      .replace(/<\s*\/t[dh]\s*>/gi, '\t')
      .replace(/<[^>]+>/g, '');
    return stripAnsi(normalized).replace(/[ \t]+\n/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
  };
  const tryParseJson = (value: unknown): any | null => {
    if (typeof value !== 'string') return null;
    const trimmed = value.trim();
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) return null;
    try {
      return JSON.parse(trimmed);
    } catch {
      return null;
    }
  };
  const tryParseLooseJson = (value: unknown): any | null => {
    if (typeof value !== 'string') return null;
    const trimmed = value.trim();
    const direct = tryParseJson(trimmed);
    if (direct) return direct;
    const extractJsonSnippet = (input: string): string | null => {
      const start = input.search(/[\[{]/);
      if (start < 0) return null;

      const open = input[start];
      const close = open === '{' ? '}' : ']';
      let depth = 0;
      let inString = false;
      let escaped = false;

      for (let i = start; i < input.length; i++) {
        const char = input[i];
        if (inString) {
          if (escaped) {
            escaped = false;
            continue;
          }
          if (char === '\\') {
            escaped = true;
            continue;
          }
          if (char === '"') inString = false;
          continue;
        }

        if (char === '"') {
          inString = true;
          continue;
        }
        if (char === open) depth++;
        if (char === close) {
          depth--;
          if (depth === 0) return input.slice(start, i + 1);
        }
      }

      return null;
    };

    const snippet = extractJsonSnippet(trimmed);
    const wrapped = snippet || (trimmed.startsWith('{') || trimmed.startsWith('[') ? trimmed : `{${trimmed}}`);
    try {
      return JSON.parse(wrapped);
    } catch {
      return null;
    }
  };
  const extractJsonSnippetBounds = (input: string): { start: number; end: number; snippet: string } | null => {
    const start = input.search(/[\[{]/);
    if (start < 0) return null;

    const open = input[start];
    const close = open === '{' ? '}' : ']';
    let depth = 0;
    let inString = false;
    let escaped = false;

    for (let i = start; i < input.length; i++) {
      const char = input[i];
      if (inString) {
        if (escaped) {
          escaped = false;
          continue;
        }
        if (char === '\\') {
          escaped = true;
          continue;
        }
        if (char === '"') inString = false;
        continue;
      }

      if (char === '"') {
        inString = true;
        continue;
      }
      if (char === open) depth++;
      if (char === close) {
        depth--;
        if (depth === 0) return { start, end: i + 1, snippet: input.slice(start, i + 1) };
      }
    }

    return null;
  };
  const formatScalar = (value: unknown): string => {
    if (value === null || value === undefined) return 'n/a';
    if (typeof value === 'string') return value;
    if (typeof value === 'number' || typeof value === 'boolean') return String(value);
    if (Array.isArray(value)) return `[${value.length} item(s)]`;
    return '[object]';
  };
  const normalizeCandidateText = (value: unknown): string => {
    if (value === null || value === undefined) return '';
    if (typeof value === 'string') {
      const decodedHtml = /<\w+[^>]*>/.test(value) ? htmlToText(value) : decodeHtmlEntities(value);
      const parsed = tryParseLooseJson(decodedHtml);
      if (parsed) return normalizeCandidateText(parsed);
      return stripAnsi(decodedHtml).trim();
    }
    if (typeof value === 'number' || typeof value === 'boolean') return String(value);
    if (Array.isArray(value)) {
      return value.map((item) => normalizeCandidateText(item)).filter(Boolean).join('\n');
    }
    if (typeof value === 'object') {
      const obj = value as Record<string, unknown>;
      const preferred = [
        obj.patchText, obj.patch, obj.diff, obj.output, obj.result, obj.content,
        obj.text, obj.message, obj.preview, obj.stdout, obj.stderr,
        obj.title, obj.summary, obj.description, obj.status, obj.command,
        obj.filePath, obj.path,
      ];
      return preferred
        .map((item) => normalizeCandidateText(item))
        .filter(Boolean)
        .join('\n');
    }
    return String(value);
  };
  const formatReadableValue = (value: unknown, indent: string = ''): string => {
    if (value === null || value === undefined) return `${indent}n/a`;
    if (typeof value === 'string') {
      const parsed = tryParseLooseJson(value);
      if (parsed) return formatReadableValue(parsed, indent);
      return value.split(/\r?\n/).map((line) => `${indent}${line}`).join('\n');
    }
    if (typeof value === 'number' || typeof value === 'boolean') return `${indent}${value}`;
    if (Array.isArray(value)) {
      if (value.length === 0) return `${indent}[]`;
      return value.map((item, index) => {
        if (item && typeof item === 'object' && !Array.isArray(item)) {
          const objectText = formatReadableValue(item, `${indent}  `);
          return `${indent}- item ${index + 1}\n${objectText}`;
        }
        return `${indent}- ${formatScalar(item)}`;
      }).join('\n');
    }
    if (typeof value === 'object') {
      const entries = Object.entries(value as Record<string, unknown>);
      if (entries.length === 0) return `${indent}{}`;
      return entries.map(([key, entryValue]) => {
        if (entryValue && typeof entryValue === 'object' && !Array.isArray(entryValue)) {
          return `${indent}${key}:\n${formatReadableValue(entryValue, `${indent}  `)}`;
        }
        if (Array.isArray(entryValue)) {
          return `${indent}${key}:\n${formatReadableValue(entryValue, `${indent}  `)}`;
        }
        return `${indent}${key}: ${formatScalar(entryValue)}`;
      }).join('\n');
    }
    return `${indent}${String(value)}`;
  };
  const pickString = (...values: unknown[]): string | undefined => {
    for (const value of values) {
      if (typeof value === 'string' && value.trim()) return value.trim();
    }
    return undefined;
  };
  const collectImportantText = (value: unknown, depth: number = 0, seen: Set<string> = new Set()): string[] => {
    if (depth > 3 || value === null || value === undefined) return [];

    if (typeof value === 'string') {
      const parsed = tryParseLooseJson(value);
      if (parsed) return collectImportantText(parsed, depth + 1, seen);
      const cleaned = normalizeCandidateText(value);
      if (!cleaned) return [];
      const normalized = cleaned.replace(/\n+/g, '\n').replace(/\s+/g, ' ');
      if (seen.has(normalized)) return [];
      seen.add(normalized);
      return [cleaned];
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
      return [String(value)];
    }

    if (Array.isArray(value)) {
      return value.flatMap((item) => collectImportantText(item, depth + 1, seen));
    }

    if (typeof value === 'object') {
      const obj = value as Record<string, unknown>;
      const priorityKeys = [
        'preview', 'message', 'messageText', 'text', 'content', 'output', 'diff', 'patch', 'stdout', 'stderr',
        'title', 'summary', 'description', 'status', 'command', 'filePath', 'path', 'result', 'error', 'details',
      ];

      const out: string[] = [];
      for (const key of priorityKeys) {
        if (obj[key] !== undefined) {
          out.push(...collectImportantText(obj[key], depth + 1, seen));
        }
      }
      return out;
    }

    return [];
  };
  const summarizeToolLikeObject = (value: any): string => {
    if (!value || typeof value !== 'object') return '';

    const lines: string[] = [];
    const metadata = value.metadata && typeof value.metadata === 'object'
      ? value.metadata
      : value.state?.metadata && typeof value.state.metadata === 'object'
        ? value.state.metadata
        : {};
    const fileEntries = [value.files, metadata.files].find((item) => Array.isArray(item) && item.length > 0) as any[] | undefined;
    const firstFile = fileEntries?.find((item) => item && typeof item === 'object');
    const tool = pickString(value.tool, value.name, value.title, value.type, value.state?.tool);
    const status = pickString(value.state?.status, value.status, value.result?.status);
    const filePath = pickString(value.filePath, value.path, value.relativePath, firstFile?.relativePath, firstFile?.filePath, firstFile?.path, value.state?.input?.filePath, value.state?.input?.path);
    const command = pickString(value.command, value.state?.input?.command, value.input?.command);
    const input = value.state?.input || value.input;
    const title = pickString(value.title, value.state?.title, value.summary, value.description, value.state?.input?.description, metadata.title, metadata.description);
    const additions = value.additions ?? value.state?.additions ?? firstFile?.additions;
    const deletions = value.deletions ?? value.state?.deletions ?? firstFile?.deletions;

    if (tool) lines.push(`Tool: ${tool}`);
    if (status) lines.push(`Status: ${status}`);
    if (title) lines.push(`Title: ${title}`);
    if (filePath) lines.push(`File: ${filePath}`);
    if (command) lines.push(`Command: ${command}`);
    if (Number.isFinite(Number(additions)) || Number.isFinite(Number(deletions))) {
      lines.push(`Changes: +${Number(additions) || 0} / -${Number(deletions) || 0}`);
    }

    if (Array.isArray(fileEntries) && fileEntries.length > 0) {
      const fileBlocks = fileEntries.slice(0, 8).map((entry) => {
        if (!entry || typeof entry !== 'object') return '';
        const entryPath = pickString(entry.relativePath, entry.filePath, entry.path);
        const entryAdditions = entry.additions;
        const entryDeletions = entry.deletions;
        const entryDiff = normalizeCandidateText(entry.patch ?? entry.diff ?? entry.patchText);
        const block: string[] = [];
        if (entryPath) block.push(`File: ${entryPath}`);
        if (Number.isFinite(Number(entryAdditions)) || Number.isFinite(Number(entryDeletions))) {
          block.push(`Changes: +${Number(entryAdditions) || 0} / -${Number(entryDeletions) || 0}`);
        }
        if (entryDiff) {
          block.push('Diff:');
          block.push('```diff');
          block.push(collapseDuplicateBlocks(entryDiff));
          block.push('```');
        }
        return block.join('\n');
      }).filter(Boolean);

      if (fileBlocks.length > 0) {
        lines.push(fileBlocks.join('\n\n'));
      }
    }

    if (input && typeof input === 'object') {
      const inputBits: string[] = [];
      for (const key of ['offset', 'limit', 'page', 'query', 'provider', 'model', 'sessionId']) {
        const v = (input as any)[key];
        if (v !== undefined && v !== null && String(v).trim() !== '') {
          inputBits.push(`${key}=${String(v)}`);
        }
      }
      if (inputBits.length > 0) lines.push(`Input: ${inputBits.join(' | ')}`);
    }

    const rawOutput =
      value.state?.input?.patchText ??
      value.patchText ??
      value.patch ??
      value.diff ??
      metadata.diff ??
      metadata.patch ??
      firstFile?.patch ??
      firstFile?.diff ??
      value.state?.output ??
      value.output ??
      value.result ??
      metadata.preview ??
      metadata.output ??
      value.preview;
    const importantText = collectImportantText(rawOutput).join('\n').trim();
    if (importantText) {
      const diffLike = /^diff\s|^[+-]{1}\s|^@@\s|^Index:/mi.test(importantText) || /(^|\n)[+-].+/m.test(importantText);
      if (diffLike) {
        lines.push('Diff:');
        lines.push('```diff');
        lines.push(collapseDuplicateBlocks(importantText));
        lines.push('```');
      } else {
        const cleaned = collapseDuplicateBlocks(importantText);
        const outputLines = cleaned.split(/\r?\n/).slice(0, 25);
        lines.push('Output:');
        lines.push(outputLines.join('\n'));
        if (cleaned.split(/\r?\n/).length > 25) lines.push('...');
      }
    }

    return lines.join('\n').trim();
  };
  const summarizeMixedPayloadString = (value: string): string => {
    const cleaned = /<\w+[^>]*>/.test(value) ? htmlToText(value) : decodeHtmlEntities(value);
    let remaining = cleaned;
    const sections: string[] = [];
    const seen = new Set<string>();

    const pushSection = (section: string): void => {
      const normalized = section.replace(/\s+/g, ' ').trim();
      if (!normalized || seen.has(normalized)) return;
      seen.add(normalized);
      sections.push(section.trim());
    };

    while (remaining.trim()) {
      const snippet = extractJsonSnippetBounds(remaining);
      if (!snippet) {
        const plain = collapseDuplicateBlocks(htmlToText(remaining));
        if (plain) pushSection(plain);
        break;
      }

      const before = remaining.slice(0, snippet.start).trim();
      if (before) {
        const plain = collapseDuplicateBlocks(htmlToText(before));
        if (plain) pushSection(plain);
      }

      const parsed = tryParseLooseJson(snippet.snippet);
      if (parsed && typeof parsed === 'object') {
        const summary = summarizeToolLikeObject(parsed);
        if (summary) pushSection(summary);
      }

      remaining = remaining.slice(snippet.end);
    }

    return sections.join('\n\n').trim();
  };
  const collapseDuplicateBlocks = (text: string): string => {
    const blocks = (text || '').replace(/\r\n/g, '\n').split(/\n{2,}/);
    const seen = new Set<string>();
    const output: string[] = [];

    for (const block of blocks) {
      const cleaned = stripAnsi(block).trim();
      if (!cleaned) continue;
      const normalized = cleaned.replace(/\s+/g, ' ');
      if (seen.has(normalized)) continue;
      seen.add(normalized);
      output.push(cleaned);
    }

    return output.join('\n\n').trim();
  };

  const summarizePart = (part: any, index: number): string => {
    if (!part || typeof part !== 'object') return '';
    const type = String(part.type || 'part');
    const title = [part.title, part.summary, part.description, part.command, part.name, part.path, part.filePath, part.status]
      .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    const text = typeof part.text === 'string'
      ? part.text
      : typeof part.content === 'string'
        ? part.content
        : typeof part.output === 'string'
          ? part.output
          : '';

    const lines: string[] = [`${index + 1}. [${type}]${title ? ` ${title.trim()}` : ''}`];

    const objectSummary = summarizeToolLikeObject(part);
    if (objectSummary) {
      lines.push(objectSummary);
      return lines.join('\n');
    }

    const beforeValue = pickString(part.before, part.previous, part.old, part.oldContent, part.original, part.from, part.beforeText, part.beforeCode, part.source);
    const afterValue = pickString(part.after, part.next, part.new, part.newContent, part.updated, part.to, part.afterText, part.afterCode, part.target);
    if (beforeValue !== undefined || afterValue !== undefined) {
      const beforeText = collapseDuplicateBlocks(beforeValue || '').trim();
      const afterText = collapseDuplicateBlocks(afterValue || '').trim();
      if (beforeText || afterText) {
        const filePath = pickString(part.filePath, part.path);
        const languageSource = filePath || title || '';
        const match = languageSource.toLowerCase().match(/\.([a-z0-9]+)$/);
        const map: Record<string, string> = {
          kt: 'kotlin', kts: 'kotlin', java: 'java', ts: 'ts', tsx: 'tsx', js: 'js', jsx: 'jsx',
          json: 'json', xml: 'xml', gradle: 'gradle', md: 'md', yaml: 'yaml', yml: 'yaml', py: 'python',
        };
        const language = match ? (map[match[1]] || 'text') : 'text';
        if (filePath) lines.push(`file: ${filePath}`);
        if (beforeText) {
          lines.push('Before:');
          lines.push('```' + language);
          lines.push(beforeText);
          lines.push('```');
        }
        if (afterText) {
          lines.push('After:');
          lines.push('```' + language);
          lines.push(afterText);
          lines.push('```');
        }
        return lines.join('\n');
      }
    }

    const filePath = [part.filePath, part.path].find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    const fileFenceLanguage = (() => {
      const source = filePath || title || '';
      const match = source.toLowerCase().match(/\.([a-z0-9]+)$/);
      if (!match) return 'text';
      const ext = match[1];
      const map: Record<string, string> = {
        kt: 'kotlin',
        kts: 'kotlin',
        java: 'java',
        ts: 'ts',
        tsx: 'tsx',
        js: 'js',
        jsx: 'jsx',
        json: 'json',
        xml: 'xml',
        gradle: 'gradle',
        md: 'md',
        yaml: 'yaml',
        yml: 'yaml',
        py: 'python',
      };
      return map[ext] || 'text';
    })();

    if (beforeValue !== undefined || afterValue !== undefined) {
      const beforeText = collapseDuplicateBlocks(beforeValue || '').trim();
      const afterText = collapseDuplicateBlocks(afterValue || '').trim();
      if (beforeText || afterText) {
        if (filePath) lines.push(`file: ${filePath}`);
        if (beforeText) {
          lines.push('Before:');
          lines.push('```' + fileFenceLanguage);
          lines.push(beforeText);
          lines.push('```');
        }
        if (afterText) {
          lines.push('After:');
          lines.push('```' + fileFenceLanguage);
          lines.push(afterText);
          lines.push('```');
        }
        return lines.join('\n');
      }
    }

    const diffText = [part.diff, part.patch, part.change, part.changes, part.code, part.content]
      .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    if (diffText && (/^diff\s|^[+-]{1}\s|^@@\s|^Index:/mi.test(diffText) || type.includes('diff') || type.includes('patch') || type.includes('edit'))) {
      lines.push('```diff');
      lines.push(collapseDuplicateBlocks(diffText.trim()));
      lines.push('```');
      return lines.join('\n');
    }

    if (text.trim()) {
      const cleaned = summarizeMixedPayloadString(text.trim()) || collapseDuplicateBlocks(text.trim());
      if (cleaned) lines.push(cleaned);
    }

    const fileHints = [filePath].filter((v) => typeof v === 'string' && v.trim()) as string[];
    if (fileHints.length > 0) lines.push(`file: ${fileHints[0]}`);

    return lines.join('\n');
  };

  const summarizeRaw = (raw: any): string => {
    if (!raw || typeof raw !== 'object') return typeof raw === 'string' ? raw : '';

    const lines: string[] = [];
    const patchText = normalizeCandidateText(raw.patchText || raw.patch || raw.diff || raw.state?.patchText || raw.state?.patch || raw.state?.diff);
    if (patchText) {
      const preview = collapseDuplicateBlocks(patchText);
      lines.push(`Patch:\n${preview}`);
    }

    const outputText = normalizeCandidateText(raw.output || raw.state?.output || raw.result || raw.state?.result || raw.preview);
    if (outputText) {
      const preview = collapseDuplicateBlocks(outputText);
      lines.push(`Output:\n${preview}`);
    }

    const meta = raw.metadata || raw.info || raw.state || {};
    const provider = meta.provider || meta.providerID || raw.providerID;
    const model = meta.model || meta.modelID || raw.modelID;
    const finish = meta.finish || raw.finish;
    const timeCreated = meta.time?.created || raw.time?.created || raw.state?.time?.start;
    const timeCompleted = meta.time?.completed || raw.time?.completed || raw.state?.time?.end;
    const tokenInfo = meta.tokens || raw.tokens;

    const metaLines: string[] = [];
    if (provider || model) metaLines.push(`Provider/Model: ${provider || 'unknown'}/${model || 'unknown'}`);
    if (finish) metaLines.push(`Finish: ${finish}`);
    if (timeCreated || timeCompleted) metaLines.push(`Time: ${formatViDateTime(timeCreated)} -> ${formatViDateTime(timeCompleted)}`);
    if (tokenInfo) metaLines.push(`Tokens: total=${tokenInfo.total ?? 'n/a'}, in=${tokenInfo.input ?? 'n/a'}, out=${tokenInfo.output ?? 'n/a'}`);
    if (metaLines.length > 0) {
      lines.push(metaLines.join('\n'));
    }

    return lines.filter(Boolean).join('\n\n').trim();
  };

  if (process.env.AI_VERBOSE_PAYLOAD === '1') {
    const sections: string[] = [];

    const info = response?.info || {};
    const tokens = info?.tokens || response?.tokens;
    const partCount = Array.isArray(response?.parts) ? response.parts.length : 0;
    const eventCount = Array.isArray(response?.events) ? response.events.length : 0;
    const finish = info?.finish || response?.raw?.finish || 'unknown';
    const model = info?.modelID || response?.raw?.modelID || 'unknown';
    const provider = info?.providerID || response?.raw?.providerID || 'unknown';
    const createdAt = info?.time?.created || response?.raw?.time?.created;
    const completedAt = info?.time?.completed || response?.raw?.time?.completed;

    sections.push(`Provider/Model: ${provider}/${model}`);
    sections.push(`Finish: ${finish}`);
    sections.push(`Time: ${formatViDateTime(createdAt)} -> ${formatViDateTime(completedAt)}`);
    if (tokens) {
      sections.push(`Tokens: total=${tokens.total ?? 'n/a'}, in=${tokens.input ?? 'n/a'}, out=${tokens.output ?? 'n/a'}`);
    }
    sections.push(`Parts: ${partCount}`);
    sections.push(`Events: ${eventCount}`);

    const renderedText = collapseDuplicateBlocks(normalizeCandidateText(response?.displayText || response?.text || '').trim());
    if (renderedText) sections.push(`Output:\n${renderedText}`);

    if (partCount > 0) {
      const partSummary = response.parts.slice(0, 40).map((part: any, index: number) => summarizePart(part, index)).filter(Boolean).join('\n\n');
      if (partSummary) sections.push(`Parts:\n${partSummary}`);
    }

    const rawSummary = response?.raw !== undefined ? summarizeRaw(response.raw) : '';
    if (rawSummary) sections.push(rawSummary);

    return sections.join('\n\n');
  }

  const sections: string[] = [];
  const info = response?.info || {};
  const tokens = info?.tokens || response?.tokens;
  const partCount = Array.isArray(response?.parts) ? response.parts.length : 0;
  const eventCount = Array.isArray(response?.events) ? response.events.length : 0;
  const finish = info?.finish || response?.raw?.finish || 'unknown';
  const model = info?.modelID || response?.raw?.modelID || 'unknown';
  const provider = info?.providerID || response?.raw?.providerID || 'unknown';
  const createdAt = info?.time?.created || response?.raw?.time?.created;
  const completedAt = info?.time?.completed || response?.raw?.time?.completed;

  sections.push(`Provider/Model: ${provider}/${model}`);
  sections.push(`Finish: ${finish}`);
  sections.push(`Time: ${formatViDateTime(createdAt)} -> ${formatViDateTime(completedAt)}`);
  if (tokens) sections.push(`Tokens: total=${tokens.total ?? 'n/a'}, in=${tokens.input ?? 'n/a'}, out=${tokens.output ?? 'n/a'}`);
  sections.push(`Parts: ${partCount}`);

  const textPart = Array.isArray(response?.parts)
    ? response.parts.find((p: any) => p?.type === 'text' && typeof p?.text === 'string' && p.text.trim())
    : null;
  if (textPart?.text) {
    const preview = collapseDuplicateBlocks(normalizeCandidateText(textPart.text)).replace(/\s+/g, ' ').trim();
    sections.push(`Output:\n${preview.slice(0, 220)}${preview.length > 220 ? '...' : ''}`);
  }

  if (partCount > 0) {
    const compactPartSummary = response.parts
      .slice(0, 8)
      .map((part: any, index: number) => summarizePart(part, index))
      .filter(Boolean)
      .join('\n\n');
    if (compactPartSummary) {
      sections.push(`Parts:\n${compactPartSummary}`);
    }
  }

  return sections.join('\n\n');
}

async function sendAIArtifacts(chatId: string, response: any, title: string): Promise<void> {
  const dump = buildAIArtifactsDump(response);
  if (!dump.trim()) return;

  const chunks = splitMessage(`📦 ${title}\n\n${dump}`, 3500);
  for (const chunk of chunks) {
    await telegramBot.sendMessage(chatId, chunk);
  }
}

async function sendTelegramModelsPage(chatId: number, provider?: string, page: number = 0, messageId?: number): Promise<void> {
  if (aiService.getCLI() !== 'opencode') {
    const currentCLI = aiService.getCLI();
    const currentModel = aiService.getDefaultModel();
    const text = currentCLI === 'codex'
      ? `*${aiService.getCliDisplayName(currentCLI)} Models*\n\nCurrent CLI: \`${currentCLI}\`\nCurrent model: \`${modelDisplayName(currentModel)}\`\n\nLoaded from OpenAI Models API when available.`
      : `*${aiService.getCliDisplayName(currentCLI)} Models*\n\nCurrent CLI: \`${currentCLI}\`\nCurrent model: \`${modelDisplayName(currentModel)}\`\n\nThis picker is only available for OpenCode. Use the compatible quick picks below.`;
    const payload = {
      parse_mode: 'Markdown' as const,
      reply_markup: {
        inline_keyboard: await buildTelegramQuickModelKeyboardPage(currentCLI, currentModel, 0),
      },
    };

    if (messageId) {
      await safeTelegramEditMessageText(text, {
        chat_id: chatId,
        message_id: messageId,
        ...payload,
      });
      return;
    }

    await telegramBot.sendMessage(chatId, text, payload);
    return;
  }

  const models = await aiService.listAvailableModelsDetailed(provider);
  const current = aiService.getDefaultModel();
  const defaultPriority = ['opencode/big-pickle', 'opencode/minimax-m2.5-free', 'opencode/nemotron-3-super-free'];
  models.sort((a: any, b: any) => {
    const aIdx = defaultPriority.findIndex(p => a.key.includes(p) || a.key.endsWith(p.replace('opencode/', '')));
    const bIdx = defaultPriority.findIndex(p => b.key.includes(p) || b.key.endsWith(p.replace('opencode/', '')));
    if (aIdx >= 0 && bIdx >= 0) return aIdx - bIdx;
    if (aIdx >= 0) return -1;
    if (bIdx >= 0) return 1;
    return 0;
  });

  if (models.length === 0) {
    await telegramBot.sendMessage(chatId, '📭 No models returned by OpenCode.');
    return;
  }

  const safePage = Math.max(0, page);
  const pageModels = getTelegramModelsPage(models, safePage);
  const totalPages = Math.max(1, Math.ceil(models.length / 8));

  let text = `🤖 *Available Models*\n\nCurrent: \`${modelDisplayName(current)}\`\nFound: ${models.length}\nPage: ${safePage + 1}/${totalPages}\n\n`;
  text += pageModels.map((model: any, index: number) => {
    const badges = [model.key === current ? 'current' : '', model.paidPriority > 1 ? 'paid' : 'free', model.status || 'unknown', model.variants.length ? `variants: ${model.variants.join(', ')}` : '']
      .filter(Boolean)
      .join(' | ');
    return `${safePage * 8 + index + 1}. ${model.key === current ? '✅ ' : ''}\`${modelDisplayName(model.key)}\`${badges ? ` (${badges})` : ''}`;
  }).join('\n');
  text += '\n\nTap a button to switch.';

  const keyboard: Array<Array<{ text: string; callback_data: string }>> = [];
  for (const model of pageModels) {
    const display = modelDisplayName(model.key);
    keyboard.push([{ text: display.length > 34 ? display.slice(0, 31) + '...' : `${display}${model.variants.length ? ' +' : ''}`, callback_data: `modelpick_${model.key}` }]);
  }

  keyboard.push([
    { text: 'Prev', callback_data: `modelpage_${provider || 'all'}__${safePage - 1}` },
    { text: 'Next', callback_data: `modelpage_${provider || 'all'}__${safePage + 1}` },
  ]);

  const payload = {
    parse_mode: 'Markdown' as const,
    reply_markup: { inline_keyboard: keyboard },
  };

  if (messageId) {
    await safeTelegramEditMessageText(text, {
      chat_id: chatId,
      message_id: messageId,
      ...payload,
    });
    return;
  }

  await telegramBot.sendMessage(chatId, text, payload);
}

async function buildTelegramQuickModelKeyboard(currentCLI: string, currentModel: string) {
  const examples = currentCLI === 'opencode'
    ? aiService.getModelExamples(currentCLI as any)
    : await aiService.listModelsForCLI(currentCLI as any).catch(() => aiService.getSupportedModels(currentCLI as any));
  const rows: Array<Array<{ text: string; callback_data: string }>> = [];
  for (let i = 0; i < Math.min(examples.length, 24); i += 3) {
    rows.push(
      examples.slice(i, i + 3).map((example: string) => ({
        text: example === currentModel ? `${modelDisplayName(example)} ✅` : modelDisplayName(example),
        callback_data: `quickmodel_${example}`,
      }))
    );
  }
  return rows;
}

async function buildTelegramQuickModelKeyboardPage(currentCLI: string, currentModel: string, page: number = 0) {
  const examples = currentCLI === 'opencode'
    ? aiService.getModelExamples(currentCLI as any)
    : await aiService.listModelsForCLI(currentCLI as any).catch(() => aiService.getSupportedModels(currentCLI as any));
  const safePage = Math.max(0, page);
  const pageItems = getTelegramModelsPage(examples, safePage, 12);
  const rows: Array<Array<{ text: string; callback_data: string }>> = [];
  for (let i = 0; i < pageItems.length; i += 3) {
    rows.push(
      pageItems.slice(i, i + 3).map((example: string) => ({
        text: example === currentModel ? `${modelDisplayName(example)} ✅` : modelDisplayName(example),
        callback_data: `quickmodel_${example}`,
      }))
    );
  }

  if (examples.length > 12) {
    rows.push([
      { text: 'Prev', callback_data: `quickmodelpage_${currentCLI}__${safePage - 1}` },
      { text: 'Next', callback_data: `quickmodelpage_${currentCLI}__${safePage + 1}` },
    ]);
  }

  return rows;
}

function isLikelyLimitIssue(message: string): boolean {
  const text = (message || '').toLowerCase();
  return text.includes('rate limit')
    || text.includes('rate-limited')
    || text.includes('too many requests')
    || text.includes('quota')
    || text.includes('credit')
    || text.includes('billing')
    || text.includes('insufficient')
    || text.includes('high traffic')
    || text.includes('overloaded')
    || text.includes('capacity');
}

function getOpencodeQuotaSnapshot(): string | null {
  try {
    const launcher = getOpenCodeLauncher();
    const output = execFileSync(launcher.command, [...launcher.args, 'stats'], {
      encoding: 'utf-8',
      windowsHide: true,
      timeout: 12000,
    });
    const stats = parseTokenStatsOutput(output);
    const lines = [
      `Used: ${stats.used || 'N/A'}`,
      `Remaining: ${stats.remaining || 'N/A'}`,
      `Limit: ${stats.limit || 'N/A'}`,
      `Input: ${stats.inputTokens || 'N/A'}`,
      `Output: ${stats.outputTokens || 'N/A'}`,
    ];
    return lines.join('\n');
  } catch {
    return null;
  }
}

function buildLimitNotice(cli: string, errorMessage: string): string | null {
  if (!isLikelyLimitIssue(errorMessage)) {
    return null;
  }

  if (cli === 'opencode') {
    const quota = getOpencodeQuotaSnapshot();
    if (quota) {
      return `\n\n⚠️ *Possible limit detected*\n\n\`\`\`\n${quota}\n\`\`\`\n\nTry changing model or wait for quota reset.`;
    }
  }

  return '\n\n⚠️ *Possible provider limit detected*\nTry again later or switch model.';
}

async function buildProjectModelKeyboardPage(cli: string, projectIndex: number, currentModel: string, page: number = 0) {
  const models = cli === 'opencode'
    ? (await aiService.listAvailableModelsDetailed().catch(() => [])).map((item: any) => item.key)
    : await aiService.listModelsForCLI(cli as any).catch(() => aiService.getSupportedModels(cli as any));

  if (cli === 'opencode') {
    const freePriority = [
      'opencode/big-pickle',
      'opencode/minimax-m2.5-free',
      'opencode/nemotron-3-super-free',
    ];
    models.sort((a: string, b: string) => {
      const aIdx = freePriority.indexOf(a);
      const bIdx = freePriority.indexOf(b);
      if (aIdx >= 0 && bIdx >= 0) return aIdx - bIdx;
      if (aIdx >= 0) return -1;
      if (bIdx >= 0) return 1;
      return a.localeCompare(b);
    });
  }

  if (models.length === 0) {
    const fallback = aiService.getDefaultModel(cli as any);
    const payloadId = storeTelegramCallbackPayload(JSON.stringify({ projectIndex, cli, model: fallback }));
    return {
      rows: [[{ text: formatModelButtonLabel(fallback, fallback === currentModel), callback_data: `projmodelset_${payloadId}` }]],
      totalPages: 1,
      safePage: 0,
    };
  }

  const safePage = Math.max(0, page);
  const pageItems = getTelegramModelsPage(models, safePage, 12);
  const rows: Array<Array<{ text: string; callback_data: string }>> = [];
  for (let i = 0; i < pageItems.length; i += 3) {
    rows.push(
      pageItems.slice(i, i + 3).map((modelId: string) => {
        const payloadId = storeTelegramCallbackPayload(JSON.stringify({ projectIndex, cli, model: modelId }));
        return {
          text: formatModelButtonLabel(modelId, modelId === currentModel),
          callback_data: `projmodelset_${payloadId}`,
        };
      })
    );
  }

  const totalPages = Math.max(1, Math.ceil(models.length / 12));
  if (totalPages > 1) {
    rows.push([
      { text: 'Prev', callback_data: `projmodelpage_${cli}_${projectIndex}_${safePage - 1}` },
      { text: 'Next', callback_data: `projmodelpage_${cli}_${projectIndex}_${safePage + 1}` },
    ]);
  }

  return { rows, totalPages, safePage };
}

async function requestTelegramWriteApproval(chatId: string, userId: string, targetPath: string): Promise<void> {
  const request = permissionBrokerService.createRequest(targetPath, userId, chatId);
  await telegramBot.sendMessage(chatId, `Folder này chưa có quyền ghi cho bot:\n\`${request.rootPath}\`\n\nBấm *Approve write* để broker cấp quyền và cho phép 3 CLI sửa file trong root này.`, {
    parse_mode: 'Markdown',
    reply_markup: {
      inline_keyboard: [
        [
          { text: 'Approve write', callback_data: `writeapprove_${request.id}` },
          { text: 'Deny', callback_data: `writedeny_${request.id}` },
        ],
      ],
    },
  });
}

function isTelegramMessageNotModifiedError(error: unknown): boolean {
  return normalizeErrorMessage(error).toLowerCase().includes('message is not modified');
}

async function safeTelegramEditMessageText(
  text: string,
  options: TelegramBot.EditMessageTextOptions
): Promise<void> {
  try {
    await telegramBot.editMessageText(text, options);
  } catch (error) {
    if (isTelegramMessageNotModifiedError(error)) {
      return;
    }
    throw error;
  }
}

async function safeTelegramEditMessageReplyMarkup(
  replyMarkup: TelegramBot.InlineKeyboardMarkup,
  options: TelegramBot.EditMessageReplyMarkupOptions
): Promise<void> {
  try {
    await telegramBot.editMessageReplyMarkup(replyMarkup, options);
  } catch (error) {
    if (isTelegramMessageNotModifiedError(error)) {
      return;
    }
    throw error;
  }
}

function parseTokenStatsOutput(output: string) {
  const used = extractTokenValue(output, [/Used\s+([^\n]+)/i, /Usage\s+([^\n]+)/i]);
  const remaining = extractTokenValue(output, [/Remaining\s+([^\n]+)/i, /Left\s+([^\n]+)/i, /Balance\s+([^\n]+)/i]);
  const limit = extractTokenValue(output, [/Limit\s+([^\n]+)/i, /Quota\s+([^\n]+)/i]);
  const totalCost = extractTokenValue(output, [/Total Cost\s+([^\n]+)/i, /Cost\s+([^\n]+)/i]);
  const inputTokens = extractTokenValue(output, [/Input Tokens\s+([^\n]+)/i, /Input\s+([^\n]+)/i]);
  const outputTokens = extractTokenValue(output, [/Output Tokens\s+([^\n]+)/i, /Output\s+([^\n]+)/i]);
  return {
    used,
    remaining,
    limit,
    totalCost,
    inputTokens,
    outputTokens,
    usedPercent: extractTokenPercent(used),
    remainingPercent: extractTokenPercent(remaining),
    modelBreakdown: extractTokenModelBreakdown(output),
  };
}

function buildTokenTrendText(): string {
  const history = memoryService.getTokenHistory(7);
  if (history.length === 0) return 'No history yet';
  return history
    .slice(0, 7)
    .reverse()
    .map(item => `${new Date(item.timestamp).toLocaleDateString()}: ${item.remaining || 'N/A'}`)
    .join('\n');
}

function startTokenWatchScheduler(): void {
  setInterval(async () => {
    try {
      const watch = memoryService.getTokenWatch();
      if (!watch.enabled) return;

      const launcher = getOpenCodeLauncher();
      const output = execFileSync(launcher.command, [...launcher.args, 'stats'], {
        encoding: 'utf-8',
        windowsHide: true,
      });

      const stats = parseTokenStatsOutput(output);
      memoryService.addTokenSnapshot({ timestamp: new Date().toISOString(), ...stats });

      if (stats.remainingPercent === null || stats.remainingPercent > watch.thresholdPercent) {
        return;
      }

      const lastAlert = watch.lastAlertAt ? new Date(watch.lastAlertAt).getTime() : 0;
      if (Date.now() - lastAlert < 60 * 60 * 1000) {
        return;
      }

      const alertText = `⚠️ Token quota low\nRemaining: ${stats.remaining || 'Unknown'}\nThreshold: ${watch.thresholdPercent}%\nModel: ${aiService.getDefaultModel()}`;

      for (const chatId of watch.telegramChats) {
        telegramBot.sendMessage(chatId, alertText).catch(() => {});
      }

      for (const channelId of watch.discordChannels) {
        const channel = await discordClient.channels.fetch(channelId).catch(() => null);
        if (channel && 'send' in channel) {
          (channel as any).send(alertText).catch(() => {});
        }
      }

      memoryService.markTokenAlertSent();
    } catch (error) {
      loggerService.warn('Token watch check failed', { error: String(error) });
    }
  }, 30 * 60 * 1000);
}

discordClient.commands = new Collection();
discordClient.runningProcesses = new Map();

const chatSessions: Map<string, ChatSession> = new Map();
const projectContexts: Map<string, { content: string; timestamp: number; used: boolean }> = new Map();
const aiThinkingRequests: Map<string, { requestId: string; mode: 'chat' | 'ai'; startedAt: number }> = new Map();
const telegramChatQueueByChat: Map<string, QueuedTelegramChatRequest[]> = new Map();
const telegramCallbackPayloads: Map<string, string> = new Map();
const selectedAISessionsByChat: Map<string, { cli: 'opencode' | 'claude' | 'codex'; workdir: string; sessionId: string }> = new Map();
const MAX_HISTORY = 10;
const STALE_AI_CHECK_AFTER_MS = 60000;

function storeTelegramCallbackPayload(payload: string): string {
  const id = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;
  telegramCallbackPayloads.set(id, payload);
  if (telegramCallbackPayloads.size > 3000) {
    const oldestKey = telegramCallbackPayloads.keys().next().value;
    if (oldestKey) {
      telegramCallbackPayloads.delete(oldestKey);
    }
  }
  return id;
}

function takeTelegramCallbackPayload(id: string): string | null {
  const payload = telegramCallbackPayloads.get(id);
  if (!payload) return null;
  telegramCallbackPayloads.delete(id);
  return payload;
}

function formatModelButtonLabel(modelId: string, selected: boolean): string {
  const display = modelDisplayName(modelId);
  const maxBaseLen = 52;
  const base = display.length > maxBaseLen ? `${display.slice(0, maxBaseLen - 3)}...` : display;
  const withMarker = selected ? `${base} ✅` : base;
  return withMarker.length > 64 ? `${withMarker.slice(0, 61)}...` : withMarker;
}

function resolveSelectedSessionId(chatId: string, cli: 'opencode' | 'claude' | 'codex', workdir: string): string | undefined {
  const selected = selectedAISessionsByChat.get(chatId);
  if (!selected) return undefined;
  if (selected.cli !== cli) return undefined;
  if (selected.workdir !== workdir) return undefined;
  return selected.sessionId || undefined;
}

function beginAIThinking(chatId: string, mode: 'chat' | 'ai'): string | null {
  if (aiThinkingRequests.has(chatId)) return null;
  const requestId = `tg:${chatId}:${Date.now()}`;
  aiThinkingRequests.set(chatId, { requestId, mode, startedAt: Date.now() });
  const scheduleCheck = () => {
    setTimeout(() => {
      const current = aiThinkingRequests.get(chatId);
      if (!current || current.requestId !== requestId) return;
      if (aiService.isRequestActive(requestId)) {
        scheduleCheck();
        return;
      }
      aiThinkingRequests.delete(chatId);
      aiService.resetCLIState(aiService.getCLI(), 'stale thinking detected by periodic checker');
      loggerService.warn('Recovered stale AI thinking state from periodic checker', {
        chatId,
        mode,
        requestId,
      });
      kickTelegramChatQueue(chatId, '♻️ Recovered stuck AI state, resuming queued /chat.');
    }, STALE_AI_CHECK_AFTER_MS);
  };
  scheduleCheck();
  return requestId;
}

function endAIThinking(chatId: string, requestId: string): void {
  const current = aiThinkingRequests.get(chatId);
  if (current && current.requestId === requestId) {
    aiThinkingRequests.delete(chatId);
  }
}

function getAIThinking(chatId: string): { requestId: string; mode: 'chat' | 'ai'; startedAt: number } | undefined {
  return aiThinkingRequests.get(chatId);
}


function cancelAIThinking(chatId: string): boolean {
  const thinking = aiThinkingRequests.get(chatId);
  if (!thinking) return false;
  aiThinkingRequests.delete(chatId);
  aiService.resetCLIState(aiService.getCLI(), 'request cancelled by user');
  return aiService.cancelRequest(thinking.requestId);
}

function enqueueTelegramChatRequest(chatId: string, request: QueuedTelegramChatRequest): number {
  const queue = telegramChatQueueByChat.get(chatId) || [];
  queue.push(request);
  telegramChatQueueByChat.set(chatId, queue);
  return queue.length;
}

function dequeueTelegramChatRequest(chatId: string): QueuedTelegramChatRequest | undefined {
  const queue = telegramChatQueueByChat.get(chatId);
  if (!queue || queue.length === 0) return undefined;
  const next = queue.shift();
  if (!queue.length) {
    telegramChatQueueByChat.delete(chatId);
  } else {
    telegramChatQueueByChat.set(chatId, queue);
  }
  return next;
}

function getTelegramChatQueueSize(chatId: string): number {
  return telegramChatQueueByChat.get(chatId)?.length || 0;
}

function clearTelegramChatQueue(chatId: string): number {
  const queue = telegramChatQueueByChat.get(chatId);
  if (!queue || queue.length === 0) return 0;
  const size = queue.length;
  telegramChatQueueByChat.delete(chatId);
  return size;
}

function recoverFinishedButStuckAIThinking(chatId: string): boolean {
  const thinking = aiThinkingRequests.get(chatId);
  if (!thinking) return false;

  const ageMs = Date.now() - thinking.startedAt;
  if (ageMs < STALE_AI_CHECK_AFTER_MS) return false;
  if (aiService.isRequestActive(thinking.requestId)) return false;

  aiThinkingRequests.delete(chatId);
  aiService.resetCLIState(aiService.getCLI(), 'stale thinking detected (no active request)');
  loggerService.warn('Recovered stale AI thinking state that had no active request', {
    chatId,
    mode: thinking.mode,
    requestId: thinking.requestId,
    ageMs,
  });
  return true;
}

function kickTelegramChatQueue(chatId: string, announce?: string): void {
  const nextQueued = dequeueTelegramChatRequest(chatId);
  if (!nextQueued) return;
  const remaining = getTelegramChatQueueSize(chatId);
  const note = announce || `▶️ Processing queued /chat from queue. Remaining after this: ${remaining}.`;
  telegramBot.sendMessage(chatId, note).catch(() => {});
  executeTelegramChatRequest(chatId, nextQueued).catch((err) => {
    loggerService.error('Failed to process queued Telegram /chat request', {
      chatId,
      error: normalizeErrorMessage(err),
    });
  });
}

function applyLastUsedModelForChat(cwd: string, cli: SupportedCLI, model: string): void {
  aiService.setDefaultModel(model, cli);
  memoryService.setDefaultModel(model, cli);
  memoryService.setProjectAISettingsByCwd(cwd, { cli, model });
}

function clearAIConversationForChat(chatId: string): { removedInMemory: number; removedPersisted: number } {
  const keys = Array.from(chatSessions.keys()).filter((key) => key.startsWith(`${chatId}_`));
  let removedPersisted = 0;

  for (const key of keys) {
    chatSessions.delete(key);
    if (memoryService.clearChatHistory(key)) {
      removedPersisted += 1;
    }
  }

  projectContexts.delete(chatId);
  cancelAIThinking(chatId);
  saveRuntimeState();

  return {
    removedInMemory: keys.length,
    removedPersisted,
  };
}

function clearAllAIConversations(): { removedInMemory: number; removedPersisted: number } {
  const keys = Array.from(chatSessions.keys());
  let removedPersisted = 0;

  for (const key of keys) {
    chatSessions.delete(key);
    if (memoryService.clearChatHistory(key)) {
      removedPersisted += 1;
    }
  }

  const contextCount = projectContexts.size;
  projectContexts.clear();

  const thinkingChatIds = Array.from(aiThinkingRequests.keys());
  for (const chatId of thinkingChatIds) {
    cancelAIThinking(chatId);
  }

  saveRuntimeState();

  return {
    removedInMemory: keys.length + contextCount,
    removedPersisted,
  };
}

function startAIWaitingHeartbeat(
  chatId: string,
  messageId: number,
  title: string,
): { markChunkReceived: () => void; stop: () => void } {
  const startedAt = Date.now();
  let sawChunk = false;
  const timer = setInterval(() => {
    if (sawChunk) return;
    const elapsedSeconds = Math.max(1, Math.floor((Date.now() - startedAt) / 1000));
    safeTelegramEditMessageText(`${title}\n\nStill waiting for the model response...\nElapsed: ${elapsedSeconds}s\nStatus: request is still open`, {
      chat_id: chatId,
      message_id: messageId,
      parse_mode: 'Markdown',
    }).catch(() => {});
  }, 30000);

  return {
    markChunkReceived: () => {
      sawChunk = true;
      clearInterval(timer);
    },
    stop: () => {
      clearInterval(timer);
    },
  };
}

function saveRuntimeState(): void {
  try {
    runtimeStateService.setCombinedState({
      currentCLI: aiService.getCLI(),
      chatSessions: Array.from(chatSessions.entries()).map(([key, session]) => ({
        key,
        workdir: session.workdir,
        messages: [],
      })),
      projectContexts: Array.from(projectContexts.entries()).map(([chatId, context]) => ({
        chatId,
        content: context.content,
        timestamp: context.timestamp,
        used: context.used,
      })),
      selectedAISessionsByChat: Array.from(selectedAISessionsByChat.entries()).map(([chatId, selected]) => ({
        chatId,
        cli: selected.cli,
        workdir: selected.workdir,
        sessionId: selected.sessionId,
      })),
    });
  } catch (error) {
    loggerService.warn('Failed to save combined runtime state', { error: String(error) });
  }
}

function loadRuntimeState(): void {
  try {
    const parsed = runtimeStateService.getCombinedState() as CombinedRuntimeState;

    if (parsed.currentCLI && ['opencode', 'claude', 'codex'].includes(parsed.currentCLI)) {
      aiService.setCLI(parsed.currentCLI);
    }

    for (const session of parsed.chatSessions || []) {
      if (!session || typeof session.key !== 'string' || typeof session.workdir !== 'string') {
        continue;
      }
      chatSessions.set(session.key, {
        workdir: session.workdir,
        messages: [],
      });
    }

    for (const context of parsed.projectContexts || []) {
      if (!context || typeof context.chatId !== 'string' || typeof context.content !== 'string' || typeof context.timestamp !== 'number') {
        continue;
      }
      projectContexts.set(context.chatId, {
        content: context.content,
        timestamp: context.timestamp,
        used: context.used === true,
      });
    }

    for (const selected of parsed.selectedAISessionsByChat || []) {
      if (
        !selected ||
        typeof selected.chatId !== 'string' ||
        typeof selected.workdir !== 'string' ||
        typeof selected.sessionId !== 'string' ||
        !['opencode', 'claude', 'codex'].includes(selected.cli)
      ) {
        continue;
      }

      selectedAISessionsByChat.set(selected.chatId, {
        cli: selected.cli,
        workdir: selected.workdir,
        sessionId: selected.sessionId,
      });
    }

    loggerService.info('Restored combined runtime state', {
      chatSessions: chatSessions.size,
      projectContexts: projectContexts.size,
      selectedAISessionsByChat: selectedAISessionsByChat.size,
      cli: aiService.getCLI(),
    });
  } catch (error) {
    loggerService.warn('Failed to restore combined runtime state', { error: String(error) });
  }
}

loggerService.initialize();
loggerService.info(`Starting Discord Terminal Bot v${BOT_VERSION}...`, { startTime: new Date().toISOString() });
aiService.setDefaultModels(memoryService.getDefaultModels());
loadRuntimeState();

const commandsPath = path.join(__dirname, 'commands');
const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js') && !file.endsWith('.d.js'));

let loadedCount = 0;
for (const file of commandFiles) {
  try {
    const command = require(path.join(commandsPath, file));
    if (command.data && command.execute) {
      discordClient.commands.set(command.data.name, command);
      loadedCount++;
      loggerService.info(`Loaded command: ${command.data.name}`);
    } else {
      loggerService.warn(`Command file ${file} missing data or execute function`);
    }
  } catch (error) {
    loggerService.error(`Failed to load command ${file}`, { 
      error: error instanceof Error ? error.message : String(error) 
    });
  }
}

if (loadedCount === 0) {
  loggerService.warn('No commands loaded');
} else {
  loggerService.info(`Loaded ${loadedCount} command(s)`);
}

const rest = new REST({ version: '10' }).setToken(token);

discordClient.on(Events.ClientReady, async (c) => {
  loggerService.info(`Discord bot logged in as ${c.user?.tag}`);
  loggerService.info(`Bot is ready in ${c.guilds.cache.size} guild(s)`);

  startTokenWatchScheduler();

  try {
    const commands = discordClient.commands.map(cmd => cmd.data.toJSON());
    if (commands.length === 0) {
      loggerService.warn('No commands registered');
    }
    
    if (guildId && clientId) {
      await rest.put(Routes.applicationGuildCommands(clientId, guildId), { body: commands });
      loggerService.info(`Registered ${commands.length} guild commands`);
    } else if (clientId) {
      await rest.put(Routes.applicationCommands(clientId), { body: commands });
      loggerService.info(`Registered ${commands.length} global commands`);
    } else {
      loggerService.warn('No clientId configured, skipping command registration');
    }
  } catch (error) {
    loggerService.error('Failed to register commands', { 
      error: error instanceof Error ? error.message : String(error) 
    });
  }
});

discordClient.on(Events.InteractionCreate, async (interaction: any) => {
  if (!interaction.isChatInputCommand() && !interaction.isButton() && !interaction.isStringSelectMenu()) return;

  if (interaction.isChatInputCommand()) {
    const command = discordClient.commands.get(interaction.commandName);
    if (!command) {
      loggerService.warn('Command not found', { commandName: interaction.commandName });
      return;
    }

    try {
      if (command.ownerOnly && !securityService.isOwner(interaction.user.id)) {
        if (!interaction.replied) {
          await interaction.reply({
            content: '❌ This command is restricted to bot owners only.',
            flags: 64,
          });
        }
        return;
      }

      await command.execute(interaction);
    } catch (error) {
      loggerService.error(`Error executing command ${interaction.commandName}`, { 
        error: error instanceof Error ? error.message : String(error), 
        user: interaction.user.id,
        stack: error instanceof Error ? error.stack : undefined
      });

      const errorMessage = normalizeErrorMessage(error);

      try {
        if (interaction.deferred && !interaction.replied) {
          await interaction.editReply({
            content: `❌ Error: ${errorMessage}`,
          });
        } else if (!interaction.replied) {
          await interaction.reply({
            content: `❌ Error: ${errorMessage}`,
            flags: 64,
          });
        }
      } catch (replyError) {
        loggerService.error('Failed to send error reply', { error: String(replyError) });
      }
    }
  }

  if (interaction.isButton()) {
    try {
      const customId = interaction.customId;
      if (!customId) {
        loggerService.warn('Button interaction missing customId');
        return;
      }

      if (customId.startsWith('modelbtn_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized to use this action.', flags: 64 });
          return;
        }

        if (aiService.getCLI() !== 'opencode') {
          await interaction.reply({ content: ` This model browser belongs to OpenCode. Current CLI: \`${aiService.getCLI()}\`. Use \`/model\` or \`/models\` again for the active CLI.`, flags: 64 });
          return;
        }

        const selectedModel = customId.slice('modelbtn_'.length);
        const detailedModels = await aiService.listAvailableModelsDetailed();
        const model = detailedModels.find((item: any) => item.key === selectedModel);
        if (!model) {
          await interaction.reply({ content: `❌ Model not supported: \`${selectedModel}\``, flags: 64 });
          return;
        }

        if (model.variants.length > 0) {
          const { ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
          const rows = [];
          for (let i = 0; i < model.variants.length; i += 5) {
            const row = new ActionRowBuilder();
            for (const variant of model.variants.slice(i, i + 5)) {
              row.addComponents(
                new ButtonBuilder()
                  .setCustomId(`modelvariant_${selectedModel}__${variant}`)
                  .setLabel(variant)
                  .setStyle(ButtonStyle.Primary)
              );
            }
            rows.push(row);
          }

          await interaction.reply({
            content: `Choose variant for \`${selectedModel}\``,
            components: rows,
            flags: 64,
          });
          return;
        }

        aiService.setDefaultModel(selectedModel, aiService.getCLI());
        memoryService.setDefaultModel(selectedModel, aiService.getCLI());
        const currentCwd = terminalService.getSession(interaction.channelId)?.cwd;
        if (currentCwd) {
          memoryService.setProjectAISettingsByCwd(currentCwd, {
            cli: aiService.getCLI(),
            model: selectedModel,
          });
        }
        await interaction.reply({ content: `✅ Default model set to \`${selectedModel}\``, flags: 64 });
        return;
      }

      if (customId.startsWith('modelvariant_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized to use this action.', flags: 64 });
          return;
        }

        if (aiService.getCLI() !== 'opencode') {
          await interaction.reply({ content: ` Variants are only available through OpenCode. Current CLI: \`${aiService.getCLI()}\`.`, flags: 64 });
          return;
        }

        const payload = customId.slice('modelvariant_'.length);
        const [selectedModel, variant] = payload.split('__');
        const selection = `${selectedModel}#${variant}`;
        const validation = await aiService.validateModelSelection(selection);
        if (!validation.ok) {
          await interaction.reply({ content: `❌ ${validation.error}`, flags: 64 });
          return;
        }

        aiService.setDefaultModel(selection, aiService.getCLI());
        memoryService.setDefaultModel(selection, aiService.getCLI());
        const variantCwd = terminalService.getSession(interaction.channelId)?.cwd;
        if (variantCwd) {
          memoryService.setProjectAISettingsByCwd(variantCwd, {
            cli: aiService.getCLI(),
            model: selection,
          });
        }
        await interaction.reply({ content: `✅ Default model set to \`${selection}\``, flags: 64 });
        return;
      }

      if (customId.startsWith('quickmodel_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: ' You are not authorized to use this action.', flags: 64 });
          return;
        }

        const selectedModel = customId.slice('quickmodel_'.length);
        const validation = await aiService.validateModelSelectionForCurrentCLI(selectedModel);
        if (!validation.ok) {
          await interaction.reply({ content: ` ${validation.error}`, flags: 64 });
          return;
        }

        const normalizedModel = validation.normalized || selectedModel;
        aiService.setDefaultModel(normalizedModel, aiService.getCLI());
        memoryService.setDefaultModel(normalizedModel, aiService.getCLI());
        if (aiService.getCLI() === 'opencode') {
          aiService.resetSession();
        }
        const quickModelCwd = terminalService.getSession(interaction.channelId)?.cwd;
        if (quickModelCwd) {
          memoryService.setProjectAISettingsByCwd(quickModelCwd, {
            cli: aiService.getCLI(),
            model: normalizedModel,
          });
        }
        await interaction.reply({ content: ` Default model set to \`${normalizedModel}\``, flags: 64 });
        return;
      }

      if (customId.startsWith('quickmodelpage_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized to use this action.', flags: 64 });
          return;
        }

        const payload = customId.slice('quickmodelpage_'.length);
        const [cliName, pageRaw] = payload.split('_');
        const page = Math.max(0, Number(pageRaw) || 0);
        const models = await aiService.listModelsForCLI(cliName as any).catch(() => aiService.getSupportedModels(cliName as any));
        const pageModels = models.slice(page * 10, page * 10 + 10);
        const { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder, Colors } = require('discord.js');
        const rows: any[] = [];

        for (let i = 0; i < pageModels.length; i += 5) {
          const row = new ActionRowBuilder();
          for (const model of pageModels.slice(i, i + 5)) {
            row.addComponents(
              new ButtonBuilder()
                .setCustomId(`quickmodel_${model}`)
                .setLabel(model.slice(0, 80))
                .setStyle(model === aiService.getDefaultModel() ? ButtonStyle.Success : ButtonStyle.Secondary)
            );
          }
          rows.push(row);
        }

        if (models.length > 10) {
          rows.push(
            new ActionRowBuilder().addComponents(
              new ButtonBuilder()
                .setCustomId(`quickmodelpage_${cliName}_${page - 1}`)
                .setLabel('Prev')
                .setStyle(ButtonStyle.Primary)
                .setDisabled(page <= 0),
              new ButtonBuilder()
                .setCustomId(`quickmodelpage_${cliName}_${page + 1}`)
                .setLabel('Next')
                .setStyle(ButtonStyle.Primary)
                .setDisabled((page + 1) * 10 >= models.length)
            )
          );
        }

        const embed = new EmbedBuilder()
          .setTitle(`${aiService.getCliDisplayName(cliName as any)} Models`)
          .setColor(Colors.Blue)
          .setDescription(`Current CLI: \`${cliName}\`\nCurrent default: \`${aiService.getDefaultModel()}\`\nPage: ${page + 1}/${Math.max(1, Math.ceil(models.length / 10))}`)
          .addFields({
            name: 'Models',
            value: pageModels.map((model: string) => `\`${model}\``).join('\n').slice(0, 1024) || 'No models',
          })
          .setTimestamp();

        await interaction.update({ embeds: [embed], components: rows });
        return;
      }

      if (customId.startsWith('modelpage_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized to use this action.', flags: 64 });
          return;
        }

        if (aiService.getCLI() !== 'opencode') {
          await interaction.reply({ content: ` This model browser belongs to OpenCode. Current CLI: \`${aiService.getCLI()}\`. Use \`/models\` again for the active CLI.`, flags: 64 });
          return;
        }

        const [, providerRaw, pageRaw] = customId.split('_');
        const provider = providerRaw === 'all' ? undefined : providerRaw;
        const page = Math.max(0, Number(pageRaw) || 0);
        const models = await aiService.listAvailableModelsDetailed(provider);
        const current = aiService.getDefaultModel();
        const start = page * 10;
        const pageModels = models.slice(start, start + 10);

        const rows: any[] = [];
        for (let i = 0; i < pageModels.length; i += 5) {
          const { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder, Colors } = require('discord.js');
          const row = new ActionRowBuilder();
          for (const model of pageModels.slice(i, i + 5)) {
            row.addComponents(
              new ButtonBuilder()
                .setCustomId(`modelbtn_${model.key}`)
                .setLabel((model.key === current ? `Current: ${model.key}` : `${model.key}${model.variants.length ? ' +' : ''}`).slice(0, 80))
                .setStyle(model.key === current ? ButtonStyle.Success : ButtonStyle.Secondary)
            );
          }
          rows.push(row);
        }

        const { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder, Colors } = require('discord.js');
        rows.push(
          new ActionRowBuilder().addComponents(
            new ButtonBuilder()
              .setCustomId(`modelpage_${provider || 'all'}_${page - 1}`)
              .setLabel('Prev')
              .setStyle(ButtonStyle.Primary)
              .setDisabled(page <= 0),
            new ButtonBuilder()
              .setCustomId(`modelpage_${provider || 'all'}_${page + 1}`)
              .setLabel('Next')
              .setStyle(ButtonStyle.Primary)
              .setDisabled((page + 1) * 10 >= models.length)
          )
        );

        const embed = new EmbedBuilder()
          .setTitle('Available Models')
          .setColor(Colors.Blue)
          .setDescription(`Current default: \`${current}\`\nFound: ${models.length} model(s)`)
          .addFields({
            name: `Models Page ${page + 1}`,
            value: pageModels.map((model: any) => {
              const badges = [model.key === current ? 'current' : '', model.paidPriority > 1 ? 'paid' : 'free', model.variants.length ? `variants: ${model.variants.join(', ')}` : '']
                .filter(Boolean)
                .join(' | ');
              return `• ${model.key}${badges ? ` (${badges})` : ''}`;
            }).join('\n').slice(0, 1024) || 'No models',
          })
          .setTimestamp();

        await interaction.update({ embeds: [embed], components: rows });
        return;
      }

      if (customId.startsWith('clibtn_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized.', flags: 64 });
          return;
        }
        const selectedCLI = customId.slice('clibtn_'.length);
        const validCLIs = ['opencode', 'claude', 'codex'];
        if (!validCLIs.includes(selectedCLI)) {
          await interaction.reply({ content: `❌ Unknown CLI: \`${selectedCLI}\``, flags: 64 });
          return;
        }
        aiService.setCLI(selectedCLI as any);
        saveRuntimeState();
        const cliCwd = terminalService.getSession(interaction.channelId)?.cwd;
        if (cliCwd) {
          memoryService.setProjectAISettingsByCwd(cliCwd, {
            cli: selectedCLI as any,
            model: aiService.getDefaultModel(),
          });
        }
        await interaction.reply({ content: `✅ AI CLI switched to \`${selectedCLI}\``, flags: 64 });
        return;
      }

      if (customId.startsWith('discordwriteapprove_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: ' You are not authorized.', flags: 64 });
          return;
        }

        const requestId = customId.slice('discordwriteapprove_'.length);
        try {
          const request = permissionBrokerService.approveRequest(requestId);
          const project = memoryService.getProjectByPath(request.rootPath);
          if (project && request.chatId === interaction.channelId) {
            terminalService.getOrCreateSession(interaction.channelId);
            terminalService.changeDirectory(interaction.channelId, project.path);
            if (project.preferredCLI) {
              aiService.setCLI(project.preferredCLI);
            }
            if (project.preferredModel) {
              aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
            }
            memoryService.setProjectAISettings(project.path, {
              cli: aiService.getCLI(),
              model: aiService.getDefaultModel(),
            });
            const agentsContext = memoryService.generateAgentsMdForAI(project.path);
            projectContexts.set(interaction.channelId, { content: agentsContext, timestamp: Date.now(), used: false });
            saveRuntimeState();
            memoryService.updateProjectContextLoaded(project.path);
          }
          await interaction.update({
            content: project && request.chatId === interaction.channelId
              ? `✅ Write access approved and project switched to \`${project.name}\`.\nPath: \`${project.path}\`\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``
              : `✅ Write access approved for \`${request.rootPath}\`.`,
            components: [],
          });
        } catch (error) {
          await interaction.update({
            content: ` Failed to grant write access: ${normalizeErrorMessage(error)}`,
            components: [],
          });
        }
        return;
      }

      if (customId.startsWith('discordwritedeny_')) {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: ' You are not authorized.', flags: 64 });
          return;
        }

        const requestId = customId.slice('discordwritedeny_'.length);
        const request = permissionBrokerService.denyRequest(requestId);
        await interaction.update({
          content: request
            ? `âš ï¸ Write access denied for \`${request.rootPath}\`.`
            : 'âš ï¸ Write request not found.',
          components: [],
        });
        return;
      }

      if (customId.startsWith('sessionclearcurrent_')) {
        const targetChannelId = customId.slice('sessionclearcurrent_'.length);
        const existing = terminalService.getSession(targetChannelId);
        if (!existing) {
          await interaction.reply({ content: ' Session not found.', flags: 64 });
          return;
        }
        terminalService.destroySession(targetChannelId);
        await interaction.reply({ content: ` Cleared session \`${targetChannelId}\`.`, flags: 64 });
        return;
      }

      if (customId === 'sessionclearall_global') {
        const cleared = terminalService.clearAllSessions();
        await interaction.reply({ content: ` Cleared ${cleared} terminal session${cleared === 1 ? '' : 's'}.`, flags: 64 });
        return;
      }

      if (customId.startsWith('projectpick_')) {
        const targetPath = customId.slice('projectpick_'.length);
        const project = memoryService.getProjectByPath(targetPath);
        if (!project) {
          await interaction.reply({ content: 'Project not found.', flags: 64 });
          return;
        }

        if (permissionBrokerService.requiresApproval(project.path)) {
          const request = permissionBrokerService.createRequest(project.path, interaction.user.id, interaction.channelId);
          const { ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
          await interaction.update({
            content: `Project selected: \`${project.name}\`.\nWrite access is required for \`${request.rootPath}\` before switching. Approve access and the bot will switch to this project automatically.`,
            components: [
              new ActionRowBuilder().addComponents(
                new ButtonBuilder()
                  .setCustomId(`discordwriteapprove_${request.id}`)
                  .setLabel('Approve write')
                  .setStyle(ButtonStyle.Success),
                new ButtonBuilder()
                  .setCustomId(`discordwritedeny_${request.id}`)
                  .setLabel('Deny')
                  .setStyle(ButtonStyle.Secondary)
              ),
            ],
            embeds: [],
          });
          return;
        }

        terminalService.getOrCreateSession(interaction.channelId);
        terminalService.changeDirectory(interaction.channelId, project.path);
        if (project.preferredCLI) {
          aiService.setCLI(project.preferredCLI);
          saveRuntimeState();
        }
        if (project.preferredModel) {
          aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
        }
        memoryService.setProjectAISettings(project.path, {
          cli: aiService.getCLI(),
          model: aiService.getDefaultModel(),
        });

        const agentsContext = memoryService.generateAgentsMdForAI(project.path);
        projectContexts.set(interaction.channelId, { content: agentsContext, timestamp: Date.now(), used: false });
        saveRuntimeState();
        memoryService.updateProjectContextLoaded(project.path);

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle(`Project: ${project.name}`)
              .setColor(require('discord.js').Colors.Green)
              .setDescription(`Path: \`${project.path}\`\nCWD updated for this channel.\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (customId.startsWith('stop_')) {
        const channelId = customId.slice('stop_'.length);
        const processInfo = terminalService.getActiveProcess(channelId);
        const processStopped = !!processInfo;
        const aiStopped = cancelAIThinking(channelId);
        if (processInfo) {
          terminalService.killProcess(channelId);
          discordClient.runningProcesses.delete(channelId);
        }

        if (processStopped || aiStopped) {
          await interaction.reply({
            content: processStopped && aiStopped
              ? '🛑 Process and AI request stopped.'
              : processStopped
                ? '🛑 Process stopped.'
                : '🛑 AI request cancelled.',
            flags: 64,
          });
        } else {
          await interaction.reply({
            content: 'No running process or AI request to stop.',
            flags: 64,
          });
        }
        return;
      }

      if (customId.startsWith('refresh_')) {
        const channelId = customId.slice('refresh_'.length);
        const session = terminalService.getSession(channelId);
        if (session) {
          const embed = loggerService.createSessionEmbed(session);
          await interaction.reply({
            embeds: [embed],
            flags: 64,
          });
        } else {
          await interaction.reply({
            content: 'No active session.',
            flags: 64,
          });
        }
        return;
      }

      const parts = customId.split('_');
      if (parts.length < 2) {
        loggerService.warn('Invalid button customId format', { customId });
        return;
      }

      const action = parts[0];
      const subAction = parts[1];
      const channelId = parts.slice(2).join('_');

      if (action === 'ai' && subAction === 'stop' && channelId) {
        try {
          const { stopAIActivity } = require('./commands/ai');
          const sessionKey = `${channelId}_${interaction.user.id}`;
          const stopped = stopAIActivity(sessionKey);
          await interaction.reply({
            content: stopped ? '🛑 Terminal session ended.' : 'No active terminal session.',
            flags: 64,
          });
        } catch (aiError) {
          loggerService.error('Failed to stop AI activity', { error: String(aiError) });
          await interaction.reply({
            content: '❌ Failed to stop AI session.',
            flags: 64,
          });
        }
      }

      if (action === 'chat' && subAction === 'stop' && channelId) {
        try {
          const { stopChat } = require('./commands/chat');
          const historyKey = `${channelId}_${interaction.user.id}`;
          stopChat(historyKey);
          await interaction.reply({
            content: '🛑 Chat stopped.',
            flags: 64,
          });
        } catch (chatError) {
          loggerService.error('Failed to stop chat', { error: String(chatError) });
          await interaction.reply({
            content: '❌ Failed to stop chat.',
            flags: 64,
          });
        }
      }

      if (action === 'chat' && subAction === 'clear' && channelId) {
        await interaction.reply({
          content: '🗑️ Use `/chat --clear` to clear chat history.',
          flags: 64,
        });
      }
    } catch (buttonError) {
      loggerService.error('Button interaction error', { 
        error: String(buttonError),
        customId: interaction.customId,
        user: interaction.user.id
      });
      
      try {
        await interaction.reply({
          content: '❌ An error occurred while processing your request.',
          flags: 64,
        });
      } catch {}
    }
  }

  if (interaction.isStringSelectMenu()) {
    try {
      if (interaction.customId === 'projectpick_select') {
        const selectedValue = interaction.values[0];
        const projects = memoryService.getProjects();
        const project = selectedValue.startsWith('project_index_')
          ? projects[Number(selectedValue.slice('project_index_'.length))]
          : memoryService.getProjectByPath(selectedValue);
        if (!project) {
          await interaction.reply({ content: ' Project not found.', flags: 64 });
          return;
        }

        if (permissionBrokerService.requiresApproval(project.path)) {
          const request = permissionBrokerService.createRequest(project.path, interaction.user.id, interaction.channelId);
          const { ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
          await interaction.update({
            content: `Project selected: \`${project.name}\`.\nWrite access is required for \`${request.rootPath}\` before switching. Approve access and the bot will switch to this project automatically.`,
            components: [
              new ActionRowBuilder().addComponents(
                new ButtonBuilder()
                  .setCustomId(`discordwriteapprove_${request.id}`)
                  .setLabel('Approve write')
                  .setStyle(ButtonStyle.Success),
                new ButtonBuilder()
                  .setCustomId(`discordwritedeny_${request.id}`)
                  .setLabel('Deny')
                  .setStyle(ButtonStyle.Secondary)
              ),
            ],
            embeds: [],
          });
          return;
        }

        terminalService.getOrCreateSession(interaction.channelId);
        terminalService.changeDirectory(interaction.channelId, project.path);

        if (project.preferredCLI) {
          aiService.setCLI(project.preferredCLI);
          saveRuntimeState();
        }
        if (project.preferredModel) {
          aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
        }

        memoryService.setProjectAISettings(project.path, {
          cli: aiService.getCLI(),
          model: aiService.getDefaultModel(),
        });

        const agentsContext = memoryService.generateAgentsMdForAI(project.path);
        projectContexts.set(interaction.channelId, { content: agentsContext, timestamp: Date.now(), used: false });
        saveRuntimeState();
        memoryService.updateProjectContextLoaded(project.path);

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle(`Project: ${project.name}`)
              .setColor(require('discord.js').Colors.Green)
              .setDescription(`Path: \`${project.path}\`\nCWD updated for this channel.\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (interaction.customId === 'sessionpick_select') {
        const channelId = interaction.values[0];
        const session = terminalService.getSession(channelId);
        if (!session) {
          await interaction.reply({ content: ' Session not found.', flags: 64 });
          return;
        }

        terminalService.changeDirectory(interaction.channelId, session.cwd);
        terminalService.setSelectedDevice(interaction.channelId, session.selectedDeviceId);

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle('Session Switched')
              .setColor(require('discord.js').Colors.Green)
              .setDescription(`Using session from channel: \`${session.channelId}\`\nCWD switched to: \`${session.cwd}\`\nDevice: ${session.selectedDeviceId ? `\`${session.selectedDeviceId}\`` : 'auto-detect'}`)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (interaction.customId === 'devicepick_select') {
        const selectedValue = interaction.values[0];
        const deviceId = selectedValue === 'device_auto'
          ? undefined
          : selectedValue.startsWith('device_')
            ? selectedValue.slice('device_'.length)
            : undefined;

        terminalService.setSelectedDevice(interaction.channelId, deviceId);

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle('Android Device Selected')
              .setColor(require('discord.js').Colors.Green)
              .setDescription(deviceId ? `Selected device: \`${deviceId}\`` : 'Selected device: auto-detect')
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }
    } catch (selectError) {
      loggerService.error('String select interaction error', {
        error: String(selectError),
        customId: interaction.customId,
        user: interaction.user.id,
      });

      try {
        await interaction.reply({
          content: ' An error occurred while processing your selection.',
          flags: 64,
        });
      } catch {}
    }
  }
});

discordClient.on('disconnect', () => {
  loggerService.warn('Discord disconnected');
});

discordClient.on('reconnecting', () => {
  loggerService.info('Discord reconnecting...');
});

discordClient.on('resumed', () => {
  loggerService.info('Discord reconnected');
});

const isAuthorized = (userId: number): boolean => {
  return securityService.isOwner(userId.toString());
};

const sendLargeMessage = async (chatId: number, text: string, maxLength: number = 4000) => {
  const messages = splitMessage(text, maxLength - 100);
  for (const msg of messages) {
    await telegramBot.sendMessage(chatId, msg, { parse_mode: 'Markdown' });
  }
};

const syncTelegramCodeBlockParts = async (
  chatId: string,
  messageIds: number[],
  text: string,
  header: string
): Promise<number[]> => {
  const parts = buildTelegramHtmlCodeBlockMessages(text || 'No response', header, 4096);
  const ids = [...messageIds];

  while (ids.length < parts.length) {
    const sent = await telegramBot.sendMessage(chatId, parts[ids.length], { parse_mode: 'HTML' });
    ids.push(sent.message_id);
  }

  for (let i = 0; i < parts.length; i++) {
    await telegramBot.editMessageText(parts[i], {
      chat_id: chatId,
      message_id: ids[i],
      parse_mode: 'HTML',
    }).catch((error) => {
      if (!normalizeErrorMessage(error).toLowerCase().includes('message is not modified')) {
        throw error;
      }
    });
  }

  return ids;
};

const streamAIChunk = async (chatId: string, chunk: string, header: string, isFirst: boolean, lastMessageId: number | null): Promise<number | null> => {
  try {
    const safeChunk = chunk || '';
    if (isFirst) {
      const sent = await telegramBot.sendMessage(chatId, `${header}\n${safeChunk}`);
      return sent.message_id;
    } else {
      const sent = await telegramBot.sendMessage(chatId, `📝 Continue:\n${safeChunk}`);
      return sent.message_id;
    }
  } catch (e: any) {
    loggerService.warn('Failed to stream AI chunk to Telegram', { chatId, error: String(e?.message || e) });
    return lastMessageId;
  }
};

interface StreamRenderState {
  rawText: string;
  visibleText: string;
  lastDeltaNormalized: string;
  repeatCount: number;
  seenNormalizedSegments: Set<string>;
}

const collapseDuplicateStreamSegments = (text: string, state: StreamRenderState): string => {
  const blocks = (text || '').replace(/\r\n/g, '\n').split(/\n{2,}/);
  const output: string[] = [];

  for (const block of blocks) {
    const trimmedBlock = block.trim();
    if (!trimmedBlock) continue;

    const normalizedBlock = trimmedBlock.replace(/\s+/g, ' ');
    if (state.seenNormalizedSegments.has(normalizedBlock)) {
      continue;
    }
    state.seenNormalizedSegments.add(normalizedBlock);

    const lines = trimmedBlock.split('\n');
    const dedupedLines: string[] = [];
    const seenLines = new Set<string>();
    for (const line of lines) {
      const normalizedLine = normalizeStreamChunk(line);
      if (!normalizedLine) {
        if (dedupedLines.length > 0 && dedupedLines[dedupedLines.length - 1] !== '') {
          dedupedLines.push('');
        }
        continue;
      }
      if (seenLines.has(normalizedLine)) continue;
      seenLines.add(normalizedLine);
      dedupedLines.push(line.trimEnd());
    }

    const cleaned = dedupedLines.join('\n').trimEnd();
    if (cleaned) output.push(cleaned);
  }

  return output.join('\n\n').replace(/\n{3,}/g, '\n\n').trimEnd();
};

const appendVisibleStreamDelta = (latestText: string, state: StreamRenderState): string | null => {
  const latest = (latestText || '').replace(/\r\n/g, '\n');
  const previous = (state.rawText || '').replace(/\r\n/g, '\n');
  if (!latest) return null;

  const latestNormalized = normalizeStreamChunk(latest);
  const previousNormalized = normalizeStreamChunk(previous);

  if (latestNormalized && latestNormalized === previousNormalized) {
    state.rawText = latest;
    state.repeatCount += 1;
    return `${state.visibleText} (${state.repeatCount})`;
  }

  let delta = '';
  if (!previous) {
    delta = latest;
  } else if (latest === previous) {
    delta = '';
  } else if (latest.startsWith(previous)) {
    delta = latest.slice(previous.length);
  } else {
    const trimmedPrevious = previous.trimEnd();
    if (trimmedPrevious && latest.startsWith(trimmedPrevious)) {
      delta = latest.slice(trimmedPrevious.length);
    } else {
      delta = latest;
    }
  }

  const deltaNormalized = normalizeStreamChunk(delta);
  if (!deltaNormalized) {
    state.rawText = latest;
    return null;
  }

  if (deltaNormalized === state.lastDeltaNormalized) {
    state.rawText = latest;
    state.repeatCount += 1;
    return `${state.visibleText} (${state.repeatCount})`;
  }

  const dedupedDelta = collapseDuplicateStreamSegments(delta, state);
  const dedupedDeltaNormalized = normalizeStreamChunk(dedupedDelta);
  if (!dedupedDeltaNormalized) {
    state.rawText = latest;
    return null;
  }

  if (dedupedDeltaNormalized === state.lastDeltaNormalized) {
    state.rawText = latest;
    state.repeatCount += 1;
    return `${state.visibleText} (${state.repeatCount})`;
  }

  state.rawText = latest;
  state.lastDeltaNormalized = dedupedDeltaNormalized;
  state.repeatCount = 1;
  state.visibleText += dedupedDelta;
  return state.visibleText;
};

const getUnsyncedChunkText = (latestText: string, previousText: string): string => {
  if (!latestText) return '';
  if (!previousText) return latestText;
  if (latestText === previousText) return '';
  if (latestText.startsWith(previousText)) {
    return latestText.slice(previousText.length);
  }

  const trimmedPrevious = previousText.trimEnd();
  if (trimmedPrevious && latestText.startsWith(trimmedPrevious)) {
    return latestText.slice(trimmedPrevious.length);
  }

  return latestText;
};

const normalizeStreamChunk = (value: string): string => {
  return (value || '').replace(/\s+/g, ' ').trim();
};

const isDuplicateStreamChunk = (chunk: string, previousNormalized: string): boolean => {
  if (!chunk) return true;
  const normalized = normalizeStreamChunk(chunk);
  if (!normalized) return true;
  return normalized === previousNormalized;
};

const trackStreamChunkRepeat = (normalizedChunk: string, repeatCounter: Map<string, number>): number => {
  if (!normalizedChunk) return 0;
  const next = (repeatCounter.get(normalizedChunk) || 0) + 1;
  if (repeatCounter.size >= 120 && !repeatCounter.has(normalizedChunk)) {
    const first = repeatCounter.keys().next().value;
    if (first) repeatCounter.delete(first);
  }
  repeatCounter.set(normalizedChunk, next);
  return next;
};

const telegramBot = new TelegramBot(telegramToken, { polling: true });

const telegramMenuKeyboard: TelegramBot.ReplyKeyboardMarkup = {
  keyboard: [
    [{ text: '/menu' }, { text: '.models' }, { text: '/status' }],
    [{ text: '/sessions' }, { text: '/queue' }, { text: '/current' }],
    [{ text: '/chat' }, { text: '/ai' }],
    [{ text: '/runapp' }, { text: '/syncproject' }],
  ],
  resize_keyboard: true,
  is_persistent: true,
};

function telegramMenuMarkup(): TelegramBot.SendMessageOptions {
  return {
    parse_mode: 'Markdown',
    reply_markup: telegramMenuKeyboard,
  };
}

function telegramMenuText(): string {
  return [
    '📋 *Telegram Menu*',
    '',
    'Use the buttons below or type a command:',
    '• `.models` - Browse available models',
    '• `/status` - Check session/process/system',
    '• `/sessions` - List sessions',
    '• `/queue` - Show AI queue',
    '• `/current` - Show current context',
    '• `/chat` - Chat with AI',
    '• `/ai` - Ask AI once',
    '• `/runapp` - Build and launch Android app',
    '• `/syncproject` - Auto-sync project setup',
    '• `/pushgit` - Auto commit + push',
    '• `/ping` - Check latency',
  ].join('\n');
}


loggerService.info(`Starting Telegram Terminal Bot v${BOT_VERSION}...`);

telegramBot.onText(/\/model(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const selectedModel = match?.[1]?.trim();
  const currentCLI = aiService.getCLI();
  const examples = currentCLI === 'opencode'
    ? aiService.getModelExamples(currentCLI)
    : await aiService.listModelsForCLI(currentCLI).catch(() => aiService.getModelExamples(currentCLI));

  if (!selectedModel) {
    await telegramBot.sendMessage(
      msg.chat.id,
      'AI CLI: `' + currentCLI + '`\nCurrent model: `' + modelDisplayName(aiService.getDefaultModel()) + '`\n\n' + (currentCLI === 'opencode' ? 'Examples' : currentCLI === 'codex' ? 'Available models' : 'Suggested models') + ':\n' + examples.slice(0, 20).map(example => '/model ' + example).join('\n') + (currentCLI === 'opencode' ? '' : currentCLI === 'codex' ? '\n\nLoaded from OpenAI Models API when available. You can still enter any compatible model id manually.' : '\n\nThe active CLI does not expose a full model list here. You can still enter any compatible model id manually.'),
      {
        parse_mode: 'Markdown',
        reply_markup: {
          inline_keyboard: await buildTelegramQuickModelKeyboardPage(currentCLI, aiService.getDefaultModel(), 0),
        },
      }
    );
    return;
  }

  const validation = await aiService.validateModelSelectionForCurrentCLI(selectedModel);
  if (!validation.ok) {
    await telegramBot.sendMessage(msg.chat.id, `❌ ${validation.error}`);
    return;
  }

  const normalizedModel = validation.normalized || selectedModel;
  aiService.setDefaultModel(normalizedModel, currentCLI);
  memoryService.setDefaultModel(normalizedModel, currentCLI);
  if (currentCLI === 'opencode') {
    aiService.resetSession();
  }
  const currentCwd = terminalService.getSession(msg.chat.id.toString())?.cwd;
  if (currentCwd) {
    memoryService.setProjectAISettingsByCwd(currentCwd, {
      cli: currentCLI,
      model: normalizedModel,
    });
  }
  await telegramBot.sendMessage(msg.chat.id, `✅ Default model set to: \`${normalizedModel}\``, {
    parse_mode: 'Markdown',
  });
});

telegramBot.onText(/\/menu/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  await telegramBot.sendMessage(msg.chat.id, telegramMenuText(), telegramMenuMarkup());
});

telegramBot.onText(/\/help/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const os = getOS();
  const shellInfo = os === 'windows' ? 'PowerShell' : 'Bash';

  await telegramBot.sendMessage(msg.chat.id, `
📟 *Multi-Platform Terminal Bot - Help*

🔧 *Commands:*
• /run <command> [timeout] - Execute a terminal command (timeout in minutes, default from config)
  - Example: /run npm install
  - Example: /run npm install 15
  - Example: /run npm install -t 15
• /status [session|process|system] - Check status
• /stop - Stop the running process
• /cd <directory> - Change working directory
• /history [lines] [--clear] - View/clear command history
• /logs [lines] - View command logs
• /sessions - List all terminal sessions
• /queue - Show current AI request and queued /chat messages
• /sessionclear [all] - Clear current session or all sessions
• /chat <message> [--path dir] [--clear] - Chat with AI
• /ai <prompt> [--model] [--path dir] - Ask AI
• /cli [name] - View or switch AI CLI (opencode/claude/codex)
• /model [name] - View or change default AI model
• /models [provider] - Browse OpenCode models or show suggested models for the active CLI
• /tokens [days] - View token/quota usage for the active CLI
• /usage [days] [cli] - View tracked AI usage from this bot
• /changes [path] - Show git working tree changes
• /review [file] - Review a file or current git changes
• /test [pattern] [--coverage] - Run project tests
• /commit [message] - Create a git commit
• /pushgit [path] - Auto commit with datetime + summary and push
• /health - Check bot and AI backend health
• /project [name] [--path dir] - List/switch projects
• /addFolder - Add current working folder as a project
• /delProject [name|path] - Delete a tracked project (default: current folder)
• /memory [commands|projects|stats] - View memory
• /devices - List adb devices and choose the active Android device for this chat
• /runapp [device] - Build and launch the current Android app
• /ping - Check bot latency
• /both - Show both bots status

🔒 *Security:*
• Only owner(s) can use commands
• Dangerous commands are blocked
• Command cooldown enforced
• All commands are logged

🖥️ *System Info:*
• Shell: ${shellInfo}
• OS: ${os.charAt(0).toUpperCase() + os.slice(1)}
• Process Timeout: 10 minutes
• Max Output: 4000 characters
  `.trim(), telegramMenuMarkup());
});

telegramBot.onText(/\/devices\b/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const devices = androidService.listConnectedDevices();
  const currentDevice = terminalService.getSelectedDevice(chatId);

  if (devices.length === 0) {
    await telegramBot.sendMessage(msg.chat.id, '📭 No adb devices detected.');
    return;
  }

  const description = devices.map((device, index) => {
    const selected = device.id === currentDevice ? ' [selected]' : '';
    return `${index + 1}. \`${device.id}\` | ${device.status}${selected}`;
  }).join('\n');

  const keyboard = [
    [{ text: currentDevice ? 'Auto-detect' : 'Auto-detect ✓', callback_data: 'devicepick_auto' }],
    ...devices.slice(0, 24).map((device, index) => [{
      text: `${device.id === currentDevice ? '✓ ' : ''}${device.id}`.slice(0, 48),
      callback_data: `devicepick_${index}`,
    }]),
  ];

  await telegramBot.sendMessage(msg.chat.id, `
📱 *Android Devices*

${description}

Current device: ${currentDevice ? `\`${currentDevice}\`` : 'auto-detect'}
  `.trim(), {
    parse_mode: 'Markdown',
    reply_markup: { inline_keyboard: keyboard },
  });
});

telegramBot.onText(/\/both/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }
  
  const discordStatus = discordClient.isReady() ? '✅ Online' : '❌ Offline';
  const guildCount = discordClient.guilds.cache.size;
  const os = getOS();
  
  await telegramBot.sendMessage(msg.chat.id, `
🤖 *Both Bots Status*

**Discord Bot:** ${discordStatus}
• Guilds: ${guildCount}
• Commands: ${discordClient.commands.size}

**Telegram Bot:** ✅ Online

**System:**
• OS: ${os}
• Node: ${process.version}
• Uptime: ${Math.floor(process.uptime() / 60)} minutes
  `.trim(), { parse_mode: 'Markdown' });
});

telegramBot.onText(/\/ping/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }
  const start = Date.now();
  await telegramBot.sendMessage(msg.chat.id, '🏓 Pong!');
  const latency = Date.now() - start;
  await telegramBot.sendMessage(msg.chat.id, `📊 Latency: ${latency}ms`);
});

telegramBot.onText(/\/sessions/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const activeCLI = aiService.getCLI();
  const workdir = terminalService.getSession(chatId)?.cwd || process.cwd();

  if (activeCLI === 'opencode') {
    await aiService.ensureReadyForCLI(activeCLI, workdir).catch(() => false);
  }

  const sessions = await aiService.listSessionsForCLI(activeCLI, workdir).catch(() => []);
  const sessionButtons: Array<{ text: string; callback_data: string }> = [];
  const newPayloadId = storeTelegramCallbackPayload(JSON.stringify({
    cli: activeCLI,
    workdir,
    sessionId: '',
  }));
  sessionButtons.push({ text: '🆕 New Session', callback_data: `aisetsel_${newPayloadId}` });

  sessions.slice(0, 12).forEach((s, index) => {
    const payloadId = storeTelegramCallbackPayload(JSON.stringify({
      cli: activeCLI,
      workdir,
      sessionId: s.id,
    }));
    sessionButtons.push({ text: `${index + 1}. ${s.title}`, callback_data: `aisetsel_${payloadId}` });
  });

  const selected = resolveSelectedSessionId(chatId, activeCLI as 'opencode' | 'claude' | 'codex', workdir);
  const selectedText = selected ? `\`${selected}\`` : 'new session';
  await telegramBot.sendMessage(msg.chat.id, `📇 *AI Sessions from CLI*\n\nCLI: \`${activeCLI}\`\nCWD: \`${workdir}\`\nSelected: ${selectedText}\nFound: ${sessions.length}\n\nPick one for next /chat or /ai request.`, {
    parse_mode: 'Markdown',
    reply_markup: {
      inline_keyboard: sessionButtons.map((button) => [{ text: button.text, callback_data: button.callback_data }]),
    },
  });
});

telegramBot.onText(/\/status/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  const parts = text.split(' ');
  let type = 'session';
  
  if (parts.length > 1) {
    const t = parts[1].toLowerCase();
    if (t === 'process' || t === 'system' || t === 'all') {
      type = t;
    }
  }

  const chatId = msg.chat.id.toString();
  const session = terminalService.getSession(chatId);

  if (type === 'session' || type === 'all') {
    if (session) {
      const processRunning = terminalService.isProcessRunning(chatId);
      await telegramBot.sendMessage(msg.chat.id, `
📁 *Session Status*
• CWD: \`${session.cwd}\`
• Process: ${processRunning ? '🔄 Running' : '⏸️ Idle'}
• History: ${session.history.length} commands
      `.trim(), { parse_mode: 'Markdown' });
    } else {
      await telegramBot.sendMessage(chatId, '📭 No active session. Use /run to create one.');
    }
  }

  if (type === 'process' || type === 'all') {
    const processInfo = terminalService.getActiveProcess(chatId);
    if (processInfo) {
      const duration = Date.now() - processInfo.startTime.getTime();
      await telegramBot.sendMessage(msg.chat.id, `
🔄 *Running Process*
• Command: \`${processInfo.command}\`
• Duration: ${duration}ms
• User: \`${processInfo.userId}\`
      `.trim(), { parse_mode: 'Markdown' });
    } else if (type === 'process') {
      await telegramBot.sendMessage(chatId, '📭 No running process.');
    }
  }

  if (type === 'system') {
    const os = getOS();
    await telegramBot.sendMessage(msg.chat.id, `
🖥️ *System Status*
• OS: ${os}
• Node: ${process.version}
• Uptime: ${Math.floor(process.uptime() / 60)} minutes
• Memory: ${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB
    `.trim(), { parse_mode: 'Markdown' });
  }
});

telegramBot.onText(/\/queue/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  recoverFinishedButStuckAIThinking(chatId);

  const activeThinking = getAIThinking(chatId);
  const queued = getTelegramChatQueueSize(chatId);
  const lines = ['📚 *AI Queue Status*', ''];

  if (activeThinking) {
    const runningSeconds = Math.max(1, Math.floor((Date.now() - activeThinking.startedAt) / 1000));
    lines.push(`• Active: Yes (${activeThinking.mode}, ${runningSeconds}s)`);
  } else {
    lines.push('• Active: No');
  }

  lines.push(`• Queued /chat: ${queued}`);
  if (queued > 0) {
    lines.push('• Queue mode: FIFO (first in, first out)');
  }

  await telegramBot.sendMessage(msg.chat.id, lines.join('\n'), { parse_mode: 'Markdown' });
});


telegramBot.onText(/\/stop/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const processInfo = terminalService.getActiveProcess(chatId);
  const aiStopped = cancelAIThinking(chatId);
  const droppedQueuedChats = clearTelegramChatQueue(chatId);

  if (processInfo) {
    terminalService.killProcess(chatId);
    await telegramBot.sendMessage(
      msg.chat.id,
      droppedQueuedChats > 0
        ? `${aiStopped ? '🛑 Process and AI request stopped.' : '🛑 Process stopped.'} Also cleared ${droppedQueuedChats} queued /chat request(s).`
        : (aiStopped ? '🛑 Process and AI request stopped.' : '🛑 Process stopped.')
    );
  } else if (aiStopped) {
    await telegramBot.sendMessage(
      msg.chat.id,
      droppedQueuedChats > 0
        ? `🛑 AI request cancelled. Also cleared ${droppedQueuedChats} queued /chat request(s).`
        : '🛑 AI request cancelled.'
    );
  } else if (droppedQueuedChats > 0) {
    await telegramBot.sendMessage(msg.chat.id, `🧹 Cleared ${droppedQueuedChats} queued /chat request(s).`);
  } else {
    await telegramBot.sendMessage(msg.chat.id, '📭 No running process or AI request to stop.');
  }
});

telegramBot.onText(/\/cd/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  const parts = text.split(' ');
  parts.shift();
  const newCwd = parts.join(' ').trim();

  if (!newCwd) {
    await telegramBot.sendMessage(msg.chat.id, '❌ Please provide a directory path.\nUsage: /cd <directory>');
    return;
  }

  const chatId = msg.chat.id.toString();
  const session = terminalService.getOrCreateSession(chatId);
  
  if (terminalService.changeDirectory(chatId, newCwd)) {
    await telegramBot.sendMessage(chatId, `✅ Changed directory to: \`${session.cwd}\``, { parse_mode: 'Markdown' });
  } else {
    await telegramBot.sendMessage(chatId, `❌ Failed to change directory to: ${newCwd}\nDirectory may not exist or is not accessible.`);
  }
});

telegramBot.onText(/\/history/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  const parts = text.split(' ');
  let lines = 10;
  let clear = false;

  for (let i = 1; i < parts.length; i++) {
    if (parts[i] === '--clear') {
      clear = true;
    } else if (!isNaN(parseInt(parts[i]))) {
      lines = parseInt(parts[i]);
    }
  }

  const chatId = msg.chat.id.toString();

  if (clear) {
    terminalService.clearHistory(chatId);
    await telegramBot.sendMessage(chatId, '🗑️ History cleared.');
    return;
  }

  const history = terminalService.getHistory(chatId);
  if (history.length === 0) {
    await telegramBot.sendMessage(chatId, '📭 No command history.');
    return;
  }

const recentCommands = history.slice(-lines);
  let response = `📜 *Command History* (last ${recentCommands.length}):\n\n`;
  recentCommands.forEach((cmd, i) => {
    response += `${i + 1}. \`${cmd}\`\n`;
  });
  await telegramBot.sendMessage(chatId, response, {
    parse_mode: 'Markdown',
    reply_markup: {
      inline_keyboard: [[{ text: 'Clear History', callback_data: `historyclear_${chatId}` }]],
    },
  });
});

telegramBot.onText(/\/logs/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  const parts = text.split(' ');
  let limit = 10;
  
  for (let i = 1; i < parts.length; i++) {
    if (!isNaN(parseInt(parts[i]))) {
      limit = parseInt(parts[i]);
    }
  }

  const logs = securityService.getRecentLogs(limit);

  if (logs.length === 0) {
    await telegramBot.sendMessage(msg.chat.id, '📭 No command logs.');
    return;
  }

  let response = `📋 *Recent Logs* (last ${logs.length}):\n\n`;
  logs.slice(0, 5).forEach((log) => {
    const status = log.status === 'success' ? '✅' : log.status === 'failed' ? '❌' : '⚠️';
    const time = new Date(log.timestamp).toLocaleTimeString();
    response += `${status} [${time}] \`${log.command}\` (${log.status})\n`;
  });
  await telegramBot.sendMessage(msg.chat.id, response, { parse_mode: 'Markdown' });
});

telegramBot.onText(/\/sessionclear(?:\s+(all))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, ' You are not authorized to use this bot.');
    return;
  }

  const clearAll = (match?.[1] || '').trim().toLowerCase() === 'all';
  if (clearAll) {
    const cleared = terminalService.clearAllSessions();
    const clearedAI = clearAllAIConversations();
    await telegramBot.sendMessage(msg.chat.id, `🧹 Cleared ${cleared} terminal session${cleared === 1 ? '' : 's'} and reset AI context (${clearedAI.removedPersisted} saved chat thread${clearedAI.removedPersisted === 1 ? '' : 's'}).`);
    return;
  }

  const channelId = msg.chat.id.toString();
  const existing = terminalService.getSession(channelId);
  if (!existing) {
    const clearedAI = clearAIConversationForChat(channelId);
    if (clearedAI.removedInMemory > 0 || clearedAI.removedPersisted > 0) {
      await telegramBot.sendMessage(msg.chat.id, `🧹 No terminal session found, but AI context was reset (${clearedAI.removedPersisted} saved chat thread${clearedAI.removedPersisted === 1 ? '' : 's'}).`);
    } else {
      await telegramBot.sendMessage(msg.chat.id, '📭 No terminal session to clear in this chat.');
    }
    return;
  }

  terminalService.destroySession(channelId);
  const clearedAI = clearAIConversationForChat(channelId);
  await telegramBot.sendMessage(msg.chat.id, `🧹 Cleared the current terminal session and reset AI context (${clearedAI.removedPersisted} saved chat thread${clearedAI.removedPersisted === 1 ? '' : 's'}).`);
});

telegramBot.onText(/\/run\b/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  let timeoutMinutes: number | undefined;
  const parts = text.split(' ');
  parts.shift();
  const parsedParts: string[] = [];
  for (const part of parts) {
    if (part === '-t' || part === '--timeout' || part.startsWith('/timeout')) {
      const nextIdx = parts.indexOf(part) + 1;
      if (nextIdx < parts.length) {
        const parsed = parseInt(parts[nextIdx], 10);
        if (!isNaN(parsed) && parsed > 0) {
          timeoutMinutes = parsed;
          continue;
        }
      }
    }
    const match = part.match(/^(-t|--timeout|=)(\d+)$/);
    if (match) {
      const parsed = parseInt(match[2], 10);
      if (!isNaN(parsed) && parsed > 0) {
        timeoutMinutes = parsed;
        continue;
      }
    }
    parsedParts.push(part);
  }
  const command = parsedParts.join(' ').trim();

  if (!command) {
    await telegramBot.sendMessage(msg.chat.id, '❌ Please provide a command.\nUsage: /run <command> [timeout]\nExample: /run npm install\nExample: /run npm install 15\nExample: /run npm install -t 15');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();

  const validation = securityService.validateCommand(command);
  if (!validation.valid) {
    await telegramBot.sendMessage(msg.chat.id, `❌ ${validation.reason}`);
    return;
  }

  const cooldown = securityService.checkCooldown(userId);
  if (!cooldown.allowed) {
    await telegramBot.sendMessage(msg.chat.id, `⏳ Please wait ${Math.ceil((cooldown.remainingMs || 0) / 1000)} seconds before running another command.`);
    return;
  }

  const resolvedCommand = securityService.resolveAlias(command);
  const session = terminalService.getOrCreateSession(chatId);

  if (terminalService.isProcessRunning(chatId)) {
    await telegramBot.sendMessage(msg.chat.id, '⚠️ A process is already running. Please stop it first with /stop.');
    return;
  }

  const sentMessage = await telegramBot.sendMessage(chatId, `
⏳ *Executing Command*

📁 CWD: \`${session.cwd}\`
🔧 Command: \`${resolvedCommand}\`
⏱️ Timeout: ${timeoutMinutes ? `${timeoutMinutes} min` : 'default'}
📊 Status: Running...
  `.trim(), { parse_mode: 'Markdown', reply_markup: { inline_keyboard: [[{ text: '🛑 Stop', callback_data: `stop_${chatId}` }]] } });

  const outputs: string[] = [];
  const startTime = Date.now();
  const streamFlushIntervalMs = Math.max(200, config.streamFlushIntervalMs || 800);
  const streamFlushMaxChars = Math.max(500, config.streamFlushMaxChars || 3000);
  let streamBuffer = '';
  let streamFlushTimer: NodeJS.Timeout | null = null;
  let streamSendChain: Promise<void> = Promise.resolve();

  const sendGuaranteedTelegramCompletionNotice = async (text: string): Promise<void> => {
    const message = text || 'Command finished, but final response could not be rendered.';

    try {
      await safeTelegramEditMessageText(message, {
        chat_id: chatId,
        message_id: sentMessage.message_id,
        reply_markup: { inline_keyboard: [] },
      });
      return;
    } catch {}

    try {
      await telegramBot.sendMessage(chatId, message);
      return;
    } catch {}

    loggerService.error('All Telegram completion response fallback attempts failed', {
      chatId,
      userId,
      command: resolvedCommand,
    });
  };

  const enqueueStreamOutput = (text: string): void => {
    const normalized = (text || '').trim();
    if (!normalized) return;

    const parts = splitMessage(normalized, 3500);
    streamSendChain = streamSendChain
      .then(async () => {
        for (const part of parts) {
          await telegramBot.sendMessage(chatId, `📤 *Output:*\n\`\`\`\n${part}\n\`\`\``, { parse_mode: 'Markdown' });
        }
      })
      .catch((streamError) => {
        loggerService.warn('Failed to stream Telegram command output chunk', {
          chatId,
          userId,
          command: resolvedCommand,
          error: normalizeErrorMessage(streamError),
        });
      });
  };

  const flushStreamBuffer = (): void => {
    if (!streamBuffer.trim()) return;
    const chunk = streamBuffer;
    streamBuffer = '';
    enqueueStreamOutput(chunk);
  };

  const scheduleStreamFlush = (): void => {
    if (streamFlushTimer) return;
    streamFlushTimer = setTimeout(() => {
      streamFlushTimer = null;
      flushStreamBuffer();
    }, streamFlushIntervalMs);
  };

  try {
    await terminalService.executeCommand(
      chatId,
      userId,
      resolvedCommand,
      (data, type) => {
        const prefix = type === 'stderr' ? '❌ ' : '';
        const text = prefix + data;
        outputs.push(text);

        streamBuffer += text;
        if (streamBuffer.length >= streamFlushMaxChars) {
          if (streamFlushTimer) {
            clearTimeout(streamFlushTimer);
            streamFlushTimer = null;
          }
          flushStreamBuffer();
          return;
        }

        scheduleStreamFlush();
      },
      async (exitCode, signal) => {
        try {
          if (streamFlushTimer) {
            clearTimeout(streamFlushTimer);
            streamFlushTimer = null;
          }
          flushStreamBuffer();
          await Promise.race([
            streamSendChain,
            new Promise<void>((resolve) => setTimeout(resolve, 10000)),
          ]);

          const duration = Date.now() - startTime;
          
          securityService.logCommand({
            userId,
            channelId: chatId,
            command: resolvedCommand,
            status: exitCode === 0 ? 'success' : 'failed',
            duration,
          });

          const combinedOutput = outputs.join('');

          memoryService.trackCommand(
            resolvedCommand,
            session.cwd,
            exitCode,
            duration,
            userId,
            chatId,
            combinedOutput
          );

          const truncated = truncateOutput(combinedOutput, 3200);

          let statusEmoji = '✅';
          let statusText = 'Completed';
          if (signal) {
            statusEmoji = '🛑';
            statusText = 'Stopped';
          } else if (exitCode !== 0) {
            statusEmoji = '❌';
            statusText = 'Failed';
          }

          await safeTelegramEditMessageText(`
${statusEmoji} Command ${statusText}

CWD: ${session.cwd}
Command: ${resolvedCommand}
Timeout: ${timeoutMinutes ? `${timeoutMinutes} min` : 'default'}
Exit Code: ${exitCode ?? signal ?? 'N/A'}
Duration: ${duration}ms

Output:
${truncated || 'No output'}
          `.trim(), {
            chat_id: chatId,
            message_id: sentMessage.message_id,
            reply_markup: { inline_keyboard: [] }
          });
        } catch (finalizeError) {
          loggerService.error('Failed to finalize Telegram run response', {
            chatId,
            userId,
            command: resolvedCommand,
            error: normalizeErrorMessage(finalizeError),
          });

          await sendGuaranteedTelegramCompletionNotice(`⚠️ Command finished. Exit: ${exitCode ?? signal ?? 'N/A'} (final render had an error).`);
        }
      },
      timeoutMinutes
    );
  } catch (error: any) {
    await safeTelegramEditMessageText(`
❌ *Error*

🔧 Command: \`${command}\`
📝 Error: ${error.message}
    `.trim(), {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: [] }
    });
  }
});

telegramBot.onText(/\/runapp(?:\s+(\S+))?\b/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, 'You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const deviceId = match?.[1]?.trim() || terminalService.getSelectedDevice(chatId);
  const session = terminalService.getOrCreateSession(chatId);

  if (!androidService.isAndroidProject(session.cwd)) {
    await telegramBot.sendMessage(
      msg.chat.id,
      `Current directory is not an Android project.\nPath: \`${session.cwd}\``,
      { parse_mode: 'Markdown' }
    );
    return;
  }

  const cooldown = securityService.checkCooldown(userId);
  if (!cooldown.allowed) {
    await telegramBot.sendMessage(msg.chat.id, `Please wait ${Math.ceil((cooldown.remainingMs || 0) / 1000)} seconds before running another command.`);
    return;
  }

  if (terminalService.isProcessRunning(chatId)) {
    await telegramBot.sendMessage(msg.chat.id, 'A process is already running. Please stop it first with /stop.');
    return;
  }

  const plan = androidService.buildRunAppCommand(session.cwd, deviceId);
const sentMessage = await telegramBot.sendMessage(chatId, `
*Running Android App*

CWD: \`${session.cwd}\`
Runner: \`${plan.runner}\`
Device: ${plan.deviceId ? `\`${plan.deviceId}\`` : 'auto-detect'}
${plan.applicationId ? `App ID: \`${plan.applicationId}\`\n` : ''}Status: ${plan.requiresPhysicalDevice ? 'Checking connected physical device, installing, and verifying app launch...' : 'Installing debug build...'}
  `.trim(), { parse_mode: 'Markdown', reply_markup: { inline_keyboard: [[{ text: 'Stop', callback_data: `stop_${chatId}` }]] } });

  const outputs: string[] = [];
  const startTime = Date.now();
  let earlyErrorReported = false;

  const extractRunAppFailureTail = (output: string): string => {
    const normalized = (output || '').replace(/\r\n/g, '\n');
    const markers = [
      /^> Task .* FAILED$/m,
      /^FAILURE: Build failed.*$/m,
      /^BUILD FAILED.*$/m,
      /^FAILURE:.*$/m,
      /INSTALL_FAILED/m,
      /adb: failed/i,
      /error:/i,
    ];

    let startIndex = -1;
    for (const marker of markers) {
      const match = marker.exec(normalized);
      if (match && typeof match.index === 'number') {
        if (startIndex < 0 || match.index < startIndex) {
          startIndex = match.index;
        }
      }
    }

    return startIndex >= 0 ? normalized.slice(startIndex).trim() : normalized.trim();
  };

  const shouldReportEarlyError = (chunk: string, type: 'stdout' | 'stderr'): boolean => {
    if (type === 'stderr') return true;
    return /FAILURE:|BUILD FAILED|error:|INSTALL_FAILED|adb: failed/i.test(chunk);
  };

  const reportEarlyError = async (chunk: string): Promise<void> => {
    if (earlyErrorReported) return;
    earlyErrorReported = true;

    await safeTelegramEditMessageText(`
*Android Run Error Detected*

CWD: \`${session.cwd}\`
Runner: \`${plan.runner}\`
Device: ${plan.deviceId ? `\`${plan.deviceId}\`` : 'auto-detect'}
${plan.applicationId ? `App ID: \`${plan.applicationId}\`\n` : ''}Status: Error detected. Waiting for process to finish...

\`\`\`
${truncateOutput(extractRunAppFailureTail(chunk), 1500) || 'Unknown error'}
\`\`\`
    `.trim(), {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: [[{ text: 'Stop', callback_data: `stop_${chatId}` }]] }
    });
  };

  try {
    await terminalService.executeCommand(
      chatId,
      userId,
      plan.command,
      (data, type) => {
        const prefix = type === 'stderr' ? 'ERR: ' : '';
        outputs.push(prefix + data);
        if (shouldReportEarlyError(data, type)) {
          void reportEarlyError(data);
        }
      },
      async (exitCode, signal) => {
        const duration = Date.now() - startTime;

        securityService.logCommand({
          userId,
          channelId: chatId,
          command: plan.command,
          status: exitCode === 0 ? 'success' : 'failed',
          duration,
        });

        const combinedOutput = outputs.join('');

        memoryService.trackCommand(
          plan.command,
          session.cwd,
          exitCode,
          duration,
          userId,
          chatId,
          combinedOutput
        );

        const outputForUser = exitCode !== 0 && !signal
          ? extractRunAppFailureTail(combinedOutput)
          : combinedOutput;
        const truncated = truncateOutput(outputForUser, 3950);

        let statusText = plan.launchEnabled ? 'Launched on Device' : 'Installed';
        if (signal) {
          statusText = 'Stopped';
        } else if (exitCode !== 0) {
          statusText = 'Failed';
        }

        await safeTelegramEditMessageText(`
*Android App ${statusText}*

CWD: \`${session.cwd}\`
Runner: \`${plan.runner}\`
Device: ${plan.deviceId ? `\`${plan.deviceId}\`` : 'auto-detect'}
${plan.applicationId ? `App ID: \`${plan.applicationId}\`\n` : ''}Exit Code: ${exitCode ?? signal ?? 'N/A'}
Duration: ${duration}ms

\`\`\`
${truncated || 'No output'}
\`\`\`
        `.trim(), {
          chat_id: chatId,
          message_id: sentMessage.message_id,
          parse_mode: 'Markdown',
          reply_markup: { inline_keyboard: [] }
        });
      }
    );
  } catch (error: any) {
    await safeTelegramEditMessageText(`
*Error*

CWD: \`${session.cwd}\`
Error: ${error.message}
    `.trim(), {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: [] }
    });
  }
});

telegramBot.onText(/\/syncproject\b/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const session = terminalService.getOrCreateSession(chatId);

  if (!androidService.isAndroidProject(session.cwd)) {
    await telegramBot.sendMessage(
      msg.chat.id,
      `❌ Current directory is not an Android project.\nPath: \`${session.cwd}\``,
      { parse_mode: 'Markdown' }
    );
    return;
  }

  const cooldown = securityService.checkCooldown(userId);
  if (!cooldown.allowed) {
    await telegramBot.sendMessage(msg.chat.id, `⏳ Please wait ${Math.ceil((cooldown.remainingMs || 0) / 1000)} seconds before running another command.`);
    return;
  }

  if (terminalService.isProcessRunning(chatId)) {
    await telegramBot.sendMessage(msg.chat.id, '⚠️ A process is already running. Please stop it first with /stop.');
    return;
  }

  const plan = androidService.buildSyncProjectCommand(session.cwd);
  const sentMessage = await telegramBot.sendMessage(chatId, `
⏳ *Syncing Android Project*

📁 CWD: \`${session.cwd}\`
🔧 Runner: \`${plan.runner}\`
📊 Status: Running Gradle sync...
  `.trim(), { parse_mode: 'Markdown', reply_markup: { inline_keyboard: [[{ text: '🛑 Stop', callback_data: `stop_${chatId}` }]] } });

  const outputs: string[] = [];
  const startTime = Date.now();

  try {
    await terminalService.executeCommand(
      chatId,
      userId,
      plan.command,
      (data, type) => {
        const prefix = type === 'stderr' ? '❌ ' : '';
        outputs.push(prefix + data);
      },
      async (exitCode, signal) => {
        const duration = Date.now() - startTime;

        securityService.logCommand({
          userId,
          channelId: chatId,
          command: plan.command,
          status: exitCode === 0 ? 'success' : 'failed',
          duration,
        });

        const combinedOutput = outputs.join('');
        memoryService.trackCommand(
          plan.command,
          session.cwd,
          exitCode,
          duration,
          userId,
          chatId,
          combinedOutput
        );

        const truncated = truncateOutput(combinedOutput, 3950);
        let statusEmoji = '✅';
        let statusText = 'Synced';
        if (signal) {
          statusEmoji = '🛑';
          statusText = 'Stopped';
        } else if (exitCode !== 0) {
          statusEmoji = '❌';
          statusText = 'Failed';
        }

        await safeTelegramEditMessageText(`
${statusEmoji} *Android Project ${statusText}*

📁 CWD: \`${session.cwd}\`
🔧 Runner: \`${plan.runner}\`
📊 Exit Code: ${exitCode ?? signal ?? 'N/A'}
⏱️ Duration: ${duration}ms

\`\`\`
${truncated || 'No output'}
\`\`\`
        `.trim(), {
          chat_id: chatId,
          message_id: sentMessage.message_id,
          parse_mode: 'Markdown',
          reply_markup: { inline_keyboard: [] }
        });
      }
    );
  } catch (error: any) {
    await safeTelegramEditMessageText(`
❌ *Error*

📁 CWD: \`${session.cwd}\`
📝 Error: ${error.message}
    `.trim(), {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: [] }
    });
  }
});

async function executeTelegramChatRequest(chatId: string, request: QueuedTelegramChatRequest): Promise<void> {
  const { userId, message, workdir, model, cli: activeCLI } = request;
  const cliName = aiService.getCliDisplayName(activeCLI);
  const sessionKey = `${chatId}_${userId}`;

  applyLastUsedModelForChat(workdir, activeCLI, model);

  const requestId = beginAIThinking(chatId, 'chat');
  if (!requestId) {
    const queuePosition = enqueueTelegramChatRequest(chatId, request);
    await telegramBot.sendMessage(chatId, `⏳ AI is busy. Your message was queued at position #${queuePosition}.`);
    return;
  }

  let session = chatSessions.get(sessionKey);
  if (!session) {
    session = {
      messages: [],
      workdir,
    };
    chatSessions.set(sessionKey, session);
  } else if (workdir !== session.workdir) {
    session.workdir = workdir;
  }

  const sentMessage = await telegramBot.sendMessage(chatId, '🤖 *Thinking...*', { parse_mode: 'Markdown' });
  const waitingHeartbeat = startAIWaitingHeartbeat(chatId, sentMessage.message_id, '🤖 *Thinking...*');
  let streamMessageIds = [sentMessage.message_id];
  const startTime = Date.now();

  const chatStreamState: StreamRenderState = { rawText: '', visibleText: '', lastDeltaNormalized: '', repeatCount: 0, seenNormalizedSegments: new Set() };

  try {
    const selectedSessionId = resolveSelectedSessionId(chatId, activeCLI as 'opencode' | 'claude' | 'codex', session.workdir);
    const response = await aiService.chatStream(
      [],
      message,
      (chunk) => {
        const rendered = appendVisibleStreamDelta(chunk, chatStreamState);
        if (!rendered) {
          return;
        }
        waitingHeartbeat.markChunkReceived();
        syncTelegramCodeBlockParts(chatId, streamMessageIds, rendered, `🤖 *${cliName} Chat*`).then((ids) => {
          streamMessageIds = ids;
        }).catch(() => {});
      },
      {
        workdir: session.workdir,
        model,
        cli: activeCLI,
        requestId,
        sessionId: selectedSessionId,
      }
    );

    const duration = Date.now() - startTime;
    const renderedText = (response.displayText || response.text).replace(/\x1b\[[0-9;]*m/g, '').trim();

    if (renderedText) {
      const renderedFinal = appendVisibleStreamDelta(renderedText, chatStreamState);
      if (renderedFinal) {
        await syncTelegramCodeBlockParts(chatId, streamMessageIds, renderedFinal, `🤖 *${cliName} Chat*`);
      }
    }

    await sendAIArtifacts(chatId, response, `${activeCLI} response payload`);

    if (response.tokens) {
      memoryService.addAIUsageSnapshot({
        timestamp: new Date().toISOString(),
        cli: activeCLI,
        model,
        mode: 'chat',
        channelId: chatId,
        userId,
        cwd: session.workdir,
        inputTokens: response.tokens.input,
        outputTokens: response.tokens.output,
        totalTokens: response.tokens.total,
        cost: response.tokens.cost,
      });
    }

    let responseText = `✅ *${cliName} Chat finished*\n`;
    responseText += `⏱️ Time: ${duration}ms\n`;
    responseText += `💬 Messages: 1`;
    if (response.tokens) {
      responseText += `\n📊 Tokens: 📥${response.tokens.input} | 📤${response.tokens.output}`;
      if (response.tokens.quota) {
        const q = response.tokens.quota;
        if (q.remaining) {
          responseText += `\n💰 Remaining: ${q.remaining}`;
        }
        if (q.usedPercent) {
          responseText += ` (${q.usedPercent}% used)`;
        }
        if (q.resetsIn) {
          responseText += ` | Resets: ${q.resetsIn}`;
        }
      }
    }

    await telegramBot.sendMessage(chatId, responseText, {
      parse_mode: 'Markdown'
    });
  } catch (error: any) {
    const duration = Date.now() - startTime;
    const errorMessage = normalizeErrorMessage(error);
    const limitNotice = buildLimitNotice(activeCLI, errorMessage) || '';
    await safeTelegramEditMessageText(`❌ *Error*\n\n${errorMessage}${limitNotice}\n⏱️ Time: ${duration}ms`, {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown'
    });
  } finally {
    waitingHeartbeat.stop();
    endAIThinking(chatId, requestId);

    const nextQueued = dequeueTelegramChatRequest(chatId);
    if (nextQueued) {
      const remaining = getTelegramChatQueueSize(chatId);
      await telegramBot.sendMessage(
        chatId,
        `▶️ Processing queued /chat from queue. Remaining after this: ${remaining}.`
      );
      executeTelegramChatRequest(chatId, nextQueued).catch((err) => {
        loggerService.error('Failed to process queued Telegram /chat request', {
          chatId,
          error: normalizeErrorMessage(err),
        });
      });
    }
  }
}

telegramBot.onText(/\/chat/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const text = msg.text || '';
  const parts = text.split(' ');
  parts.shift();

  let workdir = terminalService.getSession(chatId)?.cwd || process.cwd();
  let clear = false;
  let message = '';
  const activeCLI = aiService.getCLI();
  let model = aiService.getDefaultModel(activeCLI);

  for (let i = 0; i < parts.length; i++) {
    if (parts[i] === '--path' && i + 1 < parts.length) {
      workdir = parts[i + 1];
      i++;
    } else if (parts[i] === '--model' && i + 1 < parts.length) {
      model = parts[i + 1];
      i++;
    } else if (parts[i] === '--clear') {
      clear = true;
    } else {
      message += (message ? ' ' : '') + parts[i];
    }
  }

  const sessionKey = `${chatId}_${userId}`;

  if (clear) {
    chatSessions.delete(sessionKey);
    memoryService.clearChatHistory(sessionKey);
    cancelAIThinking(chatId);
    clearTelegramChatQueue(chatId);
    saveRuntimeState();
    await telegramBot.sendMessage(chatId, '🗑️ Chat history cleared.');
    return;
  }

  if (!message.trim()) {
    await telegramBot.sendMessage(chatId, '❌ Please provide a message.\nUsage: /chat <message> [--path directory] [--clear]');
    return;
  }

  {
    const modelValidation = await aiService.validateModelSelectionForCLI(activeCLI, model);
    if (!modelValidation.ok) {
      await telegramBot.sendMessage(chatId, `Error: ${modelValidation.error}`);
      return;
    }
    model = modelValidation.normalized || model;
  }

  if (!memoryService.getProjectByPath(workdir)) {
    await telegramBot.sendMessage(chatId, '❌ Select a project first with /project, then use /chat.');
    return;
  }

  if (activeCLI === 'opencode' && !aiService.isOpenCodeReadyForWorkdir(workdir)) {
    await telegramBot.sendMessage(chatId, '? AI is not started for this project yet. Run /ai first to start/check AI, then use /chat.');
    return;
  }

  recoverFinishedButStuckAIThinking(chatId);

  const request: QueuedTelegramChatRequest = {
    userId,
    message,
    workdir,
    model,
    cli: activeCLI,
    enqueuedAt: Date.now(),
  };

  const activeThinking = getAIThinking(chatId);
  if (activeThinking) {
    const queuePosition = enqueueTelegramChatRequest(chatId, request);
    const waitSeconds = Math.max(1, Math.floor((Date.now() - activeThinking.startedAt) / 1000));
    await telegramBot.sendMessage(
      chatId,
      `⏳ AI is still generating (${activeThinking.mode}, ${waitSeconds}s). Added to queue at position #${queuePosition}.`
    );
    return;
  }

  await executeTelegramChatRequest(chatId, request);
});

telegramBot.onText(/\/ai/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  const parts = text.split(' ');
  parts.shift();

  const chatId = msg.chat.id.toString();
  const activeCLI = aiService.getCLI();
  const cliName = aiService.getCliDisplayName(activeCLI);
  let model = aiService.getDefaultModel(activeCLI);
  let workdir = terminalService.getSession(chatId)?.cwd || process.cwd();
  let prompt = '';

  for (let i = 0; i < parts.length; i++) {
    if (parts[i] === '--model' && i + 1 < parts.length) {
      model = parts[i + 1];
      i++;
    } else if (parts[i] === '--path' && i + 1 < parts.length) {
      workdir = parts[i + 1];
      i++;
    } else {
      prompt += (prompt ? ' ' : '') + parts[i];
    }
  }

  const project = memoryService.getProjectByPath(workdir);
  if (!project) {
    await telegramBot.sendMessage(chatId, '? Select a project first with /project, then use /ai or /chat.');
    return;
  }

  {
    const modelValidation = await aiService.validateModelSelectionForCLI(activeCLI, model);
    if (!modelValidation.ok) {
      await telegramBot.sendMessage(chatId, `Error: ${modelValidation.error}`);
      return;
    }
    model = modelValidation.normalized || model;
  }

  if (!prompt.trim()) {
    if (activeCLI === 'opencode' && !aiService.usesOpenCodeDirectCLI()) {
      const started = await aiService.ensureReadyForCLI(activeCLI, workdir);
      if (!started) {
        await telegramBot.sendMessage(chatId, `❌ Failed to start ${cliName} for project \`${project.name}\`.`, {
          parse_mode: 'Markdown',
        });
        return;
      }

      const status = aiService.getStatus(activeCLI);
      const statusLines = [
        '✅ *AI status*',
        `CLI: \`${status.cli}\``,
        `Model: \`${status.model}\``,
        `Project: \`${project.name}\``,
        `CWD: \`${workdir}\``,
        `Mode: ${aiService.getCliModeLabel(activeCLI)}`,
        `Started: ${status.started ? 'Yes' : 'No'}`,
        `Ready: ${status.ready ? 'Yes' : 'No'}`,
      ];
      await telegramBot.sendMessage(chatId, statusLines.join('\n'), {
        parse_mode: 'Markdown',
      });

      const sessions = await aiService.listSessionsForCLI(activeCLI, workdir).catch(() => []);
      const sessionButtons: Array<{ text: string; callback_data: string }> = [];
      const newPayloadId = storeTelegramCallbackPayload(JSON.stringify({
        cli: activeCLI,
        workdir,
        sessionId: '',
      }));
      sessionButtons.push({ text: '🆕 New Session', callback_data: `aisetsel_${newPayloadId}` });

      sessions.slice(0, 8).forEach((s, index) => {
        const payloadId = storeTelegramCallbackPayload(JSON.stringify({
          cli: activeCLI,
          workdir,
          sessionId: s.id,
        }));
        sessionButtons.push({ text: `${index + 1}. ${s.title}`, callback_data: `aisetsel_${payloadId}` });
      });

      await telegramBot.sendMessage(chatId, `📇 *Select session for this project*\n\nCLI: \`${activeCLI}\`\nModel: \`${model}\`\nProject: \`${project.name}\`\n\nThen use /chat <message> or /ai <prompt>.`, {
        parse_mode: 'Markdown',
        reply_markup: {
          inline_keyboard: sessionButtons.map((button) => [{ text: button.text, callback_data: button.callback_data }]),
        },
      });
      return;
    }

    const status = aiService.getStatus(activeCLI);
    await telegramBot.sendMessage(chatId, [
      '✅ *AI status*',
      `CLI: \`${status.cli}\``,
      `Model: \`${status.model}\``,
      `Project: \`${project.name}\``,
      `CWD: \`${workdir}\``,
      `Mode: ${aiService.getCliModeLabel(activeCLI)}`,
    ].join('\n'), {
      parse_mode: 'Markdown',
    });

    const sessions = await aiService.listSessionsForCLI(activeCLI, workdir).catch(() => []);
    const sessionButtons: Array<{ text: string; callback_data: string }> = [];
    const newPayloadId = storeTelegramCallbackPayload(JSON.stringify({
      cli: activeCLI,
      workdir,
      sessionId: '',
    }));
    sessionButtons.push({ text: '🆕 New Session', callback_data: `aisetsel_${newPayloadId}` });

    sessions.slice(0, 8).forEach((s, index) => {
      const payloadId = storeTelegramCallbackPayload(JSON.stringify({
        cli: activeCLI,
        workdir,
        sessionId: s.id,
      }));
      sessionButtons.push({ text: `${index + 1}. ${s.title}`, callback_data: `aisetsel_${payloadId}` });
    });

    await telegramBot.sendMessage(chatId, `📇 *Select session for this project*\n\nCLI: \`${activeCLI}\`\nModel: \`${model}\`\nProject: \`${project.name}\`\n\nThen use /chat <message> or /ai <prompt>.`, {
      parse_mode: 'Markdown',
      reply_markup: {
        inline_keyboard: sessionButtons.map((button) => [{ text: button.text, callback_data: button.callback_data }]),
      },
    });
    return;
  }

  recoverFinishedButStuckAIThinking(chatId);
  const activeThinking = getAIThinking(chatId);
  if (activeThinking) {
    const waitSeconds = Math.max(1, Math.floor((Date.now() - activeThinking.startedAt) / 1000));
    await telegramBot.sendMessage(chatId, `⏳ AI is still generating (${activeThinking.mode}, ${waitSeconds}s). Please wait or run /stop to cancel first.`);
    return;
  }

  const startNotice = await telegramBot.sendMessage(chatId, `🤖 *Preparing ${cliName}...*`, {
    parse_mode: 'Markdown',
  });

  try {
    if (activeCLI === 'opencode' && !aiService.usesOpenCodeDirectCLI()) {
      const ready = await aiService.ensureReadyForCLI(activeCLI, workdir);
      if (!ready) {
        await safeTelegramEditMessageText(`❌ *Failed to start AI*\n\nProject: \`${project.name}\`\nCLI: \`${activeCLI}\``, {
          chat_id: chatId,
          message_id: startNotice.message_id,
          parse_mode: 'Markdown',
        });
        return;
      }
    }

    const sessions = await aiService.listSessionsForCLI(activeCLI, workdir).catch(() => []);
    const newPayloadId = storeTelegramCallbackPayload(JSON.stringify({
      sessionId: 'new',
      model,
      workdir,
      prompt,
      cli: activeCLI,
    }));
    const sessionButtons = [{
      text: '🆕 New Session',
      callback_data: `aisession_${newPayloadId}`,
    }];

    sessions.slice(0, 5).forEach((s, i) => {
      const payloadId = storeTelegramCallbackPayload(JSON.stringify({
        sessionId: s.id,
        model,
        workdir,
        prompt,
        cli: activeCLI,
      }));
      sessionButtons.push({
        text: `${i + 1}. ${s.title}`,
        callback_data: `aisession_${payloadId}`,
      });
    });

    const promptPreview = prompt.slice(0, 80).replace(/[`]/g, "'");
    await safeTelegramEditMessageText(`📇 *Select session for /ai*\n\nCLI: \`${activeCLI}\`\nModel: \`${model}\`\nProject: \`${project.name}\`\n\nPrompt: ${promptPreview}${prompt.length > 80 ? '...' : ''}`, {
      chat_id: chatId,
      message_id: startNotice.message_id,
      parse_mode: 'Markdown',
      reply_markup: {
        inline_keyboard: sessionButtons.map((button) => [{ text: button.text, callback_data: button.callback_data }]),
      },
    });
  } catch (error: any) {
    const errorMessage = normalizeErrorMessage(error);
    const limitNotice = buildLimitNotice(activeCLI, errorMessage) || '';
    await safeTelegramEditMessageText(`❌ *Error*\n\n${errorMessage}${limitNotice}`, {
      chat_id: chatId,
      message_id: startNotice.message_id,
      parse_mode: 'Markdown',
    });
  }
});
telegramBot.onText(/\/cli(?:\s+(\S+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const selected = match?.[1]?.trim() as any;
  const validCLIs = ['opencode', 'claude', 'codex'];

  if (!selected) {
    const current = aiService.getCLI();
    await telegramBot.sendMessage(
      msg.chat.id,
      'AI CLI Backend\n\nCurrent: `' + current + '`\nCurrent model: `' + modelDisplayName(aiService.getDefaultModel()) + '`\n\nAvailable:\n- `opencode` - OpenCode CLI (`opencode run --format json`)\n- `claude` - Claude Code CLI (`claude --print`)\n- `codex` - OpenAI Codex CLI (`codex exec --json`)\n\nSwitch with: /cli opencode | /cli claude | /cli codex',
      {
        parse_mode: 'Markdown',
        reply_markup: {
          inline_keyboard: [
            validCLIs.map((cli) => ({
              text: cli === current ? `${cli} ✅` : cli,
              callback_data: `clibtn_${cli}`,
            })),
          ],
        },
      }
    );
    return;
  }

  if (!validCLIs.includes(selected)) {
    await telegramBot.sendMessage(msg.chat.id, `❌ Unknown CLI: \`${selected}\`. Choose from: ${validCLIs.join(', ')}`, { parse_mode: 'Markdown' });
    return;
  }

  aiService.setCLI(selected);
  saveRuntimeState();
  const currentCwd = terminalService.getSession(msg.chat.id.toString())?.cwd;
  if (currentCwd) {
    memoryService.setProjectAISettingsByCwd(currentCwd, {
      cli: selected,
      model: aiService.getDefaultModel(),
    });
  }
  await telegramBot.sendMessage(msg.chat.id, `✅ AI CLI switched to \`${selected}\``, { parse_mode: 'Markdown' });
});

telegramBot.onText(/(?:\/|\.)models(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const provider = match?.[1]?.trim();

  if (aiService.getCLI() !== 'opencode') {
    const currentCLI = aiService.getCLI();
    const allModels = await aiService.listModelsForCLI(currentCLI as any).catch(() => aiService.getSupportedModels(currentCLI as any));
    const filteredModels = provider ? allModels.filter(model => model.toLowerCase().includes(provider.toLowerCase())) : allModels;
    await telegramBot.sendMessage(
      msg.chat.id,
      (currentCLI === 'codex'
        ? 'Available models for CLI `codex`\nCurrent model: `' + modelDisplayName(aiService.getDefaultModel()) + '`\n' + (provider ? `Filter: \`${provider}\`\n` : '\n') + '\nLoaded from OpenAI Models API when available. You can still set any compatible model manually with /model <name>.'
        : 'Suggested models for CLI `' + currentCLI + '`\nCurrent model: `' + modelDisplayName(aiService.getDefaultModel()) + '`\n' + (provider ? `Filter: \`${provider}\`\n` : '\n') + '\nThis is not a full list from the CLI. You can still set any compatible model manually with /model <name>.'),
      {
        parse_mode: 'Markdown',
        reply_markup: {
          inline_keyboard: await (async () => {
            const rows: Array<Array<{ text: string; callback_data: string }>> = [];
            const pageItems = filteredModels.slice(0, 12);
            for (let i = 0; i < pageItems.length; i += 3) {
              rows.push(
                pageItems.slice(i, i + 3).map((example: string) => ({
                  text: example === aiService.getDefaultModel() ? `${modelDisplayName(example)} ✅` : modelDisplayName(example),
                  callback_data: `quickmodel_${example}`,
                }))
              );
            }
            if (filteredModels.length > 12) {
              rows.push([
                { text: 'Prev', callback_data: `quickmodelpage_${currentCLI}__-1` },
                { text: 'Next', callback_data: `quickmodelpage_${currentCLI}__1` },
              ]);
            }
            return rows;
          })(),
        },
      }
    );
    return;
  }

  try {
    await sendTelegramModelsPage(msg.chat.id, provider || undefined, 0);
  } catch (error: any) {
    await telegramBot.sendMessage(msg.chat.id, `❌ Failed to list models: ${normalizeErrorMessage(error)}`);
  }
});

telegramBot.onText(/\/tokens(?:\s+(.*))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const currentCLI = aiService.getCLI();
  const rawArgs = match?.[1]?.trim() || '';
  const parts = rawArgs.split(/\s+/).filter(Boolean);
  const command = parts[0] || 'show';
  const days = /^\d+$/.test(command) ? command : parts.find(part => /^\d+$/.test(part));
  const threshold = parts.find(part => /^\d+$/.test(part) && part !== days);

  try {
    if (currentCLI !== 'opencode') {
      const cliLabel = aiService.getCliDisplayName(currentCLI);
      await telegramBot.sendMessage(
        msg.chat.id,
        `📊 *${cliLabel} Token Usage*\n\nCurrent CLI: \`${currentCLI}\`\nCurrent model: \`${modelDisplayName(aiService.getDefaultModel())}\`\n\n${cliLabel} CLI does not expose quota or balance stats through this bot.\nPer-request token counts may still appear in /ai or /chat responses when the CLI returns them.`,
        { parse_mode: 'Markdown' }
      );
      return;
    }

    if (command === 'watch' || command === 'watch-on') {
      const parsedThreshold = threshold ? Number(threshold) : undefined;
      memoryService.updateTokenWatch({
        enabled: true,
        thresholdPercent: parsedThreshold || memoryService.getTokenWatch().thresholdPercent,
      });
      memoryService.subscribeTokenWatch({ telegramChat: msg.chat.id.toString() });
      const watch = memoryService.getTokenWatch();
      await telegramBot.sendMessage(msg.chat.id, `✅ Token watch enabled at ${watch.thresholdPercent}% for this chat.`);
      return;
    }

    if (command === 'watch-off') {
      memoryService.unsubscribeTokenWatch({ telegramChat: msg.chat.id.toString() });
      await telegramBot.sendMessage(msg.chat.id, '✅ Token watch disabled for this chat.');
      return;
    }

    if (command === 'watch-status') {
      const watch = memoryService.getTokenWatch();
      await telegramBot.sendMessage(msg.chat.id, `Watch enabled: ${watch.enabled ? 'Yes' : 'No'}\nThreshold: ${watch.thresholdPercent}%\nTelegram chats: ${watch.telegramChats.length}\nDiscord channels: ${watch.discordChannels.length}`);
      return;
    }

    const launcher = getOpenCodeLauncher();
    const output = execFileSync(launcher.command, [...launcher.args, 'stats', '--days', String(days)], {
      encoding: 'utf-8',
      windowsHide: true,
    });

    const stats = parseTokenStatsOutput(output);
    memoryService.addTokenSnapshot({ timestamp: new Date().toISOString(), ...stats });

    const { used, remaining, totalCost, inputTokens, outputTokens, usedPercent, remainingPercent, modelBreakdown } = stats;
    const alert = buildTokenAlert(remainingPercent);

    let text = '📊 *OpenCode Token Usage*\n\n';
    if (used) text += `• Used: ${used}\n`;
    if (remaining) text += `• Remaining: ${remaining}\n`;
    if (usedPercent !== null) text += `• Used Bar: ${buildTokenBar(usedPercent)}\n`;
    if (remainingPercent !== null) text += `• Remaining Bar: ${buildTokenBar(remainingPercent)}\n`;
    if (alert) text += `• ${alert}\n`;
    if (totalCost) text += `• Cost: ${totalCost}\n`;
    if (inputTokens) text += `• Input Tokens: ${inputTokens}\n`;
    if (outputTokens) text += `• Output Tokens: ${outputTokens}\n`;
    if (modelBreakdown.length > 0) text += `\n*By Model:*\n${modelBreakdown.map(line => `• ${line}`).join('\n')}\n`;
    text += `\n*Trend:*\n${buildTokenTrendText()}\n`;
    const watch = memoryService.getTokenWatch();
    text += `\n*Watch:* ${watch.enabled ? 'On' : 'Off'} at ${watch.thresholdPercent}%`;

    if (text === '📊 *OpenCode Token Usage*\n\n') {
      text += '```\n' + output.trim().slice(0, 3000) + '\n```';
    }

    await telegramBot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
  } catch (error: any) {
    await telegramBot.sendMessage(msg.chat.id, `❌ Failed to get token stats: ${normalizeErrorMessage(error)}`);
  }
});

telegramBot.onText(/\/usage(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, ' You are not authorized to use this bot.');
    return;
  }

  const raw = match?.[1]?.trim() || '';
  const parts = raw.split(/\s+/).filter(Boolean);
  const days = Number(parts.find(part => /^\d+$/.test(part)) || '7');
  const cli = parts.find(part => ['opencode', 'claude', 'codex'].includes(part)) as any;
  const rows = memoryService.summarizeAIUsage(days, cli);
  const recent = memoryService.getAIUsageHistory(8, cli);

  let text = `ðŸ“Š *AI Usage*\n\nDays: ${days}\nCLI filter: ${cli || 'all'}\nCurrent CLI: ${aiService.getCLI()}\n\n`;
  text += '*Daily Summary*\n';
  text += rows.length > 0
    ? rows.slice(0, 8).map(row => `â€¢ ${row.day} | ${row.cli} | req=${row.requests} | total=${row.totalTokens} | in=${row.inputTokens} | out=${row.outputTokens}`).join('\n')
    : 'No tracked AI usage yet.';
  text += '\n\n*Recent Requests*\n';
  text += recent.length > 0
    ? recent.map(item => `â€¢ ${item.timestamp.slice(0, 16).replace('T', ' ')} | ${item.cli} | ${item.mode} | ${item.model} | total=${item.totalTokens || 0}`).join('\n')
    : 'No recent requests.';

  await telegramBot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
});

telegramBot.onText(/\/changes(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, ' You are not authorized to use this bot.');
    return;
  }

  const workdir = match?.[1]?.trim() || terminalService.getSession(msg.chat.id.toString())?.cwd || process.cwd();
  git.setCwd(workdir);

  try {
    const status = await git.getStatus();
    let text = `ðŸ“ *Git Changes*\n\nPath: \`${workdir}\`\nBranch: \`${status.branch || 'unknown'}\`\nAhead: ${status.ahead} | Behind: ${status.behind}\n\n`;
    text += `*Modified*\n${status.modified.length ? status.modified.slice(0, 20).map(f => `â€¢ ${f}`).join('\n') : 'None'}\n\n`;
    text += `*Staged*\n${status.staged.length ? status.staged.slice(0, 20).map(f => `â€¢ ${f}`).join('\n') : 'None'}\n\n`;
    text += `*Untracked*\n${status.untracked.length ? status.untracked.slice(0, 20).map(f => `â€¢ ${f}`).join('\n') : 'None'}`;
    await telegramBot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
  } catch (error: any) {
    await telegramBot.sendMessage(msg.chat.id, ` Failed to read git changes: ${normalizeErrorMessage(error)}`);
  }
});

telegramBot.onText(/\/health/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, ' You are not authorized to use this bot.');
    return;
  }

  const aiHealthy = aiService.getCLI() === 'opencode' && !aiService.usesOpenCodeDirectCLI()
    ? await aiService.healthCheck().catch(() => false)
    : true;
  const backendLabel = aiService.getCLI() === 'opencode' && !aiService.usesOpenCodeDirectCLI()
    ? `${aiHealthy ? 'Healthy' : 'Unhealthy'} (${aiService.getCliModeLabel()})`
    : aiService.getCliModeLabel();
  const stats = memoryService.getStats();
  const sessions = terminalService.getAllSessions();

  const text = `ðŸ©º *Bot Health*\n\nCurrent CLI: \`${aiService.getCLI()}\`\nCurrent model: \`${modelDisplayName(aiService.getDefaultModel())}\`\nAI backend: ${backendLabel}\nSessions: ${sessions.length}\nMemory: ${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB\nTracked commands: ${stats.totalCommands}\nTracked projects: ${stats.totalProjects}\nUptime: ${stats.uptime}`;
  await telegramBot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
});

telegramBot.onText(/\/current/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const current = buildCurrentContext(msg.chat.id.toString());
  const text = [
    '📍 *Current Context*',
    '',
    `Project: ${current.projectName ? `\`${current.projectName}\`` : 'None selected'}`,
    `Type: ${current.projectType || 'Unknown'}`,
    `CWD: \`${current.cwd}\``,
    `CLI: \`${current.currentCLI}\``,
    `Model: \`${current.currentModel}\``,
    `AI Status: ${current.aiStatus}`,
    `Token Used: ${current.tokenUsed || 'N/A'}`,
    `Token Remaining: ${current.tokenRemaining || 'N/A'}`,
    `Token Limit: ${current.tokenLimit || 'N/A'}`,
    `Latest Tracked Tokens: ${current.latestTrackedTotalTokens !== null ? current.latestTrackedTotalTokens : 'N/A'}`,
  ].join('\n');

  await telegramBot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
});

telegramBot.onText(/\/test(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, ' You are not authorized to use this bot.');
    return;
  }

  const raw = match?.[1]?.trim() || '';
  const coverage = /\b--coverage\b/.test(raw);
  const pattern = raw.replace(/\b--coverage\b/, '').trim() || undefined;
  const workdir = terminalService.getSession(msg.chat.id.toString())?.cwd || process.cwd();
  tests.setCwd(workdir);
  const result = coverage ? await tests.runCoverage() : await tests.runTests(pattern);
  const framework = tests.detectFramework();

  let text = `*${coverage ? 'Test Coverage' : 'Test Run'}*\n\nFramework: \`${framework}\`\nPath: \`${workdir}\`\nSuccess: ${result.success ? 'Yes' : 'No'}\nDuration: ${result.duration}ms\nPassed: ${result.passed}\nFailed: ${result.failed}\nTotal: ${result.total}`;
  if (result.coverage) {
    text += `\nCoverage: lines=${result.coverage.lines}% statements=${result.coverage.statements}%`;
  }
  if (result.output) {
    text += `\n\n\`\`\`\n${result.output.slice(-1200)}\n\`\`\``;
  }
  await telegramBot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
});

telegramBot.onText(/\/review(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, ' You are not authorized to use this bot.');
    return;
  }

  const target = match?.[1]?.trim() || '';
  const workdir = terminalService.getSession(msg.chat.id.toString())?.cwd || process.cwd();
  review.setCwd(workdir);

  try {
    const result = target ? await review.reviewFile(target) : await review.reviewGitChanges();
    const issues = (result.issues || []).slice(0, 8);
    let text = `ðŸ” *Review*\n\nPath: \`${workdir}\`\nScore: ${result.score}/10\n\n${result.summary || 'No summary'}\n\n*Findings*\n`;
    text += issues.length ? issues.map(issue => `â€¢ [${issue.severity}] ${issue.type}: ${issue.message}${issue.line ? ` (line ${issue.line})` : ''}`).join('\n') : 'No findings.';
    if (result.suggestions?.length) {
      text += `\n\n*Suggestions*\n${result.suggestions.slice(0, 6).map(item => `â€¢ ${item}`).join('\n')}`;
    }
    await telegramBot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
  } catch (error: any) {
    await telegramBot.sendMessage(msg.chat.id, ` Review failed: ${normalizeErrorMessage(error)}`);
  }
});

telegramBot.onText(/\/commit(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, ' You are not authorized to use this bot.');
    return;
  }

  const message = match?.[1]?.trim() || undefined;
  const workdir = terminalService.getSession(msg.chat.id.toString())?.cwd || process.cwd();
  git.setCwd(workdir);

  try {
    const output = message ? (await git.add(), await git.commit(message)) : await git.autoCommit();
    await telegramBot.sendMessage(msg.chat.id, ` *Commit finished*\nPath: \`${workdir}\`\n\n\`\`\`\n${output.slice(-1500)}\n\`\`\``, { parse_mode: 'Markdown' });
  } catch (error: any) {
    await telegramBot.sendMessage(msg.chat.id, ` Commit failed: ${normalizeErrorMessage(error)}`);
  }
});

telegramBot.onText(/\/pushgit(?:\s+(.+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const explicitPath = match?.[1]?.trim();
  const workdir = explicitPath || terminalService.getSession(msg.chat.id.toString())?.cwd || process.cwd();
  git.setCwd(workdir);

  try {
    const output = await git.pushGitAuto();
    await telegramBot.sendMessage(msg.chat.id, `✅ *PushGit finished*\nPath: \`${workdir}\`\n\n\`\`\`\n${output.slice(-1500)}\n\`\`\``, { parse_mode: 'Markdown' });
  } catch (error: any) {
    await telegramBot.sendMessage(msg.chat.id, `❌ PushGit failed: ${normalizeErrorMessage(error)}`);
  }
});

telegramBot.onText(/\/project/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  const parts = text.split(' ');
  const chatId = msg.chat.id.toString();

  let projectName = '';
  let newPath = '';

  for (let i = 1; i < parts.length; i++) {
    if (parts[i] === '--path' && i + 1 < parts.length) {
      newPath = parts[i + 1];
      i++;
    } else {
      projectName += (projectName ? ' ' : '') + parts[i];
    }
  }

  const projects = memoryService.getProjects();

  if (projects.length === 0) {
    await telegramBot.sendMessage(chatId, '📭 No projects found. Run /addFolder in the folder you want to track.');
    return;
  }

  if (projectName) {
    const projectIndex = projects.findIndex(p =>
      p.name.toLowerCase() === projectName.toLowerCase() ||
      p.path === projectName
    );
    const project = projectIndex >= 0 ? projects[projectIndex] : undefined;

    if (!project) {
      await telegramBot.sendMessage(chatId, `❌ Project "${projectName}" not found.`);
      return;
    }

    const pathExists = fs.existsSync(project.path);

    if (!pathExists && newPath) {
      const updated = memoryService.updateProjectPath(project.path, newPath);
      if (!updated) {
        await telegramBot.sendMessage(chatId, `❌ Failed to update project path to: \`${newPath}\``);
        return;
      }
      await telegramBot.sendMessage(chatId, `✅ Updated project path to: \`${newPath}\``);
      return;
    }

    if (!pathExists) {
      await telegramBot.sendMessage(chatId, `⚠️ Path "${project.path}" does not exist.\nPlease provide new path:\n/project ${project.name} --path <new-path>`);
      return;
    }

    if (permissionBrokerService.requiresApproval(project.path)) {
      await telegramBot.sendMessage(chatId, `📁 Selected project: *${project.name}*\nWrite access is required before switching to this project. Approve access and the bot will switch automatically.`, { parse_mode: 'Markdown' });
      await requestTelegramWriteApproval(chatId, msg.from!.id.toString(), project.path);
      return;
    }

    terminalService.changeDirectory(chatId, project.path);
    if (project.preferredCLI) {
      aiService.setCLI(project.preferredCLI as any);
      saveRuntimeState();
    }
    if (project.preferredModel) {
      aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
    }
    memoryService.setProjectAISettings(project.path, {
      cli: aiService.getCLI(),
      model: aiService.getDefaultModel(),
    });

    const mdFiles = memoryService.getProjectMarkdownFiles(project.path);
    memoryService.updateProjectReadme(project.path);

    let mdSummary = '';
    if (mdFiles.length > 0) {
      mdSummary = '\n📄 *Docs found:*\n';
      for (const f of mdFiles) {
        mdSummary += `• ${f.filename} (${f.size} bytes)\n`;
      }
    }

const agentsContext = memoryService.generateAgentsMdForAI(project.path);
    projectContexts.set(chatId, { content: agentsContext, timestamp: Date.now(), used: false });
    saveRuntimeState();
    memoryService.updateProjectContextLoaded(project.path);

const cli = aiService.getCLI();
    const projectManageButtons = [
      [{ text: '🤖 CLI', callback_data: `projcli_${projectIndex}` }, { text: '📦 Model', callback_data: `projmodel_${projectIndex}` }],
      [{ text: '🗑️ Delete', callback_data: `projdel_${projectIndex}` }],
    ];

    await telegramBot.sendMessage(chatId, `
✅ Project: ${project.name}
📁 ${project.path}
Type: ${project.type}${project.applicationId ? '\n📱 ' + project.applicationId : ''}${mdSummary}

🤖 CLI: ${cli} | Model: ${aiService.getDefaultModel()}

Next: choose CLI -> model, then run /ai <prompt> to pick session and chat.
    `.trim(), {
      parse_mode: 'Markdown',
      reply_markup: {
        inline_keyboard: projectManageButtons,
      },
});
    return;
  }

  let response = '📁 *Projects*\n\n';
  
  for (let i = 0; i < projects.length; i++) {
    const p = projects[i];
    const exists = fs.existsSync(p.path);
    const emoji = exists ? '✅' : '⚠️';
    const typeEmoji = p.type === 'android' ? '🤖' : p.type === 'node' ? '📦' : p.type === 'python' ? '🐍' : '📁';
    
    response += `${i + 1}. ${typeEmoji} *${p.name}*\n`;
    response += `   ${emoji} \`${p.path}\`\n`;
    if (p.applicationId) response += `   📱 ${p.applicationId}\n`;
    if (p.preferredCLI || p.preferredModel) response += `   🤖 ${p.preferredCLI || aiService.getCLI()} | ${p.preferredModel || aiService.getDefaultModel()}\n`;
    response += `   📊 ${p.commands.length} commands | Last: ${new Date(p.lastActivity).toLocaleDateString()}\n\n`;
  }

  response += '\n💡 *Usage:*\n';
  response += '• /project <name> - Select project\n';
  response += '• /project <name> --path <new-path> - Update path\n';
  response += '• /addFolder - Add current folder as project\n';
  response += '• /delProject [name|path] - Delete tracked project';

  await telegramBot.sendMessage(chatId, response, {
    parse_mode: 'Markdown',
    reply_markup: {
      inline_keyboard: projects.slice(0, 10).map((project, index) => ([{
        text: project.name,
        callback_data: `projectpick_${index}`,
      }])),
    },
  });
});

telegramBot.onText(/\/addfolder\b/i, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const session = terminalService.getOrCreateSession(chatId);
  const currentPath = session.cwd;

  if (!fs.existsSync(currentPath) || !fs.statSync(currentPath).isDirectory()) {
    await telegramBot.sendMessage(chatId, `❌ Current path is invalid: \`${currentPath}\``, { parse_mode: 'Markdown' });
    return;
  }

  const result = memoryService.addProject(currentPath, '/addFolder');
  const project = result.project;
  const status = result.created ? '✅ Added project' : 'ℹ️ Project already tracked (updated activity)';

  await telegramBot.sendMessage(chatId, `${status}\n\n*${project.name}*\n\`${project.path}\`\nType: ${project.type}${project.applicationId ? `\nApp ID: \`${project.applicationId}\`` : ''}`, {
    parse_mode: 'Markdown',
  });
});

telegramBot.onText(/\/(?:delproject|deleteproject|removeproject)(?:\s+(.+))?/i, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const session = terminalService.getOrCreateSession(chatId);
  const target = match?.[1]?.trim() || session.cwd;
  const deleted = memoryService.removeProject(target);

  if (!deleted) {
    await telegramBot.sendMessage(chatId, `❌ Project not found: \`${target}\`\nUsage: /delProject [name|path]`, { parse_mode: 'Markdown' });
    return;
  }

  if (session.cwd === deleted.path) {
    projectContexts.delete(chatId);
    saveRuntimeState();
  }

  await telegramBot.sendMessage(chatId, `✅ Deleted project: *${deleted.name}*\n\`${deleted.path}\``, { parse_mode: 'Markdown' });
});

telegramBot.onText(/\/memory/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await telegramBot.sendMessage(msg.chat.id, '❌ You are not authorized to use this bot.');
    return;
  }

  const text = msg.text || '';
  const parts = text.split(' ');
  const chatId = msg.chat.id.toString();
  const type = parts[1] || 'stats';

  if (!parts[1]) {
    await telegramBot.sendMessage(chatId, '*Memory*\n\nChoose a view:', {
      parse_mode: 'Markdown',
      reply_markup: {
        inline_keyboard: [
          [{ text: 'Stats', callback_data: 'memorypick_stats' }, { text: 'Projects', callback_data: 'memorypick_projects' }],
          [{ text: 'Commands', callback_data: 'memorypick_commands' }, { text: 'Clear', callback_data: 'memorypick_clear' }],
        ],
      },
    });
    return;
  }

  if (type === 'clear') {
    memoryService.clearHistory();
    await telegramBot.sendMessage(chatId, '🗑️ Memory history cleared.');
    return;
  }

  if (type === 'projects') {
    const projects = memoryService.getProjects();
    if (projects.length === 0) {
      await telegramBot.sendMessage(chatId, '📭 No projects tracked.');
      return;
    }

    let response = '📁 *Projects*\n\n';
    for (const p of projects) {
      const exists = fs.existsSync(p.path);
      response += `• ${exists ? '✅' : '⚠️'} *${p.name}*\n`;
      response += `  \`${p.path}\`\n`;
      response += `  ${p.commands.length} commands | ${p.type}\n\n`;
    }
    await telegramBot.sendMessage(chatId, response, { parse_mode: 'Markdown' });
    return;
  }

  if (type === 'commands') {
    const commands = memoryService.getRecentCommands(20);
    if (commands.length === 0) {
      await telegramBot.sendMessage(chatId, '📭 No commands logged.');
      return;
    }

    let response = '📜 *Recent Commands*\n\n';
    for (let i = 0; i < commands.length; i++) {
      const c = commands[i];
      const status = c.exitCode === 0 ? '✅' : c.exitCode === null ? '⏳' : '❌';
      response += `${i + 1}. ${status} \`${c.command}\`\n`;
      response += `   📁 ${c.cwd}\n`;
      response += `   ⏱️ ${c.duration}ms | ${new Date(c.timestamp).toLocaleTimeString()}\n\n`;
    }
    await telegramBot.sendMessage(chatId, response, { parse_mode: 'Markdown' });
    return;
  }

  const stats = memoryService.getStats();
  const projects = memoryService.getProjects();
  const commands = memoryService.getRecentCommands(5);

  await telegramBot.sendMessage(chatId, `
🧠 *Bot Memory*

📊 *Stats:*
• Total Commands: ${stats.totalCommands}
• Total Projects: ${stats.totalProjects}
• Uptime: ${stats.uptime}

📁 *Projects:* ${projects.length}
📜 *Recent:* ${commands.length} commands

💡 /memory projects - View all projects
💡 /memory commands - View recent commands
  `.trim(), { parse_mode: 'Markdown' });
});

telegramBot.on('callback_query', async (query: CallbackQuery) => {
  const chatId = query.message?.chat.id.toString();
  const userId = query.from.id;
  const data = query.data;

  if (!chatId || !data) return;

  if (!isAuthorized(userId)) {
    await telegramBot.answerCallbackQuery(query.id, { text: '❌ Unauthorized' });
    return;
  }

  const parts = data.split('_');
  const action = parts[0];
  const channelId = parts.slice(1).join('_');

  if (action === 'modelpick') {
    if (aiService.getCLI() !== 'opencode') {
      await telegramBot.answerCallbackQuery(query.id, { text: 'OpenCode-only picker' });
      await telegramBot.sendMessage(query.message!.chat.id, ` This model picker belongs to OpenCode. Current CLI: \`${aiService.getCLI()}\`. Use /model or /models again.`, {
        parse_mode: 'Markdown',
      });
      return;
    }

    const selectedModel = data.slice('modelpick_'.length);
    const detailedModels = await aiService.listAvailableModelsDetailed();
    const model = detailedModels.find((item: any) => item.key === selectedModel);
    if (!model) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Model not supported' });
      return;
    }

    if (model.status && model.status !== 'active') {
      await telegramBot.answerCallbackQuery(query.id, { text: `Model unavailable: ${model.status}` });
      return;
    }

    if (model.variants.length > 0) {
      const keyboard = model.variants.map((variant: string) => [{ text: variant, callback_data: `modelvariant_${selectedModel}__${variant}` }]);
      await telegramBot.answerCallbackQuery(query.id, { text: `Choose variant for ${selectedModel}` });
      await telegramBot.sendMessage(query.message!.chat.id, `Choose variant for \`${selectedModel}\``, {
        parse_mode: 'Markdown',
        reply_markup: { inline_keyboard: keyboard },
      });
      return;
    }

    aiService.setDefaultModel(selectedModel, aiService.getCLI());
    memoryService.setDefaultModel(selectedModel, aiService.getCLI());
    await telegramBot.answerCallbackQuery(query.id, { text: `Model set: ${selectedModel}` });
    await telegramBot.sendMessage(query.message!.chat.id, `✅ Default model set to: \`${selectedModel}\``, {
      parse_mode: 'Markdown',
    });
    return;
  }

if (action === 'projectpick') {
    const index = Number(data.slice('projectpick_'.length));
    const projects = memoryService.getProjects();
    const project = projects[index];
    if (!project) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Project not found' });
      return;
    }

    if (permissionBrokerService.requiresApproval(project.path)) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Write approval required' });
      await telegramBot.sendMessage(query.message!.chat.id, `📁 Selected project: *${project.name}*\nWrite access is required before switching to this project. Approve access and the bot will switch automatically.`, {
        parse_mode: 'Markdown',
      });
      await requestTelegramWriteApproval(chatId, userId.toString(), project.path);
      return;
    }

    terminalService.changeDirectory(chatId, project.path);
    if (project.preferredCLI) {
      aiService.setCLI(project.preferredCLI as any);
      saveRuntimeState();
    }
    if (project.preferredModel) {
      aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
    }
    memoryService.setProjectAISettings(project.path, {
      cli: aiService.getCLI(),
      model: aiService.getDefaultModel(),
    });

    const selectedCLI = project.preferredCLI || aiService.getCLI();
    const clis: Array<'opencode' | 'claude' | 'codex'> = ['opencode', 'claude', 'codex'];
    const cliButtons = clis.map((cli) => ([{
      text: cli === selectedCLI ? `${cli} ✅` : cli,
      callback_data: `projcliset_${cli}_${index}`,
    }]));

    await telegramBot.answerCallbackQuery(query.id, { text: `Project set: ${project.name}` });
    await telegramBot.editMessageText(`✅ *Project selected*

*${project.name}*
\`${project.path}\`

Step 1/2: choose CLI`, {
      chat_id: query.message!.chat.id,
      message_id: query.message!.message_id,
      parse_mode: 'Markdown',
      reply_markup: {
        inline_keyboard: cliButtons,
      },
    });
    return;
  }

  if (action === 'aisession') {
    const payloadId = data.slice('aisession_'.length);
    const payloadRaw = takeTelegramCallbackPayload(payloadId);
    if (!payloadRaw) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Session action expired. Run /ai again.' });
      return;
    }

    let payload: {
      sessionId: string;
      model: string;
      workdir: string;
      prompt: string;
      cli: 'opencode' | 'claude' | 'codex';
    };
    try {
      payload = JSON.parse(payloadRaw);
    } catch {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Invalid session payload. Run /ai again.' });
      return;
    }
    const sessionId = payload.sessionId;
    const model = payload.model;
    const workdir = payload.workdir || process.cwd();
    const prompt = payload.prompt || '';
    const cli = (payload.cli || aiService.getCLI()) as 'opencode' | 'claude' | 'codex';

    if (!prompt.trim()) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Missing prompt' });
      return;
    }

    recoverFinishedButStuckAIThinking(chatId);
    const activeThinking = getAIThinking(chatId);
    if (activeThinking) {
      const waitSeconds = Math.max(1, Math.floor((Date.now() - activeThinking.startedAt) / 1000));
      await telegramBot.answerCallbackQuery(query.id, { text: `AI busy (${waitSeconds}s)` });
      return;
    }

    const requestId = beginAIThinking(chatId, 'ai');
    if (!requestId) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'AI is still generating' });
      return;
    }

    const cliName = aiService.getCliDisplayName(cli);
    applyLastUsedModelForChat(workdir, cli, model);
    await telegramBot.answerCallbackQuery(query.id, { text: `Using ${sessionId === 'new' ? 'new session' : 'selected session'}` });
    const sentMessage = await telegramBot.sendMessage(chatId, `🤖 *Running ${cliName}*\nModel: \`${model}\``, { parse_mode: 'Markdown' });
    const waitingHeartbeat = startAIWaitingHeartbeat(chatId, sentMessage.message_id, `🤖 *Running ${cliName}*`);
    const startTime = Date.now();

    let lastMsgId: number | null = null;
    const aiStreamState: StreamRenderState = { rawText: '', visibleText: '', lastDeltaNormalized: '', repeatCount: 0, seenNormalizedSegments: new Set() };
    try {
      const response = await aiService.chat(prompt, (chunk) => {
        const rendered = appendVisibleStreamDelta(chunk, aiStreamState);
        if (!rendered) {
          return;
        }
        waitingHeartbeat.markChunkReceived();
        syncTelegramCodeBlockParts(chatId, [sentMessage.message_id, ...(lastMsgId ? [lastMsgId] : [])], rendered, `🤖 *${cliName}*`).then((ids) => {
          lastMsgId = ids[ids.length - 1] || lastMsgId;
        }).catch(() => {});
      }, {
        workdir,
        model,
        cli,
        requestId,
        sessionId: sessionId === 'new' ? undefined : sessionId,
      });

    const duration = Date.now() - startTime;
    const cleanText = response.text.replace(/\x1b\[[0-9;]*m/g, '').trim();
    const renderedText = (response.displayText || response.text).replace(/\x1b\[[0-9;]*m/g, '').trim();
      if (renderedText) {
        const renderedFinal = appendVisibleStreamDelta(renderedText, aiStreamState);
        if (renderedFinal) {
          await syncTelegramCodeBlockParts(chatId, [sentMessage.message_id, ...(lastMsgId ? [lastMsgId] : [])], renderedFinal, `🤖 *${cliName}*`);
        }
      }

      if (response.tokens) {
        memoryService.addAIUsageSnapshot({
          timestamp: new Date().toISOString(),
          cli,
          model,
          mode: 'ai',
          channelId: chatId,
          userId: query.from.id.toString(),
          cwd: workdir,
          inputTokens: response.tokens.input,
          outputTokens: response.tokens.output,
          totalTokens: response.tokens.total,
          cost: response.tokens.cost,
        });
      }

      await sendAIArtifacts(chatId, response, `${cli} response payload`);

      let responseText = `✅ *${cliName} AI finished*\n⏱️ Time: ${duration}ms`;
      if (response.tokens) {
        responseText += `\n📊 Tokens: 📥${response.tokens.input} | 📤${response.tokens.output}`;
      }
      await telegramBot.sendMessage(chatId, responseText, { parse_mode: 'Markdown' });
    } catch (error: any) {
      const errorMessage = normalizeErrorMessage(error);
      const limitNotice = buildLimitNotice(cli, errorMessage) || '';
      await safeTelegramEditMessageText(`❌ *Error*\n\n${errorMessage}${limitNotice}`, {
        chat_id: chatId,
        message_id: sentMessage.message_id,
        parse_mode: 'Markdown',
      });
    } finally {
      waitingHeartbeat.stop();
      endAIThinking(chatId, requestId);
    }
    return;
  }

  if (action === 'aisetsel') {
    const payloadId = data.slice('aisetsel_'.length);
    const payloadRaw = takeTelegramCallbackPayload(payloadId);
    if (!payloadRaw) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Session selection expired. Run /ai again.' });
      return;
    }

    let payload: { cli: 'opencode' | 'claude' | 'codex'; workdir: string; sessionId: string };
    try {
      payload = JSON.parse(payloadRaw);
    } catch {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Invalid session selection.' });
      return;
    }

    let selectedSessionId = payload.sessionId || '';
    if (!selectedSessionId && payload.cli === 'opencode') {
      const created = await aiService.createSessionForCLI('opencode', payload.workdir).catch(() => null);
      if (created) {
        selectedSessionId = created;
      }
    }

    selectedAISessionsByChat.set(chatId, {
      cli: payload.cli,
      workdir: payload.workdir,
      sessionId: selectedSessionId,
    });
    saveRuntimeState();

    await telegramBot.answerCallbackQuery(query.id, {
      text: selectedSessionId ? 'Session selected' : 'New session mode selected',
    });

    await telegramBot.sendMessage(chatId, selectedSessionId
      ? `✅ Session selected for \`${payload.cli}\`\n\`${selectedSessionId}\`\n\nNow use /chat <message> or /ai <prompt>.`
      : `✅ New session mode selected for \`${payload.cli}\`\n\nNext /chat or /ai will create a fresh session.`, {
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'projcli') {
    const projectIndex = Number(data.slice('projcli_'.length));
    const projects = memoryService.getProjects();
    const project = projects[projectIndex];
    if (!project) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Project not found' });
      return;
    }
    
    const clis = ['opencode', 'claude', 'codex'];
    const cliButtons = clis.map(cli => ({ text: cli, callback_data: `projcliset_${cli}_${projectIndex}` }));
    await telegramBot.answerCallbackQuery(query.id, { text: 'Select CLI' });
    await telegramBot.editMessageText(`🤖 *Select CLI for project*\n\nCurrent: \`${project.preferredCLI || aiService.getCLI()}\``, {
      chat_id: query.message!.chat.id,
      message_id: query.message!.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: cliButtons.map(c => [c]) },
    });
    return;
  }

  if (action === 'projcliset') {
    const [cli, projectIndexRaw] = data.slice('projcliset_'.length).split('_');
    const projectIndex = Number(projectIndexRaw);
    const projects = memoryService.getProjects();
    const project = projects[projectIndex];
    if (!project) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Project not found' });
      return;
    }
    if (project) {
      (project as any).preferredCLI = cli;
      memoryService.setProjectAISettings(project.path, { cli, model: aiService.getDefaultModel() });
    }
    aiService.setCLI(cli as 'opencode' | 'claude' | 'codex');
    saveRuntimeState();
    await telegramBot.answerCallbackQuery(query.id, { text: `CLI set to: ${cli}` });

    const currentModel = aiService.getDefaultModel(cli as any);
    const modelPicker = await buildProjectModelKeyboardPage(cli, projectIndex, currentModel, 0);
    await telegramBot.editMessageText(`✅ *CLI set to \`${cli}\`*\n\n📦 *Now select model for this project*\nPage: ${modelPicker.safePage + 1}/${modelPicker.totalPages}`, {
      chat_id: query.message!.chat.id,
      message_id: query.message!.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: modelPicker.rows },
    });
    return;
  }

  if (action === 'projmodel') {
    const projectIndex = Number(data.slice('projmodel_'.length));
    const projects = memoryService.getProjects();
    const project = projects[projectIndex];
    if (!project) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Project not found' });
      return;
    }

    const cli = project.preferredCLI || aiService.getCLI();
    const currentModel = project.preferredModel || aiService.getDefaultModel(cli as any);
    const modelPicker = await buildProjectModelKeyboardPage(cli, projectIndex, currentModel, 0);
    await telegramBot.answerCallbackQuery(query.id, { text: 'Select model' });
    await telegramBot.editMessageText(`📦 *Select model for project*\n\nCLI: \`${cli}\`\nCurrent: \`${modelDisplayName(currentModel)}\`\nPage: ${modelPicker.safePage + 1}/${modelPicker.totalPages}`, {
      chat_id: query.message!.chat.id,
      message_id: query.message!.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: modelPicker.rows },
    });
    return;
  }

  if (action === 'projmodelpage') {
    const [cli, projectIndexRaw, pageRaw] = data.slice('projmodelpage_'.length).split('_');
    const projectIndex = Number(projectIndexRaw);
    const page = Math.max(0, Number(pageRaw) || 0);
    const currentModel = aiService.getDefaultModel(cli as any);
    const modelPicker = await buildProjectModelKeyboardPage(cli, projectIndex, currentModel, page);
    await telegramBot.answerCallbackQuery(query.id);
    await telegramBot.editMessageText(`📦 *Select model for project*\n\nCLI: \`${cli}\`\nCurrent: \`${modelDisplayName(currentModel)}\`\nPage: ${modelPicker.safePage + 1}/${modelPicker.totalPages}`, {
      chat_id: query.message!.chat.id,
      message_id: query.message!.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: modelPicker.rows },
    });
    return;
  }

  if (action === 'projmodelset') {
    const payloadId = data.slice('projmodelset_'.length);
    const payloadRaw = takeTelegramCallbackPayload(payloadId);
    if (!payloadRaw) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Model selection expired' });
      return;
    }
    const payload = JSON.parse(payloadRaw) as { model: string; projectIndex: number; cli: 'opencode' | 'claude' | 'codex' };
    const model = payload.model;
    const projectIndex = Number(payload.projectIndex);
    const selectedCLI = payload.cli;
    const projects = memoryService.getProjects();
    const project = projects[projectIndex];
    const cli = (selectedCLI || aiService.getCLI()) as 'opencode' | 'claude' | 'codex';
    if (project) {
      project.preferredModel = model;
      memoryService.setProjectAISettings(project.path, { cli, model });
    }
    aiService.setCLI(cli);
    aiService.setDefaultModel(model, cli);
    saveRuntimeState();
    await telegramBot.answerCallbackQuery(query.id, { text: `Model set to: ${model}` });
    await telegramBot.editMessageText(`✅ *Model updated*\n\nCLI: \`${cli}\`\nModel: \`${model}\`\n\nNow run /ai <prompt>.`, {
      chat_id: query.message!.chat.id,
      message_id: query.message!.message_id,
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'projdel') {
    const projectIndex = Number(data.slice('projdel_'.length));
    const projects = memoryService.getProjects();
    const project = Number.isInteger(projectIndex) ? projects[projectIndex] : undefined;
    if (project) {
      const deleted = memoryService.removeProject(project.path);
      if (!deleted) {
        await telegramBot.answerCallbackQuery(query.id, { text: 'Project not found' });
        return;
      }

      if (query.message?.chat?.id) {
        const chatId = query.message.chat.id.toString();
        const session = terminalService.getSession(chatId);
        if (session?.cwd === deleted.path) {
          projectContexts.delete(chatId);
          saveRuntimeState();
        }
      }

      await telegramBot.answerCallbackQuery(query.id, { text: 'Project deleted' });
      await telegramBot.editMessageText(`✅ *Deleted project:* ${deleted.name}`, {
        chat_id: query.message!.chat.id,
        message_id: query.message!.message_id,
        parse_mode: 'Markdown',
      });
    } else {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Project not found' });
    }
    return;
  }

  if (action === 'devicepick') {
    const selected = data.slice('devicepick_'.length);
    let deviceId: string | undefined;

    if (selected !== 'auto') {
      const index = Number(selected);
      const devices = androidService.listConnectedDevices().slice(0, 24);
      const device = Number.isInteger(index) ? devices[index] : undefined;
      if (!device) {
        await telegramBot.answerCallbackQuery(query.id, { text: 'Device not found' });
        return;
      }
      deviceId = device.id;
    }

    terminalService.setSelectedDevice(chatId, deviceId);

    await telegramBot.answerCallbackQuery(query.id, {
      text: deviceId ? `Selected: ${deviceId}` : 'Selected: auto-detect',
    });
    await telegramBot.sendMessage(query.message!.chat.id, deviceId
      ? `✅ Selected Android device: \`${deviceId}\``
      : '✅ Selected Android device: auto-detect', {
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'writeapprove') {
    const requestId = data.slice('writeapprove_'.length);
    try {
      const request = permissionBrokerService.approveRequest(requestId);
      const project = memoryService.getProjectByPath(request.rootPath);
      if (project && request.chatId === chatId) {
        terminalService.changeDirectory(chatId, project.path);
        if (project.preferredCLI) {
          aiService.setCLI(project.preferredCLI as any);
          saveRuntimeState();
        }
        if (project.preferredModel) {
          aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
        }
        memoryService.setProjectAISettings(project.path, {
          cli: aiService.getCLI(),
          model: aiService.getDefaultModel(),
        });
        const agentsContext = memoryService.generateAgentsMdForAI(project.path);
        projectContexts.set(chatId, { content: agentsContext, timestamp: Date.now(), used: false });
        saveRuntimeState();
        memoryService.updateProjectContextLoaded(project.path);
      }
      await telegramBot.answerCallbackQuery(query.id, { text: 'Write approved' });
      await telegramBot.sendMessage(query.message!.chat.id, project && request.chatId === chatId
        ? `? *Write approved and project ready*\n\n*${project.name}*\n\`${project.path}\`\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``
        : `? Write broker approved for:\n\`${request.rootPath}\``, {
        parse_mode: 'Markdown',
      });
    } catch (error) {
      const errorMessage = normalizeErrorMessage(error);
      await telegramBot.answerCallbackQuery(query.id, { text: 'Approval failed' });
      await telegramBot.sendMessage(query.message!.chat.id, ` Failed to grant write access:\n\`${errorMessage}\``, {
        parse_mode: 'Markdown',
      });
    }
    return;
  }

  if (action === 'writedeny') {
    const requestId = data.slice('writedeny_'.length);
    const request = permissionBrokerService.denyRequest(requestId);
    await telegramBot.answerCallbackQuery(query.id, { text: 'Write denied' });
    await telegramBot.sendMessage(query.message!.chat.id, request
      ? `âš ï¸ Write access denied for:\n\`${request.rootPath}\``
      : 'âš ï¸ Write request not found.', {
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'memorypick') {
    const selected = data.slice('memorypick_'.length);
    await telegramBot.answerCallbackQuery(query.id);

    if (selected === 'clear') {
      memoryService.clearHistory();
      await telegramBot.sendMessage(query.message!.chat.id, '✅ Memory history cleared.');
      return;
    }

    if (selected === 'projects') {
      const projects = memoryService.getProjects();
      const text = projects.length > 0
        ? '📁 *Projects*\n\n' + projects.slice(0, 12).map(project => `• *${project.name}*\n\`${project.path}\``).join('\n\n')
        : '📭 No projects tracked.';
      await telegramBot.sendMessage(query.message!.chat.id, text, { parse_mode: 'Markdown' });
      return;
    }

    if (selected === 'commands') {
      const commands = memoryService.getRecentCommands(12);
      const text = commands.length > 0
        ? '📜 *Recent Commands*\n\n' + commands.map((cmd, index) => `${index + 1}. \`${cmd.command}\`\n\`${cmd.cwd}\``).join('\n\n')
        : '📭 No commands tracked.';
      await telegramBot.sendMessage(query.message!.chat.id, text, { parse_mode: 'Markdown' });
      return;
    }

    const stats = memoryService.getStats();
    await telegramBot.sendMessage(query.message!.chat.id, `🧠 *Memory Stats*\n\nCommands: ${stats.totalCommands}\nProjects: ${stats.totalProjects}\nUptime: ${stats.uptime}`, {
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'sessionpick') {
    const selectedChannelId = data.slice('sessionpick_'.length);
    const session = terminalService.getSession(selectedChannelId);
    if (!session) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Session not found' });
      await telegramBot.sendMessage(query.message!.chat.id, '📭 Session not found.');
      return;
    }

    terminalService.changeDirectory(chatId, session.cwd);
    terminalService.setSelectedDevice(chatId, session.selectedDeviceId);

    await telegramBot.answerCallbackQuery(query.id, { text: 'Session switched' });
    await telegramBot.sendMessage(query.message!.chat.id, `✅ *Session switched*\n\nSource session: \`${session.channelId}\`\nCWD: \`${session.cwd}\`\nDevice: ${session.selectedDeviceId ? `\`${session.selectedDeviceId}\`` : 'auto-detect'}`, {
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'sessionclearcurrent') {
    const targetChannelId = data.slice('sessionclearcurrent_'.length);
    const existing = terminalService.getSession(targetChannelId);
    await telegramBot.answerCallbackQuery(query.id, { text: existing ? 'Session cleared' : 'Session not found' });
    if (!existing) {
      const clearedAI = clearAIConversationForChat(targetChannelId);
      if (clearedAI.removedInMemory > 0 || clearedAI.removedPersisted > 0) {
        await telegramBot.sendMessage(query.message!.chat.id, `🧹 Session not found, but AI context for \`${targetChannelId}\` was reset (${clearedAI.removedPersisted} saved chat thread${clearedAI.removedPersisted === 1 ? '' : 's'}).`, { parse_mode: 'Markdown' });
      } else {
        await telegramBot.sendMessage(query.message!.chat.id, '📭 Session not found.');
      }
      return;
    }

    terminalService.destroySession(targetChannelId);
    const clearedAI = clearAIConversationForChat(targetChannelId);
    await telegramBot.sendMessage(query.message!.chat.id, `🧹 Cleared session \`${targetChannelId}\` and reset AI context (${clearedAI.removedPersisted} saved chat thread${clearedAI.removedPersisted === 1 ? '' : 's'}).`, { parse_mode: 'Markdown' });
    return;
  }

  if (data === 'sessionclearall_global') {
    const cleared = terminalService.clearAllSessions();
    const clearedAI = clearAllAIConversations();
    await telegramBot.answerCallbackQuery(query.id, { text: 'All sessions cleared' });
    await telegramBot.sendMessage(query.message!.chat.id, `🧹 Cleared ${cleared} terminal session${cleared === 1 ? '' : 's'} and reset AI context (${clearedAI.removedPersisted} saved chat thread${clearedAI.removedPersisted === 1 ? '' : 's'}).`);
    return;
  }

  if (action === 'historyclear') {
    terminalService.clearHistory(chatId);
    await telegramBot.answerCallbackQuery(query.id, { text: 'History cleared' });
    await telegramBot.sendMessage(query.message!.chat.id, '✅ History cleared.');
    return;
  }

  if (action === 'modelpage') {
    if (aiService.getCLI() !== 'opencode') {
      await telegramBot.answerCallbackQuery(query.id, { text: 'OpenCode-only picker' });
      await telegramBot.sendMessage(query.message!.chat.id, ` This model browser belongs to OpenCode. Current CLI: \`${aiService.getCLI()}\`.`, {
        parse_mode: 'Markdown',
      });
      return;
    }

    const payload = data.slice('modelpage_'.length);
    const [providerRaw, pageRaw] = payload.split('__');
    const provider = providerRaw === 'all' ? undefined : providerRaw;
    const page = Math.max(0, Number(pageRaw) || 0);
    await telegramBot.answerCallbackQuery(query.id);
    await sendTelegramModelsPage(query.message!.chat.id, provider, page, query.message?.message_id);
    return;
  }

  if (action === 'modelvariant') {
    if (aiService.getCLI() !== 'opencode') {
      await telegramBot.answerCallbackQuery(query.id, { text: 'OpenCode-only variant' });
      await telegramBot.sendMessage(query.message!.chat.id, ` Variants are only available through OpenCode. Current CLI: \`${aiService.getCLI()}\`.`, {
        parse_mode: 'Markdown',
      });
      return;
    }

    const payload = data.slice('modelvariant_'.length);
    const [selectedModel, variant] = payload.split('__');
    const selection = `${selectedModel}#${variant}`;
    const validation = await aiService.validateModelSelection(selection);
    if (!validation.ok) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Variant not supported' });
      await telegramBot.sendMessage(query.message!.chat.id, `❌ ${validation.error}`);
      return;
    }

    aiService.setDefaultModel(selection, aiService.getCLI());
    memoryService.setDefaultModel(selection, aiService.getCLI());
    const variantCwd = terminalService.getSession(chatId)?.cwd;
    if (variantCwd) {
      memoryService.setProjectAISettingsByCwd(variantCwd, {
        cli: aiService.getCLI(),
        model: selection,
      });
    }
    await telegramBot.answerCallbackQuery(query.id, { text: `Model set: ${selection}` });
    await telegramBot.sendMessage(query.message!.chat.id, `✅ Default model set to: \`${selection}\``, {
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'quickmodel') {
    const selectedModel = data.slice('quickmodel_'.length);
    const validation = await aiService.validateModelSelectionForCurrentCLI(selectedModel);
    if (!validation.ok) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Model not compatible' });
      await telegramBot.sendMessage(query.message!.chat.id, ` ${validation.error}`);
      return;
    }

    const normalizedModel = validation.normalized || selectedModel;
    aiService.setDefaultModel(normalizedModel, aiService.getCLI());
    memoryService.setDefaultModel(normalizedModel, aiService.getCLI());
    const quickModelCwd = terminalService.getSession(chatId)?.cwd;
    if (quickModelCwd) {
      memoryService.setProjectAISettingsByCwd(quickModelCwd, {
        cli: aiService.getCLI(),
        model: normalizedModel,
      });
    }
    await telegramBot.answerCallbackQuery(query.id, { text: `Model set: ${normalizedModel}` });
    await telegramBot.sendMessage(query.message!.chat.id, ` Default model set to: \`${normalizedModel}\``, {
      parse_mode: 'Markdown',
    });
    return;
  }

  if (action === 'quickmodelpage') {
    const payload = data.slice('quickmodelpage_'.length);
    const [cliName, pageRaw] = payload.split('__');
    const page = Math.max(0, Number(pageRaw) || 0);
    await telegramBot.answerCallbackQuery(query.id);
    await safeTelegramEditMessageReplyMarkup({
      inline_keyboard: await buildTelegramQuickModelKeyboardPage(cliName, aiService.getDefaultModel(), page),
    }, {
      chat_id: query.message!.chat.id,
      message_id: query.message!.message_id,
    });
    return;
  }

  if (action === 'clibtn') {
    const selectedCLI = data.slice('clibtn_'.length);
    const validCLIs = ['opencode', 'claude', 'codex'];

    if (!validCLIs.includes(selectedCLI)) {
      await telegramBot.answerCallbackQuery(query.id, { text: 'Unknown CLI' });
      return;
    }

    aiService.setCLI(selectedCLI as any);
    saveRuntimeState();
    const cliCwd = terminalService.getSession(chatId)?.cwd;
    if (cliCwd) {
      memoryService.setProjectAISettingsByCwd(cliCwd, {
        cli: selectedCLI as any,
        model: aiService.getDefaultModel(),
      });
    }
    await telegramBot.answerCallbackQuery(query.id, { text: `CLI set: ${selectedCLI}` });
    await safeTelegramEditMessageText(
      'AI CLI Backend\n\nCurrent: `' + selectedCLI + '`\nCurrent model: `' + modelDisplayName(aiService.getDefaultModel()) + '`\n\nAvailable:\n- `opencode` - OpenCode CLI (`opencode run --format json`)\n- `claude` - Claude Code CLI (`claude --print`)\n- `codex` - OpenAI Codex CLI (`codex exec --json`)\n\nSwitch with: /cli opencode | /cli claude | /cli codex',
      {
        chat_id: query.message!.chat.id,
        message_id: query.message!.message_id,
        parse_mode: 'Markdown',
        reply_markup: {
          inline_keyboard: [
            validCLIs.map((cli) => ({
              text: cli === selectedCLI ? `${cli} ✅` : cli,
              callback_data: `clibtn_${cli}`,
            })),
          ],
        },
      }
    );
    return;
  }

  if (action === 'stop') {
    const processInfo = terminalService.getActiveProcess(channelId);
    const processStopped = !!processInfo;
    const aiStopped = cancelAIThinking(channelId);
    const droppedQueuedChats = clearTelegramChatQueue(channelId);
    if (processInfo) {
      terminalService.killProcess(channelId);
    }

    if (processStopped || aiStopped || droppedQueuedChats > 0) {
      await telegramBot.answerCallbackQuery(query.id, {
        text:
          processStopped && aiStopped
            ? `🛑 Process + AI stopped${droppedQueuedChats > 0 ? `, queue cleared (${droppedQueuedChats})` : ''}`
            : processStopped
              ? `🛑 Process stopped${droppedQueuedChats > 0 ? `, queue cleared (${droppedQueuedChats})` : ''}`
              : aiStopped
                ? `🛑 AI cancelled${droppedQueuedChats > 0 ? `, queue cleared (${droppedQueuedChats})` : ''}`
                : `🧹 Queue cleared (${droppedQueuedChats})`,
      });
      await safeTelegramEditMessageReplyMarkup({ inline_keyboard: [] }, { chat_id: query.message?.chat.id, message_id: query.message?.message_id });
    } else {
      await telegramBot.answerCallbackQuery(query.id, { text: 'No running process or AI request' });
    }
  }
});

telegramBot.on('polling_error', (error) => {
  loggerService.error('Telegram polling error', { error: error.message });
});

process.on('unhandledRejection', (reason) => {
  writeHeartbeat('degraded', { event: 'unhandledRejection' });
  loggerService.error('Unhandled Rejection', { reason: String(reason) });
});

process.on('uncaughtException', (error) => {
  writeHeartbeat('degraded', { event: 'uncaughtException', error: error.message });
  loggerService.error('Uncaught Exception', { error: error.message, stack: error.stack });
  process.exit(1);
});

process.on('SIGINT', async () => {
  writeHeartbeat('stopping', { signal: 'SIGINT' });
  stopHeartbeatLoop();
  stopEventLoopWatchdog();
  loggerService.info('Received SIGINT, shutting down...');
  await aiService.shutdown();
  loggerService.shutdown();
  telegramBot.stopPolling();
  discordClient.destroy();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  writeHeartbeat('stopping', { signal: 'SIGTERM' });
  stopHeartbeatLoop();
  stopEventLoopWatchdog();
  loggerService.info('Received SIGTERM, shutting down...');
  await aiService.shutdown();
  loggerService.shutdown();
  telegramBot.stopPolling();
  discordClient.destroy();
  process.exit(0);
});

loggerService.info('Attempting to login to Discord...');
startHeartbeatLoop();
startEventLoopWatchdog();
const maxRetries = 3;
const retryDelay = 5000;

const attemptLogin = async (attempt: number): Promise<void> => {
  try {
    await discordClient.login(token);
  } catch (error) {
    loggerService.error(`Discord login attempt ${attempt}/${maxRetries} failed`, { 
      error: error instanceof Error ? error.message : String(error) 
    });
    
    if (attempt < maxRetries) {
      loggerService.info(`Retrying Discord login in ${retryDelay / 1000}s...`);
      await new Promise(resolve => setTimeout(resolve, retryDelay));
      return attemptLogin(attempt + 1);
    }
    
    loggerService.error('All Discord login attempts failed', { 
      error: error instanceof Error ? error.message : String(error) 
    });
    process.exit(1);
  }
};

attemptLogin(1);

loggerService.info('Telegram bot is polling...');







