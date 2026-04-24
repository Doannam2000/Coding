import { SlashCommandBuilder, ActionRowBuilder, StringSelectMenuBuilder, EmbedBuilder, Colors } from 'discord.js';
import { securityService, androidService, terminalService } from '../services';

export const data = new SlashCommandBuilder()
  .setName('devices')
  .setDescription('List connected adb devices and choose the active Android device for this channel');

export async function execute(interaction: any): Promise<void> {
  const userId = interaction.user.id;
  const channelId = interaction.channelId;

  if (!securityService.isOwner(userId)) {
    await interaction.reply({
      content: 'You are not authorized to use this command.',
      flags: 64,
    });
    return;
  }

  const devices = androidService.listConnectedDevices();
  const currentDevice = terminalService.getSelectedDevice(channelId);

  if (devices.length === 0) {
    await interaction.reply({
      embeds: [
        new EmbedBuilder()
          .setTitle('Android Devices')
          .setColor(Colors.Blue)
          .setDescription('No adb devices detected.')
          .setTimestamp(),
      ],
      flags: 64,
    });
    return;
  }

  const selectMenu = new StringSelectMenuBuilder()
    .setCustomId('devicepick_select')
    .setPlaceholder('Choose an Android device')
    .addOptions([
      {
        label: 'Auto-detect',
        value: 'device_auto',
        description: 'Use the first connected device with status device',
      },
      ...devices.slice(0, 24).map((device) => ({
        label: device.id.slice(0, 100),
        value: `device_${device.id}`,
        description: `${device.status}${device.id === currentDevice ? ' | selected' : ''}`.slice(0, 100),
      })),
    ]);

  const row = new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(selectMenu);
  const description = devices.map((device, index) => {
    const selected = device.id === currentDevice ? ' [selected]' : '';
    return `${index + 1}. \`${device.id}\` | ${device.status}${selected}`;
  }).join('\n');

  await interaction.reply({
    embeds: [
      new EmbedBuilder()
        .setTitle('Android Devices')
        .setColor(Colors.Blue)
        .setDescription(description)
        .setFooter({ text: `Current device: ${currentDevice || 'auto-detect'}` })
        .setTimestamp(),
    ],
    components: [row],
    flags: 64,
  });
}

export default { data, execute };
