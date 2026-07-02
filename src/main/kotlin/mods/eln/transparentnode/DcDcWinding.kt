package mods.eln.transparentnode

import mods.eln.gui.ISlotSkin
import mods.eln.gui.ISlotWithComment
import mods.eln.gui.SlotWithSkin
import mods.eln.i18n.I18N.tr
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import kotlin.math.abs
import kotlin.math.ceil

private const val MAX_RENDERED_WINDINGS = 16

internal data class DcDcWinding(
    val descriptor: ElectricalCableDescriptor,
    val amount: Double
)

internal fun dcDcWinding(stack: ItemStack?): DcDcWinding? {
    if (stack == null) return null
    val descriptor = ElectricalCableDescriptor.getDescriptor(
        stack,
        ElectricalCableDescriptor::class.java
    ) as? ElectricalCableDescriptor ?: return null
    if (descriptor.signalWire) return null
    if (descriptor is UtilityCableDescriptor && descriptor.melted) return null

    val amount = if (descriptor is UtilityCableDescriptor) {
        descriptor.getRemainingLengthMeters(stack)
    } else {
        stack.stackSize.toDouble()
    }
    if (!amount.isFinite() || amount <= UtilityCableDescriptor.LENGTH_METERS_EPSILON) return null
    return DcDcWinding(descriptor, amount)
}

private fun dcDcWindingRenderAmount(stack: ItemStack?): Double? {
    if (stack == null) return null
    val descriptor = ElectricalCableDescriptor.getDescriptor(
        stack,
        ElectricalCableDescriptor::class.java
    ) as? ElectricalCableDescriptor ?: return null
    if (descriptor.signalWire) return null
    val amount = if (descriptor is UtilityCableDescriptor) {
        descriptor.getRemainingLengthMeters(stack)
    } else {
        stack.stackSize.toDouble()
    }
    return amount.takeIf { it.isFinite() && it > UtilityCableDescriptor.LENGTH_METERS_EPSILON }
}

internal fun dcDcWindingMeltCurrent(stack: ItemStack?): Double {
    val descriptor = dcDcWinding(stack)?.descriptor ?: return 5.0
    return descriptor.electricalMaximalCurrent.takeIf { it.isFinite() && it > 0.0 } ?: 5.0
}

internal fun meltDcDcWindingIfOverCurrent(inventory: IInventory, slot: Int, current: Double): Boolean {
    val stack = inventory.getStackInSlot(slot) ?: return false
    val descriptor = dcDcWinding(stack)?.descriptor as? UtilityCableDescriptor ?: return false
    val limit = dcDcWindingMeltCurrent(stack)
    if (abs(current) <= limit) return false

    val melted = descriptor.meltedDescriptor ?: return false
    val replacement = melted.newItemStack(1)
    melted.setRemainingLengthMeters(replacement, descriptor.getRemainingLengthMeters(stack))
    inventory.setInventorySlotContents(slot, replacement)
    inventory.markDirty()
    return true
}

internal fun dcDcRenderedWindingCount(stack: ItemStack?): Int {
    val amount = dcDcWindingRenderAmount(stack) ?: return 0
    return ceil(amount).toInt().coerceIn(1, MAX_RENDERED_WINDINGS)
}

internal class DcDcWindingSlot(
    inventory: IInventory,
    slot: Int,
    x: Int,
    y: Int,
    private val stackLimit: Int,
    private val comment: Array<String> = arrayOf(tr("Power cable or wire slot"))
) : SlotWithSkin(inventory, slot, x, y, ISlotSkin.SlotSkin.medium), ISlotWithComment {
    override fun isItemValid(itemStack: ItemStack): Boolean {
        return dcDcWinding(itemStack) != null
    }

    override fun getSlotStackLimit(): Int {
        return stackLimit
    }

    override fun getComment(list: MutableList<String>) {
        comment.forEach(list::add)
    }
}
