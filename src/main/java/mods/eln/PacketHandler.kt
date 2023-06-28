package mods.eln

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent
import io.netty.channel.ChannelHandler.Sharable
import mods.eln.client.ClientKeyHandler
import mods.eln.client.ClientProxy
import mods.eln.misc.Coordinate
import mods.eln.misc.Utils.println
import mods.eln.misc.Utils.sendPacketToClient
import mods.eln.node.INodeEntity
import mods.eln.node.NodeManager
import mods.eln.sound.SoundClient
import mods.eln.sound.SoundCommand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.NetworkManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

@Sharable
class PacketHandler {
    @SubscribeEvent
    fun onServerPacket(event: ServerCustomPacketEvent) {
        val packet = event.packet
        val stream = DataInputStream(ByteArrayInputStream(packet.payload().array()))
        val manager = event.manager
        val player: EntityPlayer = (event.handler as NetHandlerPlayServer).playerEntity // EntityPlayerMP
        packetRx(stream, manager, player)
    }

    fun packetRx(stream: DataInputStream, manager: NetworkManager, player: EntityPlayer) {
        try {
            when (stream.readByte()) {
                Eln.packetPlayerKey -> packetPlayerKey(stream, manager, player)
                Eln.packetNodeSingleSerialized -> packetNodeSingleSerialized(stream, manager, player)
                Eln.packetPublishForNode -> packetForNode(stream, manager, player)
                Eln.packetForClientNode -> packetForClientNode(stream, manager, player)
                Eln.packetOpenLocalGui -> packetOpenLocalGui(stream, manager, player)
                Eln.packetPlaySound -> packetPlaySound(stream, manager, player)
                Eln.packetDestroyUuid -> packetDestroyUuid(stream, manager, player)
                Eln.packetClientToServerConnection -> packetNewClient(manager, player)
                Eln.packetServerToClientInfo -> packetServerInfo(stream, manager, player)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun packetNewClient(@Suppress("UNUSED_PARAMETER") manager: NetworkManager, player: EntityPlayer) {
        val bos = ByteArrayOutputStream(64)
        val stream = DataOutputStream(bos)
        try {
            stream.writeByte(Eln.packetServerToClientInfo.toInt())
            for (c in Eln.instance.configShared) {
                c.serializeConfig(stream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        sendPacketToClient(bos, (player as EntityPlayerMP))
    }

    private fun packetServerInfo(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, @Suppress("UNUSED_PARAMETER") player: EntityPlayer) {
        for (c in Eln.instance.configShared) {
            try {
                c.deserialize(stream)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun packetDestroyUuid(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, @Suppress("UNUSED_PARAMETER") player: EntityPlayer) {
        try {
            ClientProxy.uuidManager.kill(stream.readInt())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun packetPlaySound(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, player: EntityPlayer) {
        try {
            if (stream.readByte().toInt() != player.dimension) return
            SoundClient.play(SoundCommand.fromStream(stream, player.worldObj))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun packetOpenLocalGui(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, player: EntityPlayer) {
        try {
            player.openGui(Eln.instance, stream.readInt(),
                player.worldObj, stream.readInt(), stream.readInt(),
                stream.readInt())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun packetForNode(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, player: EntityPlayer?) {
        try {
            val coordinate = Coordinate(stream.readInt(), stream.readInt(), stream.readInt(), stream.readByte().toInt())
            val node = NodeManager.instance!!.getNodeFromCoordonate(coordinate)
            if (node != null && node.nodeUuid == stream.readUTF()) {
                node.networkUnserialize(stream, player as EntityPlayerMP?)
            } else {
                println("packetForNode node found")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun packetForClientNode(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, player: EntityPlayer) {
        try {
            val x = stream.readInt()
            val y = stream.readInt()
            val z = stream.readInt()
            val dimension = stream.readByte().toInt()
            if (player.dimension == dimension) {
                val entity = player.worldObj.getTileEntity(x, y, z)
                if (entity != null && entity is INodeEntity) {
                    val node = entity as INodeEntity
                    if (node.nodeUuid == stream.readUTF()) {
                        node.serverPacketUnserialize(stream)
                        if (0 != stream.available()) {
                            println("0 != stream.available()")
                        }
                    } else {
                        println("Wrong node UUID warning")
                        val dataSkipLength = stream.readByte().toInt()
                        for (idx in 0 until dataSkipLength) {
                            stream.readByte()
                        }
                    }
                }
            } else println("No node found for $x $y $z")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun packetNodeSingleSerialized(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, player: EntityPlayer) {
        try {
            val x: Int = stream.readInt()
            val y: Int = stream.readInt()
            val z: Int = stream.readInt()
            val dimension: Int = stream.readByte().toInt()
            if (player.dimension == dimension) {
                val entity = player.worldObj.getTileEntity(x, y, z)
                if (entity != null && entity is INodeEntity) {
                    val node = entity as INodeEntity
                    if (node.nodeUuid == stream.readUTF()) {
                        node.serverPublishUnserialize(stream)
                        if (0 != stream.available()) {
                            println("0 != stream.available()")
                        }
                    } else {
                        println("Wrong node UUID warning")
                        val dataSkipLength = stream.readByte().toInt()
                        for (idx in 0 until dataSkipLength) {
                            stream.readByte()
                        }
                    }
                } else println("No node found for $x $y $z")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun packetPlayerKey(stream: DataInputStream, @Suppress("UNUSED_PARAMETER") manager: NetworkManager, player: EntityPlayer?) {
        val playerMP = player as EntityPlayerMP?
        val id: Byte
        try {
            id = stream.readByte()
            val state = stream.readBoolean()
            if (id.toInt() == ClientKeyHandler.wrenchId) {
                val metadata = Eln.playerManager[playerMP!!]
                metadata!!.interactEnable = state
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    init {
        Eln.eventChannel.register(this)
    }
}
