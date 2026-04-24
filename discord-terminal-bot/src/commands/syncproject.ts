import { SlashCommandBuilder, EmbedBuilder, Colors, ButtonBuilder, ButtonStyle, ActionRowBuilder } from 'discord.js';
import { terminalService, securityService, loggerService, memoryService, androidService } from '../services';
import { truncateOutput } from '../utils';

function extractSyncFailureTail(output: string): string {
  const normalized = (output || '').replace(/\r\n/g, '\n');
  const markers = [
    /^> Task .* FAILED$/m,
    /^FAILURE: Build failed.*$/m,
    /^BUILD FAILED.*$/m,
    /^FAILURE:.*$/m,
    /error:/i,
  ];

  let startIndex = -1;
  for (const marker of markers) {
    const match = marker.exec(normalized);
    if (match && typeof match.index === 'number') {
      if (startIndex < 0 || match.index < startIndex) {
        startIndex = match.index;
      }
    }
  }

  return startIndex >= 0 ? normalized.slice(startIndex).trim() : normalized.trim();
}

export const data = new SlashCommandBuilder()
  .setName('syncproject')
  .setDescription('Sync the current Android project with Gradle')
  .addBooleanOption(option =>
    option.setName('ephemeral')
      .setDescription('Show output only to you')
      .setRequired(false)
  );

export async function execute(interaction: any): Promise<void> {
  const ephemeral = interaction.options.getBoolean('ephemeral') ?? false;
  const channelId = interaction.channelId;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({ content: 'You are not authorized to use this command.', flags: 64 });
    return;
  }

  const session = terminalService.getOrCreateSession(channelId);
  if (!androidService.isAndroidProject(session.cwd)) {
    await interaction.reply({ content: `Current directory is not an Android project: \`${session.cwd}\``, flags: 64 });
    return;
  }

  const cooldown = securityService.checkCooldown(userId);
  if (!cooldown.allowed) {
    await interaction.reply({
      content: `Please wait ${Math.ceil((cooldown.remainingMs || 0) / 1000)} seconds before running another command.`,
      flags: 64,
    });
    return;
  }

  if (terminalService.isProcessRunning(channelId)) {
    await interaction.reply({
      content: 'A process is already running in this channel. Please stop it first.',
      flags: 64,
    });
    return;
  }

  const plan = androidService.buildSyncProjectCommand(session.cwd);
  await interaction.deferReply({ ephemeral });

  const row = new ActionRowBuilder<ButtonBuilder>().addComponents(
    new ButtonBuilder().setCustomId(`stop_${channelId}`).setLabel('Stop').setStyle(ButtonStyle.Danger),
    new ButtonBuilder().setCustomId(`refresh_${channelId}`).setLabel('Refresh').setStyle(ButtonStyle.Secondary),
  );

  await interaction.editReply({
    embeds: [
      new EmbedBuilder()
        .setTitle('Syncing Android Project')
        .setColor(Colors.Yellow)
        .addFields(
          { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
          { name: 'Runner', value: `\`${plan.runner}\``, inline: true },
          { name: 'Status', value: 'Running Gradle sync...', inline: true },
        )
        .setTimestamp(),
    ],
    components: [row],
  });

  const maxCapturedOutput = 12000;
  let capturedOutput = '';
  const startTime = Date.now();

  const appendOutput = (chunk: string): void => {
    if (!chunk) return;
    capturedOutput = (capturedOutput + chunk).slice(-maxCapturedOutput);
  };

  try {
    await terminalService.executeCommand(
      channelId,
      userId,
      plan.command,
      (data, type) => {
        appendOutput((type === 'stderr' ? 'ERR: ' : '') + data);
      },
      async (exitCode, signal) => {
        const duration = Date.now() - startTime;
        const combinedOutput = capturedOutput;
        const succeeded = exitCode === 0 && !signal;

        const conciseOutput = succeeded
          ? 'Success: Gradle sync completed.'
          : signal
            ? `Stopped by signal: ${signal}`
            : truncateOutput(extractSyncFailureTail(combinedOutput) || 'Failed with no detailed output.', 1500);

        securityService.logCommand({
          userId,
          channelId,
          command: plan.command,
          status: exitCode === 0 ? 'success' : 'failed',
          duration,
        });

        memoryService.trackCommand(
          plan.command,
          session.cwd,
          exitCode,
          duration,
          userId,
          channelId,
          conciseOutput
        );
        const resultEmbed = new EmbedBuilder()
          .setTitle(exitCode === 0 && !signal ? 'Android Project Synced' : signal ? 'Android Sync Stopped' : 'Android Sync Failed')
          .setColor(exitCode === 0 && !signal ? Colors.Green : signal ? Colors.Orange : Colors.Red)
          .addFields(
            { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
            { name: 'Runner', value: `\`${plan.runner}\``, inline: true },
            { name: 'Exit Code', value: exitCode?.toString() || signal || 'N/A', inline: true },
            { name: 'Duration', value: `${duration}ms`, inline: true },
          )
          .setTimestamp();

        resultEmbed.addFields({ name: 'Result', value: `\`\`\`\n${conciseOutput}\n\`\`\``, inline: false });

        await interaction.editReply({ embeds: [resultEmbed], components: [] });
      }
    );
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    loggerService.error('Failed to sync Android project', { channelId, userId, error: errorMessage });
    await interaction.editReply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Error')
          .setColor(Colors.Red)
          .addFields(
            { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
            { name: 'Error', value: errorMessage, inline: false },
          )
          .setTimestamp(),
      ],
      components: [],
    });
  }
}

export default { data, execute };
