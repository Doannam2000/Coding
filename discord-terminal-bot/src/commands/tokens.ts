import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { execFileSync } from 'child_process';
import { aiService, memoryService, securityService } from '../services';
import { getOpenCodeLauncher } from '../utils';

function extractValue(output: string, patterns: RegExp[]): string | null {
  for (const pattern of patterns) {
    const match = output.match(pattern);
    if (match?.[1]) {
      return match[1].trim();
    }
  }
  return null;
}

function extractPercent(value: string | null): number | null {
  if (!value) return null;
  const match = value.match(/(\d+(?:\.\d+)?)\s*%/);
  return match ? Number(match[1]) : null;
}

function buildProgressBar(percent: number, size: number = 10): string {
  const clamped = Math.max(0, Math.min(100, percent));
  const filled = Math.round((clamped / 100) * size);
  return `${'█'.repeat(filled)}${'░'.repeat(size - filled)} ${clamped.toFixed(1)}%`;
}

function buildUsageAlert(remainingPercent: number | null): string | null {
  if (remainingPercent === null) return null;
  if (remainingPercent <= 10) return '🚨 Remaining quota is critically low';
  if (remainingPercent <= 20) return '⚠️ Remaining quota is getting low';
  return null;
}

function extractModelBreakdown(output: string): string[] {
  return output
    .split('\n')
    .map(line => line.trim())
    .filter(line => /[a-z0-9_-]+\/[a-z0-9._-]+/i.test(line) && /(token|cost|\$|%)/i.test(line))
    .slice(0, 6);
}

function parseTokenStats(output: string) {
  const used = extractValue(output, [/Used\s+([^\n]+)/i, /Usage\s+([^\n]+)/i]);
  const remaining = extractValue(output, [/Remaining\s+([^\n]+)/i, /Left\s+([^\n]+)/i, /Balance\s+([^\n]+)/i]);
  const limit = extractValue(output, [/Limit\s+([^\n]+)/i, /Quota\s+([^\n]+)/i]);
  const totalCost = extractValue(output, [/Total Cost\s+([^\n]+)/i, /Cost\s+([^\n]+)/i]);
  const inputTokens = extractValue(output, [/Input Tokens\s+([^\n]+)/i, /Input\s+([^\n]+)/i]);
  const outputTokens = extractValue(output, [/Output Tokens\s+([^\n]+)/i, /Output\s+([^\n]+)/i]);
  const totalTokens = extractValue(output, [/Total Tokens\s+([^\n]+)/i]);
  const modelBreakdown = extractModelBreakdown(output);

  return {
    used,
    remaining,
    limit,
    totalCost,
    inputTokens,
    outputTokens,
    totalTokens,
    usedPercent: extractPercent(used),
    remainingPercent: extractPercent(remaining),
    modelBreakdown,
  };
}

function buildTrend(history = memoryService.getTokenHistory(7)): string {
  if (history.length === 0) return 'No history yet';
  return history
    .slice(0, 7)
    .reverse()
    .map(item => {
      const day = new Date(item.timestamp).toLocaleDateString();
      const remaining = item.remaining || 'N/A';
      return `${day}: ${remaining}`;
    })
    .join('\n')
    .slice(0, 1024);
}

function buildTokenSummary(output: string): { name: string; value: string; inline?: boolean }[] {
  const stats = parseTokenStats(output);
  const { totalCost, inputTokens, outputTokens, totalTokens, used, remaining, limit, usedPercent, remainingPercent, modelBreakdown } = stats;

  const fields: { name: string; value: string; inline?: boolean }[] = [];

  if (used) fields.push({ name: 'Used', value: used, inline: true });
  if (remaining) fields.push({ name: 'Remaining', value: remaining, inline: true });
  if (limit) fields.push({ name: 'Limit', value: limit, inline: true });

  if (usedPercent !== null || remainingPercent !== null) {
    const usedValue = usedPercent !== null ? `${usedPercent.toFixed(1)}%` : 'Unknown';
    const remainingValue = remainingPercent !== null ? `${remainingPercent.toFixed(1)}%` : 'Unknown';
    fields.push({ name: 'Usage', value: `Used: ${usedValue}\nLeft: ${remainingValue}`, inline: true });
  }

  if (usedPercent !== null) {
    fields.push({ name: 'Used Bar', value: buildProgressBar(usedPercent), inline: false });
  }

  if (remainingPercent !== null) {
    fields.push({ name: 'Remaining Bar', value: buildProgressBar(remainingPercent), inline: false });
  }

  const alert = buildUsageAlert(remainingPercent);
  if (alert) {
    fields.push({ name: 'Alert', value: alert, inline: false });
  }

  if (totalCost) fields.push({ name: 'Total Cost', value: totalCost, inline: true });
  if (inputTokens) fields.push({ name: 'Input Tokens', value: inputTokens, inline: true });
  if (outputTokens) fields.push({ name: 'Output Tokens', value: outputTokens, inline: true });
  if (totalTokens) fields.push({ name: 'Total Tokens', value: totalTokens, inline: true });

  if (modelBreakdown.length > 0) {
    fields.push({ name: 'By Model', value: modelBreakdown.join('\n').slice(0, 1024), inline: false });
  }

  fields.push({ name: 'Trend', value: buildTrend(), inline: false });

  const watch = memoryService.getTokenWatch();
  fields.push({
    name: 'Watch',
    value: `Enabled: ${watch.enabled ? 'Yes' : 'No'}\nThreshold: ${watch.thresholdPercent}%\nSubscribed channels: ${watch.discordChannels.length}`,
    inline: false,
  });

  return fields;
}

