package com.arcananaturae.item;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import com.arcananaturae.MNRegistry;
import com.arcananaturae.table.DeconTables;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Research Log: stores research points (aspects) taken from Deconstruction Tables, up to 64 of each aspect.
 * Sneak-use on a Deconstruction Table to file its result; use to pour everything into your aspect pool.
 * The Transcribing Table fills logs by itself.
 */
public class ResearchLogItem extends Item {
    public static final int CAP = 64;

    public ResearchLogItem(Item.Properties properties) {
        super(properties);
    }

    public static AspectList points(ItemStack stack) {
        return stack.getOrDefault(MNRegistry.RESEARCH_POINTS.get(), AspectList.EMPTY);
    }

    public static boolean canAdd(ItemStack stack, Holder<IAspect> aspect) {
        return points(stack).amountOf(aspect) < CAP;
    }

    public static void add(ItemStack stack, Holder<IAspect> aspect) {
        stack.set(MNRegistry.RESEARCH_POINTS.get(), points(stack).add(aspect, 1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        Holder<IAspect> aspect = DeconTables.take(level, context.getClickedPos(), a -> canAdd(stack, a));
        if (aspect == null) {
            return InteractionResult.PASS;
        }
        add(stack, aspect);
        level.playSound(null, context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        AspectList points = points(stack);
        if (points.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AspectPoolAccess.grantAll(serverPlayer, points);
            stack.remove(MNRegistry.RESEARCH_POINTS.get());
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
            player.sendOverlayMessage(Component.translatable("message.arcananaturae.research_log_read", points.totalAmount()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        AspectList points = points(stack);
        if (points.isEmpty()) {
            builder.accept(Component.translatable("tooltip.arcananaturae.research_log_empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (AspectInstance entry : points.sortedByAmount()) {
            String key = entry.aspect().unwrapKey().map(k -> "aspect." + k.identifier().getNamespace() + "." + k.identifier().getPath()).orElse("?");
            builder.accept(Component.literal(entry.amount() + "x ").append(Component.translatable(key))
                    .withColor(entry.aspect().value().color()));
        }
    }
}
