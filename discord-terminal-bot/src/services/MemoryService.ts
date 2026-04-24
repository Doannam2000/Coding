import fs from 'fs';
import path from 'path';
import { loggerService } from './LoggerService';
import type { SupportedCLI } from './AIService';

export interface ProjectRecord {
  path: string;
  name: string;
  type: 'android' | 'node' | 'python' | 'other';
  applicationId?: string;
  preferredCLI?: SupportedCLI;
  preferredModel?: string;
  lastAccessed: string;
  commands: string[];
  lastActivity: string;
  lastReadme?: string;
  contextLoaded?: string;
}

export interface CommandRecord {
  command: string;
  cwd: string;
  exitCode: number | null;
  duration: number;
  timestamp: string;
  userId: string;
  channelId: string;
  output?: string;
}

export interface MarkdownFile {
  filename: string;
  path: string;
  content: string;
  size: number;
}

export interface TokenSnapshot {
  timestamp: string;
  used?: string | null;
  remaining?: string | null;
  limit?: string | null;
  totalCost?: string | null;
  inputTokens?: string | null;
  outputTokens?: string | null;
  totalTokens?: string | null;
  usedPercent?: number | null;
  remainingPercent?: number | null;
  modelBreakdown?: string[];
}

export interface TokenWatchConfig {
  enabled: boolean;
  thresholdPercent: number;
  telegramChats: string[];
  discordChannels: string[];
  lastAlertAt?: string;
}

export interface AIUsageSnapshot {
  timestamp: string;
  cli: SupportedCLI;
  model: string;
  mode: 'ai' | 'chat';
  channelId: string;
  userId: string;
  cwd?: string;
  inputTokens?: number;
  outputTokens?: number;
  totalTokens?: number;
  cost?: number;
}

export interface MemoryData {
  projects: ProjectRecord[];
  recentCommands: CommandRecord[];
  defaultModel?: string;
  defaultModels?: Partial<Record<SupportedCLI, string>>;
  tokenHistory?: TokenSnapshot[];
  aiUsageHistory?: AIUsageSnapshot[];
  tokenWatch?: TokenWatchConfig;
  botStats: {
    totalCommands: number;
    totalProjects: number;
    uptime: string;
    startTime: string;
  };
  lastUpdated: string;
}

