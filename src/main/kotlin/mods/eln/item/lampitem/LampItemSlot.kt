package mods.eln.item.lampitem

import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class LampItemSlot(inventory: IInventory, slot: Int, x: Int, y: Int, stackLimit: Int, val acceptedLampTypes: Array<BoilerplateLampData>) :
    GenericItemUsingDamageSlot(
        inventory, slot, x, y, stackLimit, LampDescriptor::class.java,
        ISlotSkin.SlotSkin.medium, arrayOf(I18N.tr("Lamp slot"))
    ) {

    override fun isItemValid(itemStack: ItemStack): Boolean {
        return if (!super.isItemValid(itemStack)) false
        else (Utils.getItemObject(itemStack) as LampDescriptor).lampData.technology in acceptedLampTypes
    }

}