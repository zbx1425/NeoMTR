package mtr.screen;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class Checkbox extends AbstractButton {
    private static final Identifier CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final Identifier CHECKBOX_SELECTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected");
    private static final Identifier CHECKBOX_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_highlighted");
    private static final Identifier CHECKBOX_SPRITE = Identifier.withDefaultNamespace("widget/checkbox");
    private static final int TEXT_COLOR = 14737632;
    private boolean selected;

    public Checkbox(int var1, int var2, int var3, int var4, Component var5, boolean var6) {
        super(var1, var2, var3, var4, var5);
        this.selected = var6;
    }

    @Override
    public void onPress() {
        this.selected = !this.selected;
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput var1) {
        var1.add(NarratedElementType.TITLE, (Component)this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                var1.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.checkbox.usage.focused"));
            } else {
                var1.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.checkbox.usage.hovered"));
            }
        }

    }

    private static int boxSize(Font var0) {
        return 9 + 8;
    }


    @Override
    public void renderWidget(GuiGraphicsExtractor var1, int var2, int var3, float var4) {
        Minecraft var5 = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        Font var6 = var5.font;
        var1.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        Identifier var7;
        if (this.selected) {
            var7 = this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
        } else {
            var7 = this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
        }

        int var8 = boxSize(var6);
        int var9 = this.getX() + var8 + 4;
        int var10 = this.getY() + (this.height >> 1) - (9 >> 1);
        var1.blitSprite(var7, this.getX(), this.getY(), var8, var8);
        var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
//        var1.drawString(var6, this.getMessage(), var9, var10, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
    }
}