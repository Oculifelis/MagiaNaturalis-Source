package com.arcananaturae.client;

import com.arcananaturae.ArcanaNaturae;
import com.arcananaturae.table.TranscribingTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class TranscribingTableScreen extends AbstractContainerScreen<TranscribingTableMenu> {
    private static final Identifier BACKGROUND = ArcanaNaturae.id("textures/gui/container/transcribing_table.png");

    public TranscribingTableScreen(TranscribingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 148);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
    }
}
