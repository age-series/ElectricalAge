package mods.eln.railroad

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromInt
import mods.eln.misc.Utils.fatal
import mods.eln.node.INodeEntity
import mods.eln.node.NodeManager
import mods.eln.server.DelayedBlockRemove
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S3FPacketCustomPayload
import net.minecraft.tileentity.TileEntity
import java.io.DataInputStream

/**
 * Tile entity that mirrors [SimpleNodeEntity] behaviour but is tailored
 * for the rail block so it can host a [ThirdRailNode].
 */
class ThirdRailTileEntity : TileEntity(), INodeEntity {

    var node: ThirdRailNode? = null
        get() {
            if (worldObj.isRemote) {
                fatal()
                return null
            }
            if (worldObj == null) return null
            if (field == null) {
                field = NodeManager.instance!!.getNodeFromCoordonate(Coordinate(xCoord, yCoord, zCoord, worldObj)) as ThirdRailNode?
                if (field == null) {
                    DelayedBlockRemove.add(Coordinate(xCoord, yCoord, zCoord, worldObj))
                    return null
                }
            }
            return field
        }

    override val nodeUuid: String
        get() = ThirdRailNode.NODE_UUID

    var front: Direction? = null

    override fun serverPublishUnserialize(stream: DataInputStream) {
        try {
            val newFront = fromInt(stream.readByte().toInt())
            if (newFront != front) {
                front = newFront
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun serverPacketUnserialize(stream: DataInputStream) {}

    override fun getDescriptionPacket(): Packet? {
        val node = node ?: return null
        return S3FPacketCustomPayload(Eln.channelName, node.publishPacket!!.toByteArray())
    }

    fun onBlockAdded() {
        if (!worldObj.isRemote && node == null) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord)
        }
    }

    fun onBreakBlock() {
        if (!worldObj.isRemote) {
            node?.onBreakBlock()
        }
    }

    fun onNeighborBlockChange() {
        if (!worldObj.isRemote) {
            node?.onNeighborBlockChange()
        }
    }

    fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (!worldObj.isRemote) {
            val node = node ?: return false
            node.onBlockActivated(entityPlayer, side!!, vx, vy, vz)
        }
        return true
    }

    override fun onChunkUnload() {
        super.onChunkUnload()
        if (worldObj.isRemote) {
            destructor()
        }
    }

    override fun invalidate() {
        if (worldObj.isRemote) {
            destructor()
        }
        super.invalidate()
    }

    fun destructor() {}

    @SideOnly(Side.CLIENT)
    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? = null

    override fun newContainer(side: Direction, player: EntityPlayer): Container? = null

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        if (nbt.hasKey("thirdRailFront")) {
            front = fromInt(nbt.getByte("thirdRailFront").toInt())
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        if (front != null) {
            nbt.setByte("thirdRailFront", front!!.int.toByte())
        }
    }

}
