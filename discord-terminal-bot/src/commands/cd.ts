import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { terminalService, securityService } from '../services';
import path from 'path';

export const data = new SlashCommandBuilder()
  .setName('cd')
  .setDescription('Change the current working directory')
  .addStringOption(option =>
    option.setName('directory')
      .setDescription('The directory to change to')
      .setRequired(true)
  );

export async function execute(interaction: any): Promise<void> {
  const newDir = interaction.options.getString('directory', true);
  const channelId = interaction.channelId;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const session = terminalService.getOrCreateSession(channelId);
  
  let targetDir = newDir;
  if (!path.isAbsolute(targetDir)) {
    targetDir = path.resolve(session.cwd, newDir);
  }

  const success = terminalService.changeDirectory(channelId, targetDir);

  if (success) {
    const updatedSession = terminalService.getSession(channelId);
    
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Directory Changed')
          .setColor(Colors.Green)
          .addFields(
            { name: 'New Directory', value: `\`${updatedSession?.cwd || targetDir}\``, inline: false }
          )
          .setTimestamp()
      ],
      flags: 64,
    });
  } else {
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Failed to Change Directory')
          .setColor(Colors.Red)
          .addFields(
            { name: 'Directory', value: `\`${targetDir}\``, inline: false },
            { name: 'Error', value: 'Directory does not exist or is not accessible', inline: false }
          )
          .setTimestamp()
      ],
      flags: 64,
    });
  }
}

export default { data, execute };
