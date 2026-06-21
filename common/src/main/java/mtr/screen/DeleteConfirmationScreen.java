package mtr.screen;

import mtr.MTR;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.screen.base.MTRScreen;
import mtr.mappings.Text;
import mtr.util.UtilitiesClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

public class DeleteConfirmationScreen extends MTRScreen implements IGui {

	private final Runnable deleteCallback;
	private final String name;
	private final DashboardScreen dashboardScreen;
	private final Button buttonYes;
	private final Button buttonNo;

	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HALF_PADDING = 10;

	public DeleteConfirmationScreen(Runnable deleteCallback, String name, DashboardScreen dashboardScreen) {
		super(Text.literal(""));

		this.deleteCallback = deleteCallback;
		this.name = name;
		this.dashboardScreen = dashboardScreen;

		buttonYes = UtilitiesClient.newButton(Text.translatable("gui.yes"), button -> onYes());
		buttonNo = UtilitiesClient.newButton(Text.translatable("gui.no"), button -> onNo());
	}

	@Override
	protected void init() {
		super.init();
		IDrawing.setPositionAndWidth(buttonYes, width / 2 - BUTTON_WIDTH - BUTTON_HALF_PADDING, height / 2, BUTTON_WIDTH);
		IDrawing.setPositionAndWidth(buttonNo, width / 2 + BUTTON_HALF_PADDING, height / 2, BUTTON_WIDTH);
		addRenderableWidget(buttonYes);
		addRenderableWidget(buttonNo);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(guiGraphics, mouseX, mouseY, delta);
		try {
			guiGraphics.centeredText(font, Text.translatable("gui.mtr.delete_confirmation", IGui.formatStationName(name)), width / 2, height / 2 - SQUARE_SIZE * 2 + TEXT_PADDING, ARGB_WHITE);
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
	}

	@Override
	public void onClose() {
		super.onClose();
        UtilitiesClient.setScreen(minecraft, dashboardScreen);
    }

	private void onYes() {
		deleteCallback.run();
		onClose();
	}

	private void onNo() {
		onClose();
	}
}
