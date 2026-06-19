package mtr.screen;

import io.netty.buffer.Unpooled;
import mtr.MTR;
import mtr.RegistryClient;
import mtr.client.ClientData;
import mtr.data.IGui;
import mtr.data.NameColorDataBase;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.packet.IPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.FriendlyByteBuf;

public class RailActionsScreen extends ScreenMapper implements IGui, IPacket {

	final DashboardList railActionsList;

	public RailActionsScreen() {
		super(Text.literal(""));
		railActionsList = new DashboardList(null, null, null, null, null, this::onDelete, null, () -> "", text -> {
		});

	}

	@Override
	protected void init() {
		super.init();
		railActionsList.x = SQUARE_SIZE;
		railActionsList.y = SQUARE_SIZE * 2;
		railActionsList.width = width - SQUARE_SIZE * 2;
		railActionsList.height = height - SQUARE_SIZE * 2;
		railActionsList.init(this::addDrawableChild);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(guiGraphics, mouseX, mouseY, delta);
		try {
			railActionsList.render(guiGraphics, font);
			guiGraphics.centeredText(font, Text.translatable("gui.mtr.rail_actions"), width / 2, SQUARE_SIZE + TEXT_PADDING, ARGB_WHITE);
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		railActionsList.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double amount) {
		railActionsList.mouseScrolled(mouseX, mouseY, scrollX, amount);
		return super.mouseScrolled(mouseX, mouseY, scrollX, amount);
	}

	@Override
	public void tick() {
		railActionsList.tick();
		railActionsList.setData(ClientData.RAIL_ACTIONS, false, false, false, false, false, true);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void onDelete(NameColorDataBase data, int index) {
		final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
		packet.writeLong(data.id);
		RegistryClient.sendToServer(PACKET_REMOVE_RAIL_ACTION, packet);
	}
}
