package mtr.mappings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntityMapper extends BlockEntity {

	public BlockEntityMapper(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final void loadAdditional(ValueInput valueInput) {
		super.loadAdditional(valueInput);
		readCompoundTag(valueInput);
	}

	@Override
	public final void saveAdditional(ValueOutput valueOutput) {
		super.saveAdditional(valueOutput);
		writeCompoundTag(valueOutput);
	}

	public void readCompoundTag(ValueInput valueInput) {
	}

	public void writeCompoundTag(ValueOutput valueOutput) {
	}
}
