import { Client, GatewayIntentBits, Collection } from 'discord.js';
import config, { token, clientId, guildId } from './config';
import { loggerService, securityService, terminalService, aiService, memoryService, permissionBrokerService } from './services';
import path from 'path';
import fs from 'fs';

const BOT_VERSION = '1.0.2';
let eventLoopWatchdogTimer: NodeJS.Timeout | null = null;

function startEventLoopWatchdog(): void {
  const checkIntervalMs = Math.max(1000, config.eventLoopWatchdogIntervalMs || 10000);
  const warnThresholdMs = Math.max(100, config.eventLoopLagWarnMs || 1500);
  let expectedAt = Date.now() + checkIntervalMs;

  eventLoopWatchdogTimer = setInterval(() => {
    const now = Date.now();
    const lagMs = Math.max(0, now - expectedAt);
    expectedAt = now + checkIntervalMs;

    if (lagMs < warnThresholdMs) return;

    loggerService.warn('Event loop lag detected', {
      lagMs,
      thresholdMs: warnThresholdMs,
      memoryMb: Math.round(process.memoryUsage().rss / 1024 / 1024),
    });
  }, checkIntervalMs);

  eventLoopWatchdogTimer.unref();
}

function stopEventLoopWatchdog(): void {
  if (!eventLoopWatchdogTimer) return;
  clearInterval(eventLoopWatchdogTimer);
  eventLoopWatchdogTimer = null;
}

interface ExtendedClient extends Client {
  commands: Collection<any, any>;
  runningProcesses: Map<string, string>;
}

const client: ExtendedClient = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent,
    GatewayIntentBits.DirectMessages,
  ],
}) as ExtendedClient;

client.commands = new Collection();
client.runningProcesses = new Map();

loggerService.initialize();
loggerService.info(`Starting Discord Terminal Bot v${BOT_VERSION}...`, { startTime: new Date().toISOString() });
aiService.setDefaultModels(memoryService.getDefaultModels());

const commandsPath = path.join(__dirname, 'commands');
const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js') && !file.endsWith('.d.js'));

let loadedCount = 0;
for (const file of commandFiles) {
  try {
    const command = require(path.join(commandsPath, file));
    if (command.data && command.execute) {
      client.commands.set(command.data.name, command);
      loadedCount++;
      loggerService.info(`Loaded command: ${command.data.name}`);
    } else {
      loggerService.warn(`Command file ${file} missing data or execute function`);
    }
  } catch (error) {
    loggerService.error(`Failed to load command ${file}`, { 
      error: error instanceof Error ? error.message : String(error) 
    });
  }
}

if (loadedCount === 0) {
  loggerService.warn('No commands loaded');
} else {
  loggerService.info(`Loaded ${loadedCount} command(s)`);
}

import { Events } from 'discord.js';
import { REST, Routes } from 'discord.js';

const rest = new REST({ version: '10' }).setToken(token);

client.on(Events.ClientReady, async (c) => {
  loggerService.info(`Logged in as ${c.user?.tag}`);
  loggerService.info(`Bot is ready in ${c.guilds.cache.size} guild(s)`);

  try {
    const commands = client.commands.map(cmd => cmd.data.toJSON());
    if (commands.length === 0) {
      loggerService.warn('No commands registered');
    }
    
    if (guildId && clientId) {
      await rest.put(Routes.applicationGuildCommands(clientId, guildId), { body: commands });
      loggerService.info(`Registered ${commands.length} guild commands`);
    } else if (clientId) {
      await rest.put(Routes.applicationCommands(clientId), { body: commands });
      loggerService.info(`Registered ${commands.length} global commands`);
    } else {
      loggerService.warn('No clientId configured, skipping command registration');
    }
  } catch (error) {
    loggerService.error('Failed to register commands', { 
      error: error instanceof Error ? error.message : String(error) 
    });
  }
});

