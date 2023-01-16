package mods.eln.gui

import mods.eln.gui.ISlotSkin.SlotSkin
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

open class SlotWithSkin(
    inventory: IInventory?,
    slotIndex: Int,
    xDisplayPosition: Int,
    yDisplayPosition: Int,
    var skin: SlotSkin
): Slot(
    inventory,
    slotIndex,
    xDisplayPosition,
    yDisplayPosition
), ISlotSkin {
    override fun getSlotSkin(): SlotSkin {
        return skin
    }
}
