import { spawn, ChildProcess } from 'child_process';
import fs from 'fs';
import https from 'https';
import http from 'http';
import net from 'net';
import { URL } from 'url';
import { EventEmitter } from 'events';
import path from 'path';
import { getOpenCodeLauncher, getProcessEnv } from '../utils';
import { loggerService } from './LoggerService';

export type SupportedCLI = 'opencode' | 'claude' | 'codex';

export interface AIResponse {
  text: string;
  displayText?: string;
  tokens?: TokenInfo;
  done: boolean;
  parts?: any[];
  info?: any;
  events?: any[];
  raw?: any;
}

export interface TokenInfo {
  input: number;
  output: number;
  total: number;
  cost: number;
  quota?: {
    used?: number | string;
    remaining?: number | string;
    limit?: number | string;
    usedPercent?: number | string;
    resetsIn?: string;
  };
}

export interface ModelMetadata {
  key: string;
  providerID: string;
  modelID: string;
  name: string;
  status?: string;
  variants: string[];
  variantConfigs: Record<string, any>;
  options?: Record<string, any>;
  paidPriority: number;
}

type AIRequestOptions = {
  workdir?: string;
  model?: string;
  cli?: SupportedCLI;
  requestId?: string;
  sessionId?: string;
  freshSession?: boolean;
};

type CliDefaultModels = Record<SupportedCLI, string>;

function getPositiveTimeoutMs(envName: string, fallbackMs: number): number {
  const raw = Number(process.env[envName]);
  return Number.isFinite(raw) && raw > 0 ? raw : fallbackMs;
}

class AIService extends EventEmitter {
  private readonly useOpenCodeDirectCLI = true;
  private serverProcess: ChildProcess | null = null;
  private isReady = false;
  private serverPort = process.env.OPENCODE_PORT || '4096';
  private currentModel = '';
  private sessionId: string | null = null;
  private lastSessionTime: number = 0;
  private workdirSessions: Map<string, string> = new Map(); // REMOVED - always query from CLI
  private opencodeWorkdir: string | null = null;
  private initRetries = 0;
  private readonly maxInitRetries = 3;
  private readonly maxRateLimitRetries = 5;
  private readonly retryDelay = 5000;
  private initPromise: Promise<boolean> | null = null;
  private sseConnections: Map<string, http.ClientRequest> = new Map();
  private defaultModels: CliDefaultModels = {
    opencode: 'opencode/big-pickle',
    claude: 'sonnet',
    codex: 'gpt-5.3-codex',
  };
  private currentCLI: SupportedCLI = 'opencode';
  private modelCache: Map<string, ModelMetadata[]> = new Map();
  private readonly requestTimeoutMs = getPositiveTimeoutMs('OPENCODE_REQUEST_TIMEOUT_MS', 240000);
  private readonly initialResponseTimeoutMs = getPositiveTimeoutMs('OPENCODE_INITIAL_RESPONSE_TIMEOUT_MS', 60000);
  private readonly stallTimeoutMs = getPositiveTimeoutMs('OPENCODE_STALL_TIMEOUT_MS', 90000);
  private readonly modelListTimeoutMs = getPositiveTimeoutMs('OPENCODE_MODEL_LIST_TIMEOUT_MS', 15000);
  private activeHttpRequests: Map<string, http.ClientRequest> = new Map();
  private activeCliProcesses: Map<string, ChildProcess> = new Map();
  private cancelledRequests: Set<string> = new Set();
  private codexModelListCache: { fetchedAt: number; models: string[] } | null = null;
  private serverStatusCallback: ((msg: string) => void) | null = null;

  setServerStatusCallback(callback: (msg: string) => void): void {
    this.serverStatusCallback = callback;
  }

