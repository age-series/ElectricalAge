package mods.eln.server

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import mods.eln.misc.Utils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import java.util.*

class PlayerManager {
    private val metadataHash: MutableMap<EntityPlayerMP, PlayerMetadata> = Hashtable()

    inner class PlayerMetadata(p: EntityPlayer) {
        private var timeout = 0
        var interactEnable: Boolean = false
            get() {
                timeoutReset()
                return field
            }
            set(interactEnable) {
                if (!this.interactEnable && interactEnable) {
                    interactRiseBuffer = true
                    Utils.println("interactRiseBuffer")
                }
                field = interactEnable
                timeoutReset()
                Utils.println("interactEnable : $interactEnable")
            }
        var interactRise = false
        var interactRiseBuffer = false
        var player: EntityPlayer
        fun needDelete(): Boolean {
            return timeout == 0
        }

        fun timeoutReset() {
            timeout = 20 * 120
        }

        fun timeoutDec() {
            timeout--
            if (timeout < 0) timeout = 0
        }

        init {
            timeoutReset()
            player = p
        }
    }

    fun clear() {
        metadataHash.clear()
    }

    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        for ((key, p) in metadataHash) {
            p.interactRise = p.interactRiseBuffer
            p.interactRiseBuffer = false
            if (p.needDelete()) {
                metadataHash.remove(key)
            }
        }
    }

    operator fun get(player: EntityPlayerMP): PlayerMetadata? {
        val metadata = metadataHash[player]
        if (metadata != null) return metadata
        metadataHash[player] = PlayerMetadata(player)
        return metadataHash[player]
    }

    operator fun get(player: EntityPlayer): PlayerMetadata? {
        return get(player as EntityPlayerMP)
    }

    init {
        FMLCommonHandler.instance().bus().register(this)
    }
}
