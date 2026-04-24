import TelegramBot, { Message, CallbackQuery } from 'node-telegram-bot-api';
import config, { telegramToken } from '../config';
import { loggerService, securityService, terminalService, aiService, androidService } from '../services';
import { splitMessage, truncateOutput, getOS, normalizeErrorMessage, buildCodeBlockMessages, buildTelegramHtmlCodeBlockMessages, createCoalescedAsyncRenderer } from '../utils';
import { buildCurrentContext } from '../utils/current';

const BOT_VERSION = '1.0.2';
let eventLoopWatchdogTimer: NodeJS.Timeout | null = null;

function startEventLoopWatchdog(): void {
  const checkIntervalMs = Math.max(1000, config.eventLoopWatchdogIntervalMs || 10000);
  const warnThresholdMs = Math.max(100, config.eventLoopLagWarnMs || 1500);
  let expectedAt = Date.now() + checkIntervalMs;

  eventLoopWatchdogTimer = setInterval(() => {
    const now = Date.now();
    const lagMs = Math.max(0, now - expectedAt);
    expectedAt = now + checkIntervalMs;

    if (lagMs < warnThresholdMs) return;

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

interface ChatSession {
  workdir: string;
}

interface ChatDraft {
  parts: string[];
  workdir: string;
}

interface ActiveRequest {
  abort: () => void;
}

interface AutoCodeState {
  running: boolean;
  stopRequested: boolean;
  startedAt: number;
  currentStep: number;
  maxSteps: number;
  goal: string;
  workdir: string;
  lastSummary: string;
}

interface ChatRunResult {
  ok: boolean;
  finalText: string;
}

const chatSessions: Map<string, ChatSession> = new Map();
const chatDrafts: Map<string, ChatDraft> = new Map();
const autoCodeStates: Map<string, AutoCodeState> = new Map();

const bot = new TelegramBot(telegramToken, { polling: true });

const mainMenuKeyboard: TelegramBot.ReplyKeyboardMarkup = {
  keyboard: [
    [{ text: '/status' }, { text: '/ping' }],
    [{ text: '/sessions' }, { text: '/current' }, { text: '/models' }],
    [{ text: '/syncproject' }, { text: '/runapp' }],
  ],
  resize_keyboard: true,
  is_persistent: true,
};

function menuMarkup(): TelegramBot.SendMessageOptions {
  return {
    parse_mode: 'Markdown',
    reply_markup: mainMenuKeyboard,
  };
}

function menuText(): string {
  return [
    '📋 *Telegram Menu*',
    '',
    'Buttons on keyboard:',
    '• `/status` - Check session/process/system',
    '• `/ping` - Check latency',
    '• `/sessions` - List sessions',
    '• `/current` - Show current context',
    '• `/models` - Show available models',
    '• `/syncproject` - Sync Android project',
    '• `/runapp` - Build and launch app',
    '',
    'Extra commands (type manually):',
    '• `/chat` - Chat with AI',
    '• `/chatadd` - Add prompt draft',
    '• `/chatend` - Send draft to AI',
    '• `/autocode` - Start autonomous coding loop',
    '• `/autocodestatus` - Show loop status',
    '• `/autocodestop` - Stop loop',
    '• `/menu` - Show this menu',
  ].join('\n');
}

const runTelegramChatPrompt = async (chatId: string, userId: string, workdir: string, message: string): Promise<ChatRunResult> => {
  const cliName = aiService.getCliDisplayName(aiService.getCLI());
  const sentMessage = await bot.sendMessage(chatId, '*Thinking...*', { parse_mode: 'Markdown' });
  let streamMessageIds = [sentMessage.message_id];
  const streamState: StreamRenderState = { rawText: '', visibleText: '', lastDeltaNormalized: '', repeatCount: 0, seenNormalizedSegments: new Set() };
  const streamHeader = `🤖 *${cliName} Chat*`;
  const streamRenderer = createCoalescedAsyncRenderer<string>(
    async (text) => {
      streamMessageIds = await syncTelegramCodeBlockParts(chatId, streamMessageIds, text, streamHeader);
    },
    (error) => {
      loggerService.warn('Failed to render Telegram chat stream chunk', {
        chatId,
        userId,
        cli: aiService.getCLI(),
        error: normalizeErrorMessage(error),
      });
    }
  );

  const startTime = Date.now();

  try {
    const response = await aiService.chatStream(
      [],
      message,
      (chunk) => {
        const rendered = appendVisibleStreamDelta(chunk, streamState);
        if (!rendered) return;
        streamRenderer.schedule(rendered);
      },
      { workdir, model: 'opencode/big-pickle' }
    );

    const duration = Date.now() - startTime;
    const cleanText = response.text.replace(/\x1b\[[0-9;]*m/g, '').trim();
    const renderedText = (response.displayText || response.text).replace(/\x1b\[[0-9;]*m/g, '').trim();
    await streamRenderer.flush();
    const renderedFinal = appendVisibleStreamDelta(renderedText || cleanText || 'No response', streamState);
    if (renderedFinal) {
      streamMessageIds = await syncTelegramCodeBlockParts(chatId, streamMessageIds, appendEndMarker(renderedFinal), `${cliName} Chat done`);
    }

    let responseText = `${cliName} chat finished\n`;
    responseText += `Time: ${duration}ms\n`;
    responseText += 'Messages: 1';
    if (response.tokens) {
      responseText += `\nTokens: in=${response.tokens.input} | out=${response.tokens.output} | total=${response.tokens.total}`;
      if (response.tokens.quota) {
        const q = response.tokens.quota;
        if (q.remaining) {
          responseText += `\nRemaining: ${q.remaining}`;
        }
        if (q.usedPercent) {
          responseText += ` (${q.usedPercent}% used)`;
        }
        if (q.resetsIn) {
          responseText += ` | Resets: ${q.resetsIn}`;
        }
      }
      responseText += `\nCost: ${response.tokens.cost > 0 ? `$${response.tokens.cost.toFixed(6)}` : 'Free'}`;
    }

    await bot.sendMessage(chatId, responseText);
    return {
      ok: true,
      finalText: renderedText || cleanText || 'No response',
    };
  } catch (error: any) {
    const duration = Date.now() - startTime;
    const errorMessage = normalizeErrorMessage(error);
    await safeEditMessageText(`*Error*\n\n${errorMessage}\nTime: ${duration}ms`, {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown'
    });
    return {
      ok: false,
      finalText: errorMessage,
    };
  }
};

loggerService.initialize();
loggerService.info(`Starting Telegram Terminal Bot v${BOT_VERSION}...`, { startTime: new Date().toISOString() });
startEventLoopWatchdog();

const telegramMenuCommands = [
  { command: 'status', description: 'Check session/process/system status' },
  { command: 'ping', description: 'Check bot latency' },
  { command: 'sessions', description: 'List active terminal sessions' },
  { command: 'current', description: 'Show current project and AI context' },
  { command: 'models', description: 'List available models' },
  { command: 'run', description: 'Run terminal command: /run <cmd>' },
  { command: 'syncproject', description: 'Sync Android project' },
  { command: 'runapp', description: 'Build and launch Android app' },
  { command: 'chat', description: 'Chat with AI: /chat <message>' },
  { command: 'chatadd', description: 'Add prompt part to draft' },
  { command: 'chatend', description: 'Send all draft parts to AI' },
  { command: 'autocode', description: 'Run autonomous coding loop' },
  { command: 'autocodestop', description: 'Stop autonomous coding loop' },
  { command: 'autocodestatus', description: 'Show autonomous coding status' },
  { command: 'menu', description: 'Show custom keyboard menu' },
  { command: 'help', description: 'Show help message' },
];

void bot.setMyCommands(telegramMenuCommands)
  .then(() => {
    loggerService.info('Telegram command menu configured', { count: telegramMenuCommands.length });
  })
  .catch((error) => {
    loggerService.warn('Failed to configure Telegram command menu', {
      error: normalizeErrorMessage(error),
    });
  });

const isAuthorized = (userId: number): boolean => {
  return securityService.isOwner(userId.toString());
};

const sendLargeMessage = async (chatId: number, text: string, maxLength: number = 4000) => {
  const messages = splitMessage(text, maxLength - 100);
  for (const msg of messages) {
    await bot.sendMessage(chatId, msg, { parse_mode: 'Markdown' });
  }
};

const syncTelegramCodeBlockParts = async (
  chatId: string,
  messageIds: number[],
  text: string,
  header: string
): Promise<number[]> => {
  const parts = buildTelegramHtmlCodeBlockMessages(text || 'No response', header, 3900);
  const ids = [...messageIds];

  while (ids.length < parts.length) {
    const sent = await bot.sendMessage(chatId, parts[ids.length], { parse_mode: 'HTML' });
    ids.push(sent.message_id);
  }

  for (let i = 0; i < parts.length; i++) {
    await safeEditMessageText(parts[i], {
      chat_id: chatId,
      message_id: ids[i],
      parse_mode: 'HTML'
    });
  }

  return ids;
};

const appendEndMarker = (text: string): string => {
  const normalized = (text || '').trimEnd();
  if (!normalized) return '--- END ---';
  if (normalized.endsWith('--- END ---')) return normalized;
  return `${normalized}\n\n--- END ---`;
};

const AUTO_CODE_DEFAULT_STEPS = 20;

const sleep = async (ms: number): Promise<void> => {
  await new Promise((resolve) => setTimeout(resolve, ms));
};

const buildAutoCodePrompt = (goal: string, previousSummary: string, step: number, maxSteps: number): string => {
  const hasPrevious = !!previousSummary.trim();
  return [
    'You are continuing an autonomous coding loop on this local project.',
    `Primary goal: ${goal}`,
    `Current iteration: ${step}/${maxSteps}`,
    hasPrevious ? `Previous iteration summary:\n${previousSummary}` : 'Previous iteration summary: none',
    'Instructions:',
    '1) Inspect current project state and identify the highest-value next coding task.',
    '2) Implement the task directly in the repository with safe, incremental changes.',
    '3) Run relevant verification (build/tests/lint as needed).',
    '4) Summarize exactly what was completed, what remains, and the next best task.',
    '5) If goal is complete, clearly include: AUTOCODE_DONE',
    '6) If blocked by missing credentials/secrets/manual approval, include: AUTOCODE_BLOCKED and explain briefly.',
  ].join('\n\n');
};

const renderAutoCodeStatus = (state: AutoCodeState): string => {
  return [
    '*AutoCode status*',
    '',
    `Running: ${state.running ? 'Yes' : 'No'}`,
    `Stop requested: ${state.stopRequested ? 'Yes' : 'No'}`,
    `Step: ${state.currentStep}/${state.maxSteps}`,
    `Workdir: \`${state.workdir}\``,
    `Goal: ${state.goal}`,
  ].join('\n');
};

const runAutoCodeLoop = async (chatId: string, userId: string, sessionKey: string, state: AutoCodeState): Promise<void> => {
  const header = '*AutoCode started*';
  await bot.sendMessage(chatId, [header, `Goal: ${state.goal}`, `Max steps: ${state.maxSteps}`].join('\n'));

  while (state.running && !state.stopRequested && state.currentStep < state.maxSteps) {
    state.currentStep += 1;
    const step = state.currentStep;
    const prompt = buildAutoCodePrompt(state.goal, state.lastSummary, step, state.maxSteps);

    await bot.sendMessage(chatId, `AutoCode step ${step}/${state.maxSteps}: running...`);
    const result = await runTelegramChatPrompt(chatId, userId, state.workdir, prompt);
    if (!result.ok) {
      state.running = false;
      await bot.sendMessage(chatId, `AutoCode stopped at step ${step} due to an execution error.`);
      break;
    }

    const latestSummary = (result.finalText || '').trim();
    state.lastSummary = latestSummary;

    if ((latestSummary || '').includes('AUTOCODE_DONE')) {
      state.running = false;
      await bot.sendMessage(chatId, `AutoCode finished early at step ${step} (goal completed).`);
      break;
    }

    if ((latestSummary || '').includes('AUTOCODE_BLOCKED')) {
      state.running = false;
      await bot.sendMessage(chatId, `AutoCode blocked at step ${step}. Check previous output for details.`);
      break;
    }

    await sleep(800);
  }

  if (state.stopRequested) {
    state.running = false;
    await bot.sendMessage(chatId, 'AutoCode stop request acknowledged.');
  }

  if (state.running && state.currentStep >= state.maxSteps) {
    state.running = false;
    await bot.sendMessage(chatId, `AutoCode reached max steps (${state.maxSteps}) and stopped.`);
  }

  autoCodeStates.set(sessionKey, state);
};

const parseAutoCodeInput = (text: string): { goal: string; steps: number; workdir?: string } => {
  let raw = (text || '').replace(/^\/autocode\b/i, '').trim();
  let steps = AUTO_CODE_DEFAULT_STEPS;
  let workdir: string | undefined;

  const stepsMatch = raw.match(/(?:^|\s)--steps\s+(\d+)/i);
  if (stepsMatch?.[1]) {
    const parsed = parseInt(stepsMatch[1], 10);
    if (Number.isFinite(parsed)) {
      steps = Math.max(1, Math.min(200, parsed));
    }
    raw = raw.replace(stepsMatch[0], ' ').trim();
  }

  const pathMatch = raw.match(/(?:^|\s)--path\s+(\S+)/i);
  if (pathMatch?.[1]) {
    workdir = pathMatch[1].trim();
    raw = raw.replace(pathMatch[0], ' ').trim();
  }

  return { goal: raw.trim(), steps, workdir };
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
      const normalizedLine = (line || '').replace(/\s+/g, ' ').trim();
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

  const latestNormalized = (latest || '').replace(/\s+/g, ' ').trim();
  const previousNormalized = (previous || '').replace(/\s+/g, ' ').trim();
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

  const deltaNormalized = (delta || '').replace(/\s+/g, ' ').trim();
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
  const dedupedDeltaNormalized = (dedupedDelta || '').replace(/\s+/g, ' ').trim();
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

function isTelegramMessageNotModifiedError(error: unknown): boolean {
  return normalizeErrorMessage(error).toLowerCase().includes('message is not modified');
}

async function safeEditMessageText(
  text: string,
  options: TelegramBot.EditMessageTextOptions
): Promise<void> {
  try {
    await bot.editMessageText(text, options);
  } catch (error) {
    if (isTelegramMessageNotModifiedError(error)) {
      return;
    }
    throw error;
  }
}

async function safeEditMessageReplyMarkup(
  replyMarkup: TelegramBot.InlineKeyboardMarkup,
  options: TelegramBot.EditMessageReplyMarkupOptions
): Promise<void> {
  try {
    await bot.editMessageReplyMarkup(replyMarkup, options);
  } catch (error) {
    if (isTelegramMessageNotModifiedError(error)) {
      return;
    }
    throw error;
  }
}

bot.onText(/\/start/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }
  await bot.sendMessage(msg.chat.id, `
?? *Telegram Terminal Bot v${BOT_VERSION}*

Welcome! This bot provides remote terminal access via Telegram.

?? *Available Commands:*
• /run <command> - Execute a terminal command
• /status [type] - Check status (session/process/system)
• /stop - Stop the running process
• /cd <directory> - Change working directory
• /history [lines] [--clear] - View/clear command history
• /logs [lines] - View command logs
• /sessions - List all terminal sessions
• /chat <message> [--path] [--clear] - Chat with AI
• /ping - Check bot latency
• /help - Show help message

?? *Security:* Only owner(s) can use commands.
  `.trim(), menuMarkup());
});

bot.onText(/\/menu/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  await bot.sendMessage(msg.chat.id, menuText(), menuMarkup());
});

bot.onText(/\/help/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const os = getOS();
  const shellInfo = os === 'windows' ? 'PowerShell' : 'Bash';

  await bot.sendMessage(msg.chat.id, `
?? *Telegram Terminal Bot - Help*

?? *Commands:*
• /run <command> - Execute a terminal command
• /status [session|process|system] - Check status
• /stop - Stop the running process
• /cd <directory> - Change working directory
• /history [lines] [--clear] - View/clear command history
• /logs [lines] - View command logs
• /sessions - List all terminal sessions
• /sessionclear [all] - Clear current session or all sessions
• /chat <message> [--path dir] [--clear] - Chat with AI
• /chatadd <text> [--path dir] - Add prompt part
• /chatend [--path dir] [--clear] - Send all /chatadd parts
• /ping - Check bot latency

?? *Security:*
• Only owner(s) can use commands
• Dangerous commands are blocked
• Command cooldown enforced
• All commands are logged

??? *System Info:*
• Shell: ${shellInfo}
• OS: ${os.charAt(0).toUpperCase() + os.slice(1)}
• Process Timeout: 10 minutes
• Max Output: 4000 characters
  `.trim(), menuMarkup());
});

bot.onText(/\/ping/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }
  const start = Date.now();
  await bot.sendMessage(msg.chat.id, '?? Pong!');
  const latency = Date.now() - start;
  await bot.sendMessage(msg.chat.id, `?? Latency: ${latency}ms`);
});

