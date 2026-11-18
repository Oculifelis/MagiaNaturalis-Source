package com.arcananaturae.morph;

import com.arcananaturae.ArcanaNaturae;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Sends the mutation cycles to clients on every datapack sync, so JEI can show them on a dedicated server too. */
public record MorphSync(Map<Block, Block> next) implements CustomPacketPayload {
    public static final Type<MorphSync> TYPE = new Type<>(ArcanaNaturae.id("mutation_cycles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSync> CODEC = ByteBufCodecs
            .map(HashMap<Block, Block>::new, ByteBufCodecs.registry(Registries.BLOCK), ByteBufCodecs.registry(Registries.BLOCK))
            .map(MorphSync::new, s -> new HashMap<>(s.next()));

    @Override
    public Type<MorphSync> type() {
        return TYPE;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional()
                .playToClient(TYPE, CODEC, (payload, ctx) -> MorphCycles.set(payload.next())));
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> {
            MorphSync payload = new MorphSync(MorphCycles.all());
            e.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, payload));
        });
    }
}
