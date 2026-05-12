package mods.eln.cable

import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import mods.eln.node.six.SixNodeItemSlot
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import kotlin.math.abs

/**
 * This class should be used going forward for all devices which accept cables in their inventory. It provides the
 * ability to only accept utility cable spools of a certain length, as well as prevent legacy signal cables from being
 * accepted in inventories where they do not make sense.
 */
class CableItemSlot(
    inventory: IInventory,
    slot: Int,
    x: Int,
    y: Int,
    stackLimit: Int,
    val acceptSignalCable: Boolean,
    val expectedCableLength: Double,
    comment: Array<String> = arrayOf(I18N.tr("Cable slot"))
) : SixNodeItemSlot(
    inventory, slot, x, y, stackLimit, arrayOf(
        ElectricalCableDescriptor::class.java, CurrentCableDescriptor::class.java, UtilityCableDescriptor::class.java
    ), ISlotSkin.SlotSkin.medium, comment
) {

    override fun isItemValid(itemStack: ItemStack): Boolean {
        if (!super.isItemValid(itemStack)) return false

        return when (val cableDescriptor = Utils.getItemObject(itemStack)) {
            is UtilityCableDescriptor -> abs(cableDescriptor.getRemainingLengthMeters(itemStack) - expectedCableLength) < UtilityCableDescriptor.LENGTH_METERS_EPSILON
            is ElectricalCableDescriptor -> !(cableDescriptor.signalWire && !acceptSignalCable)
            is CurrentCableDescriptor -> true
            else -> false
        }
    }

}