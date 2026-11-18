package com.arcananaturae.client;

import com.arcananaturae.ArcanaNaturae;
import com.arcananaturae.chest.ArcaneChestMenu;
import com.arcananaturae.chest.ArcaneChestType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** One drawn background per chest kind (tools/pixelart.py draws them at the menu's exact size). */
public class ArcaneChestScreen extends AbstractContainerScreen<ArcaneChestMenu> {
    private final Identifier background;

    public ArcaneChestScreen(ArcaneChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, width(menu.type()), height(menu.type()));
        this.background = ArcanaNaturae.id("textures/gui/container/" + menu.type().getSerializedName() + "_arcane_chest.png");
        this.inventoryLabelX = ArcaneChestMenu.inventoryLeft(menu.type());
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public static int width(ArcaneChestType type) {
        return type.columns * 18 + 14;
    }

    public static int height(ArcaneChestType type) {
        return 114 + type.rows * 18;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
    }
}
