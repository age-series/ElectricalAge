package mods.eln.misc

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack

class FakeSideInventory : ISidedInventory {
    override fun getSizeInventory(): Int {
        return 0
    }

    override fun getStackInSlot(var1: Int): ItemStack? {
        return null
    }

    override fun decrStackSize(var1: Int, var2: Int): ItemStack? {
        return null
    }

    override fun getStackInSlotOnClosing(var1: Int): ItemStack? {
        return null
    }

    override fun setInventorySlotContents(var1: Int, var2: ItemStack) {}
    override fun getInventoryName(): String {
        return "FakeSideInventory"
    }

    override fun hasCustomInventoryName(): Boolean {
        return false
    }

    override fun getInventoryStackLimit(): Int {
        return 0
    }

    override fun markDirty() {}
    override fun isUseableByPlayer(var1: EntityPlayer): Boolean {
        return false
    }

    override fun openInventory() {}
    override fun closeInventory() {}
    override fun isItemValidForSlot(var1: Int, var2: ItemStack): Boolean {
        return false
    }

    override fun getAccessibleSlotsFromSide(var1: Int): IntArray {
        return intArrayOf()
    }

    override fun canInsertItem(var1: Int, var2: ItemStack, var3: Int): Boolean {
        return false
    }

    override fun canExtractItem(var1: Int, var2: ItemStack, var3: Int): Boolean {
        return false
    }

    companion object {
        @JvmStatic
        val instance = FakeSideInventory()
    }
}
