package mods.eln.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import mods.eln.Achievements;
import mods.eln.debug.DP;
import mods.eln.debug.DPType;

public class AchievePacketHandler implements IMessageHandler<AchievePacket, IMessage> {

    @Override
    public IMessage onMessage(AchievePacket message, MessageContext ctx) {
        //System.out.println("Got message: " + message.text);
        if (message.text.equals("craft50VMacerator")) {
            ctx.getServerHandler().playerEntity.triggerAchievement(Achievements.craft50VMacerator);
        } else {
            DP.println(DPType.NETWORK, "ELN Wiki Achievement Handler has received an invalid message/packet: " + message.text);
        }
        return null;
    }
}
