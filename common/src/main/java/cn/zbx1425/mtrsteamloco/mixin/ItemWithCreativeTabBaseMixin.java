package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.gui.RailEditorGeometryScreen;
import cn.zbx1425.mtrsteamloco.gui.RailEditorVisualScreen;
import cn.zbx1425.mtrsteamloco.network.PacketScreen;
import mtr.item.ItemWithCreativeTabBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemWithCreativeTabBase.class)
public abstract class ItemWithCreativeTabBaseMixin extends Item {

    public ItemWithCreativeTabBaseMixin(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (((Item)(Object)this) == Main.RAIL_EDITOR_VISUAL.get()) {
            Level level = context.getLevel();
            BlockState blockState = level.getBlockState(context.getClickedPos());
            if (blockState.getBlock() instanceof mtr.block.BlockNode) {
                if (context.isSecondaryUseActive()) {
                    if (level.isClientSide()) {
                        RailEditorVisualScreen.acquirePickInfoWhenUse();
                        return super.useOn(context);
                    } else {
                        PacketScreen.sendScreenBlockS2C((ServerPlayer) context.getPlayer(), "rail_editor_visual", BlockPos.ZERO);
                    }
                } else {
                    if (level.isClientSide()) {
                        RailEditorVisualScreen.acquirePickInfoWhenUse();
                        CompoundTag toolTag = context.getItemInHand().getOrDefault(Main.TOOL_TAG.get(), new CompoundTag()).copy();
                        RailEditorVisualScreen.batchApplyBrushTemplate(toolTag);
                    } else {
                        return super.useOn(context);
                    }
                }
                return InteractionResult.SUCCESS;
            } else if (context.isSecondaryUseActive() && level.isClientSide()) {
                if (RailEditorVisualScreen.hasValidLastPick()) {
                    RailEditorVisualScreen.openLastPickedScreen();
                }
                return InteractionResult.SUCCESS;
            } else {
                return super.useOn(context);
            }
        } else if (((Item)(Object)this) == Main.RAIL_EDITOR_GEOMETRY.get()) {
            Level level = context.getLevel();
            BlockState blockState = level.getBlockState(context.getClickedPos());
            if (blockState.getBlock() instanceof mtr.block.BlockNode) {
                if (context.isSecondaryUseActive()) {
                    if (level.isClientSide()) {
                        RailEditorGeometryScreen.acquirePickInfoWhenUse();
                        return super.useOn(context);
                    } else {
                        PacketScreen.sendScreenBlockS2C((ServerPlayer) context.getPlayer(), "rail_editor_geometry", BlockPos.ZERO);
                    }
                } else {
                    if (level.isClientSide()) {
                        RailEditorGeometryScreen.acquirePickInfoWhenUse();
                        CompoundTag toolTag = context.getPlayer().getMainHandItem().getOrDefault(Main.TOOL_TAG.get(), new CompoundTag()).copy();
                        RailEditorGeometryScreen.batchApply(toolTag);
                    } else {
                        return super.useOn(context);
                    }
                }
                return InteractionResult.SUCCESS;
            } else {
                return super.useOn(context);
            }
        } else {
            return super.useOn(context);
        }
    }
}