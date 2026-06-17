package mtr.item;

import mtr.CreativeModeTabs;
import mtr.mappings.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Function;

public abstract class ItemBlockClickingBase extends ItemWithCreativeTabBase {

	public static final String TAG_POS = "pos";

	public ItemBlockClickingBase(CreativeModeTabs.Wrapper creativeModeTab, Function<Properties, Properties> propertiesConsumer) {
		super(creativeModeTab, propertiesConsumer);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!context.getLevel().isClientSide()) {
			if (clickCondition(context)) {
				final CustomData customData = context.getItemInHand()
						.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
				CompoundTag compoundTag = customData.copyTag();
				if (compoundTag.contains(TAG_POS)) {
					final BlockPos posEnd = BlockPos.of(compoundTag.getLong(TAG_POS));
					onEndClick(context, posEnd, compoundTag);
					compoundTag.remove(TAG_POS);
				} else {
					compoundTag.putLong(TAG_POS, context.getClickedPos().asLong());
					onStartClick(context, compoundTag);
				}
				context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(compoundTag));
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			return super.useOn(context);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
		final CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		final CompoundTag compoundTag = customData.copyTag();
		final long posLong = compoundTag.getLong(TAG_POS);
		if (posLong != 0) {
			tooltip.add(Text.translatable("tooltip.mtr.selected_block", BlockPos.of(posLong).toShortString()).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
		}
	}

	protected abstract void onStartClick(UseOnContext context, CompoundTag compoundTag);

	protected abstract void onEndClick(UseOnContext context, BlockPos posEnd, CompoundTag compoundTag);

	protected abstract boolean clickCondition(UseOnContext context);
}
