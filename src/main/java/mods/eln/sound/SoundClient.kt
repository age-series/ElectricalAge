package mods.eln.sound

import mods.eln.debug.DP
import mods.eln.debug.DPType
import net.minecraft.client.Minecraft

class SoundClient {
    companion object {
        @JvmStatic
        fun play(p: SoundCommand) {
            //val player = Minecraft.getMinecraft().thePlayer
            //if (p.world?.provider?.dimensionId != player.dimension) return
            /*
            val distance = Math.sqrt(Math.pow(p.x - player.posX, 2.0) + Math.pow(p.y - player.posY, 2.0) + Math.pow(p.z - player.posZ, 2.0))
            if (distance >= p.rangeMax) return
            var distanceFactor = 1f
            if (distance > p.rangeNominal) {
                distanceFactor = ((p.rangeMax - distance) / (p.rangeMax - p.rangeNominal)).toFloat()
            }*/
            //val blockFactor = 1.0//Utils.traceRay(p.world, player.posX, player.posY, player.posZ, p.x, p.y, p.z, Utils.TraceRayWeightOpaque()) * p.blockFactor
            //val temp = 1.0f / (1 + blockFactor)
            p.volume *= Math.pow(1.0, 2.0).toFloat()
            DP.println(DPType.SOUND, "${p.volume}")
            //p.volume *= distanceFactor
            if (p.volume <= 0) return
            DP.println(DPType.SOUND, "${p.volume}")
            p.world?.playSound(p.x, p.y, p.z, p.track, p.volume, p.pitch, false)
        }
    }
}
