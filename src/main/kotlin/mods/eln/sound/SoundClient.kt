package mods.eln.sound

import mods.eln.client.ClientProxy
import mods.eln.client.SoundLoader
import mods.eln.misc.Utils.TraceRayWeightOpaque
import mods.eln.misc.Utils.println
import mods.eln.misc.Utils.traceRay
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.pow
import kotlin.math.sqrt

object SoundClient {
    fun play(p: SoundCommand) {
        ClientProxy.soundClientEventListener.currentUuid = p.uuid

        val player: EntityPlayer = Minecraft.getMinecraft().thePlayer
        if (p.world!!.provider.dimensionId != player.dimension) return
        val distance = sqrt((p.x - player.posX).pow(2.0) + (p.y - player.posY).pow(2.0) + (p.z - player.posZ).pow(2.0))
        if (distance >= p.rangeMax) return
        var distanceFactor = 1f
        if (distance > p.rangeNominal) {
            distanceFactor = ((p.rangeMax - distance) / (p.rangeMax - p.rangeNominal)).toFloat()
        }

        val blockFactor = traceRay(
            p.world!!,
            player.posX,
            player.posY,
            player.posZ,
            p.x,
            p.y,
            p.z,
            TraceRayWeightOpaque()
        ) * p.blockFactor

        val trackCount = SoundLoader.getTrackCount(p.track)

        if (trackCount == 1) {
            val temp = 1.0f / (1 + blockFactor)
            p.volume *= temp.pow(2.0f)
            p.volume *= distanceFactor
            if (p.volume <= 0) return

            p.world!!.playSound(
                player.posX + 2 * (p.x - player.posX) / distance,
                player.posY + 2 * (p.y - player.posY) / distance,
                player.posZ + 2 * (p.z - player.posZ) / distance,
                p.track,
                p.volume,
                p.pitch,
                false
            )
        } else {
            for (idx in 0 until trackCount) {
                var bandVolume = p.volume
                bandVolume *= distanceFactor

                bandVolume -= (((trackCount - 1 - idx) / (trackCount - 1f) + 0.2) * blockFactor).toFloat()
                println(bandVolume)
                p.world!!.playSound(
                    player.posX + 2 * (p.x - player.posX) / distance,
                    player.posY + 2 * (p.y - player.posY) / distance,
                    player.posZ + 2 * (p.z - player.posZ) / distance,
                    p.track + "_" + idx + "x",
                    bandVolume,
                    p.pitch,
                    false
                )
            }
        }

        ClientProxy.soundClientEventListener.currentUuid = null
    }
}
