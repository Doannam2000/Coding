import fs from 'fs';
import path from 'path';
import { loggerService } from './LoggerService';
import type { SupportedCLI } from './AIService';

export interface PersistedTerminalSession {
  channelId: string;
  cwd: string;
  history: string[];
  createdAt: string;
  selectedDeviceId?: string;
}

export interface PersistedChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface PersistedChatSession {
  key: string;
  workdir: string;
  messages: PersistedChatMessage[];
}

export interface PersistedProjectContext {
  chatId: string;
  content: string;
  timestamp: number;
  used?: boolean;
}

export interface PersistedCombinedRuntimeState {
  currentCLI?: SupportedCLI;
  chatSessions: PersistedChatSession[];
  projectContexts: PersistedProjectContext[];
  selectedAISessionsByChat?: Array<{
    chatId: string;
    cli: SupportedCLI;
    workdir: string;
    sessionId: string;
  }>;
}

export interface PersistedWriteAccessRequest {
  id: string;
  rootPath: string;
  requestedBy: string;
  chatId: string;
  createdAt: string;
  status: 'pending' | 'approved' | 'denied' | 'failed';
  error?: string;
}

export interface PersistedWriteBrokerState {
  approvedRoots: string[];
  requests: PersistedWriteAccessRequest[];
}

interface RuntimeStateData {
  version: 1;
  terminalSessions: PersistedTerminalSession[];
  combined: PersistedCombinedRuntimeState;
  writeBroker: PersistedWriteBrokerState;
  lastUpdated: string;
}

export class RuntimeStateService {
  private readonly dataPath: string;
  private data: RuntimeStateData;

  constructor() {
    this.dataPath = path.join(process.cwd(), 'runtime-state.json');
    this.data = this.load();
  }

  private buildDefaultState(): RuntimeStateData {
    return {
      version: 1,
      terminalSessions: [],
      combined: {
        currentCLI: 'opencode',
        chatSessions: [],
        projectContexts: [],
      },
      writeBroker: {
        approvedRoots: [],
        requests: [],
      },
      lastUpdated: new Date().toISOString(),
    };
  }

  private loadJsonFile<T>(filePath: string): T | null {
    try {
      if (!fs.existsSync(filePath)) return null;
      return JSON.parse(fs.readFileSync(filePath, 'utf-8')) as T;
    } catch (error) {
      loggerService.warn('Failed to read runtime state source', { filePath, error: String(error) });
      return null;
    }
  }

  private migrateLegacyState(): RuntimeStateData {
    const state = this.buildDefaultState();
    const root = process.cwd();

    const terminalSessions = this.loadJsonFile<PersistedTerminalSession[]>(path.join(root, 'terminal-sessions.json'));
    if (Array.isArray(terminalSessions)) {
      state.terminalSessions = terminalSessions.filter((session) =>
        session &&
        typeof session.channelId === 'string' &&
        typeof session.cwd === 'string' &&
        Array.isArray(session.history) &&
        typeof session.createdAt === 'string'
      );
    }

    const combined = this.loadJsonFile<Partial<PersistedCombinedRuntimeState>>(path.join(root, 'combined-runtime-state.json'));
    if (combined) {
      state.combined = {
        currentCLI: combined.currentCLI,
        chatSessions: Array.isArray(combined.chatSessions) ? combined.chatSessions.filter((session) =>
          session &&
          typeof session.key === 'string' &&
          typeof session.workdir === 'string' &&
          Array.isArray(session.messages)
        ) : [],
        projectContexts: Array.isArray(combined.projectContexts) ? combined.projectContexts.filter((context) =>
          context &&
          typeof context.chatId === 'string' &&
          typeof context.content === 'string' &&
          typeof context.timestamp === 'number'
        ) : [],
      };
    }

    const writeBroker = this.loadJsonFile<Partial<PersistedWriteBrokerState>>(path.join(root, 'write-broker.json'));
    if (writeBroker) {
      state.writeBroker = {
        approvedRoots: Array.isArray(writeBroker.approvedRoots)
          ? writeBroker.approvedRoots.filter((rootPath): rootPath is string => typeof rootPath === 'string')
          : [],
        requests: Array.isArray(writeBroker.requests)
          ? writeBroker.requests.filter((request) =>
              request &&
              typeof request.id === 'string' &&
              typeof request.rootPath === 'string' &&
              typeof request.requestedBy === 'string' &&
              typeof request.chatId === 'string' &&
              typeof request.createdAt === 'string' &&
              typeof request.status === 'string'
            )
          : [],
      };
    }

    if (
      state.terminalSessions.length > 0 ||
      state.combined.chatSessions.length > 0 ||
      state.combined.projectContexts.length > 0 ||
      state.writeBroker.approvedRoots.length > 0 ||
      state.writeBroker.requests.length > 0
    ) {
      loggerService.info('Migrated legacy runtime state into unified store', {
        terminalSessions: state.terminalSessions.length,
        chatSessions: state.combined.chatSessions.length,
        projectContexts: state.combined.projectContexts.length,
        writeRequests: state.writeBroker.requests.length,
      });
    }

    return state;
  }

