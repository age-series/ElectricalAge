package mods.eln.node.six

import mods.eln.Eln
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkinAndComment
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class SixNodeItemSlot(
    inventory: IInventory?, slot: Int,
    x: Int, y: Int,
    var stackLimit: Int,
    var descriptorClassList: Array<Class<*>>, skin: SlotSkin, comment: Array<String>
) : SlotWithSkinAndComment(inventory, slot, x, y, skin, comment) {
    /**
     * Check if the stack is a valid item for this slot. Always true beside for the armor slots.
     */
    override fun isItemValid(itemStack: ItemStack): Boolean {
        if (itemStack.item !== Eln.sixNodeItem) return false
        val descriptor = Eln.sixNodeItem.getDescriptor(itemStack)
        for (classFilter in descriptorClassList) {
            if (descriptor!!.javaClass == classFilter) return true
        }
        return false
    }

    override fun getSlotStackLimit(): Int {
        return stackLimit
    }
}
