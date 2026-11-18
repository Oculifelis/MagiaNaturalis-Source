package com.arcananaturae.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * Who a thaumium key belongs to: the forger (whose locks it opens), the bound player (who may use an Unraveling key)
 * and the endorsed players (added to a lock by an Endorsing key). Replaces the original's NBT strings.
 */
public record KeyPlayers(Optional<Named> forger, Optional<Named> bound, List<Named> endorsed) {
    public static final int MAX_ENDORSED = 16;
    public static final KeyPlayers EMPTY = new KeyPlayers(Optional.empty(), Optional.empty(), List.of());

    public record Named(UUID id, String name) {
        public static final Codec<Named> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Named::id),
                Codec.STRING.fieldOf("name").forGetter(Named::name)).apply(i, Named::new));
        public static final StreamCodec<ByteBuf, Named> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Named::id, ByteBufCodecs.STRING_UTF8, Named::name, Named::new);

        public static Named of(Player player) {
            return new Named(player.getUUID(), player.getGameProfile().name());
        }
    }

    public static final Codec<KeyPlayers> CODEC = RecordCodecBuilder.create(i -> i.group(
            Named.CODEC.optionalFieldOf("forger").forGetter(KeyPlayers::forger),
            Named.CODEC.optionalFieldOf("bound").forGetter(KeyPlayers::bound),
            Named.CODEC.listOf().optionalFieldOf("endorsed", List.of()).forGetter(KeyPlayers::endorsed)).apply(i, KeyPlayers::new));
    public static final StreamCodec<ByteBuf, KeyPlayers> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Named.STREAM_CODEC), KeyPlayers::forger,
            ByteBufCodecs.optional(Named.STREAM_CODEC), KeyPlayers::bound,
            Named.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENDORSED)), KeyPlayers::endorsed,
            KeyPlayers::new);

    public KeyPlayers withForger(Player player) {
        return new KeyPlayers(Optional.of(Named.of(player)), bound, endorsed);
    }

    public KeyPlayers withBound(Player player) {
        return new KeyPlayers(forger, Optional.of(Named.of(player)), endorsed);
    }

    public KeyPlayers withEndorsed(Player player) {
        List<Named> list = new ArrayList<>(endorsed);
        list.removeIf(n -> n.id().equals(player.getUUID()));
        list.add(Named.of(player));
        return new KeyPlayers(forger, bound, List.copyOf(list.subList(Math.max(0, list.size() - MAX_ENDORSED), list.size())));
    }
}
