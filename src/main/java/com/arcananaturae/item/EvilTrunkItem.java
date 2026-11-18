package com.arcananaturae.item;

import com.arcananaturae.MNRegistry;
import com.arcananaturae.entity.EvilTrunk;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** A packed Evil Trunk. Use it on a block to let the trunk out; it keeps whatever was inside when it was packed. */
public class EvilTrunkItem extends Item {
    private final EvilTrunk.TrunkVariant variant;

    public EvilTrunkItem(EvilTrunk.TrunkVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EvilTrunk trunk = MNRegistry.EVIL_TRUNK.get().spawn(level, stack, player, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
        if (trunk == null) {
            return InteractionResult.FAIL;
        }
        trunk.unpack(player, stack, variant);
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null && contents.nonEmptyItems().iterator().hasNext()) {
            tooltip.accept(Component.translatable("tooltip.arcananaturae.trunk_full").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.arcananaturae.trunk_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
