package mods.eln.node.six

import mods.eln.Eln
import mods.eln.item.IConfigurable
import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromInt
import mods.eln.misc.LRDU
import mods.eln.misc.LRDUCubeMask
import mods.eln.misc.Utils.generateHeightMap
import mods.eln.misc.Utils.isCreative
import mods.eln.misc.Utils.newNbtTagCompund
import mods.eln.misc.Utils.println
import mods.eln.misc.Utils.updateAllLightTypes
import mods.eln.misc.Utils.updateSkylight
import mods.eln.node.ISixNodeCache
import mods.eln.node.Node
import mods.eln.node.NodeConnection
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalConnection
import mods.eln.sim.ThermalLoad
import net.minecraft.block.Block
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.*

class SixNode : Node() {
    @JvmField
    var sideElementList = arrayOfNulls<SixNodeElement>(6)
    @JvmField
    var sideElementIdList = IntArray(6)
    var internalElectricalConnectionList = ArrayList<ElectricalConnection>(1)
    var internalThermalConnectionList = ArrayList<ThermalConnection>(1)
    @JvmField
    var sixNodeCacheBlock = Blocks.air
    @JvmField
    var sixNodeCacheBlockMeta: Byte = 0
    @JvmField
    var lrduElementMask = LRDUCubeMask()
    fun getElement(side: Direction): SixNodeElement? {
        return sideElementList[side.int]
    }

    override fun canConnectRedstone(): Boolean {
        for (element in sideElementList) {
            if (element != null && element.canConnectRedstone()) {
                return true
            }
        }
        return false
    }

    override fun isProvidingWeakPower(side: Direction?): Int {
        var value = 0
        for (element in sideElementList) {
            if (element != null) {
                val eValue = element.isProvidingWeakPower
                if (eValue > value) value = eValue
            }
        }
        return value
    }

