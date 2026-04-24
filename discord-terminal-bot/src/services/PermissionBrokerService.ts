import fs from 'fs';
import path from 'path';
import { execFileSync } from 'child_process';
import { loggerService } from './LoggerService';
import { PersistedWriteAccessRequest, PersistedWriteBrokerState, runtimeStateService } from './RuntimeStateService';

export type WriteAccessRequest = PersistedWriteAccessRequest;
type PermissionBrokerData = PersistedWriteBrokerState;

export class PermissionBrokerService {
  private data: PermissionBrokerData;

  constructor() {
    this.data = runtimeStateService.getWriteBrokerState();
  }

  private save(): void {
    try {
      runtimeStateService.setWriteBrokerState(this.data);
    } catch (error) {
      loggerService.error('Failed to save write broker state', { error: String(error) });
    }
  }

  private normalizeRoot(targetPath: string): string {
    const resolved = path.resolve(targetPath);
    return process.platform === 'win32' ? resolved.toLowerCase() : resolved;
  }

  private generateId(): string {
    return `${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  }

  canWriteDirectly(targetPath: string): boolean {
    try {
      fs.accessSync(path.resolve(targetPath), fs.constants.W_OK);
      return true;
    } catch {
      return false;
    }
  }

  isApproved(targetPath: string): boolean {
    const normalized = this.normalizeRoot(targetPath);
    return this.data.approvedRoots.some((approvedRoot) => normalized === approvedRoot || normalized.startsWith(`${approvedRoot}${path.sep}`));
  }

  requiresApproval(targetPath: string): boolean {
    return !this.canWriteDirectly(targetPath) && !this.isApproved(targetPath);
  }

  createRequest(targetPath: string, requestedBy: string, chatId: string): WriteAccessRequest {
    const rootPath = path.resolve(targetPath);
    const existing = this.data.requests.find((request) =>
      request.status === 'pending' &&
      this.normalizeRoot(request.rootPath) === this.normalizeRoot(rootPath) &&
      request.chatId === chatId
    );

    if (existing) {
      return existing;
    }

    const request: WriteAccessRequest = {
      id: this.generateId(),
      rootPath,
      requestedBy,
      chatId,
      createdAt: new Date().toISOString(),
      status: 'pending',
    };

    this.data.requests.unshift(request);
    this.data.requests = this.data.requests.slice(0, 200);
    this.save();
    return request;
  }

  getRequest(id: string): WriteAccessRequest | undefined {
    return this.data.requests.find((request) => request.id === id);
  }

  denyRequest(id: string): WriteAccessRequest | undefined {
    const request = this.getRequest(id);
    if (!request) return undefined;
    request.status = 'denied';
    this.save();
    return request;
  }

  private grantWindowsModify(rootPath: string): void {
    const username = process.env.USERNAME?.trim();
    if (!username) {
      throw new Error('Current Windows username is not available.');
    }

    execFileSync('icacls', [rootPath, '/grant', `${username}:(OI)(CI)M`, '/T', '/C'], {
      windowsHide: true,
      stdio: 'pipe',
    });
  }

  approveRequest(id: string): WriteAccessRequest {
    const request = this.getRequest(id);
    if (!request) {
      throw new Error('Write access request not found.');
    }

    try {
      if (process.platform === 'win32') {
        this.grantWindowsModify(request.rootPath);
      } else if (!this.canWriteDirectly(request.rootPath)) {
        throw new Error('Automatic permission grant is only implemented for Windows.');
      }

      if (!this.data.approvedRoots.includes(this.normalizeRoot(request.rootPath))) {
        this.data.approvedRoots.push(this.normalizeRoot(request.rootPath));
      }

      request.status = 'approved';
      delete request.error;
      this.save();
      loggerService.info('Approved write access root', { rootPath: request.rootPath, requestedBy: request.requestedBy });
      return request;
    } catch (error) {
      request.status = 'failed';
      request.error = error instanceof Error ? error.message : String(error);
      this.save();
      loggerService.error('Failed to approve write access root', { rootPath: request.rootPath, error: request.error });
      throw error;
    }
  }

  ensureWritableOrThrow(targetPath: string): void {
    if (this.canWriteDirectly(targetPath) || this.isApproved(targetPath)) {
      return;
    }
    throw new Error(`Write access approval required for: ${path.resolve(targetPath)}`);
  }
}

export const permissionBrokerService = new PermissionBrokerService();