bot.onText(/\/sessions/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const sessions = terminalService.getAllSessions();
  if (sessions.length === 0) {
    await bot.sendMessage(msg.chat.id, '?? No active sessions.');
    return;
  }

  let response = '?? *Active Sessions:*\n\n';
  for (const session of sessions) {
    const status = terminalService.isProcessRunning(session.channelId) ? '?? Running' : '?? Idle';
    response += `• Chat ID: \`${session.channelId}\`\n`;
    response += `  CWD: \`${session.cwd}\`\n`;
    response += `  Status: ${status}\n`;
    response += `  History: ${session.history.length} commands\n\n`;
  }
  await bot.sendMessage(msg.chat.id, response, {
    parse_mode: 'Markdown',
    reply_markup: {
      inline_keyboard: [
        ...sessions.slice(0, 10).map((session) => ([{
          text: session.channelId === msg.chat.id.toString() ? `Current ${session.channelId}` : session.channelId,
          callback_data: `sessionpick_${session.channelId}`,
        }])),
        [
          { text: 'Clear current', callback_data: `sessionclearcurrent_${msg.chat.id}` },
          { text: 'Clear all', callback_data: 'sessionclearall_global' },
        ],
      ],
    },
  });
});

bot.onText(/\/current/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const current = buildCurrentContext(msg.chat.id.toString());
  const text = [
    '*Current Context*',
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

  await bot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
});

