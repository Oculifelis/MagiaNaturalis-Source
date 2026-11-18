package com.arcananaturae.item;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Quicksilver Stone (damage 1 of the original Alchemical Stone): strengthens the user's active effects, one glowstone
 * dust each. An effect below level III goes up one level at 30% of its remaining time; one already at III overloads
 * into nausea and poison. Without Permutatio discovered the stone only makes the user dizzy.
 * Fix for issue #40 (holding use crashed the client and kicked from servers): a single server-side use per click with
 * a cooldown, working on a copy of the effect list.
 */
public class QuicksilverStoneItem extends Item {
    private static final int COOLDOWN = 40;
    private static final int MAX_AMPLIFIER = 2;

    public QuicksilverStoneItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stone = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        player.getCooldowns().addCooldown(stone, COOLDOWN);
        if (!AspectPoolAccess.isDiscovered(player, Aspects.resolve(level, TTAspects.PERMUTATIO))) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 144, 1));
            return InteractionResult.SUCCESS;
        }
        List<MobEffectInstance> active = new ArrayList<>(player.getActiveEffects());
        boolean changed = false;
        for (MobEffectInstance effect : active) {
            if (effect.getEffect().equals(MobEffects.NAUSEA) || effect.isInfiniteDuration()) {
                continue;
            }
            if (!consumeGlowstone(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 144, 1));
                break;
            }
            changed = true;
            if (effect.getAmplifier() < MAX_AMPLIFIER) {
                player.removeEffect(effect.getEffect());
                player.addEffect(new MobEffectInstance(effect.getEffect(), Math.max(20, (int) (effect.getDuration() * 0.3F)),
                        effect.getAmplifier() + 1, effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 288, 2));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 144, 0));
            }
        }
        if (changed) {
            level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
        serverPlayer.inventoryMenu.broadcastChanges();
        return InteractionResult.SUCCESS;
    }

    private static boolean consumeGlowstone(Player player) {
        if (player.isCreative()) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.GLOWSTONE_DUST)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
