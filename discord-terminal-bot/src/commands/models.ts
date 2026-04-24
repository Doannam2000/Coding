import {
  SlashCommandBuilder,
  EmbedBuilder,
  Colors,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
} from 'discord.js';
import type { ModelMetadata } from '../services/AIService';
import { aiService, securityService } from '../services';

function truncateLabel(value: string, max: number = 80): string {
  return value.length > max ? value.slice(0, max - 3) + '...' : value;
}

function getPageModels<T>(models: T[], page: number, pageSize: number = 10): T[] {
  const start = page * pageSize;
  return models.slice(start, start + pageSize);
}

function buildModelButtons(models: ModelMetadata[], currentModel: string, provider?: string, page: number = 0) {
  const pageModels = getPageModels(models, page);
  const rows: ActionRowBuilder<ButtonBuilder>[] = [];

  for (let i = 0; i < pageModels.length; i += 5) {
    const row = new ActionRowBuilder<ButtonBuilder>();
    for (const model of pageModels.slice(i, i + 5)) {
      row.addComponents(
        new ButtonBuilder()
          .setCustomId(`modelbtn_${model.key}`)
          .setLabel(truncateLabel(`${model.key}${model.variants.length ? ' +' : ''}`))
          .setStyle(model.key === currentModel ? ButtonStyle.Success : ButtonStyle.Secondary)
          .setDisabled(!!model.status && model.status !== 'active')
      );
    }
    rows.push(row);
  }

  const hasPrev = page > 0;
  const hasNext = (page + 1) * 10 < models.length;
  const navRow = new ActionRowBuilder<ButtonBuilder>().addComponents(
    new ButtonBuilder()
      .setCustomId(`modelpage_${provider || 'all'}_${page - 1}`)
      .setLabel('Prev')
      .setStyle(ButtonStyle.Primary)
      .setDisabled(!hasPrev),
    new ButtonBuilder()
      .setCustomId(`modelpage_${provider || 'all'}_${page + 1}`)
      .setLabel('Next')
      .setStyle(ButtonStyle.Primary)
      .setDisabled(!hasNext)
  );
  rows.push(navRow);

  return rows;
}

function buildQuickModelButtons(cli: string, models: string[], currentModel: string, page: number = 0) {
  const pageModels = getPageModels(models, page);
  const rows: ActionRowBuilder<ButtonBuilder>[] = [];

  for (let i = 0; i < pageModels.length; i += 5) {
    rows.push(
      new ActionRowBuilder<ButtonBuilder>().addComponents(
        pageModels.slice(i, i + 5).map(model =>
          new ButtonBuilder()
            .setCustomId(`quickmodel_${model}`)
            .setLabel(truncateLabel(model))
            .setStyle(model === currentModel ? ButtonStyle.Success : ButtonStyle.Secondary)
        )
      )
    );
  }

  if (models.length > 10) {
    rows.push(
      new ActionRowBuilder<ButtonBuilder>().addComponents(
        new ButtonBuilder()
          .setCustomId(`quickmodelpage_${cli}_${page - 1}`)
          .setLabel('Prev')
          .setStyle(ButtonStyle.Primary)
          .setDisabled(page <= 0),
        new ButtonBuilder()
          .setCustomId(`quickmodelpage_${cli}_${page + 1}`)
          .setLabel('Next')
          .setStyle(ButtonStyle.Primary)
          .setDisabled((page + 1) * 10 >= models.length)
      )
    );
  }

  return rows;
}

export const data = new SlashCommandBuilder()
  .setName('models')
  .setDescription('Browse models for the active CLI')
  .addStringOption(option =>
    option.setName('provider')
      .setDescription('Optional filter keyword or provider, e.g. openai or gpt-5')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;
  const provider = interaction.options.getString('provider') || undefined;
  const currentCLI = aiService.getCLI();

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  if (currentCLI !== 'opencode') {
    let examples = await aiService.listModelsForCLI(currentCLI).catch(() => aiService.getSupportedModels(currentCLI));
    if (provider) {
      const filter = provider.toLowerCase();
      examples = examples.filter(example => example.toLowerCase().includes(filter));
    }
    const page = 0;
    const rows = buildQuickModelButtons(currentCLI, examples, aiService.getDefaultModel(), page);

    const embed = new EmbedBuilder()
      .setTitle(`${aiService.getCliDisplayName(currentCLI)} Models`)
      .setColor(Colors.Blue)
      .setDescription(
        currentCLI === 'codex'
          ? `Current CLI: \`${currentCLI}\`\nCurrent default: \`${aiService.getDefaultModel()}\`\nLoaded from OpenAI Models API when available.${provider ? `\nFilter: \`${provider}\`` : ''}`
          : `Current CLI: \`${currentCLI}\`\nCurrent default: \`${aiService.getDefaultModel()}\`\nThe ${aiService.getCliDisplayName(currentCLI)} CLI does not expose a full model list here, so these are suggested quick picks.${provider ? `\nFilter: \`${provider}\`` : ''}`
      )
      .addFields({
        name: currentCLI === 'codex' ? 'Available Models' : 'Suggested Quick Picks',
        value: getPageModels(examples, page).map(example => `\`${example}\``).join('\n'),
      });

    await interaction.reply({
      embeds: [embed],
      components: rows,
      flags: 64,
    });
    return;
  }

  await interaction.deferReply({ flags: 64 });

  try {
    const models = await aiService.listAvailableModelsDetailed(provider);
    const current = aiService.getDefaultModel();
    const page = 0;
    const shown = getPageModels(models, page);

    const embed = new EmbedBuilder()
      .setTitle('Available Models')
      .setColor(Colors.Blue)
      .setDescription(`Current default: \`${current}\`\nFound: ${models.length} model(s)`)
      .addFields({
        name: `Models Page ${page + 1}`,
        value: shown.map(model => {
          const badges = [`${model.key === current ? 'current' : ''}`, `${model.paidPriority > 1 ? 'paid' : 'free'}`, `${model.status || 'unknown'}`, model.variants.length ? `variants: ${model.variants.join(', ')}` : '']
            .filter(Boolean)
            .join(' | ');
          return `• ${model.key}${badges ? ` (${badges})` : ''}`;
        }).join('\n').slice(0, 1024),
      })
      .setTimestamp();

    const components = models.length > 0 ? buildModelButtons(models, current, provider, page) : [];

    await interaction.editReply({ embeds: [embed], components });
  } catch (error: any) {
    await interaction.editReply({
      content: `❌ Failed to list models: ${error.message}`,
    });
  }
}

export default { data, execute };
