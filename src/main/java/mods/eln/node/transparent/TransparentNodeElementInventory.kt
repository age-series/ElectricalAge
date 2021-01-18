package mods.eln.node.transparent

import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils.readFromNBT
import mods.eln.misc.Utils.writeToNBT
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

open class TransparentNodeElementInventory : ISidedInventory, INBTTReady {
    @JvmField
    protected var transparentNodeRender: TransparentNodeElementRender? = null
    @JvmField
    protected var transparentNodeElement: TransparentNodeElement? = null
    var stackLimit: Int

    constructor(size: Int, stackLimit: Int, TransparentnodeRender: TransparentNodeElementRender?) {
        inv = arrayOfNulls(size)
        this.stackLimit = stackLimit
        transparentNodeRender = TransparentnodeRender
    }

    constructor(size: Int, stackLimit: Int, TransparentNodeElement: TransparentNodeElement?) {
        inv = arrayOfNulls(size)
        this.stackLimit = stackLimit
        transparentNodeElement = TransparentNodeElement
    }

    private var inv: Array<ItemStack?>
    override fun getSizeInventory(): Int {
        return inv.size
    }

    override fun getStackInSlot(slot: Int): ItemStack? {
        return inv[slot]
    }

    override fun decrStackSize(slot: Int, amt: Int): ItemStack? {
        var stack = getStackInSlot(slot)
        if (stack != null) {
            if (stack.stackSize <= amt) {
                setInventorySlotContents(slot, null)
            } else {
                stack = stack.splitStack(amt)
                if (stack.stackSize == 0) {
                    setInventorySlotContents(slot, null)
                }
            }
        }
        return stack
    }

    override fun getStackInSlotOnClosing(slot: Int): ItemStack? {
        val stack = getStackInSlot(slot)
        if (stack != null) {
            setInventorySlotContents(slot, null)
        }
        return stack
    }

    override fun setInventorySlotContents(slot: Int, stack: ItemStack?) {
        inv[slot] = stack
        if (stack != null && stack.stackSize > inventoryStackLimit) {
            stack.stackSize = inventoryStackLimit
        }
    }

    override fun getInventoryName(): String {
        return "tco.TransparentNodeInventory"
    }

    override fun getInventoryStackLimit(): Int {
        return stackLimit
    }

    override fun isUseableByPlayer(player: EntityPlayer): Boolean {
        return true
    }

    override fun openInventory() {}
    override fun closeInventory() {}
    override fun markDirty() {
        if (transparentNodeElement != null && !transparentNodeElement!!.node!!.isDestructing) {
            transparentNodeElement!!.inventoryChange(this)
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        readFromNBT(nbt, str, this)
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        writeToNBT(nbt, str, this)
    }

    override fun isItemValidForSlot(i: Int, itemstack: ItemStack): Boolean {
        for (idx in 0..5) {
            val lol = getAccessibleSlotsFromSide(idx)
            for (hohoho in lol) {
                if (hohoho == i && canInsertItem(i, itemstack, idx)) {
                    return true
                }
            }
        }
        return false
    }

    override fun hasCustomInventoryName(): Boolean {
        return false
    }

    override fun getAccessibleSlotsFromSide(side: Int): IntArray {
        return intArrayOf()
    }

    override fun canInsertItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        return false
    }

    override fun canExtractItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        return false
    }
}
