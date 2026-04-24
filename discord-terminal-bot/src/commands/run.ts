import { SlashCommandBuilder, EmbedBuilder, Colors, ButtonBuilder, ButtonStyle, ActionRowBuilder } from 'discord.js';
import { terminalService, securityService, loggerService, memoryService } from '../services';
import config from '../config';
import { splitMessage } from '../utils';
import { ExtendedClient } from '../types';

export const data = new SlashCommandBuilder()
  .setName('run')
  .setDescription('Run a terminal command')
  .addStringOption(option =>
    option.setName('command')
      .setDescription('The command to run')
      .setRequired(true)
      .setAutocomplete(true)
  )
  .addNumberOption(option =>
    option.setName('timeout')
      .setDescription('Timeout in minutes (default: from config)')
      .setRequired(false)
  )
  .addBooleanOption(option =>
    option.setName('ephemeral')
      .setDescription('Show output only to you')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const command = interaction.options.getString('command', true);
  const timeout = interaction.options.getNumber('timeout') ?? undefined;
  const ephemeral = interaction.options.getBoolean('ephemeral') ?? false;
  const channelId = interaction.channelId;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const validation = securityService.validateCommand(command);
  if (!validation.valid) {
    await interaction.reply({
      content: `❌ ${validation.reason}`,
      flags: 64,
    });
    return;
  }

  const cooldown = securityService.checkCooldown(userId);
  if (!cooldown.allowed) {
    await interaction.reply({
      content: `⏳ Please wait ${Math.ceil((cooldown.remainingMs || 0) / 1000)} seconds before running another command.`,
      flags: 64,
    });
    return;
  }

  const resolvedCommand = securityService.resolveAlias(command);
  const session = terminalService.getOrCreateSession(channelId);

  if (terminalService.isProcessRunning(channelId)) {
    await interaction.reply({
      content: '⚠️ A process is already running in this channel. Please stop it first.',
      flags: 64,
    });
    return;
  }

  await interaction.deferReply({ ephemeral });

  const statusEmbed = new EmbedBuilder()
    .setTitle('Executing Command')
    .setColor(Colors.Yellow)
    .addFields(
      { name: 'Command', value: `\`${resolvedCommand}\``, inline: false },
      { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
      { name: 'Timeout', value: timeout ? `${timeout} min` : 'default', inline: true },
      { name: 'Status', value: '⏳ Running...', inline: true }
    )
    .setTimestamp();

  const stopButton = new ButtonBuilder()
    .setCustomId(`stop_${channelId}`)
    .setLabel('Stop')
    .setStyle(ButtonStyle.Danger);

  const refreshButton = new ButtonBuilder()
    .setCustomId(`refresh_${channelId}`)
    .setLabel('Refresh')
    .setStyle(ButtonStyle.Secondary);

  const row = new ActionRowBuilder<ButtonBuilder>()
    .addComponents(stopButton, refreshButton);

  const sendGuaranteedCompletionNotice = async (text: string): Promise<void> => {
    const message = text || 'Command finished, but final response could not be rendered.';

    try {
      await interaction.editReply({
        content: message,
        embeds: [],
        components: [],
      });
      return;
    } catch {}

    try {
      await interaction.followUp({ content: message, ephemeral });
      return;
    } catch {}

    try {
      if (interaction.channel && typeof interaction.channel.send === 'function') {
        await interaction.channel.send(message);
        return;
      }
    } catch {}

    loggerService.error('All completion response fallback attempts failed', {
      channelId,
      userId,
      command: resolvedCommand,
    });
  };

  const completionTrace = (stage: string, extra: Record<string, unknown> = {}): void => {
    loggerService.info('Run command completion stage', {
      channelId,
      userId,
      command: resolvedCommand,
      stage,
      ...extra,
    });
  };

  await interaction.editReply({
    embeds: [statusEmbed],
    components: [row],
  });

  const outputs: string[] = [];
  const streamFlushIntervalMs = Math.max(200, config.streamFlushIntervalMs || 800);
  const streamFlushMaxChars = Math.max(500, config.streamFlushMaxChars || 3000);
  let streamBuffer = '';
  let streamFlushTimer: NodeJS.Timeout | null = null;
  let streamSendChain: Promise<void> = Promise.resolve();

  const startTime = Date.now();

  const enqueueStreamOutput = (text: string): void => {
    const normalized = (text || '').trim();
    if (!normalized) return;

    const parts = splitMessage(normalized, 3500);
    streamSendChain = streamSendChain
      .then(async () => {
        for (const part of parts) {
          await interaction.followUp({
            content: `📤 **Output:**\n\`\`\`\n${part}\n\`\`\``,
            ephemeral,
          });
        }
      })
      .catch((streamError) => {
        loggerService.warn('Failed to stream command output chunk', {
          channelId,
          userId,
          command: resolvedCommand,
          error: streamError instanceof Error ? streamError.message : String(streamError),
        });
      });
  };

  const flushStreamBuffer = (): void => {
    if (!streamBuffer.trim()) return;
    const chunk = streamBuffer;
    streamBuffer = '';
    enqueueStreamOutput(chunk);
  };

  const scheduleStreamFlush = (): void => {
    if (streamFlushTimer) return;
    streamFlushTimer = setTimeout(() => {
      streamFlushTimer = null;
      flushStreamBuffer();
    }, streamFlushIntervalMs);
  };

  try {
    await terminalService.executeCommand(
      channelId,
      userId,
      resolvedCommand,
      (data, type) => {
        const prefix = type === 'stderr' ? '❌ ' : '';
        const text = prefix + data;
        outputs.push(text);

        streamBuffer += text;
        if (streamBuffer.length >= streamFlushMaxChars) {
          if (streamFlushTimer) {
            clearTimeout(streamFlushTimer);
            streamFlushTimer = null;
          }
          flushStreamBuffer();
          return;
        }

        scheduleStreamFlush();
      },
      async (exitCode, signal) => {
        try {
          completionTrace('onComplete:start', { exitCode, signal });
          const duration = Date.now() - startTime;
          if (streamFlushTimer) {
            clearTimeout(streamFlushTimer);
            streamFlushTimer = null;
          }
          flushStreamBuffer();
          completionTrace('onComplete:stream-flush-requested');
          const streamChainSnapshot = streamSendChain;
          await Promise.race([
            streamSendChain,
            new Promise<void>((resolve) => setTimeout(resolve, 10000)),
          ]);
          completionTrace('onComplete:stream-flush-finished');

          if (streamChainSnapshot !== streamSendChain) {
            loggerService.warn('Command output stream still draining while finalizing response', {
              channelId,
              userId,
              command: resolvedCommand,
            });
          }
          
          securityService.logCommand({
            userId,
            channelId,
            command: resolvedCommand,
            status: exitCode === 0 ? 'success' : 'failed',
            duration,
          });

          const combinedOutput = outputs.join('');
          memoryService.trackCommand(
            resolvedCommand,
            session.cwd,
            exitCode,
            duration,
            userId,
            channelId,
            combinedOutput
          );

          const messages = splitMessage(combinedOutput, 1700);
          completionTrace('onComplete:result-built', { outputParts: messages.length, outputLength: combinedOutput.length });
          const resultEmbed = new EmbedBuilder()
            .setTitle(exitCode === 0 && !signal ? 'Command Completed' : signal ? 'Process Stopped' : 'Command Failed')
            .setColor(exitCode === 0 && !signal ? Colors.Green : signal ? Colors.Orange : Colors.Red)
            .addFields(
              { name: 'Command', value: `\`${resolvedCommand}\``, inline: false },
              { name: 'Timeout', value: timeout ? `${timeout} min` : 'default', inline: true },
              { name: 'Exit Code', value: exitCode?.toString() || signal || 'N/A', inline: true },
              { name: 'Duration', value: `${duration}ms`, inline: true },
              { name: 'Output', value: messages.length > 0 ? `See ${messages.length} output message(s) below.` : 'No output', inline: false }
            )
            .setTimestamp();

          await interaction.editReply({
            embeds: [resultEmbed],
            components: [],
          });
          completionTrace('onComplete:reply-edited');

          for (let i = 0; i < messages.length; i++) {
            try {
              await interaction.followUp({
                content: `**Output Part ${i + 1}/${messages.length}:**\n\`\`\`\n${messages[i]}\n\`\`\``,
                ephemeral,
              });
            } catch (followUpError) {
              loggerService.warn('Failed to send output part follow-up', {
                channelId,
                userId,
                command: resolvedCommand,
                part: i + 1,
                total: messages.length,
                error: followUpError instanceof Error ? followUpError.message : String(followUpError),
              });
            }
          }
          completionTrace('onComplete:done');
        } catch (finalizeError) {
          loggerService.error('Failed to finalize command response', {
            channelId,
            userId,
            command: resolvedCommand,
            error: finalizeError instanceof Error ? finalizeError.message : String(finalizeError),
          });

          await sendGuaranteedCompletionNotice(`⚠️ Command finished. Exit: ${exitCode ?? signal ?? 'N/A'} (final render had an error).`);
          completionTrace('onComplete:fallback-sent', { exitCode, signal });
        }
      },
      timeout
    );
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    
    const errorEmbed = new EmbedBuilder()
      .setTitle('Error')
      .setColor(Colors.Red)
      .addFields(
        { name: 'Command', value: `\`${resolvedCommand}\``, inline: false },
        { name: 'Error', value: errorMessage, inline: false }
      )
      .setTimestamp();

    await interaction.editReply({
      embeds: [errorEmbed],
      components: [],
    });

    securityService.logCommand({
      userId,
      channelId,
      command: resolvedCommand,
      status: 'failed',
      output: errorMessage,
    });
  }
}

export default { data, execute };
