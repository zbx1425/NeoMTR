package mtr.screen;

import mtr.data.IGui;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Function;

public class WidgetShorterSlider extends AbstractSliderButton implements IGui {

	private static final Identifier WIDGETS_LOCATION = Identifier.fromNamespaceAndPath("mtr", "textures/gui/widgets.png");

	private final int maxValue;
	private final int markerFrequency;
	private final int markerDisplayedRatio;
	private final Function<Integer, String> setMessage;
	private final Consumer<Integer> shiftClickAction;

	private static final int SLIDER_WIDTH = 6;
	private static final int TICK_HEIGHT = SQUARE_SIZE / 2;

	public WidgetShorterSlider(int x, int width, int maxValue, int markerFrequency, int markerDisplayedRatio, Function<Integer, String> setMessage, Consumer<Integer> shiftClickAction) {
		super(x, 0, width, 0, Text.literal(""), 0);
		this.maxValue = maxValue;
		this.setMessage = setMessage;
		this.shiftClickAction = shiftClickAction;
		this.markerFrequency = markerFrequency;
		this.markerDisplayedRatio = markerDisplayedRatio;
	}

	public WidgetShorterSlider(int x, int width, int maxValue, Function<Integer, String> setMessage, Consumer<Integer> shiftClickAction) {
		this(x, width, maxValue, 0, 0, setMessage, shiftClickAction);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
		super.onClick(event, isDoubleClick);
		checkShiftClick();
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(Math.min(width, 380));
	}

	@Override
	protected void updateMessage() {
		setMessage(Text.literal(setMessage.apply(getIntValue())));
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dx, double dy) {
		super.onDrag(event, dx, dy);
		checkShiftClick();
	}

	@Override
	protected void applyValue() {
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		render(guiGraphics);
	}
	
	public void setValue(int valueInt) {
		value = (double) valueInt / maxValue;
		updateMessage();
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getIntValue() {
		return (int) Math.round(value * maxValue);
	}

	private void render(GuiGraphicsExtractor guiGraphics) {
		final Minecraft client = Minecraft.getInstance();

		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this), UtilitiesClient.getWidgetY(this), 0, 46, width / 2, height / 2, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this), UtilitiesClient.getWidgetY(this) + height / 2, 0, (66 - height / 2), width / 2, height / 2, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this) + width / 2, UtilitiesClient.getWidgetY(this), (200 - width / 2), 46, width / 2, height / 2, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this) + width / 2, UtilitiesClient.getWidgetY(this) + height / 2, (200 - width / 2), (66 - height / 2), width / 2, height / 2, 256, 256);

		final int v = UtilitiesClient.isHovered(this) ? 86 : 66;
		final int xOffset = (width - SLIDER_WIDTH) * getIntValue() / maxValue;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this) + xOffset, UtilitiesClient.getWidgetY(this), 0, v, SLIDER_WIDTH / 2, height / 2, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this) + xOffset, UtilitiesClient.getWidgetY(this) + height / 2, 0, v + 20 - height / 2, SLIDER_WIDTH / 2, height / 2, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this) + xOffset + SLIDER_WIDTH / 2, UtilitiesClient.getWidgetY(this), 200 - SLIDER_WIDTH / 2, v, SLIDER_WIDTH / 2, height / 2, 256, 256);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this) + xOffset + SLIDER_WIDTH / 2, UtilitiesClient.getWidgetY(this) + height / 2, 200 - SLIDER_WIDTH / 2, v + 20 - height / 2, SLIDER_WIDTH / 2, height / 2, 256, 256);

		guiGraphics.text(client.font, getMessage().getString(), UtilitiesClient.getWidgetX(this) + width + TEXT_PADDING, UtilitiesClient.getWidgetY(this) + (height - TEXT_HEIGHT) / 2, ARGB_WHITE);

		if (markerFrequency > 0) {
			for (int i = 1; i <= maxValue / markerFrequency; i++) {
				final int xOffset1 = (width - SLIDER_WIDTH) * i * markerFrequency / maxValue;
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, UtilitiesClient.getWidgetX(this) + xOffset1 + SLIDER_WIDTH / 3, UtilitiesClient.getWidgetY(this) + height, 10, 68, 2, TICK_HEIGHT, 2, TICK_HEIGHT);
				guiGraphics.centeredText(client.font, String.valueOf(i * markerFrequency / markerDisplayedRatio), UtilitiesClient.getWidgetX(this) + xOffset1 + SLIDER_WIDTH / 2, UtilitiesClient.getWidgetY(this) + height + TICK_HEIGHT + 2, ARGB_WHITE);
			}
		}
	}

	private void checkShiftClick() {
		if (shiftClickAction != null && Minecraft.getInstance().hasShiftDown()) {
			shiftClickAction.accept(getIntValue());
		}
	}
}
