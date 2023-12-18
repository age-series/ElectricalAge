package mods.eln.sound

import cpw.mods.fml.common.FMLCommonHandler
import mods.eln.Eln
import mods.eln.misc.Utils.sendPacketToClient
import net.minecraft.entity.player.EntityPlayerMP
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

object SoundServer {
    fun play(p: SoundCommand) {
        val bos = ByteArrayOutputStream(64)
        val stream = DataOutputStream(bos)
        try {
            stream.writeByte(Eln.packetPlaySound.toInt())
            stream.writeByte(p.world!!.provider.dimensionId)
            p.writeTo(stream)
            val server = FMLCommonHandler.instance().minecraftServerInstance
            for (obj in server.configurationManager.playerEntityList) {
                val player = obj as EntityPlayerMP
                if (player.dimension == p.world!!.provider.dimensionId && player.getDistance(
                        p.x,
                        p.y,
                        p.z
                    ) < p.rangeMax + 2
                ) {
                    sendPacketToClient(bos, player)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