client.on(Events.InteractionCreate, async (interaction: any) => {
  if (!interaction.isChatInputCommand() && !interaction.isButton() && !interaction.isStringSelectMenu()) return;

  if (interaction.isChatInputCommand()) {
    const command = client.commands.get(interaction.commandName);
    if (!command) {
      loggerService.warn('Command not found', { commandName: interaction.commandName });
      return;
    }

    try {
      if (command.ownerOnly && !securityService.isOwner(interaction.user.id)) {
        if (!interaction.replied) {
          await interaction.reply({
            content: '❌ This command is restricted to bot owners only.',
            flags: 64,
          });
        }
        return;
      }

      await command.execute(interaction);
    } catch (error) {
      loggerService.error(`Error executing command ${interaction.commandName}`, { 
        error: error instanceof Error ? error.message : String(error), 
        user: interaction.user.id,
        stack: error instanceof Error ? error.stack : undefined
      });

      const errorMessage = error instanceof Error ? error.message : 'Unknown error';

      try {
        if (interaction.deferred && !interaction.replied) {
          await interaction.editReply({
            content: `❌ Error: ${errorMessage}`,
          });
        } else if (!interaction.replied) {
          await interaction.reply({
            content: `❌ Error: ${errorMessage}`,
            flags: 64,
          });
        }
      } catch (replyError) {
        loggerService.error('Failed to send error reply', { error: String(replyError) });
      }
    }
  }

  if (interaction.isButton()) {
    try {
      const customId = interaction.customId;
      if (!customId) {
        loggerService.warn('Button interaction missing customId');
        return;
      }

      if (customId.startsWith('discordwriteapprove_')) {
        const requestId = customId.slice('discordwriteapprove_'.length);
        try {
          const request = permissionBrokerService.approveRequest(requestId);
          const project = memoryService.getProjectByPath(request.rootPath);
          if (project && request.chatId === interaction.channelId) {
            terminalService.getOrCreateSession(interaction.channelId);
            terminalService.changeDirectory(interaction.channelId, project.path);
            if (project.preferredCLI) {
              aiService.setCLI(project.preferredCLI);
            }
            if (project.preferredModel) {
              aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
            }
            memoryService.setProjectAISettings(project.path, {
              cli: aiService.getCLI(),
              model: aiService.getDefaultModel(),
            });
          }
          await interaction.update({
            content: project && request.chatId === interaction.channelId
              ? `Write access approved and project switched to \`${project.name}\`.\nPath: \`${project.path}\`\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``
              : `Write access approved for \`${request.rootPath}\`.`,
            components: [],
          });
        } catch (error: any) {
          await interaction.update({
            content: `Failed to grant write access: ${error instanceof Error ? error.message : String(error)}`,
            components: [],
          });
        }
        return;
      }

      if (customId.startsWith('discordwritedeny_')) {
        const requestId = customId.slice('discordwritedeny_'.length);
        const request = permissionBrokerService.denyRequest(requestId);
        await interaction.update({
          content: request
            ? `Write access denied for \`${request.rootPath}\`.`
            : 'Write request not found.',
          components: [],
        });
        return;
      }

      if (customId.startsWith('sessionclearcurrent_')) {
        const targetChannelId = customId.slice('sessionclearcurrent_'.length);
        const existing = terminalService.getSession(targetChannelId);
        if (!existing) {
          await interaction.reply({ content: 'Session not found.', flags: 64 });
          return;
        }
        terminalService.destroySession(targetChannelId);
        await interaction.reply({ content: `Cleared session \`${targetChannelId}\`.`, flags: 64 });
        return;
      }

      if (customId === 'sessionclearall_global') {
        const cleared = terminalService.clearAllSessions();
        await interaction.reply({ content: `Cleared ${cleared} terminal session${cleared === 1 ? '' : 's'}.`, flags: 64 });
        return;
      }

      if (customId.startsWith('projectpick_')) {
        const targetPath = customId.slice('projectpick_'.length);
        const project = memoryService.getProjectByPath(targetPath);
        if (!project) {
          await interaction.reply({ content: 'Project not found.', flags: 64 });
          return;
        }

        if (permissionBrokerService.requiresApproval(project.path)) {
          const request = permissionBrokerService.createRequest(project.path, interaction.user.id, interaction.channelId);
          const { ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
          await interaction.update({
            content: `Project selected: \`${project.name}\`.\nWrite access is required for \`${request.rootPath}\` before switching. Approve access and the bot will switch to this project automatically.`,
            components: [
              new ActionRowBuilder().addComponents(
                new ButtonBuilder()
                  .setCustomId(`discordwriteapprove_${request.id}`)
                  .setLabel('Approve write')
                  .setStyle(ButtonStyle.Success),
                new ButtonBuilder()
                  .setCustomId(`discordwritedeny_${request.id}`)
                  .setLabel('Deny')
                  .setStyle(ButtonStyle.Secondary)
              ),
            ],
            embeds: [],
          });
          return;
        }

        terminalService.getOrCreateSession(interaction.channelId);
        terminalService.changeDirectory(interaction.channelId, project.path);
        if (project.preferredCLI) {
          aiService.setCLI(project.preferredCLI);
        }
        if (project.preferredModel) {
          aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
        }
        memoryService.setProjectAISettings(project.path, {
          cli: aiService.getCLI(),
          model: aiService.getDefaultModel(),
        });

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle(`Project: ${project.name}`)
              .setColor(require('discord.js').Colors.Green)
              .setDescription(`Path: \`${project.path}\`\nCWD updated for this channel.\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (customId.startsWith('stop_')) {
        const targetChannelId = customId.slice('stop_'.length);
        const processInfo = terminalService.getActiveProcess(targetChannelId);
        if (processInfo) {
          terminalService.killProcess(targetChannelId);
          await interaction.reply({
            content: 'Process stopped.',
            flags: 64,
          });
          client.runningProcesses.delete(targetChannelId);
        } else {
          await interaction.reply({
            content: 'No running process to stop.',
            flags: 64,
          });
        }
        return;
      }

      if (customId.startsWith('refresh_')) {
        const targetChannelId = customId.slice('refresh_'.length);
        const session = terminalService.getSession(targetChannelId);
        if (session) {
          const embed = loggerService.createSessionEmbed(session);
          await interaction.reply({
            embeds: [embed],
            flags: 64,
          });
        } else {
          await interaction.reply({
            content: 'No active session.',
            flags: 64,
          });
        }
        return;
      }

      const parts = customId.split('_' );
      if (parts.length < 2) {
        loggerService.warn('Invalid button customId format', { customId });
        return;
      }

      const action = parts[0];
      const subAction = parts[1];
      const channelId = parts.slice(2).join('_');

      if (!channelId && action !== 'help') {
        loggerService.warn('Button interaction missing channelId', { customId });
        return;
      }

      if (action === 'ai' && subAction === 'stop' && channelId) {
        try {
          const { stopAIActivity } = require('./commands/ai');
          const sessionKey = `${channelId}_${interaction.user.id}`;
          const stopped = stopAIActivity(sessionKey);
          await interaction.reply({
            content: stopped ? 'Terminal session ended.' : 'No active terminal session.',
            flags: 64,
          });
        } catch (aiError) {
          loggerService.error('Failed to stop AI activity', { error: String(aiError) });
          await interaction.reply({
            content: 'Failed to stop AI session.',
            flags: 64,
          });
        }
      }

      if (action === 'chat' && subAction === 'stop' && channelId) {
        try {
          const { stopChat } = require('./commands/chat');
          const historyKey = `${channelId}_${interaction.user.id}`;
          stopChat(historyKey);
          await interaction.reply({
            content: 'Chat stopped.',
            flags: 64,
          });
        } catch (chatError) {
          loggerService.error('Failed to stop chat', { error: String(chatError) });
          await interaction.reply({
            content: 'Failed to stop chat.',
            flags: 64,
          });
        }
      }

      if (action === 'chat' && subAction === 'clear' && channelId) {
        await interaction.reply({
          content: 'Use `/chat --clear` to clear chat history.',
          flags: 64,
        });
      }    } catch (buttonError) {
      loggerService.error('Button interaction error', { 
        error: String(buttonError),
        customId: interaction.customId,
        user: interaction.user.id
      });
      
      try {
        await interaction.reply({
          content: '❌ An error occurred while processing your request.',
          flags: 64,
        });
      } catch {}
    }
  }

  if (interaction.isStringSelectMenu()) {
    try {
      if (interaction.customId === 'projectpick_select') {
        const selectedValue = interaction.values[0];
        const projects = memoryService.getProjects();
        const project = selectedValue.startsWith('project_index_')
          ? projects[Number(selectedValue.slice('project_index_'.length))]
          : memoryService.getProjectByPath(selectedValue);
        if (!project) {
          await interaction.reply({ content: ' Project not found.', flags: 64 });
          return;
        }

        if (permissionBrokerService.requiresApproval(project.path)) {
          const request = permissionBrokerService.createRequest(project.path, interaction.user.id, interaction.channelId);
          const { ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
          await interaction.update({
            content: `Project selected: \`${project.name}\`.\nWrite access is required for \`${request.rootPath}\` before switching. Approve access and the bot will switch to this project automatically.`,
            components: [
              new ActionRowBuilder().addComponents(
                new ButtonBuilder()
                  .setCustomId(`discordwriteapprove_${request.id}`)
                  .setLabel('Approve write')
                  .setStyle(ButtonStyle.Success),
                new ButtonBuilder()
                  .setCustomId(`discordwritedeny_${request.id}`)
                  .setLabel('Deny')
                  .setStyle(ButtonStyle.Secondary)
              ),
            ],
            embeds: [],
          });
          return;
        }

        terminalService.getOrCreateSession(interaction.channelId);
        terminalService.changeDirectory(interaction.channelId, project.path);
        if (project.preferredCLI) {
          aiService.setCLI(project.preferredCLI);
        }
        if (project.preferredModel) {
          aiService.setDefaultModel(project.preferredModel, aiService.getCLI());
        }
        memoryService.setProjectAISettings(project.path, {
          cli: aiService.getCLI(),
          model: aiService.getDefaultModel(),
        });

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle(`Project: ${project.name}`)
              .setColor(require('discord.js').Colors.Green)
              .setDescription(`Path: \`${project.path}\`\nCWD updated for this channel.\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (interaction.customId === 'sessionpick_select') {
        const channelId = interaction.values[0];
        const session = terminalService.getSession(channelId);
        if (!session) {
          await interaction.reply({ content: ' Session not found.', flags: 64 });
          return;
        }

        terminalService.changeDirectory(interaction.channelId, session.cwd);
        terminalService.setSelectedDevice(interaction.channelId, session.selectedDeviceId);

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle('Session Switched')
              .setColor(require('discord.js').Colors.Green)
              .setDescription(`Using session from channel: \`${session.channelId}\`\nCWD switched to: \`${session.cwd}\`\nDevice: ${session.selectedDeviceId ? `\`${session.selectedDeviceId}\`` : 'auto-detect'}`)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (interaction.customId === 'devicepick_select') {
        const selectedValue = interaction.values[0];
        const deviceId = selectedValue === 'device_auto'
          ? undefined
          : selectedValue.startsWith('device_')
            ? selectedValue.slice('device_'.length)
            : undefined;

        terminalService.setSelectedDevice(interaction.channelId, deviceId);

        await interaction.update({
          embeds: [
            new (require('discord.js').EmbedBuilder)()
              .setTitle('Android Device Selected')
              .setColor(require('discord.js').Colors.Green)
              .setDescription(deviceId ? `Selected device: \`${deviceId}\`` : 'Selected device: auto-detect')
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }
    } catch (selectError) {
      loggerService.error('String select interaction error', {
        error: String(selectError),
        customId: interaction.customId,
        user: interaction.user.id,
      });

      try {
        await interaction.reply({
          content: ' An error occurred while processing your selection.',
          flags: 64,
        });
      } catch {}
    }
  }
});

