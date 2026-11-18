package com.arcananaturae.table;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.arcananaturae.MNRegistry;
import com.arcananaturae.item.ResearchLogItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Transcribing Table: every two seconds it looks at one Deconstruction Table two blocks away (straight or diagonal)
 * and files that table's result into the Research Log in its first slot. A log that cannot take what it found moves
 * to the output slot. Hoppers feed logs in from any side but the top and pull filled logs out.
 */
public class TranscribingTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_LOG = 0;
    public static final int SLOT_DONE = 1;
    private static final int INTERVAL = 40;
    private static final int[] SIDE_SLOTS = {SLOT_LOG, SLOT_DONE};
    private static final int[] TOP_SLOTS = {SLOT_DONE};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int timer = INTERVAL;

    public TranscribingTableBlockEntity(BlockPos pos, BlockState state) {
        super(MNRegistry.TRANSCRIBING_TABLE_ENTITY.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, TranscribingTableBlockEntity table) {
        ItemStack log = table.items.get(SLOT_LOG);
        if (!(log.getItem() instanceof ResearchLogItem) || --table.timer > 0) {
            return;
        }
        table.timer = INTERVAL;
        int dx = (level.getRandom().nextInt(2) - level.getRandom().nextInt(2)) * 2;
        int dz = (level.getRandom().nextInt(2) - level.getRandom().nextInt(2)) * 2;
        if (dx == 0 && dz == 0) {
            return;
        }
        BlockPos target = pos.offset(dx, 0, dz);
        boolean[] full = {false};
        Holder<IAspect> aspect = DeconTables.take(level, target, a -> {
            full[0] = !ResearchLogItem.canAdd(log, a);
            return !full[0];
        });
        if (aspect != null) {
            ResearchLogItem.add(log, aspect);
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.6f, 1.3f);
            table.setChanged();
        } else if (full[0] && table.items.get(SLOT_DONE).isEmpty()) {
            table.items.set(SLOT_DONE, log);
            table.items.set(SLOT_LOG, ItemStack.EMPTY);
            table.setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("timer", timer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        timer = input.getIntOr("timer", INTERVAL);
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.arcananaturae.transcribing_table");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new TranscribingTableMenu(containerId, inventory, this);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_LOG && stack.getItem() instanceof ResearchLogItem;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP_SLOTS : SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_DONE;
    }
}
