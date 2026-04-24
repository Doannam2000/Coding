import { SlashCommandBuilder, EmbedBuilder, Colors, ButtonBuilder, ButtonStyle, ActionRowBuilder } from 'discord.js';
import { securityService, loggerService, aiService, memoryService, terminalService } from '../services';
import { buildCodeBlockMessages, createCoalescedAsyncRenderer, normalizeErrorMessage } from '../utils';

const chatSessions: Map<string, { workdir: string }> = new Map();

export const data = new SlashCommandBuilder()
  .setName('chat')
  .setDescription('Chat with the active AI CLI - remembers conversation')
  .addStringOption(option =>
    option.setName('message')
      .setDescription('Your message')
      .setRequired(true)
  )
  .addStringOption(option =>
    option.setName('path')
      .setDescription('Working directory')
      .setRequired(false)
  )
  .addBooleanOption(option =>
    option.setName('clear')
      .setDescription('Clear chat history')
      .setRequired(false)
  )
  .addStringOption(option =>
    option.setName('model')
      .setDescription('AI model override')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const message = interaction.options.getString('message', true);
  const activeCLI = aiService.getCLI();
  const workdir = interaction.options.getString('path') || (interaction.channelId ? (terminalService.getSession(interaction.channelId)?.cwd || process.cwd()) : process.cwd());
  const clear = interaction.options.getBoolean('clear') ?? false;
  let model = interaction.options.getString('model') || aiService.getDefaultModel(activeCLI);
  const channelId = interaction.channelId;
  const userId = interaction.user.id;
  const sessionKey = `${channelId}_${userId}`;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: 'You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  if (clear) {
    chatSessions.delete(sessionKey);
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Chat Cleared')
          .setColor(Colors.Green)
          .setDescription('Chat history has been cleared.')
          .setTimestamp(),
      ],
      flags: 64,
    });
    return;
  }

  const modelValidation = await aiService.validateModelSelectionForCLI(activeCLI, model);
  if (!modelValidation.ok) {
    await interaction.reply({
      content: `Error: ${modelValidation.error}`,
      flags: 64,
    });
    return;
  }
  model = modelValidation.normalized || model;

  const project = memoryService.getProjectByPath(workdir);
  if (!project) {
    await interaction.reply({
      content: 'Select a project first with `/project`, then use `/chat`.',
      flags: 64,
    });
    return;
  }

  if (activeCLI === 'opencode' && !aiService.isOpenCodeReadyForWorkdir(workdir)) {
    await interaction.reply({
      content: 'AI is not started for this project yet. Run `/ai` first to start/check AI, then use `/chat`.',
      flags: 64,
    });
    return;
  }

  await interaction.deferReply({ ephemeral: false });
  const cliName = aiService.getCliDisplayName(activeCLI);

  chatSessions.set(sessionKey, { workdir });

  const stopButton = new ButtonBuilder()
    .setCustomId(`chat_stop_${channelId}`)
    .setLabel('Stop')
    .setStyle(ButtonStyle.Danger);
  const row = new ActionRowBuilder<ButtonBuilder>().addComponents(stopButton);

  const reply = await interaction.editReply({
    content: '**Thinking...**',
    components: [row],
  });

  const streamMessages: any[] = [reply];
  const startTime = Date.now();

  const renderParts = async (text: string, withStopButton: boolean): Promise<void> => {
    const parts = buildCodeBlockMessages(text || '...', `**${cliName} Chat**`, 2000);

    while (streamMessages.length < parts.length) {
      const nextMessage = await interaction.followUp({
        content: parts[streamMessages.length],
        ephemeral: false,
      });
      streamMessages.push(nextMessage);
    }

    for (let i = 0; i < parts.length; i++) {
      const payload: any = { content: parts[i] };
      if (i === 0) {
        payload.components = withStopButton ? [row] : [];
      }
      await streamMessages[i].edit(payload);
    }
  };

  const streamRenderer = createCoalescedAsyncRenderer<{ text: string; withStopButton: boolean }>(
    ({ text, withStopButton }) => renderParts(text, withStopButton),
    (error) => {
      loggerService.warn('Failed to render Discord chat stream chunk', {
        channelId,
        userId,
        cli: activeCLI,
        model,
        error: normalizeErrorMessage(error),
      });
    }
  );

  try {
    const response = await aiService.chatStream(
      [],
      message,
      (chunk) => {
        streamRenderer.schedule({ text: chunk, withStopButton: true });
      },
      { workdir, model, cli: activeCLI }
    );

    const duration = Date.now() - startTime;
    const cleanText = response.text.replace(/\x1b\[[0-9;]*m/g, '').trim();
    const renderedText = (response.displayText || response.text).replace(/\x1b\[[0-9;]*m/g, '').trim();

    await streamRenderer.flush();
    await renderParts(renderedText || cleanText || 'No response', false);

    if (response.tokens) {
      memoryService.addAIUsageSnapshot({
        timestamp: new Date().toISOString(),
        cli: activeCLI,
        model,
        mode: 'chat',
        channelId,
        userId,
        cwd: workdir,
        inputTokens: response.tokens.input,
        outputTokens: response.tokens.output,
        totalTokens: response.tokens.total,
        cost: response.tokens.cost,
      });
    }

    const summaryParts = [
      `${cliName} chat finished`,
      `Time: ${duration}ms`,
      `Messages: 1`,
    ];
    if (response.tokens) {
      summaryParts.push(`Tokens: in=${response.tokens.input} | out=${response.tokens.output} | total=${response.tokens.total}`);
      if (response.tokens.quota) {
        const q = response.tokens.quota;
        if (q.remaining) summaryParts.push(`Remaining: ${q.remaining}`);
        if (q.usedPercent) summaryParts.push(`${q.usedPercent}% used`);
        if (q.resetsIn) summaryParts.push(`Resets: ${q.resetsIn}`);
      }
      summaryParts.push(`Cost: ${response.tokens.cost > 0 ? `$${response.tokens.cost.toFixed(6)}` : 'Free'}`);
    }
    const summary = summaryParts.filter(Boolean).join('\n');

    await interaction.followUp({
      content: summary,
      ephemeral: false,
    });

    loggerService.info('Chat response', { userId, channelId, duration, tokens: response.tokens });
  } catch (error: any) {
    const duration = Date.now() - startTime;
    const errorMessage = normalizeErrorMessage(error);

    const errorEmbed = new EmbedBuilder()
      .setTitle('Error')
      .setColor(Colors.Red)
      .setDescription(errorMessage.length > 4000 ? errorMessage.slice(0, 4000) : errorMessage)
      .addFields({ name: 'Duration', value: `${duration}ms`, inline: true })
      .setTimestamp();

    await reply.edit({
      content: '',
      embeds: [errorEmbed],
      components: [],
    });

    loggerService.error('Chat error', {
      cli: activeCLI,
      model,
      workdir,
      channelId,
      userId,
      error: errorMessage,
      rawError: error?.stack || String(error),
    });
  }
}

export function stopChat(sessionKey: string): boolean {
  chatSessions.delete(sessionKey);
  return true;
}

export default { data, execute };
