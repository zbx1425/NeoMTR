package mtr.screen;

import mtr.MTR;
import mtr.block.BlockLiftTrackFloor;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.packet.IPacket;
import mtr.packet.PacketTrainDataGuiClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LiftTrackFloorScreen extends ScreenMapper implements IGui, IPacket {

	private final WidgetBetterTextField textFieldFloorNumber;
	private final WidgetBetterTextField textFieldFloorDescription;
	private final WidgetBetterCheckbox checkboxShouldDing;
	private final WidgetBetterCheckbox checkboxDisableCarCall;

	private final BlockPos pos;
	private final String initialFloorNumber;
	private final String initialFloorDescription;
	private final boolean initialShouldDing;
	private final boolean initialDisableCarCall;
	private final int textWidth;
	private static final Component TEXT_FLOOR_NUMBER = Text.translatable("gui.mtr.lift_floor_number");
	private static final Component TEXT_FLOOR_DESCRIPTION = Text.translatable("gui.mtr.lift_floor_description");
	private static final int TEXT_FIELD_WIDTH = 240;

	public LiftTrackFloorScreen(BlockPos pos) {
		super(Text.literal(""));
		this.pos = pos;

		textFieldFloorNumber = new WidgetBetterTextField("1", 8);
		textFieldFloorDescription = new WidgetBetterTextField("Concourse");
		checkboxShouldDing = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.mtr.lift_should_ding"), checked -> {
		});
		// TODO I18n
		checkboxDisableCarCall = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.literal("Car Call Disable"), checked -> {
		});

		final Level world = Minecraft.getInstance().level;
		if (world == null) {
			initialFloorNumber = "1";
			initialFloorDescription = "";
			initialShouldDing = false;
			initialDisableCarCall = false;
		} else {
			final BlockEntity entity = world.getBlockEntity(pos);
			if (entity instanceof BlockLiftTrackFloor.TileEntityLiftTrackFloor) {
				initialFloorNumber = ((BlockLiftTrackFloor.TileEntityLiftTrackFloor) entity).getFloorNumber();
				initialFloorDescription = ((BlockLiftTrackFloor.TileEntityLiftTrackFloor) entity).getFloorDescription();
				initialShouldDing = ((BlockLiftTrackFloor.TileEntityLiftTrackFloor) entity).getShouldDing();
				initialDisableCarCall = ((BlockLiftTrackFloor.TileEntityLiftTrackFloor) entity).getDisableCarCall();
			} else {
				initialFloorNumber = "1";
				initialFloorDescription = "";
				initialShouldDing = false;
				initialDisableCarCall = false;
			}
		}

		font = Minecraft.getInstance().font;
		textWidth = Math.max(font.width(TEXT_FLOOR_NUMBER), font.width(TEXT_FLOOR_DESCRIPTION));
	}

	@Override
	protected void init() {
		super.init();

		final int startX = (width - textWidth - TEXT_PADDING - TEXT_FIELD_WIDTH) / 2;
		final int startY = (height - SQUARE_SIZE * 3 - TEXT_FIELD_PADDING * 2) / 2;
		IDrawing.setPositionAndWidth(textFieldFloorNumber, startX + textWidth + TEXT_PADDING + TEXT_FIELD_PADDING / 2, startY + TEXT_FIELD_PADDING / 2, TEXT_FIELD_WIDTH - TEXT_FIELD_PADDING);
		IDrawing.setPositionAndWidth(textFieldFloorDescription, startX + textWidth + TEXT_PADDING + TEXT_FIELD_PADDING / 2, startY + SQUARE_SIZE + TEXT_FIELD_PADDING * 3 / 2, TEXT_FIELD_WIDTH - TEXT_FIELD_PADDING);
		IDrawing.setPositionAndWidth(checkboxShouldDing, startX, startY + SQUARE_SIZE * 2 + TEXT_FIELD_PADDING * 2, TEXT_FIELD_WIDTH);
		IDrawing.setPositionAndWidth(checkboxDisableCarCall, startX, startY + SQUARE_SIZE * 2 + TEXT_FIELD_PADDING * 2 + SQUARE_SIZE, TEXT_FIELD_WIDTH);

		textFieldFloorNumber.setValue(initialFloorNumber);
		textFieldFloorDescription.setValue(initialFloorDescription);
		checkboxShouldDing.setChecked(initialShouldDing);
		checkboxDisableCarCall.setChecked(initialDisableCarCall);

		addDrawableChild(textFieldFloorNumber);
		addDrawableChild(textFieldFloorDescription);
		addDrawableChild(checkboxShouldDing);
		addDrawableChild(checkboxDisableCarCall);
	}

	@Override
	public void tick() {

	}

	@Override
	public void renderBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.renderBackground(guiGraphics, mouseX, mouseY, delta);
		try {
			final int startX = (width - textWidth - TEXT_PADDING - TEXT_FIELD_WIDTH) / 2;
			final int startY = (height - SQUARE_SIZE * 3 - TEXT_FIELD_PADDING * 2) / 2;
			guiGraphics.text(font, TEXT_FLOOR_NUMBER, startX, startY + TEXT_FIELD_PADDING / 2 + TEXT_PADDING, ARGB_WHITE);
			guiGraphics.text(font, TEXT_FLOOR_DESCRIPTION, startX, startY + SQUARE_SIZE + TEXT_FIELD_PADDING * 3 / 2 + TEXT_PADDING, ARGB_WHITE);
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
		guiGraphics.pose().translate(0, 0, 100);
	}

	@Override
	public void onClose() {
		PacketTrainDataGuiClient.sendLiftTrackFloorC2S(pos, textFieldFloorNumber.getValue(), textFieldFloorDescription.getValue(),
				checkboxShouldDing.selected(), checkboxDisableCarCall.selected());
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
