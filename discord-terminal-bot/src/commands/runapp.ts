import { SlashCommandBuilder, EmbedBuilder, Colors, ButtonBuilder, ButtonStyle, ActionRowBuilder } from 'discord.js';
import { terminalService, securityService, loggerService, memoryService, androidService } from '../services';
import { truncateOutput } from '../utils';

export const data = new SlashCommandBuilder()
  .setName('runapp')
  .setDescription('Build and launch the current Android project in debug mode')
  .addStringOption(option =>
    option.setName('device')
      .setDescription('Optional adb device serial to target')
      .setRequired(false)
  )
  .addBooleanOption(option =>
    option.setName('ephemeral')
      .setDescription('Show output only to you')
      .setRequired(false)
  );

function extractRunAppFailureTail(output: string): string {
  const normalized = (output || '').replace(/\r\n/g, '\n');
  const markers = [
    /^> Task .* FAILED$/m,
    /^FAILURE: Build failed.*$/m,
    /^BUILD FAILED.*$/m,
    /^FAILURE:.*$/m,
    /INSTALL_FAILED/m,
    /adb: failed/i,
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

export async function execute(interaction: any): Promise<void> {
  const ephemeral = interaction.options.getBoolean('ephemeral') ?? false;
  const channelId = interaction.channelId;
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: 'You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const session = terminalService.getOrCreateSession(channelId);

  if (!androidService.isAndroidProject(session.cwd)) {
    await interaction.reply({
      content: `Current directory is not an Android project: \`${session.cwd}\``,
      flags: 64,
    });
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

  const requestedDeviceId = interaction.options.getString('device') || undefined;
  const deviceId = requestedDeviceId || terminalService.getSelectedDevice(channelId);
  const plan = androidService.buildRunAppCommand(session.cwd, deviceId);

  await interaction.deferReply({ ephemeral });

  const stopButton = new ButtonBuilder()
    .setCustomId(`stop_${channelId}`)
    .setLabel('Stop')
    .setStyle(ButtonStyle.Danger);

  const refreshButton = new ButtonBuilder()
    .setCustomId(`refresh_${channelId}`)
    .setLabel('Refresh')
    .setStyle(ButtonStyle.Secondary);

  const row = new ActionRowBuilder<ButtonBuilder>()
    .addComponents(stopButton, refreshButton);

  const statusEmbed = new EmbedBuilder()
    .setTitle('Running Android App')
    .setColor(Colors.Yellow)
    .addFields(
      { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
      { name: 'Runner', value: `\`${plan.runner}\``, inline: true },
      { name: 'Device', value: plan.deviceId ? `\`${plan.deviceId}\`` : 'Auto-detect', inline: true },
      { name: 'Application ID', value: plan.applicationId ? `\`${plan.applicationId}\`` : 'Not detected', inline: true },
      { name: 'Status', value: plan.requiresPhysicalDevice ? 'Checking connected physical device, installing, and verifying app launch...' : 'Installing debug build...', inline: true }
    )
    .setTimestamp();

  await interaction.editReply({
    embeds: [statusEmbed],
    components: [row],
  });

  const maxCapturedOutput = 12000;
  let capturedOutput = '';
  const startTime = Date.now();
  let earlyErrorReported = false;

  const appendOutput = (chunk: string): void => {
    if (!chunk) return;
    capturedOutput = (capturedOutput + chunk).slice(-maxCapturedOutput);
  };

  const shouldReportEarlyError = (chunk: string, type: 'stdout' | 'stderr'): boolean => {
    if (type === 'stderr') return true;
    return /FAILURE:|BUILD FAILED|error:|INSTALL_FAILED|adb: failed/i.test(chunk);
  };

  const reportEarlyError = async (chunk: string): Promise<void> => {
    if (earlyErrorReported) return;
    earlyErrorReported = true;

    const errorEmbed = new EmbedBuilder()
      .setTitle('Android Run Error Detected')
      .setColor(Colors.Red)
      .addFields(
        { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
        { name: 'Runner', value: `\`${plan.runner}\``, inline: true },
        { name: 'Device', value: plan.deviceId ? `\`${plan.deviceId}\`` : 'Auto-detect', inline: true },
        { name: 'Status', value: 'Error detected. Waiting for process to finish...', inline: true },
        { name: 'Error', value: `\`\`\`\n${truncateOutput(extractRunAppFailureTail(chunk), 1500) || 'Unknown error'}\n\`\`\``, inline: false }
      )
      .setTimestamp();

    if (plan.applicationId) {
      errorEmbed.addFields({ name: 'Application ID', value: `\`${plan.applicationId}\``, inline: true });
    }

    await interaction.editReply({
      embeds: [errorEmbed],
      components: [row],
    });
  };

  try {
    await terminalService.executeCommand(
      channelId,
      userId,
      plan.command,
      (data, type) => {
        const prefix = type === 'stderr' ? 'ERR: ' : '';
        appendOutput(prefix + data);
        if (shouldReportEarlyError(data, type)) {
          void reportEarlyError(data);
        }
      },
      async (exitCode, signal) => {
        const duration = Date.now() - startTime;

        securityService.logCommand({
          userId,
          channelId,
          command: plan.command,
          status: exitCode === 0 ? 'success' : 'failed',
          duration,
        });

        const combinedOutput = capturedOutput;
        const succeeded = exitCode === 0 && !signal;
        const resultTitle = succeeded
          ? (plan.launchEnabled ? 'Android App Launched on Device' : 'Android Debug Build Installed')
          : signal
            ? 'Android Run Stopped'
            : 'Android Run Failed';

        const outputForSummary = exitCode !== 0 && !signal
          ? extractRunAppFailureTail(combinedOutput)
          : combinedOutput;

        const conciseOutput = succeeded
          ? 'Success: build/install completed.'
          : signal
            ? `Stopped by signal: ${signal}`
            : truncateOutput(outputForSummary || 'Failed with no detailed output.', 1500);

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
          .setTitle(resultTitle)
          .setColor(succeeded ? Colors.Green : signal ? Colors.Orange : Colors.Red)
          .addFields(
            { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
            { name: 'Runner', value: `\`${plan.runner}\``, inline: true },
            { name: 'Device', value: plan.deviceId ? `\`${plan.deviceId}\`` : 'Auto-detect', inline: true },
            { name: 'Exit Code', value: exitCode?.toString() || signal || 'N/A', inline: true },
            { name: 'Duration', value: `${duration}ms`, inline: true }
          )
          .setTimestamp();

        if (plan.applicationId) {
          resultEmbed.addFields({ name: 'Application ID', value: `\`${plan.applicationId}\``, inline: false });
        }

        resultEmbed.addFields({ name: 'Result', value: `\`\`\`\n${conciseOutput}\n\`\`\``, inline: false });

        await interaction.editReply({
          embeds: [resultEmbed],
          components: [],
        });
      }
    );
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';

    loggerService.error('Failed to run Android app', {
      channelId,
      userId,
      error: errorMessage,
    });

    const errorEmbed = new EmbedBuilder()
      .setTitle('Error')
      .setColor(Colors.Red)
      .addFields(
        { name: 'Directory', value: `\`${session.cwd}\``, inline: false },
        { name: 'Error', value: errorMessage, inline: false }
      )
      .setTimestamp();

    await interaction.editReply({
      embeds: [errorEmbed],
      components: [],
    });

    securityService.logCommand({
      userId,
      channelId,
      command: plan.command,
      status: 'failed',
      output: errorMessage,
    });
  }
}

export default { data, execute };
