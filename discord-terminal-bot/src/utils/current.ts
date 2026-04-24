import { execFileSync } from 'child_process';
import { aiService, memoryService, terminalService } from '../services';
import { getOpenCodeLauncher } from './index';

function extractValue(output: string, patterns: RegExp[]): string | null {
  for (const pattern of patterns) {
    const match = output.match(pattern);
    if (match?.[1]) {
      return match[1].trim();
    }
  }
  return null;
}

export interface CurrentContext {
  channelId: string;
  currentCLI: ReturnType<typeof aiService.getCLI>;
  currentModel: string;
  cwd: string;
  projectName: string | null;
  projectType: string | null;
  aiStatus: string;
  tokenUsed: string | null;
  tokenRemaining: string | null;
  tokenLimit: string | null;
  latestTrackedTotalTokens: number | null;
}

export function buildCurrentContext(channelId: string): CurrentContext {
  const currentCLI = aiService.getCLI();
  const currentModel = aiService.getDefaultModel(currentCLI);
  const cwd = terminalService.getSession(channelId)?.cwd || process.cwd();
  const project = memoryService.getProjectByPath(cwd);
  const aiStatus = aiService.getCliRuntimeStatus(currentCLI);

  let tokenUsed: string | null = null;
  let tokenRemaining: string | null = null;
  let tokenLimit: string | null = null;

  if (currentCLI === 'opencode') {
    try {
      const launcher = getOpenCodeLauncher();
      const output = execFileSync(launcher.command, [...launcher.args, 'stats'], {
        encoding: 'utf-8',
        windowsHide: true,
      });
      tokenUsed = extractValue(output, [/Used\s+([^\n]+)/i, /Usage\s+([^\n]+)/i]);
      tokenRemaining = extractValue(output, [/Remaining\s+([^\n]+)/i, /Left\s+([^\n]+)/i, /Balance\s+([^\n]+)/i]);
      tokenLimit = extractValue(output, [/Limit\s+([^\n]+)/i, /Quota\s+([^\n]+)/i]);
    } catch {
      // Ignore token lookup failures in status view.
    }
  }

  const latestUsage = memoryService.getAIUsageHistory(20, currentCLI).find((item) => item.cwd === cwd);

  return {
    channelId,
    currentCLI,
    currentModel,
    cwd,
    projectName: project?.name || null,
    projectType: project?.type || null,
    aiStatus,
    tokenUsed,
    tokenRemaining,
    tokenLimit,
    latestTrackedTotalTokens: latestUsage?.totalTokens || null,
  };
}
