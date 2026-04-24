import { SlashCommandBuilder, ActionRowBuilder, StringSelectMenuBuilder, EmbedBuilder, Colors } from 'discord.js';
import { memoryService } from '../services';
import fs from 'fs';

export const data = new SlashCommandBuilder()
  .setName('memory')
  .setDescription('View bot memory and statistics')
  .addStringOption(option =>
    option.setName('type')
      .setDescription('Type of memory to view')
      .setRequired(false)
      .addChoices(
        { name: 'Stats', value: 'stats' },
        { name: 'Projects', value: 'projects' },
        { name: 'Commands', value: 'commands' },
        { name: 'Clear', value: 'clear' }
      )
  );

export async function execute(interaction: any) {
  const type = interaction.options.getString('type') || 'stats';

  if (!interaction.options.getString('type')) {
    const menu = new StringSelectMenuBuilder()
      .setCustomId('memorypick_select')
      .setPlaceholder('Choose memory view')
      .addOptions(
        { label: 'Stats', value: 'stats', description: 'Show bot memory stats' },
        { label: 'Projects', value: 'projects', description: 'Show tracked projects' },
        { label: 'Commands', value: 'commands', description: 'Show recent commands' },
        { label: 'Clear', value: 'clear', description: 'Clear recent command history' },
      );

    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Memory')
          .setColor(Colors.Blue)
          .setDescription('Choose which memory view to open.')
          .setTimestamp(),
      ],
      components: [new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(menu)],
      ephemeral: true,
    });
    return;
  }

  if (type === 'clear') {
    memoryService.clearHistory();
    await interaction.reply({
      content: '🗑️ Memory history cleared.',
      ephemeral: true
    });
    return;
  }

  if (type === 'projects') {
    const projects = memoryService.getProjects();
    
    if (projects.length === 0) {
      await interaction.reply({
        content: '📭 No projects tracked.',
        ephemeral: true
      });
      return;
    }

    let response = '📁 *Projects*\n\n';
    for (const p of projects) {
      const exists = fs.existsSync(p.path);
      const emoji = exists ? '✅' : '⚠️';
      const typeEmoji = p.type === 'android' ? '🤖' : p.type === 'node' ? '📦' : p.type === 'python' ? '🐍' : '📁';
      
      response += `${emoji} *${p.name}* ${typeEmoji}\n`;
      response += `\`${p.path}\`\n`;
      if (p.applicationId) response += `📱 ${p.applicationId}\n`;
      response += `${p.commands.length} commands | Last: ${new Date(p.lastActivity).toLocaleDateString()}\n\n`;
    }

    await interaction.reply({
      content: response,
      ephemeral: true
    });
    return;
  }

  if (type === 'commands') {
    const commands = memoryService.getRecentCommands(20);
    
    if (commands.length === 0) {
      await interaction.reply({
        content: '📭 No commands logged.',
        ephemeral: true
      });
      return;
    }

    let response = '📜 *Recent Commands*\n\n';
    for (let i = 0; i < Math.min(commands.length, 10); i++) {
      const c = commands[i];
      const status = c.exitCode === 0 ? '✅' : c.exitCode === null ? '⏳' : '❌';
      response += `${i + 1}. ${status} \`${c.command}\`\n`;
      response += `📁 ${c.cwd}\n`;
      response += `⏱️ ${c.duration}ms | ${new Date(c.timestamp).toLocaleTimeString()}\n\n`;
    }

    await interaction.reply({
      content: response,
      ephemeral: true
    });
    return;
  }

  const stats = memoryService.getStats();
  const projects = memoryService.getProjects();
  const commands = memoryService.getRecentCommands(5);

  await interaction.reply({
    content: `
🧠 *Bot Memory*

📊 *Stats:*
• Total Commands: ${stats.totalCommands}
• Total Projects: ${stats.totalProjects}
• Uptime: ${stats.uptime}

📁 *Projects:* ${projects.length}
📜 *Recent:* ${commands.length} commands

💡 /memory projects - View all projects
💡 /memory commands - View recent commands
    `.trim(),
    ephemeral: true
  });
}
