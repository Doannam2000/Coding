import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { securityService, loggerService } from '../services';
import os from 'os';

export const data = new SlashCommandBuilder()
  .setName('ping')
  .setDescription('Check bot latency and system info');

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const wsLatency = interaction.client.ws.ping;
  const startTime = Date.now();
  
  await interaction.deferReply({ flags: 64 });
  
  const apiLatency = Date.now() - startTime;

  const embed = new EmbedBuilder()
    .setTitle('Bot Status')
    .setColor(Colors.Green)
    .addFields(
      { name: 'WebSocket Latency', value: `${wsLatency}ms`, inline: true },
      { name: 'API Latency', value: `${apiLatency}ms`, inline: true },
      { name: 'Platform', value: os.platform(), inline: true },
      { name: 'Node.js', value: process.version, inline: true },
      { name: 'Memory Usage', value: `${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB`, inline: true },
      { name: 'Uptime', value: formatUptime(process.uptime()), inline: true }
    )
    .setTimestamp();

  await interaction.editReply({
    embeds: [embed],
  });
}

function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  const parts: string[] = [];
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0) parts.push(`${minutes}m`);
  if (secs > 0 || parts.length === 0) parts.push(`${secs}s`);

  return parts.join(' ');
}

export default { data, execute };
