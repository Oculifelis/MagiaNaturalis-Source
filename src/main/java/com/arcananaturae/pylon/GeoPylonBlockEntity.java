package com.arcananaturae.pylon;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aura.BiomeAspects;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import com.leclowndu93150.thaumaturge.registry.TTDataMaps;
import com.arcananaturae.MNRegistry;
import com.arcananaturae.ArcanaNaturae;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Geo Pylon: repaints the biome around it into the one recorded on a Biome Sampler, one 4x4 biome column at a time
 * within 8 blocks. Each column costs essentia matching the target biome's aspects (Thaumaturge's biome_aspects data map,
 * 4 of each), pulled from jars and other essentia stores within 4 blocks. It runs only on top of its plinth: one empty
 * block below it, then three blocks from the {@code arcananaturae:geo_pylon_base} tag (obsidian tile by default).
 */
public class GeoPylonBlockEntity extends BlockEntity {
    public static final TagKey<Block> BASE = TagKey.create(Registries.BLOCK, ArcanaNaturae.id("geo_pylon_base"));
    private static final int QUART_RADIUS = 2;
    private static final int SIDE = QUART_RADIUS * 2 + 1;
    private static final int DRAIN_RANGE = 4;
    private static final int COST_PER_ASPECT = 4;

    private @Nullable ResourceKey<Biome> target;
    private boolean active;
    private int cursor = -1;
    private boolean priced;
    private AspectList owed = AspectList.EMPTY;
    private final List<BlockPos> sources = new ArrayList<>();

    public GeoPylonBlockEntity(BlockPos pos, BlockState state) {
        super(MNRegistry.GEO_PYLON_ENTITY.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GeoPylonBlockEntity pylon) {
        if (!pylon.active || level.getGameTime() % 5 != 0) {
            return;
        }
        if (pylon.target == null || !structureValid(level, pos)) {
            pylon.stop();
            return;
        }
        Holder<Biome> biome = level.registryAccess().lookupOrThrow(Registries.BIOME).get(pylon.target).orElse(null);
        if (biome == null) {
            pylon.stop();
            return;
        }
        if (pylon.priced) {
            if (!pylon.owed.isEmpty()) {
                pylon.drainOne(level);
                return;
            }
            pylon.paint(level, biome);
            pylon.priced = false;
            pylon.cursor++;
            pylon.setChanged();
        } else if (level.getGameTime() % 20 == 0) {
            pylon.findNext(level, biome);
        }
    }

    public static boolean structureValid(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isAir()) {
            return false;
        }
        for (int i = 2; i <= 4; i++) {
            if (!level.getBlockState(pos.below(i)).is(BASE)) {
                return false;
            }
        }
        return true;
    }

    public void setTarget(ResourceKey<Biome> biome) {
        target = biome;
        cursor = -1;
        priced = false;
        owed = AspectList.EMPTY;
        setChanged();
    }

    public @Nullable ResourceKey<Biome> target() {
        return target;
    }

    public AspectList owed() {
        return owed;
    }

    public boolean toggle() {
        active = !active && target != null;
        if (active) {
            cursor = 0;
            priced = false;
            sources.clear();
        }
        setChanged();
        return active;
    }

    private void stop() {
        active = false;
        cursor = -1;
        priced = false;
        owed = AspectList.EMPTY;
        setChanged();
    }

    private BlockPos columnPos(int index) {
        int qx = QuartPos.fromBlock(worldPosition.getX()) - QUART_RADIUS + index % SIDE;
        int qz = QuartPos.fromBlock(worldPosition.getZ()) - QUART_RADIUS + index / SIDE;
        return new BlockPos(QuartPos.toBlock(qx), worldPosition.getY(), QuartPos.toBlock(qz));
    }

