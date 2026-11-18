package com.arcananaturae.morph;

import com.arcananaturae.ArcanaNaturae;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * Datapack cycles for the Phenomorphous Stone: {@code data/<ns>/arcananaturae/mutation_cycle/<name>.json} holds
 * {@code {"blocks": [ids...]}} and a click moves a block to the next id in its list (the last wraps to the first).
 * The original hardcoded these as metadata maths; unknown ids (a mod that is not installed) are skipped.
 */
public final class MorphCycles extends SimpleJsonResourceReloadListener<MorphCycles.Cycle> {
    public record Cycle(List<Identifier> blocks) {
        public static final Codec<Cycle> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.listOf().fieldOf("blocks").forGetter(Cycle::blocks)
        ).apply(i, Cycle::new));
    }

    private static volatile Map<Block, Block> next = Map.of();

    public MorphCycles() {
        super(Cycle.CODEC, FileToIdConverter.json(ArcanaNaturae.MODID + "/mutation_cycle"));
    }

    @Override
    protected void apply(Map<Identifier, Cycle> cycles, ResourceManager manager, ProfilerFiller profiler) {
        Map<Block, Block> map = new HashMap<>();
        cycles.forEach((id, cycle) -> {
            List<Block> blocks = cycle.blocks().stream()
                    .map(b -> BuiltInRegistries.BLOCK.getOptional(b).orElse(Blocks.AIR))
                    .filter(b -> b != Blocks.AIR)
                    .toList();
            for (int i = 0; i < blocks.size() && blocks.size() > 1; i++) {
                Block previous = map.put(blocks.get(i), blocks.get((i + 1) % blocks.size()));
                if (previous != null) {
                    ArcanaNaturae.LOG.warn("Mutation cycle {} lists {} again; the later cycle wins", id, blocks.get(i));
                }
            }
        });
        next = Map.copyOf(map);
        ArcanaNaturae.LOG.info("Loaded {} mutation cycles covering {} blocks", cycles.size(), map.size());
    }

    /** Every block in a cycle and the block it turns into (for JEI). */
    public static Map<Block, Block> all() {
        return next;
    }

    /** Client side: the server's cycles, from {@link MorphSync}. */
    static void set(Map<Block, Block> map) {
        next = Map.copyOf(map);
    }

    /** The next look for this block, keeping every block state property the two blocks share; null if not in a cycle. */
    public static @Nullable BlockState morph(BlockState state) {
        Block to = next.get(state.getBlock());
        if (to == null) {
            return null;
        }
        BlockState out = to.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            out = copy(state, out, property);
        }
        return out;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        Property<?> target = to.getBlock().getStateDefinition().getProperty(property.getName());
        return target == null ? to : set(to, target, property.getName(from.getValue(property)));
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(v -> state.setValue(property, v)).orElse(state);
    }
}
