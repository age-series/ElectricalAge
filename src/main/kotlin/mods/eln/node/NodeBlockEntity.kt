package mods.eln.node

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.Utils.fatal
import mods.eln.misc.Utils.notifyNeighbor
import mods.eln.misc.Utils.println
import mods.eln.misc.UtilsClient
import mods.eln.server.DelayedBlockRemove.Companion.add
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S3FPacketCustomPayload
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.EnumSkyBlock
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

abstract class NodeBlockEntity : TileEntity(), ITileEntitySpawnClient, INodeEntity {
    val block: NodeBlock
        get() = getBlockType() as NodeBlock
    var redstone = false
    var lastLight = 0xFF
    var firstUnserialize = true
    override fun serverPublishUnserialize(stream: DataInputStream) {
        var light = 0
        try {
            if (firstUnserialize) {
                firstUnserialize = false
                notifyNeighbor(this)
            }
            val b = stream.readByte()
            light = b.toInt() and 0xF
            val newRedstone = b.toInt() and 0x10 != 0
            if (redstone != newRedstone) {
                redstone = newRedstone
                worldObj.notifyBlockChange(xCoord, yCoord, zCoord, getBlockType())
            } else {
                redstone = newRedstone
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        /*	if(lastLight == 0xFF) //boot trololol
        {
			lastLight = 15;
			worldObj.updateLightByType(EnumSkyBlock.Block,xCoord,yCoord,zCoord);
		}*/if (lastLight != light) {
            lastLight = light
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord)
        }
    }

    override fun serverPacketUnserialize(stream: DataInputStream) {}

    abstract fun isProvidingWeakPower(side: Direction?): Int

    var internalNode: Node? = null

    val node: Node?
        get() {
            if (worldObj.isRemote) {
                fatal()
            }
            if (internalNode == null) {
                val nodeFromCoordonate = NodeManager.instance!!.getNodeFromCoordonate(Coordinate(xCoord, yCoord, zCoord, worldObj))
                if (nodeFromCoordonate is Node) {
                    internalNode = nodeFromCoordonate
                } else {
                    println("ASSERT WRONG TYPE public Node getNode " + Coordinate(xCoord, yCoord, zCoord, worldObj))
                }
                if (internalNode == null) {
                    Utils.println("This is actually used?")
                    add(Coordinate(xCoord, yCoord, zCoord, worldObj))
                }
            }
            return internalNode
        }

    override fun newContainer(side: Direction, player: EntityPlayer): Container? {
        return null
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? {
        // Debugging tip: If the GUI isn't working, but you can see it trying to open in the client debug log,
        // check that you have the renderer (client) class set correctly in the descriptor
        return null
    }

    @SideOnly(Side.CLIENT)
    override fun getRenderBoundingBox(): AxisAlignedBB {
        return if (cameraDrawOptimisation()) {
            AxisAlignedBB.getBoundingBox((xCoord - 1).toDouble(), (yCoord - 1).toDouble(), (zCoord - 1).toDouble(), (xCoord + 1).toDouble(), (yCoord + 1).toDouble(), (zCoord + 1).toDouble())
        } else {
            INFINITE_EXTENT_AABB
        }
    }

    open fun cameraDrawOptimisation(): Boolean {
        return true
    }

    val lightValue: Int
        get() = if (worldObj.isRemote) {
            if (lastLight == 0xFF) {
                0
            } else lastLight
        } else {
            node?.lightValue?: 0
        }

    /**
     * Reads a tile entity from NBT.
     */
    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
    }

    /**
     * Writes a tile entity to NBT.
     */
    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
    }

    //max draw distance
    @SideOnly(Side.CLIENT)
    override fun getMaxRenderDistanceSquared(): Double {
        return 4096.0 * 4 * 4
    }

    @Suppress("UNUSED_PARAMETER") fun onBlockPlacedBy(front: Direction?, entityLiving: EntityLivingBase?, metadata: Int) {}
    override fun canUpdate(): Boolean {
        return true
    }

    var updateEntityFirst = true
    override fun updateEntity() {
        if (updateEntityFirst) {
            updateEntityFirst = false
            if (!worldObj.isRemote) {
                // worldObj.setBlock(xCoord, yCoord, zCoord, 0);
            } else {
                clientList.add(this)
            }
        }
    }

    fun onBlockAdded() {
        if (!worldObj.isRemote && node == null) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord)
        }
    }

    fun onBreakBlock() {
        if (!worldObj.isRemote) {
            if (node == null) return
            node!!.onBreakBlock()
        }
    }

    override fun onChunkUnload() {
        if (worldObj.isRemote) {
            destructor()
        }
    }

    //client only
    open fun destructor() {
        clientList.remove(this)
    }

    override fun invalidate() {
        if (worldObj.isRemote) {
            destructor()
        }
        super.invalidate()
    }

    fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (!worldObj.isRemote) {
            if (node == null) return false
            node!!.onBlockActivated(entityPlayer!!, side!!, vx, vy, vz)
            return true
        }
        //if(entityPlayer.getCurrentEquippedItem().getItem() instanceof ItemBlock)
        run { return true }
        //return true;
    }

    fun onNeighborBlockChange() {
        if (!worldObj.isRemote) {
            if (node == null) return
            node!!.onNeighborBlockChange()
        }
    }

    override fun getDescriptionPacket(): Packet? {
        val node = node //TO DO NULL POINTER
        if (node == null) {
            println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity")
            return null
        }
        return S3FPacketCustomPayload(Eln.channelName, node.publishPacket!!.toByteArray())
    }

    open fun preparePacketForServer(stream: DataOutputStream) {
        try {
            stream.writeByte(Eln.packetPublishForNode.toInt())
            stream.writeInt(xCoord)
            stream.writeInt(yCoord)
            stream.writeInt(zCoord)
            stream.writeByte(worldObj.provider.dimensionId)
            stream.writeUTF(nodeUuid)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun sendPacketToServer(bos: ByteArrayOutputStream?) {
        UtilsClient.sendPacketToServer(bos!!)
    }

    open fun getCableRender(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        return null
    }

    open fun getCableDry(side: Direction?, lrdu: LRDU?): Int {
        return 0
    }

    fun canConnectRedstone(@Suppress("UNUSED_PARAMETER") xn: Direction?): Boolean {
        return if (worldObj.isRemote) redstone else {
            if (node == null) false else node!!.canConnectRedstone()
        }
    }

    open fun clientRefresh(deltaT: Float) {}

    companion object {
        @JvmField
        //val clientList = LinkedList<NodeBlockEntity>()
        val clientList = LinkedBlockingQueue<NodeBlockEntity>()
        fun getEntity(x: Int, y: Int, z: Int): NodeBlockEntity? {
            var entity: TileEntity?
            if (Minecraft.getMinecraft().theWorld.getTileEntity(x, y, z).also { entity = it } != null) {
                if (entity is NodeBlockEntity) {
                    return entity as NodeBlockEntity?
                }
            }
            return null
        }
    }
}
