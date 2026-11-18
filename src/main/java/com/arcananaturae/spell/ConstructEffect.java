package com.arcananaturae.spell;

import com.leclowndu93150.thaumaturge.api.casters.ICaster;
import com.leclowndu93150.thaumaturge.api.casters.IFocusBlockPicker;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.spell.casting.CasterHands;
import com.arcananaturae.MNRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Construction (the original Construction Focus): builds a square of the caster's picked block on the face that was
 * hit, using blocks from the caster's inventory (free in creative). The spell's "radius" setting (0-2) picks 1x1, 3x3 or
 * 5x5. Placement fires the normal block-place event, so claims and protection mods can veto it.
 */
public final class ConstructEffect extends AbstractEffectBehavior implements IFocusBlockPicker {
    public static final ConstructEffect INSTANCE = new ConstructEffect();
    public static final MapCodec<ConstructEffect> CODEC = MapCodec.unit(INSTANCE);

    private ConstructEffect() {
    }

    @Override
    public SpellBehaviorType<?> type() {
        return MNRegistry.CONSTRUCT_BEHAVIOR.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (target.block().isEmpty() || !(ctx.caster() instanceof Player player) || !player.mayBuild()) {
            return;
        }
        ItemStack held = CasterHands.held(player);
        BlockState picked = held.getItem() instanceof ICaster caster ? caster.getPickedBlock(held) : null;
        if (picked == null || picked.isAir()) {
            return;
        }
        ServerLevel level = ctx.level();
        BlockHitResult hit = target.block().get();
        Direction face = hit.getDirection();
        BlockPos centre = hit.getBlockPos().relative(face);
        int half = Math.max(0, Math.min(2, ctx.setting("radius")));
        Direction.Axis axis = face.getAxis();
        boolean placedAny = false;
        for (int a = -half; a <= half; a++) {
            for (int b = -half; b <= half; b++) {
                BlockPos pos = switch (axis) {
                    case X -> centre.offset(0, a, b);
                    case Y -> centre.offset(a, 0, b);
                    case Z -> centre.offset(a, b, 0);
                };
                placedAny |= place(level, player, pos, picked);
            }
        }
        if (placedAny) {
            SoundType sound = picked.getSoundType(level, centre, player);
            level.playSound(null, centre, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }
    }

    private static boolean place(ServerLevel level, Player player, BlockPos pos, BlockState state) {
        if (!level.isInWorldBounds(pos) || !level.mayInteract(player, pos) || !level.getBlockState(pos).canBeReplaced()
                || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
            return false;
        }
        int slot = player.getAbilities().instabuild ? -1 : findItem(player, state);
        if (slot == -2) {
            return false;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }
        if (EventHooks.onBlockPlace(player, snapshot, Direction.UP)) {
            snapshot.restore(Block.UPDATE_ALL);
            return false;
        }
        if (slot >= 0) {
            player.getInventory().getItem(slot).shrink(1);
        }
        return true;
    }

    /** Inventory slot holding the block's item, -2 when there is none. */
    private static int findItem(Player player, BlockState state) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(state.getBlock().asItem())) {
                return i;
            }
        }
        return -2;
    }
}
