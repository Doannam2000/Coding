import { SlashCommandBuilder, EmbedBuilder, Colors, ActionRowBuilder, StringSelectMenuBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { securityService, terminalService, processQueueService } from '../services';
import { splitMessage } from '../utils';

export const data = new SlashCommandBuilder()
  .setName('sessions')
  .setDescription('List all terminal sessions');

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: '❌ You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const sessions = terminalService.getAllSessions();

  if (sessions.length === 0) {
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Terminal Sessions')
          .setColor(Colors.Blue)
          .setDescription('No active sessions.')
          .setTimestamp()
      ],
      flags: 64,
    });
    return;
  }

  const sessionList = sessions.map(session => {
    const hasProcess = session.activeProcess !== null;
    return `**Channel:** ${session.channelId}\n` +
           `**CWD:** \`${session.cwd}\`\n` +
           `**History:** ${session.history.length} commands\n` +
           `**Active Process:** ${hasProcess ? '✅ Yes' : '❌ No'}\n` +
           `**Created:** ${session.createdAt.toLocaleString()}`;
  });

  const messages = splitMessage(sessionList.join('\n\n---\n\n'), 4000);

  const embeds = messages.map((msg, i) => 
    new EmbedBuilder()
      .setTitle(`Terminal Sessions (${sessions.length})`)
      .setColor(Colors.Purple)
      .setDescription(msg)
      .setFooter({ text: i === 0 ? `Page 1/${messages.length}` : `Page ${i + 1}/${messages.length}` })
      .setTimestamp()
  );

  await interaction.reply({
    embeds,
    components: sessions.length > 0 ? [
      new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(
        new StringSelectMenuBuilder()
          .setCustomId('sessionpick_select')
          .setPlaceholder('Inspect a session')
          .addOptions(
            sessions.slice(0, 25).map(session => ({
              label: session.channelId.slice(0, 100),
              value: session.channelId,
              description: session.cwd.slice(0, 100),
            }))
          )
      ),
      new ActionRowBuilder<ButtonBuilder>().addComponents(
        new ButtonBuilder()
          .setCustomId(`sessionclearcurrent_${interaction.channelId}`)
          .setLabel('Clear current')
          .setStyle(ButtonStyle.Danger),
        new ButtonBuilder()
          .setCustomId('sessionclearall_global')
          .setLabel('Clear all')
          .setStyle(ButtonStyle.Secondary)
      ),
    ] : [],
    flags: 64,
  });
}

export default { data, execute };
