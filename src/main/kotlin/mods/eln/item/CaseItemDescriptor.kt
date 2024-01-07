package mods.eln.item

import mods.eln.i18n.I18N.tr
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class CaseItemDescriptor(name: String) : GenericItemUsingDamageDescriptorUpgrade(name) {
    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Can be used to encase EA items that support it"))
    }
}
