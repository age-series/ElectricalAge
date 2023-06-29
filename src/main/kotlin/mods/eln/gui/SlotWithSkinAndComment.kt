package mods.eln.gui

import mods.eln.gui.ISlotSkin.SlotSkin
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

open class SlotWithSkinAndComment(
    inventory: IInventory?,
    slotIndex: Int,
    xDisplayPosition: Int,
    yDisplayPosition: Int,
    var skin: SlotSkin,
    var comment: Array<String>
) : Slot(
    inventory,
    slotIndex,
    xDisplayPosition,
    yDisplayPosition
), ISlotSkin, ISlotWithComment {
    override fun getSlotSkin(): SlotSkin {
        return skin
    }

    override fun getComment(list: MutableList<String>) {
        list.addAll(comment)
    }
}
