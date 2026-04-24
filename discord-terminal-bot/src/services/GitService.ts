import { spawn } from 'child_process';
import { decodeProcessChunk, getProcessEnv, wrapWindowsUtf8Command } from '../utils';
import { loggerService } from './LoggerService';

interface GitStatus {
  branch: string;
  modified: string[];
  staged: string[];
  untracked: string[];
  ahead: number;
  behind: number;
}

interface GitLog {
  hash: string;
  message: string;
  author: string;
  date: string;
}

export class GitService {
  private cwd: string;

  constructor(cwd: string = process.cwd()) {
    this.cwd = cwd;
  }

  setCwd(cwd: string): void {
    this.cwd = cwd;
  }

  private exec(command: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const isWindows = process.platform === 'win32';
      const shell = isWindows ? 'cmd.exe' : '/bin/bash';
      const args = isWindows ? ['/d', '/c', wrapWindowsUtf8Command(command)] : ['-c', command];

      const proc = spawn(shell, args, {
        cwd: this.cwd,
        windowsHide: true,
        env: getProcessEnv(),
      });

      let stdout = '';
      let stderr = '';

      proc.stdout?.on('data', (data) => {
        stdout += decodeProcessChunk(data);
      });

      proc.stderr?.on('data', (data) => {
        stderr += decodeProcessChunk(data);
      });

      proc.on('close', (code) => {
        if (code === 0) {
          resolve(stdout.trim());
        } else {
          reject(new Error(stderr || `Command failed with code ${code}`));
        }
      });

      proc.on('error', (err) => {
        reject(err);
      });
    });
  }

  async getStatus(): Promise<GitStatus> {
    try {
      const branch = await this.exec('git branch --show-current');
      const statusOutput = await this.exec('git status --porcelain');

      const modified: string[] = [];
      const staged: string[] = [];
      const untracked: string[] = [];

      for (const line of statusOutput.split('\n')) {
        if (!line.trim()) continue;
        const status = line.slice(0, 2);
        const file = line.slice(3);

        if (status.includes('M')) modified.push(file);
        if (status.includes('A') || status.includes('R')) staged.push(file);
        if (status.trim() === '??') untracked.push(file);
      }

      let ahead = 0, behind = 0;
      try {
        const revInfo = await this.exec('git rev-list --left-right --count HEAD...@{upstream}');
        const parts = revInfo.split('\t');
        ahead = parseInt(parts[0]) || 0;
        behind = parseInt(parts[1]) || 0;
      } catch {}

      return { branch, modified, staged, untracked, ahead, behind };
    } catch (error: any) {
      throw new Error(`Git not available: ${error.message}`);
    }
  }

  async getLog(limit: number = 10): Promise<GitLog[]> {
    const output = await this.exec(`git log --oneline -${limit}`);
    if (!output) return [];

    return output.split('\n').map(line => {
      const match = line.match(/^([a-f0-9]+)\s+(.*)$/);
      if (match) {
        return {
          hash: match[1].slice(0, 7),
          message: match[2],
          author: '',
          date: '',
        };
      }
      return { hash: '', message: line, author: '', date: '' };
    }).filter(l => l.hash);
  }

  async add(files: string[] = ['.']): Promise<string> {
    const filesStr = files.join(' ');
    return this.exec(`git add ${filesStr}`);
  }

  async commit(message: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const proc = spawn('git', ['commit', '-m', message], {
        cwd: this.cwd,
        windowsHide: true,
        env: getProcessEnv(),
      });

      let stdout = '';
      let stderr = '';

      proc.stdout?.on('data', (data) => {
        stdout += decodeProcessChunk(data);
      });

      proc.stderr?.on('data', (data) => {
        stderr += decodeProcessChunk(data);
      });

      proc.on('close', (code) => {
        if (code === 0) {
          resolve(stdout.trim());
        } else {
          reject(new Error((stderr || stdout || `Command failed with code ${code}`).trim()));
        }
      });

      proc.on('error', (err) => {
        reject(err);
      });
    });
  }

  async push(remote: string = 'origin', branch?: string): Promise<string> {
    const branchPart = branch || '';
    return this.exec(`git push ${remote} ${branchPart}`);
  }

  async pull(remote: string = 'origin', branch?: string): Promise<string> {
    const branchPart = branch || '';
    return this.exec(`git pull ${remote} ${branchPart}`);
  }

  async getBranches(): Promise<string[]> {
    const output = await this.exec('git branch -a');
    if (!output) return [];

    return output.split('\n')
      .map(b => b.replace(/^\*?\s*/, '').trim())
      .filter(b => b);
  }

  async createBranch(name: string, checkout: boolean = true): Promise<string> {
    if (checkout) {
      return this.exec(`git checkout -b ${name}`);
    }
    return this.exec(`git branch ${name}`);
  }

  async checkout(branch: string): Promise<string> {
    return this.exec(`git checkout ${branch}`);
  }

  async diff(file?: string): Promise<string> {
    const filePart = file || '';
    return this.exec(`git diff ${filePart}`);
  }

  async diffStaged(file?: string): Promise<string> {
    const filePart = file || '';
    return this.exec(`git diff --cached ${filePart}`);
  }

  private formatCommitTimestamp(date: Date = new Date()): string {
    const pad = (value: number): string => String(value).padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    const seconds = pad(date.getSeconds());
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  private summarizeChangedFiles(files: string[], max: number = 3): string {
    if (!files.length) return 'none';
    const picked = files.slice(0, max).join(', ');
    return files.length > max ? `${picked}...` : picked;
  }

  private buildPushGitMessage(status: GitStatus): string {
    const modifiedCount = status.modified.length;
    const stagedCount = status.staged.length;
    const untrackedCount = status.untracked.length;
    const touchedCount = modifiedCount + stagedCount + untrackedCount;

    const summaryParts: string[] = [];
    if (modifiedCount > 0) summaryParts.push(`updated ${modifiedCount} file(s)`);
    if (stagedCount > 0) summaryParts.push(`staged ${stagedCount} file(s)`);
    if (untrackedCount > 0) summaryParts.push(`added ${untrackedCount} new file(s)`);
    const summary = summaryParts.length ? summaryParts.join(', ') : 'no file changes';

    const optimized = touchedCount > 0
      ? `streamlined git flow for ${touchedCount} changed file(s)`
      : 'synchronized branch state with remote';

    const touchedFiles = this.summarizeChangedFiles([
      ...status.modified,
      ...status.staged,
      ...status.untracked,
    ]);

    return `${this.formatCommitTimestamp()} | fixed: ${summary} | optimized: ${optimized} | files: ${touchedFiles}`;
  }

  async autoCommit(message?: string): Promise<string> {
    const status = await this.getStatus();

    if (status.staged.length === 0 && status.modified.length === 0 && status.untracked.length === 0) {
      return 'Nothing to commit';
    }

    await this.add();

    if (!message) {
      const changed = status.modified.length + status.untracked.length;
      message = `Update: ${changed} file(s) changed`;
    }

    return this.commit(message);
  }

  async pushGitAuto(): Promise<string> {
    const status = await this.getStatus();
    const hasChanges = status.staged.length > 0 || status.modified.length > 0 || status.untracked.length > 0;

    let message = '';
    let commitOutput = 'No local changes to commit.';

    if (hasChanges) {
      await this.add();
      message = this.buildPushGitMessage(status);
      commitOutput = await this.commit(message);
    }

    const pushOutput = await this.push('origin', status.branch || undefined);

    const lines = [
      `Branch: ${status.branch || 'unknown'}`,
      hasChanges ? `Commit message: ${message}` : 'Commit message: skipped',
      `Commit: ${commitOutput || 'done'}`,
      `Push: ${pushOutput || 'done'}`,
    ];

    return lines.join('\n');
  }

  async fetch(): Promise<string> {
    return this.exec('git fetch --all');
  }
}

export const gitService = new GitService();
