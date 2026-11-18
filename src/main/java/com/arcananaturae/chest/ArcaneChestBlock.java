package com.arcananaturae.chest;

import com.leclowndu93150.thaumaturge.content.warding.ArcaneAccess;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * Greatwood and Silverwood Arcane Chests. The lock is Thaumaturge's own arcane lock (block tag
 * {@code thaumaturge:arcane_locks}), so its iron and gold keys and our thaumium keys all work on it. The owner is set
 * when the chest is placed (issue #42); access is checked on the server before the menu opens (issue #41). Only
 * someone with access can break it; the owner can sneak-use a wand on it to pack it up with its contents.
 */
public class ArcaneChestBlock extends BaseEntityBlock {
    public static final MapCodec<ArcaneChestBlock> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(ArcaneChestType.CODEC.fieldOf("chest").forGetter(ArcaneChestBlock::type), propertiesCodec()).apply(i, ArcaneChestBlock::new));
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);
    private static final TagKey<Item> WANDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thaumaturge", "wands"));

    private final ArcaneChestType type;

    public ArcaneChestBlock(ArcaneChestType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    public ArcaneChestType type() {
        return type;
    }

    @Override
    protected MapCodec<ArcaneChestBlock> codec() {
        return CODEC;
    }

    /** A chest nobody locked (placed by a structure or a command) is open to everyone, as an ordinary chest. */
    public static boolean canUse(ServerLevel level, BlockPos pos, Player player) {
        return ArcaneAccess.owner(level, pos) == null || ArcaneAccess.canAccess(level, pos, player);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && placer instanceof Player player) {
            ArcaneAccess.lock(serverLevel, pos, player.getUUID());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof ArcaneChestBlockEntity chest)) {
            return InteractionResult.SUCCESS;
        }
        if (!canUse(serverLevel, pos, player)) {
            player.sendOverlayMessage(Component.translatable("message.arcananaturae.arcane_chest_locked", chest.getDisplayName()));
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.FAIL;
        }
        player.openMenu(chest);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(WANDS) || !player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof ArcaneChestBlockEntity chest)) {
            return InteractionResult.SUCCESS;
        }
        if (!player.getUUID().equals(ArcaneAccess.owner(serverLevel, pos)) && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.translatable("message.arcananaturae.arcane_chest_not_owner"));
            return InteractionResult.FAIL;
        }
        // Pack the chest: its contents and name ride along as item components, like a shulker box.
        ItemStack packed = new ItemStack(this);
        packed.applyComponents(chest.collectComponents());
        chest.clearContent();
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 0.8f);
        if (!player.getInventory().add(packed)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, packed);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Server-side break guard; the chest also shrugs off explosions through its blast resistance. */
    public static void onBreak(BreakBlockEvent event) {
        if (event.getState().getBlock() instanceof ArcaneChestBlock && event.getLevel() instanceof ServerLevel level
                && !canUse(level, event.getPos(), event.getPlayer())) {
            event.getPlayer().sendOverlayMessage(Component.translatable("message.arcananaturae.arcane_chest_not_owner"));
            event.setCanceled(true);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
        ArcaneAccess.removeLock(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ArcaneChestBlockEntity chest) {
            chest.recheckOpen();
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneChestBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
