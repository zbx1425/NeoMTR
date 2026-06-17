package mtr.screen;

import mtr.MTR;
import mtr.client.ClientData;
import mtr.data.DataConverter;
import mtr.data.IGui;
import mtr.data.LiftClient;
import mtr.data.NameColorDataBase;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.render.RenderTrains;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class LiftSelectionScreen extends ScreenMapper implements IGui {

	private final DashboardList selectionList;
	private final List<BlockPos> floorLevels = new ArrayList<>();
	private final List<String> floorDescriptions = new ArrayList<>();
	private final List<Boolean> floorCallEnabled = new ArrayList<>();
	private final LiftClient lift;

	public LiftSelectionScreen(LiftClient lift) {
		super(Text.literal(""));
		this.lift = lift;
		lift.iterateFloors(floor -> {
			floorLevels.add(floor);
			floorDescriptions.add(IGui.formatStationName(String.join("|", ClientData.DATA_CACHE.requestLiftFloorText(floor))));
			floorCallEnabled.add(!ClientData.DATA_CACHE.requestLiftFloorDisableCarCall(floor));
		});
		selectionList = new DashboardList(this::onPress, null, null, null, null, null, null, () -> "", text -> {
		});
	}

	@Override
	protected void init() {
		super.init();
		selectionList.x = width / 2 - PANEL_WIDTH;
		selectionList.y = SQUARE_SIZE;
		selectionList.width = PANEL_WIDTH * 2;
		selectionList.height = height - SQUARE_SIZE * 2;
		selectionList.init(this::addDrawableChild);
	}

	@Override
	public void tick() {
		selectionList.tick();
		final List<NameColorDataBase> list = new ArrayList<>();
		for (int i = floorLevels.size() - 1; i >= 0; i--) {
			if (floorCallEnabled.get(i)) {
				list.add(new DataConverter(floorDescriptions.get(i),
						lift.liftInstructions.containsInstruction(floorLevels.get(i).getY()) ? RenderTrains.LIFT_LIGHT_COLOR : ARGB_BLACK));
			} else {
				list.add(new DataConverter(DashboardList.ID_DISABLED, floorDescriptions.get(i), 0xFF444444));
			}
		}
		selectionList.setData(list, true, false, false, false, false, false);
	}

	@Override
	public void renderBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.renderBackground(guiGraphics, mouseX, mouseY, delta);
		try {
			selectionList.render(guiGraphics, font);
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
		guiGraphics.pose().translate(0, 0, 100);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		selectionList.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double amount) {
		selectionList.mouseScrolled(mouseX, mouseY, scrollX, amount);
		return super.mouseScrolled(mouseX, mouseY, scrollX, amount);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void onPress(NameColorDataBase data, int index) {
		if (lift != null) {
			if (!floorCallEnabled.get(floorLevels.size() - index - 1)) return;
			PacketTrainDataGuiClient.sendPressLiftButtonC2S(lift.id, floorLevels.get(floorLevels.size() - index - 1).getY());
		}
		onClose();
	}
}
