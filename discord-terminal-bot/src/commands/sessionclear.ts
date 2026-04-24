import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { securityService, terminalService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('sessionclear')
  .setDescription('Clear the current terminal session or all sessions')
  .addBooleanOption(option =>
    option
      .setName('all')
      .setDescription('Clear all terminal sessions')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;
  const clearAll = interaction.options.getBoolean('all') ?? false;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: 'You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  if (clearAll) {
    const cleared = terminalService.clearAllSessions();
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Sessions Cleared')
          .setColor(Colors.Green)
          .setDescription(`Cleared ${cleared} terminal session${cleared === 1 ? '' : 's'}.`)
          .setTimestamp(),
      ],
      flags: 64,
    });
    return;
  }

  const channelId = interaction.channelId;
  const existing = terminalService.getSession(channelId);
  if (!existing) {
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('No Session')
          .setColor(Colors.Blue)
          .setDescription('There is no terminal session to clear for this channel.')
          .setTimestamp(),
      ],
      flags: 64,
    });
    return;
  }

  terminalService.destroySession(channelId);
  await interaction.reply({
    embeds: [
      new EmbedBuilder()
        .setTitle('Session Cleared')
        .setColor(Colors.Green)
        .setDescription(`Cleared the terminal session for channel \`${channelId}\`.`)
        .setTimestamp(),
    ],
    flags: 64,
  });
}

export default { data, execute };
