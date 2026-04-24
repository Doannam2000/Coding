import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { aiService, memoryService, securityService, terminalService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('health')
  .setDescription('Check bot, AI backend, and runtime health');

export async function execute(interaction: any): Promise<void> {
  if (!securityService.isOwner(interaction.user.id)) {
    await interaction.reply({ content: '❌ You are not authorized to use this command.', flags: 64 });
    return;
  }

  await interaction.deferReply({ flags: 64 });

  const aiHealthy = aiService.getCLI() === 'opencode' && !aiService.usesOpenCodeDirectCLI()
    ? await aiService.healthCheck().catch(() => false)
    : true;
  const stats = memoryService.getStats();
  const sessions = terminalService.getAllSessions();
  const backendLabel = aiService.getCLI() === 'opencode' && !aiService.usesOpenCodeDirectCLI()
    ? `${aiHealthy ? 'Healthy' : 'Unhealthy'} (${aiService.getCliModeLabel()})`
    : aiService.getCliModeLabel();

  const embed = new EmbedBuilder()
    .setTitle('Bot Health')
    .setColor(aiHealthy ? Colors.Green : Colors.Orange)
    .setDescription(`Current CLI: \`${aiService.getCLI()}\`\nCurrent model: \`${aiService.getDefaultModel()}\``)
    .addFields(
      { name: 'AI Backend', value: backendLabel, inline: true },
      { name: 'Sessions', value: `${sessions.length}`, inline: true },
      { name: 'Memory', value: `${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB`, inline: true },
      { name: 'Tracked Commands', value: `${stats.totalCommands}`, inline: true },
      { name: 'Tracked Projects', value: `${stats.totalProjects}`, inline: true },
      { name: 'Uptime', value: stats.uptime, inline: true },
    )
    .setTimestamp();

  await interaction.editReply({ embeds: [embed] });
}

export default { data, execute };
