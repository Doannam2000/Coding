import { SlashCommandBuilder, EmbedBuilder, Colors, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { aiService, memoryService, securityService, terminalService } from '../services';
import { SupportedCLI } from '../services/AIService';

const SUPPORTED_CLIS: { value: SupportedCLI; label: string; desc: string }[] = [
  { value: 'opencode', label: 'opencode', desc: 'OpenCode CLI (`opencode run --format json`)' },
  { value: 'claude', label: 'claude', desc: 'Claude Code CLI (`claude --print`)' },
  { value: 'codex', label: 'codex', desc: 'OpenAI Codex CLI (`codex exec --json`)' },
];

export const data = new SlashCommandBuilder()
  .setName('cli')
  .setDescription('View or switch the active AI CLI backend')
  .addStringOption(option =>
    option.setName('name')
      .setDescription('CLI to activate: opencode, claude, codex')
      .setRequired(false)
      .addChoices(
        { name: 'opencode', value: 'opencode' },
        { name: 'claude', value: 'claude' },
        { name: 'codex', value: 'codex' },
      )
  );

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;
  const name = interaction.options.getString('name') as SupportedCLI | null;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({ content: '❌ You are not authorized to use this command.', flags: 64 });
    return;
  }

  if (!name) {
    const current = aiService.getCLI();
    const embed = new EmbedBuilder()
      .setTitle('AI CLI Backend')
      .setColor(Colors.Blue)
      .setDescription(`Current CLI: \`${current}\`\nCurrent model: \`${aiService.getDefaultModel()}\``)
      .addFields({
        name: 'Available CLIs',
        value: SUPPORTED_CLIS.map(c =>
          `\`${c.value}\`${c.value === current ? ' ✅' : ''} — ${c.desc}`
        ).join('\n'),
      })
      .setFooter({ text: 'Click a button to switch CLI' });

    const row = new ActionRowBuilder<ButtonBuilder>().addComponents(
      SUPPORTED_CLIS.map(c =>
        new ButtonBuilder()
          .setCustomId(`clibtn_${c.value}`)
          .setLabel(c.value)
          .setStyle(c.value === current ? ButtonStyle.Success : ButtonStyle.Secondary)
      )
    );

    await interaction.reply({ embeds: [embed], components: [row], flags: 64 });
    return;
  }

  const valid = SUPPORTED_CLIS.map(c => c.value);
  if (!valid.includes(name)) {
    await interaction.reply({
      content: `❌ Unknown CLI: \`${name}\`. Choose from: ${valid.join(', ')}`,
      flags: 64,
    });
    return;
  }

  aiService.setCLI(name);
  const currentCwd = terminalService.getSession(interaction.channelId)?.cwd;
  if (currentCwd) {
    memoryService.setProjectAISettingsByCwd(currentCwd, {
      cli: name,
      model: aiService.getDefaultModel(),
    });
  }

  const info = SUPPORTED_CLIS.find(c => c.value === name)!;
  await interaction.reply({
    content: `✅ AI CLI switched to \`${name}\` — ${info.desc}`,
    flags: 64,
  });
}

export default { data, execute };
