import { SlashCommandBuilder, EmbedBuilder, Colors, AttachmentBuilder } from 'discord.js';
import { securityService, loggerService } from '../services';
import fs from 'fs';
import path from 'path';

export const data = new SlashCommandBuilder()
  .setName('logs')
  .setDescription('View command logs')
  .addIntegerOption(option =>
    option.setName('lines')
      .setDescription('Number of recent logs to show')
      .setRequired(false)
      .setMinValue(1)
      .setMaxValue(100)
  )
  .addBooleanOption(option =>
    option.setName('file')
      .setDescription('Export logs as a file')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const lines = interaction.options.getInteger('lines') || 20;
  const asFile = interaction.options.getBoolean('file') ?? false;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const logs = securityService.getRecentLogs(lines);

  if (logs.length === 0) {
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Command Logs')
          .setColor(Colors.Blue)
          .setDescription('No logs available.')
          .setTimestamp()
      ],
      flags: 64,
    });
    return;
  }

  const logText = logs
    .map(log => `[${log.timestamp.toISOString()}] [${log.status.toUpperCase()}] ${log.userId}: ${log.command}`)
    .join('\n');

  if (asFile) {
    const logPath = path.join(process.cwd(), 'temp_logs.txt');
    fs.writeFileSync(logPath, logText);
    
    const attachment = new AttachmentBuilder(logPath);
    
    await interaction.reply({
      content: 'Here are the command logs:',
      files: [attachment],
      flags: 64,
    });

    fs.unlinkSync(logPath);
  } else {
    const truncatedLogs = logText.length > 3900 ? logText.substring(0, 3900) + '\n...(truncated)' : logText;
    
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Command Logs')
          .setColor(Colors.Blue)
          .setDescription(`\`\`\`\n${truncatedLogs}\n\`\`\``)
          .setFooter({ text: `Showing ${logs.length} logs` })
          .setTimestamp()
      ],
      flags: 64,
    });
  }
}

export default { data, execute };
