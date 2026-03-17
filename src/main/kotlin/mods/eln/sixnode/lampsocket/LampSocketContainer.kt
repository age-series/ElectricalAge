package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.misc.BasicContainer
import mods.eln.node.six.SixNodeItemSlot
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class LampSocketContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf(
        LampSlot(inventory, LAMP_SLOT_ID, 70+0, 57, 1, ACCEPTED_LAMP_TYPES),
        SixNodeItemSlot(inventory, CABLE_SLOT_ID, 70+18, 57, 1, arrayOf(ElectricalCableDescriptor::class.java),
            SlotSkin.medium, arrayOf(tr("Electrical cable slot"))
        )
)) {

    companion object {
        const val LAMP_SLOT_ID: Int = 0
        const val CABLE_SLOT_ID: Int = 1

        @JvmField
        val ACCEPTED_LAMP_TYPES = arrayOf(
            Eln.lampLists.getLampData("incandescent")!!,
            Eln.lampLists.getLampData("carbonIncandescent")!!,
            Eln.lampLists.getLampData("fluorescent")!!,
            Eln.lampLists.getLampData("farming")!!,
            Eln.lampLists.getLampData("led")!!
        )
    }

}