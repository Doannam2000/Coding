import { SlashCommandBuilder, EmbedBuilder, Colors, ButtonBuilder, ButtonStyle, ActionRowBuilder } from 'discord.js';
import { securityService, loggerService, aiService, memoryService, terminalService } from '../services';
import { buildCodeBlockMessages, createCoalescedAsyncRenderer, normalizeErrorMessage } from '../utils';

interface ActiveRequest {
  abort: () => void;
}

const activeRequests: Map<string, ActiveRequest> = new Map();

export const data = new SlashCommandBuilder()
  .setName('ai')
  .setDescription('Start/check the active AI CLI or ask a one-off prompt')
  .addStringOption(option =>
    option.setName('prompt')
      .setDescription('Optional one-off prompt; omit to start/check AI status')
      .setRequired(false)
  )
  .addStringOption(option =>
    option.setName('model')
      .setDescription('AI model')
      .setRequired(false)
  )
  .addStringOption(option =>
    option.setName('path')
      .setDescription('Working directory')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const prompt = interaction.options.getString('prompt');
  const activeCLI = aiService.getCLI();
  let model = interaction.options.getString('model') || aiService.getDefaultModel(activeCLI);
  const workdir = interaction.options.getString('path') || (interaction.channelId ? (terminalService.getSession(interaction.channelId)?.cwd || process.cwd()) : process.cwd());
  const userId = interaction.user.id;
  const channelId = interaction.channelId;
  const cliName = aiService.getCliDisplayName(activeCLI);

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: 'You are not authorized to use this command.',
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
      content: 'Select a project first with `/project`, then use `/ai` or `/chat`.',
      flags: 64,
    });
    return;
  }

  if (!prompt?.trim()) {
    if (activeCLI === 'opencode' && !aiService.usesOpenCodeDirectCLI()) {
      await interaction.deferReply({ ephemeral: true });
      const started = await aiService.ensureReadyForCLI(activeCLI, workdir);
      if (!started) {
        await interaction.editReply({
          content: `Failed to start ${cliName} for project \`${project.name}\`.`,
        });
        return;
      }
    }

    const status = aiService.getStatus(activeCLI);
    const lines = [
      'AI status',
      `CLI: \`${status.cli}\``,
      `Model: \`${status.model}\``,
      `Project: \`${project.name}\``,
      `CWD: \`${workdir}\``,
      `Mode: ${aiService.getCliModeLabel(activeCLI)}`,
    ];
    if (activeCLI === 'opencode' && !aiService.usesOpenCodeDirectCLI()) {
      lines.push(`Started: ${status.started ? 'Yes' : 'No'}`);
      lines.push(`Ready: ${status.ready ? 'Yes' : 'No'}`);
    }

    if (activeCLI === 'opencode' && !aiService.usesOpenCodeDirectCLI()) {
      await interaction.editReply({
        content: lines.join('\n'),
      });
      return;
    }

    await interaction.reply({
      content: lines.join('\n'),
      flags: 64,
    });
    return;
  }

  await interaction.deferReply({ ephemeral: false });

  const startTime = Date.now();
  const stopButton = new ButtonBuilder()
    .setCustomId(`ai_stop_${channelId}`)
    .setLabel('Stop')
    .setStyle(ButtonStyle.Danger);
  const row = new ActionRowBuilder<ButtonBuilder>().addComponents(stopButton);
  const cliLabel = `${cliName}${model !== aiService.getDefaultModel(activeCLI) ? ` | ${model}` : ''}`;

  const reply = await interaction.editReply({
    content: `**CLI: ${cliLabel}**\nStarting...`,
    components: [row],
  });

  const streamMessages: any[] = [reply];

  const renderParts = async (text: string, withStopButton: boolean): Promise<void> => {
    const parts = buildCodeBlockMessages(text || '...', `**${model}**`, 2000);

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
      loggerService.warn('Failed to render Discord AI stream chunk', {
        channelId,
        userId,
        cli: activeCLI,
        model,
        error: normalizeErrorMessage(error),
      });
    }
  );

  try {
    const response = await aiService.chat(prompt, (chunk) => {
      streamRenderer.schedule({ text: chunk, withStopButton: true });
    }, { workdir, model, cli: activeCLI });

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
        mode: 'ai',
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
      `${cliName} AI finished`,
      `Time: ${duration}ms`,
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

    loggerService.info('AI response', { userId, prompt, duration, tokens: response.tokens });
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

    loggerService.error('AI error', {
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

export function stopAIActivity(channelId: string): boolean {
  const request = activeRequests.get(channelId);
  if (request) {
    request.abort();
    activeRequests.delete(channelId);
    return true;
  }
  return false;
}

export default { data, execute };
