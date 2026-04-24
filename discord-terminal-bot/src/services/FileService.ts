import fs from 'fs';
import path from 'path';
import { loggerService } from './LoggerService';

export interface FileOperation {
  type: 'create' | 'edit' | 'delete' | 'read';
  path: string;
  content?: string;
  success: boolean;
  error?: string;
}

export interface FileMatch {
  path: string;
  size: number;
  isDirectory: boolean;
}

export class FileService {
  private cwd: string;

  constructor(cwd: string = process.cwd()) {
    this.cwd = cwd;
  }

  setCwd(cwd: string): void {
    this.cwd = cwd;
  }

  resolvePath(filePath: string): string {
    if (path.isAbsolute(filePath)) {
      return filePath;
    }
    return path.join(this.cwd, filePath);
  }

  async read(filePath: string): Promise<string> {
    const fullPath = this.resolvePath(filePath);

    if (!fs.existsSync(fullPath)) {
      throw new Error(`File not found: ${filePath}`);
    }

    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      throw new Error(`Path is a directory: ${filePath}`);
    }

    return fs.readFileSync(fullPath, 'utf-8');
  }

  async write(filePath: string, content: string): Promise<FileOperation> {
    const fullPath = this.resolvePath(filePath);
    const dir = path.dirname(fullPath);

    try {
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }

      fs.writeFileSync(fullPath, content, 'utf-8');

      return {
        type: 'create',
        path: filePath,
        content,
        success: true,
      };
    } catch (error: any) {
      return {
        type: 'create',
        path: filePath,
        success: false,
        error: error.message,
      };
    }
  }

  async edit(filePath: string, oldString: string, newString: string): Promise<FileOperation> {
    const fullPath = this.resolvePath(filePath);

    if (!fs.existsSync(fullPath)) {
      return {
        type: 'edit',
        path: filePath,
        success: false,
        error: 'File not found',
      };
    }

    try {
      const content = fs.readFileSync(fullPath, 'utf-8');

      if (!content.includes(oldString)) {
        return {
          type: 'edit',
          path: filePath,
          success: false,
          error: 'Old string not found in file',
        };
      }

      const newContent = content.replace(oldString, newString);
      fs.writeFileSync(fullPath, newContent, 'utf-8');

      return {
        type: 'edit',
        path: filePath,
        content: newContent,
        success: true,
      };
    } catch (error: any) {
      return {
        type: 'edit',
        path: filePath,
        success: false,
        error: error.message,
      };
    }
  }

  async delete(filePath: string): Promise<FileOperation> {
    const fullPath = this.resolvePath(filePath);

    if (!fs.existsSync(fullPath)) {
      return {
        type: 'delete',
        path: filePath,
        success: false,
        error: 'File not found',
      };
    }

    try {
      const stat = fs.statSync(fullPath);
      if (stat.isDirectory()) {
        fs.rmSync(fullPath, { recursive: true });
      } else {
        fs.unlinkSync(fullPath);
      }

      return {
        type: 'delete',
        path: filePath,
        success: true,
      };
    } catch (error: any) {
      return {
        type: 'delete',
        path: filePath,
        success: false,
        error: error.message,
      };
    }
  }

  async list(dirPath: string = '.'): Promise<FileMatch[]> {
    const fullPath = this.resolvePath(dirPath);

    if (!fs.existsSync(fullPath)) {
      throw new Error(`Directory not found: ${dirPath}`);
    }

    const stat = fs.statSync(fullPath);
    if (!stat.isDirectory()) {
      throw new Error(`Not a directory: ${dirPath}`);
    }

    const files = fs.readdirSync(fullPath);
    return files.map(file => {
      const fileStat = fs.statSync(path.join(fullPath, file));
      return {
        path: path.join(dirPath, file),
        size: fileStat.size,
        isDirectory: fileStat.isDirectory(),
      };
    });
  }

  async glob(pattern: string): Promise<string[]> {
    const results: string[] = [];
    const baseDir = pattern.startsWith('/') || pattern.match(/^[a-zA-Z]:/)
      ? path.dirname(pattern)
      : this.cwd;

    const searchPattern = path.basename(pattern);

    const walk = (dir: string) => {
      try {
        const files = fs.readdirSync(dir);
        for (const file of files) {
          const fullPath = path.join(dir, file);
          const stat = fs.statSync(fullPath);

          if (stat.isDirectory()) {
            if (!file.startsWith('.')) {
              walk(fullPath);
            }
          } else {
            if (this.matchPattern(file, searchPattern)) {
              results.push(fullPath);
            }
          }
        }
      } catch {}
    };

    try {
      if (fs.existsSync(baseDir)) {
        walk(baseDir);
      }
    } catch {}

    return results;
  }

  private matchPattern(filename: string, pattern: string): boolean {
    const regex = pattern
      .replace(/\./g, '\\.')
      .replace(/\*\*/g, '.*')
      .replace(/\*/g, '[^/]*')
      .replace(/\?/g, '.');
    return new RegExp(`^${regex}$`).test(filename);
  }

  async exists(filePath: string): Promise<boolean> {
    const fullPath = this.resolvePath(filePath);
    return fs.existsSync(fullPath);
  }

  getStats(filePath: string): { size: number; modified: Date; isDirectory: boolean } | null {
    const fullPath = this.resolvePath(filePath);

    if (!fs.existsSync(fullPath)) {
      return null;
    }

    const stat = fs.statSync(fullPath);
    return {
      size: stat.size,
      modified: stat.mtime,
      isDirectory: stat.isDirectory(),
    };
  }

  generateComponent(prompt: string, filename: string): string {
    const name = filename.replace(/\.(tsx|jsx|ts|js)$/, '');
    const componentName = name.split(/[-_]/).map(w => 
      w.charAt(0).toUpperCase() + w.slice(1)
    ).join('');

    if (filename.endsWith('.tsx') || filename.endsWith('.jsx')) {
      return `import React from 'react';

interface ${componentName}Props {
  className?: string;
}

export const ${componentName}: React.FC<${componentName}Props> = ({ className }) => {
  return (
    <div className={className}>
      {/* TODO: Implement ${componentName} based on: ${prompt} */}
    </div>
  );
};

export default ${componentName};
`;
    }

    if (filename.endsWith('.css') || filename.endsWith('.scss')) {
      return `/* ${componentName} component styles */
/* Based on: ${prompt} */

.${name.toLowerCase()} {
  /* Add your styles here */
}
`;
    }

    return `// ${filename}\n// TODO: Implement based on: ${prompt}\n`;
  }
}

export const fileService = new FileService();