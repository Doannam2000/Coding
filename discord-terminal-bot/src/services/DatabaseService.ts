import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { decodeProcessChunk, getProcessEnv } from '../utils';
import { loggerService } from './LoggerService';

interface QueryResult {
  success: boolean;
  rows?: any[];
  columns?: string[];
  rowCount?: number;
  duration: number;
  output?: string;
  error?: string;
}

interface MigrationResult {
  success: boolean;
  applied: string[];
  failed: string[];
}

export class DatabaseService {
  private cwd: string;
  private dbType: 'postgresql' | 'mysql' | 'sqlite' | 'mongodb' | 'unknown' = 'unknown';

  constructor(cwd: string = process.cwd()) {
    this.cwd = cwd;
    this.detectDatabase();
  }

  setCwd(cwd: string): void {
    this.cwd = cwd;
    this.detectDatabase();
  }

  private detectDatabase(): void {
    const packageJsonPath = path.join(this.cwd, 'package.json');
    const prismaSchemaPath = path.join(this.cwd, 'prisma', 'schema.prisma');
    const drizzleConfigPath = path.join(this.cwd, 'drizzle.config.ts');

    if (fs.existsSync(prismaSchemaPath)) {
      const schema = fs.readFileSync(prismaSchemaPath, 'utf-8');
      if (schema.includes('provider = "postgresql"') || schema.includes('provider = "postgres"')) {
        this.dbType = 'postgresql';
      } else if (schema.includes('provider = "mysql"')) {
        this.dbType = 'mysql';
      } else if (schema.includes('provider = "sqlite"')) {
        this.dbType = 'sqlite';
      }
    } else if (fs.existsSync(drizzleConfigPath)) {
      this.dbType = 'postgresql';
    } else if (fs.existsSync(packageJsonPath)) {
      const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
      const deps = { ...pkg.dependencies, ...pkg.devDependencies };
      if (deps['pg'] || deps['pg-promise']) this.dbType = 'postgresql';
      else if (deps['mysql2'] || deps['mysql']) this.dbType = 'mysql';
      else if (deps['better-sqlite3'] || deps['sqlite3']) this.dbType = 'sqlite';
      else if (deps['mongodb'] || deps['mongoose']) this.dbType = 'mongodb';
    }
  }

  getDbType(): string {
    return this.dbType;
  }

  async runQuery(sql: string): Promise<QueryResult> {
    const startTime = Date.now();

    try {
      let output: string;

      switch (this.dbType) {
        case 'postgresql':
          output = await this.runCommand('psql', ['-c', sql]);
          break;
        case 'mysql':
          output = await this.runCommand('mysql', ['-e', sql]);
          break;
        case 'sqlite':
          output = await this.runCommand('sqlite3', [path.join(this.cwd, 'database.sqlite'), sql]);
          break;
        default:
          throw new Error('Database type not detected. Add Prisma or use TypeORM/Drizzle.');
      }

      return {
        success: true,
        rows: [],
        rowCount: 0,
        duration: Date.now() - startTime,
        output: output.slice(-2000),
      };
    } catch (error: any) {
      return {
        success: false,
        duration: Date.now() - startTime,
        error: error.message,
      };
    }
  }

  private runCommand(cmd: string, args: string[]): Promise<string> {
    return new Promise((resolve, reject) => {
      const proc = spawn(cmd, args, {
        cwd: this.cwd,
        windowsHide: true,
        env: getProcessEnv(),
      });

      let stdout = '';
      let stderr = '';

      proc.stdout?.on('data', (data) => { stdout += decodeProcessChunk(data); });
      proc.stderr?.on('data', (data) => { stderr += decodeProcessChunk(data); });

      proc.on('close', (code) => {
        if (code === 0) resolve(stdout);
        else reject(new Error(stderr || `Command failed with code ${code}`));
      });

      proc.on('error', reject);
    });
  }

  async runMigrations(): Promise<MigrationResult> {
    try {
      switch (this.dbType) {
        case 'postgresql':
        case 'mysql':
        case 'sqlite':
          if (fs.existsSync(path.join(this.cwd, 'prisma'))) {
            const output = await this.runCommand('npx', ['prisma', 'migrate', 'deploy']);
            return {
              success: true,
              applied: ['Prisma migrations'],
              failed: [],
            };
          }
          throw new Error('No migration tool found');
        default:
          throw new Error('Database type not supported for migrations');
      }
    } catch (error: any) {
      return {
        success: false,
        applied: [],
        failed: [error.message],
      };
    }
  }

  async generateMigrations(name: string): Promise<string> {
    try {
      if (fs.existsSync(path.join(this.cwd, 'prisma'))) {
        await this.runCommand('npx', ['prisma', 'migrate', 'dev', '--name', name]);
        return `Migration "${name}" created successfully`;
      }
      throw new Error('Prisma not found');
    } catch (error: any) {
      throw new Error(`Failed to generate migration: ${error.message}`);
    }
  }

  async seedDatabase(): Promise<string> {
    try {
      if (fs.existsSync(path.join(this.cwd, 'prisma'))) {
        await this.runCommand('npx', ['prisma', 'db', 'seed']);
        return 'Database seeded successfully';
      }
      throw new Error('No seeder found');
    } catch (error: any) {
      throw new Error(`Failed to seed: ${error.message}`);
    }
  }

  async resetDatabase(): Promise<string> {
    try {
      if (fs.existsSync(path.join(this.cwd, 'prisma'))) {
        await this.runCommand('npx', ['prisma', 'migrate', 'reset', '--force']);
        return 'Database reset successfully';
      }
      throw new Error('Prisma not found');
    } catch (error: any) {
      throw new Error(`Failed to reset: ${error.message}`);
    }
  }

  getConnectionString(): string | null {
    const envPath = path.join(this.cwd, '.env');
    if (!fs.existsSync(envPath)) return null;

    const env = fs.readFileSync(envPath, 'utf-8');
    const match = env.match(/DATABASE_URL=(.+)/);
    return match ? match[1] : null;
  }
}

export const databaseService = new DatabaseService();
