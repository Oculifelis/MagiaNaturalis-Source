package com.arcananaturae.table;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import com.leclowndu93150.thaumaturge.content.research.decon.BlockEntityDeconstructionTable;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * Takes the aspect a Thaumaturge Deconstruction Table has broken out, without giving it to anyone's pool. The table
 * only hands its result out through {@code collect(player)}, so a fake player collects it and spends it again.
 */
public final class DeconTables {
    private DeconTables() {
    }

    /** The waiting aspect, if any and if {@code accept} wants it; the table is emptied only when it is taken. */
    public static @Nullable Holder<IAspect> take(ServerLevel level, BlockPos pos, Predicate<Holder<IAspect>> accept) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityDeconstructionTable table)) {
            return null;
        }
        Identifier id = table.resultAspect();
        if (id == null) {
            return null;
        }
        Holder<IAspect> aspect = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY)
                .get(ResourceKey.create(IAspect.REGISTRY_KEY, id)).orElse(null);
        if (aspect == null || !accept.test(aspect)) {
            return null;
        }
        FakePlayer hand = FakePlayerFactory.getMinecraft(level);
        table.collect(hand);
        AspectPoolAccess.spend(hand, aspect, 1);
        return aspect;
    }
}
