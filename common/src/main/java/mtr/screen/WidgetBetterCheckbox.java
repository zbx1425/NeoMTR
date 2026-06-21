package mtr.screen;

import mtr.data.IGui;
import mtr.util.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class WidgetBetterCheckbox extends Checkbox implements IGui {

	private final OnClick onClick;

	public WidgetBetterCheckbox(int x, int y, int width, int height, Component text, OnClick onClick) {
		super(x, y, width, height, text, false);
		this.onClick = onClick;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		super.onPress(input);
		onClick.onClick(selected());
	}

	@Override
	public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractContents(guiGraphics, mouseX, mouseY, delta);
		if (visible) {
			guiGraphics.text(Minecraft.getInstance().font, getMessage(), UtilitiesClient.getWidgetX(this) + 24, UtilitiesClient.getWidgetY(this) + (height - 8) / 2, ARGB_WHITE);
		}
	}


	public void setChecked(boolean checked) {
		if (checked != selected()) {
			setSelected(checked);
		}
	}

	@FunctionalInterface
	public interface OnClick {
		void onClick(boolean checked);
	}
}
