package mtr.screen;

import mtr.block.BlockTrainAnnouncer;
import mtr.data.DataConverter;
import mtr.data.RailwayData;
import mtr.mappings.Text;
import mtr.util.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.stream.Collectors;

public class TrainAnnouncerScreen extends TrainSensorScreenBase {

	private final String initialMessage;
	private final String initialSoundIdString;
	private final DashboardList availableSoundsList;

	private static final int MAX_MESSAGE_LENGTH = 256;

	public TrainAnnouncerScreen(BlockPos pos) {
		super(pos, true,
				new Tuple<>(new WidgetBetterTextField("", MAX_MESSAGE_LENGTH), Text.translatable("gui.mtr.announcement_message")),
				new Tuple<>(new WidgetBetterTextField("", MAX_MESSAGE_LENGTH), Text.translatable("gui.mtr.sound_file"))
		);

		final ClientLevel world = minecraft.level;
		if (world != null) {
			final BlockEntity entity = world.getBlockEntity(pos);
			if (entity instanceof BlockTrainAnnouncer.TileEntityTrainAnnouncer) {
				initialMessage = ((BlockTrainAnnouncer.TileEntityTrainAnnouncer) entity).getMessage();
				initialSoundIdString = ((BlockTrainAnnouncer.TileEntityTrainAnnouncer) entity).getSoundIdString();
			} else {
				initialMessage = "";
				initialSoundIdString = "";
			}
		} else {
			initialMessage = "";
			initialSoundIdString = "";
		}

		availableSoundsList = new DashboardList((data, color) -> {
			final String soundIdString = data.name;
			if (!soundIdString.isEmpty() && world != null && minecraft.player != null) {
				world.playLocalSound(pos, SoundEvent.createVariableRangeEvent(Identifier.parse(soundIdString)), SoundSource.BLOCKS, 1000000, 1, false);
			}
		}, null, null, null, (data, color) -> {
			textFields[1].setValue(data.name);
			setListVisibility(false);
		}, null, null, () -> "", text -> {
		}, false);
		availableSoundsList.setData(Minecraft.getInstance().getSoundManager().getAvailableSounds().stream().map(soundId -> new DataConverter(soundId.toString(), ARGB_BACKGROUND)).sorted().collect(Collectors.toList()), true, false, false, false, true, false);
	}

	@Override
	protected void init() {
		super.init();
		textFields[0].setValue(initialMessage);
		textFields[1].setValue(initialSoundIdString);

		setListVisibility(false);
		availableSoundsList.y = SQUARE_SIZE * 2 + TEXT_HEIGHT + TEXT_PADDING + TEXT_FIELD_PADDING;
		availableSoundsList.height = height - availableSoundsList.y - SQUARE_SIZE;
		availableSoundsList.width = width / 2 - SQUARE_SIZE;
		availableSoundsList.init(this::addRenderableWidget);
	}

	@Override
	public void tick() {
		super.tick();
		availableSoundsList.tick();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		if (event.button() == 0) {
			if (RailwayData.isBetween(event.x(), UtilitiesClient.getWidgetX(textFields[1]), UtilitiesClient.getWidgetX(textFields[1]) + textFields[1].getWidth()) && RailwayData.isBetween(event.y(), UtilitiesClient.getWidgetY(textFields[1]), UtilitiesClient.getWidgetY(textFields[1]) + textFields[1].getHeight())) {
				setListVisibility(true);
			} else if (!RailwayData.isBetween(event.x(), availableSoundsList.x, availableSoundsList.x + availableSoundsList.width) || !RailwayData.isBetween(event.y(), availableSoundsList.y, availableSoundsList.y + availableSoundsList.height)) {
				setListVisibility(false);
			}
		}
		return super.mouseClicked(event, isDoubleClick);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		availableSoundsList.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double amount) {
		availableSoundsList.mouseScrolled(mouseX, mouseY, scrollX, amount);
		return super.mouseScrolled(mouseX, mouseY, scrollX, amount);
	}

	@Override
	protected void renderAdditional(GuiGraphicsExtractor guiGraphics) {
		guiGraphics.fill(availableSoundsList.x, availableSoundsList.y, availableSoundsList.x + availableSoundsList.width, availableSoundsList.y + availableSoundsList.height, ARGB_BACKGROUND);
		availableSoundsList.render(guiGraphics, font);
	}

	private void setListVisibility(boolean visible) {
		availableSoundsList.x = visible ? width / 2 : width;
	}
}
