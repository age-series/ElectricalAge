package mods.eln.misc

import mods.eln.Eln
import mods.eln.Vars
import net.minecraft.entity.player.EntityPlayer

fun EntityPlayer?.isHoldingMeter(): Boolean {
    if (this == null) return false
    val equippedItem = currentEquippedItem
    return (Vars.multiMeterElement.checkSameItemStack(equippedItem)
        || Vars.thermometerElement.checkSameItemStack(equippedItem)
        || Vars.allMeterElement.checkSameItemStack(equippedItem))
}
