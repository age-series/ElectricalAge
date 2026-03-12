package mods.eln.sixnode.lampsocket

import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.LampDescriptor
import mods.eln.misc.Utils.getItemObject
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class LampSlot(inventory: IInventory, slot: Int, x: Int, y: Int, stackLimit: Int, val acceptedTechnology: Array<LampDescriptor.Technology>) :
    GenericItemUsingDamageSlot(inventory, slot, x, y, stackLimit, LampDescriptor::class.java, SlotSkin.medium, arrayOf(tr("Lamp slot"))) {

    override fun isItemValid(itemStack: ItemStack): Boolean {
        return if (!super.isItemValid(itemStack)) false
        else (getItemObject(itemStack) as LampDescriptor?)?.technology in acceptedTechnology
    }

}