export const data = new SlashCommandBuilder()
  .setName('tokens')
  .setDescription('Check token/quota usage for the active AI CLI')
  .addStringOption(option =>
    option.setName('action')
      .setDescription('Optional action')
      .setRequired(false)
      .addChoices(
        { name: 'Show', value: 'show' },
        { name: 'Watch On', value: 'watch-on' },
        { name: 'Watch Off', value: 'watch-off' },
        { name: 'Watch Status', value: 'watch-status' }
      )
  )
  .addNumberOption(option =>
    option.setName('days')
      .setDescription('Show stats for the last N days')
      .setRequired(false)
  )
  .addNumberOption(option =>
    option.setName('threshold')
      .setDescription('Low quota alert threshold percent')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;
  const currentCLI = aiService.getCLI();

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const days = interaction.options.getNumber('days');
  const action = interaction.options.getString('action') || 'show';
  const threshold = interaction.options.getNumber('threshold');

  await interaction.deferReply({ flags: 64 });

  try {
    if (currentCLI !== 'opencode') {
      const cliLabel = aiService.getCliDisplayName(currentCLI);
      const unsupportedEmbed = new EmbedBuilder()
        .setTitle(`📊 ${cliLabel} Token Usage`)
        .setColor(Colors.Yellow)
        .setDescription(`${cliLabel} CLI does not expose quota/balance stats through this bot.`)
        .addFields(
          {
            name: 'Current CLI',
            value: `\`${currentCLI}\``,
            inline: true,
          },
          {
            name: 'Current model',
            value: `\`${aiService.getDefaultModel()}\``,
            inline: true,
          },
          {
            name: 'What works',
            value: 'Per-request token counts still appear in `/ai` and `/chat` responses when the CLI returns them.',
            inline: false,
          }
        )
        .setTimestamp();

      await interaction.editReply({ embeds: [unsupportedEmbed] });
      return;
    }

    if (action === 'watch-on') {
      if (threshold !== null) {
        memoryService.updateTokenWatch({ thresholdPercent: threshold, enabled: true });
      } else {
        memoryService.updateTokenWatch({ enabled: true });
      }
      memoryService.subscribeTokenWatch({ discordChannel: interaction.channelId });
      const watch = memoryService.getTokenWatch();
      await interaction.editReply({ content: `✅ Token watch enabled at ${watch.thresholdPercent}% for this channel.` });
      return;
    }

    if (action === 'watch-off') {
      memoryService.unsubscribeTokenWatch({ discordChannel: interaction.channelId });
      await interaction.editReply({ content: '✅ Token watch disabled for this channel.' });
      return;
    }

    if (action === 'watch-status') {
      const watch = memoryService.getTokenWatch();
      await interaction.editReply({
        content: `Watch enabled: ${watch.enabled ? 'Yes' : 'No'}\nThreshold: ${watch.thresholdPercent}%\nDiscord channels: ${watch.discordChannels.length}\nTelegram chats: ${watch.telegramChats.length}`,
      });
      return;
    }

    const launcher = getOpenCodeLauncher();
    const output = execFileSync(launcher.command, [...launcher.args, 'stats', ...(days ? ['--days', String(days)] : [])], {
      encoding: 'utf-8',
      windowsHide: true,
    });

    memoryService.addTokenSnapshot({
      timestamp: new Date().toISOString(),
      ...parseTokenStats(output),
    });

    const embed = new EmbedBuilder()
      .setTitle('📊 OpenCode Token Usage')
      .setColor(Colors.Blue)
      .setTimestamp();

    const fields = buildTokenSummary(output);
    embed.addFields(fields.length > 0 ? fields : [{ name: 'Stats', value: '```\n' + output.trim().slice(0, 3500) + '\n```', inline: false }]);

    await interaction.editReply({ embeds: [embed] });
  } catch (error: any) {
    const errorEmbed = new EmbedBuilder()
      .setTitle('❌ Error')
      .setColor(Colors.Red)
      .setDescription(`Failed to get token stats: ${error.message}`)
      .setTimestamp();

    await interaction.editReply({ embeds: [errorEmbed] });
  }
}

export default { data, execute };
