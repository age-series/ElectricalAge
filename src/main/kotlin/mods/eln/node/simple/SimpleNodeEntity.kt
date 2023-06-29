package mods.eln.node.simple

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromInt
import mods.eln.misc.Utils.fatal
import mods.eln.misc.Utils.println
import mods.eln.node.INodeEntity
import mods.eln.node.NodeEntityClientSender
import mods.eln.node.NodeManager
import mods.eln.node.simple.DescriptorManager.get
import mods.eln.server.DelayedBlockRemove.Companion.add
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S3FPacketCustomPayload
import net.minecraft.tileentity.TileEntity
import java.io.DataInputStream
import java.io.IOException

abstract class SimpleNodeEntity(override val nodeUuid: String) : TileEntity(), INodeEntity {
    open var node: SimpleNode? = null
        get() {
            if (worldObj.isRemote) {
                fatal()
                return null
            }
            if (worldObj == null) return null
            if (field == null) {
                field = NodeManager.instance!!.getNodeFromCoordonate(Coordinate(xCoord, yCoord, zCoord, worldObj)) as SimpleNode?
                if (field == null) {
                    add(Coordinate(xCoord, yCoord, zCoord, worldObj))
                    return null
                }
            }
            return field
        }

    //***************** Wrapping **************************
    /*
	public void onBlockPlacedBy(Direction front, EntityLivingBase entityLiving, int metadata) {
	
	}
*/
    fun onBlockAdded() {
        /*if (!worldObj.isRemote){
			if (getNode() == null) {
				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			}
		}*/
    }

    fun onBreakBlock() {
        if (!worldObj.isRemote) {
            if (node == null) return
            node!!.onBreakBlock()
        }
    }

    override fun onChunkUnload() {
        super.onChunkUnload()
        if (worldObj.isRemote) {
            destructor()
        }
    }

    // client only
    fun destructor() {}
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
        return true
    }

    fun onNeighborBlockChange() {
        if (!worldObj.isRemote) {
            if (node == null) return
            node!!.onNeighborBlockChange()
        }
    }

    //***************** Descriptor **************************
    val descriptor: Any?
        get() {
            val b = getBlockType() as SimpleNodeBlock
            return get<Any>(b.descriptorKey)
        }

    //***************** Network **************************
    var front: Direction? = null
    override fun serverPublishUnserialize(stream: DataInputStream) {
        try {
            if (front !== fromInt(stream.readByte().toInt()).also { front = it }) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun serverPacketUnserialize(stream: DataInputStream) {}
    override fun getDescriptionPacket(): Packet? {
        val node = node
        if (node == null) {
            println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity")
            return null
        }
        return S3FPacketCustomPayload(Eln.channelName, node.publishPacket!!.toByteArray())
    }

    open lateinit var sender: NodeEntityClientSender

    init {
        println("NodeUUID: $nodeUuid")
        sender = NodeEntityClientSender(this, nodeUuid)
    }

    //*********************** GUI ***************************
    override fun newContainer(side: Direction, player: EntityPlayer): Container? {
        return null
    }

    @SideOnly(Side.CLIENT)
    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? {
        return null
    }
}
