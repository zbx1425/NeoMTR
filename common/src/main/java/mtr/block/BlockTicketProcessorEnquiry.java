package mtr.block;

import mtr.SoundEvents;
import mtr.data.TicketSystem;
import mtr.mappings.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockTicketProcessorEnquiry extends BlockTicketProcessor {

	public BlockTicketProcessorEnquiry(BlockBehaviour.Properties properties) {
		super(properties, false, false, false);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult blockHitResult) {
		if (!world.isClientSide()) {
			final int playerScore = TicketSystem.getPlayerScore(world, player, TicketSystem.BALANCE_OBJECTIVE).get();
			player.sendOverlayMessage(Text.translatable("gui.mtr.balance", String.valueOf(playerScore)));
			world.playSound(null, pos, SoundEvents.TICKET_PROCESSOR_ENTRY, SoundSource.BLOCKS, 1, 1);
		}
		return InteractionResult.SUCCESS;
	}
}
