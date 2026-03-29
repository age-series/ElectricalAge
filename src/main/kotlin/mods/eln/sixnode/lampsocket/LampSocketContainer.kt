package mods.eln.sixnode.lampsocket

import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.LampLists
import mods.eln.gui.LampItemSlot
import mods.eln.misc.BasicContainer
import mods.eln.node.six.SixNodeItemSlot
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class LampSocketContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(
    player, inventory, arrayOf(
        LampItemSlot(inventory, LAMP_SLOT_ID, 70+1, 57+1, 1, ACCEPTED_LAMP_TYPES),
        SixNodeItemSlot(
            inventory, CABLE_SLOT_ID, 88+1, 57+1, 1, arrayOf(
                ElectricalCableDescriptor::class.java,
                CurrentCableDescriptor::class.java
            ), SlotSkin.medium, arrayOf(tr("Electrical/current cable slot"))
        )
    )
) {

    companion object {
        const val LAMP_SLOT_ID: Int = 0
        const val CABLE_SLOT_ID: Int = 1

        @JvmField
        val ACCEPTED_LAMP_TYPES = arrayOf(
            LampLists.getLampData("incandescent")!!,
            LampLists.getLampData("carbonIncandescent")!!,
            LampLists.getLampData("fluorescent")!!,
            LampLists.getLampData("farming")!!,
            LampLists.getLampData("led")!!
        )
    }

}