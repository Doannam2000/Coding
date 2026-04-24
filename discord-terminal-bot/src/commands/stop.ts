import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { terminalService, securityService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('stop')
  .setDescription('Stop the running process in this channel');

export async function execute(interaction: any): Promise<void> {
  const channelId = interaction.channelId;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  if (!terminalService.isProcessRunning(channelId)) {
    await interaction.reply({
      content: 'No process is currently running in this channel.',
      flags: 64,
    });
    return;
  }

  const killed = terminalService.killProcess(channelId);

  if (killed) {
    securityService.logCommand({
      userId,
      channelId,
      command: '[STOP]',
      status: 'stopped',
    });

    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Process Stopped')
          .setColor(Colors.Orange)
          .setDescription('The running process has been terminated.')
          .setTimestamp()
      ],
      flags: 64,
    });
  } else {
    await interaction.reply({
      content: 'Failed to stop the process. It may have already completed.',
      flags: 64,
    });
  }
}

export default { data, execute };
