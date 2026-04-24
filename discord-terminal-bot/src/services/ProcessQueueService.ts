import config from '../config';
import { ProcessInfo } from '../types';

interface QueuedCommand {
  channelId: string;
  userId: string;
  command: string;
  timestamp: Date;
  resolve: (value: string) => void;
  reject: (reason: string) => void;
}

export class ProcessQueueService {
  private queues: Map<string, QueuedCommand[]> = new Map();
  private processing: Map<string, boolean> = new Map();

  async enqueue(
    channelId: string,
    userId: string,
    command: string,
    executor: (command: string) => Promise<string>
  ): Promise<string> {
    const queue = this.queues.get(channelId) || [];
    
    if (queue.length >= config.maxQueueSize) {
      throw new Error(`Queue is full (max: ${config.maxQueueSize}). Please wait for pending commands to complete.`);
    }

    return new Promise((resolve, reject) => {
      const queuedCommand: QueuedCommand = {
        channelId,
        userId,
        command,
        timestamp: new Date(),
        resolve,
        reject,
      };

      queue.push(queuedCommand);
      this.queues.set(channelId, queue);

      if (!this.processing.get(channelId)) {
        this.processNext(channelId, executor);
      }
    });
  }

  private async processNext(
    channelId: string,
    executor: (command: string) => Promise<string>
  ): Promise<void> {
    const queue = this.queues.get(channelId);
    if (!queue || queue.length === 0) {
      this.processing.set(channelId, false);
      return;
    }

    this.processing.set(channelId, true);
    const next = queue.shift()!;
    this.queues.set(channelId, queue);

    try {
      const result = await executor(next.command);
      next.resolve(result);
    } catch (error) {
      next.reject(error instanceof Error ? error.message : 'Unknown error');
    }

    this.processNext(channelId, executor);
  }

  getQueueSize(channelId: string): number {
    return this.queues.get(channelId)?.length || 0;
  }

  getAllQueues(): Map<string, number> {
    const result = new Map<string, number>();
    for (const [channelId, queue] of this.queues) {
      result.set(channelId, queue.length);
    }
    return result;
  }

  clearQueue(channelId: string): number {
    const queue = this.queues.get(channelId);
    if (!queue) return 0;

    for (const cmd of queue) {
      cmd.reject('Queue cleared');
    }

    const size = queue.length;
    this.queues.delete(channelId);
    this.processing.set(channelId, false);
    return size;
  }

  isProcessing(channelId: string): boolean {
    return this.processing.get(channelId) || false;
  }
}

export const processQueueService = new ProcessQueueService();
