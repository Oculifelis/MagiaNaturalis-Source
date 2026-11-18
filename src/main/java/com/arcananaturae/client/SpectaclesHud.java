package com.arcananaturae.client;

import com.arcananaturae.item.RevealingGlassesItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Spectacles' crosshair line: the name of the block or creature you look at. Names come from the block's own
 * {@code getName()}, so nothing shows a raw key like the original's {@code table.blockAiry.0.name} (issue #43).
 */
public final class SpectaclesHud {
    private SpectaclesHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null
                || !RevealingGlassesItem.wearing(mc.player, RevealingGlassesItem.SHOWS_BLOCK_NAMES)) {
            return;
        }
        Component name = null;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            name = mc.level.getBlockState(blockHit.getBlockPos()).getBlock().getName();
        } else if (hit instanceof EntityHitResult entityHit) {
            name = entityHit.getEntity().getDisplayName();
        }
        if (name != null) {
            graphics.centeredText(mc.font, name, graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 10, 0xFFE0D0FF);
        }
    }
}