bot.onText(/\/models(?:\s+(\S+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const cli = aiService.getCLI();
  const currentModel = aiService.getDefaultModel(cli);
  const filter = match?.[1]?.trim().toLowerCase();

  try {
    let models = await aiService.listModelsForCLI(cli).catch(() => aiService.getSupportedModels(cli));
    if (filter) {
      models = models.filter((model) => model.toLowerCase().includes(filter));
    }

    if (models.length === 0) {
      await bot.sendMessage(msg.chat.id, 'No models found for the current filter.');
      return;
    }

    const preview = models.slice(0, 20).map((model) => `• \`${model}\`${model === currentModel ? ' (current)' : ''}`).join('\n');
    const more = models.length > 20 ? `\n... and ${models.length - 20} more` : '';
    const text = [
      '*Available Models*',
      '',
      `CLI: \`${cli}\``,
      `Current: \`${currentModel}\``,
      filter ? `Filter: \`${filter}\`` : '',
      '',
      preview + more,
      '',
      'Usage: `/models` or `/models <keyword>`'
    ].filter(Boolean).join('\n');

    await bot.sendMessage(msg.chat.id, text, { parse_mode: 'Markdown' });
  } catch (error: any) {
    await bot.sendMessage(msg.chat.id, `Failed to list models: ${error.message}`);
  }
});

bot.onText(/\/sessionclear(?:\s+(all))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const clearAll = (match?.[1] || '').trim().toLowerCase() === 'all';
  if (clearAll) {
    const cleared = terminalService.clearAllSessions();
    await bot.sendMessage(msg.chat.id, `?? Cleared ${cleared} terminal session${cleared === 1 ? '' : 's'}.`);
    return;
  }

  const channelId = msg.chat.id.toString();
  const existing = terminalService.getSession(channelId);
  if (!existing) {
    await bot.sendMessage(msg.chat.id, '?? No terminal session to clear in this chat.');
    return;
  }

  terminalService.destroySession(channelId);
  await bot.sendMessage(msg.chat.id, '?? Cleared the current terminal session.');
});

