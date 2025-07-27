package mods.eln.transparentnode.floodlight

import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.LampDescriptor
import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class FloodlightContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf(
    GenericItemUsingDamageSlot(inventory, LAMP_SLOT_1_ID, 70+1, 61+1, 1,
        arrayOf<Class<*>>(LampDescriptor::class.java), SlotSkin.medium, arrayOf(tr("Lamp slot 1"))),
    GenericItemUsingDamageSlot(inventory, LAMP_SLOT_2_ID, 88+1, 61+1, 1,
        arrayOf<Class<*>>(LampDescriptor::class.java), SlotSkin.medium, arrayOf(tr("Lamp slot 2"))))) {

    companion object {
        const val LAMP_SLOT_1_ID: Int = 0
        const val LAMP_SLOT_2_ID: Int = 1
    }

}