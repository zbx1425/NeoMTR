package mtr.data;

import com.mojang.serialization.Codec;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityClientSerializableMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public interface IPIDS extends IBlock {

	abstract class TileEntityPIDS extends BlockEntityClientSerializableMapper implements IPIDSRenderChild {

		private long cachedRefreshTime;
		private long cachedPlatformId;
		public final String[] messages = new String[getMaxArrivals() * getLinesPerArrival()];
		private final boolean[] hideArrival = new boolean[getMaxArrivals()];
		private final Set<Long> platformIds = new HashSet<>();
		private int displayPage = 0;
		private static final String KEY_MESSAGE = "message";
		private static final String KEY_HIDE_ARRIVAL = "hide_arrival";
		private static final String KEY_PLATFORM_IDS = "platform_ids";
		private static final String KEY_DISPLAY_PAGE = "display_page";

		public TileEntityPIDS(BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
		}

		@Override
		public void readCompoundTag(ValueInput compoundTag) {
			for (int i = 0; i < getMaxArrivals() * getLinesPerArrival(); i++) {
				messages[i] = compoundTag.getStringOr(KEY_MESSAGE + i, "");
			}
			for (int i = 0; i < getMaxArrivals(); i++) {
				hideArrival[i] = compoundTag.getBooleanOr(KEY_HIDE_ARRIVAL + i, false);
			}
			platformIds.clear();
			compoundTag.listOrEmpty(KEY_PLATFORM_IDS, Codec.LONG).forEach(platformIds::add);
			displayPage = compoundTag.getIntOr(KEY_DISPLAY_PAGE, 0);
		}

		@Override
		public void writeCompoundTag(ValueOutput compoundTag) {
			for (int i = 0; i < getMaxArrivals() * getLinesPerArrival(); i++) {
				compoundTag.putString(KEY_MESSAGE + i, messages[i] == null ? "" : messages[i]);
			}
			for (int i = 0; i < getMaxArrivals(); i++) {
				compoundTag.putBoolean(KEY_HIDE_ARRIVAL + i, hideArrival[i]);
			}
			ValueOutput.TypedOutputList<Long> platformIdOutputList = compoundTag.list(KEY_PLATFORM_IDS, Codec.LONG);
			platformIds.forEach(platformIdOutputList::add);
			compoundTag.putInt(KEY_DISPLAY_PAGE, displayPage);
		}

		public AABB getRenderBoundingBox() {
			return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		}

		public void setData(String[] messages, boolean[] hideArrival, Set<Long> platformIds, int displayPage) {
			System.arraycopy(messages, 0, this.messages, 0, Math.min(messages.length, this.messages.length));
			System.arraycopy(hideArrival, 0, this.hideArrival, 0, Math.min(hideArrival.length, this.hideArrival.length));
			this.platformIds.clear();
			this.platformIds.addAll(platformIds);
			this.displayPage = displayPage;
			setChanged();
			syncData();
		}

		public long getPlatformId(Set<Platform> platforms, DataCache dataCache) {
			if (dataCache.needsRefresh(cachedRefreshTime)) {
				cachedPlatformId = RailwayData.getClosePlatformId(platforms, dataCache, getBlockPos());
				cachedRefreshTime = System.currentTimeMillis();
			}
			return cachedPlatformId;
		}

		public int getDisplayPage() {
			return displayPage;
		}

		public Set<Long> getPlatformIds() {
			return platformIds;
		}

		public String getMessage(int index) {
			if (index >= 0 && index < getMaxArrivals() * getLinesPerArrival()) {
				if (messages[index] == null) {
					messages[index] = "";
				}
				return messages[index];
			} else {
				return "";
			}
		}

		public boolean getHideArrival(int index) {
			if (index >= 0 && index < getMaxArrivals()) {
				return hideArrival[index];
			} else {
				return false;
			}
		}

		public abstract int getMaxArrivals();

		public abstract int getLinesPerArrival();
	}
}
