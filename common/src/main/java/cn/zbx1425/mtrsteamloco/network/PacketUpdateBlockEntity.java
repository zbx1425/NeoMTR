package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class PacketUpdateBlockEntity {

    public static Identifier PACKET_UPDATE_BLOCK_ENTITY = Main.id("update_block_entity");

    public static void sendUpdateC2S(BlockEntityMapper blockEntity) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeIdentifier(level.dimension().identifier());
        packet.writeBlockPos(blockEntity.getBlockPos());
        packet.writeVarInt(BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()));
        CompoundTag tag = new CompoundTag();
        blockEntity.writeCompoundTag(tag);
        packet.writeNbt(tag);

        RegistryClient.sendToServer(PACKET_UPDATE_BLOCK_ENTITY, packet);
    }

    public static void receiveUpdateC2S(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
        ResourceKey<Level> levelKey = packet.readResourceKey(net.minecraft.core.registries.Registries.DIMENSION);
        BlockPos blockPos = packet.readBlockPos();
        BlockEntityType<?> blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.byId(packet.readVarInt());

        CompoundTag compoundTag = packet.readNbt();

        server.execute(() -> {
            ServerLevel level = server.getLevel(levelKey);
            if (level == null || blockEntityType == null) return;
            level.getBlockEntity(blockPos, blockEntityType).ifPresent(blockEntity -> {
                if (compoundTag != null) {
                    blockEntity.loadCustomOnly(compoundTag, server.overworld().registryAccess());
                    blockEntity.setChanged();
                    level.getChunkSource().blockChanged(blockPos);
                }
            });
        });
    }
}
