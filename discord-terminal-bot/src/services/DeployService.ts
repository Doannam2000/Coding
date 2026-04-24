import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { decodeProcessChunk, getProcessEnv, wrapWindowsUtf8Command } from '../utils';
import { loggerService } from './LoggerService';

interface DeployResult {
  success: boolean;
  url?: string;
  output?: string;
  error?: string;
}

export class DeployService {
  private cwd: string;

  constructor(cwd: string = process.cwd()) {
    this.cwd = cwd;
  }

  setCwd(cwd: string): void {
    this.cwd = cwd;
  }

  private exec(command: string, args: string[] = []): Promise<string> {
    return new Promise((resolve, reject) => {
      const isWindows = process.platform === 'win32';
      const shell = isWindows ? 'cmd.exe' : '/bin/bash';
      const windowsCommand = [command, ...args].join(' ');
      const shellArgs = isWindows ? ['/d', '/c', wrapWindowsUtf8Command(windowsCommand)] : ['-c', command];

      const proc = spawn(shell, shellArgs, {
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
          resolve(stdout);
        } else {
          reject(new Error(stderr || `Command failed with code ${code}`));
        }
      });

      proc.on('error', (err) => {
        reject(err);
      });
    });
  }

  async deployVercel(): Promise<DeployResult> {
    try {
      loggerService.info('Deploying to Vercel...');

      const hasVercel = fs.existsSync(path.join(this.cwd, 'vercel.json')) ||
                        fs.existsSync(path.join(this.cwd, 'package.json'));

      if (!hasVercel) {
        throw new Error('No Vercel config found. Add vercel.json or ensure package.json exists.');
      }

      const output = await this.exec('npx', ['vercel', '--yes', '--prod']);

      const urlMatch = output.match(/https:\/\/[^\s]+\.vercel\.app/);
      const url = urlMatch ? urlMatch[0] : 'https://vercel.com/dashboard';

      loggerService.info('Vercel deployment complete', { url });

      return {
        success: true,
        url,
        output: output.slice(-2000),
      };
    } catch (error: any) {
      loggerService.error('Vercel deploy failed', { error: error.message });
      return {
        success: false,
        error: error.message,
      };
    }
  }

  async deployNetlify(): Promise<DeployResult> {
    try {
      loggerService.info('Deploying to Netlify...');

      const hasNetlify = fs.existsSync(path.join(this.cwd, 'netlify.toml')) ||
                         fs.existsSync(path.join(this.cwd, 'package.json'));

      if (!hasNetlify) {
        throw new Error('No Netlify config found. Add netlify.toml or ensure package.json exists.');
      }

      const output = await this.exec('npx', ['netlify', 'deploy', '--prod', '--dir', '.']);

      const urlMatch = output.match(/Live url: ([^\s]+)/);
      const url = urlMatch ? urlMatch[1] : 'https://app.netlify.com';

      loggerService.info('Netlify deployment complete', { url });

      return {
        success: true,
        url,
        output: output.slice(-2000),
      };
    } catch (error: any) {
      loggerService.error('Netlify deploy failed', { error: error.message });
      return {
        success: false,
        error: error.message,
      };
    }
  }

  async buildDocker(imageName?: string): Promise<DeployResult> {
    try {
      loggerService.info('Building Docker image...');

      const hasDockerfile = fs.existsSync(path.join(this.cwd, 'Dockerfile'));

      if (!hasDockerfile) {
        throw new Error('No Dockerfile found in project root.');
      }

      const name = imageName || path.basename(this.cwd).toLowerCase().replace(/\s+/g, '-');
      const tag = `${name}:latest`;

      const buildOutput = await this.exec('docker', ['build', '-t', tag, '.']);

      loggerService.info('Docker image built', { tag });

      return {
        success: true,
        output: `Image built: ${tag}\n\n${buildOutput.slice(-1000)}`,
      };
    } catch (error: any) {
      loggerService.error('Docker build failed', { error: error.message });
      return {
        success: false,
        error: error.message,
      };
    }
  }

  async runDocker(imageName?: string, port: number = 3000): Promise<DeployResult> {
    try {
      const name = imageName || path.basename(this.cwd).toLowerCase().replace(/\s+/g, '-');
      const tag = `${name}:latest`;

      await this.exec('docker', ['run', '-d', '-p', `${port}:${port}`, '--name', name, tag]);

      return {
        success: true,
        output: `Container running at http://localhost:${port}`,
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.message,
      };
    }
  }

  async listContainers(): Promise<string> {
    return this.exec('docker', ['ps', '--format', '{{.Names}}\t{{.Status}}\t{{.Ports}}']);
  }

  async stopContainer(name: string): Promise<string> {
    return this.exec('docker', ['stop', name]);
  }
}

export const deployService = new DeployService();
