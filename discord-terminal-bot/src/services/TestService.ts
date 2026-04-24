import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { decodeProcessChunk, getProcessEnv, wrapWindowsUtf8Command } from '../utils';
import { loggerService } from './LoggerService';

interface TestResult {
  success: boolean;
  passed: number;
  failed: number;
  total: number;
  duration: number;
  output?: string;
  coverage?: {
    lines: number;
    branches: number;
    functions: number;
    statements: number;
  };
}

export class TestService {
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
        resolve(stdout + stderr);
      });

      proc.on('error', (err) => {
        reject(err);
      });
    });
  }

  detectFramework(): 'jest' | 'vitest' | 'mocha' | 'pytest' | 'playwright' | 'unknown' {
    if (fs.existsSync(path.join(this.cwd, 'jest.config.js')) ||
        fs.existsSync(path.join(this.cwd, 'jest.config.ts')) ||
        fs.existsSync(path.join(this.cwd, 'package.json')) && 
        fs.readFileSync(path.join(this.cwd, 'package.json'), 'utf-8').includes('"jest"')) {
      return 'jest';
    }
    if (fs.existsSync(path.join(this.cwd, 'vitest.config.ts')) ||
        fs.existsSync(path.join(this.cwd, 'vite.config.ts'))) {
      return 'vitest';
    }
    if (fs.existsSync(path.join(this.cwd, 'mocha.opts')) ||
        fs.existsSync(path.join(this.cwd, '.mocharc'))) {
      return 'mocha';
    }
    if (fs.existsSync(path.join(this.cwd, 'pytest.ini')) ||
        fs.existsSync(path.join(this.cwd, 'pyproject.toml')) && 
        fs.readFileSync(path.join(this.cwd, 'pyproject.toml'), 'utf-8').includes('[tool.pytest')) {
      return 'pytest';
    }
    if (fs.existsSync(path.join(this.cwd, 'playwright.config.ts')) ||
        fs.existsSync(path.join(this.cwd, 'playwright.config.js'))) {
      return 'playwright';
    }
    return 'unknown';
  }

  async runTests(pattern?: string): Promise<TestResult> {
    const startTime = Date.now();
    const framework = this.detectFramework();

    try {
      let command: string;
      let args: string[] = [];

      switch (framework) {
        case 'jest':
          command = 'npx';
          args = pattern ? ['jest', pattern] : ['jest'];
          break;
        case 'vitest':
          command = 'npx';
          args = pattern ? ['vitest', 'run', pattern] : ['vitest', 'run'];
          break;
        case 'mocha':
          command = 'npx';
          args = pattern ? ['mocha', pattern] : ['mocha'];
          break;
        case 'pytest':
          command = 'python';
          args = pattern ? ['-m', 'pytest', pattern] : ['-m', 'pytest'];
          break;
        case 'playwright':
          command = 'npx';
          args = pattern ? ['playwright', 'test', pattern] : ['playwright', 'test'];
          break;
        default:
          if (fs.existsSync(path.join(this.cwd, 'package.json'))) {
            const pkg = JSON.parse(fs.readFileSync(path.join(this.cwd, 'package.json'), 'utf-8'));
            const testScript = pkg.scripts?.test;
            if (testScript) {
              command = 'npm';
              args = ['test', '--', '--passWithNoTests'];
            } else {
              throw new Error('No test framework detected');
            }
          } else {
            throw new Error('No test framework detected');
          }
      }

      const output = await this.exec(`${command} ${args.join(' ')}`);
      const duration = Date.now() - startTime;

      const passed = (output.match(/✓/g) || []).length;
      const failed = (output.match(/✗/g) || []).length;

      const passedMatch = output.match(/(\d+) passed/);
      const failedMatch = output.match(/(\d+) failed/);
      const totalMatch = output.match(/(\d+) tests?/);

      return {
        success: failed === 0,
        passed: passedMatch ? parseInt(passedMatch[1]) : passed,
        failed: failedMatch ? parseInt(failedMatch[1]) : failed,
        total: totalMatch ? parseInt(totalMatch[1]) : passed + failed,
        duration,
        output: output.slice(-3000),
      };
    } catch (error: any) {
      return {
        success: false,
        passed: 0,
        failed: 1,
        total: 1,
        duration: Date.now() - startTime,
        output: error.message,
      };
    }
  }

  async runCoverage(): Promise<TestResult> {
    const startTime = Date.now();
    const framework = this.detectFramework();

    try {
      let command: string;

      switch (framework) {
        case 'jest':
          command = 'npx jest --coverage';
          break;
        case 'vitest':
          command = 'npx vitest run --coverage';
          break;
        case 'pytest':
          command = 'python -m pytest --cov=. --cov-report=term';
          break;
        default:
          throw new Error(`Coverage not supported for ${framework}`);
      }

      const output = await this.exec(command);
      const duration = Date.now() - startTime;

      const linesMatch = output.match(/Lines:\s*(\d+\.\d+)%/);
      const statementsMatch = output.match(/Statements:\s*(\d+\.\d+)%/);

      return {
        success: true,
        passed: 0,
        failed: 0,
        total: 0,
        duration,
        output: output.slice(-3000),
        coverage: {
          lines: linesMatch ? parseFloat(linesMatch[1]) : 0,
          statements: statementsMatch ? parseFloat(statementsMatch[1]) : 0,
          branches: 0,
          functions: 0,
        },
      };
    } catch (error: any) {
      return {
        success: false,
        passed: 0,
        failed: 0,
        total: 0,
        duration: Date.now() - startTime,
        output: error.message,
      };
    }
  }
}

export const testService = new TestService();
