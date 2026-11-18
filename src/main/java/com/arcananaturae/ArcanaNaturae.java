package com.arcananaturae;

import com.arcananaturae.chest.ArcaneChestBlock;
import com.arcananaturae.item.SickleDrops;
import com.arcananaturae.item.ThaumiumKeyItem;
import com.arcananaturae.morph.MorphCycles;
import com.arcananaturae.morph.MorphSync;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;

/** Magia Naturalis (Elenterius, v0.5.0), ported from the Thaumcraft 4 addon to Thaumaturge (NeoForge 26.1.2). */
@Mod(ArcanaNaturae.MODID)
public final class ArcanaNaturae {
    public static final String MODID = "arcananaturae";
    /** The mod id before the 2026-11-14 rename; see {@link LegacyIds}. */
    public static final String LEGACY_MODID = "magianaturalis";
    public static final Logger LOG = LogUtils.getLogger();

    public ArcanaNaturae(IEventBus modBus) {
        LegacyIds.register(modBus);
        MNRegistry.register(modBus);
        MorphSync.register(modBus);
        NeoForge.EVENT_BUS.addListener(ArcanaNaturae::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(SickleDrops::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(ArcaneChestBlock::onBreak);
        NeoForge.EVENT_BUS.addListener(ThaumiumKeyItem::onAttack);
    }

    private static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(id("mutation_cycles"), new MorphCycles());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /** The same path under the old mod id. */
    public static Identifier legacyId(String path) {
        return Identifier.fromNamespaceAndPath(LEGACY_MODID, path);
    }
}
