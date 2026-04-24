import { SlashCommandBuilder, EmbedBuilder, Colors, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { aiService, memoryService, securityService, terminalService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('model')
  .setDescription('View or change the default AI model for the active CLI')
  .addStringOption(option =>
    option.setName('name')
      .setDescription('Model id for the active CLI')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;
  const name = interaction.options.getString('name');
  const currentCLI = aiService.getCLI();
  const cliLabel = aiService.getCliDisplayName(currentCLI);
  const examples = currentCLI === 'opencode'
    ? aiService.getModelExamples(currentCLI)
    : await aiService.listModelsForCLI(currentCLI).catch(() => aiService.getModelExamples(currentCLI));

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  if (!name) {
    const rows: ActionRowBuilder<ButtonBuilder>[] = [];
    for (let i = 0; i < examples.length; i += 5) {
      rows.push(
        new ActionRowBuilder<ButtonBuilder>().addComponents(
          examples.slice(i, i + 5).map(example =>
            new ButtonBuilder()
              .setCustomId(`quickmodel_${example}`)
              .setLabel(example.slice(0, 80))
              .setStyle(example === aiService.getDefaultModel() ? ButtonStyle.Success : ButtonStyle.Secondary)
          )
        )
      );
    }

    const embed = new EmbedBuilder()
      .setTitle('AI Model')
      .setColor(Colors.Blue)
      .setDescription(`Current CLI: \`${currentCLI}\`\nCurrent default model: \`${aiService.getDefaultModel()}\``)
      .addFields({
        name: currentCLI === 'opencode' ? 'Examples' : 'Suggested Models',
        value: examples.slice(0, 20).map(example => `\`${example}\``).join('\n'),
      })
      .setFooter({
        text: currentCLI === 'opencode'
          ? 'Use /models to browse OpenCode models.'
          : currentCLI === 'codex'
            ? 'Showing models from OpenAI Models API when available. You can still enter any compatible model id manually.'
            : `${cliLabel} CLI does not expose a full model list here. You can still enter any compatible model id manually.`,
      });

    await interaction.reply({ embeds: [embed], components: rows, flags: 64 });
    return;
  }

  const validation = await aiService.validateModelSelectionForCurrentCLI(name);
  if (!validation.ok) {
    await interaction.reply({
      content: `❌ ${validation.error}`,
      flags: 64,
    });
    return;
  }

  const selectedModel = validation.normalized || name;
  aiService.setDefaultModel(selectedModel, currentCLI);
  memoryService.setDefaultModel(selectedModel, currentCLI);
  
  if (currentCLI === 'opencode') {
    aiService.resetSession();
  }
  
  const currentCwd = terminalService.getSession(interaction.channelId)?.cwd;
  if (currentCwd) {
    memoryService.setProjectAISettingsByCwd(currentCwd, {
      cli: currentCLI,
      model: selectedModel,
    });
  }

  await interaction.reply({
    content: `✅ Default model set to \`${selectedModel}\`${validation.variant ? ` with variant \`${validation.variant}\`` : ''}`,
    flags: 64,
  });
}

export default { data, execute };
