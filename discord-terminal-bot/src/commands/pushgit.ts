import { SlashCommandBuilder } from 'discord.js';
import { gitService, securityService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('pushgit')
  .setDescription('Auto commit with timestamp + summary and push to remote')
  .addStringOption(option =>
    option.setName('path')
      .setDescription('Working directory')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  if (!securityService.isOwner(interaction.user.id)) {
    await interaction.reply({ content: '❌ You are not authorized to use this command.', flags: 64 });
    return;
  }

  const workdir = interaction.options.getString('path') || process.cwd();
  gitService.setCwd(workdir);
  await interaction.deferReply({ flags: 64 });

  try {
    const output = await gitService.pushGitAuto();
    await interaction.editReply({ content: `✅ PushGit finished in \`${workdir}\`\n\`\`\`\n${output.slice(-1500)}\n\`\`\`` });
  } catch (error: any) {
    await interaction.editReply({ content: `❌ PushGit failed: ${error.message}` });
  }
}

export default { data, execute };
