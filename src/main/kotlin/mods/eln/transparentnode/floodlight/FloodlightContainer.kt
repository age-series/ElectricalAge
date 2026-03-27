package mods.eln.transparentnode.floodlight

import mods.eln.item.LampLists
import mods.eln.misc.BasicContainer
import mods.eln.gui.LampItemSlot
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class FloodlightContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(
    player, inventory, arrayOf(
        LampItemSlot(inventory, LAMP_SLOT_1_ID, 70 + 1, 61 + 1, 1, ACCEPTED_LAMP_TYPES),
        LampItemSlot(inventory, LAMP_SLOT_2_ID, 88 + 1, 61 + 1, 1, ACCEPTED_LAMP_TYPES)
    )
) {

    companion object {
        const val LAMP_SLOT_1_ID: Int = 0
        const val LAMP_SLOT_2_ID: Int = 1

        val ACCEPTED_LAMP_TYPES = arrayOf(LampLists.getLampData("halogen")!!)
    }

}