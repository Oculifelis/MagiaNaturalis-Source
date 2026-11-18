package com.arcananaturae.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How far a sickle's reap spreads from the block it cut, and what happens to the loot.
 *
 * @param radius    steps the reap may take through touching blocks of the same kind (the original's "area size")
 * @param maxBlocks hard cap on extra blocks per swing (the original had none, so a big field could cut hundreds)
 * @param collect   reaped drops go straight to the reaper (the Sickle of Abundance's "following items")
 * @param abundance extra loot rolls for blocks in {@code arcananaturae:abundant} (crops), as the original's
 *                  abundance level
 */
public record SickleReach(int radius, int maxBlocks, boolean collect, int abundance) {
    public static final Codec<SickleReach> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 16).fieldOf("radius").forGetter(SickleReach::radius),
            Codec.intRange(0, 1024).fieldOf("max_blocks").forGetter(SickleReach::maxBlocks),
            Codec.BOOL.optionalFieldOf("collect", false).forGetter(SickleReach::collect),
            Codec.intRange(0, 8).optionalFieldOf("abundance", 0).forGetter(SickleReach::abundance)
    ).apply(i, SickleReach::new));
    public static final StreamCodec<ByteBuf, SickleReach> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SickleReach::radius, ByteBufCodecs.VAR_INT, SickleReach::maxBlocks,
            ByteBufCodecs.BOOL, SickleReach::collect, ByteBufCodecs.VAR_INT, SickleReach::abundance, SickleReach::new);

    public SickleReach(int radius, int maxBlocks) {
        this(radius, maxBlocks, false, 0);
    }
}
