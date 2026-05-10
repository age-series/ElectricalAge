package mods.eln.sixnode.lampsocket

import mods.eln.cable.CableItemSlot
import mods.eln.item.lampitem.LampItemSlot
import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class LampSocketContainer(player: EntityPlayer, inventory: IInventory, descriptor: LampSocketDescriptor) :
    BasicContainer(
        player, inventory, arrayOf(
            LampItemSlot(inventory, LAMP_SLOT_ID, 16 + 1, 59 + 1, 1, descriptor.acceptedLampTypes),
            CableItemSlot(inventory, CABLE_SLOT_ID, 142 + 1, 59 + 1, 1, false, REQUIRED_CABLE_LENGTH)
        )
    ) {

    companion object {
        const val LAMP_SLOT_ID = 0
        const val CABLE_SLOT_ID = 1

        // This applies only for utility cables
        const val REQUIRED_CABLE_LENGTH = 1.0
    }

}