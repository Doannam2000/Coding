import { SlashCommandBuilder, EmbedBuilder, Colors, Attachment } from 'discord.js';
import { securityService } from '../services';
import fs from 'fs';
import path from 'path';

export const data = new SlashCommandBuilder()
  .setName('upload')
  .setDescription('Upload a file to the server')
  .addAttachmentOption(option =>
    option.setName('file')
      .setDescription('The file to upload')
      .setRequired(true)
  )
  .addStringOption(option =>
    option.setName('path')
      .setDescription('Destination path (default: current directory)')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  await interaction.deferReply({ flags: 64 });

  const attachment = interaction.options.getAttachment('file');
  const destinationPath = interaction.options.getString('path') || process.cwd();

  try {
    const response = await fetch(attachment.url);
    if (!response.ok) {
      throw new Error(`Failed to download file: ${response.statusText}`);
    }

    const buffer = Buffer.from(await response.arrayBuffer());
    const fileName = attachment.name;
    const fullPath = path.join(destinationPath, fileName);

    const dirExists = fs.existsSync(destinationPath);
    if (!dirExists) {
      fs.mkdirSync(destinationPath, { recursive: true });
    }

    fs.writeFileSync(fullPath, buffer);

    const stats = fs.statSync(fullPath);
    
    await interaction.editReply({
      embeds: [
        new EmbedBuilder()
          .setTitle('File Uploaded Successfully')
          .setColor(Colors.Green)
          .addFields(
            { name: 'File Name', value: fileName, inline: true },
            { name: 'Size', value: formatBytes(stats.size), inline: true },
            { name: 'Destination', value: `\`${fullPath}\``, inline: false }
          )
          .setTimestamp()
      ],
    });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    
    await interaction.editReply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Upload Failed')
          .setColor(Colors.Red)
          .addFields(
            { name: 'Error', value: errorMessage, inline: false }
          )
          .setTimestamp()
      ],
    });
  }
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export default { data, execute };
