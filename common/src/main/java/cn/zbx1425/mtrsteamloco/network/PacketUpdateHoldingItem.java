package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PacketUpdateHoldingItem {

    public static Identifier PACKET_UPDATE_HOLDING_ITEM = Main.id("update_holding_item");

    public static void sendUpdateC2S() {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        assert Minecraft.getInstance().player != null;
        assert Minecraft.getInstance().level != null;

        Tag tag = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, Minecraft.getInstance().player.getMainHandItem()).getOrThrow();
        packet.writeNbt(tag);
        RegistryClient.sendToServer(PACKET_UPDATE_HOLDING_ITEM, packet);
    }

    private static boolean isAllowedItem(Holder<Item> item) {
        return item.is(resourceKey -> resourceKey.identifier().getNamespace().equals(Main.MOD_ID));
    }

    public static void receiveUpdateC2S(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
        CompoundTag itemTag = packet.readNbt();
        ItemStack stack = ItemStack.CODEC.parse(NbtOps.INSTANCE, itemTag).getOrThrow();
        if (!stack.is(PacketUpdateHoldingItem::isAllowedItem)) return;
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
    }

}
