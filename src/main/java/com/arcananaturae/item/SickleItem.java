package com.arcananaturae.item;

import com.arcananaturae.MNRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.common.IShearable;
import org.jspecify.annotations.Nullable;

/**
 * Thaumium and Void Sickles: cut one plant and the touching plants of the same kind fall with it.
 * Sneak to cut a single block. Fixes over the original (see docs/BUGS.md): the reap starts from the block state
 * captured before the break (the original read the already-broken air and often reaped nothing, issue #31), every
 * extra block goes through the player's normal break (protection, drops, events, break particles), a visited set and
 * a hard cap stop runaway reaps, and the tool breaks through hurtAndBreak, so the break sound and animation play (#29).
 */
public class SickleItem extends Item {
    /** Guards the re-entry: each extra block is broken through the game mode, which calls mineBlock again. */
    private static final ThreadLocal<Boolean> REAPING = ThreadLocal.withInitial(() -> false);
    private static final int REPAIR_INTERVAL = 20;

    private final boolean voidMetal;

    public SickleItem(Item.Properties properties, boolean voidMetal) {
        super(properties);
        this.voidMetal = voidMetal;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!state.is(MNRegistry.SICKLE_MINEABLE)) {
            return super.mineBlock(stack, level, state, pos, owner);
        }
        // One durability per plant, instant-break ones too (vanilla tools skip those, which would make a reap free).
        boolean result = true;
        if (!level.isClientSide()) {
            stack.hurtAndBreak(1, owner, EquipmentSlot.MAINHAND);
        }
        if (REAPING.get() || !(owner instanceof ServerPlayer player) || player.isShiftKeyDown()) {
            return result;
        }
        SickleReach reach = stack.get(MNRegistry.SICKLE_REACH.get());
        if (reach == null || reach.radius() <= 0) {
            return result;
        }
        List<BlockPos> targets = plotReap((ServerLevel) level, player, pos, state, reach);
        REAPING.set(true);
        try {
            for (BlockPos target : targets) {
                if (stack.isEmpty() || player.getMainHandItem() != stack) {
                    break;
                }
                BlockState targetState = level.getBlockState(target);
                if (player.gameMode.destroyBlock(target)) {
                    level.levelEvent(2001, target, Block.getId(targetState));
                }
            }
        } finally {
            REAPING.set(false);
        }
        return result;
    }

    /** Breadth-first walk over touching blocks (sides and diagonals) of the cut block's kind, nearest first. */
    private static List<BlockPos> plotReap(ServerLevel level, Player player, BlockPos origin, BlockState originState, SickleReach reach) {
        boolean ripeOnly = originState.getBlock() instanceof CropBlock crop && crop.isMaxAge(originState);
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(origin);
        ArrayDeque<BlockPos> layer = new ArrayDeque<>();
        layer.add(origin);
        for (int step = 0; step < reach.radius() && !layer.isEmpty(); step++) {
            ArrayDeque<BlockPos> next = new ArrayDeque<>();
            for (BlockPos from : layer) {
                for (BlockPos to : BlockPos.betweenClosed(from.offset(-1, -1, -1), from.offset(1, 1, 1))) {
                    if (found.size() >= reach.maxBlocks()) {
                        return found;
                    }
                    BlockPos p = to.immutable();
                    if (!seen.add(p) || !level.mayInteract(player, p)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() != originState.getBlock() || s.getDestroySpeed(level, p) < 0) {
                        continue;
                    }
                    if (ripeOnly && !((CropBlock) s.getBlock()).isMaxAge(s)) {
                        continue;
                    }
                    found.add(p);
                    next.add(p);
                }
            }
            layer = next;
        }
        return found;
    }

    /** Shears sheep, mooshrooms and other IShearable mobs, as the original did (copied from NeoForge's shears). */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof IShearable target && target.isShearable(player, stack, entity.level(), entity.blockPosition())) {
            BlockPos pos = entity.blockPosition();
            List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
            if (entity.level() instanceof ServerLevel serverLevel) {
                drops.forEach(drop -> target.spawnShearedDrop(serverLevel, pos, drop));
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
            entity.gameEvent(GameEvent.SHEAR, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (voidMetal && stack.isDamaged() && owner.tickCount % REPAIR_INTERVAL == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (voidMetal && target.level() instanceof ServerLevel level
                && (!(target instanceof Player) || level.getGameRules().get(GameRules.PVP))) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80), attacker);
        }
    }
}
