package mods.eln.transparentnode.floodlight

import mods.eln.item.LampDescriptor
import mods.eln.misc.BasicContainer
import mods.eln.sixnode.lampsocket.LampSlot
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class FloodlightContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf(
    LampSlot(inventory, LAMP_SLOT_1_ID, 70+1, 61+1, 1, ACCEPTED_LAMP_TECHNOLOGY),
    LampSlot(inventory, LAMP_SLOT_2_ID, 88+1, 61+1, 1, ACCEPTED_LAMP_TECHNOLOGY)
)) {

    companion object {
        const val LAMP_SLOT_1_ID: Int = 0
        const val LAMP_SLOT_2_ID: Int = 1

        val ACCEPTED_LAMP_TECHNOLOGY = arrayOf(LampDescriptor.Technology.HALOGEN)
    }

}