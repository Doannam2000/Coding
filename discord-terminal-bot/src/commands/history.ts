import { SlashCommandBuilder, EmbedBuilder, Colors, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { terminalService, securityService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('history')
  .setDescription('Show command history for this channel')
  .addIntegerOption(option =>
    option.setName('lines')
      .setDescription('Number of recent commands to show')
      .setRequired(false)
      .setMinValue(1)
      .setMaxValue(100)
  )
  .addBooleanOption(option =>
    option.setName('clear')
      .setDescription('Clear the command history')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const lines = interaction.options.getInteger('lines') || 10;
  const clear = interaction.options.getBoolean('clear') ?? false;
  const channelId = interaction.channelId;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  if (clear) {
    terminalService.clearHistory(channelId);
    
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('History Cleared')
          .setColor(Colors.Green)
          .setDescription('Command history has been cleared for this channel.')
          .setTimestamp()
      ],
      flags: 64,
    });
    return;
  }

  const history = terminalService.getHistory(channelId);

  if (history.length === 0) {
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Command History')
          .setColor(Colors.Blue)
          .setDescription('No commands in history for this channel.')
          .setTimestamp()
      ],
      flags: 64,
    });
    return;
  }

  const recentHistory = history.slice(-lines);
  const historyText = recentHistory
    .map((cmd, i) => `\`${history.length - recentHistory.length + i + 1}.\` \`${cmd}\``)
    .join('\n');

  await interaction.reply({
    embeds: [
      new EmbedBuilder()
        .setTitle('Command History')
        .setColor(Colors.Blue)
        .setDescription(historyText)
        .setFooter({ text: `Showing ${recentHistory.length} of ${history.length} total commands` })
        .setTimestamp()
    ],
    components: [
      new ActionRowBuilder<ButtonBuilder>().addComponents(
        new ButtonBuilder()
          .setCustomId(`historyclear_${channelId}`)
          .setLabel('Clear History')
          .setStyle(ButtonStyle.Danger)
      )
    ],
    flags: 64,
  });
}

export default { data, execute };
