import { SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { securityService, testService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('test')
  .setDescription('Run detected project tests')
  .addStringOption(option =>
    option.setName('pattern')
      .setDescription('Optional test pattern')
      .setRequired(false)
  )
  .addBooleanOption(option =>
    option.setName('coverage')
      .setDescription('Run coverage instead of regular tests')
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

  const pattern = interaction.options.getString('pattern') || undefined;
  const coverage = interaction.options.getBoolean('coverage') ?? false;
  const workdir = interaction.options.getString('path') || process.cwd();

  testService.setCwd(workdir);
  await interaction.deferReply({ flags: 64 });

  const result = coverage ? await testService.runCoverage() : await testService.runTests(pattern);
  const framework = testService.detectFramework();

  const embed = new EmbedBuilder()
    .setTitle(coverage ? 'Test Coverage' : 'Test Run')
    .setColor(result.success ? Colors.Green : Colors.Red)
    .setDescription(`Framework: \`${framework}\`\nPath: \`${workdir}\``)
    .addFields(
      { name: 'Result', value: result.success ? 'Success' : 'Failed', inline: true },
      { name: 'Duration', value: `${result.duration}ms`, inline: true },
      { name: 'Totals', value: `passed=${result.passed} failed=${result.failed} total=${result.total}`, inline: true },
    )
    .setTimestamp();

  if (result.coverage) {
    embed.addFields({
      name: 'Coverage',
      value: `lines=${result.coverage.lines}% statements=${result.coverage.statements}%`,
      inline: false,
    });
  }

  if (result.output) {
    embed.addFields({
      name: 'Output',
      value: `\`\`\`\n${result.output.slice(-900)}\n\`\`\``,
      inline: false,
    });
  }

  await interaction.editReply({ embeds: [embed] });
}

export default { data, execute };