client.on('disconnect', () => {
  loggerService.warn('Disconnected from Discord');
});

client.on('reconnecting', () => {
  loggerService.info('Reconnecting to Discord...');
});

client.on('resumed', () => {
  loggerService.info('Reconnected to Discord');
});

process.on('unhandledRejection', (reason, promise) => {
  loggerService.error('Unhandled Rejection', { reason: String(reason) });
});

process.on('uncaughtException', (error) => {
  loggerService.error('Uncaught Exception', { error: error.message, stack: error.stack });
  process.exit(1);
});

process.on('SIGINT', async () => {
  stopEventLoopWatchdog();
  loggerService.info('Received SIGINT, shutting down...');
  await aiService.shutdown();
  loggerService.shutdown();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  stopEventLoopWatchdog();
  loggerService.info('Received SIGTERM, shutting down...');
  await aiService.shutdown();
  loggerService.shutdown();
  process.exit(0);
});

loggerService.info('Attempting to login...');
startEventLoopWatchdog();
const maxRetries = 3;
const retryDelay = 5000;

const attemptLogin = async (attempt: number): Promise<void> => {
  try {
    await client.login(token);
  } catch (error) {
    loggerService.error(`Login attempt ${attempt}/${maxRetries} failed`, { 
      error: error instanceof Error ? error.message : String(error) 
    });
    
    if (attempt < maxRetries) {
      loggerService.info(`Retrying in ${retryDelay / 1000}s...`);
      await new Promise(resolve => setTimeout(resolve, retryDelay));
      return attemptLogin(attempt + 1);
    }
    
    loggerService.error('All login attempts failed', { 
      error: error instanceof Error ? error.message : String(error) 
    });
    process.exit(1);
  }
};

attemptLogin(1);

export default client;





