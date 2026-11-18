package com.arcananaturae.table;

import com.arcananaturae.MNRegistry;
import com.arcananaturae.item.ResearchLogItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Two slots: the log being filled (left) and the finished log (right). */
public class TranscribingTableMenu extends AbstractContainerMenu {
    public static final int LOG_X = 62;
    public static final int DONE_X = 98;
    public static final int SLOT_Y = 26;

    private final Container container;

    public static TranscribingTableMenu client(int containerId, Inventory inventory) {
        return new TranscribingTableMenu(containerId, inventory, new SimpleContainer(2));
    }

    public TranscribingTableMenu(int containerId, Inventory inventory, Container container) {
        super(MNRegistry.TRANSCRIBING_TABLE_MENU.get(), containerId);
        checkContainerSize(container, 2);
        this.container = container;
        container.startOpen(inventory.player);
        addSlot(new Slot(container, TranscribingTableBlockEntity.SLOT_LOG, LOG_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ResearchLogItem;
            }
        });
        addSlot(new Slot(container, TranscribingTableBlockEntity.SLOT_DONE, DONE_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, 66);
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
            boolean moved = slotIndex < 2 ? moveItemStackTo(stack, 2, slots.size(), true)
                    : stack.getItem() instanceof ResearchLogItem && moveItemStackTo(stack, 0, 1, false);
            if (!moved) {
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
