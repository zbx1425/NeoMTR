package mtr.item;

import mtr.CreativeModeTabs;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ItemResourcePackCreator extends ItemWithCreativeTabBase {

	public ItemResourcePackCreator() {
		super(CreativeModeTabs.CORE);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		if (!world.isClientSide()) {
			PacketTrainDataGuiServer.openResourcePackCreatorScreenS2C((ServerPlayer) user);
		}
		return super.use(world, user, hand);
	}
}
