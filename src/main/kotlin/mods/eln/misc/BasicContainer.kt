package mods.eln.misc

import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkin
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import kotlin.math.min

open class BasicContainer(player: EntityPlayer, protected var inventory: IInventory, slot: Array<Slot>) : Container() {
    init {
        for (i in slot.indices) {
            addSlotToContainer(slot[i])
        }
        bindPlayerInventory(player.inventory)
    }

    override fun canInteractWith(player: EntityPlayer): Boolean {
        return inventory.isUseableByPlayer(player)
    }

    private fun bindPlayerInventory(inventoryPlayer: InventoryPlayer?) {
        for (i in 0..2) {
            for (j in 0..8) {
                addSlotToContainer(SlotWithSkin(inventoryPlayer, j + i * 9 + 9, j * 18, i * 18, SlotSkin.medium))
            }
        }
        for (i in 0..8) {
            addSlotToContainer(SlotWithSkin(inventoryPlayer, i, i * 18, 58, SlotSkin.medium))
        }
    }

    override fun addSlotToContainer(slot: Slot): Slot {
        return super.addSlotToContainer(slot)
    }

    override fun transferStackInSlot(player: EntityPlayer, slotId: Int): ItemStack? {
        val slot = inventorySlots[slotId] as Slot?
        if (slot != null && slot.hasStack) {
            val itemstack1 = slot.stack
            val invSize = inventory.getSizeInventory()
            if (slotId < invSize) {
                mergeItemStack(itemstack1, invSize, inventorySlots.size, true)
            } else {
                if (!mergeItemStack(itemstack1, 0, invSize, true)) {
                    if (slotId < invSize + 27) {
                        mergeItemStack(itemstack1, invSize + 27, inventorySlots.size, false)
                    } else {
                        mergeItemStack(itemstack1, invSize, invSize + 27, false)
                    }
                }
            }

            if (itemstack1.stackSize == 0) {
                slot.putStack(null as ItemStack?)
            } else {
                slot.onSlotChanged()
            }
        }

        return null
    }

    override fun mergeItemStack(par1ItemStack: ItemStack, par2: Int, par3: Int, par4: Boolean): Boolean {
        var flag1 = false
        var k = par2
        if (par4) {
            k = par3 - 1
        }
        var slot: Slot
        var itemstack1: ItemStack?
        if (par1ItemStack.isStackable) {
            while (par1ItemStack.stackSize > 0 && (!par4 && k < par3 || par4 && k >= par2)) {
                slot = inventorySlots[k] as Slot
                itemstack1 = slot.stack
                if (slot.isItemValid(par1ItemStack) && itemstack1 != null && itemstack1.item === par1ItemStack.item && (!par1ItemStack.hasSubtypes || par1ItemStack.itemDamage == itemstack1.itemDamage) && ItemStack.areItemStackTagsEqual(
                        par1ItemStack,
                        itemstack1
                    )
                ) {
                    val l = itemstack1.stackSize + par1ItemStack.stackSize
                    val maxSize = min(slot.slotStackLimit.toDouble(), par1ItemStack.maxStackSize.toDouble())
                        .toInt()
                    if (l <= maxSize) {
                        par1ItemStack.stackSize = 0
                        itemstack1.stackSize = l
                        slot.onSlotChanged()
                        flag1 = true
                    } else if (itemstack1.stackSize < maxSize) {
                        par1ItemStack.stackSize -= maxSize - itemstack1.stackSize
                        itemstack1.stackSize = maxSize
                        slot.onSlotChanged()
                        flag1 = true
                    }
                }
                if (par4) {
                    --k
                } else {
                    ++k
                }
            }
        }
        if (par1ItemStack.stackSize > 0) {
            k = if (par4) {
                par3 - 1
            } else {
                par2
            }
            while (!par4 && k < par3 || par4 && k >= par2) {
                slot = inventorySlots[k] as Slot
                itemstack1 = slot.stack
                if (itemstack1 == null && slot.isItemValid(par1ItemStack)) {
                    val l = par1ItemStack.stackSize
                    val maxSize = min(slot.slotStackLimit.toDouble(), par1ItemStack.maxStackSize.toDouble())
                        .toInt()
                    if (l <= maxSize) {
                        slot.putStack(par1ItemStack.copy())
                        slot.onSlotChanged()
                        par1ItemStack.stackSize = 0
                        flag1 = true
                        break
                    } else {
                        par1ItemStack.stackSize -= maxSize
                        val newItemStack = par1ItemStack.copy()
                        newItemStack.stackSize = maxSize
                        slot.putStack(newItemStack)
                        slot.onSlotChanged()
                        flag1 = true
                        break
                    }
                }
                if (par4) {
                    --k
                } else {
                    ++k
                }
            }
        }
        return flag1
    }
}
