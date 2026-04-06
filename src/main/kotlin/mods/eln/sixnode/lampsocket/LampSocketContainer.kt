package mods.eln.sixnode.lampsocket

import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N
import mods.eln.item.lampitem.LampItemSlot
import mods.eln.misc.BasicContainer
import mods.eln.node.six.SixNodeItemSlot
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class LampSocketContainer(player: EntityPlayer, inventory: IInventory, descriptor: LampSocketDescriptor) :
    BasicContainer(player, inventory, arrayOf(
        LampItemSlot(inventory, LAMP_SLOT_ID, 70+1, 57+1, 1, descriptor.acceptedLampTypes),
        SixNodeItemSlot(inventory, CABLE_SLOT_ID, 88+1, 57+1, 1, arrayOf(ElectricalCableDescriptor::class.java,
            CurrentCableDescriptor::class.java), SlotSkin.medium, arrayOf(I18N.tr("Cable slot"))
        )
    )) {

    companion object {
        const val LAMP_SLOT_ID: Int = 0
        const val CABLE_SLOT_ID: Int = 1
    }

}