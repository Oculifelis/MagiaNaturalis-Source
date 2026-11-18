package com.arcananaturae.pylon;

import com.arcananaturae.MNRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Use with an empty hand to start or stop the pylon; sneak-use to read what it is doing. */
public class GeoPylonBlock extends BaseEntityBlock {
    public static final MapCodec<GeoPylonBlock> CODEC = simpleCodec(GeoPylonBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 3, 13), Block.box(5, 3, 5, 11, 13, 11), Block.box(4, 13, 4, 12, 16, 12));

    public GeoPylonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GeoPylonBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof GeoPylonBlockEntity pylon)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            player.sendOverlayMessage(pylon.status());
            return InteractionResult.SUCCESS_SERVER;
        }
        if (pylon.target() == null) {
            player.sendOverlayMessage(Component.translatable("message.arcananaturae.pylon_no_target"));
        } else if (!GeoPylonBlockEntity.structureValid(server, pos)) {
            player.sendOverlayMessage(Component.translatable("message.arcananaturae.pylon_no_base"));
        } else {
            boolean on = pylon.toggle();
            level.playSound(null, pos, on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 1.2F);
            player.sendOverlayMessage(pylon.status());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeoPylonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, MNRegistry.GEO_PYLON_ENTITY.get(),
                (l, p, s, pylon) -> GeoPylonBlockEntity.serverTick((ServerLevel) l, p, s, pylon));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
