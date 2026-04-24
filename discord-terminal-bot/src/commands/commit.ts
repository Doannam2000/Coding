import { SlashCommandBuilder } from 'discord.js';
import { gitService, securityService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('commit')
  .setDescription('Create a git commit for current changes')
  .addStringOption(option =>
    option.setName('message')
      .setDescription('Commit message; omitted = auto message')
      .setRequired(false)
  )
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

  const message = interaction.options.getString('message') || undefined;
  const workdir = interaction.options.getString('path') || process.cwd();
  gitService.setCwd(workdir);
  await interaction.deferReply({ flags: 64 });

  try {
    const output = message ? (await gitService.add(), await gitService.commit(message)) : await gitService.autoCommit();
    await interaction.editReply({ content: `✅ Commit finished in \`${workdir}\`\n\`\`\`\n${output.slice(-1500)}\n\`\`\`` });
  } catch (error: any) {
    await interaction.editReply({ content: `❌ Commit failed: ${error.message}` });
  }
}

export default { data, execute };
