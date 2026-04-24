import fs from 'fs';
import path from 'path';
import { loggerService } from './LoggerService';
import { aiService } from './AIService';

interface ReviewResult {
  score: number;
  issues: ReviewIssue[];
  suggestions: string[];
  summary: string;
}

interface ReviewIssue {
  severity: 'high' | 'medium' | 'low';
  type: string;
  message: string;
  line?: number;
}

export class ReviewService {
  private cwd: string;

  constructor(cwd: string = process.cwd()) {
    this.cwd = cwd;
  }

  setCwd(cwd: string): void {
    this.cwd = cwd;
  }

  async reviewFile(filePath: string): Promise<ReviewResult> {
    const fullPath = path.isAbsolute(filePath) ? filePath : path.join(this.cwd, filePath);

    if (!fs.existsSync(fullPath)) {
      throw new Error(`File not found: ${filePath}`);
    }

    const content = fs.readFileSync(fullPath, 'utf-8');
    const ext = path.extname(filePath);

    const prompt = `You are a code reviewer. Review the following ${ext} code and identify:

1. **Issues** (bugs, security vulnerabilities, performance problems)
2. **Code quality** (naming, structure, complexity)
3. **Best practices** violations
4. **Suggestions** for improvement

Provide a JSON response with this format:
{
  "score": 1-10,
  "issues": [
    {
      "severity": "high|medium|low",
      "type": "bug|security|performance|quality",
      "message": "description",
      "line": number (optional)
    }
  ],
  "suggestions": ["suggestion 1", "suggestion 2"],
  "summary": "overall summary"
}

Code to review:
\`\`\`
${content.slice(0, 5000)}
\`\`\`
`;

    try {
      const response = await aiService.chat(prompt, () => {}, { workdir: this.cwd });

      try {
        const jsonMatch = response.text.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
          return JSON.parse(jsonMatch[0]);
        }
      } catch {}

      return {
        score: 7,
        issues: [],
        suggestions: [],
        summary: response.text.slice(0, 500),
      };
    } catch (error: any) {
      throw new Error(`Review failed: ${error.message}`);
    }
  }

  async reviewGitChanges(): Promise<ReviewResult> {
    const hasGit = fs.existsSync(path.join(this.cwd, '.git'));

    if (!hasGit) {
      throw new Error('Not a git repository');
    }

    const statusCmd = `git diff --name-only`;
    const { execSync } = require('child_process');

    try {
      const changedFiles = execSync(statusCmd, { cwd: this.cwd, encoding: 'utf-8' })
        .split('\n')
        .filter((f: string) => f.trim() && !f.includes('node_modules'));

      if (changedFiles.length === 0) {
        return {
          score: 10,
          issues: [],
          suggestions: [],
          summary: 'No changes to review',
        };
      }

      let allChanges = '';
      for (const file of changedFiles.slice(0, 5)) {
        const diff = execSync(`git diff ${file}`, { cwd: this.cwd, encoding: 'utf-8' });
        allChanges += `\n\n## ${file}\n\`\`\`diff\n${diff.slice(-2000)}\n\`\`\``;
      }

      const prompt = `Review these git changes and identify:
1. Potential bugs
2. Security issues
3. Code quality problems

Provide JSON:
{
  "score": 1-10,
  "issues": [{"severity": "high|medium|low", "type": "bug|security|quality", "message": "desc"}],
  "suggestions": ["suggestion"],
  "summary": "brief summary"
}

Changes:${allChanges.slice(0, 6000)}
`;

      const response = await aiService.chat(prompt, () => {}, { workdir: this.cwd });

      try {
        const jsonMatch = response.text.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
          return JSON.parse(jsonMatch[0]);
        }
      } catch {}

      return {
        score: 7,
        issues: [],
        suggestions: [],
        summary: response.text.slice(0, 500),
      };
    } catch (error: any) {
      throw new Error(`Review failed: ${error.message}`);
    }
  }

  async explainCode(filePath: string, specificSection?: string): Promise<string> {
    const fullPath = path.isAbsolute(filePath) ? filePath : path.join(this.cwd, filePath);

    if (!fs.existsSync(fullPath)) {
      throw new Error(`File not found: ${filePath}`);
    }

    let content = fs.readFileSync(fullPath, 'utf-8');

    if (specificSection) {
      const lines = content.split('\n');
      content = lines.slice(
        Math.max(0, parseInt(specificSection) - 5),
        Math.min(lines.length, parseInt(specificSection) + 10)
      ).join('\n');
    }

    const prompt = `Explain this code in simple terms. What does it do? How does it work?

\`\`\`
${content.slice(0, 4000)}
\`\`\`
`;

    const response = await aiService.chat(prompt, () => {}, { workdir: this.cwd });
    return response.text;
  }
}

export const reviewService = new ReviewService();
