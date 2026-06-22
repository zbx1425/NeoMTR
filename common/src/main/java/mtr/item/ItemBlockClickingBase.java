package mtr.item;

import mtr.CreativeModeTabs;
import mtr.DataComponentTypes;
import mtr.mappings.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ItemBlockClickingBase extends ItemWithCreativeTabBase {

	public ItemBlockClickingBase(Item.Properties properties, CreativeModeTabs.Wrapper creativeModeTab, Function<Properties, Properties> propertiesConsumer) {
		super(properties, creativeModeTab, propertiesConsumer);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!context.getLevel().isClientSide()) {
			if (clickCondition(context)) {

				final BlockPos pos = context.getItemInHand().get(DataComponentTypes.START_POS.get());
				if (pos != null) {
					onEndClick(context, pos);
					context.getItemInHand().remove(DataComponentTypes.START_POS.get());
				} else {
					context.getItemInHand().set(DataComponentTypes.START_POS.get(), context.getClickedPos());
					onStartClick(context);
				}
				return InteractionResult.SUCCESS_SERVER;
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			return super.useOn(context);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
		final BlockPos pos = stack.get(DataComponentTypes.START_POS.get());
		if (pos != null) {
			tooltipAdder.accept(Text.translatable("tooltip.mtr.selected_block", pos.toShortString()).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
		}
	}

	protected abstract void onStartClick(UseOnContext context);

	protected abstract void onEndClick(UseOnContext context, BlockPos posEnd);

	protected abstract boolean clickCondition(UseOnContext context);
}
