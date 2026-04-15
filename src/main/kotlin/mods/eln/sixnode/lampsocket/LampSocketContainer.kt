package mods.eln.sixnode.lampsocket

import mods.eln.cable.CableItemSlot
import mods.eln.item.lampitem.LampItemSlot
import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class LampSocketContainer(player: EntityPlayer, inventory: IInventory, descriptor: LampSocketDescriptor) :
    BasicContainer(player, inventory, arrayOf(
        LampItemSlot(inventory, LAMP_SLOT_ID, 70+1, 57+1, 1, descriptor.acceptedLampTypes),
        CableItemSlot(inventory, CABLE_SLOT_ID, 88+1, 57+1, 1, false)
    )) {

    companion object {
        const val LAMP_SLOT_ID: Int = 0
        const val CABLE_SLOT_ID: Int = 1
    }

}