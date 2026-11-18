package com.arcananaturae.item;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import com.arcananaturae.ArcanaNaturae;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * Spectacles and Dark Crystal Goggles: head gear that counts as Thaumaturge revealing goggles (the
 * {@code thaumaturge:goggles_upgrade} component on the item) and lowers vis costs through {@link IVisDiscountGear}.
 * The Spectacles also name the block under the crosshair (see SpectaclesHud). The Dark Crystal Goggles shake off
 * blindness and darkness, and anyone wearing an item in {@code arcananaturae:reveals_invisible} sees invisible
 * creatures as ghosts.
 */
public class RevealingGlassesItem extends Item implements IVisDiscountGear {
    public static final TagKey<Item> SHOWS_BLOCK_NAMES = TagKey.create(Registries.ITEM, ArcanaNaturae.id("shows_block_names"));
    public static final TagKey<Item> REVEALS_INVISIBLE = TagKey.create(Registries.ITEM, ArcanaNaturae.id("reveals_invisible"));

    private final int discount;
    private final boolean clearsDarkness;

    public RevealingGlassesItem(Item.Properties properties, int discount, boolean clearsDarkness) {
        super(properties);
        this.discount = discount;
        this.clearsDarkness = clearsDarkness;
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return discount;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (clearsDarkness && slot == EquipmentSlot.HEAD && owner instanceof LivingEntity living) {
            living.removeEffect(MobEffects.BLINDNESS);
            living.removeEffect(MobEffects.DARKNESS);
        }
    }

    public static boolean wearing(@Nullable LivingEntity entity, TagKey<Item> tag) {
        return entity != null && entity.getItemBySlot(EquipmentSlot.HEAD).is(tag);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.arcananaturae.vis_discount", discount).withStyle(ChatFormatting.DARK_PURPLE));
        if (clearsDarkness) {
            builder.accept(Component.translatable("tooltip.arcananaturae.dark_goggles").withStyle(ChatFormatting.GRAY));
        }
    }
}
