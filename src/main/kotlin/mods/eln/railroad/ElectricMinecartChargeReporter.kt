package mods.eln.railroad

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import net.minecraftforge.event.entity.player.PlayerInteractEvent

class ElectricMinecartChargeReporter {

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.entityPlayer ?: return
        val world = player.worldObj
        if (world == null || world.isRemote) return

        when (event.action) {
            PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
            PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK -> {
                val minecart = player.ridingEntity as? EntityElectricMinecart ?: return
                val heldItem = player.currentEquippedItem ?: return
                val multiMeter = Eln.multiMeterElement
                val allMeter = Eln.allMeterElement
                val holdingMeter = (multiMeter != null && multiMeter.checkSameItemStack(heldItem)) ||
                        (allMeter != null && allMeter.checkSameItemStack(heldItem))
                if (!holdingMeter) {
                    return
                }
                val message = I18N.tr("Cart Energy: ") + Utils.plotEnergy(minecart.energyBufferJoules)
                Utils.addChatMessage(player, message)
            }
            else -> return
        }
    }
}
