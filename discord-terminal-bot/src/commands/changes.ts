import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { gitService, securityService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('changes')
  .setDescription('Show git working tree changes')
  .addStringOption(option =>
    option.setName('path')
      .setDescription('Working directory')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  if (!securityService.isOwner(interaction.user.id)) {
    await interaction.reply({ content: '❌ You are not authorized to use this command.', flags: 64 });
    return;
  }

  const workdir = interaction.options.getString('path') || process.cwd();
  gitService.setCwd(workdir);

  try {
    const status = await gitService.getStatus();
    const embed = new EmbedBuilder()
      .setTitle('Git Changes')
      .setColor(Colors.Blue)
      .setDescription(`Path: \`${workdir}\`\nBranch: \`${status.branch || 'unknown'}\``)
      .addFields(
        { name: 'Modified', value: status.modified.length ? status.modified.slice(0, 20).map(f => `• ${f}`).join('\n').slice(0, 1024) : 'None', inline: false },
        { name: 'Staged', value: status.staged.length ? status.staged.slice(0, 20).map(f => `• ${f}`).join('\n').slice(0, 1024) : 'None', inline: false },
        { name: 'Untracked', value: status.untracked.length ? status.untracked.slice(0, 20).map(f => `• ${f}`).join('\n').slice(0, 1024) : 'None', inline: false },
      )
      .setFooter({ text: `Ahead: ${status.ahead} | Behind: ${status.behind}` })
      .setTimestamp();

    await interaction.reply({ embeds: [embed], flags: 64 });
  } catch (error: any) {
    await interaction.reply({ content: `❌ Failed to read git changes: ${error.message}`, flags: 64 });
  }
}

export default { data, execute };
