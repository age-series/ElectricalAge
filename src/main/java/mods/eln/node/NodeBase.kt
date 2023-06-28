@file:Suppress("NAME_SHADOWING")
package mods.eln.node

import mods.eln.misc.Utils.println
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.Coordinate
import net.minecraft.entity.player.EntityPlayerMP
import mods.eln.misc.LRDUCubeMask
import net.minecraft.world.World
import mods.eln.Eln
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import mods.eln.sound.SoundCommand
import mods.eln.GuiHandler
import mods.eln.misc.LRDU
import mods.eln.sim.ThermalLoad
import mods.eln.sim.ElectricalLoad
import mods.eln.node.six.SixNode
import mods.eln.sim.IProcess
import mods.eln.misc.INBTTReady
import java.io.IOException
import kotlin.jvm.JvmOverloads
import net.minecraft.server.MinecraftServer
import cpw.mods.fml.common.FMLCommonHandler
import net.minecraft.world.WorldServer
import net.minecraft.entity.item.EntityItem
import net.minecraft.inventory.IInventory
import net.minecraft.init.Blocks
import mods.eln.ghost.GhostBlock
import mods.eln.misc.Direction
import mods.eln.misc.Utils
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ThermalConnection
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.ArrayList
import kotlin.experimental.or

abstract class NodeBase {
    var neighborOpaque: Byte = 0
    var neighborWrapable: Byte = 0
    @JvmField
    var coordinate: Coordinate
    @JvmField
    var nodeConnectionList = ArrayList<NodeConnection>(4)
    private var initialized = false
    private var isAdded = false
    var needPublish = false

    // public static boolean canBePlacedOn(ItemStack itemStack,Direction side)
    open fun mustBeSaved(): Boolean {
        return true
    }

    open val blockMetadata: Int
        get() = 0

