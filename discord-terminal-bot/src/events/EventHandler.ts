import { Events, Client, GatewayIntentBits, REST, Routes, Collection, ButtonBuilder, ButtonStyle, ActionRowBuilder, EmbedBuilder, Colors } from 'discord.js';
import { token, clientId, guildId } from '../config';
import { loggerService, terminalService, securityService, aiService, memoryService } from '../services';
import { SupportedCLI } from '../services/AIService';
import path from 'path';
import fs from 'fs';

export function registerEvents(client: Client & { commands: Collection<any, any>; runningProcesses: Map<string, string> }): void {
  const eventsPath = path.join(__dirname, '../events');
  const eventFiles = fs.readdirSync(eventsPath).filter(file => file.endsWith('.ts') || file.endsWith('.js'));

  for (const file of eventFiles) {
    const event = require(path.join(eventsPath, file)).default;
    if (event.once) {
      client.once(event.name, (...args: any[]) => event.execute(...args, client));
    } else {
      client.on(event.name, (...args: any[]) => event.execute(...args, client));
    }
  }

  client.on(Events.ClientReady, async (c: Client) => {
    loggerService.info(`Logged in as ${c.user?.tag}`);
    loggerService.info(`Bot is ready in ${c.guilds.cache.size} guild(s)`);

    try {
      const rest = new REST({ version: '10' }).setToken(token);
      const commands = [];
      const commandsPath = path.join(__dirname, '../commands');

      for (const file of fs.readdirSync(commandsPath).filter(f => f.endsWith('.ts') || f.endsWith('.js'))) {
        const command = require(path.join(commandsPath, file)).default;
        if (command.data) {
          commands.push(command.data.toJSON());
        }
      }

      if (guildId && clientId) {
        await rest.put(Routes.applicationGuildCommands(clientId, guildId), { body: commands });
        loggerService.info(`Registered ${commands.length} guild commands`);
      } else {
        await rest.put(Routes.applicationCommands(clientId), { body: commands });
        loggerService.info(`Registered ${commands.length} global commands`);
      }
    } catch (error) {
      loggerService.error('Failed to register commands', { error });
    }
  });

  client.on(Events.InteractionCreate, async (interaction: any) => {
    if (!interaction.isChatInputCommand() && !interaction.isButton() && !interaction.isStringSelectMenu()) return;

    if (interaction.isChatInputCommand()) {
      const command = client.commands.get(interaction.commandName);
      if (!command) return;

      try {
        if (command.ownerOnly && !securityService.isOwner(interaction.user.id)) {
          await interaction.reply({
            content: '❌ This command is restricted to bot owners only.',
            flags: 64,
          });
          return;
        }

        await command.execute(interaction);
      } catch (error) {
        loggerService.error(`Error executing command ${interaction.commandName}`, { error, user: interaction.user.id });
        
        const errorMessage = error instanceof Error ? error.message : 'An unknown error occurred';
        
        if (interaction.deferred) {
          await interaction.editReply({
            content: `❌ Error: ${errorMessage}`,
          });
        } else {
          await interaction.reply({
            content: `❌ Error: ${errorMessage}`,
            flags: 64,
          });
        }
      }
    }

    if (interaction.isButton()) {
      const [action, channelId] = interaction.customId.split('_');
      
      if (action === 'stop' && channelId) {
        const processInfo = terminalService.getActiveProcess(channelId);
        if (processInfo) {
          terminalService.killProcess(channelId);
          await interaction.reply({
            content: '🛑 Process stopped.',
            flags: 64,
          });
          client.runningProcesses.delete(channelId);
        } else {
          await interaction.reply({
            content: 'No running process to stop.',
            flags: 64,
          });
        }
      }

      if (action === 'refresh' && channelId) {
        const session = terminalService.getSession(channelId);
        if (session) {
          const embed = loggerService.createSessionEmbed(session);
          await interaction.reply({
            embeds: [embed],
            flags: 64,
          });
        }
      }

      if (action === 'clibtn') {
        const cliName = channelId as SupportedCLI;
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized.', flags: 64 });
          return;
        }

        const SUPPORTED_CLIS: { value: SupportedCLI; label: string; desc: string }[] = [
          { value: 'opencode', label: 'opencode', desc: 'OpenCode CLI (`opencode run --format json`)' },
          { value: 'claude', label: 'claude', desc: 'Claude Code CLI (`claude --print`)' },
          { value: 'codex', label: 'codex', desc: 'OpenAI Codex CLI (`codex exec --json`)' },
        ];

        aiService.setCLI(cliName);
        const currentCwd = terminalService.getSession(interaction.channelId)?.cwd;
        if (currentCwd) {
          memoryService.setProjectAISettingsByCwd(currentCwd, {
            cli: cliName,
            model: aiService.getDefaultModel(),
          });
        }

        const embed = new EmbedBuilder()
          .setTitle('AI CLI Backend')
          .setColor(Colors.Green)
          .setDescription(`Switched to: \`${cliName}\`\nCurrent model: \`${aiService.getDefaultModel()}\``)
          .addFields({
            name: 'Available CLIs',
            value: SUPPORTED_CLIS.map(c =>
              `\`${c.value}\`${c.value === cliName ? ' ✅' : ''} — ${c.desc}`
            ).join('\n'),
          });

        const row = new ActionRowBuilder<ButtonBuilder>().addComponents(
          SUPPORTED_CLIS.map(c =>
            new ButtonBuilder()
              .setCustomId(`clibtn_${c.value}`)
              .setLabel(c.value)
              .setStyle(c.value === cliName ? ButtonStyle.Success : ButtonStyle.Secondary)
          )
        );

        await interaction.update({ embeds: [embed], components: [row] });
      }

      if (action === 'quickmodel') {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized.', flags: 64 });
          return;
        }

        const selectedModel = channelId;
        const validation = await aiService.validateModelSelectionForCurrentCLI(selectedModel);
        if (!validation.ok) {
          await interaction.reply({ content: `❌ ${validation.error}`, flags: 64 });
          return;
        }

        const normalizedModel = validation.normalized || selectedModel;
        aiService.setDefaultModel(normalizedModel, aiService.getCLI());
        memoryService.setDefaultModel(normalizedModel, aiService.getCLI());
        const currentCwd = terminalService.getSession(interaction.channelId)?.cwd;
        if (currentCwd) {
          memoryService.setProjectAISettingsByCwd(currentCwd, {
            cli: aiService.getCLI(),
            model: normalizedModel,
          });
        }

        await interaction.reply({
          content: `✅ Default model set to \`${normalizedModel}\``,
          flags: 64,
        });
      }

      if (action === 'quickmodelpage') {
        if (!securityService.isOwner(interaction.user.id)) {
          await interaction.reply({ content: '❌ You are not authorized.', flags: 64 });
          return;
        }

        const [, cliName, pageRaw] = interaction.customId.split('_');
        const page = Math.max(0, Number(pageRaw) || 0);
        const models = await aiService.listModelsForCLI(cliName as SupportedCLI).catch(() => aiService.getSupportedModels(cliName as SupportedCLI));
        const pageModels = models.slice(page * 10, page * 10 + 10);
        const rows: ActionRowBuilder<ButtonBuilder>[] = [];

        for (let i = 0; i < pageModels.length; i += 5) {
          rows.push(
            new ActionRowBuilder<ButtonBuilder>().addComponents(
              pageModels.slice(i, i + 5).map(model =>
                new ButtonBuilder()
                  .setCustomId(`quickmodel_${model}`)
                  .setLabel(model.slice(0, 80))
                  .setStyle(model === aiService.getDefaultModel() ? ButtonStyle.Success : ButtonStyle.Secondary)
              )
            )
          );
        }

        if (models.length > 10) {
          rows.push(
            new ActionRowBuilder<ButtonBuilder>().addComponents(
              new ButtonBuilder()
                .setCustomId(`quickmodelpage_${cliName}_${page - 1}`)
                .setLabel('Prev')
                .setStyle(ButtonStyle.Primary)
                .setDisabled(page <= 0),
              new ButtonBuilder()
                .setCustomId(`quickmodelpage_${cliName}_${page + 1}`)
                .setLabel('Next')
                .setStyle(ButtonStyle.Primary)
                .setDisabled((page + 1) * 10 >= models.length)
            )
          );
        }

        await interaction.update({
          embeds: [
            new EmbedBuilder()
              .setTitle(`${aiService.getCliDisplayName(cliName as SupportedCLI)} Models`)
              .setColor(Colors.Blue)
              .setDescription(`Current CLI: \`${cliName}\`\nCurrent default: \`${aiService.getDefaultModel()}\`\nPage: ${page + 1}/${Math.max(1, Math.ceil(models.length / 10))}`)
              .addFields({
                name: 'Models',
                value: pageModels.map(model => `\`${model}\``).join('\n') || 'No models',
              })
              .setTimestamp(),
          ],
          components: rows,
        });
      }

      if (action === 'historyclear' && channelId) {
        terminalService.clearHistory(channelId);
        await interaction.reply({
          content: '✅ Command history cleared.',
          flags: 64,
        });
      }
    }

    if (interaction.isStringSelectMenu()) {
      if (interaction.customId === 'projectpick_select') {
        const projectPath = interaction.values[0];
        const project = memoryService.getProjectByPath(projectPath);
        if (!project) {
          await interaction.reply({ content: '❌ Project not found.', flags: 64 });
          return;
        }

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
            new EmbedBuilder()
              .setTitle(`Project: ${project.name}`)
              .setColor(Colors.Green)
              .setDescription(`Path: \`${project.path}\`\nCLI: \`${aiService.getCLI()}\`\nModel: \`${aiService.getDefaultModel()}\``)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (interaction.customId === 'memorypick_select') {
        const selected = interaction.values[0];
        if (selected === 'clear') {
          memoryService.clearHistory();
          await interaction.update({
            embeds: [
              new EmbedBuilder()
                .setTitle('Memory')
                .setColor(Colors.Green)
                .setDescription('Recent memory history cleared.')
                .setTimestamp(),
            ],
            components: [],
          });
          return;
        }

        if (selected === 'projects') {
          const projects = memoryService.getProjects();
          await interaction.update({
            embeds: [
              new EmbedBuilder()
                .setTitle('Memory: Projects')
                .setColor(Colors.Blue)
                .setDescription(
                  projects.length > 0
                    ? projects.slice(0, 12).map(project => `• ${project.name}\n\`${project.path}\``).join('\n\n').slice(0, 4000)
                    : 'No projects tracked.'
                )
                .setTimestamp(),
            ],
            components: [],
          });
          return;
        }

        if (selected === 'commands') {
          const commands = memoryService.getRecentCommands(12);
          await interaction.update({
            embeds: [
              new EmbedBuilder()
                .setTitle('Memory: Commands')
                .setColor(Colors.Blue)
                .setDescription(
                  commands.length > 0
                    ? commands.map((cmd, index) => `${index + 1}. \`${cmd.command}\`\n\`${cmd.cwd}\``).join('\n\n').slice(0, 4000)
                    : 'No commands tracked.'
                )
                .setTimestamp(),
            ],
            components: [],
          });
          return;
        }

        const stats = memoryService.getStats();
        await interaction.update({
          embeds: [
            new EmbedBuilder()
              .setTitle('Memory: Stats')
              .setColor(Colors.Blue)
              .setDescription(`Commands: ${stats.totalCommands}\nProjects: ${stats.totalProjects}\nUptime: ${stats.uptime}`)
              .setTimestamp(),
          ],
          components: [],
        });
        return;
      }

      if (interaction.customId === 'sessionpick_select') {
        const selectedChannelId = interaction.values[0];
        const session = terminalService.getSession(selectedChannelId);
        await interaction.update({
          embeds: [
            new EmbedBuilder()
              .setTitle('Session')
              .setColor(Colors.Blue)
              .setDescription(
                session
                  ? `Channel: \`${session.channelId}\`\nCWD: \`${session.cwd}\`\nHistory: ${session.history.length}\nActive Process: ${session.activeProcess ? 'Yes' : 'No'}`
                  : 'Session not found.'
              )
              .setTimestamp(),
          ],
          components: [],
        });
        return;
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
    loggerService.info('Received SIGINT, shutting down...');
    loggerService.shutdown();
    process.exit(0);
  });

  process.on('SIGTERM', async () => {
    loggerService.info('Received SIGTERM, shutting down...');
    loggerService.shutdown();
    process.exit(0);
  });
}
