package com.arcananaturae.item;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.arcananaturae.ArcanaNaturae;
import com.arcananaturae.morph.MorphCycles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jspecify.annotations.Nullable;

/**
 * Phenomorphous Stone (the original Mutation Stone, damage 0 of its Alchemical Stone): click a block to give it the
 * next look in its mutation cycle (wool colours, wood types, stone variants), sneak-click to turn logs, pillars,
 * stairs and slabs. Never consumed. The cycles are datapack JSON (see {@link MorphCycles}).
 */
public class MutationStoneItem extends Item {
    /** Blocks the sneak-click may turn; kept to simple building blocks so it never twists machines or multiblocks. */
    public static final TagKey<Block> ROTATABLE = TagKey.create(Registries.BLOCK, ArcanaNaturae.id("mutation_rotatable"));

    public MutationStoneItem(Item.Properties properties) {
        super(properties);
    }

    /** The stone is a crafting tool for the wood and colour conversions: it comes back out of the grid. */
    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        return new ItemStackTemplate(this);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        BlockState changed = player.isShiftKeyDown() ? turn(state) : MorphCycles.morph(state);
        if (changed == null || changed == state) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, changed, Block.UPDATE_ALL);
            level.playSound(null, pos, TTSounds.ZAP.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    private static @Nullable BlockState turn(BlockState state) {
        if (!state.is(ROTATABLE)) {
            return null;
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.cycle(BlockStateProperties.AXIS);
        }
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
            return type == SlabType.DOUBLE ? null
                    : state.setValue(BlockStateProperties.SLAB_TYPE, type == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && state.hasProperty(BlockStateProperties.HALF)) {
            // Stairs: four turns, then flip upside down, like the original walk through stair metadata.
            BlockState turned = state.cycle(BlockStateProperties.HORIZONTAL_FACING);
            boolean fullCircle = turned.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    == state.getBlock().defaultBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            return fullCircle ? turned.cycle(BlockStateProperties.HALF) : turned;
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.cycle(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.cycle(BlockStateProperties.FACING);
        }
        return null;
    }
}
