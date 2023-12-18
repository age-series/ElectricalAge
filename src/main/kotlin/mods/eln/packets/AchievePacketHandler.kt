package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.Achievements

class AchievePacketHandler : IMessageHandler<AchievePacket?, IMessage?> {

    override fun onMessage(message: AchievePacket?, ctx: MessageContext?): IMessage? {
        if (message == null || ctx == null) return null
        //System.out.println("Got message: " + message.text);
        if (message.text == "openWiki") {
            ctx.serverHandler.playerEntity.triggerAchievement(Achievements.openGuide)
        } else if (message.text == "craft50VMacerator") {
            ctx.serverHandler.playerEntity.triggerAchievement(Achievements.craft50VMacerator)
        } else {
            println("[ELN]: ELN Wiki Achievement Handler has received an invalid message/packet: " + message.text)
        }
        return null
    }
}