bot.onText(/\/status(?:\s+(.*))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const type = match?.[1]?.trim() || 'session';
  const chatId = msg.chat.id.toString();
  const session = terminalService.getSession(chatId);

  if (type === 'session' || type === 'all') {
    if (session) {
      const processRunning = terminalService.isProcessRunning(chatId);
      await bot.sendMessage(msg.chat.id, `
?? *Session Status*
• CWD: \`${session.cwd}\`
• Process: ${processRunning ? '?? Running' : '?? Idle'}
• History: ${session.history.length} commands
      `.trim(), { parse_mode: 'Markdown' });
    } else {
      await bot.sendMessage(msg.chat.id, '?? No active session. Use /run to create one.');
    }
  }

  if (type === 'process' || type === 'all') {
    const processInfo = terminalService.getActiveProcess(chatId);
    if (processInfo) {
      const duration = Date.now() - processInfo.startTime.getTime();
      await bot.sendMessage(msg.chat.id, `
?? *Running Process*
• Command: \`${processInfo.command}\`
• Duration: ${duration}ms
• User: \`${processInfo.userId}\`
      `.trim(), { parse_mode: 'Markdown' });
    } else {
      await bot.sendMessage(msg.chat.id, '?? No running process.');
    }
  }

  if (type === 'system') {
    const os = getOS();
    await bot.sendMessage(msg.chat.id, `
??? *System Status*
• OS: ${os}
• Node: ${process.version}
• Uptime: ${Math.floor(process.uptime() / 60)} minutes
• Memory: ${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB
      `.trim(), { parse_mode: 'Markdown' });
  }
});