    fun createSubBlock(itemStack: ItemStack, direction: Direction, player: EntityPlayer?): Boolean {
        val descriptor = Eln.sixNodeItem.getDescriptor(itemStack)
        if (sideElementList[direction.int] != null) return false
        try {
            sideElementIdList[direction.int] = itemStack.itemDamage //Je sais c'est moche !
            sideElementList[direction.int] = descriptor!!.ElementClass.getConstructor(SixNode::class.java, Direction::class.java, SixNodeDescriptor::class.java).newInstance(this, direction, descriptor) as SixNodeElement
            sideElementIdList[direction.int] = 0
            disconnect()
            sideElementList[direction.int]!!.front = descriptor.getFrontFromPlace(direction, player!!)!!
            sideElementList[direction.int]!!.initialize()
            sideElementIdList[direction.int] = itemStack.itemDamage
            connect()
            println("createSubBlock " + sideElementIdList[direction.int] + " " + direction)
            needPublish = true
            return true
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return false
    }

    fun playerAskToBreakSubBlock(entityPlayer: EntityPlayerMP?, direction: Direction): Boolean {
        if (sideElementList[direction.int] == null) return deleteSubBlock(entityPlayer, direction)
        return if (sideElementList[direction.int]!!.playerAskToBreak()) {
            deleteSubBlock(entityPlayer, direction)
        } else {
            false
        }
    }

    fun deleteSubBlock(entityPlayer: EntityPlayerMP?, direction: Direction): Boolean {
        if (sideElementList[direction.int] == null) return false
        println("deleteSubBlock  $direction")
        disconnect()
        val e = sideElementList[direction.int]
        sideElementList[direction.int] = null
        sideElementIdList[direction.int] = 0
        e!!.destroy(entityPlayer)
        connect()
        recalculateLightValue()
        needPublish = true
        return true
    }

    val ifSideRemain: Boolean
        get() {
            for (sideElement in sideElementList) {
                if (sideElement != null) return true
            }
            return false
        }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt.getCompoundTag("node"))
        sixNodeCacheBlock = Block.getBlockById(nbt.getInteger("cacheBlockId"))
        sixNodeCacheBlockMeta = nbt.getByte("cacheBlockMeta")
        var idx: Int
        idx = 0
        while (idx < 6) {
            val sideElementId = nbt.getShort("EID$idx")
            if (sideElementId.toInt() == 0) {
                sideElementList[idx] = null
                sideElementIdList[idx] = 0
            } else {
                try {
                    val descriptor = Eln.sixNodeItem.getDescriptor(sideElementId.toInt())
                    sideElementIdList[idx] = sideElementId.toInt()
                    sideElementList[idx] = descriptor!!.ElementClass.getConstructor(SixNode::class.java, Direction::class.java, SixNodeDescriptor::class.java).newInstance(this, fromInt(idx), descriptor) as SixNodeElement
                    sideElementList[idx]!!.readFromNBT(nbt.getCompoundTag("ED$idx"))
                    sideElementList[idx]!!.initialize()
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                } catch (e: NoSuchMethodException) {
                    e.printStackTrace()
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
            idx++
        }
        initializeFromNBT()
    }

    override fun nodeAutoSave(): Boolean {
        return false
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        var idx = 0
        nbt.setInteger("cacheBlockId", Block.getIdFromBlock(sixNodeCacheBlock))
        nbt.setByte("cacheBlockMeta", sixNodeCacheBlockMeta)
        for (sideElement in sideElementList) {
            if (sideElement == null) {
                nbt.setShort("EID$idx", 0.toShort())
            } else {
                nbt.setShort("EID$idx", sideElementIdList[idx].toShort())
                sideElement.writeToNBT(newNbtTagCompund(nbt, "ED$idx"))
            }
            idx++
        }
        val nodeNbt = NBTTagCompound()
        super.writeToNBT(nodeNbt)
        nbt.setTag("node", nodeNbt)
    }

    fun getSideEnable(direction: Direction): Boolean {
        return sideElementList[direction.int] != null
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? {
        val elementSide = side.applyLRDU(lrdu)
        val element = sideElementList[elementSide.int] ?: return null
        return element.getElectricalLoad(elementSide.getLRDUGoingTo(side)!!, mask)
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? {
        val elementSide = side.applyLRDU(lrdu)
        val element = sideElementList[elementSide.int] ?: return null
        return element.getThermalLoad(elementSide.getLRDUGoingTo(side)!!, mask)
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        val elementSide = side.applyLRDU(lrdu)
        val element = sideElementList[elementSide.int] ?: return 0
        return element.getConnectionMask(elementSide.getLRDUGoingTo(side)!!)
    }

    override fun multiMeterString(side: Direction): String {
        val element = sideElementList[side.int] ?: return ""
        return element.multiMeterString()
    }

    override fun thermoMeterString(side: Direction): String {
        val element = sideElementList[side.int] ?: return ""
        return element.thermoMeterString()
    }

    override fun readConfigTool(side: Direction?, tag: NBTTagCompound?, invoker: EntityPlayer?): Boolean {
        val element = sideElementList[side!!.int]
        if (element is IConfigurable) {
            (element as IConfigurable).readConfigTool(tag, invoker)
            return true
        }
        return false
    }

    override fun writeConfigTool(side: Direction?, tag: NBTTagCompound?, invoker: EntityPlayer?): Boolean {
        val element = sideElementList[side!!.int]
        if (element is IConfigurable) {
            (element as IConfigurable).writeConfigTool(tag, invoker)
            return true
        }
        return false
    }

    override fun publishSerialize(stream: DataOutputStream) {
        super.publishSerialize(stream)
        try {
            var idx = 0
            stream.writeInt(Block.getIdFromBlock(sixNodeCacheBlock))
            stream.writeByte(sixNodeCacheBlockMeta.toInt())
            for (sideElement in sideElementList) {
                if (sideElement == null) {
                    stream.writeShort((0).toInt())
                } else {
                    stream.writeShort(sideElementIdList[idx])
                    sideElement.networkSerialize(stream)
                }
                idx++
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun preparePacketForClient(stream: DataOutputStream, e: SixNodeElement) {
        try {
            super.preparePacketForClient(stream)
            val side = e.side.int
            stream.writeByte(side)
            stream.writeShort(e.sixNodeElementDescriptor.parentItemDamage)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    override fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?,
                                    itemStack: ItemStack?) {
        neighborBlockRead()
    }

    override fun initializeFromNBT() {
        connect()
    }

    override fun connectInit() {
        super.connectInit()
        internalElectricalConnectionList.clear()
        internalThermalConnectionList.clear()
        lrduElementMask.clear()
    }

    override fun connectJob() {
        super.connectJob()
        for (element in sideElementList) {
            element?.connectJob()
        }

        //INTERNAL
        run {
            val side = Direction.YN
            val element = sideElementList[side.int]
            if (element != null) {
                for (lrdu in LRDU.values()) {
                    val otherSide = side.applyLRDU(lrdu)
                    val otherElement = sideElementList[otherSide.int]
                    if (otherElement != null) {
                        val otherLRDU = otherSide.getLRDUGoingTo(side)!!
                        tryConnectTwoInternalElement(side, element, lrdu, otherSide, otherElement, otherLRDU)
                    }
                }
            }
        }
        run {
            val side = Direction.YP
            val element = sideElementList[side.int]
            if (element != null) {
                for (lrdu in LRDU.values()) {
                    val otherSide = side.applyLRDU(lrdu)
                    val otherElement = sideElementList[otherSide.int]
                    if (otherElement != null) {
                        val otherLRDU = otherSide.getLRDUGoingTo(side)!!
                        tryConnectTwoInternalElement(side, element, lrdu, otherSide, otherElement, otherLRDU)
                    }
                }
            }
        }
        run {
            var side = Direction.XN
            for (idx in 0..3) {
                val otherSide = side.right()
                val element = sideElementList[side.int]
                val otherElement = sideElementList[otherSide.int]
                if (element != null && otherElement != null) {
                    tryConnectTwoInternalElement(side, element, LRDU.Right, otherSide, otherElement, LRDU.Left)
                }
                side = otherSide
            }
        }
    }

    override fun disconnectJob() {
        super.disconnectJob()
        for (element in sideElementList) {
            element?.disconnectJob()
        }
        Eln.simulator.removeAllElectricalConnection(internalElectricalConnectionList)
        Eln.simulator.removeAllThermalConnection(internalThermalConnectionList)
    }

    fun tryConnectTwoInternalElement(side: Direction, element: SixNodeElement, lrdu: LRDU, otherSide: Direction, otherElement: SixNodeElement, otherLRDU: LRDU) {
        println("SixNode.tCTIE:")
        val mskThis = element.getConnectionMask(lrdu)
        val mskOther = otherElement.getConnectionMask(otherLRDU)
        if (compareConnectionMask(mskThis, mskOther)) {
            println("\tConnection OK.")
            lrduElementMask[side, lrdu] = true
            lrduElementMask[otherSide, otherLRDU] = true
            val nodeConnection = NodeConnection(this, side, lrdu, this, otherSide, otherLRDU)
            nodeConnectionList.add(nodeConnection)
            element.newConnectionAt(nodeConnection, false)
            otherElement.newConnectionAt(nodeConnection, true)
            var eLoad: ElectricalLoad?
            if (element.getElectricalLoad(lrdu, mskOther).also { eLoad = it } != null) {
                val otherELoad = otherElement.getElectricalLoad(otherLRDU, mskThis)
                if (otherELoad != null) {
                    val eCon = ElectricalConnection(eLoad, otherELoad)
                    Eln.simulator.addElectricalComponent(eCon)
                    internalElectricalConnectionList.add(eCon)
                    nodeConnection.addConnection(eCon)
                }
            }
            var tLoad: ThermalLoad?
            if (getThermalLoad(side, lrdu, mskOther).also { tLoad = it } != null) {
                val otherTLoad = element.getThermalLoad(otherLRDU, mskThis)
                if (otherTLoad != null) {
                    val tCon = ThermalConnection(tLoad, otherTLoad)
                    Eln.simulator.addThermalConnection(tCon)
                    internalThermalConnectionList.add(tCon)
                    nodeConnection.addConnection(tCon)
                }
            }
        }
    }

    override fun newConnectionAt(connection: NodeConnection?, isA: Boolean) {
        val side = if (isA) connection!!.dir1 else connection!!.dir2
        val lrdu = if (isA) connection.lrdu1 else connection.lrdu2
        val elementSide = side.applyLRDU(lrdu)
        val element = sideElementList[elementSide.int]
        if (element == null) {
            println("sixnode newConnectionAt error")
            while (true);
        }
        lrduElementMask[elementSide, elementSide.getLRDUGoingTo(side)] = true
        element!!.newConnectionAt(connection, isA)
    }

    override fun externalDisconnect(side: Direction?, lrdu: LRDU?) {
        val elementSide = side!!.applyLRDU(lrdu!!)
        val element = sideElementList[elementSide.int]
        if (element == null) {
            println("sixnode newConnectionAt error")
            while (true);
        }
        lrduElementMask[elementSide, elementSide.getLRDUGoingTo(side)] = false
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return if (sixNodeCacheBlock !== Blocks.air) {
            false
        } else {
            val stack = entityPlayer.currentEquippedItem
            var b = Blocks.air
            if (stack != null) b = Block.getBlockFromItem(stack.item)
            var accepted = false
            if (Eln.playerManager[entityPlayer]!!.interactEnable && stack != null) {
                for (a in sixNodeCacheList) {
                    if (a.accept(stack)) {
                        accepted = true
                        sixNodeCacheBlock = b
                        sixNodeCacheBlockMeta = a.getMeta(stack).toByte()
                        break
                    }
                }
            }

            if (accepted) {
                println("ACACAC")
                needPublish = true
                if (!isCreative((entityPlayer as EntityPlayerMP))) entityPlayer.inventory.decrStackSize(entityPlayer.inventory.currentItem, 1)

                run {
                    val chunk = coordinate.world().getChunkFromBlockCoords(coordinate.x, coordinate.z)
                    generateHeightMap(chunk)
                    updateSkylight(chunk)
                    chunk.generateSkylightMap()
                    updateAllLightTypes(coordinate.world(), coordinate.x, coordinate.y, coordinate.z)
                }
                true
            } else {
                val element = sideElementList[side.int] ?: return false
                if (element.onBlockActivated(entityPlayer, side, vx, vy, vz)) true else super.onBlockActivated(entityPlayer, side, vx, vy, vz)
            }
        }
    }

    override fun hasGui(side: Direction): Boolean {
        return if (sideElementList[side.int] == null) false else sideElementList[side.int]!!.hasGui()
    }

    fun getInventory(side: Direction): IInventory? {
        return if (sideElementList[side.int] == null) null else sideElementList[side.int]!!.inventory
    }

    fun newContainer(side: Direction, player: EntityPlayer): Container? {
        return if (sideElementList[side.int] == null) null else sideElementList[side.int]!!.newContainer(side, player)
    }

    fun physicalSelfDestructionExplosionStrength(): Float {
        return 1.0f
    }

    fun recalculateLightValue() {
        var light = 0
        for (element in sideElementList) {
            if (element == null) continue
            val eLight = element.lightValue
            if (eLight > light) light = eLight
        }
        lightValue = light
    }

    override fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP?) {
        super.networkUnserialize(stream, player)
        val side: Direction?
        try {
            side = fromInt(stream.readByte().toInt())
            if (side != null && sideElementIdList[side.int] == stream.readShort().toInt()) {
                sideElementList[side.int]!!.networkUnserialize(stream, player)
            } else {
                println("sixnode unserialize miss")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun hasVolume(): Boolean {
        for (element in sideElementList) {
            if (element != null && element.sixNodeElementDescriptor.hasVolume()) return true
        }
        return false
    }

    override val nodeUuid: String
        get() = Eln.sixNodeBlock.nodeUuid

    override fun globalBoot() {
        super.globalBoot()
        for (e in sideElementList) {
            if (e == null) continue
            e.globalBoot()
        }
    }

    override fun unload() {
        super.unload()
        for (e in sideElementList) {
            if (e == null) continue
            e.unload()
        }
    }

    companion object {
        @JvmField
        val sixNodeCacheList = ArrayList<ISixNodeCache>()
    }

    init {
        for (idx in 0..5) {
            sideElementList[idx] = null
            sideElementIdList[idx] = 0
        }
        lrduElementMask.clear()
    }
}
