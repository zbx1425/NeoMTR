package mtr.util;

import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Function;

public interface Utilities {

	static void incrementYaw(Entity entity, float yaw) {
		entity.setYRot(entity.getYRot() + yaw);
	}

	static boolean isHolding(Player player, Function<Item, Boolean> predicate) {
		return player != null && player.isHolding(itemStack -> predicate.apply(itemStack.getItem()));
	}

	static InputStream getInputStream(Optional<Resource> optionalResource) throws IOException {
		if (optionalResource.isPresent()) {
			return optionalResource.get().open();
		} else {
			return IOUtils.toInputStream("", Charset.defaultCharset());
		}
	}

	@FunctionalInterface
	interface TileEntitySupplier<T extends BlockEntityMapper> {
		T supplier(BlockPos pos, BlockState state);
	}
}
