import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { securityService } from '../services';
import { getOS } from '../utils';

export const data = new SlashCommandBuilder()
  .setName('help')
  .setDescription('Show help information for all commands');

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: 'You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const os = getOS();
  const shellInfo = os === 'windows' ? 'PowerShell' : 'Bash';

  const embed = new EmbedBuilder()
    .setTitle('Discord Terminal Bot - Help')
    .setColor(Colors.Blue)
    .setDescription('Remote terminal access via Discord')
    .addFields(
      {
        name: 'Available Commands',
        value: `
**\`/run <command> [timeout]\`** - Execute a terminal command (timeout in minutes)
- Example: \`/run npm install\` (default timeout from config)
- Example: \`/run npm install 15\` or \`/run npm install -t 15\` (15 minutes)
**\`/devices\`** - List adb devices and choose the active Android device for this channel
**\`/runapp\`** - Build and launch the current Android project in debug mode
**\`/syncproject\`** - Sync the current Android project with Gradle
**\`/status [type]\`** - Check status (session/process/system)
**\`/stop\`** - Stop the running process
**\`/cd <directory>\`** - Change working directory
**\`/history [lines] [--clear]\`** - View/clear command history
**\`/logs [lines] [--file]\`** - View command logs
**\`/sessions\`** - List all terminal sessions
**\`/sessionclear [all]\`** - Clear the current terminal session or all sessions
**\`/upload <file> [path]\`** - Upload file to server
**\`/cli [name]\`** - View or switch AI CLI
**\`/model [name]\`** - View or change default AI model
**\`/current\`** - Show current project, CLI, model, and token status
**\`/models [provider]\`** - Browse models for active CLI
**\`/usage [days] [cli]\`** - Show tracked AI usage from this bot
**\`/changes [path]\`** - Show git working tree changes
**\`/review [file] [path]\`** - Review a file or current git changes
**\`/test [pattern] [coverage] [path]\`** - Run project tests
**\`/commit [message] [path]\`** - Create a git commit
**\`/pushgit [path]\`** - Auto commit with datetime + summary and push
**\`/health\`** - Check bot and AI backend health
**\`/ping\`** - Check bot latency
**\`/help\`** - Show this help message
        `.trim(),
        inline: false,
      },
      {
        name: 'Security',
        value: `
- Only owner(s) can use commands
- Dangerous commands are blocked
- Command cooldown enforced
- All commands are logged
        `.trim(),
        inline: false,
      },
      {
        name: 'Tips',
        value: `
- Use \`/run npm install\` to install packages
- Use \`/run ls\` or \`/run dir\` to list files
- Use \`/devices\` to pick the Android device before \`/runapp\`
- Use \`/runapp\` inside an Android project to install and launch the debug app
- Use \`/syncproject\` inside an Android project to sync Gradle/project config
- Use \`/cd <folder>\` to change directories
- Use \`/history\` to see past commands
- Click **Stop** to terminate running processes
        `.trim(),
        inline: false,
      },
      {
        name: 'System Info',
        value: `
- Shell: ${shellInfo}
- OS: ${os.charAt(0).toUpperCase() + os.slice(1)}
- Process Timeout: 10 minutes
- Max Output: 4000 characters
        `.trim(),
        inline: false,
      }
    )
    .setTimestamp();

  await interaction.reply({
    embeds: [embed],
    flags: 64,
  });
}

export default { data, execute };
