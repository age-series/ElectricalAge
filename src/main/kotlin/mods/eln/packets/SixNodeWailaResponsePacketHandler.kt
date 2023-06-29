package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.integration.waila.SixNodeCoordonate
import mods.eln.integration.waila.SixNodeWailaData
import mods.eln.integration.waila.WailaCache
import mods.eln.misc.Coordinate

class SixNodeWailaResponsePacketHandler : IMessageHandler<SixNodeWailaResponsePacket, IMessage> {

    private fun Coordinate.isNull() = this.x == 0 && this.y == 0 && this.z == 0 && this.dimension == 0

    override fun onMessage(message: SixNodeWailaResponsePacket, ctx: MessageContext?): IMessage? {
        if (!message.coord.isNull()) {
            WailaCache.sixNodes.put(SixNodeCoordonate(message.coord, message.side),
                SixNodeWailaData(message.itemStack, message.map))
        }

        return null
    }
}
