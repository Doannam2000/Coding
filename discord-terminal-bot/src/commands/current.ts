import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { securityService } from '../services';
import { buildCurrentContext } from '../utils/current';

export const data = new SlashCommandBuilder()
  .setName('current')
  .setDescription('Show the currently selected project, AI CLI, model, and token status');

export async function execute(interaction: any): Promise<void> {
  if (!securityService.isOwner(interaction.user.id)) {
    await interaction.reply({
      content: 'You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const current = buildCurrentContext(interaction.channelId);
  const embed = new EmbedBuilder()
    .setTitle('Current Context')
    .setColor(Colors.Blue)
    .addFields(
      { name: 'Project', value: current.projectName ? `\`${current.projectName}\`` : 'None selected', inline: true },
      { name: 'Type', value: current.projectType || 'Unknown', inline: true },
      { name: 'CWD', value: `\`${current.cwd}\``, inline: false },
      { name: 'CLI', value: `\`${current.currentCLI}\``, inline: true },
      { name: 'Model', value: `\`${current.currentModel}\``, inline: true },
      { name: 'AI Status', value: current.aiStatus, inline: true },
      { name: 'Token Used', value: current.tokenUsed || 'N/A', inline: true },
      { name: 'Token Remaining', value: current.tokenRemaining || 'N/A', inline: true },
      { name: 'Token Limit', value: current.tokenLimit || 'N/A', inline: true },
      { name: 'Latest Tracked Tokens', value: current.latestTrackedTotalTokens !== null ? `${current.latestTrackedTotalTokens}` : 'N/A', inline: true },
    )
    .setTimestamp();

  await interaction.reply({ embeds: [embed], flags: 64 });
}

export default { data, execute };
