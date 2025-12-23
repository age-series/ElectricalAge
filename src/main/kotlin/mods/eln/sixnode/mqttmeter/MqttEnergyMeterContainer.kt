package mods.eln.sixnode.mqttmeter

import mods.eln.gui.ISlotSkin
import mods.eln.misc.BasicContainer
import mods.eln.node.six.SixNodeItemSlot
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import mods.eln.i18n.I18N.tr

class MqttEnergyMeterContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(
    player,
    inventory,
    arrayOf(
        SixNodeItemSlot(
            inventory,
            cableSlotId,
            140,
            80,
            1,
            arrayOf(ElectricalCableDescriptor::class.java, CurrentCableDescriptor::class.java),
            ISlotSkin.SlotSkin.medium,
            arrayOf(tr("Electrical cable slot"))
        )
    )
) {
    companion object {
        const val cableSlotId = 0
    }
}
