package mtr.screen.base;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class MTRScreen extends Screen {

	protected MTRScreen(Component title) {
		super(title);
	}
}