bot.onText(/\/stop/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const processInfo = terminalService.getActiveProcess(chatId);
  
  if (processInfo) {
    terminalService.killProcess(chatId);
    await bot.sendMessage(msg.chat.id, '?? Process stopped.');
  } else {
    await bot.sendMessage(msg.chat.id, '?? No running process to stop.');
  }
});

bot.onText(/\/cd(?:\s+(.*))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const newCwd = match?.[1]?.trim();
  if (!newCwd) {
    await bot.sendMessage(msg.chat.id, '? Please provide a directory path.\nUsage: /cd <directory>');
    return;
  }

  const chatId = msg.chat.id.toString();
  const session = terminalService.getOrCreateSession(chatId);
  
  if (terminalService.changeDirectory(chatId, newCwd)) {
    await bot.sendMessage(msg.chat.id, `? Changed directory to: \`${session.cwd}\``, { parse_mode: 'Markdown' });
  } else {
    await bot.sendMessage(msg.chat.id, `? Failed to change directory to: ${newCwd}\nDirectory may not exist or is not accessible.`);
  }
});

bot.onText(/\/history(?:\s+(\d+))?(?:\s+--clear)?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const lines = parseInt(match?.[1] || '10', 10);
  const clear = (msg.text || '').includes('--clear');

  if (clear) {
    terminalService.clearHistory(chatId);
    await bot.sendMessage(msg.chat.id, '??? History cleared.');
    return;
  }

  const history = terminalService.getHistory(chatId);
  if (history.length === 0) {
    await bot.sendMessage(msg.chat.id, '?? No command history.');
    return;
  }

  const recentCommands = history.slice(-lines);
  let response = `?? *Command History* (last ${recentCommands.length}):\n\n`;
  recentCommands.forEach((cmd, i) => {
    response += `${i + 1}. \`${cmd}\`\n`;
  });
  await bot.sendMessage(msg.chat.id, response, { parse_mode: 'Markdown' });
});

bot.onText(/\/logs(?:\s+(\d+))?/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const limit = parseInt(match?.[1] || '10', 10);
  const logs = securityService.getRecentLogs(limit);

  if (logs.length === 0) {
    await bot.sendMessage(msg.chat.id, '?? No command logs.');
    return;
  }

  let response = `?? *Recent Logs* (last ${logs.length}):\n\n`;
  logs.slice(0, 5).forEach((log) => {
    const status = log.status === 'success' ? '?' : log.status === 'failed' ? '?' : '??';
    const time = new Date(log.timestamp).toLocaleTimeString();
    response += `${status} [${time}] \`${log.command}\` (${log.status})\n`;
  });
  await bot.sendMessage(msg.chat.id, response, { parse_mode: 'Markdown' });
});

