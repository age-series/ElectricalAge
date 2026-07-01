package mods.eln.eventhandlers

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import mods.eln.packets.AchievePacket
import mods.eln.wiki.Root
import net.minecraftforge.client.event.GuiOpenEvent

class ElnForgeEventsHandler {
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    fun openGuide(event: GuiOpenEvent) {
        if (event.gui is Root) {
            Eln.elnNetwork.sendToServer(openWikiPacket)
        }
    }

    companion object {
        private val openWikiPacket = AchievePacket("openWiki")
    }
}
