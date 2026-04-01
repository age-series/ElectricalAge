package mods.eln.item

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils.addChatMessage
import mods.eln.server.ElnDestroyHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.world.World

private const val NOPE_WAND_RADIUS = 10

class NopeWandDescriptor(name: String) : GenericItemUsingDamageDescriptor(name, "nopewand") {
    override fun onItemRightClick(s: ItemStack, w: World, p: EntityPlayer): ItemStack {
        if (w.isRemote || p !is EntityPlayerMP) return s

        val summary = ElnDestroyHelper.destroyAroundPlayer(w, p, NOPE_WAND_RADIUS)
        if (summary == null) {
            addChatMessage(p, tr("The Nope Wand fizzles: node manager unavailable."))
            return s
        }

        addChatMessage(
            p,
            tr(
                "The Nope Wand removed %1$ ELN nodes and cleared %2$ ELN blocks in a %3$-block radius.",
                summary.nodesDestroyed,
                summary.blocksCleared,
                NOPE_WAND_RADIUS
            )
        )
        return s
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Right click to immediately remove ELN nodes and blocks in a 10-block radius."))
        list.add(tr("Removes them without dropping items, much like /eln zonedestroy."))
    }
}
