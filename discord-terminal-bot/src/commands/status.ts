import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { terminalService, securityService } from '../services';
import config from '../config';

export const data = new SlashCommandBuilder()
  .setName('status')
  .setDescription('Check terminal session and system status')
  .addStringOption(option =>
    option.setName('type')
      .setDescription('Type of status to check')
      .setRequired(false)
      .addChoices(
        { name: 'Session', value: 'session' },
        { name: 'Process', value: 'process' },
        { name: 'System', value: 'system' }
      )
  );

export async function execute(interaction: any): Promise<void> {
  const type = interaction.options.getString('type') || 'session';
  const channelId = interaction.channelId;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const session = terminalService.getSession(channelId);

  if (type === 'session' || type === 'process') {
    if (!session) {
      await interaction.reply({
        content: 'No terminal session found for this channel. Run a command to create one.',
        flags: 64,
      });
      return;
    }

    const processInfo = session.activeProcess;
    
    const embed = new EmbedBuilder()
      .setTitle('Terminal Status')
      .setColor(Colors.Blue)
      .addFields(
        { name: 'Channel ID', value: channelId, inline: true },
        { name: 'Current Directory', value: `\`${session.cwd}\``, inline: false },
        { name: 'History Count', value: session.history.length.toString(), inline: true },
        { name: 'Active Process', value: processInfo ? '✅ Yes' : '❌ No', inline: true }
      );

    if (processInfo) {
      const duration = Date.now() - processInfo.startTime.getTime();
      embed.addFields(
        { name: 'Running Command', value: `\`${processInfo.command}\``, inline: false },
        { name: 'PID', value: processInfo.process.pid?.toString() || 'N/A', inline: true },
        { name: 'Duration', value: `${duration}ms`, inline: true }
      );
    }

    if (type === 'session') {
      const history = session.history.slice(-10);
      if (history.length > 0) {
        embed.addFields({
          name: 'Recent Commands',
          value: history.map((cmd, i) => `\`${i + 1}.\` ${cmd}`).join('\n'),
          inline: false,
        });
      }
    }

    await interaction.reply({
      embeds: [embed],
      flags: 64,
    });
  }

  if (type === 'system') {
    const os = require('os');
    
    const embed = new EmbedBuilder()
      .setTitle('System Status')
      .setColor(Colors.Green)
      .addFields(
        { name: 'Platform', value: `${os.platform()} (${os.arch()})`, inline: true },
        { name: 'CPU Cores', value: os.cpus().length.toString(), inline: true },
        { name: 'Hostname', value: os.hostname(), inline: true },
        { name: 'Total RAM', value: formatBytes(os.totalmem()), inline: true },
        { name: 'Free RAM', value: formatBytes(os.freemem()), inline: true },
        { name: 'Uptime', value: formatUptime(os.uptime()), inline: true },
        { name: 'Node.js Version', value: process.version, inline: true },
        { name: 'Active Sessions', value: terminalService.getAllSessions().length.toString(), inline: true }
      )
      .setTimestamp();

    await interaction.reply({
      embeds: [embed],
      flags: 64,
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

function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;

  const parts: string[] = [];
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0) parts.push(`${minutes}m`);
  if (secs > 0 || parts.length === 0) parts.push(`${secs}s`);

  return parts.join(' ');
}

export default { data, execute };
