package com.arcananaturae.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Enderic Focus Pouch: a belt pouch woven into the ender chest. Use it to open your ender chest from anywhere; it can
 * be worn in a Curios belt slot. (Thaumaturge's wands only draw foci from its own focus pouch, so this one stores
 * nothing of its own.)
 */
public class EndericPouchItem extends Item {
    private static final Component TITLE = Component.translatable("container.enderchest");

    public EndericPouchItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()), TITLE));
            player.awardStat(Stats.OPEN_ENDERCHEST);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }
}
