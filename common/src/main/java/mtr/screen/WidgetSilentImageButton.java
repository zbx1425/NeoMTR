package mtr.screen;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;

public class WidgetSilentImageButton extends ImageButton {

	private final boolean playSound;

	public WidgetSilentImageButton(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier texture, int textureWidth, int textureHeight, OnPress onPress, boolean playSound) {
		super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, onPress);
		this.playSound = playSound;
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
		if (playSound) {
			super.playDownSound(soundManager);
		}
	}
}
