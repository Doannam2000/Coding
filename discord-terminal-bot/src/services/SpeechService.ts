import https from 'https';
import fs from 'fs';
import path from 'path';
import { loggerService } from './LoggerService';

interface SpeechResult {
  success: boolean;
  audioPath?: string;
  duration?: number;
  error?: string;
}

export class SpeechService {
  private cwd: string;
  private usePiper: boolean = false;

  constructor(cwd: string = process.cwd()) {
    this.cwd = cwd;
    this.detectPiper();
  }

  setCwd(cwd: string): void {
    this.cwd = cwd;
  }

  private detectPiper(): void {
    try {
      const { execSync } = require('child_process');
      execSync('piper --version', { windowsHide: true });
      this.usePiper = true;
    } catch {
      this.usePiper = false;
    }
  }

  async speak(text: string, outputPath?: string): Promise<SpeechResult> {
    const audioPath = outputPath || path.join(this.cwd, 'speech_output.wav');

    try {
      if (this.usePiper) {
        return await this.speakWithPiper(text, audioPath);
      }

      return await this.speakWithEdgeTTS(text, audioPath);
    } catch (error: any) {
      return {
        success: false,
        error: error.message,
      };
    }
  }

  private async speakWithPiper(text: string, outputPath: string): Promise<SpeechResult> {
    const { spawn } = require('child_process');

    return new Promise((resolve) => {
      const proc = spawn('piper', [
        '--model', 'en_US-lessac',
        '--output_file', outputPath,
      ], {
        windowsHide: true,
        stdio: ['pipe', 'pipe', 'pipe'],
      });

      proc.stdin?.write(text);
      proc.stdin?.end();

      let stderr = '';
      proc.stderr?.on('data', (data: Buffer) => {
        stderr += data.toString();
      });

      proc.on('close', (code: number) => {
        if (code === 0 && fs.existsSync(outputPath)) {
          resolve({
            success: true,
            audioPath: outputPath,
            duration: text.length * 50,
          });
        } else {
          resolve({
            success: false,
            error: stderr || 'Piper failed',
          });
        }
      });

      proc.on('error', (err: Error) => {
        resolve({
          success: false,
          error: err.message,
        });
      });
    });
  }

  private async speakWithEdgeTTS(text: string, outputPath: string): Promise<SpeechResult> {
    return new Promise((resolve) => {
      const tempFile = path.join(this.cwd, 'temp_speech.mp3');

      const postData = JSON.stringify({
        text: text.slice(0, 1000),
        voice: 'en-US-AriaNeural',
      });

      const options = {
        hostname: 'api.opencode.ai',
        path: '/tts',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData),
        },
      };

      const req = https.request(options, (res) => {
        if (res.statusCode === 200) {
          const file = fs.createWriteStream(tempFile);
          res.pipe(file);
          file.on('finish', () => {
            file.close();
            fs.renameSync(tempFile, outputPath);
            resolve({
              success: true,
              audioPath: outputPath,
              duration: text.length * 50,
            });
          });
        } else {
          resolve({
            success: false,
            error: `TTS API returned status ${res.statusCode}`,
          });
        }
      });

      req.on('error', (err) => {
        resolve({
          success: false,
          error: err.message,
        });
      });

      req.write(postData);
      req.end();
    });
  }

  async speakToFile(text: string, filename: string): Promise<SpeechResult> {
    const outputPath = path.join(this.cwd, filename);
    return this.speak(text, outputPath);
  }

  getVoices(): string[] {
    return [
      'en-US-AriaNeural',
      'en-US-GuyNeural', 
      'en-US-JennyNeural',
      'en-GB-SoniaNeural',
      'en-AU-NatashaNeural',
    ];
  }

  async describeAudio(audioPath: string): Promise<string> {
    if (!fs.existsSync(audioPath)) {
      throw new Error(`Audio file not found: ${audioPath}`);
    }

    return `Audio file: ${path.basename(audioPath)}\nSize: ${fs.statSync(audioPath).size} bytes`;
  }
}

export const speechService = new SpeechService();