    open fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP?) {}
    fun notifyNeighbor() {
        coordinate.world().notifyBlockChange(coordinate.x, coordinate.y, coordinate.z, coordinate.block)
    }

    //public abstract Block getBlock();
    abstract val nodeUuid: String?
    @JvmField
    var lrduCubeMask = LRDUCubeMask()
    fun neighborBlockRead() {
        val vector = IntArray(3)
        val world = coordinate.world()
        neighborOpaque = 0
        neighborWrapable = 0
        for (direction in Direction.values()) {
            vector[0] = coordinate.x
            vector[1] = coordinate.y
            vector[2] = coordinate.z
            direction.applyTo(vector, 1)
            val b = world.getBlock(vector[0], vector[1], vector[2])
            neighborOpaque = neighborOpaque or (1 shl direction.int).toByte()
            if (isBlockWrappable(b, world, coordinate.x, coordinate.y, coordinate.z)) neighborWrapable = neighborWrapable or (1 shl direction.int).toByte()
        }
    }

    open fun hasGui(side: Direction): Boolean {
        return false
    }

    open fun onNeighborBlockChange() {
        neighborBlockRead()
        if (isAdded) {
            reconnect()
        }
    }

    fun isBlockWrappable(direction: Direction): Boolean {
        return neighborWrapable.toInt() shr direction.int and 1 != 0
    }

    fun isBlockOpaque(direction: Direction): Boolean {
        return neighborOpaque.toInt() shr direction.int and 1 != 0
    }

    var isDestructing = false
    fun physicalSelfDestruction(explosionStrength: Float) {
        var explosionStrength = explosionStrength
        if (isDestructing) return
        isDestructing = true
        if (!Eln.explosionEnable) explosionStrength = 0f
        disconnect()
        coordinate.world().setBlockToAir(coordinate.x, coordinate.y, coordinate.z)
        NodeManager.instance!!.removeNode(this)
        if (explosionStrength != 0f) {
            coordinate.world().createExplosion(null as Entity?, coordinate.x.toDouble(), coordinate.y.toDouble(), coordinate.z.toDouble(), explosionStrength, true)
        }
    }

    fun onBlockPlacedBy(coordinate: Coordinate, front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        this.coordinate = coordinate
        neighborBlockRead()
        NodeManager.instance!!.addNode(this)
        initializeFromThat(front, entityLiving, itemStack)
        if (itemStack != null) println("Node::constructor( meta = " + itemStack.itemDamage + ")")
    }

    abstract fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?)

    fun getNeighbor(direction: Direction): NodeBase? {
        val position = IntArray(3)
        position[0] = coordinate.x
        position[1] = coordinate.y
        position[2] = coordinate.z
        direction.applyTo(position, 1)
        val nodeCoordinate = Coordinate(position[0], position[1], position[2], coordinate.dimension)
        return NodeManager.instance!!.getNodeFromCoordonate(nodeCoordinate)
    }

    open fun onBreakBlock() {
        isDestructing = true
        disconnect()
        NodeManager.instance!!.removeNode(this)
        println("Node::onBreakBlock()")
    }

    open fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (!entityPlayer.worldObj.isRemote && entityPlayer.currentEquippedItem != null) {
            val equipped = entityPlayer.currentEquippedItem
            if (Eln.multiMeterElement.checkSameItemStack(equipped)) {
                val str = multiMeterString(side)
                addChatMessage(entityPlayer, str)
                return true
            }
            if (Eln.thermometerElement.checkSameItemStack(equipped)) {
                val str = thermoMeterString(side)
                addChatMessage(entityPlayer, str)
                return true
            }
            if (Eln.allMeterElement.checkSameItemStack(equipped)) {
                val str1 = multiMeterString(side)
                val str2 = thermoMeterString(side)
                var str = ""
                str += str1
                str += str2
                if (str != "") addChatMessage(entityPlayer, str)
                return true
            }
            if (Eln.configCopyToolElement.checkSameItemStack(equipped)) {
                if (!equipped.hasTagCompound()) {
                    equipped.tagCompound = NBTTagCompound()
                }
                val act: String
                var snd = beepError
                if (entityPlayer.isSneaking || Eln.playerManager[entityPlayer]!!.interactEnable) {
                    if (writeConfigTool(side, equipped.tagCompound, entityPlayer)) snd = beepDownloaded
                    act = "write"
                } else {
                    if (readConfigTool(side, equipped.tagCompound, entityPlayer)) snd = beepUploaded
                    act = "read"
                }
                snd.set(
                    entityPlayer.posX,
                    entityPlayer.posY,
                    entityPlayer.posZ,
                    entityPlayer.worldObj
                ).play()
                println(String.format("NB.oBA: act %s data %s", act, equipped.tagCompound.toString()))
                return true
            }
        }
        if (hasGui(side)) {
            entityPlayer.openGui(Eln.instance, GuiHandler.nodeBaseOpen + side.int, coordinate.world(), coordinate.x, coordinate.y, coordinate.z)
            return true
        }
        return false
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    abstract fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int
    abstract fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad?
    abstract fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad?
    open fun checkCanStay(onCreate: Boolean) {}
    open fun connectJob() {
        // EXTERNAL OTHERS SIXNODE
        run {
            val emptyBlockCoord = IntArray(3)
            val otherBlockCoord = IntArray(3)
            for (direction in Direction.values()) {
                if (isBlockWrappable(direction)) {
                    emptyBlockCoord[0] = coordinate.x
                    emptyBlockCoord[1] = coordinate.y
                    emptyBlockCoord[2] = coordinate.z
                    direction.applyTo(emptyBlockCoord, 1)
                    for (lrdu in LRDU.values()) {
                        val elementSide = direction.applyLRDU(lrdu)
                        otherBlockCoord[0] = emptyBlockCoord[0]
                        otherBlockCoord[1] = emptyBlockCoord[1]
                        otherBlockCoord[2] = emptyBlockCoord[2]
                        elementSide.applyTo(otherBlockCoord, 1)
                        val otherNode = NodeManager.instance!!.getNodeFromCoordonate(Coordinate(otherBlockCoord[0], otherBlockCoord[1], otherBlockCoord[2], coordinate.dimension))
                            ?: continue
                        val otherDirection = elementSide.inverse
                        val otherLRDU = otherDirection.getLRDUGoingTo(direction)!!.inverse()
                        if (this is SixNode || otherNode is SixNode) {
                            tryConnectTwoNode(this, direction, lrdu, otherNode, otherDirection, otherLRDU)
                        }
                    }
                }
            }
        }
        run {
            for (dir in Direction.values()) {
                val otherNode = getNeighbor(dir)
                if (otherNode != null && otherNode.isAdded) {
                    for (lrdu in LRDU.values()) {
                        tryConnectTwoNode(this, dir, lrdu, otherNode, dir.inverse, lrdu.inverseIfLR())
                    }
                }
            }
        }
    }

    open fun disconnectJob() {
        for (c in nodeConnectionList) {
            if (c.N1 !== this) {
                c.N1.nodeConnectionList.remove(c)
                c.N1.needPublish = true
                c.N1.lrduCubeMask[c.dir1, c.lrdu1] = false
            }
            if (c.N2 !== this) {
                c.N2.nodeConnectionList.remove(c)
                c.N2.needPublish = true
                c.N2.lrduCubeMask[c.dir2, c.lrdu2] = false
            }
            c.destroy()
        }
        lrduCubeMask.clear()
        nodeConnectionList.clear()
    }

    open fun externalDisconnect(side: Direction?, lrdu: LRDU?) {}
    open fun newConnectionAt(connection: NodeConnection?, isA: Boolean) {}
    open fun connectInit() {
        lrduCubeMask.clear()
        nodeConnectionList.clear()
    }

    fun connect() {
        if (isAdded) {
            disconnect()
        }
        connectInit()
        connectJob()
        isAdded = true
        needPublish = true
    }

    fun disconnect() {
        if (!isAdded) {
            println("Node destroy error already destroy")
            return
        }
        disconnectJob()
        isAdded = false
    }

    open fun nodeAutoSave(): Boolean {
        return true
    }

    open fun readFromNBT(nbt: NBTTagCompound) {
        coordinate.readFromNBT(nbt, "c")
        neighborOpaque = nbt.getByte("NBOpaque")
        neighborWrapable = nbt.getByte("NBWrap")
        initialized = true
    }

    open fun writeToNBT(nbt: NBTTagCompound) {
        coordinate.writeToNBT(nbt, "c")
        nbt.setByte("NBOpaque", neighborOpaque)
        nbt.setByte("NBWrap", neighborWrapable)
    }

    open fun multiMeterString(side: Direction): String {
        return ""
    }

    open fun thermoMeterString(side: Direction): String {
        return ""
    }

    open fun readConfigTool(side: Direction?, tag: NBTTagCompound?, invoker: EntityPlayer?): Boolean {
        return false
    }

    open fun writeConfigTool(side: Direction?, tag: NBTTagCompound?, invoker: EntityPlayer?): Boolean {
        return false
    }

    private fun isINodeProcess(process: IProcess): Boolean {
        for (c in process.javaClass.interfaces) {
            if (c == INBTTReady::class.java) return true
        }
        return false
    }

    @JvmField
    var needNotify = false
    open fun publishSerialize(stream: DataOutputStream) {}
    fun preparePacketForClient(stream: DataOutputStream) {
        try {
            stream.writeByte(Eln.packetForClientNode.toInt())
            stream.writeInt(coordinate.x)
            stream.writeInt(coordinate.y)
            stream.writeInt(coordinate.z)
            stream.writeByte(coordinate.dimension)
            stream.writeUTF(nodeUuid!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendPacketToClient(bos: ByteArrayOutputStream?, player: EntityPlayerMP?) {
        Utils.sendPacketToClient(bos!!, player!!)
    }

    @JvmOverloads
    fun sendPacketToAllClient(bos: ByteArrayOutputStream?, range: Double = 100000.0) {
        val server = FMLCommonHandler.instance().minecraftServerInstance
        for (obj in server.configurationManager.playerEntityList) {
            val player = obj as EntityPlayerMP?
            val worldServer = MinecraftServer.getServer().worldServerForDimension(player!!.dimension) as WorldServer
            val playerManager = worldServer.playerManager
            if (player.dimension != coordinate.dimension) continue
            if (!playerManager.isPlayerWatchingChunk(player, coordinate.x / 16, coordinate.z / 16)) continue
            if (coordinate.distanceTo(player) > range) continue
            Utils.sendPacketToClient(bos!!, player)
        }
    }

    val publishPacket: ByteArrayOutputStream?
        get() {
            val bos = ByteArrayOutputStream(64)
            val stream = DataOutputStream(bos)
            try {
                stream.writeByte(Eln.packetNodeSingleSerialized.toInt())
                stream.writeInt(coordinate.x)
                stream.writeInt(coordinate.y)
                stream.writeInt(coordinate.z)
                stream.writeByte(coordinate.dimension)
                stream.writeUTF(nodeUuid!!)
                publishSerialize(stream)
                return bos
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

    fun publishToAllPlayer() {
        val server = FMLCommonHandler.instance().minecraftServerInstance
        for (obj in server.configurationManager.playerEntityList) {
            val player = obj as EntityPlayerMP?
            val worldServer = MinecraftServer.getServer().worldServerForDimension(player!!.dimension) as WorldServer
            val playerManager = worldServer.playerManager
            if (player.dimension != coordinate.dimension) continue
            if (!playerManager.isPlayerWatchingChunk(player, coordinate.x / 16, coordinate.z / 16)) continue
            Utils.sendPacketToClient(publishPacket!!, player)
        }
        if (needNotify) {
            needNotify = false
            notifyNeighbor()
        }
        needPublish = false
    }

    fun publishToPlayer(player: EntityPlayerMP?) {
        Utils.sendPacketToClient(publishPacket!!, player!!)
    }

    fun dropItem(itemStack: ItemStack?) {
        if (itemStack == null) return
        if (coordinate.world().gameRules.getGameRuleBooleanValue("doTileDrops")) {
            val var6 = 0.7f
            val var7 = (coordinate.world().rand.nextFloat() * var6).toDouble() + (1.0f - var6).toDouble() * 0.5
            val var9 = (coordinate.world().rand.nextFloat() * var6).toDouble() + (1.0f - var6).toDouble() * 0.5
            val var11 = (coordinate.world().rand.nextFloat() * var6).toDouble() + (1.0f - var6).toDouble() * 0.5
            val var13 = EntityItem(coordinate.world(), coordinate.x.toDouble() + var7, coordinate.y.toDouble() + var9, coordinate.z.toDouble() + var11, itemStack)
            var13.delayBeforeCanPickup = 10
            coordinate.world().spawnEntityInWorld(var13)
        }
    }

    fun dropInventory(inventory: IInventory?) {
        if (inventory == null) return
        for (idx in 0 until inventory.sizeInventory) {
            dropItem(inventory.getStackInSlot(idx))
        }
    }

    abstract fun initializeFromNBT()
    open fun globalBoot() {}
    fun needPublish() {
        needPublish = true
    }

    open fun unload() {
        disconnect()
    }

    companion object {
        const val maskElectricalPower = 1 shl 0
        const val maskThermal = 1 shl 1
        const val maskElectricalGate = 1 shl 2
        const val maskElectricalAll = maskElectricalPower or maskElectricalGate
        const val maskElectricalInputGate = maskElectricalGate
        const val maskElectricalOutputGate = maskElectricalGate
        const val maskWire = 0
        const val maskElectricalWire = 1 shl 3
        const val maskThermalWire = maskWire + maskThermal
        const val maskSignal = 1 shl 9
        const val maskRs485 = 1 shl 10
        const val maskSignalBus = 1 shl 11
        const val maskColorData = 0xF shl 16
        const val maskColorShift = 16
        const val maskColorCareShift = 20
        const val maskColorCareData = 1 shl 20
        const val networkSerializeUFactor = 10.0
        const val networkSerializeIFactor = 100.0
        const val networkSerializeTFactor = 10.0
        var teststatic = 0
        @JvmStatic
        fun isBlockWrappable(block: Block, w: World?, x: Int, y: Int, z: Int): Boolean {
            if (block.isReplaceable(w, x, y, z)) return true
            if (block === Blocks.air) return true
            if (block === Eln.sixNodeBlock) return true
            if (block is GhostBlock) return true
            if (block === Blocks.torch) return true
            if (block === Blocks.redstone_torch) return true
            if (block === Blocks.unlit_redstone_torch) return true
            return block === Blocks.redstone_wire
        }

        var beepUploaded = SoundCommand("eln:beep_accept_2").smallRange()!!
        var beepDownloaded = SoundCommand("eln:beep_accept").smallRange()!!
        var beepError = SoundCommand("eln:beep_error").smallRange()!!

        fun tryConnectTwoNode(nodeA: NodeBase, directionA: Direction, lrduA: LRDU, nodeB: NodeBase, directionB: Direction, lrduB: LRDU) {
            val mskA = nodeA.getSideConnectionMask(directionA, lrduA)
            val mskB = nodeB.getSideConnectionMask(directionB, lrduB)
            if (compareConnectionMask(mskA, mskB)) {
                val eCon: ElectricalConnection?
                val tCon: ThermalConnection?
                val nodeConnection = NodeConnection(nodeA, directionA, lrduA, nodeB, directionB, lrduB)
                nodeA.nodeConnectionList.add(nodeConnection)
                nodeB.nodeConnectionList.add(nodeConnection)
                nodeA.needPublish = true
                nodeB.needPublish = true
                nodeA.lrduCubeMask[directionA, lrduA] = true
                nodeB.lrduCubeMask[directionB, lrduB] = true
                nodeA.newConnectionAt(nodeConnection, true)
                nodeB.newConnectionAt(nodeConnection, false)
                var eLoad: ElectricalLoad?
                if (nodeA.getElectricalLoad(directionA, lrduA, mskB).also { eLoad = it } != null) {
                    val otherELoad = nodeB.getElectricalLoad(directionB, lrduB, mskA)
                    if (otherELoad != null) {
                        eCon = ElectricalConnection(eLoad, otherELoad)
                        Eln.simulator.addElectricalComponent(eCon)
                        nodeConnection.addConnection(eCon)
                    }
                }
                var tLoad: ThermalLoad?
                if (nodeA.getThermalLoad(directionA, lrduA, mskB).also { tLoad = it } != null) {
                    val otherTLoad = nodeB.getThermalLoad(directionB, lrduB, mskA)
                    if (otherTLoad != null) {
                        tCon = ThermalConnection(tLoad, otherTLoad)
                        Eln.simulator.addThermalConnection(tCon)
                        nodeConnection.addConnection(tCon)
                    }
                }
            }
        }

        @JvmStatic
        fun compareConnectionMask(mask1: Int, mask2: Int): Boolean {
            if (mask1 and 0xFFFF and (mask2 and 0xFFFF) == 0) return false
            if (mask1 and maskColorCareData and (mask2 and maskColorCareData) == 0) return true
            return mask1 and maskColorData == mask2 and maskColorData
        }
    }

    init {
        coordinate = Coordinate()
    }
}
