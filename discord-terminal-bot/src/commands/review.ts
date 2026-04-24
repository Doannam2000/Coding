import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { reviewService, securityService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('review')
  .setDescription('Review a file or current git changes with AI')
  .addStringOption(option =>
    option.setName('file')
      .setDescription('Optional file path to review')
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

  const file = interaction.options.getString('file');
  const workdir = interaction.options.getString('path') || process.cwd();
  reviewService.setCwd(workdir);
  await interaction.deferReply({ flags: 64 });

  try {
    const result = file ? await reviewService.reviewFile(file) : await reviewService.reviewGitChanges();
    const issues = result.issues || [];
    const ordered = ['high', 'medium', 'low']
      .flatMap(level => issues.filter(issue => issue.severity === level));

    const embed = new EmbedBuilder()
      .setTitle(file ? `Review: ${file}` : 'Review: Git Changes')
      .setColor(ordered.length > 0 ? Colors.Orange : Colors.Green)
      .setDescription(result.summary || 'No summary')
      .addFields({
        name: 'Findings',
        value: ordered.length > 0
          ? ordered.slice(0, 12).map(issue => `[${issue.severity}] ${issue.type}: ${issue.message}${issue.line ? ` (line ${issue.line})` : ''}`).join('\n').slice(0, 1024)
          : 'No findings returned.',
        inline: false,
      })
      .addFields({
        name: 'Suggestions',
        value: result.suggestions?.length
          ? result.suggestions.slice(0, 8).map(item => `• ${item}`).join('\n').slice(0, 1024)
          : 'No suggestions.',
        inline: false,
      })
      .setFooter({ text: `Score: ${result.score}/10 | Path: ${workdir}` })
      .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
  } catch (error: any) {
    await interaction.editReply({ content: `❌ Review failed: ${error.message}` });
  }
}

export default { data, execute };