bot.onText(/\/run\b\s+(.+)/, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const command = match?.[1]?.trim();
  if (!command) {
    await bot.sendMessage(msg.chat.id, '? Please provide a command.\nUsage: /run <command>');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();

  const validation = securityService.validateCommand(command);
  if (!validation.valid) {
    await bot.sendMessage(msg.chat.id, `? ${validation.reason}`);
    return;
  }

  const cooldown = securityService.checkCooldown(userId);
  if (!cooldown.allowed) {
    await bot.sendMessage(msg.chat.id, `? Please wait ${Math.ceil((cooldown.remainingMs || 0) / 1000)} seconds before running another command.`);
    return;
  }

  const resolvedCommand = securityService.resolveAlias(command);
  const session = terminalService.getOrCreateSession(chatId);

  if (terminalService.isProcessRunning(chatId)) {
    await bot.sendMessage(msg.chat.id, '?? A process is already running. Please stop it first with /stop.');
    return;
  }

  const sentMessage = await bot.sendMessage(chatId, [
    '*Executing command*',
    '',
    `CWD: \`${session.cwd}\``,
    `Command: \`${resolvedCommand}\``,
    'Status: Running...'
  ].join('\n'), {
    parse_mode: 'Markdown',
    reply_markup: { inline_keyboard: [[{ text: 'Stop', callback_data: `stop_${chatId}` }]] }
  });

  const outputs: string[] = [];
  const startTime = Date.now();

  try {
    await terminalService.executeCommand(
      chatId,
      userId,
      resolvedCommand,
      (data, type) => {
        const prefix = type === 'stderr' ? '? ' : '';
        outputs.push(prefix + data);
      },
      async (exitCode, signal) => {
        const duration = Date.now() - startTime;
        
        securityService.logCommand({
          userId,
          channelId: chatId,
          command: resolvedCommand,
          status: exitCode === 0 ? 'success' : 'failed',
          duration,
        });

        const combinedOutput = outputs.join('');
        const truncated = truncateOutput(combinedOutput, 3950);

        let statusText = 'Completed';
        if (signal) {
          statusText = 'Stopped';
        } else if (exitCode !== 0) {
          statusText = 'Failed';
        }

        await safeEditMessageText([
          `*Command ${statusText}*`,
          '',
          `CWD: \`${session.cwd}\``,
          `Command: \`${resolvedCommand}\``,
          `Exit code: ${exitCode ?? signal ?? 'N/A'}`,
          `Duration: ${duration}ms`,
          '',
          '```',
          truncated || 'No output',
          '```'
        ].join('\n'), {
          chat_id: chatId,
          message_id: sentMessage.message_id,
          parse_mode: 'Markdown',
          reply_markup: { inline_keyboard: [] }
        });
      }
    );
  } catch (error: any) {
    await safeEditMessageText([
      '*Error*',
      '',
      `Command: \`${command}\``,
      `Error: ${error.message}`,
    ].join('\n'), {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: [] }
    });
  }
});

bot.onText(/\/chat(?:\s+--path\s+(\S+))?(?:\s+--clear)?\s*(.*)?$/s, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const workdir = match?.[1] || process.cwd();
  const clear = (msg.text || '').includes('--clear');
  const message = match?.[2]?.trim();
  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const sessionKey = `${chatId}_${userId}`;
  const draft = chatDrafts.get(sessionKey);

  if (clear) {
    chatSessions.delete(sessionKey);
    chatDrafts.delete(sessionKey);
    await bot.sendMessage(chatId, 'Chat history cleared.');
    return;
  }

  if (!message) {
    await bot.sendMessage(chatId, '? Please provide a message.\nUsage: /chat <message> [--path directory] [--clear]');
    return;
  }

  let session = chatSessions.get(sessionKey);
  if (!session) {
    session = { workdir };
    chatSessions.set(sessionKey, session);
  } else if (workdir !== session.workdir) {
    session.workdir = workdir;
  }

  const composedMessage = draft?.parts?.length
    ? [...draft.parts, message].join('\n\n')
    : message;
  chatDrafts.delete(sessionKey);

  await runTelegramChatPrompt(chatId, userId, session.workdir, composedMessage);
});

bot.onText(/\/chatadd(?:\s+--path\s+(\S+))?\s+(.+)$/s, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const workdir = match?.[1] || process.cwd();
  const content = match?.[2]?.trim();
  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const sessionKey = `${chatId}_${userId}`;

  if (!content) {
    await bot.sendMessage(chatId, 'Please provide text to add.\nUsage: /chatadd <text> [--path directory]');
    return;
  }

  const existing = chatDrafts.get(sessionKey);
  if (!existing) {
    chatDrafts.set(sessionKey, { parts: [content], workdir });
  } else {
    existing.parts.push(content);
    if (workdir) existing.workdir = workdir;
  }

  const current = chatDrafts.get(sessionKey)!;
  await bot.sendMessage(chatId, `Added to draft.\nParts: ${current.parts.length}\nUse /chatend to send all parts.`);
});

