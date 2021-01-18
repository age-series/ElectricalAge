package mods.eln.node.transparent

// Seems unused.

/*
class TransparentNodeEntityWithSiededInv : TransparentNodeEntity(), ISidedInventory {
    override fun getSidedInventory(): ISidedInventory {
        if (worldObj.isRemote) {
            if (elementRender == null) return instance
            val i = elementRender!!.inventory
            if (i != null && i is ISidedInventory) {
                return i
            }
        } else {
            val node = node
            if (node != null && node is TransparentNode) {
                val i = node.getInventory(null)
                if (i != null && i is ISidedInventory) {
                    return i
                }
            }
        }
        return instance
    }

    override fun getSizeInventory(): Int {
        return sidedInventory.sizeInventory
    }

    override fun getStackInSlot(var1: Int): ItemStack {
        return sidedInventory.getStackInSlot(var1)
    }

    override fun decrStackSize(var1: Int, var2: Int): ItemStack {
        return sidedInventory.decrStackSize(var1, var2)
    }

    override fun getStackInSlotOnClosing(var1: Int): ItemStack {
        return sidedInventory.getStackInSlotOnClosing(var1)
    }

    override fun setInventorySlotContents(var1: Int, var2: ItemStack) {
        sidedInventory.setInventorySlotContents(var1, var2)
    }

    override fun getInventoryName(): String {
        return sidedInventory.inventoryName
    }

    override fun hasCustomInventoryName(): Boolean {
        return sidedInventory.hasCustomInventoryName()
    }

    override fun getInventoryStackLimit(): Int {
        return sidedInventory.inventoryStackLimit
    }

    override fun isUseableByPlayer(var1: EntityPlayer): Boolean {
        return sidedInventory.isUseableByPlayer(var1)
    }

    override fun openInventory() {
        sidedInventory.openInventory()
    }

    override fun closeInventory() {
        sidedInventory.closeInventory()
    }

    override fun isItemValidForSlot(var1: Int, var2: ItemStack): Boolean {
        return sidedInventory.isItemValidForSlot(var1, var2)
    }

    override fun getAccessibleSlotsFromSide(var1: Int): IntArray {
        return sidedInventory.getAccessibleSlotsFromSide(var1)
    }

    override fun canInsertItem(var1: Int, var2: ItemStack, var3: Int): Boolean {
        return sidedInventory.canInsertItem(var1, var2, var3)
    }

    override fun canExtractItem(var1: Int, var2: ItemStack, var3: Int): Boolean {
        return sidedInventory.canExtractItem(var1, var2, var3)
    }
}
 */