  private load(): RuntimeStateData {
    const current = this.loadJsonFile<RuntimeStateData>(this.dataPath);
    if (current && current.version === 1) {
      return {
        ...this.buildDefaultState(),
        ...current,
        combined: {
          ...this.buildDefaultState().combined,
          ...(current.combined || {}),
        },
        writeBroker: {
          ...this.buildDefaultState().writeBroker,
          ...(current.writeBroker || {}),
        },
      };
    }

    const migrated = this.migrateLegacyState();
    this.write(migrated);
    return migrated;
  }

  private write(state: RuntimeStateData): void {
    try {
      state.lastUpdated = new Date().toISOString();
      fs.writeFileSync(this.dataPath, JSON.stringify(state, null, 2), 'utf-8');
    } catch (error) {
      loggerService.error('Failed to save unified runtime state', { error: String(error) });
    }
  }

  private save(): void {
    this.write(this.data);
  }

  getTerminalSessions(): PersistedTerminalSession[] {
    return this.data.terminalSessions.map((session) => ({
      ...session,
      history: [...session.history],
    }));
  }

  setTerminalSessions(sessions: PersistedTerminalSession[]): void {
    this.data.terminalSessions = sessions.map((session) => ({
      channelId: session.channelId,
      cwd: session.cwd,
      history: [...session.history],
      createdAt: session.createdAt,
    }));
    this.save();
  }

  getCombinedState(): PersistedCombinedRuntimeState {
    return {
      currentCLI: this.data.combined.currentCLI,
      chatSessions: this.data.combined.chatSessions.map((session) => ({
        key: session.key,
        workdir: session.workdir,
        messages: session.messages.map((message) => ({ ...message })),
      })),
      projectContexts: this.data.combined.projectContexts.map((context) => ({ ...context })),
      selectedAISessionsByChat: (this.data.combined.selectedAISessionsByChat || []).map((selected) => ({ ...selected })),
    };
  }

  setCombinedState(state: PersistedCombinedRuntimeState): void {
    this.data.combined = {
      currentCLI: state.currentCLI,
      chatSessions: state.chatSessions.map((session) => ({
        key: session.key,
        workdir: session.workdir,
        messages: session.messages.map((message) => ({ ...message })),
      })),
      projectContexts: state.projectContexts.map((context) => ({ ...context })),
      selectedAISessionsByChat: (state.selectedAISessionsByChat || []).map((selected) => ({ ...selected })),
    };
    this.save();
  }

  getWriteBrokerState(): PersistedWriteBrokerState {
    return {
      approvedRoots: [...this.data.writeBroker.approvedRoots],
      requests: this.data.writeBroker.requests.map((request) => ({ ...request })),
    };
  }

  setWriteBrokerState(state: PersistedWriteBrokerState): void {
    this.data.writeBroker = {
      approvedRoots: [...state.approvedRoots],
      requests: state.requests.map((request) => ({ ...request })),
    };
    this.save();
  }
}

export const runtimeStateService = new RuntimeStateService();
