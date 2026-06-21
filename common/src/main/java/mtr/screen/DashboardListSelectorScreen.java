package mtr.screen;

import mtr.MTR;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.data.NameColorDataBase;
import mtr.screen.base.MTRScreen;
import mtr.mappings.Text;
import mtr.util.UtilitiesClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DashboardListSelectorScreen extends MTRScreen implements IGui {

	private final DashboardList availableList;
	private final DashboardList selectedList;
	private final Button buttonDone;

	private final MTRScreen previousScreen;
	private final Runnable onClose;
	private final List<NameColorDataBase> allData;
	private final Collection<Long> selectedIds;
	private final boolean isSingleSelect;
	private final boolean canRepeat;

	public DashboardListSelectorScreen(Runnable onClose, List<NameColorDataBase> allData, Collection<Long> selectedIds, boolean isSingleSelect, boolean canRepeat) {
		this(null, onClose, allData, selectedIds, isSingleSelect, canRepeat);
	}

	public DashboardListSelectorScreen(MTRScreen previousScreen, List<NameColorDataBase> allData, Collection<Long> selectedIds, boolean isSingleSelect, boolean canRepeat) {
		this(previousScreen, null, allData, selectedIds, isSingleSelect, canRepeat);
	}

	private DashboardListSelectorScreen(MTRScreen previousScreen, Runnable onClose, List<NameColorDataBase> allData, Collection<Long> selectedIds, boolean isSingleSelect, boolean canRepeat) {
		super(Text.literal(""));
		this.previousScreen = previousScreen;
		this.onClose = onClose;
		this.allData = allData;
		this.selectedIds = selectedIds;
		this.isSingleSelect = isSingleSelect;
		this.canRepeat = canRepeat;

		availableList = new DashboardList(null, null, null, null, this::onAdd, null, null, () -> ClientData.ROUTES_PLATFORMS_SEARCH, text -> ClientData.ROUTES_PLATFORMS_SEARCH = text);
		selectedList = new DashboardList(null, null, null, this::updateList, null, this::onDelete, () -> selectedIds instanceof ArrayList ? (List<Long>) selectedIds : new ArrayList<>(), () -> ClientData.ROUTES_PLATFORMS_SELECTED_SEARCH, text -> ClientData.ROUTES_PLATFORMS_SELECTED_SEARCH = text);
		buttonDone = UtilitiesClient.newButton(Text.translatable("gui.done"), button -> onClose());
	}

	@Override
	protected void init() {
		super.init();
		availableList.x = width / 2 - PANEL_WIDTH - SQUARE_SIZE;
		selectedList.x = width / 2 + SQUARE_SIZE;
		availableList.y = selectedList.y = SQUARE_SIZE * 2;
		availableList.height = selectedList.height = height - SQUARE_SIZE * 5;
		availableList.width = selectedList.width = PANEL_WIDTH;
		availableList.init(this::addRenderableWidget);
		selectedList.init(this::addRenderableWidget);
		IDrawing.setPositionAndWidth(buttonDone, (width - PANEL_WIDTH) / 2, height - SQUARE_SIZE * 2, PANEL_WIDTH);
		addRenderableWidget(buttonDone);
		updateList();
	}

	@Override
	public void tick() {
		availableList.tick();
		selectedList.tick();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(guiGraphics, mouseX, mouseY, delta);
		try {
			availableList.render(guiGraphics, font);
			selectedList.render(guiGraphics, font);
			guiGraphics.centeredText(font, Text.translatable("gui.mtr.available"), width / 2 - PANEL_WIDTH / 2 - SQUARE_SIZE, SQUARE_SIZE + TEXT_PADDING, ARGB_WHITE);
			guiGraphics.centeredText(font, Text.translatable("gui.mtr.selected"), width / 2 + PANEL_WIDTH / 2 + SQUARE_SIZE, SQUARE_SIZE + TEXT_PADDING, ARGB_WHITE);
		} catch (Exception e) {
			MTR.LOGGER.error("", e);
		}
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		availableList.mouseMoved(mouseX, mouseY);
		selectedList.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		availableList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		selectedList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void onClose() {
		super.onClose();
		if (onClose != null) {
			onClose.run();
		}
		if (previousScreen != null) {
			UtilitiesClient.setScreen(minecraft, previousScreen);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void updateList() {
		final List<NameColorDataBase> availableData = new ArrayList<>();
		for (final NameColorDataBase data : allData) {
			if (canRepeat || !selectedIds.contains(data.id)) {
				availableData.add(data);
			}
		}

		final List<NameColorDataBase> selectedData = new ArrayList<>();
		for (final long selectedId : selectedIds) {
			allData.stream().filter(data -> data.id == selectedId).findFirst().ifPresent(selectedData::add);
		}

		availableList.setData(availableData, false, false, false, false, true, false);
		selectedList.setData(selectedData, false, false, false, canRepeat, false, true);
	}

	private void onAdd(NameColorDataBase data, int index) {
		if (isSingleSelect) {
			selectedIds.clear();
		}
		selectedIds.add(data.id);
		updateList();
	}

	private void onDelete(NameColorDataBase data, int index) {
		if (canRepeat && selectedIds instanceof ArrayList) {
			((ArrayList<Long>) selectedIds).remove(index);
		} else {
			selectedIds.remove(data.id);
		}
		updateList();
	}
}