bot.onText(/\/chatend(?:\s+--path\s+(\S+))?(?:\s+--clear)?\s*$/s, async (msg: Message, match: RegExpExecArray | null) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const overrideWorkdir = match?.[1]?.trim();
  const clearOnly = (msg.text || '').includes('--clear');
  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const sessionKey = `${chatId}_${userId}`;
  const draft = chatDrafts.get(sessionKey);

  if (!draft || draft.parts.length === 0) {
    await bot.sendMessage(chatId, 'No pending draft. Use /chatadd <text> first.');
    return;
  }

  if (clearOnly) {
    chatDrafts.delete(sessionKey);
    await bot.sendMessage(chatId, 'Draft cleared.');
    return;
  }

  const workdir = overrideWorkdir || draft.workdir || process.cwd();
  let session = chatSessions.get(sessionKey);
  if (!session) {
    session = { workdir };
    chatSessions.set(sessionKey, session);
  } else {
    session.workdir = workdir;
  }

  const prompt = draft.parts.join('\n\n');
  chatDrafts.delete(sessionKey);
  await runTelegramChatPrompt(chatId, userId, session.workdir, prompt);
});

bot.onText(/\/autocode(?:\s+.*)?$/s, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const sessionKey = `${chatId}_${userId}`;
  const parsed = parseAutoCodeInput(msg.text || '');

  if (!parsed.goal) {
    await bot.sendMessage(chatId, 'Usage: /autocode <goal> [--steps N] [--path directory]');
    return;
  }

  const existing = autoCodeStates.get(sessionKey);
  if (existing?.running) {
    await bot.sendMessage(chatId, 'AutoCode is already running. Use /autocodestatus or /autocodestop.');
    return;
  }

  const session = chatSessions.get(sessionKey) || { workdir: parsed.workdir || process.cwd() };
  if (parsed.workdir) {
    session.workdir = parsed.workdir;
  }
  chatSessions.set(sessionKey, session);

  const state: AutoCodeState = {
    running: true,
    stopRequested: false,
    startedAt: Date.now(),
    currentStep: 0,
    maxSteps: parsed.steps,
    goal: parsed.goal,
    workdir: session.workdir,
    lastSummary: '',
  };
  autoCodeStates.set(sessionKey, state);

  void runAutoCodeLoop(chatId, userId, sessionKey, state).catch(async (error) => {
    const current = autoCodeStates.get(sessionKey);
    if (current) {
      current.running = false;
      autoCodeStates.set(sessionKey, current);
    }
    await bot.sendMessage(chatId, `AutoCode failed: ${normalizeErrorMessage(error)}`);
  });
});

bot.onText(/\/autocodestatus\b/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const sessionKey = `${chatId}_${userId}`;
  const state = autoCodeStates.get(sessionKey);

  if (!state) {
    await bot.sendMessage(chatId, 'No AutoCode session for this chat.');
    return;
  }

  await bot.sendMessage(chatId, renderAutoCodeStatus(state), { parse_mode: 'Markdown' });
});

bot.onText(/\/autocodestop\b/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const sessionKey = `${chatId}_${userId}`;
  const state = autoCodeStates.get(sessionKey);

  if (!state || !state.running) {
    await bot.sendMessage(chatId, 'AutoCode is not running.');
    return;
  }

  state.stopRequested = true;
  autoCodeStates.set(sessionKey, state);
  await bot.sendMessage(chatId, 'Stopping AutoCode...');
});

bot.onText(/\/ai\b/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  await bot.sendMessage(msg.chat.id, 'Command /ai has been removed. Use /chat, /chatadd, and /chatend instead.');
});

