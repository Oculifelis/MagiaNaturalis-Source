package com.arcananaturae.item;

import com.leclowndu93150.thaumaturge.content.warding.ArcaneAccess;
import com.arcananaturae.MNRegistry;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Thaumium keys, working on every Thaumaturge arcane lock (arcane doors, pressure plates, our arcane chests).
 * <ul>
 * <li>Key of Unraveling: forged by its crafter, bound to a player by hitting them (or sneak-using it yourself). The
 * bound player uses it on any lock the forger owns to gain access. The original compared the bound player with
 * itself, so anyone holding the key got in; now only the bound player can use it.</li>
 * <li>Key of Endorsing: hit players to put them on its list, then use it on a lock you own (or hold a gold key's
 * rights to) to give everyone on the list access.</li>
 * </ul>
 * Neither key is used up.
 */
public class ThaumiumKeyItem extends Item {
    private final boolean endorsing;

    public ThaumiumKeyItem(Item.Properties properties, boolean endorsing) {
        super(properties);
        this.endorsing = endorsing;
    }

    private static KeyPlayers players(ItemStack stack) {
        return stack.getOrDefault(MNRegistry.KEY_PLAYERS.get(), KeyPlayers.EMPTY);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        if (players(stack).forger().isEmpty()) {
            stack.set(MNRegistry.KEY_PLAYERS.get(), players(stack).withForger(player));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return players(stack).forger().isPresent() || super.isFoil(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            KeyPlayers players = players(stack);
            if (players.forger().isEmpty()) {
                stack.set(MNRegistry.KEY_PLAYERS.get(), players.withForger(player));
                message(player, "forged", player.getGameProfile().name());
            } else if (!endorsing && players.bound().isEmpty()) {
                stack.set(MNRegistry.KEY_PLAYERS.get(), players.withBound(player));
                message(player, "bound", player.getGameProfile().name());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Hitting a player with a key binds them (Unraveling) or adds them to the list (Endorsing) instead of hurting. */
    public static void onAttack(AttackEntityEvent event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (!(stack.getItem() instanceof ThaumiumKeyItem key) || !(event.getTarget() instanceof Player target)) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        KeyPlayers players = players(stack);
        if (key.endorsing) {
            stack.set(MNRegistry.KEY_PLAYERS.get(), players.withEndorsed(target));
            message(event.getEntity(), "endorsed", target.getGameProfile().name());
        } else {
            stack.set(MNRegistry.KEY_PLAYERS.get(), players.withBound(target));
            message(event.getEntity(), "bound", target.getGameProfile().name());
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (player == null || !ArcaneAccess.isLock(state)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos origin = ArcaneAccess.lockOrigin(state, context.getClickedPos());
        KeyPlayers players = players(stack);
        if (endorsing) {
            if (!ArcaneAccess.canBind(serverLevel, origin, player, false)) {
                message(player, "no_rights");
                return InteractionResult.FAIL;
            }
            int added = 0;
            for (KeyPlayers.Named named : players.endorsed()) {
                if (ArcaneAccess.grantAccess(serverLevel, origin, named.id(), false)) {
                    added++;
                }
            }
            message(player, "endorsed_lock", added);
            return InteractionResult.SUCCESS;
        }
        if (players.forger().isEmpty() || players.bound().isEmpty()) {
            message(player, "unfinished");
            return InteractionResult.FAIL;
        }
        UUID bound = players.bound().get().id();
        if (!player.getUUID().equals(bound)) {
            message(player, "not_bound", players.bound().get().name());
            return InteractionResult.FAIL;
        }
        if (!players.forger().get().id().equals(ArcaneAccess.owner(serverLevel, origin))) {
            message(player, "wrong_owner", players.forger().get().name());
            return InteractionResult.FAIL;
        }
        if (ArcaneAccess.canAccess(serverLevel, origin, player)) {
            message(player, "already");
            return InteractionResult.SUCCESS;
        }
        ArcaneAccess.grantAccess(serverLevel, origin, bound, false);
        message(player, "granted");
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        String base = "item.arcananaturae." + (endorsing ? "endorsing_key" : "unraveling_key");
        builder.accept(Component.translatable(base + ".desc").withStyle(ChatFormatting.DARK_PURPLE));
        KeyPlayers players = players(stack);
        players.forger().ifPresent(f -> builder.accept(Component.translatable("tooltip.arcananaturae.key.forger", f.name()).withStyle(ChatFormatting.GRAY)));
        players.bound().ifPresent(b -> builder.accept(Component.translatable("tooltip.arcananaturae.key.bound", b.name()).withStyle(ChatFormatting.GRAY)));
        for (KeyPlayers.Named named : players.endorsed()) {
            builder.accept(Component.literal(" - " + named.name()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void message(Player player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.arcananaturae.key." + key, args).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
