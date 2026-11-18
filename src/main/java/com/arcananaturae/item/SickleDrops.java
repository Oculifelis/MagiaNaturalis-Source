package com.arcananaturae.item;

import com.arcananaturae.MNRegistry;
import com.arcananaturae.ArcanaNaturae;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Sickle of Abundance loot: extra loot-table rolls for crops (the original's abundance level), and every drop of a
 * reaped block lands in the reaper's inventory (the original spawned "following items" that flew to the player).
 * Works off the drop event, so it covers the cut block and every block of the reap alike.
 */
public final class SickleDrops {
    /** Blocks whose drops the abundance rolls repeat; crops by default, never leaves or blocks that drop themselves. */
    public static final TagKey<Block> ABUNDANT = TagKey.create(Registries.BLOCK, ArcanaNaturae.id("abundant"));

    private SickleDrops() {
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || player.isCreative()) {
            return;
        }
        ItemStack tool = event.getTool();
        SickleReach reach = tool.get(MNRegistry.SICKLE_REACH.get());
        if (reach == null || !event.getState().is(MNRegistry.SICKLE_MINEABLE)) {
            return;
        }
        ServerLevel level = event.getLevel();
        boolean silk = EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
        if (reach.abundance() > 0 && !silk && event.getState().is(ABUNDANT)) {
            for (int i = 0; i < reach.abundance(); i++) {
                for (ItemStack extra : Block.getDrops(event.getState(), level, event.getPos(), event.getBlockEntity(), player, tool)) {
                    ItemEntity entity = new ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5,
                            event.getPos().getZ() + 0.5, extra);
                    entity.setDefaultPickUpDelay();
                    event.getDrops().add(entity);
                }
            }
        }
        if (reach.collect()) {
            event.getDrops().removeIf(entity -> {
                ItemStack stack = entity.getItem();
                player.getInventory().add(stack);
                return stack.isEmpty();
            });
            // What did not fit falls at the reaper's feet instead of out in the field.
            for (ItemEntity left : event.getDrops()) {
                left.setPos(player.getX(), player.getY() + 0.2, player.getZ());
                left.setNoPickUpDelay();
            }
        }
    }
}
