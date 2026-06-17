package mtr.mappings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class HorizontalBlockWithSoftLanding extends BlockDirectionalMapper {

	public HorizontalBlockWithSoftLanding(Properties settings) {
		super(settings);
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		super.fallOn(level, state, pos, entity, fallDistance * (softenLanding() ? 0.5F : 1));
	}

	public boolean softenLanding() {
		return false;
	}
}
