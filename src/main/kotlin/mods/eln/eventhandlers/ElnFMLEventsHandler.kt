package mods.eln.eventhandlers

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent
import mods.eln.Eln
import mods.eln.packets.AchievePacket

class ElnFMLEventsHandler {
    @SubscribeEvent
    fun onCraft(event: ItemCraftedEvent) {
        if (event.crafting.unlocalizedName.lowercase() == "48v_macerator") {
            Eln.elnNetwork.sendToServer(craft50VMaceratorPacket)
        }
    }

    companion object {
        private val craft50VMaceratorPacket = AchievePacket("craft50VMacerator")
    }
}
