import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { aiService, memoryService, securityService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('usage')
  .setDescription('Show recent AI usage tracked by this bot')
  .addNumberOption(option =>
    option.setName('days')
      .setDescription('Number of days to summarize')
      .setRequired(false)
  )
  .addStringOption(option =>
    option.setName('cli')
      .setDescription('Optional CLI filter')
      .setRequired(false)
      .addChoices(
        { name: 'opencode', value: 'opencode' },
        { name: 'claude', value: 'claude' },
        { name: 'codex', value: 'codex' },
      )
  );

export async function execute(interaction: any): Promise<void> {
  if (!securityService.isOwner(interaction.user.id)) {
    await interaction.reply({ content: '❌ You are not authorized to use this command.', flags: 64 });
    return;
  }

  const days = interaction.options.getNumber('days') || 7;
  const cli = interaction.options.getString('cli') || undefined;
  const rows = memoryService.summarizeAIUsage(days, cli as any);
  const recent = memoryService.getAIUsageHistory(10, cli as any);

  const embed = new EmbedBuilder()
    .setTitle('AI Usage')
    .setColor(Colors.Blue)
    .setDescription(`Tracked requests from this bot over the last ${days} day(s). Current CLI: \`${aiService.getCLI()}\``)
    .setTimestamp();

  embed.addFields({
    name: 'Daily Summary',
    value: rows.length > 0
      ? rows.slice(0, 10).map(row =>
          `\`${row.day}\` | \`${row.cli}\` | req=${row.requests} | total=${row.totalTokens} | in=${row.inputTokens} | out=${row.outputTokens} | models=${row.models.slice(0, 3).join(', ')}`
        ).join('\n').slice(0, 1024)
      : 'No tracked AI usage yet.',
    inline: false,
  });

  embed.addFields({
    name: 'Recent Requests',
    value: recent.length > 0
      ? recent.map(item =>
          `\`${item.timestamp.slice(0, 16).replace('T', ' ')}\` | \`${item.cli}\` | \`${item.mode}\` | \`${item.model}\` | total=${item.totalTokens || 0}`
        ).join('\n').slice(0, 1024)
      : 'No recent requests.',
    inline: false,
  });

  await interaction.reply({ embeds: [embed], flags: 64 });
}

export default { data, execute };