bot.on('callback_query', async (query: CallbackQuery) => {
  const chatId = query.message?.chat.id.toString();
  const userId = query.from.id;
  const data = query.data;

  if (!chatId || !data) return;

  if (!isAuthorized(userId)) {
    await bot.answerCallbackQuery(query.id, { text: '? Unauthorized' });
    return;
  }

  const parts = data.split('_');
  const action = parts[0];
  const channelId = parts.slice(1).join('_');

if (action === 'stop') {
    const processInfo = terminalService.getActiveProcess(channelId);
    if (processInfo) {
      terminalService.killProcess(channelId);
      await bot.answerCallbackQuery(query.id, { text: 'Process stopped' });
      await safeEditMessageReplyMarkup({ inline_keyboard: [] }, { chat_id: query.message?.chat.id, message_id: query.message?.message_id });
    } else {
      await bot.answerCallbackQuery(query.id, { text: 'No running process' });
    }
  }


  if (action === 'sessionpick') {
    const selectedChannelId = data.slice('sessionpick_'.length);
    const session = terminalService.getSession(selectedChannelId);
    await bot.answerCallbackQuery(query.id);
    await bot.sendMessage(query.message!.chat.id, session
      ? `?? *Session*\n\nChannel: \`${session.channelId}\`\nCWD: \`${session.cwd}\`\nHistory: ${session.history.length}\nActive Process: ${session.activeProcess ? 'Yes' : 'No'}`
      : '?? Session not found.', { parse_mode: 'Markdown' });
    return;
  }

  if (action === 'sessionclearcurrent') {
    const targetChannelId = data.slice('sessionclearcurrent_'.length);
    const existing = terminalService.getSession(targetChannelId);
    await bot.answerCallbackQuery(query.id, { text: existing ? 'Session cleared' : 'Session not found' });
    if (!existing) {
      await bot.sendMessage(query.message!.chat.id, '?? Session not found.');
      return;
    }
    terminalService.destroySession(targetChannelId);
    await bot.sendMessage(query.message!.chat.id, `?? Cleared session \`${targetChannelId}\`.`, { parse_mode: 'Markdown' });
    return;
  }

  if (data === 'sessionclearall_global') {
    const cleared = terminalService.clearAllSessions();
    await bot.answerCallbackQuery(query.id, { text: 'All sessions cleared' });
    await bot.sendMessage(query.message!.chat.id, `?? Cleared ${cleared} terminal session${cleared === 1 ? '' : 's'}.`);
    return;
  }
});

bot.onText(/\/syncproject\b/, async (msg: Message) => {
  if (!isAuthorized(msg.from!.id)) {
    await bot.sendMessage(msg.chat.id, '? You are not authorized to use this bot.');
    return;
  }

  const chatId = msg.chat.id.toString();
  const userId = msg.from!.id.toString();
  const session = terminalService.getOrCreateSession(chatId);

  if (!androidService.isAndroidProject(session.cwd)) {
    await bot.sendMessage(msg.chat.id, `? Current directory is not an Android project.\nPath: \`${session.cwd}\``, { parse_mode: 'Markdown' });
    return;
  }

  const cooldown = securityService.checkCooldown(userId);
  if (!cooldown.allowed) {
    await bot.sendMessage(msg.chat.id, `? Please wait ${Math.ceil((cooldown.remainingMs || 0) / 1000)} seconds before running another command.`);
    return;
  }

  if (terminalService.isProcessRunning(chatId)) {
    await bot.sendMessage(msg.chat.id, '?? A process is already running. Please stop it first with /stop.');
    return;
  }

  const plan = androidService.buildSyncProjectCommand(session.cwd);
  const sentMessage = await bot.sendMessage(chatId, `
? *Syncing Android Project*

?? CWD: \`${session.cwd}\`
?? Runner: \`${plan.runner}\`
?? Status: Running Gradle sync...
  `.trim(), { parse_mode: 'Markdown', reply_markup: { inline_keyboard: [[{ text: '?? Stop', callback_data: `stop_${chatId}` }]] } });

  const outputs: string[] = [];
  const startTime = Date.now();

  try {
    await terminalService.executeCommand(
      chatId,
      userId,
      plan.command,
      (data, type) => {
        const prefix = type === 'stderr' ? '? ' : '';
        outputs.push(prefix + data);
      },
      async (exitCode, signal) => {
        const duration = Date.now() - startTime;
        const combinedOutput = outputs.join('');
        const truncated = truncateOutput(combinedOutput, 3950);

        let statusEmoji = '?';
        let statusText = 'Synced';
        if (signal) {
          statusEmoji = '??';
          statusText = 'Stopped';
        } else if (exitCode !== 0) {
          statusEmoji = '?';
          statusText = 'Failed';
        }

        await safeEditMessageText(`
${statusEmoji} *Android Project ${statusText}*

?? CWD: \`${session.cwd}\`
?? Runner: \`${plan.runner}\`
?? Exit Code: ${exitCode ?? signal ?? 'N/A'}
?? Duration: ${duration}ms

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
    await safeEditMessageText(`
? *Error*

?? CWD: \`${session.cwd}\`
?? Error: ${error.message}
    `.trim(), {
      chat_id: chatId,
      message_id: sentMessage.message_id,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard: [] }
    });
  }
});

bot.on('polling_error', (error) => {
  loggerService.error('Telegram polling error', { error: error.message });
});

process.on('unhandledRejection', (reason) => {
  loggerService.error('Unhandled Rejection', { reason: String(reason) });
});

process.on('uncaughtException', (error) => {
  loggerService.error('Uncaught Exception', { error: error.message, stack: error.stack });
  process.exit(1);
});

process.on('SIGINT', async () => {
  stopEventLoopWatchdog();
  loggerService.info('Received SIGINT, shutting down...');
  await aiService.shutdown();
  loggerService.shutdown();
  bot.stopPolling();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  stopEventLoopWatchdog();
  loggerService.info('Received SIGTERM, shutting down...');
  await aiService.shutdown();
  loggerService.shutdown();
  bot.stopPolling();
  process.exit(0);
});

export default bot;
