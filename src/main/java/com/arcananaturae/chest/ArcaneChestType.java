package com.arcananaturae.chest;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The original's two chest kinds: greatwood holds 54 stacks (9 x 6), silverwood 77 (11 x 7). */
public enum ArcaneChestType implements StringRepresentable {
    GREATWOOD("greatwood", 9, 6),
    SILVERWOOD("silverwood", 11, 7);

    public static final Codec<ArcaneChestType> CODEC = StringRepresentable.fromEnum(ArcaneChestType::values);

    private final String name;
    public final int columns;
    public final int rows;

    ArcaneChestType(String name, int columns, int rows) {
        this.name = name;
        this.columns = columns;
        this.rows = rows;
    }

    public int size() {
        return columns * rows;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
