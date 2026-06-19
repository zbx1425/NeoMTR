package mtr.screen;

import mtr.MTR;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import mtr.packet.IPacket;
import mtr.packet.PacketTrainDataGuiClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public class TicketMachineScreen extends ScreenMapper implements IGui, IPacket {

	private final Button[] buttons = new Button[BUTTON_COUNT];
	private final Component balanceText;

	private static final int EMERALD_TO_DOLLAR = 10;
	private static final int BUTTON_COUNT = 10;
	private static final int BUTTON_WIDTH = 80;

	public TicketMachineScreen(int balance) {
		super(Text.literal(""));

		for (int i = 0; i < BUTTON_COUNT; i++) {
			final int index = i;
			buttons[i] = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_value"), button -> {
				PacketTrainDataGuiClient.addBalanceC2S(getAddAmount(index), (int) Math.pow(2, index));
				if (minecraft != null) {
					UtilitiesClient.setScreen(minecraft, null);
				}
			});
		}

		balanceText = Text.translatable("gui.mtr.balance", balance);
	}

	@Override
	protected void init() {
		super.init();

		for (int i = 0; i < BUTTON_COUNT; i++) {
			IDrawing.setPositionAndWidth(buttons[i], width - BUTTON_WIDTH, SQUARE_SIZE * (i + 1), BUTTON_WIDTH - TEXT_FIELD_PADDING);
		}

		for (final Button button : buttons) {
			addDrawableChild(button);
		}
	}

	@Override
	public void tick() {
		final int emeraldCount = getEmeraldCount();
		for (int i = 0; i < BUTTON_COUNT; i++) {
			buttons[i].active = emeraldCount >= Math.pow(2, i);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(guiGraphics, mouseX, mouseY, delta);
		try {
			final Component emeraldsText = Text.translatable("gui.mtr.emeralds", getEmeraldCount());
			guiGraphics.text(font, balanceText, TEXT_PADDING, TEXT_PADDING, ARGB_WHITE);
			guiGraphics.text(font, emeraldsText, width - TEXT_PADDING - font.width(emeraldsText), TEXT_PADDING, ARGB_WHITE);

			for (int i = 0; i < BUTTON_COUNT; i++) {
				guiGraphics.text(font, Text.translatable("gui.mtr.add_balance_for_emeralds", getAddAmount(i), (int) Math.pow(2, i)), TEXT_PADDING, (i + 1) * SQUARE_SIZE + TEXT_PADDING, ARGB_WHITE);
			}
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int getEmeraldCount() {
		if (minecraft != null && minecraft.player != null) {
			return Utilities.getInventory(minecraft.player).countItem(Items.EMERALD);
		} else {
			return 0;
		}
	}

	private static int getAddAmount(int index) {
		return (int) Math.ceil(Math.pow(2, index) * (EMERALD_TO_DOLLAR + index));
	}
}
