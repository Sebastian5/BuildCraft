package buildcraft.lib.inventory;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import buildcraft.api.core.IStackFilter;

import buildcraft.lib.misc.StackUtil;

public final class SidedInventoryWrapper extends AbstractInvItemTransactor {
    private final ISidedInventory sided;
    private final InventoryWrapper normal;
    private final EnumFacing face;
    private final int[] slots;

    public SidedInventoryWrapper(ISidedInventory sided, EnumFacing face) {
        this.sided = sided;
        this.normal = new InventoryWrapper(sided);
        this.face = face;
        slots = sided.getSlotsForFace(face);
    }

    @Override
    protected ItemStack insert(int externalSlot, ItemStack stack, boolean simulate) {
        int sidedSlot = slots[externalSlot];
        if (sided.canInsertItem(sidedSlot, stack, face)) {
            // Delegate to the normal inserter - its just easier.
            return normal.insert(sidedSlot, stack, simulate);
        }
        return stack;
    }

    @Override
    protected ItemStack extract(int externalSlot, IStackFilter filter, int min, int max, boolean simulate) {
        int sidedSlot = slots[externalSlot];
        ItemStack current = sided.getStackInSlot(sidedSlot);
        if (sided.canExtractItem(sidedSlot, current, face)) {
            // Delegate to the normal inserter - its just easier.
            return normal.extract(sidedSlot, filter, min, max, simulate);
        }
        return StackUtil.EMPTY;
    }

    @Override
    protected int getSlots() {
        return slots.length;
    }

    @Override
    protected boolean isEmpty(int slot) {
        return normal.isEmpty(slots[slot]);
    }
}
