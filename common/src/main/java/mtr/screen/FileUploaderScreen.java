package mtr.screen;

import mtr.MTR;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class FileUploaderScreen extends ScreenMapper implements IGui {

	private final ScreenMapper screen;
	private final Consumer<List<Path>> filesCallback;

	public FileUploaderScreen(ScreenMapper screen, Consumer<List<Path>> filesCallback) {
		super(Text.literal(""));
		this.screen = screen;
		this.filesCallback = filesCallback;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(guiGraphics, mouseX, mouseY, delta);
		try {
			guiGraphics.centeredText(font, Text.translatable("gui.mtr.drag_file_to_upload"), width / 2, (height - TEXT_HEIGHT) / 2, ARGB_WHITE);
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		filesCallback.accept(paths);
		onClose();
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			UtilitiesClient.setScreen(minecraft, screen);
		}
	}
}
