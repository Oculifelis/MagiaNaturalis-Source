package com.arcananaturae.chest;

import com.arcananaturae.MNRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A chest grid of any width; the player inventory is centred under it. */
public class ArcaneChestMenu extends AbstractContainerMenu {
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 18;

    private final Container container;
    private final ArcaneChestType type;

    /** Client side: the contents arrive through the usual slot sync. */
    public static ArcaneChestMenu client(ArcaneChestType type, int containerId, Inventory inventory) {
        return new ArcaneChestMenu(type, containerId, inventory, new SimpleContainer(type.size()));
    }

    public ArcaneChestMenu(ArcaneChestType type, int containerId, Inventory inventory, Container container) {
        super(type == ArcaneChestType.GREATWOOD ? MNRegistry.GREATWOOD_CHEST_MENU.get() : MNRegistry.SILVERWOOD_CHEST_MENU.get(), containerId);
        checkContainerSize(container, type.size());
        this.container = container;
        this.type = type;
        container.startOpen(inventory.player);
        for (int y = 0; y < type.rows; y++) {
            for (int x = 0; x < type.columns; x++) {
                addSlot(new Slot(container, x + y * type.columns, GRID_LEFT + x * 18, GRID_TOP + y * 18));
            }
        }
        addStandardInventorySlots(inventory, inventoryLeft(type), GRID_TOP + type.rows * 18 + 13);
    }

    public static int inventoryLeft(ArcaneChestType type) {
        return GRID_LEFT + (type.columns - 9) * 9;
    }

    public ArcaneChestType type() {
        return type;
    }

    public Container getContainer() {
        return container;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            int size = type.size();
            if (slotIndex < size ? !moveItemStackTo(stack, size, slots.size(), true) : !moveItemStackTo(stack, 0, size, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