  private detectAndEmitServerStatus(msg: string): void {
    console.log('[StatusCheck] msg:', msg.slice(0, 200));
    console.log('[StatusCheck] callback:', !!this.serverStatusCallback);
    if (!this.serverStatusCallback) return;
    
    const lower = msg.toLowerCase();
    const patterns = [
      { pattern: /usage limit|quota.*reached|rate limit/i, emit: '⚠️ Model quota limit reached, waiting...' },
      { pattern: /retrying in \d+s|attempt #\d+/i, emit: '⏳ ' + msg.replace(/\n/g, ' ').slice(0, 100) },
      { pattern: /high traffic|overloaded|server busy/i, emit: '⚠️ Model is under high traffic, waiting...' },
    ];

    for (const { pattern, emit } of patterns) {
      if (pattern.test(lower)) {
        console.log('[StatusCheck] MATCH:', emit);
        this.serverStatusCallback(emit);
        return;
      }
    }
  }

  private readonly openCodeLegacyAliasMap: Record<string, string> = {
    bigpickle: 'opencode/big-pickle',
    'big-pickle': 'opencode/big-pickle',
    minimax: 'opencode/minimax-m2.5-free',
    'minimax-m2.5-free': 'opencode/minimax-m2.5-free',
    nemotron: 'opencode/nemotron-3-super-free',
    'nemotron-3-super-free': 'opencode/nemotron-3-super-free',
  };

  /**
   * Set the default model to use for AI requests
   * @param model Model name (e.g., 'bigpickle', 'minimax', 'nemotron', or provider/model format)
   */
  setDefaultModel(model: string, cli: SupportedCLI = this.currentCLI): void {
    const normalizedModel = cli === 'opencode' ? this.normalizeOpenCodeModelSelection(model) : model;
    this.defaultModels[cli] = normalizedModel;
    loggerService.info(`AI default model set to: ${normalizedModel}`, { cli });
  }

  /**
   * Get the current default model
   * @returns Current default model name
   */
  getDefaultModel(cli: SupportedCLI = this.currentCLI): string {
    const model = this.defaultModels[cli];
    return cli === 'opencode' ? this.normalizeOpenCodeModelSelection(model) : model;
  }

  setDefaultModels(models: Partial<CliDefaultModels>): void {
    this.defaultModels = {
      ...this.defaultModels,
      ...models,
    };
    this.defaultModels.opencode = this.normalizeOpenCodeModelSelection(this.defaultModels.opencode);
  }

  setCLI(cli: SupportedCLI): void {
    this.currentCLI = cli;
    loggerService.info(`AI CLI switched to: ${cli}`);
  }

  resetCLIState(cli: SupportedCLI = this.currentCLI, reason: string = 'manual reset'): void {
    loggerService.warn('Resetting AI CLI state', { cli, reason });

    if (cli === 'opencode') {
      this.sessionId = null;
      this.opencodeWorkdir = null;

      if (!this.useOpenCodeDirectCLI) {
        this.isReady = false;
        this.scheduleReconnect();
      }
    }
  }

  getCLI(): SupportedCLI {
    return this.currentCLI;
  }

  isOpenCodeStarted(): boolean {
    return this.useOpenCodeDirectCLI || this.serverProcess !== null;
  }

  isOpenCodeReady(): boolean {
    return this.useOpenCodeDirectCLI || this.isReady;
  }

  isOpenCodeReadyForWorkdir(workdir?: string): boolean {
    if (this.useOpenCodeDirectCLI) return true;
    if (!this.isReady) return false;
    const targetWorkdir = workdir || process.cwd();
    return this.opencodeWorkdir === null || this.opencodeWorkdir === targetWorkdir;
  }

  usesOpenCodeDirectCLI(): boolean {
    return this.useOpenCodeDirectCLI;
  }

  getStatus(cli: SupportedCLI = this.currentCLI): {
    cli: SupportedCLI;
    started: boolean;
    ready: boolean;
    model: string;
    sessionId: string | null;
  } {
    return {
      cli,
      started: cli === 'opencode' ? this.isOpenCodeStarted() : true,
      ready: cli === 'opencode' ? this.isOpenCodeReady() : true,
      model: this.getDefaultModel(cli),
      sessionId: cli === 'opencode' ? this.sessionId : null,
    };
  }

  resetSession(): void {
    this.sessionId = null;
  }

  private getCliExecutable(cli: 'claude' | 'codex'): string {
    return process.platform === 'win32' ? `${cli}.cmd` : cli;
  }

  private normalizeCodexStderr(stderr: string): { ignored: string[]; unexpected: string[] } {
    const ignoredPatterns = [
      /^Reading prompt from stdin\.\.\.$/i,
    ];

    const ignored: string[] = [];
    const unexpected: string[] = [];

    for (const rawLine of stderr.split(/\r?\n/)) {
      const line = rawLine.trim();
      if (!line) continue;

      if (ignoredPatterns.some((pattern) => pattern.test(line))) {
        ignored.push(line);
        continue;
      }

      unexpected.push(line);
    }

    return { ignored, unexpected };
  }

  private buildCodexDebugMeta(
    prompt: string,
    options: { workdir?: string; model?: string } | undefined,
    code: number | null,
    stderrInfo: { ignored: string[]; unexpected: string[] },
    stdoutBuffer: string
  ): Record<string, unknown> {
    return {
      exitCode: code,
      model: options?.model || this.getDefaultModel(),
      workdir: options?.workdir || process.cwd(),
      ignoredStderr: stderrInfo.ignored,
      unexpectedStderr: stderrInfo.unexpected,
      stdoutPreview: stdoutBuffer.trim().slice(0, 2000),
      promptPreview: prompt.slice(0, 500),
    };
  }

  getCliDisplayName(cli: SupportedCLI = this.currentCLI): string {
    const labels: Record<SupportedCLI, string> = {
      opencode: 'OpenCode',
      claude: 'Claude',
      codex: 'Codex',
    };
    return labels[cli];
  }

  getCliModeLabel(cli: SupportedCLI = this.currentCLI): string {
    return cli === 'opencode' && !this.useOpenCodeDirectCLI ? 'Server mode' : 'Direct CLI';
  }

  getCliRuntimeStatus(cli: SupportedCLI = this.currentCLI): string {
    if (cli !== 'opencode' || this.useOpenCodeDirectCLI) {
      return 'Direct CLI';
    }

    if (this.isOpenCodeReady()) {
      return 'Ready (Server mode)';
    }

    if (this.isOpenCodeStarted()) {
      return 'Starting (Server mode)';
    }

    return 'Stopped (Server mode)';
  }

  hasNativeModelListing(cli: SupportedCLI = this.currentCLI): boolean {
    return cli === 'opencode';
  }

  private normalizeOpenCodeModelSelection(selection: string): string {
    const trimmed = selection.trim();
    if (!trimmed) return trimmed;

    const [rawModelKey, variant] = trimmed.split('#');
    const modelKey = rawModelKey.trim();
    const normalizedModelKey =
      this.openCodeLegacyAliasMap[modelKey] ||
      (modelKey.startsWith('opencode/') ? modelKey : this.openCodeLegacyAliasMap[modelKey.toLowerCase()]) ||
      modelKey;

    return variant ? `${normalizedModelKey}#${variant}` : normalizedModelKey;
  }

  private getCanonicalOpenCodeDefaultModel(): string {
    return this.openCodeLegacyAliasMap.bigpickle;
  }

  getModelExamples(cli: SupportedCLI = this.currentCLI): string[] {
    if (cli === 'codex') {
      return ['gpt-5.3-codex', 'gpt-5.4', 'gpt-4.1'];
    }

    if (cli === 'claude') {
      return ['sonnet', 'opus', 'claude-sonnet-4-20250514'];
    }

    return [
      'openai/gpt-4o',
      'openai/gpt-4.1',
      'anthropic/claude-sonnet-4-20250514',
      'google/gemini-2.0-flash',
    ];
  }

  getSupportedModels(cli: SupportedCLI = this.currentCLI): string[] {
    if (cli === 'codex') {
      return [
        'gpt-5.3-codex',
        'gpt-5.4',
        'gpt-4.1',
      ];
    }

    if (cli === 'claude') {
      return [
        'sonnet',
        'opus',
        'claude-sonnet-4-20250514',
      ];
    }

    return [];
  }

  private shouldIncludeOpenAIModelForCodex(modelId: string): boolean {
    const id = modelId.toLowerCase();
    if (!id) return false;

    const excludedPrefixes = [
      'whisper',
      'tts-',
      'omni-moderation',
      'text-embedding',
      'text-moderation',
      'dall-e',
      'gpt-image',
      'computer-use',
      'realtime',
    ];
    if (excludedPrefixes.some(prefix => id.startsWith(prefix))) {
      return false;
    }

    if (id.includes('transcribe') || id.includes('embedding') || id.includes('moderation') || id.includes('audio')) {
      return false;
    }

    return /^(gpt|o\d|o[1-4]|codex)/.test(id);
  }

  private sortCodexModelIds(modelIds: string[]): string[] {
    const unique = Array.from(new Set(modelIds));
    return unique.sort((a, b) => {
      const score = (value: string) => {
        const id = value.toLowerCase();
        if (id.includes('codex')) return 0;
        if (id.startsWith('gpt-5')) return 1;
        if (id.startsWith('o4')) return 2;
        if (id.startsWith('o3')) return 3;
        if (id.startsWith('gpt-4.1')) return 4;
        if (id.startsWith('gpt-4')) return 5;
        return 10;
      };

      const diff = score(a) - score(b);
      return diff !== 0 ? diff : a.localeCompare(b);
    });
  }

  async listCodexModels(): Promise<string[]> {
    const apiKey = process.env.OPENAI_API_KEY?.trim();
    const now = Date.now();
    if (this.codexModelListCache && (now - this.codexModelListCache.fetchedAt) < 10 * 60 * 1000) {
      return this.codexModelListCache.models;
    }

    if (!apiKey) {
      return this.getSupportedModels('codex');
    }

    const models = await new Promise<string[]>((resolve, reject) => {
      const req = https.request('https://api.openai.com/v1/models', {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${apiKey}`,
          'Content-Type': 'application/json',
        },
      }, (res) => {
        let body = '';
        res.on('data', chunk => {
          body += chunk.toString();
        });
        res.on('end', () => {
          if ((res.statusCode || 500) >= 400) {
            reject(new Error(`Failed to list OpenAI models: HTTP ${res.statusCode}`));
            return;
          }

          try {
            const json = JSON.parse(body);
            const ids = Array.isArray(json?.data)
              ? json.data.map((item: any) => String(item?.id || '')).filter(Boolean)
              : [];
            resolve(this.sortCodexModelIds(ids.filter((id: string) => this.shouldIncludeOpenAIModelForCodex(id))));
          } catch (error) {
            reject(error);
          }
        });
      });

      req.on('error', reject);
      req.end();
    }).catch((error) => {
      loggerService.warn('Falling back to static Codex model suggestions', { error: String(error) });
      return this.getSupportedModels('codex');
    });

    this.codexModelListCache = {
      fetchedAt: now,
      models,
    };
    return models;
  }

  async listModelsForCLI(cli: SupportedCLI = this.currentCLI, provider?: string): Promise<string[]> {
    if (cli === 'opencode') {
      return this.listAvailableModels(provider);
    }

    if (cli === 'codex') {
      return this.listCodexModels();
    }

    return this.getSupportedModels(cli);
  }

  async validateModelSelectionForCLI(cli: SupportedCLI, selection: string): Promise<{ ok: boolean; error?: string; normalized?: string; variant?: string }> {
    const value = selection.trim();
    if (!value) {
      return { ok: false, error: 'Model cannot be empty.' };
    }

    if (cli === 'opencode') {
      const normalizedInput = this.normalizeOpenCodeModelSelection(value);
      if (this.useOpenCodeDirectCLI) {
        const { modelKey, variant } = this.parseModelSelection(normalizedInput);
        if (!modelKey.includes('/')) {
          return { ok: false, error: 'Use an OpenCode model id like `opencode/big-pickle` or a supported alias like `bigpickle`.' };
        }
        return { ok: true, normalized: normalizedInput, variant };
      }
      const validation = await this.validateModelSelection(normalizedInput);
      if (!validation.ok) {
        return { ok: false, error: validation.error };
      }
      return { ok: true, normalized: validation.model?.key || normalizedInput, variant: validation.variant };
    }

    if (cli === 'codex') {
      const knownAliases = new Set(['bigpickle', 'minimax', 'nemotron']);
      const base = value.split('#')[0].trim();
      if (!base) {
        return { ok: false, error: 'Model cannot be empty.' };
      }
      if (knownAliases.has(base) || base.startsWith('opencode/')) {
        return {
          ok: false,
          error: 'Current CLI is `codex`. Use an OpenAI model id like `gpt-5.3-codex` or switch to `/cli opencode`.',
        };
      }

      const normalized = base.startsWith('openai/') ? base.slice('openai/'.length) : base;
      if (normalized.includes('/')) {
        return {
          ok: false,
          error: 'Current CLI is `codex`. Use a Codex/OpenAI model id like `gpt-5.3-codex`.',
        };
      }

      return { ok: true, normalized };
    }

    const base = value.split('#')[0].trim();
    if (!base) {
      return { ok: false, error: 'Model cannot be empty.' };
    }
    if (['bigpickle', 'minimax', 'nemotron'].includes(base) || base.startsWith('opencode/')) {
      return {
        ok: false,
        error: 'Current CLI is `claude`. Use a Claude model like `sonnet` or switch to `/cli opencode`.',
      };
    }

    const normalized = base.startsWith('anthropic/') ? base.slice('anthropic/'.length) : base;
    if (normalized.includes('/')) {
      return {
        ok: false,
        error: 'Current CLI is `claude`. Use a Claude model alias or model id like `claude-sonnet-4-20250514`.',
      };
    }

    return { ok: true, normalized };
  }

  async validateModelSelectionForCurrentCLI(selection: string): Promise<{ ok: boolean; error?: string; normalized?: string; variant?: string }> {
    return this.validateModelSelectionForCLI(this.currentCLI, selection);
  }

  private resolveCliModelOption(cli: SupportedCLI, model?: string): string | undefined {
    const selected = (model || this.getDefaultModel(cli) || '').trim();
    if (!selected) return undefined;

    if (cli === 'opencode') {
      return this.normalizeOpenCodeModelSelection(selected);
    }

    // Claude accepts plain model ids; variants from OpenCode syntax are not valid here.
    if (cli === 'claude') {
      return selected.split('#')[0];
    }

    // Codex CLI expects OpenAI model ids, not OpenCode aliases/provider+variant syntax.
    let normalized = selected;

    if (normalized.includes('#')) {
      normalized = normalized.split('#')[0];
    }

    if (normalized.startsWith('openai/')) {
      normalized = normalized.slice('openai/'.length);
    }

    const knownOpencodeAliases = new Set(['bigpickle', 'minimax', 'nemotron']);
    const knownOpencodeModels = [
      'opencode/',
      'anthropic/',
      'google/',
      'x-ai/',
      'groq/',
      'deepseek/',
      'meta/',
      'mistral/',
      'qwen/',
      'moonshot/',
      'openrouter/',
    ];

    if (knownOpencodeAliases.has(normalized) || knownOpencodeModels.some(prefix => normalized.startsWith(prefix))) {
      loggerService.warn('Ignoring incompatible model for Codex CLI; falling back to Codex default model', {
        requestedModel: selected,
      });
      return undefined;
    }

    return normalized || undefined;
  }

  private safeStringify(value: unknown): string {
    if (typeof value === 'string') return value;
    if (value === null || value === undefined) return '';
    if (value instanceof Error) return value.message || String(value);
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }

  private buildPayloadDebug(payload: any): string {
    try {
      const topKeys = payload && typeof payload === 'object' ? Object.keys(payload) : [];
      const infoKeys = payload?.info && typeof payload.info === 'object' ? Object.keys(payload.info) : [];
      const partsCount = Array.isArray(payload?.parts) ? payload.parts.length : 0;
      const preview = this.safeStringify(payload).slice(0, 1200);
      return `keys=${topKeys.join(',') || 'none'} | infoKeys=${infoKeys.join(',') || 'none'} | parts=${partsCount} | preview=${preview}`;
    } catch (e) {
      return `failed-to-build-payload-debug: ${this.safeStringify(e)}`;
    }
  }

  private collectTextFromParts(parts: any[]): string {
    if (!Array.isArray(parts)) return '';
    let text = '';

    for (const part of parts) {
      if (!part) continue;
      if ((part.type === 'text' || part.type === 'reasoning') && part.text !== undefined) {
        text += this.safeStringify(part.text);
        continue;
      }
      if (part.content !== undefined && (part.type === 'text' || typeof part.content === 'string')) {
        text += this.safeStringify(part.content);
      }
    }

    return text;
  }

  private collectRenderableFromParts(parts: any[]): string {
    if (!Array.isArray(parts) || parts.length === 0) return '';
    const lines: string[] = [];

    for (const part of parts) {
      if (!part) continue;
      const type = part.type || 'part';

      if ((type === 'text' || type === 'reasoning') && part.text !== undefined) {
        const txt = this.safeStringify(part.text).trim();
        if (txt) lines.push(txt);
        continue;
      }

      if (typeof part.content === 'string' && part.content.trim()) {
        lines.push(part.content.trim());
        continue;
      }

      const candidates = [
        part.title,
        part.summary,
        part.description,
        part.command,
        part.name,
        part.path,
        part.filePath,
        part.status,
      ].filter((value) => typeof value === 'string' && value.trim()) as string[];

      if (candidates.length > 0) {
        lines.push(`[${type}] ${candidates[0]}`);
      }
    }

    if (lines.length === 0) return '';
    return lines.join('\n');
  }

  private collapseDuplicateRenderableText(text: string, seenBlocks: Set<string>): string {
    const blocks = (text || '').replace(/\r\n/g, '\n').split(/\n{2,}/);
    const output: string[] = [];

    for (const block of blocks) {
      const trimmedBlock = block.trim();
      if (!trimmedBlock) continue;

      const normalizedBlock = trimmedBlock.replace(/\s+/g, ' ');
      if (seenBlocks.has(normalizedBlock)) continue;
      seenBlocks.add(normalizedBlock);

      const lines = trimmedBlock.split('\n');
      const dedupedLines: string[] = [];
      const seenLines = new Set<string>();

      for (const line of lines) {
        const normalizedLine = line.replace(/\s+/g, ' ').trim();
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
  }

  private renderOpenCodePart(part: any): string {
    if (!part || typeof part !== 'object') return '';

    const type = String(part.type || 'part').toLowerCase();
    if (type === 'text' || type === 'reasoning') {
      return '';
    }

    const decodeHtmlEntities = (value: string): string => value
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&nbsp;/g, ' ');

    const htmlToText = (value: string): string => decodeHtmlEntities(value)
      .replace(/<\s*br\s*\/?>/gi, '\n')
      .replace(/<\s*\/p\s*>/gi, '\n\n')
      .replace(/<\s*p\b[^>]*>/gi, '')
      .replace(/<\s*\/div\s*>/gi, '\n')
      .replace(/<\s*div\b[^>]*>/gi, '')
      .replace(/<[^>]+>/g, '')
      .trim();

    const parseTextPayload = (value: unknown): any | null => {
      if (typeof value !== 'string') return null;
      const trimmed = value.trim();
      if (!trimmed) return null;
      const cleaned = /<\w+[^>]*>/.test(trimmed) ? htmlToText(trimmed) : decodeHtmlEntities(trimmed);
      const candidate = cleaned.trim();
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

      const wrapped = extractJsonSnippet(candidate) || (candidate.startsWith('{') || candidate.startsWith('[') ? candidate : `{${candidate}}`);
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
          if (depth === 0) {
            return { start, end: i + 1, snippet: input.slice(start, i + 1) };
          }
        }
      }

      return null;
    };

    const compactText = (value: unknown): string => {
      if (value === null || value === undefined) return '';
      if (typeof value === 'string') {
        const parsed = parseTextPayload(value);
        if (parsed) return compactText(parsed);
        return htmlToText(value)
          .replace(/\r\n/g, '\n')
          .replace(/[ \t]+\n/g, '\n')
          .replace(/\n{3,}/g, '\n\n')
          .trim();
      }
      if (typeof value === 'number' || typeof value === 'boolean') return String(value);
      if (Array.isArray(value)) {
        return value.map((item) => compactText(item)).filter(Boolean).join('\n');
      }
      if (typeof value === 'object') {
        const obj = value as Record<string, unknown>;
        const preferred = [
          obj.patchText, obj.patch, obj.diff, obj.output, obj.result, obj.content,
          obj.text, obj.message, obj.preview, obj.stdout, obj.stderr,
        ];
        for (const item of preferred) {
          const text = compactText(item);
          if (text) return text;
        }
        return '';
      }
      return String(value);
    };

    const summarizeDataObject = (value: any): string => {
      if (!value || typeof value !== 'object') return '';
      const lines: string[] = [];
      const metadata = value.metadata && typeof value.metadata === 'object'
        ? value.metadata
        : value.state?.metadata && typeof value.state.metadata === 'object'
          ? value.state.metadata
          : {};
      const fileEntries = [value.files, metadata.files].find((item) => Array.isArray(item) && item.length > 0) as any[] | undefined;
      const firstFile = fileEntries?.find((item) => item && typeof item === 'object');
      const filePath = [
        value.filePath,
        value.path,
        value.relativePath,
        firstFile?.relativePath,
        firstFile?.filePath,
        firstFile?.path,
        value.state?.input?.filePath,
        value.state?.input?.path,
      ].find((item) => typeof item === 'string' && item.trim()) as string | undefined;
      const title = [
        value.title,
        value.state?.title,
        value.summary,
        value.description,
        value.state?.input?.description,
        metadata.title,
        metadata.description,
        value.name,
      ].find((item) => typeof item === 'string' && item.trim()) as string | undefined;
      const additions = value.additions ?? value.state?.additions ?? firstFile?.additions;
      const deletions = value.deletions ?? value.state?.deletions ?? firstFile?.deletions;
      const tool = [value.tool, value.name, value.type, value.state?.tool].find((item) => typeof item === 'string' && item.trim()) as string | undefined;
      const status = [value.state?.status, value.status, value.result?.status].find((item) => typeof item === 'string' && item.trim()) as string | undefined;
      const command = [value.command, value.state?.input?.command, value.input?.command].find((item) => typeof item === 'string' && item.trim()) as string | undefined;

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
          const entryPath = [entry.relativePath, entry.filePath, entry.path].find((item) => typeof item === 'string' && item.trim()) as string | undefined;
          const entryAdditions = entry.additions;
          const entryDeletions = entry.deletions;
          const entryDiff = compactText(entry.patch ?? entry.diff ?? entry.patchText);
          const block: string[] = [];
          if (entryPath) block.push(`File: ${entryPath}`);
          if (Number.isFinite(Number(entryAdditions)) || Number.isFinite(Number(entryDeletions))) {
            block.push(`Changes: +${Number(entryAdditions) || 0} / -${Number(entryDeletions) || 0}`);
          }
          if (entryDiff) {
            block.push('Diff:');
            block.push('```diff');
            block.push(entryDiff.trim());
            block.push('```');
          }
          return block.join('\n');
        }).filter(Boolean);

        if (fileBlocks.length > 0) {
          lines.push(fileBlocks.join('\n\n'));
        }
      }

      const patchText = compactText(
        value.patchText ??
        value.patch ??
        value.diff ??
        value.state?.input?.patchText ??
        value.state?.patchText ??
        value.state?.patch ??
        value.state?.diff ??
        metadata.diff ??
        metadata.patch ??
        firstFile?.patch ??
        firstFile?.diff
      );
      const outputText = compactText(
        value.output ??
        value.result ??
        value.state?.output ??
        value.state?.result ??
        value.preview ??
        metadata.preview ??
        metadata.output
      );
      const payloadText = patchText || outputText;

      if (payloadText) {
        const isDiff = /^diff\s|^Index:/mi.test(payloadText) || /(^|\n)[+-].+/m.test(payloadText) || /@@\s/.test(payloadText);
        if (isDiff) {
          lines.push('Diff:');
          lines.push('```diff');
          lines.push(payloadText.trim());
          lines.push('```');
        } else {
          const preview = payloadText.split(/\r?\n/).filter(Boolean).slice(0, 30).join('\n');
          lines.push('Output:');
          lines.push(preview);
        }
      }

      return lines.join('\n').trim();
    };

    const renderLooseTextSegment = (value: string): string => {
      const cleaned = htmlToText(value).trim();
      if (!cleaned) return '';
      const isDiff = /^diff\s|^Index:/mi.test(cleaned) || /(^|\n)[+-].+/m.test(cleaned) || /@@\s/.test(cleaned);
      if (isDiff) {
        return `\`\`\`diff\n${cleaned}\n\`\`\``;
      }
      return cleaned;
    };

    const renderMixedString = (value: string): string => {
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
          pushSection(renderLooseTextSegment(remaining));
          break;
        }

        const before = remaining.slice(0, snippet.start);
        pushSection(renderLooseTextSegment(before));

        const parsed = parseTextPayload(snippet.snippet);
        if (parsed && typeof parsed === 'object') {
          pushSection(summarizeDataObject(parsed));
        }

        remaining = remaining.slice(snippet.end);
      }