export class MemoryService {
  private data: MemoryData;
  private dataPath: string;
  private maxRecentCommands = 100;
  private maxCommandsPerProject = 50;
  private readonly agentDocFileName = 'agent.md';
  private readonly projectDocFileName = 'project.md';
  private readonly fallbackAgentDocTemplate = `# agent.md (ULTRA BUILDER + GENERATOR MODE)

## ROLE
You are an elite Android Architect + UI/UX Designer + Product Builder.

You generate complete, production-ready Android apps using Kotlin + Jetpack Compose.

You NEVER:
- generate demo apps
- generate incomplete code
- generate broken UI

You ALWAYS:
- build real apps
- ensure UI is clean and balanced
- ensure full navigation flow
- ensure all states handled

---

## CORE STACK (MANDATORY)
- Kotlin
- Jetpack Compose (Material 3)
- MVVM
- Navigation Compose
- StateFlow
- Hilt
- Coroutines
- Retrofit
- DataStore
- Coil

---

## BUILD MODE

You support 2 modes:

### MODE 1 - FULL BUILD
Generate full Android project with:
- all screens
- navigation
- UI components
- sample data
- state handling

### MODE 2 - FEATURE BUILD
Generate:
- 1 feature module
- with ViewModel + UI + state

---

## UI RULE ENGINE (STRICT)

You must enforce:

### Layout safety
- No overlapping UI
- No clipped text
- No hardcoded widths (unless justified)
- Always responsive

### Text rules
- maxLines applied
- ellipsis when needed
- avoid long unwrapped text

### Spacing system
4 / 8 / 12 / 16 / 20 / 24 dp

### Button rules
- proper height (48-56dp)
- balanced padding
- no edge collision
- loading + disabled states

---

## REQUIRED SCREENS

Always include:

- Splash
- Intro
- Home
- Search
- Detail
- Favorites
- Notifications
- Profile
- Settings
- About
- Exit Dialog
- Empty / Error / Loading states

---

## NAVIGATION FLOW

Splash ->
  first time -> Intro -> Home
  else -> Home

Home:
- entry to all features

Settings:
- theme
- notifications
- about
- logout
- exit

Exit:
- must confirm

---

## STATE SYSTEM

Each screen:
- Loading
- Success
- Empty
- Error

Use sealed class UiState

---

## COMPONENT LIBRARY (REQUIRED)

- AppTopBar
- PrimaryButton
- SecondaryButton
- AppCard
- SearchBar
- SectionHeader
- EmptyStateView
- ErrorStateView
- LoadingView
- SettingItem
- ConfirmationDialog

---

## FILE STRUCTURE

data/
domain/
ui/
viewmodel/
di/

---

## DESIGN STYLE

- modern
- minimal
- premium
- clean
- soft UI
- strong hierarchy

---

## OUTPUT RULE

When building:

- always full runnable code
- no TODO
- no pseudo
- include navigation
- include theme
- include preview where useful

---

## FINAL CHECK

Before output:

Check:
- small screen safe
- text not overflow
- buttons not broken
- spacing consistent

Fix everything before finalizing.

---

## INPUT FORMAT (FROM GENERATOR)

You will receive input like:

{
  "app_name": "...",
  "idea": "...",
  "target_users": "...",
  "features": [...],
  "style": "...",
  "complexity": "simple | medium | advanced"
}

You must:
- interpret it
- expand into full Android app
- generate clean architecture code
`;
  private readonly openCodeLegacyAliasMap: Record<string, string> = {
    bigpickle: 'opencode/big-pickle',
    'big-pickle': 'opencode/big-pickle',
    minimax: 'opencode/minimax-m2.5-free',
    'minimax-m2.5-free': 'opencode/minimax-m2.5-free',
    nemotron: 'opencode/nemotron-3-super-free',
    'nemotron-3-super-free': 'opencode/nemotron-3-super-free',
  };

  constructor() {
    this.dataPath = path.join(process.cwd(), 'memory.json');
    this.data = this.loadData();
  }

  private loadData(): MemoryData {
    try {
      if (fs.existsSync(this.dataPath)) {
        const raw = fs.readFileSync(this.dataPath, 'utf-8');
        return JSON.parse(raw);
      }
    } catch (error) {
      loggerService.warn('Failed to load memory data', { error: String(error) });
    }

    return {
      projects: [],
      recentCommands: [],
      defaultModel: 'opencode/big-pickle',
      defaultModels: {
        opencode: 'opencode/big-pickle',
        claude: 'sonnet',
        codex: 'gpt-5.3-codex',
      },
      tokenHistory: [],
      aiUsageHistory: [],
      tokenWatch: {
        enabled: false,
        thresholdPercent: 20,
        telegramChats: [],
        discordChannels: [],
      },
      botStats: {
        totalCommands: 0,
        totalProjects: 0,
        uptime: '0h',
        startTime: new Date().toISOString(),
      },
      lastUpdated: new Date().toISOString(),
    };
  }

  private saveData(): void {
    try {
      this.data.lastUpdated = new Date().toISOString();
      this.data.botStats.uptime = this.calculateUptime();
      fs.writeFileSync(this.dataPath, JSON.stringify(this.data, null, 2));
    } catch (error) {
      loggerService.error('Failed to save memory data', { error: String(error) });
    }
  }

  private calculateUptime(): string {
    const start = new Date(this.data.botStats.startTime).getTime();
    const now = Date.now();
    const diff = Math.floor((now - start) / 1000 / 60 / 60);
    return `${diff}h`;
  }

