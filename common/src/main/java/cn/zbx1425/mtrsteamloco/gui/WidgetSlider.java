package cn.zbx1425.mtrsteamloco.gui;

import mtr.data.IGui;
import mtr.mappings.Text;
import net.minecraft.client.gui.components.AbstractSliderButton;

import java.util.function.Function;

public class WidgetSlider extends AbstractSliderButton implements IGui {

    private final int maxValue;
    private final Function<Integer, String> setMessage;

    private static final int SLIDER_WIDTH = 10;

    public WidgetSlider(int maxValue, int value, Function<Integer, String> setMessage) {
        super(0, 0, 0, 20, Text.literal(""), 0);
        this.maxValue = maxValue;
        this.setMessage = setMessage;
        this.setValue(value);
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal(setMessage.apply(getIntValue())));
    }

    @Override
    protected void applyValue() {
    }

    public void setValue(int valueInt) {
        value = (double) valueInt / maxValue;
        updateMessage();
    }

    public int getIntValue() {
        return (int) Math.round(value * maxValue);
    }
}