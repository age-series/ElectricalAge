package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.Eln
import mods.eln.misc.Utils
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode

/**
 * Created by Gregory Maddra on 2016-06-27.
 */
class TransparentNodeRequestPacketHandler : IMessageHandler<TransparentNodeRequestPacket, TransparentNodeResponsePacket> {
    override fun onMessage(message: TransparentNodeRequestPacket?, ctx: MessageContext?): TransparentNodeResponsePacket? {
        var c = message!!.coord
        val ghostElem = Eln.ghostManager.getGhost(c)
        if(ghostElem != null) c = ghostElem.observatorCoordonate!!
        val node = NodeManager.instance!!.getNodeFromCoordonate(c) as? TransparentNode
        var stringMap: Map<String, String> = emptyMap()
        if (node != null) {
            try {
                stringMap = node.element!!.getWaila()
            } catch (e: NullPointerException) {
                Utils.println("Attempted to get WAILA info for an invalid node!")
                e.printStackTrace()
                return null
            }
        }
        return TransparentNodeResponsePacket(stringMap, c)
    }
}