      return sections.filter(Boolean).join('\n\n').trim();
    };

    const structuredText = parseTextPayload(part.content) || parseTextPayload(part.output) || parseTextPayload(part.details) || parseTextPayload(part.diff) || parseTextPayload(part.patch);
    if (structuredText && typeof structuredText === 'object') {
      const summary = summarizeDataObject(structuredText);
      if (summary) return summary;
    }

    const filePath = [part.filePath, part.path].find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    const beforeValue = [part.before, part.previous, part.old, part.oldContent, part.original, part.from, part.beforeText, part.beforeCode, part.source]
      .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    const afterValue = [part.after, part.next, part.new, part.newContent, part.updated, part.to, part.afterText, part.afterCode, part.target]
      .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    const fenceLanguage = (() => {
      const source = filePath || part.title || part.summary || '';
      const match = String(source).toLowerCase().match(/\.([a-z0-9]+)$/);
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
        md: 'md',
        yaml: 'yaml',
        yml: 'yaml',
        py: 'python',
      };
      return map[ext] || 'text';
    })();

    if (beforeValue !== undefined || afterValue !== undefined) {
      const beforeText = this.collapseDuplicateRenderableText(beforeValue || '', new Set()).trim();
      const afterText = this.collapseDuplicateRenderableText(afterValue || '', new Set()).trim();
      if (beforeText || afterText) {
        const blocks: string[] = [];
        const title = [part.title, part.summary, part.description, part.command, part.name, part.status]
          .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
        if (title) blocks.push(`[${type}] ${title.trim()}`);
        if (filePath) blocks.push(`file: ${filePath}`);
        if (beforeText) {
          blocks.push('Before:');
          blocks.push('```' + fenceLanguage);
          blocks.push(beforeText);
          blocks.push('```');
        }
        if (afterText) {
          blocks.push('After:');
          blocks.push('```' + fenceLanguage);
          blocks.push(afterText);
          blocks.push('```');
        }
        return blocks.join('\n');
      }
    }

    const diffText = [part.diff, part.patch, part.change, part.changes, part.code, part.content]
      .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    if (diffText && (/^diff\s|^[+-]{1}\s|^@@\s|^Index:/mi.test(diffText) || type.includes('diff') || type.includes('patch') || type.includes('edit'))) {
      return `\`\`\`diff\n${diffText.trim()}\n\`\`\``;
    }

    if (type === 'step-start' || type === 'step_start') {
      const summary = [part.title, part.summary, part.command, part.name, part.path, part.filePath]
        .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
      return summary ? `⚙️ Processing... ${summary.trim()}` : '⚙️ Processing...';
    }

    if (type === 'step-finish' || type === 'step_finish') {
      return '';
    }

    const structuredRaw = summarizeDataObject(part);
    if (structuredRaw) return structuredRaw;

    const lines: string[] = [];
    const primary = [part.title, part.summary, part.description, part.command, part.name, part.path, part.filePath, part.status, part.action, part.label]
      .find((value) => typeof value === 'string' && value.trim()) as string | undefined;
    if (primary) {
      lines.push(`[${type}] ${primary.trim()}`);
    }

    const stringFields = [part.content, part.output, part.stdout, part.stderr, part.diff, part.patch, part.code, part.details];
    for (const field of stringFields) {
      if (typeof field === 'string' && field.trim()) {
        const rendered = renderMixedString(field);
        if (rendered) lines.push(rendered);
      }
    }

    const listFields = [part.files, part.changes, part.issues, part.tools, part.actions];
    for (const field of listFields) {
      if (Array.isArray(field) && field.length > 0) {
        const listed = field
          .map((item) => {
            if (typeof item === 'string') return item.trim();
            if (item && typeof item === 'object') {
              return [item.filePath, item.path, item.title, item.summary, item.name, item.status]
                .find((value) => typeof value === 'string' && value.trim()) || '';
            }
            return '';
          })
          .filter(Boolean)
          .join('\n');
        if (listed) lines.push(listed);
      }
    }

    return lines.filter(Boolean).join('\n\n').trim();
  }

  private renderOpenCodeCompletionFooter(finishReason: string, tokenInfo?: TokenInfo): string {
    const lines = ['✅ Step finished'];
    if (finishReason && finishReason !== 'unknown') {
      lines.push(`reason: ${finishReason}`);
    }
    if (tokenInfo) {
      lines.push(`tokens: in=${tokenInfo.input ?? 'n/a'} out=${tokenInfo.output ?? 'n/a'} total=${tokenInfo.total ?? 'n/a'}`);
    }
    return lines.join('\n');
  }

  private summarizeProcessingUpdates(parts: any[]): string {
    if (!Array.isArray(parts) || parts.length === 0) {
      return 'waiting for updates';
    }

    const labels: string[] = [];
    const maxLabels = 3;

    for (const part of parts) {
      if (labels.length >= maxLabels) break;
      if (!part || typeof part !== 'object') continue;

      const type = String(part.type || 'update');
      const status = typeof part.status === 'string' ? part.status.trim() : '';
      const primary = [part.title, part.summary, part.command, part.name, part.path, part.filePath]
        .find((value) => typeof value === 'string' && value.trim()) as string | undefined;

      let line = type;
      if (status) line += `:${status}`;
      if (primary) line += ` ${primary}`;

      const normalized = line.replace(/\s+/g, ' ').trim();
      if (normalized && !labels.includes(normalized)) {
        labels.push(normalized.slice(0, 80));
      }
    }

    if (labels.length === 0) {
      return `${parts.length} update(s) in progress`;
    }

    return labels.join(' | ');
  }

  private formatViDateTime(value: any): string {
    const timestamp = Number(value);
    if (!Number.isFinite(timestamp) || timestamp <= 0) return 'N/A';
    return new Date(timestamp).toLocaleString('vi-VN');
  }

  private extractAssistantText(payload: any): string {
    if (!payload || typeof payload !== 'object') return '';

    const directCandidates = [
      payload.text,
      payload.content,
      payload.output_text,
      payload.response,
      payload.message,
      payload.info?.text,
    ];

    for (const candidate of directCandidates) {
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate;
      }
    }

    const partCandidates = [
      payload.parts,
      payload.message?.parts,
      payload.response?.parts,
      payload.output?.parts,
      payload.data?.parts,
      payload.messages?.[payload.messages.length - 1]?.parts,
    ];

    for (const parts of partCandidates) {
      const fromParts = this.collectTextFromParts(parts);
      if (fromParts.trim()) return fromParts;
    }

    const nestedTextCandidates = [
      payload.message?.content,
      payload.response?.text,
      payload.output?.text,
      payload.data?.text,
      // OpenAI Codex / completions format
      payload.choices?.[0]?.text,
      payload.choices?.[0]?.message?.content,
      payload.completions?.[0]?.text,
      payload.candidates?.[0]?.text,
      payload.candidates?.[0]?.content?.parts?.[0]?.text,
      payload.results?.[0]?.text,
      payload.outputs?.[0]?.text,
    ];

    for (const candidate of nestedTextCandidates) {
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate;
      }
    }

    // Try choices array (multiple candidates)
    if (Array.isArray(payload.choices)) {
      for (const choice of payload.choices) {
        const t = choice?.text || choice?.message?.content || choice?.content;
        if (typeof t === 'string' && t.trim()) return t;
      }
    }

    return '';
  }

  private extractTextFromSSE(buffer: string): { text: string; tokenInfo?: TokenInfo; events: any[] } {
    let text = '';
    let tokenInfo: TokenInfo | undefined;
    const events: any[] = [];
    const lines = buffer.split('\n');

    for (const line of lines) {
      if (!line.startsWith('data:')) continue;
      const data = line.slice(5).trim();
      if (!data || data === '[DONE]') continue;
      try {
        const event = JSON.parse(data);
        events.push(event);
        // Codex / OpenAI streaming delta
        const delta = event.choices?.[0]?.delta?.content ?? event.choices?.[0]?.text ?? '';
        if (typeof delta === 'string' && delta) {
          text += delta;
          continue;
        }
        // OpenCode SSE parts
        if (Array.isArray(event.parts)) {
          for (const part of event.parts) {
            if ((part.type === 'text' || part.type === 'reasoning') && part.text) {
              text += this.safeStringify(part.text);
            }
            if (part.tokens) {
              tokenInfo = {
                input: part.tokens.input || 0,
                output: part.tokens.output || 0,
                total: part.tokens.total || 0,
                cost: part.cost || 0,
              };
            }
          }
        }
        // Generic text field in event
        const evText = event.text || event.content || event.delta?.text || event.delta?.content;
        if (typeof evText === 'string' && evText) text += evText;

        // Event-bus style payloads (message.part.updated)
        const partText = event.part?.text
          || event.part?.content
          || event.data?.part?.text
          || event.data?.part?.content
          || event.message?.part?.text
          || event.message?.part?.content;
        if (typeof partText === 'string' && partText) text += partText;
      } catch {}
    }

    return { text, tokenInfo, events };
  }

  private extractQuotaFromResponse(json: any, text: string): TokenInfo['quota'] | null {
    try {
      if (json.quota) {
        return {
          used: json.quota.used || json.quota.usedTokens,
          remaining: json.quota.remaining || json.quota.remainingTokens,
          limit: json.quota.limit || json.quota.total,
          usedPercent: json.quota.utilization || json.quota.usedPercent,
          resetsIn: json.quota.resetsAt || json.quota.resets_in,
        };
      }

      if (json.usage) {
        const u = json.usage;
        return {
          used: u.tokensUsed || u.tokens_used || u.used,
          remaining: u.tokensRemaining || u.tokens_remaining || u.remaining,
          limit: u.limit || u.total,
          usedPercent: u.utilization || u.usedPercent,
          resetsIn: u.resetsAt || u.resets_at,
        };
      }

      if (json.credits) {
        const c = json.credits;
        return {
          used: c.spent || c.used,
          remaining: c.balance || c.remaining,
          limit: c.limit || c.total,
          usedPercent: c.utilization || c.usedPercent,
        };
      }

      const textLower = text.toLowerCase();
      const quotaMatch = text.match(/(?:remaining|left|balance|quota)[:\s]*(\d+(?:[.,]\d+)?[kmg]?)/i);
      if (quotaMatch && (textLower.includes('remaining') || textLower.includes('left') || textLower.includes('balance'))) {
        return { remaining: quotaMatch[1] };
      }

      return null;
    } catch {
      return null;
    }
  }

  private normalizeProviderError(message: unknown, statusCode?: number): string {
    const normalized = this.safeStringify(message);
    const text = normalized.toLowerCase();

    if (text.includes('providermodelnotfounderror') || text.includes('model not found') || text.includes('unknown model')) {
      return 'Model not found or wrong model id. Check `/models` and choose an available model.';
    }

    if (statusCode === 429 || text.includes('rate limit') || text.includes('rate-limited') || text.includes('too many requests')) {
      return 'Model is rate limited right now. Try again later or switch model.';
    }

    if (text.includes('high traffic') || text.includes('high-traffic') || text.includes('overloaded') || text.includes('capacity') || text.includes('server busy')) {
      return 'Model is under high traffic right now. Try again later or switch model.';
    }

    if (statusCode === 503 || text.includes('unavailable') || text.includes('temporarily unavailable') || text.includes('provider unavailable')) {
      return 'Model provider is unavailable right now. Try again later or switch model.';
    }

    if (statusCode === 402 || text.includes('quota') || text.includes('credit') || text.includes('billing') || text.includes('insufficient')) {
      return 'Model is unavailable for this account due to quota or billing limits.';
    }

    if (text.includes('timeout')) {
      return 'Model request timed out. Try again later or switch model.';
    }

    return normalized || 'Unknown provider error';
  }

  private parseVerboseModels(stdout: string): ModelMetadata[] {
    const models: ModelMetadata[] = [];
    const lines = stdout.split(/\r?\n/);
    let i = 0;

    while (i < lines.length) {
      const idLine = lines[i]?.trim();
      if (!idLine || !idLine.includes('/')) {
        i++;
        continue;
      }

      i++;
      while (i < lines.length && !lines[i].trim()) i++;
      if (i >= lines.length || !lines[i].trim().startsWith('{')) {
        continue;
      }

      let jsonText = '';
      let depth = 0;
      let started = false;

      while (i < lines.length) {
        const line = lines[i];
        jsonText += line + '\n';
        for (const ch of line) {
          if (ch === '{') {
            depth++;
            started = true;
          }
          if (ch === '}') depth--;
        }
        i++;
        if (started && depth === 0) break;
      }

      try {
        const parsed = JSON.parse(jsonText);
        const providerID = parsed.providerID || idLine.split('/')[0];
        const modelID = parsed.id || idLine.split('/').slice(1).join('/');
        const variants = Object.keys(parsed.variants || {});
        const isFree = idLine.includes('free') || providerID === 'opencode';
        const isPriority = parsed.options?.serviceTier === 'priority';
        const paidPriority = isPriority ? 3 : isFree ? 1 : 2;

        models.push({
          key: idLine,
          providerID,
          modelID,
          name: parsed.name || idLine,
          status: parsed.status,
          variants,
          variantConfigs: parsed.variants || {},
          options: parsed.options || {},
          paidPriority,
        });
      } catch (error) {
        loggerService.warn('Failed to parse verbose model metadata', { idLine, error: String(error) });
      }
    }

    return models.sort((a, b) => {
      if (b.paidPriority !== a.paidPriority) return b.paidPriority - a.paidPriority;
      return a.key.localeCompare(b.key);
    });
  }

  async listAvailableModelsDetailed(provider?: string): Promise<ModelMetadata[]> {
    const cacheKey = provider || 'all';
    const cached = this.modelCache.get(cacheKey);
    if (cached && cached.length > 0) {
      return cached;
    }

    return new Promise((resolve, reject) => {
      const launcher = getOpenCodeLauncher();
      const proc = spawn(launcher.command, [...launcher.args, 'models', ...(provider ? [provider] : []), '--verbose'], {
        cwd: process.cwd(),
        windowsHide: true,
      });
      let settled = false;
      const startedAt = Date.now();

      let stdout = '';
      let stderr = '';

      const timer = setTimeout(() => {
        if (settled) return;
        settled = true;
        try {
          proc.kill();
        } catch {}
        reject(new Error(`Timed out while listing OpenCode models after ${this.modelListTimeoutMs}ms`));
      }, this.modelListTimeoutMs);

      proc.stdout?.on('data', (data) => {
        stdout += data.toString();
      });

      proc.stderr?.on('data', (data) => {
        stderr += data.toString();
      });

      proc.on('close', (code) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        if (code !== 0) {
          reject(new Error(stderr || `Failed to list models (${code})`));
          return;
        }

        const models = this.parseVerboseModels(stdout);
        const duration = Date.now() - startedAt;
        if (duration > 5000) {
          loggerService.warn('OpenCode model listing was slow', { duration, provider: provider || 'all' });
        }
        this.modelCache.set(cacheKey, models);
        resolve(models);
      });

      proc.on('error', (error) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        reject(error);
      });
    });
  }

  private buildOpenCodeStatelessChatPrompt(
    messages: Array<{ role: 'user' | 'assistant'; content: string }>,
    newMessage: string,
  ): string {
    const recentMessages = messages
      .filter((message) => message && typeof message.content === 'string' && message.content.trim())
      .slice(-8)
      .map((message) => `${message.role === 'assistant' ? 'Assistant' : 'User'}: ${message.content.trim()}`);

    if (recentMessages.length === 0) {
      return newMessage;
    }

    return `${recentMessages.join('\n\n')}\n\nUser: ${newMessage}`;
  }

  private async runOpenCodeCliCapture(
    args: string[],
    options?: { workdir?: string; timeoutMs?: number }
  ): Promise<{ stdout: string; stderr: string; code: number | null }> {
    const targetWorkdir = this.resolveExistingOpenCodeWorkdir(options?.workdir);
    const timeoutMs = options?.timeoutMs || this.requestTimeoutMs;

    return new Promise((resolve, reject) => {
      const launcher = getOpenCodeLauncher();
      const proc = spawn(launcher.command, [...launcher.args, ...args], {
        cwd: targetWorkdir,
        env: getProcessEnv({
          HOME: process.env.HOME || targetWorkdir,
          USERPROFILE: process.env.USERPROFILE || targetWorkdir,
        } as any),
        shell: false,
        stdio: ['ignore', 'pipe', 'pipe'],
        windowsHide: true,
      });

      let stdout = '';
      let stderr = '';
      let settled = false;

      const timer = setTimeout(() => {
        if (settled) return;
        settled = true;
        try {
          proc.kill();
        } catch {}
        reject(new Error(`OpenCode CLI timed out after ${timeoutMs}ms`));
      }, timeoutMs);

      proc.stdout?.on('data', (data) => {
        stdout += data.toString();
      });

      proc.stderr?.on('data', (data) => {
        stderr += data.toString();
      });

      proc.on('close', (code) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        resolve({ stdout, stderr, code });
      });

      proc.on('error', (error) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        reject(error);
      });
    });
  }

  private parseOpenCodeSessionList(stdout: string): Array<{ id: string; title: string; updated: string }> {
    return stdout
      .split(/\r?\n/)
      .map((line) => line.replace(/\x1b\[[0-9;]*m/g, '').trimEnd())
      .filter((line) => line.startsWith('ses_'))
      .map((line) => {
        const match = line.match(/^(ses_[^\s]+)\s{2,}(.+?)\s{2,}(.+)$/);
        if (!match) return null;
        return {
          id: match[1],
          title: match[2].trim(),
          updated: match[3].trim(),
        };
      })
      .filter((item): item is { id: string; title: string; updated: string } => item !== null);
  }

  private async getOpenCodeSessionInfo(sessionId: string): Promise<any | null> {
    try {
      const result = await this.runOpenCodeCliCapture(['export', sessionId], { timeoutMs: 10000 });
      if (result.code !== 0 || !result.stdout.trim()) {
        return null;
      }

      const parsed = JSON.parse(result.stdout);
      return parsed?.info || null;
    } catch {
      return null;
    }
  }

  private async listOpenCodeSessionsFromCli(workdir: string): Promise<Array<{ id: string; title: string; updated: string }>> {
    try {
      const targetWorkdir = this.resolveExistingOpenCodeWorkdir(workdir);
      const normalizedWorkdir = path.resolve(targetWorkdir).toLowerCase();
      const result = await this.runOpenCodeCliCapture(['session', 'list'], {
        workdir: targetWorkdir,
        timeoutMs: 10000,
      });

      if (result.code !== 0 || !result.stdout.trim()) {
        return [];
      }

      const recentSessions = this.parseOpenCodeSessionList(result.stdout).slice(0, 24);
      const matches: Array<{ id: string; title: string; updated: string }> = [];

      for (const session of recentSessions) {
        const info = await this.getOpenCodeSessionInfo(session.id);
        const sessionDir = typeof info?.directory === 'string' ? path.resolve(info.directory).toLowerCase() : null;
        if (!info || sessionDir !== normalizedWorkdir) {
          continue;
        }

        const updated = this.formatViDateTime(info.time?.updated || info.time?.created);

        matches.push({
          id: session.id,
          title: info.title || session.title || 'Untitled',
          updated: updated === 'N/A' ? session.updated : updated,
        });

        if (matches.length >= 12) {
          break;
        }
      }

      return matches;
    } catch (error) {
      loggerService.warn('Failed to list OpenCode CLI sessions', { workdir, error: String(error) });
      return [];
    }
  }

  async listAvailableModels(provider?: string): Promise<string[]> {
    const detailed = await this.listAvailableModelsDetailed(provider);
    return detailed.map(item => item.key);
  }

  parseModelSelection(selection: string): { modelKey: string; variant?: string } {
    const normalized = this.normalizeOpenCodeModelSelection(selection);
    const [modelKey, variant] = normalized.split('#');
    return { modelKey, variant };
  }

  async validateModelSelection(selection: string): Promise<{ ok: boolean; error?: string; model?: ModelMetadata; variant?: string }> {
    if (this.currentCLI !== 'opencode') {
      return { ok: true };
    }

    const { modelKey, variant } = this.parseModelSelection(selection);
    const models = await this.listAvailableModelsDetailed();
    const model = models.find(item => item.key === modelKey);

    if (!model) {
      return { ok: false, error: `Model not supported: ${modelKey}` };
    }

    if (model.status && model.status !== 'active') {
      return { ok: false, error: `Model is not available right now: ${modelKey} (status: ${model.status})` };
    }

    if (variant && !model.variants.includes(variant)) {
      return {
        ok: false,
        error: `Variant \`${variant}\` is not supported for ${modelKey}. Available: ${model.variants.join(', ') || 'none'}`,
      };
    }

    return { ok: true, model, variant };
  }

  async initialize(workdir?: string): Promise<boolean> {
    if (this.initPromise) {
      return this.initPromise;
    }

    this.initPromise = this.doInitialize(workdir);
    return this.initPromise;
  }

  async ensureReadyForCLI(cli: SupportedCLI, workdir?: string): Promise<boolean> {
    if (cli !== 'opencode') {
      return true;
    }

    if (this.useOpenCodeDirectCLI) {
      this.opencodeWorkdir = this.resolveExistingOpenCodeWorkdir(workdir);
      return true;
    }

    const requestedWorkdir = this.resolveExistingOpenCodeWorkdir(workdir);

    if (this.opencodeWorkdir && this.opencodeWorkdir !== requestedWorkdir) {
      loggerService.info('Restarting OpenCode for a new project workdir', {
        previousWorkdir: this.opencodeWorkdir,
        nextWorkdir: requestedWorkdir,
      });
      this.isReady = false;
      this.sessionId = null;
      this.initPromise = null;

      if (this.serverProcess) {
        try {
          this.serverProcess.kill();
        } catch {}
        this.serverProcess = null;
      }
    }

    if (this.isReady) {
      if (this.serverProcess && !this.serverProcess.killed) {
        this.healthCheck().then(healthy => {
          if (!healthy) {
            loggerService.warn('OpenCode server health check failed in background');
          }
        }).catch(() => {});
        return true;
      }
      
      const healthy = await this.healthCheck().catch(() => false);
      if (healthy) {
        return true;
      }

      loggerService.warn('OpenCode server was marked ready but failed health check; restarting it');
      this.isReady = false;
      this.sessionId = null;
      this.initPromise = null;

      if (this.serverProcess) {
        try {
          this.serverProcess.kill();
        } catch {}
        this.serverProcess = null;
      }
    }

    return this.initialize(requestedWorkdir);
  }

  private resolveExistingOpenCodeWorkdir(workdir?: string): string {
    const requestedWorkdir = workdir || process.cwd();
    if (fs.existsSync(requestedWorkdir)) {
      return requestedWorkdir;
    }

    const fallbackWorkdir = process.cwd();
    loggerService.warn('OpenCode workdir does not exist, falling back to process cwd', {
      requestedWorkdir,
      fallbackWorkdir,
    });
    return fallbackWorkdir;
  }

  private async doInitialize(workdir?: string): Promise<boolean> {
    const targetWorkdir = this.resolveExistingOpenCodeWorkdir(workdir);
    try {
      const resolvedPort = await this.resolveOpenCodePort();
      if (resolvedPort !== this.serverPort) {
        loggerService.warn('OpenCode port is busy, switching to fallback port', {
          previousPort: this.serverPort,
          nextPort: resolvedPort,
        });
      }
      this.serverPort = resolvedPort;
      console.log(`🔄 Starting OpenCode server on port ${this.serverPort}...`);
      
      if (this.serverProcess) {
        try {
          this.serverProcess.kill();
        } catch {}
      }
      
      const launcher = getOpenCodeLauncher();
      this.serverProcess = spawn(launcher.command, [...launcher.args, 'serve', '--port', this.serverPort], {
        cwd: targetWorkdir,
        windowsHide: false,
        detached: false,
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      this.opencodeWorkdir = targetWorkdir;

      this.serverProcess.stdout?.on('data', (data) => {
        const msg = data.toString();
        console.log('[OpenCode]', msg);
        this.detectAndEmitServerStatus(msg);
        if (msg.includes(`localhost:${this.serverPort}`) || msg.includes(`127.0.0.1:${this.serverPort}`)) {
          this.isReady = true;
          this.createSession(targetWorkdir);
        }
      });

      this.serverProcess.stderr?.on('data', (data) => {
        console.log('[OpenCode Error]', data.toString());
        this.detectAndEmitServerStatus(data.toString());
      });

      this.serverProcess.on('exit', (code, signal) => {
        console.log(`OpenCode server exited with code ${code}, signal ${signal}`);
        this.isReady = false;
        this.sessionId = null;
        this.scheduleReconnect();
      });

      this.serverProcess.on('error', (err) => {
        console.error('OpenCode server process error:', err.message);
        this.isReady = false;
        this.scheduleReconnect();
      });

      await this.waitForServer(10000);
      this.initRetries = 0;
      console.log(`✅ OpenCode server ready`);
      return true;
    } catch (error: any) {
      console.error('❌ Failed to start OpenCode:', error.message);
      this.initRetries++;
      
      if (this.initRetries < this.maxInitRetries) {
        console.log(`🔄 Retrying in ${this.retryDelay / 1000}s (attempt ${this.initRetries}/${this.maxInitRetries})...`);
        await this.sleep(this.retryDelay);
        return this.doInitialize(targetWorkdir);
      }
      
      return false;
    }
  }

  private scheduleReconnect(): void {
    if (this.initRetries >= this.maxInitRetries) {
      console.log('Max retries reached, skipping reconnection');
      return;
    }
    
    setTimeout(() => {
      console.log('Attempting to reconnect to OpenCode server...');
      this.initPromise = null;
      this.initialize(this.opencodeWorkdir || process.cwd()).catch(err => {
        console.error('Reconnection failed:', err.message);
      });
    }, this.retryDelay);
  }

  private async createSession(workdir?: string, fresh?: boolean): Promise<string | null> {
    if (this.useOpenCodeDirectCLI) {
      return null;
    }
    return this.doCreateSession(workdir);
  }

  async createSessionForCLI(cli: SupportedCLI, workdir?: string): Promise<string | null> {
    if (cli !== 'opencode') {
      return null;
    }
    if (this.useOpenCodeDirectCLI) {
      this.opencodeWorkdir = this.resolveExistingOpenCodeWorkdir(workdir);
      return null;
    }
    const ready = await this.ensureReadyForCLI('opencode', workdir);
    if (!ready) {
      return null;
    }
    return this.createSession(workdir, true);
  }

  private async doCreateSession(workdir?: string): Promise<string | null> {
    return new Promise((resolve) => {
      const postData = JSON.stringify({});
      
      const req = http.request({
        hostname: '127.0.0.1',
        port: this.serverPort,
        path: '/session',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData),
        },
      }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const json = JSON.parse(data);
            if (json.id) {
              this.sessionId = json.id;
              console.log(`📝 Session created: ${this.sessionId}`);
              resolve(json.id);
              return;
            }
          } catch {}
          resolve(null);
        });
      });
      
      req.on('error', () => resolve(null));
      req.write(postData);
      req.end();
    });
  }

  private async waitForServer(timeout: number): Promise<void> {
    const start = Date.now();
    console.log('Waiting for OpenCode server...');
    while (Date.now() - start < timeout) {
      try {
        const healthy = await this.healthCheck();
        if (healthy) {
          console.log('Server is healthy!');
          return;
        }
      } catch {}
      await this.sleep(1000);
    }
    throw new Error('Server timeout');
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private async isPortAvailable(port: number): Promise<boolean> {
    return new Promise((resolve) => {
      const server = net.createServer();
      server.once('error', () => resolve(false));
      server.once('listening', () => {
        server.close(() => resolve(true));
      });
      server.listen(port, '127.0.0.1');
    });
  }

  private async resolveOpenCodePort(): Promise<string> {
    const preferred = Number(this.serverPort) || 4096;
    const firstChoices = [preferred, preferred + 1];
    if (Math.random() >= 0.5) {
      firstChoices.reverse();
    }

    for (const candidate of firstChoices) {
      if (await this.isPortAvailable(candidate)) {
        return String(candidate);
      }
    }

    for (let offset = 2; offset < 10; offset++) {
      const candidate = preferred + offset;
      if (await this.isPortAvailable(candidate)) {
        return String(candidate);
      }
    }
    return String(preferred);
  }

  private registerHttpRequest(requestId: string | undefined, req: http.ClientRequest): void {
    if (!requestId) return;
    this.activeHttpRequests.set(requestId, req);
  }

  private clearHttpRequest(requestId: string | undefined, req?: http.ClientRequest): void {
    if (!requestId) return;
    const current = this.activeHttpRequests.get(requestId);
    if (!current) return;
    if (!req || current === req) {
      this.activeHttpRequests.delete(requestId);
    }
  }

  private registerCliProcess(requestId: string | undefined, proc: ChildProcess): void {
    if (!requestId) return;
    this.activeCliProcesses.set(requestId, proc);
  }

  private clearCliProcess(requestId: string | undefined, proc?: ChildProcess): void {
    if (!requestId) return;
    const current = this.activeCliProcesses.get(requestId);
    if (!current) return;
    if (!proc || current === proc) {
      this.activeCliProcesses.delete(requestId);
    }
  }

  private markRequestStarted(requestId: string | undefined): void {
    if (!requestId) return;
    this.cancelledRequests.delete(requestId);
  }

  private markRequestFinished(requestId: string | undefined): void {
    if (!requestId) return;
    this.cancelledRequests.delete(requestId);
  }

  private isRequestCancelled(requestId: string | undefined): boolean {
    if (!requestId) return false;
    return this.cancelledRequests.has(requestId);
  }

  cancelRequest(requestId: string): boolean {
    let cancelled = false;
    this.cancelledRequests.add(requestId);

    const req = this.activeHttpRequests.get(requestId);
    if (req) {
      this.activeHttpRequests.delete(requestId);
      try {
        req.destroy(new Error('Request cancelled by user.'));
      } catch {
        req.destroy();
      }
      cancelled = true;
    }

    const proc = this.activeCliProcesses.get(requestId);
    if (proc) {
      this.activeCliProcesses.delete(requestId);
      try {
        if (process.platform === 'win32' && proc.pid) {
          spawn('taskkill', ['/pid', proc.pid.toString(), '/f', '/t']);
        } else {
          proc.kill('SIGTERM');
          setTimeout(() => {
            if (!proc.killed) {
              try {
                proc.kill('SIGKILL');
              } catch {}
            }
          }, 3000);
        }
      } catch {}
      cancelled = true;
    }

    return cancelled || this.cancelledRequests.has(requestId);
  }

  isRequestActive(requestId: string): boolean {
    return this.activeHttpRequests.has(requestId) || this.activeCliProcesses.has(requestId);
  }

  async findOpenCodeSession(workdir: string): Promise<string | null> {
    if (this.useOpenCodeDirectCLI) {
      const sessions = await this.listOpenCodeSessionsFromCli(workdir);
      return sessions[0]?.id || null;
    }

    return new Promise((resolve) => {
      const req = http.request({
        hostname: '127.0.0.1',
        port: this.serverPort,
        path: '/session',
        method: 'GET',
      }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const sessions = JSON.parse(data);
            const match = sessions.find((s: any) => s.directory === workdir);
            resolve(match?.id || null);
          } catch {
            resolve(null);
          }
        });
      });
      req.on('error', () => resolve(null));
      req.end();
    });
  }

  async findOpenCodeSessions(workdir?: string): Promise<string[]> {
    if (this.useOpenCodeDirectCLI) {
      if (!workdir) {
        const result = await this.runOpenCodeCliCapture(['session', 'list'], { timeoutMs: 10000 }).catch(() => null);
        return result ? this.parseOpenCodeSessionList(result.stdout).map((session) => session.id) : [];
      }

      const sessions = await this.listOpenCodeSessionsFromCli(workdir);
      return sessions.map((session) => session.id);
    }

    return new Promise((resolve) => {
      const req = http.request({
        hostname: '127.0.0.1',
        port: this.serverPort,
        path: '/session',
        method: 'GET',
      }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const sessions = JSON.parse(data);
            if (workdir) {
              const matches = sessions.filter((s: any) => s.directory === workdir).map((s: any) => s.id);
              resolve(matches);
            } else {
              resolve(sessions.map((s: any) => s.id));
            }
          } catch {
            resolve([]);
          }
        });
      });
      req.on('error', () => resolve([]));
      req.end();
    });
  }

  async listSessionsForCLI(cli: string, workdir: string): Promise<{id: string, title: string, updated: string}[]> {
    if (cli === 'opencode') {
      if (this.useOpenCodeDirectCLI) {
        return this.listOpenCodeSessionsFromCli(workdir);
      }

      return new Promise((resolve) => {
        const req = http.request({
          hostname: '127.0.0.1',
          port: this.serverPort,
          path: '/session',
          method: 'GET',
        }, (res) => {
          let data = '';
          res.on('data', chunk => data += chunk);
          res.on('end', async () => {
            try {
              const sessions = JSON.parse(data);
              const matches = sessions.filter((s: any) => s.directory === workdir);
              resolve(matches.map((s: any) => ({
                id: s.id,
                title: s.title || s.slug || 'Untitled',
                updated: this.formatViDateTime(s.time?.updated || s.time?.created)
              })));
            } catch {
              resolve([]);
            }
          });
        });
        req.on('error', () => resolve([]));
        req.end();
      });
    }
    
    return new Promise((resolve) => {
      const { exec } = require('child_process');
      const cmd = cli === 'claude' ? 'claude sessions list --json' : 'codex sessions list --json';
      exec(cmd, { timeout: 10000 }, (err: any, stdout: string, stderr: string) => {
        try {
          const sessions = JSON.parse(stdout || '[]');
          const matches = sessions.filter((s: any) => s.directory === workdir || s.path === workdir);
          resolve(matches.map((s: any) => ({
            id: s.id || s.session_id,
            title: s.title || 'Untitled',
            updated: this.formatViDateTime(s.updated || s.created)
          })));
        } catch {
          resolve([]);
        }
      });
    });
  }

  async findClaudeSession(workdir: string): Promise<string | null> {
    return new Promise((resolve) => {
      const { exec } = require('child_process');
      exec('claude sessions list --json', { cwd: workdir, timeout: 10000 }, (err: any, stdout: string, stderr: string) => {
        try {
          const sessions = JSON.parse(stdout || '[]');
          const match = sessions.find((s: any) => s.directory === workdir || s.path === workdir);
          resolve(match?.id || match?.session_id || null);
        } catch {
          resolve(null);
        }
      });
    });
  }

  async findCodexSession(workdir: string): Promise<string | null> {
    return new Promise((resolve) => {
      const { exec } = require('child_process');
      exec('codex sessions list --json', { cwd: workdir, timeout: 10000 }, (err: any, stdout: string, stderr: string) => {
        try {
          const sessions = JSON.parse(stdout || '[]');
          const match = sessions.find((s: any) => s.directory === workdir || s.path === workdir);
          resolve(match?.id || match?.session_id || null);
        } catch {
          resolve(null);
        }
      });
    });
  }

  async deleteSession(sessionId: string): Promise<boolean> {
    if (this.useOpenCodeDirectCLI) {
      try {
        const result = await this.runOpenCodeCliCapture(['session', 'delete', sessionId], { timeoutMs: 10000 });
        return result.code === 0;
      } catch {
        return false;
      }
    }

    return new Promise((resolve) => {
      const req = http.request({
        hostname: '127.0.0.1',
        port: this.serverPort,
        path: `/session/${sessionId}`,
        method: 'DELETE',
      }, (res) => {
        resolve(res.statusCode === 200 || res.statusCode === 204);
      });
      req.on('error', () => resolve(false));
      req.end();
    });
  }

  async healthCheck(): Promise<boolean> {
    if (this.useOpenCodeDirectCLI) {
      try {
        const result = await this.runOpenCodeCliCapture(['stats'], { timeoutMs: 10000 });
        return result.code === 0;
      } catch {
        return false;
      }
    }

    return new Promise((resolve) => {
      const timeout = setTimeout(() => resolve(false), 5000);
      http.get(`http://127.0.0.1:${this.serverPort}/global/health`, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          clearTimeout(timeout);
          try {
            const json = JSON.parse(data);
            resolve(json.healthy === true);
          } catch {
            resolve(false);
          }
        });
      }).on('error', () => {
        clearTimeout(timeout);
        resolve(false);
      });
    });
  }

  private async fetchSessionMessages(sessionId: string): Promise<any | null> {
    return new Promise((resolve) => {
      const req = http.request({
        hostname: '127.0.0.1',
        port: this.serverPort,
        path: `/session/${sessionId}/message`,
        method: 'GET',
      }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch {
            resolve(null);
          }
        });
      });
      req.on('error', () => resolve(null));
      req.end();
    });
  }

  private extractLatestAssistantSnapshot(payload: any, minCreatedMs: number): { text: string; done: boolean; parts?: any[]; raw?: any } {
    const list = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.messages) ? payload.messages : (Array.isArray(payload?.items) ? payload.items : []));

    const assistant = [...list].reverse().find((item: any) => {
      const role = item?.role || item?.message?.role || item?.info?.role;
      const created = Number(item?.time?.created || item?.info?.time?.created || 0);
      return role === 'assistant' && created >= minCreatedMs;
    }) || null;

    const parts = assistant?.parts || assistant?.message?.parts || payload?.parts;
    const text = this.collectTextFromParts(parts || [])
      || this.collectRenderableFromParts(parts || [])
      || this.extractAssistantText(assistant)
      || this.extractAssistantText(payload)
      || '';

    const done = Boolean(
      assistant?.finish === 'stop'
      || assistant?.status === 'done'
      || assistant?.status === 'completed'
      || assistant?.time?.completed
      || payload?.finish === 'stop'
      || payload?.status === 'done'
      || payload?.status === 'completed'
    );

    return { text, done, parts: Array.isArray(parts) ? parts : undefined, raw: assistant || payload };
  }

  private async pollAssistantMessageUntilDone(
    sessionId: string,
    onChunk: (text: string) => void,
    timeoutMs: number,
    minCreatedMs: number,
    requestId?: string,
  ): Promise<{ text: string; parts?: any[]; raw?: any }> {
    let inactivityDeadline = Date.now() + timeoutMs;
    let latestText = '';
    let latestParts: any[] | undefined;
    let latestRaw: any;

    let lastProgressSignature = '';
    let lastActivitySignature = '';

    while (Date.now() < inactivityDeadline) {
      if (this.isRequestCancelled(requestId)) {
        throw new Error('Request cancelled by user.');
      }
      const payload = await this.fetchSessionMessages(sessionId);
      if (payload) {
        const snap = this.extractLatestAssistantSnapshot(payload, minCreatedMs);
        const activitySignature = JSON.stringify({
          done: snap.done,
          textLen: (snap.text || '').length,
          partCount: Array.isArray(snap.parts) ? snap.parts.length : 0,
          partTypes: Array.isArray(snap.parts) ? snap.parts.map((p: any) => p?.type || 'part') : [],
          rawId: snap.raw?.id || snap.raw?.messageID || snap.raw?.info?.id || null,
          rawUpdated: snap.raw?.time?.updated || snap.raw?.info?.time?.updated || null,
        });
        if (activitySignature !== lastActivitySignature) {
          lastActivitySignature = activitySignature;
          inactivityDeadline = Date.now() + timeoutMs;
        }
        if (snap.text && snap.text !== latestText) {
          latestText = snap.text;
          onChunk(latestText);
        }
        if (!snap.text && Array.isArray(snap.parts) && snap.parts.length > 0) {
          const signature = `${snap.parts.length}:${snap.parts.map((p: any) => p?.type || 'part').join(',')}`;
          if (signature !== lastProgressSignature) {
            lastProgressSignature = signature;
            const detail = this.summarizeProcessingUpdates(snap.parts);
            onChunk(`⚙️ Processing... ${detail}`);
          }
        }
        if (snap.parts) latestParts = snap.parts;
        if (snap.raw) latestRaw = snap.raw;
        if (snap.done) {
          if (!latestText.trim()) {
            latestText = '✅ Completed.';
          }
          return { text: latestText, parts: latestParts, raw: latestRaw };
        }
      }
      await this.sleep(700);
    }

    if (latestText.trim()) {
      return { text: latestText, parts: latestParts, raw: latestRaw };
    }
    throw new Error('Timed out while waiting for streamed assistant output from OpenCode.');
  }

  private async chatOpenCode(
    prompt: string,
    onChunk: (text: string) => void,
    options?: AIRequestOptions
  ): Promise<AIResponse> {
    return new Promise((resolve, reject) => {
      const targetWorkdir = this.resolveExistingOpenCodeWorkdir(options?.workdir);
      const launcher = getOpenCodeLauncher();
      const { modelKey, variant } = this.parseModelSelection(options?.model || this.getDefaultModel('opencode'));
      const normalizedModel = modelKey && modelKey.includes('/') ? modelKey : this.getCanonicalOpenCodeDefaultModel();
      const [providerID, ...modelParts] = normalizedModel.split('/');
      const actualModelID = modelParts.join('/') || normalizedModel;
      const args = [
        ...launcher.args,
        'run',
        prompt,
        '--format',
        'json',
        '--dir',
        targetWorkdir,
        '--model',
        normalizedModel,
      ];

      if (variant) {
        args.push('--variant', variant);
      }

      if (options?.sessionId) {
        args.push('--session', options.sessionId);
      }

      const proc = spawn(launcher.command, args, {
        cwd: targetWorkdir,
        env: getProcessEnv({
          HOME: process.env.HOME || targetWorkdir,
          USERPROFILE: process.env.USERPROFILE || targetWorkdir,
        } as any),
        shell: false,
        stdio: ['ignore', 'pipe', 'pipe'],
        windowsHide: true,
      });

      this.registerCliProcess(options?.requestId, proc);
      this.setServerStatusCallback(onChunk);

      let settled = false;
      const forceTimeout = setTimeout(() => {
        if (settled) return;
        settled = true;
        this.clearCliProcess(options?.requestId, proc);
        try {
          if (process.platform === 'win32' && proc.pid) {
            spawn('taskkill', ['/pid', proc.pid.toString(), '/f', '/t']);
          } else {
            proc.kill('SIGTERM');
          }
        } catch {
          try {
            proc.kill();
          } catch {}
        }
        reject(new Error(this.normalizeProviderError(`OpenCode CLI request timed out after ${this.requestTimeoutMs}ms`)));
      }, this.requestTimeoutMs);

      const clearForceTimeout = () => {
        clearTimeout(forceTimeout);
      };

      let stdoutBuffer = '';
      let stderr = '';
      let streamedText = '';
      let renderedText = '';
      let finalText = '';
      const seenRenderedBlocks = new Set<string>();
      let completionFooter = '';
      let latestSessionId: string | null = options?.sessionId || null;
      let createdAt: number | undefined;
      let completedAt: number | undefined;
      let finishReason = 'unknown';
      let tokenInfo: TokenInfo | undefined;
      const rawEvents: any[] = [];
      const rawParts: any[] = [];

      const renderAndPush = (segment: string): void => {
        const cleaned = this.collapseDuplicateRenderableText(segment, seenRenderedBlocks);
        if (!cleaned) return;
        renderedText += (renderedText ? '\n\n' : '') + cleaned;
        onChunk(renderedText);
      };

      const pushStreamText = (next: string): string | null => {
        if (!next) return null;
        if (!streamedText) {
          streamedText = next;
          return streamedText;
        }

        if (next === streamedText) return null;
        if (next.startsWith(streamedText)) {
          const delta = next.slice(streamedText.length);
          streamedText = next;
          return delta || null;
        }

        if (streamedText.startsWith(next)) return null;

        streamedText += next;
        return next;
      };

      const emitProgressPart = (event: any): void => {
        const partType = String(event?.part?.type || event?.type || '').toLowerCase();

        const renderedPart = this.renderOpenCodePart(event?.part || event);
        if (renderedPart) renderAndPush(renderedPart);
      };

      const consumeLine = (rawLine: string): void => {
        const line = rawLine.trim();
        if (!line) return;

        try {
          const event = JSON.parse(line);
          rawEvents.push(event);
          if (event?.part) {
            rawParts.push(event.part);
          }

          latestSessionId = event?.sessionID || event?.part?.sessionID || latestSessionId;
          if (!createdAt && Number.isFinite(event?.timestamp)) {
            createdAt = Number(event.timestamp);
          }

          const textContent = typeof event?.part?.text === 'string'
            ? event.part.text
            : typeof event?.part?.content === 'string'
              ? event.part.content
              : null;

          if ((event?.type === 'text' || event?.type === 'reasoning') && textContent !== null) {
            const delta = pushStreamText(textContent);
            if (delta) renderAndPush(delta);
          }

          emitProgressPart(event);

          if (event?.type === 'step_finish' || event?.part?.type === 'step-finish') {
            completedAt = Number(event?.timestamp) || Date.now();
            finishReason = event?.part?.reason || finishReason;
            if (event?.part?.tokens) {
              tokenInfo = {
                input: event.part.tokens.input || 0,
                output: event.part.tokens.output || 0,
                total: event.part.tokens.total || 0,
                cost: event.part.cost || 0,
              };
            }
            completionFooter = this.renderOpenCodeCompletionFooter(finishReason, tokenInfo);
          }
        } catch {
          // Ignore non-JSON noise emitted by the CLI.
        }
      };

      proc.stdout?.on('data', (data) => {
        stdoutBuffer += data.toString();
        const lines = stdoutBuffer.split(/\r?\n/);
        stdoutBuffer = lines.pop() || '';
        for (const line of lines) {
          consumeLine(line);
        }
      });

      proc.stderr?.on('data', (data) => {
        const message = data.toString();
        stderr += message;
        this.detectAndEmitServerStatus(message);
      });

      proc.on('close', (code) => {
        if (settled) return;
        settled = true;
        clearForceTimeout();
        this.clearCliProcess(options?.requestId, proc);
        if (stdoutBuffer.trim()) {
          consumeLine(stdoutBuffer);
        }

        finalText = streamedText.trim();

        if (latestSessionId) {
          this.sessionId = latestSessionId;
          this.lastSessionTime = Date.now();
        }

        if (code === 0 || finalText) {
          const finalDisplayText = [renderedText.trim(), completionFooter].filter(Boolean).join('\n\n') || finalText;
          resolve({
            text: finalText || '✅ Completed.',
            displayText: finalDisplayText,
            tokens: tokenInfo,
            done: true,
            parts: rawParts,
            info: {
              providerID,
              modelID: actualModelID,
              finish: finishReason,
              sessionID: latestSessionId,
              time: {
                created: createdAt,
                completed: completedAt || Date.now(),
              },
              tokens: tokenInfo,
            },
            events: rawEvents,
            raw: {
              stderr,
              sessionID: latestSessionId,
              exitCode: code,
            },
          });
          return;
        }

        reject(new Error(this.normalizeProviderError(stderr.trim() || `OpenCode CLI exited with code ${code}`)));
      });

      proc.on('error', (err) => {
        if (settled) return;
        settled = true;
        clearForceTimeout();
        this.clearCliProcess(options?.requestId, proc);
        reject(new Error(`Failed to run opencode: ${err.message}. Make sure the OpenCode CLI is installed.`));
      });
    });
  }

  async chat(
    prompt: string,
    onChunk: (text: string) => void,
    options?: AIRequestOptions
  ): Promise<AIResponse> {
    const requestCLI = options?.cli || this.currentCLI;
    if (options?.model) {
      this.setDefaultModel(options.model, requestCLI);
    }
    if (requestCLI === 'opencode') return this.chatOpenCode(prompt, onChunk, options);
    if (requestCLI === 'claude') return this.chatClaude(prompt, onChunk, options);
    if (requestCLI === 'codex') return this.chatCodex(prompt, onChunk, options);

    this.setServerStatusCallback(onChunk);

    const ready = await this.ensureReadyForCLI('opencode', options?.workdir);
    if (!ready) {
      throw new Error('OpenCode server is not ready. Run /ai to start it for the selected project, then try /chat again.');
    }

    const startTime = Date.now();
    let fullText = '';
    let tokenInfo: TokenInfo | undefined;
    let lastEmittedChunk = '';

    const emitChunk = (chunk: string): void => {
      const next = (chunk || '').toString();
      if (!next.trim()) return;
      if (!lastEmittedChunk) {
        lastEmittedChunk = next;
        onChunk(next);
        return;
      }

      if (next === lastEmittedChunk) return;
      if (next.startsWith(lastEmittedChunk)) {
        lastEmittedChunk = next;
        onChunk(next);
        return;
      }
      if (lastEmittedChunk.startsWith(next)) {
        return;
      }

      lastEmittedChunk = next;
      onChunk(next);
    };

    const workdir = options?.workdir;
    const cli = options?.cli || this.currentCLI;
    
    this.setServerStatusCallback(onChunk);
    
    return new Promise((resolve, reject) => {
      this.markRequestStarted(options?.requestId);
      const sendRequest = async (sid: string | null, retryCount: number = 0, rateLimitRetryCount: number = 0) => {
        if (!sid) {
          const newSid = await this.createSession(workdir, true);
          if (newSid) {
            
            sendRequest(newSid, retryCount, rateLimitRetryCount);
          } else {
            reject(new Error('No session available and failed to create new session'));
          }
          return;
        }
        
        let modelId = options?.model || this.getDefaultModel('opencode');
        let variantOptions: Record<string, any> | undefined;
        const validationStartedAt = Date.now();

        const modelMap: Record<string, string> = {
          'bigpickle': 'opencode/big-pickle',
          'minimax': 'opencode/minimax-m2.5-free',
          'nemotron': 'opencode/nemotron-3-super-free',
        };

        const aliasVariantMatch = modelId.match(/^(bigpickle|minimax|nemotron)#(.+)$/);
        if (aliasVariantMatch && modelMap[aliasVariantMatch[1]]) {
          modelId = `${modelMap[aliasVariantMatch[1]]}#${aliasVariantMatch[2]}`;
        } else if (modelMap[modelId]) {
          modelId = modelMap[modelId];
        }

        const validation = await this.validateModelSelection(modelId);
        const validationDuration = Date.now() - validationStartedAt;
        if (validationDuration > 5000) {
          loggerService.warn('OpenCode model validation was slow', {
            duration: validationDuration,
            model: modelId,
            workdir: options?.workdir || process.cwd(),
          });
        }
        if (!validation.ok || !validation.model) {
          reject(new Error(validation.error || 'Unsupported model'));
          return;
        }

        modelId = validation.model.key;
        if (validation.variant) {
          variantOptions = validation.model.variantConfigs[validation.variant];
        }
        
        if (!modelId || !modelId.includes('/')) {
          console.log('[OpenCode] Invalid model, using default');
          modelId = this.getCanonicalOpenCodeDefaultModel();
        }
        
        const providerID = modelId.split('/')[0];
        const actualModelID = modelId.split('/').slice(1).join('/');
        
        console.log('[OpenCode] Using model:', providerID, '/', actualModelID);
        
        const postData = JSON.stringify({
          parts: [{ type: 'text', text: prompt }],
          model: {
            providerID,
            modelID: actualModelID,
            ...(variantOptions ? { options: variantOptions } : {}),
          },
          noReply: false,
        });

        console.log('[OpenCode] Request body:', postData);

        let settled = false;
        let hasReceivedData = false;
        let timeoutHandle: NodeJS.Timeout | null = null;
        let firstResponseByteAt: number | null = null;
        const pollStartedAt = Date.now();
        const pollingPromise = this.pollAssistantMessageUntilDone(sid, emitChunk, this.requestTimeoutMs, pollStartedAt, options?.requestId).catch(() => null);

        const clearWatchdog = () => {
          if (timeoutHandle) {
            clearTimeout(timeoutHandle);
            timeoutHandle = null;
          }
        };

        const failRequest = (req: http.ClientRequest, message: string) => {
          if (settled) return;
          settled = true;
          clearWatchdog();
          req.destroy();
          reject(new Error(message));
        };

        const retryRequest = (req: http.ClientRequest, message: string) => {
          if (settled) return;
          settled = true;
          clearWatchdog();
          req.destroy();
          this.sessionId = null;
          loggerService.warn('Retrying OpenCode request with a fresh session after transient failure', {
            message,
            retryCount,
            model: modelId,
            workdir: options?.workdir || process.cwd(),
          });
          this.createSession(options?.workdir).then((newSid) => {
            if (newSid) {
              sendRequest(newSid, retryCount + 1, rateLimitRetryCount);
            } else {
              reject(new Error(message));
            }
          }).catch(err => reject(err));
        };

        const retryRateLimitedRequest = (message: string, statusCode?: number) => {
          const nextAttempt = rateLimitRetryCount + 1;
          if (nextAttempt > this.maxRateLimitRetries) {
            reject(new Error(`Request cancelled after ${this.maxRateLimitRetries} retries due to rate limiting (HTTP ${statusCode || 429}).`));
            return;
          }

          loggerService.warn('Rate limited by model provider; retrying request', {
            statusCode: statusCode || 429,
            attempt: nextAttempt,
            maxAttempts: this.maxRateLimitRetries,
            retryDelayMs: this.retryDelay,
            model: modelId,
            workdir: options?.workdir || process.cwd(),
          });

          setTimeout(() => {
            sendRequest(sid, retryCount, nextAttempt);
          }, this.retryDelay);
        };

        const failOrRetryRequest = (req: http.ClientRequest, message: string) => {
          const lowerMsg = message.toLowerCase();
          const shouldRetry =
            retryCount < 1 &&
            (
              lowerMsg.includes('timed out waiting for first response') ||
              lowerMsg.includes('response stalled') ||
              lowerMsg.includes('request timeout') ||
              lowerMsg.includes('rate limit') ||
              lowerMsg.includes('usage limit') ||
              lowerMsg.includes('high traffic') ||
              lowerMsg.includes('overloaded')
            );

          if (shouldRetry) {
            retryRequest(req, message);
            return;
          }

          failRequest(req, message);
        };

        const armWatchdog = (req: http.ClientRequest, ms: number, message: string) => {
          clearWatchdog();
          timeoutHandle = setTimeout(() => {
            console.error('[OpenCode] Watchdog timeout:', message);
            failOrRetryRequest(req, message);
          }, ms);
        };

        const req = http.request({
          hostname: '127.0.0.1',
          port: this.serverPort,
          path: `/session/${sid}/message`,
          method: 'POST',
          timeout: this.requestTimeoutMs,
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream, application/json',
            'Content-Length': Buffer.byteLength(postData),
          },
        }, (res) => {
          let buffer = '';
          let lastSseText = '';
          let ndjsonBuffer = '';
          const streamEvents: any[] = [];

          armWatchdog(req, this.initialResponseTimeoutMs, 'Model request timed out waiting for first response. Try again later or switch model.');
          
          res.on('data', (chunk) => {
            hasReceivedData = true;
            if (firstResponseByteAt === null) {
              firstResponseByteAt = Date.now();
              const firstByteDelay = firstResponseByteAt - pollStartedAt;
              if (firstByteDelay > 5000) {
                loggerService.warn('OpenCode first response byte was slow', {
                  delay: firstByteDelay,
                  model: modelId,
                  workdir: options?.workdir || process.cwd(),
                  retryCount,
                });
              }
            }
            armWatchdog(req, this.stallTimeoutMs, 'Model response stalled for too long. Try again later or switch model.');
            const chunkText = chunk.toString();
            buffer += chunkText;
            ndjsonBuffer += chunkText;

            if (buffer.includes('\ndata:') || buffer.startsWith('data:')) {
              const { text: sseText, events } = this.extractTextFromSSE(buffer);
              if (events.length > 0) {
                streamEvents.length = 0;
                streamEvents.push(...events);
              }
              if (sseText && sseText !== lastSseText) {
                lastSseText = sseText;
                emitChunk(sseText);
              }
            }

            if (ndjsonBuffer.includes('\n')) {
              const lines = ndjsonBuffer.split(/\r?\n/);
              ndjsonBuffer = lines.pop() || '';
              for (const rawLine of lines) {
                const line = rawLine.trim();
                if (!line || line.startsWith('data:')) continue;
                try {
                  const ev = JSON.parse(line);
                  streamEvents.push(ev);
                  const lineText = this.collectTextFromParts(ev.parts || [])
                    || ev.text
                    || ev.content
                    || ev.delta?.text
                    || ev.delta?.content
                    || ev.part?.text
                    || ev.part?.content
                    || ev.data?.part?.text
                    || ev.data?.part?.content;
                  if (typeof lineText === 'string' && lineText) {
                    const merged = lineText.startsWith(lastSseText)
                      ? lineText
                      : `${lastSseText}${lineText}`;
                    if (merged !== lastSseText) {
                      lastSseText = merged;
                      emitChunk(lastSseText);
                    }
                  }
                } catch {
                  // ignore non-json line fragments
                }
              }
            }
          });

          res.on('end', async () => {
            if (settled) return;
            settled = true;
            clearWatchdog();
            this.clearHttpRequest(options?.requestId, req);
            console.log('[OpenCode] Response status:', res.statusCode);
            console.log('[OpenCode] Response body length:', buffer.length);
            console.log('[OpenCode] Content-Type:', res.headers['content-type']);
            
            try {
              if (res.statusCode && res.statusCode >= 400) {
                if (res.statusCode === 429) {
                  retryRateLimitedRequest(buffer || 'HTTP 429 Too Many Requests', res.statusCode);
                  return;
                }
                reject(new Error(this.normalizeProviderError(buffer || `HTTP ${res.statusCode}`, res.statusCode)));
                return;
              }

              if (buffer.trim() === '') {
                const emptyMessage = hasReceivedData
                  ? 'Model returned an empty response. Try again later or switch model.'
                  : 'Model request timed out waiting for a response. Try again later or switch model.';
                reject(new Error(this.normalizeProviderError(emptyMessage, res.statusCode)));
                return;
              }

              // Try SSE format first if buffer contains SSE lines
              const looksLikeSSE = buffer.includes('\ndata:') || buffer.startsWith('data:');
              if (looksLikeSSE) {
                const { text: sseText, tokenInfo: sseTok, events } = this.extractTextFromSSE(buffer);
                if (sseText.trim()) {
                  fullText = sseText;
                  emitChunk(fullText);
                  if (sseTok) tokenInfo = sseTok;
                  resolve({ text: fullText.trim(), tokens: tokenInfo, done: true, events, raw: buffer });
                  return;
                }
              }

              const json = JSON.parse(buffer);

              console.log('[OpenCode] Response info:', JSON.stringify(json.info));
              if (json.quota) {
                console.log('[OpenCode] Quota:', JSON.stringify(json.quota));
              }
              const partsCount = Array.isArray(json.parts) ? json.parts.length : 0;
              console.log('[OpenCode] Response parts count:', partsCount);
              if (partsCount === 0) {
                console.log('[OpenCode] Empty parts — top-level keys:', Object.keys(json).join(', '));
                console.log('[OpenCode] Response full:', JSON.stringify(json));
              }

              if (json.usage) {
                console.log('[OpenCode] Usage:', JSON.stringify(json.usage));
              }
              if (json.credits || json.billing) {
                console.log('[OpenCode] Credits/Billing:', JSON.stringify({ credits: json.credits, billing: json.billing }));
              }

              const quotaInfo = this.extractQuotaFromResponse(json, fullText);
                  if (quotaInfo && tokenInfo) {
                    tokenInfo.quota = quotaInfo;
                  }

              if (json.error) {
                console.log('[OpenCode] Error in response:', json.error);
                const jsonErrorText = this.safeStringify(json.error);
                if (jsonErrorText.toLowerCase().includes('session') || res.statusCode === 404) {
                  console.log('Session expired, recreating...');
                  this.sessionId = null;
                  this.createSession(options?.workdir).then(newSid => {
                    if (newSid) {
                      sendRequest(newSid, retryCount, rateLimitRetryCount);
                    } else {
                      reject(new Error('Failed to recreate session'));
                    }
                  }).catch(err => reject(err));
                  return;
                }
                if (
                  res.statusCode === 429 ||
                  jsonErrorText.toLowerCase().includes('rate limit') ||
                  jsonErrorText.toLowerCase().includes('too many requests')
                ) {
                  retryRateLimitedRequest(jsonErrorText || 'Model is rate limited right now.', res.statusCode);
                  return;
                }
                reject(new Error(this.normalizeProviderError(jsonErrorText, res.statusCode)));
                return;
              }

              if (json.info?.error) {
                const infoError = json.info.error;
                const errMsg =
                  infoError?.data?.message ||
                  infoError?.message ||
                  this.safeStringify(infoError);
                const errStatus = infoError?.data?.statusCode || res.statusCode;
                if (
                  errStatus === 429 ||
                  errMsg.toLowerCase().includes('rate limit') ||
                  errMsg.toLowerCase().includes('too many requests')
                ) {
                  retryRateLimitedRequest(errMsg || 'Model is rate limited right now.', errStatus);
                  return;
                }
                reject(new Error(this.normalizeProviderError(errMsg, errStatus)));
                return;
              }

              if (json.parts && Array.isArray(json.parts)) {
                for (const part of json.parts) {
                  if (part.type === 'text' && part.text !== undefined) {
                    fullText += this.safeStringify(part.text);
                    emitChunk(fullText);
                  }
                  if (part.type === 'reasoning' && part.text !== undefined) {
                    fullText += this.safeStringify(part.text);
                    emitChunk(fullText);
                  }
                  if (part.tokens) {
                    tokenInfo = {
                      input: part.tokens.input || 0,
                      output: part.tokens.output || 0,
                      total: part.tokens.total || 0,
                      cost: part.cost || 0,
                    };
                  }
                }
              }

              let polledResult: { text: string; parts?: any[]; raw?: any } | null = null;
              if (!fullText.trim()) {
                polledResult = await pollingPromise;
                if (polledResult?.text?.trim()) {
                  fullText = polledResult.text;
                }
              }

              if (!fullText.trim()) {
                try {
                  const polled = await this.pollAssistantMessageUntilDone(sid, emitChunk, this.requestTimeoutMs, pollStartedAt, options?.requestId);
                  fullText = polled.text;
                  resolve({
                    text: fullText.trim(),
                    tokens: tokenInfo,
                    done: true,
                    parts: polled.parts,
                    info: json.info,
                    events: streamEvents.length > 0 ? streamEvents : undefined,
                    raw: polled.raw || json,
                  });
                  return;
                } catch {
                  // keep existing fallback below
                }
              }

              if (!fullText.trim()) {
                const fallbackText = this.extractAssistantText(json);
                if (fallbackText.trim()) {
                  fullText = fallbackText;
                  emitChunk(fullText);
                }
              }

              if (!fullText.trim()) {
                const debugText = this.buildPayloadDebug(json);
                loggerService.error('AI response contained no text content', {
                  model: modelId,
                  providerID,
                  modelID: actualModelID,
                  statusCode: res.statusCode,
                  contentType: res.headers['content-type'],
                  debug: debugText,
                  topKeys: Object.keys(json).join(','),
                  bodyPreview: JSON.stringify(json).substring(0, 800),
                });
                reject(new Error('Model returned no text content. See server logs for payload details.'));
                return;
              }

              if (json.info?.tokens) {
                tokenInfo = {
                  input: json.info.tokens.input || 0,
                  output: json.info.tokens.output || 0,
                  total: json.info.tokens.total || 0,
                  cost: json.info.cost || 0,
                };
              }

              resolve({
                text: fullText.trim(),
                tokens: tokenInfo,
                done: true,
                parts: Array.isArray(json.parts) ? json.parts : (polledResult?.parts || undefined),
                info: json.info,
                events: streamEvents.length > 0 ? streamEvents : undefined,
                raw: polledResult?.raw || json,
              });
              this.markRequestFinished(options?.requestId);
            } catch (e) {
              // JSON parse failed — try SSE fallback
              if (buffer.includes('\ndata:') || buffer.startsWith('data:')) {
                const { text: sseText, tokenInfo: sseTok, events } = this.extractTextFromSSE(buffer);
                if (sseText.trim()) {
                  if (sseTok) tokenInfo = sseTok;
                  resolve({ text: sseText.trim(), tokens: tokenInfo, done: true, events, raw: buffer });
                  this.markRequestFinished(options?.requestId);
                  return;
                }
              }
              console.error('[OpenCode] Failed to parse response:', buffer.substring(0, 500));
              loggerService.error('Failed to parse AI response payload', {
                model: modelId,
                providerID,
                modelID: actualModelID,
                statusCode: res.statusCode,
                contentType: res.headers['content-type'],
                bodyPreview: buffer.substring(0, 1200),
              });
              reject(new Error(this.normalizeProviderError('Failed to parse response: ' + buffer.substring(0, 200), res.statusCode)));
              this.markRequestFinished(options?.requestId);
            }
          });
        });

        req.on('error', (err) => {
          if (settled) return;
          settled = true;
          clearWatchdog();
          this.clearHttpRequest(options?.requestId, req);
          console.error('[OpenCode] Request error:', err.message);
          if (!this.isReady) {
            this.scheduleReconnect();
          }
          reject(new Error(this.normalizeProviderError(err.message)));
          this.markRequestFinished(options?.requestId);
        });

        req.on('timeout', () => {
          console.error('[OpenCode] Request timeout, recreating session');
          failOrRetryRequest(req, this.normalizeProviderError('Request timeout, please try again'));
        });

        req.on('close', () => {
          this.clearHttpRequest(options?.requestId, req);
          console.log('[OpenCode] Request connection closed');
        });

        this.registerHttpRequest(options?.requestId, req);

        armWatchdog(req, this.initialResponseTimeoutMs, 'Model request timed out waiting for first response. Try again later or switch model.');
        req.write(postData);
        req.end();
      };

      sendRequest(options?.freshSession ? null : (options?.sessionId ?? this.sessionId));
    });
  }

  async chatStream(
    messages: Array<{ role: 'user' | 'assistant'; content: string }>,
    newMessage: string,
    onChunk: (text: string) => void,
    options?: AIRequestOptions
  ): Promise<AIResponse> {
    const requestCLI = options?.cli || this.currentCLI;
    if (requestCLI === 'opencode') {
      if (options?.sessionId) {
        return this.chat(newMessage, onChunk, options);
      }

      const prompt = this.buildOpenCodeStatelessChatPrompt(messages, newMessage);
      return this.chat(prompt, onChunk, { ...options, freshSession: true, sessionId: undefined });
    }

    const context = messages.map(m => `${m.role}: ${m.content}`).join('\n');
    return this.chat(`${context}\n\nUser: ${newMessage}`, onChunk, options);
  }

  private async chatClaude(
    prompt: string,
    onChunk: (text: string) => void,
    options?: AIRequestOptions
  ): Promise<AIResponse> {
    return new Promise((resolve, reject) => {
      const targetWorkdir = options?.workdir || process.cwd();
      const gradleUserHome = path.join(targetWorkdir, '.gradle-user-home');
      const args = ['--print'];
      const modelOption = this.resolveCliModelOption('claude', options?.model);
      if (modelOption) args.push('--model', modelOption);
      args.push('--permission-mode', 'acceptEdits', '--add-dir', targetWorkdir);
      args.push(prompt);

      const proc = spawn(this.getCliExecutable('claude'), args, {
        cwd: targetWorkdir,
        env: getProcessEnv({
          GRADLE_USER_HOME: gradleUserHome,
          HOME: process.env.HOME || targetWorkdir,
          USERPROFILE: process.env.USERPROFILE || targetWorkdir,
        } as any),
        shell: process.platform === 'win32',
        windowsHide: true,
      });

      let fullText = '';
      this.registerCliProcess(options?.requestId, proc);

      proc.stdout?.on('data', (data) => {
        fullText += data.toString();
        onChunk(fullText);
      });

      proc.on('close', (code) => {
        this.clearCliProcess(options?.requestId, proc);
        if (code === 0 || fullText.trim()) {
          resolve({ text: fullText.trim(), done: true, raw: { stdout: fullText } });
        } else {
          reject(new Error(`Claude CLI exited with code ${code}`));
        }
      });

      proc.on('error', (err) => {
        this.clearCliProcess(options?.requestId, proc);
        reject(new Error(`Failed to run claude: ${err.message}. Make sure the Claude CLI is installed.`));
      });
    });
  }

  private async chatCodex(
    prompt: string,
    onChunk: (text: string) => void,
    options?: AIRequestOptions
  ): Promise<AIResponse> {
    return new Promise((resolve, reject) => {
      const executeCodex = (modelOption: string | undefined, allowRetryWithoutModel: boolean): void => {
        const targetWorkdir = options?.workdir || process.cwd();
        const gradleUserHome = path.join(targetWorkdir, '.gradle-user-home');
        const proc = process.platform === 'win32'
          ? spawn('cmd.exe', [
              '/d',
              '/s',
              '/c',
              `codex.cmd exec --json --skip-git-repo-check --sandbox workspace-write --add-dir "${targetWorkdir}"${modelOption ? ` -m ${modelOption}` : ''}`,
            ], {
              cwd: targetWorkdir,
              env: getProcessEnv({
                GRADLE_USER_HOME: gradleUserHome,
                HOME: process.env.HOME || targetWorkdir,
                USERPROFILE: process.env.USERPROFILE || targetWorkdir,
              } as any),
              shell: false,
              windowsHide: true,
            })
          : spawn(this.getCliExecutable('codex'), [
              'exec',
              '--json',
              '--skip-git-repo-check',
              '--sandbox',
              'workspace-write',
              '--add-dir',
              targetWorkdir,
              ...(modelOption ? ['-m', modelOption] : []),
            ], {
              cwd: targetWorkdir,
              env: getProcessEnv({
                GRADLE_USER_HOME: gradleUserHome,
                HOME: process.env.HOME || targetWorkdir,
                USERPROFILE: process.env.USERPROFILE || targetWorkdir,
              } as any),
              shell: false,
              windowsHide: true,
            });

        this.registerCliProcess(options?.requestId, proc);

        let fullText = '';
        let stderr = '';
        let stdoutBuffer = '';
        let streamedText = '';
        const rawEvents: any[] = [];

        const pushStreamText = (next: string): void => {
          if (!next) return;
          if (!streamedText) {
            streamedText = next;
            onChunk(streamedText);
            return;
          }

          if (next === streamedText) return;
          if (next.startsWith(streamedText)) {
            streamedText = next;
            onChunk(streamedText);
            return;
          }

          streamedText += next;
          onChunk(streamedText);
        };

        proc.stdout?.on('data', (data) => {
          stdoutBuffer += data.toString();
          const lines = stdoutBuffer.split(/\r?\n/);
          stdoutBuffer = lines.pop() || '';

          for (const rawLine of lines) {
            const line = rawLine.trim();
            if (!line) continue;

            try {
              const event = JSON.parse(line);
              rawEvents.push(event);
              const deltaText = event?.delta?.text
                || event?.delta?.content
                || event?.item?.delta?.text
                || event?.item?.delta?.content
                || event?.item?.output_text;
              if (typeof deltaText === 'string' && deltaText.trim()) {
                pushStreamText(deltaText);
              }

              const itemText = event?.item?.text || event?.text || event?.content;
              if (typeof itemText === 'string' && itemText.trim()) {
                pushStreamText(itemText);
                if (event?.type === 'item.completed') {
                  fullText = itemText;
                }
              }
            } catch {
              // Ignore non-JSON progress noise emitted by the CLI.
            }
          }
        });

        proc.stderr?.on('data', (data) => {
          stderr += data.toString();
        });

        proc.stdin?.write(prompt);
        proc.stdin?.end();

        proc.on('close', (code) => {
          this.clearCliProcess(options?.requestId, proc);
          const stderrInfo = this.normalizeCodexStderr(stderr);
          if (stderrInfo.unexpected.length > 0) {
            loggerService.warn('Codex CLI emitted stderr output', {
              lines: stderrInfo.unexpected,
              exitCode: code,
            });
          }

          if (!fullText.trim() && stdoutBuffer.trim()) {
            for (const rawLine of stdoutBuffer.split(/\r?\n/)) {
              const line = rawLine.trim();
              if (!line) continue;

              try {
                const event = JSON.parse(line);
                rawEvents.push(event);
                const text = event?.item?.text;
                if (event?.type === 'item.completed' && typeof text === 'string' && text.trim()) {
                  fullText = text;
                  break;
                }
              } catch {}
            }
          }

          if (!fullText.trim() && streamedText.trim()) {
            fullText = streamedText;
          }

          if (code === 0 || fullText.trim()) {
            resolve({ text: fullText.trim(), done: true, events: rawEvents, raw: { stderr, tailBuffer: stdoutBuffer } });
            return;
          }

          const hasDiagnostics = stderrInfo.unexpected.length > 0 || stdoutBuffer.trim().length > 0;
          if (allowRetryWithoutModel && modelOption && !hasDiagnostics) {
            loggerService.warn('Codex CLI failed with model override and no diagnostics; preserving requested model for consistency', {
              requestedModel: options?.model || this.getDefaultModel('codex'),
              normalizedModel: modelOption,
              workdir: options?.workdir || process.cwd(),
            });
          }

          const detail = stderrInfo.unexpected.join('\n').trim() || stdoutBuffer.trim();
          const normalizedDetail = detail.toLowerCase().includes('blocked by policy')
            ? 'Codex CLI was blocked by policy while checking write access in this folder. Approve write access for the project root, or run the request in a writable folder.'
            : detail;
          loggerService.error('Codex CLI request failed', this.buildCodexDebugMeta(
            prompt,
            options,
            code ?? null,
            stderrInfo,
            stdoutBuffer
          ));
          reject(new Error(normalizedDetail || `Codex CLI exited with code ${code}`));
        });

        proc.on('error', (err) => {
          this.clearCliProcess(options?.requestId, proc);
          loggerService.error('Failed to spawn Codex CLI', {
            error: err.message,
            model: options?.model || this.getDefaultModel('codex'),
            workdir: options?.workdir || process.cwd(),
            promptPreview: prompt.slice(0, 500),
          });
          reject(new Error(`Failed to run codex: ${err.message}. Make sure the Codex CLI is installed.`));
        });
      };

      executeCodex(this.resolveCliModelOption('codex', options?.model), true);
    });
  }

  async shutdown(): Promise<void> {
    this.initRetries = this.maxInitRetries;
    this.initPromise = null;
    
    if (this.serverProcess) {
      console.log('🔄 Stopping OpenCode server...');
      try {
        this.serverProcess.kill('SIGTERM');
      } catch {}
      this.serverProcess = null;
      this.opencodeWorkdir = null;
      this.isReady = false;
      this.sessionId = null;
    }
  }
}

export const aiService = new AIService();
