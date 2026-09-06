package mods.eln.sixnode.lampsupply

import mods.eln.cable.CableItemSlot
import mods.eln.i18n.I18N
import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class LampSupplyContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(
    player, inventory, arrayOf(
        CableItemSlot(
            inventory,
            CABLE_SLOT_ID,
            184,
            144,
            64,
            false,
            REQUIRED_CABLE_LENGTH,
            I18N.tr("Cable slot\nBase range is 32 blocks.\nEach additional cable\nincreases range by one.")
                .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
    )
) {

    companion object {
        const val CABLE_SLOT_ID = 0

        // This applies only for utility cables
        const val REQUIRED_CABLE_LENGTH = 1.0
    }

}