    /** Moves the cursor to the next column inside the circle that is not yet the target biome and prices it. */
    private void findNext(ServerLevel level, Holder<Biome> biome) {
        while (cursor < SIDE * SIDE) {
            int dx = cursor % SIDE - QUART_RADIUS;
            int dz = cursor / SIDE - QUART_RADIUS;
            BlockPos column = columnPos(cursor);
            if (dx * dx + dz * dz <= QUART_RADIUS * QUART_RADIUS && level.hasChunkAt(column) && !level.getBiome(column).is(target)) {
                owed = cost(level, biome);
                priced = true;
                setChanged();
                return;
            }
            cursor++;
        }
        stop();
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private static AspectList cost(ServerLevel level, Holder<Biome> biome) {
        BiomeAspects aspects = biome.getData(TTDataMaps.BIOME_ASPECTS);
        var registry = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY);
        AspectList list = AspectList.EMPTY;
        if (aspects != null) {
            for (ResourceKey<IAspect> key : aspects.aspects()) {
                Holder<IAspect> aspect = registry.get(key).orElse(null);
                if (aspect != null) {
                    list = list.add(aspect, COST_PER_ASPECT);
                }
            }
        }
        if (list.isEmpty()) {
            Holder<IAspect> terra = registry.get(ResourceKey.create(IAspect.REGISTRY_KEY,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumaturge", "terra"))).orElse(null);
            if (terra != null) {
                list = list.add(terra, COST_PER_ASPECT);
            }
        }
        return list;
    }

    /** Pulls one point of any owed aspect from a nearby essentia store. */
    private void drainOne(ServerLevel level) {
        if (sources.isEmpty() || level.getGameTime() % 100 == 0) {
            sources.clear();
            for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-DRAIN_RANGE, -DRAIN_RANGE, -DRAIN_RANGE),
                    worldPosition.offset(DRAIN_RANGE, DRAIN_RANGE, DRAIN_RANGE))) {
                if (!p.equals(worldPosition) && EssentiaAccess.transport(level, p, Direction.UP) != null) {
                    sources.add(p.immutable());
                }
            }
        }
        for (AspectInstance need : owed.entries()) {
            for (BlockPos p : sources) {
                IEssentiaTransport store = EssentiaAccess.transport(level, p, Direction.UP);
                if (store != null && store.takeEssentia(need.aspect(), 1, Direction.UP, true) > 0
                        && store.takeEssentia(need.aspect(), 1, Direction.UP) > 0) {
                    owed = owed.reduce(need.aspect(), 1);
                    setChanged();
                    return;
                }
            }
        }
    }

    private void paint(ServerLevel level, Holder<Biome> biome) {
        BlockPos column = columnPos(cursor);
        if (!(level.getChunk(column) instanceof LevelChunk chunk)) {
            return;
        }
        int qx = QuartPos.fromBlock(column.getX());
        int qz = QuartPos.fromBlock(column.getZ());
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        chunk.fillBiomesFromNoise((x, y, z, ignored) -> x == qx && z == qz ? biome : chunk.getNoiseBiome(x, y, z), sampler);
        chunk.markUnsaved();
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.<ChunkAccess>of(chunk));
        BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, column.offset(2, 0, 2));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, top.getX(), top.getY() + 0.5, top.getZ(), 12, 1.5, 0.4, 1.5, 0.0);
        level.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5, 10, 0.2, 0.3, 0.2, 0.5);
    }

    public Component status() {
        if (target == null) {
            return Component.translatable("message.arcananaturae.pylon_no_target");
        }
        Component biomeName = Component.translatable(target.identifier().toLanguageKey("biome"));
        if (!active) {
            return Component.translatable("message.arcananaturae.pylon_idle", biomeName);
        }
        return Component.translatable("message.arcananaturae.pylon_active", biomeName, owed.totalAmount());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("target", ResourceKey.codec(Registries.BIOME), target);
        output.putBoolean("active", active);
        output.putInt("cursor", cursor);
        output.putBoolean("priced", priced);
        output.store("owed", AspectList.CODEC, owed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        target = input.read("target", ResourceKey.codec(Registries.BIOME)).orElse(null);
        active = input.getBooleanOr("active", false);
        cursor = input.getIntOr("cursor", -1);
        priced = input.getBooleanOr("priced", false);
        owed = input.read("owed", AspectList.CODEC).orElse(AspectList.EMPTY);
    }
}