  private inferLegacyDefaultModels(): Record<SupportedCLI, string> {
    const legacy = this.data.defaultModel;
    const defaults: Record<SupportedCLI, string> = {
      opencode: 'opencode/big-pickle',
      claude: 'sonnet',
      codex: 'gpt-5.3-codex',
    };

    if (!legacy) {
      return defaults;
    }

    if (legacy.includes('claude') || legacy === 'sonnet' || legacy === 'opus') {
      defaults.claude = legacy.replace(/^anthropic\//, '');
      return defaults;
    }

    if (legacy.includes('codex') || legacy.startsWith('gpt-')) {
      defaults.codex = legacy.replace(/^openai\//, '').split('#')[0];
      return defaults;
    }

    if (legacy === 'bigpickle' || legacy === 'minimax' || legacy === 'nemotron' || legacy.includes('/')) {
      defaults.opencode = this.normalizeOpenCodeModel(legacy);
      return defaults;
    }

    defaults.codex = legacy.replace(/^openai\//, '').split('#')[0];
    return defaults;
  }

  private sanitizeDefaultModelForCli(cli: SupportedCLI, model: string): string {
    if (!model) {
      return this.inferLegacyDefaultModels()[cli];
    }

    if (cli === 'opencode') {
      if (model.includes('codex') || model === 'sonnet' || model === 'opus' || model.startsWith('gpt-')) {
        return 'opencode/big-pickle';
      }
      return this.normalizeOpenCodeModel(model);
    }

    if (cli === 'claude') {
      if (model.startsWith('opencode/') || model === 'bigpickle' || model === 'minimax' || model === 'nemotron') {
        return 'sonnet';
      }
      return model.replace(/^anthropic\//, '').split('#')[0];
    }

    if (model.startsWith('opencode/') || model === 'bigpickle' || model === 'minimax' || model === 'nemotron') {
      return 'gpt-5.3-codex';
    }

    return model.replace(/^openai\//, '').split('#')[0];
  }

  private normalizeOpenCodeModel(model: string): string {
    const trimmed = model.trim();
    if (!trimmed) return 'opencode/big-pickle';

    const [rawModel, variant] = trimmed.split('#');
    const normalizedModel =
      this.openCodeLegacyAliasMap[rawModel] ||
      this.openCodeLegacyAliasMap[rawModel.toLowerCase()] ||
      rawModel;

    return variant ? `${normalizedModel}#${variant}` : normalizedModel;
  }

  private getAgentDocTemplate(): string {
    const rootTemplatePath = path.join(process.cwd(), this.agentDocFileName);
    try {
      if (fs.existsSync(rootTemplatePath)) {
        const content = fs.readFileSync(rootTemplatePath, 'utf-8').trim();
        if (content) return content;
      }
    } catch (error) {
      loggerService.warn('Failed to load root agent.md template, using fallback', { error: String(error) });
    }

    return this.fallbackAgentDocTemplate;
  }

  private buildProjectDoc(projectPath: string, projectType: 'android' | 'node' | 'python' | 'other'): string {
    const projectName = path.basename(projectPath);
    const lines = [
      '# project.md - Core App Notes',
      '',
      '## App Identity',
      `- Name: ${projectName}`,
      `- Path: ${projectPath}`,
      `- Type: ${projectType}`,
      '',
      '## Core Features',
      '- Document the main user-facing features here.',
      '- Keep only important and core features (no minor details).',
      '',
      '## Core Architecture',
      '- Document key modules/services and their responsibilities.',
      '- Note important data flow and state management decisions.',
      '',
      '## Important Decisions',
      '- Record major behavior changes, breaking changes, and critical fixes.',
      '',
      '## Integrations',
      '- APIs, SDKs, platform services, auth, and storage that are critical to the app.',
      '',
      '## Reliability Notes',
      '- Error handling, retry behavior, and recovery logic for critical paths.',
    ];

    return `${lines.join('\n')}\n`;
  }

  private ensureProjectDocs(projectPath: string, projectType: 'android' | 'node' | 'python' | 'other'): void {
    try {
      if (!fs.existsSync(projectPath)) return;

      const agentDocPath = path.join(projectPath, this.agentDocFileName);
      if (!fs.existsSync(agentDocPath)) {
        fs.writeFileSync(agentDocPath, `${this.getAgentDocTemplate().trim()}\n`, 'utf-8');
        loggerService.info('Created missing agent.md for project', { projectPath });
      }

      const projectDocPath = path.join(projectPath, this.projectDocFileName);
      if (!fs.existsSync(projectDocPath)) {
        fs.writeFileSync(projectDocPath, this.buildProjectDoc(projectPath, projectType), 'utf-8');
        loggerService.info('Created missing project.md for project', { projectPath });
      }
    } catch (error) {
      loggerService.warn('Failed to ensure project docs', { projectPath, error: String(error) });
    }
  }

  detectProjectType(cwd: string): 'android' | 'node' | 'python' | 'other' {
    if (fs.existsSync(path.join(cwd, 'build.gradle')) || 
        fs.existsSync(path.join(cwd, 'build.gradle.kts')) ||
        fs.existsSync(path.join(cwd, 'app', 'build.gradle')) ||
        fs.existsSync(path.join(cwd, 'app', 'build.gradle.kts'))) {
      return 'android';
    }
    if (fs.existsSync(path.join(cwd, 'package.json'))) {
      return 'node';
    }
    if (fs.existsSync(path.join(cwd, 'requirements.txt')) ||
        fs.existsSync(path.join(cwd, 'pyproject.toml')) ||
        fs.existsSync(path.join(cwd, 'setup.py'))) {
      return 'python';
    }
    return 'other';
  }

  extractAndroidAppId(cwd: string): string | undefined {
    const gradlePaths = [
      path.join(cwd, 'app', 'build.gradle'),
      path.join(cwd, 'app', 'build.gradle.kts'),
      path.join(cwd, 'build.gradle'),
      path.join(cwd, 'build.gradle.kts'),
    ];

    for (const gradlePath of gradlePaths) {
      if (fs.existsSync(gradlePath)) {
        try {
          const content = fs.readFileSync(gradlePath, 'utf-8');
          const match = content.match(/applicationId\s*(?:=)?\s*["']([^"']+)["']/);
          if (match) return match[1];
          
          const namespaceMatch = content.match(/namespace\s*(?:=)?\s*["']([^"']+)["']/);
          if (namespaceMatch) return namespaceMatch[1];
        } catch {}
      }
    }
    return undefined;
  }

  trackCommand(
    command: string,
    cwd: string,
    exitCode: number | null,
    duration: number,
    userId: string,
    channelId: string,
    output?: string
  ): void {
    const record: CommandRecord = {
      command,
      cwd,
      exitCode,
      duration,
      timestamp: new Date().toISOString(),
      userId,
      channelId,
      output: output?.slice(-500),
    };

    this.data.recentCommands.unshift(record);
    if (this.data.recentCommands.length > this.maxRecentCommands) {
      this.data.recentCommands = this.data.recentCommands.slice(0, this.maxRecentCommands);
    }

    this.data.botStats.totalCommands++;
    this.trackProject(cwd, command);
    this.saveData();
  }

  trackProject(cwd: string, command: string): void {
    const existing = this.data.projects.find(p => p.path === cwd);
    
    if (existing) {
      existing.lastActivity = new Date().toISOString();
      existing.lastAccessed = new Date().toISOString();
      existing.commands.push(command);
      if (existing.commands.length > this.maxCommandsPerProject) {
        existing.commands = existing.commands.slice(-this.maxCommandsPerProject);
      }
    } else {
      const projectType = this.detectProjectType(cwd);
      const project: ProjectRecord = {
        path: cwd,
        name: path.basename(cwd),
        type: projectType,
        applicationId: projectType === 'android' ? this.extractAndroidAppId(cwd) : undefined,
        lastAccessed: new Date().toISOString(),
        commands: [command],
        lastActivity: new Date().toISOString(),
      };
      this.data.projects.push(project);
      this.data.botStats.totalProjects = this.data.projects.length;
    }

    this.ensureProjectDocs(cwd, this.detectProjectType(cwd));
  }

  addProject(projectPath: string, command: string = 'manual_add', projectName?: string): { project: ProjectRecord; created: boolean } {
    const normalizedPath = path.resolve(projectPath);
    const targetPathLower = normalizedPath.toLowerCase();
    const now = new Date().toISOString();

    let project = this.data.projects.find(p => p.path.toLowerCase() === targetPathLower);
    let created = false;

    if (!project) {
      const projectType = this.detectProjectType(normalizedPath);
      project = {
        path: normalizedPath,
        name: projectName?.trim() || path.basename(normalizedPath),
        type: projectType,
        applicationId: projectType === 'android' ? this.extractAndroidAppId(normalizedPath) : undefined,
        lastAccessed: now,
        commands: command ? [command] : [],
        lastActivity: now,
      };
      this.data.projects.push(project);
      created = true;
    } else {
      project.lastAccessed = now;
      project.lastActivity = now;
      if (projectName?.trim()) {
        project.name = projectName.trim();
      }
      if (command) {
        project.commands.push(command);
        if (project.commands.length > this.maxCommandsPerProject) {
          project.commands = project.commands.slice(-this.maxCommandsPerProject);
        }
      }
    }

    this.data.botStats.totalProjects = this.data.projects.length;
    this.ensureProjectDocs(project.path, project.type);
    this.saveData();
    return { project, created };
  }

  updateProjectPath(projectPath: string, newPath: string): boolean {
    const projectPathLower = path.resolve(projectPath).toLowerCase();
    const normalizedNewPath = path.resolve(newPath);
    const newPathLower = normalizedNewPath.toLowerCase();

    const project = this.data.projects.find(p => p.path.toLowerCase() === projectPathLower);
    if (!project) {
      return false;
    }

    const duplicate = this.data.projects.find(p => p !== project && p.path.toLowerCase() === newPathLower);
    if (duplicate) {
      return false;
    }

    project.path = normalizedNewPath;
    project.lastAccessed = new Date().toISOString();
    project.lastActivity = new Date().toISOString();
    if (project.type === 'android') {
      project.applicationId = this.extractAndroidAppId(normalizedNewPath);
    }

    this.ensureProjectDocs(project.path, project.type);

    this.saveData();
    return true;
  }

  removeProject(projectNameOrPath: string): ProjectRecord | undefined {
    const needle = projectNameOrPath.trim();
    if (!needle) return undefined;

    const needleLower = needle.toLowerCase();
    const normalizedPathLower = path.resolve(needle).toLowerCase();
    const projectIndex = this.data.projects.findIndex(p =>
      p.name.toLowerCase() === needleLower ||
      p.path.toLowerCase() === needleLower ||
      p.path.toLowerCase() === normalizedPathLower
    );

    if (projectIndex < 0) {
      return undefined;
    }

    const [removed] = this.data.projects.splice(projectIndex, 1);
    this.data.botStats.totalProjects = this.data.projects.length;
    this.saveData();
    return removed;
  }

  updateProjectAppId(projectPath: string, appId: string): void {
    const project = this.data.projects.find(p => p.path === projectPath);
    if (project && project.type === 'android') {
      project.applicationId = appId;
      this.saveData();
      loggerService.info('Updated Android app ID', { projectPath, appId });
    }
  }

  setProjectAISettings(projectPath: string, settings: { cli?: string; model?: string }): void {
    const project = this.data.projects.find(p => p.path === projectPath);
    if (!project) return;
    if (settings.cli) (project as any).preferredCLI = settings.cli;
    if (settings.model) project.preferredModel = settings.model;
    this.saveData();
  }

  setProjectAISettingsByCwd(cwd: string, settings: { cli?: string; model?: string }): void {
    const project = this.getProjectByPath(cwd);
    if (!project) return;
    this.setProjectAISettings(project.path, settings);
  }

  getProjects(): ProjectRecord[] {
    return this.data.projects.sort((a, b) => 
      new Date(b.lastActivity).getTime() - new Date(a.lastActivity).getTime()
    );
  }

  getRecentCommands(limit: number = 20): CommandRecord[] {
    return this.data.recentCommands.slice(0, limit);
  }

  getProjectCommands(projectPath: string, limit: number = 10): string[] {
    const project = this.data.projects.find(p => p.path === projectPath);
    return project ? project.commands.slice(-limit) : [];
  }

  getStats(): MemoryData['botStats'] {
    return { ...this.data.botStats };
  }

  getAllMemory(): MemoryData {
    return { ...this.data };
  }

  getDefaultModel(cli: SupportedCLI = 'opencode'): string {
    const defaults = {
      opencode: 'opencode/big-pickle',
      claude: 'sonnet',
      codex: 'gpt-5.3-codex',
      ...(this.data.defaultModels || this.inferLegacyDefaultModels()),
    };

    return this.sanitizeDefaultModelForCli(cli, defaults[cli]);
  }

  getDefaultModels(): Record<SupportedCLI, string> {
    return {
      opencode: this.getDefaultModel('opencode'),
      claude: this.getDefaultModel('claude'),
      codex: this.getDefaultModel('codex'),
    };
  }

  setDefaultModel(model: string, cli: SupportedCLI = 'opencode'): void {
    const normalizedModel = cli === 'opencode' ? this.normalizeOpenCodeModel(model) : model;
    this.data.defaultModels = {
      ...this.getDefaultModels(),
      ...(this.data.defaultModels || {}),
      [cli]: normalizedModel,
    };
    if (cli === 'opencode') {
      this.data.defaultModel = normalizedModel;
    }
    this.saveData();
  }

  addTokenSnapshot(snapshot: TokenSnapshot): void {
    this.data.tokenHistory = this.data.tokenHistory || [];
    this.data.tokenHistory.unshift(snapshot);
    this.data.tokenHistory = this.data.tokenHistory.slice(0, 200);
    this.saveData();
  }

  addAIUsageSnapshot(snapshot: AIUsageSnapshot): void {
    this.data.aiUsageHistory = this.data.aiUsageHistory || [];
    this.data.aiUsageHistory.unshift(snapshot);
    this.data.aiUsageHistory = this.data.aiUsageHistory.slice(0, 500);
    this.saveData();
  }

  getAIUsageHistory(limit: number = 50, cli?: SupportedCLI): AIUsageSnapshot[] {
    const items = this.data.aiUsageHistory || [];
    const filtered = cli ? items.filter(item => item.cli === cli) : items;
    return filtered.slice(0, limit);
  }

  summarizeAIUsage(days: number = 7, cli?: SupportedCLI): Array<{
    day: string;
    cli: SupportedCLI;
    requests: number;
    totalTokens: number;
    inputTokens: number;
    outputTokens: number;
    cost: number;
    models: string[];
  }> {
    const cutoff = Date.now() - (days * 24 * 60 * 60 * 1000);
    const rows = new Map<string, {
      day: string;
      cli: SupportedCLI;
      requests: number;
      totalTokens: number;
      inputTokens: number;
      outputTokens: number;
      cost: number;
      models: Set<string>;
    }>();

    for (const item of this.data.aiUsageHistory || []) {
      const ts = new Date(item.timestamp).getTime();
      if (Number.isNaN(ts) || ts < cutoff) continue;
      if (cli && item.cli !== cli) continue;

      const day = item.timestamp.slice(0, 10);
      const key = `${day}_${item.cli}`;
      if (!rows.has(key)) {
        rows.set(key, {
          day,
          cli: item.cli,
          requests: 0,
          totalTokens: 0,
          inputTokens: 0,
          outputTokens: 0,
          cost: 0,
          models: new Set<string>(),
        });
      }

      const row = rows.get(key)!;
      row.requests += 1;
      row.totalTokens += item.totalTokens || 0;
      row.inputTokens += item.inputTokens || 0;
      row.outputTokens += item.outputTokens || 0;
      row.cost += item.cost || 0;
      row.models.add(item.model);
    }

    return Array.from(rows.values())
      .sort((a, b) => b.day.localeCompare(a.day))
      .map(row => ({
        day: row.day,
        cli: row.cli,
        requests: row.requests,
        totalTokens: row.totalTokens,
        inputTokens: row.inputTokens,
        outputTokens: row.outputTokens,
        cost: row.cost,
        models: Array.from(row.models).sort(),
      }));
  }

  getTokenHistory(limit: number = 20): TokenSnapshot[] {
    return (this.data.tokenHistory || []).slice(0, limit);
  }

  getTokenWatch(): TokenWatchConfig {
    return this.data.tokenWatch || {
      enabled: false,
      thresholdPercent: 20,
      telegramChats: [],
      discordChannels: [],
    };
  }

  updateTokenWatch(config: Partial<TokenWatchConfig>): TokenWatchConfig {
    const current = this.getTokenWatch();
    this.data.tokenWatch = { ...current, ...config };
    this.saveData();
    return this.data.tokenWatch;
  }

  subscribeTokenWatch(target: { telegramChat?: string; discordChannel?: string }): TokenWatchConfig {
    const current = this.getTokenWatch();
    if (target.telegramChat && !current.telegramChats.includes(target.telegramChat)) {
      current.telegramChats.push(target.telegramChat);
    }
    if (target.discordChannel && !current.discordChannels.includes(target.discordChannel)) {
      current.discordChannels.push(target.discordChannel);
    }
    this.data.tokenWatch = current;
    this.saveData();
    return current;
  }

  unsubscribeTokenWatch(target: { telegramChat?: string; discordChannel?: string }): TokenWatchConfig {
    const current = this.getTokenWatch();
    if (target.telegramChat) {
      current.telegramChats = current.telegramChats.filter(chat => chat !== target.telegramChat);
    }
    if (target.discordChannel) {
      current.discordChannels = current.discordChannels.filter(channel => channel !== target.discordChannel);
    }
    this.data.tokenWatch = current;
    this.saveData();
    return current;
  }

  markTokenAlertSent(): void {
    const current = this.getTokenWatch();
    current.lastAlertAt = new Date().toISOString();
    this.data.tokenWatch = current;
    this.saveData();
  }

  generateProjectAgentsMd(projectPath: string): string {
    const project = this.data.projects.find(p => p.path === projectPath);
    if (!project) return '';

    const mdFiles = this.getProjectMarkdownFiles(projectPath);
    const projectCmds = this.getProjectCommands(projectPath, 20);

    const relevantMdContent = mdFiles
      .filter(f => ['AGENTS.md', 'README.md', 'PROJECT.md'].includes(f.filename))
      .map(f => `## ${f.filename}\n${f.content.slice(0, 2000)}`)
      .join('\n\n');

    const agentsMd = `# Project: ${project.name}

## Project Info
- **Path:** ${project.path}
- **Type:** ${project.type}
- **App ID:** ${project.applicationId || 'N/A'}
- **Last Activity:** ${new Date(project.lastActivity).toLocaleString()}

## Recent Commands (${projectCmds.length})
${projectCmds.map((cmd, i) => `${i + 1}. \`${cmd}\``).join('\n')}

## Documentation
${relevantMdContent || '_No AGENTS.md or README found_'}
`.trim();

    return agentsMd;
  }

  generateAgentsMdForAI(projectPath: string): string {
    const project = this.data.projects.find(p => p.path === projectPath);
    if (!project) return '';

    const mdFiles = this.getProjectMarkdownFiles(projectPath);

    let content = `# Project: ${project.name}\n\n`;
    content += `**Type:** ${project.type}\n`;
    content += `**Path:** ${project.path}\n`;
    if (project.applicationId) content += `**App ID:** ${project.applicationId}\n`;
    content += '\n';

    const agentMd = mdFiles.find(f => f.filename === 'AGENTS.md');
    if (agentMd) {
      content += `## AGENTS.md\n${agentMd.content.slice(0, 3000)}\n\n`;
    }

    const readmeMd = mdFiles.find(f => f.filename === 'README.md');
    if (readmeMd) {
      content += `## README.md\n${readmeMd.content.slice(0, 2000)}\n`;
    }

    return content;
  }

  getProjectMarkdownFiles(projectPath: string): MarkdownFile[] {
    const mdFiles: MarkdownFile[] = [];
    
    try {
      if (!fs.existsSync(projectPath)) return [];

      const files = fs.readdirSync(projectPath);
      for (const file of files) {
        if (file.endsWith('.md')) {
          const filePath = path.join(projectPath, file);
          try {
            const stat = fs.statSync(filePath);
            if (stat.isFile()) {
              const content = fs.readFileSync(filePath, 'utf-8');
              mdFiles.push({
                filename: file,
                path: filePath,
                content,
                size: stat.size,
              });
            }
          } catch {}
        }
      }
    } catch (error) {
      loggerService.warn('Failed to read markdown files', { projectPath, error: String(error) });
    }

    return mdFiles;
  }

  updateProjectReadme(projectPath: string): void {
    const project = this.data.projects.find(p => p.path === projectPath);
    if (project) {
      project.lastReadme = new Date().toISOString();
      this.saveData();
    }
  }

  getProjectByPath(projectPath: string): ProjectRecord | undefined {
    return this.data.projects.find(p => p.path === projectPath);
  }

  clearHistory(): void {
    this.data.recentCommands = [];
    this.saveData();
  }

  clearProjects(): void {
    this.data.projects = [];
    this.data.botStats.totalProjects = 0;
    this.saveData();
  }

  reset(): void {
    this.data = {
      projects: [],
      recentCommands: [],
      defaultModel: 'opencode/big-pickle',
      defaultModels: {
        opencode: 'opencode/big-pickle',
        claude: 'sonnet',
        codex: 'gpt-5.3-codex',
      },
      tokenHistory: [],
      aiUsageHistory: [],
      tokenWatch: {
        enabled: false,
        thresholdPercent: 20,
        telegramChats: [],
        discordChannels: [],
      },
      botStats: {
        totalCommands: 0,
        totalProjects: 0,
        uptime: '0h',
        startTime: new Date().toISOString(),
      },
      lastUpdated: new Date().toISOString(),
    };
    this.saveData();
  }

  saveChatHistory(key: string, messages: Array<{ role: 'user' | 'assistant'; content: string }>): void {
    const chatKey = `chat_${key}`;
    const existing = this.data.recentCommands.find(c => c.command === chatKey);
    
    if (existing && existing.output) {
      try {
        const chatData = JSON.parse(existing.output);
        chatData.messages = messages.slice(-20);
        existing.output = JSON.stringify(chatData);
        existing.timestamp = new Date().toISOString();
      } catch {
        existing.output = JSON.stringify({ messages: messages.slice(-20) });
      }
    } else {
      this.data.recentCommands.unshift({
        command: chatKey,
        cwd: '',
        exitCode: 0,
        duration: 0,
        timestamp: new Date().toISOString(),
        userId: 'system',
        channelId: key,
        output: JSON.stringify({ messages: messages.slice(-20) }),
      });
    }
    
    if (this.data.recentCommands.length > this.maxRecentCommands) {
      this.data.recentCommands = this.data.recentCommands.slice(0, this.maxRecentCommands);
    }
    
    this.saveData();
  }

  loadChatHistory(key: string): Array<{ role: 'user' | 'assistant'; content: string }> | null {
    const chatRecord = this.data.recentCommands.find(c => c.command === `chat_${key}`);
    if (chatRecord && chatRecord.output) {
      try {
        const chatData = JSON.parse(chatRecord.output);
        return chatData.messages || null;
      } catch {
        return null;
      }
    }
    return null;
  }

  clearChatHistory(key: string): boolean {
    const commandKey = `chat_${key}`;
    const before = this.data.recentCommands.length;
    this.data.recentCommands = this.data.recentCommands.filter((record) => record.command !== commandKey);
    const changed = this.data.recentCommands.length !== before;
    if (changed) this.saveData();
    return changed;
  }

  clearChatHistoryByChannel(channelId: string): number {
    const prefix = `chat_${channelId}_`;
    const before = this.data.recentCommands.length;
    this.data.recentCommands = this.data.recentCommands.filter((record) => !record.command.startsWith(prefix));
    const removed = before - this.data.recentCommands.length;
    if (removed > 0) this.saveData();
    return removed;
  }

  updateProjectContextLoaded(projectPath: string): void {
    const project = this.data.projects.find(p => p.path === projectPath);
    if (project) {
      project.contextLoaded = new Date().toISOString();
      this.saveData();
    }
  }
}

export const memoryService = new MemoryService();
