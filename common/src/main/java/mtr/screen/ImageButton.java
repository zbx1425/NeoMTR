package mtr.screen;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImageButton extends Button {
    protected final Identifier resourceLocation;
    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;
    protected final int textureWidth;
    protected final int textureHeight;

    public ImageButton(int var1, int var2, int var3, int var4, int var5, int var6, Identifier var7, Button.OnPress var8) {
        this(var1, var2, var3, var4, var5, var6, var4, var7, 256, 256, var8);
    }

    public ImageButton(int var1, int var2, int var3, int var4, int var5, int var6, int var7, Identifier var8, Button.OnPress var9) {
        this(var1, var2, var3, var4, var5, var6, var7, var8, 256, 256, var9);
    }

    public ImageButton(int var1, int var2, int var3, int var4, int var5, int var6, int var7, Identifier var8, int var9, int var10, Button.OnPress var11) {
        this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, CommonComponents.EMPTY);
    }

    public ImageButton(
            int var1, int var2, int var3, int var4, int var5, int var6, int var7, Identifier var8, int var9, int var10, Button.OnPress var11, Component var12
    ) {
        super(var1, var2, var3, var4, var12, var11, DEFAULT_NARRATION);
        this.textureWidth = var9;
        this.textureHeight = var10;
        this.xTexStart = var5;
        this.yTexStart = var6;
        this.yDiffTex = var7;
        this.resourceLocation = var8;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor var1, int var2, int var3, float var4) {
        this.renderTexture(
                var1,
                this.resourceLocation,
                this.getX(),
                this.getY(),
                this.xTexStart,
                this.yTexStart,
                this.yDiffTex,
                this.width,
                this.height,
                this.textureWidth,
                this.textureHeight
        );
    }

    public void renderTexture(GuiGraphicsExtractor var1, Identifier var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11) {
        int var12 = var6;
        if (!this.isActive()) {
            var12 = var6 + var7 * 2;
        } else if (this.isHoveredOrFocused()) {
            var12 = var6 + var7;
        }

        RenderSystem.enableDepthTest();
        var1.blit(var2, var3, var4, (float)var5, (float)var12, var8, var9, var10, var11);
    }
}