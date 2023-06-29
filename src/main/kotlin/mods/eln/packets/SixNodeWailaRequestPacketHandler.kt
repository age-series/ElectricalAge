package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.misc.Utils
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import net.minecraft.item.ItemStack

class SixNodeWailaRequestPacketHandler : IMessageHandler<SixNodeWailaRequestPacket, SixNodeWailaResponsePacket> {
    override fun onMessage(message: SixNodeWailaRequestPacket, ctx: MessageContext?): SixNodeWailaResponsePacket {
        val coord = message.coord
        val side = message.side
        val node = NodeManager.instance!!.getNodeFromCoordonate(coord) as? SixNode
        var stringMap: Map<String, String> = emptyMap()
        var itemStack: ItemStack? = null
        if (node != null) {
            val element = node.getElement(side)
            if (element != null) {
                if (element.getWaila() == null) {
                    Utils.println("Waila got a null getWaila() descriptor for ${element.sixNodeElementDescriptor.name}")
                }
                stringMap = element.getWaila()?.filter { true }?: mapOf()
                itemStack = element.sixNodeElementDescriptor.newItemStack()
            }
        }
        return SixNodeWailaResponsePacket(coord, side, itemStack, stringMap)
    }
}
