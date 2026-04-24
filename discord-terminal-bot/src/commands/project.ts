import {
  SlashCommandBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  Colors,
  EmbedBuilder,
  StringSelectMenuBuilder,
} from 'discord.js';
import { aiService, memoryService, terminalService, permissionBrokerService } from '../services';
import fs from 'fs';

function buildProjectPreview(limit: number = 10): string {
  const projects = memoryService.getProjects().slice(0, limit);
  return projects.map((project, index) => {
    const exists = fs.existsSync(project.path);
    const status = exists ? '[ok]' : '[missing]';
    return `${index + 1}. ${status} **${project.name}**\n\`${project.path}\`\nCLI: \`${project.preferredCLI || aiService.getCLI()}\` | Model: \`${project.preferredModel || aiService.getDefaultModel()}\``;
  }).join('\n\n');
}

export const data = new SlashCommandBuilder()
  .setName('project')
  .setDescription('List and switch between projects')
  .addStringOption(option =>
    option.setName('select')
      .setDescription('Select a project by name')
      .setRequired(false)
  )
  .addStringOption(option =>
    option.setName('path')
      .setDescription('Set new path for project')
      .setRequired(false)
  );

export async function execute(interaction: any) {
  const select = interaction.options.getString('select');
  const newPath = interaction.options.getString('path');
  const projects = memoryService.getProjects();

  if (projects.length === 0) {
    await interaction.reply({
      content: 'No projects found. Run some commands to track projects first.',
      ephemeral: true,
    });
    return;
  }

  if (select) {
    const project = projects.find(p =>
      p.name.toLowerCase() === select.toLowerCase() ||
      p.path === select
    );

    if (!project) {
      await interaction.reply({
        content: `Project "${select}" not found.`,
        ephemeral: true,
      });
      return;
    }

    const pathExists = fs.existsSync(project.path);

    if (!pathExists && newPath) {
      project.path = newPath;
      if (project.type === 'android') {
        project.applicationId = memoryService.extractAndroidAppId(newPath);
      }
      await interaction.reply({
        content: `Updated project path to \`${newPath}\``,
        ephemeral: true,
      });
      return;
    }

    if (!pathExists) {
      const buttons = projects.slice(0, 10).map(p =>
        new ButtonBuilder()
          .setCustomId(`projectpick_${p.path}`)
          .setLabel(p.name.slice(0, 80))
          .setStyle(ButtonStyle.Secondary)
      );

      const rows = [];
      for (let i = 0; i < buttons.length; i += 5) {
        rows.push(new ActionRowBuilder<ButtonBuilder>().addComponents(buttons.slice(i, i + 5)));
      }

      await interaction.reply({
        content: `Path "${project.path}" does not exist. Provide a replacement with \`/project ${project.name} --path <new-path>\`.`,
        components: rows,
        ephemeral: true,
      });
      return;
    }

    if (permissionBrokerService.requiresApproval(project.path)) {
      const request = permissionBrokerService.createRequest(project.path, interaction.user.id, interaction.channelId);
      const approvalRow = new ActionRowBuilder<ButtonBuilder>().addComponents(
        new ButtonBuilder()
          .setCustomId(`discordwriteapprove_${request.id}`)
          .setLabel('Approve write')
          .setStyle(ButtonStyle.Success),
        new ButtonBuilder()
          .setCustomId(`discordwritedeny_${request.id}`)
          .setLabel('Deny')
          .setStyle(ButtonStyle.Secondary)
      );

      await interaction.reply({
        content: `Project selected: \`${project.name}\`.\nWrite access is required for \`${request.rootPath}\` before switching. Approve access and the bot will switch to this project automatically.`,
        components: [approvalRow],
        ephemeral: true,
      });
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

    const mdFiles = memoryService.getProjectMarkdownFiles(project.path);
    memoryService.updateProjectReadme(project.path);
    memoryService.updateProjectContextLoaded(project.path);

    const embed = new EmbedBuilder()
      .setTitle(`Project: ${project.name}`)
      .setColor(Colors.Green)
      .addFields(
        { name: 'Path', value: `\`${project.path}\``, inline: false },
        { name: 'Type', value: project.type, inline: true },
        { name: 'AI CLI', value: `\`${aiService.getCLI()}\``, inline: true },
        { name: 'AI Model', value: `\`${aiService.getDefaultModel()}\``, inline: true },
      )
      .setTimestamp();

    if (project.applicationId) {
      embed.addFields({ name: 'App ID', value: `\`${project.applicationId}\``, inline: true });
    }

    if (mdFiles.length > 0) {
      embed.addFields({
        name: 'Docs',
        value: mdFiles.slice(0, 6).map(file => `- ${file.filename}`).join('\n').slice(0, 1024),
        inline: false,
      });
    }

    await interaction.reply({
      embeds: [
        embed.setFooter({
          text: 'Changing /cli or /model while this project is active updates the project AI defaults.',
        }),
      ],
      ephemeral: true,
    });
    return;
  }

  const selectMenu = new StringSelectMenuBuilder()
    .setCustomId('projectpick_select')
    .setPlaceholder('Select a project')
    .addOptions(
      projects.slice(0, 25).map((project, index) => ({
        label: project.name.slice(0, 100),
        value: `project_index_${index}`,
        description: `${project.type} | ${project.preferredCLI || aiService.getCLI()}`.slice(0, 100),
      }))
    );

  const row = new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(selectMenu);

  await interaction.reply({
    embeds: [
      new EmbedBuilder()
        .setTitle('Projects')
        .setColor(Colors.Blue)
        .setDescription(buildProjectPreview())
        .setFooter({ text: 'Use the selector to switch project' })
        .setTimestamp(),
    ],
    components: [row],
    ephemeral: true,
  });
}

export default { data, execute };

