package mods.eln.cable

import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import mods.eln.node.six.SixNodeItemSlot
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

/**
 * This is a temporary class implemented only to keep signal cables (a subtype of regular electrical cables) from being
 * accepted in the inventories of various devices. After the cable update, this class shouldn't be necessary unless
 * there are specific families of cables that should be accepted/not accepted in certain inventories. See
 * LampItemSlot.kt for an idea of how to implement this behavior in the future, if needed.
 */
class CableItemSlot(inventory: IInventory, slot: Int, x: Int, y: Int, stackLimit: Int, val acceptSignalCable: Boolean) :
    SixNodeItemSlot(
        inventory, slot, x, y, stackLimit, arrayOf(
            ElectricalCableDescriptor::class.java, CurrentCableDescriptor::class.java
        ), ISlotSkin.SlotSkin.medium, arrayOf(I18N.tr("Cable slot"))
    ) {

    override fun isItemValid(itemStack: ItemStack): Boolean {
        if (!super.isItemValid(itemStack)) return false

        val cableStack = Utils.getItemObject(itemStack)
        return if (cableStack is ElectricalCableDescriptor) !(cableStack.signalWire && !acceptSignalCable) else true
    }

}