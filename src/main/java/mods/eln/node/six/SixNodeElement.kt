package mods.eln.node.six

import mods.eln.Eln
import mods.eln.ghost.GhostObserver
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.misc.LRDU
import mods.eln.misc.LRDU.Companion.readFromNBT
import mods.eln.misc.Utils
import mods.eln.misc.Utils.isPlayerUsingWrench
import mods.eln.misc.Utils.mustDropItem
import mods.eln.misc.Utils.readFromNBT
import mods.eln.misc.Utils.writeToNBT
import mods.eln.node.INodeElement
import mods.eln.node.NodeConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalConnection
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Component
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sound.IPlayer
import mods.eln.sound.SoundCommand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

abstract class SixNodeElement(sixNode: SixNode, @JvmField var side: Direction, descriptor: SixNodeDescriptor) : GhostObserver, IPlayer, INodeElement {
    @JvmField
    var slowProcessList = ArrayList<IProcess>(4)
    @JvmField
    var electricalProcessList = ArrayList<IProcess>(4)
    @JvmField
    var electricalComponentList = ArrayList<Component>(4)
    @JvmField
    var electricalLoadList = ArrayList<NbtElectricalLoad>(4)
    @JvmField
    var thermalProcessList = ArrayList<IProcess>(4)
    @JvmField
    var thermalSlowProcessList = ArrayList<IProcess>(4)
    var thermalConnectionList = ArrayList<ThermalConnection>(4)
    @JvmField
    var thermalLoadList = ArrayList<NbtThermalLoad>(4)
    @JvmField
    var sixNode: SixNode? = sixNode

    @JvmField
    var sixNodeElementDescriptor: SixNodeDescriptor = descriptor
    open val isProvidingWeakPower: Int
        get() = 0

    override fun inventoryChange(inventory: IInventory?) {
        inventoryChanged()
    }

    open fun inventoryChanged() {}
    override fun play(s: SoundCommand) {
        s.addUuid(getUuid())
        s.set(sixNode!!.coordinate)
        s.play()
    }

    open val coordinate: Coordinate?
        get() = sixNode!!.coordinate

    override val ghostObserverCoordonate: Coordinate?
        get() = coordinate

    protected fun onBlockActivatedRotate(entityPlayer: EntityPlayer?): Boolean {
        if (isPlayerUsingWrench(entityPlayer)) {
            front = front.nextClockwise
            sixNode!!.reconnect()
            sixNode!!.needPublish = true
            return true
        }
        return false
    }

    fun sendPacketToAllClient(bos: ByteArrayOutputStream?) {
        sixNode!!.sendPacketToAllClient(bos)
    }

    fun sendPacketToAllClient(bos: ByteArrayOutputStream?, range: Double) {
        sixNode!!.sendPacketToAllClient(bos, range)
    }

    fun sendPacketToClient(bos: ByteArrayOutputStream?, player: EntityPlayerMP?) {
        sixNode!!.sendPacketToClient(bos, player)
    }

    fun notifyNeighbor() {
        sixNode!!.notifyNeighbor()
    }

    open fun connectJob() {
        // If we are about to destruct ourselves, do not add any elements to the simulation anymore.
        if (sixNode != null && sixNode!!.isDestructing) return
        Eln.simulator.addAllElectricalComponent(electricalComponentList)
        Eln.simulator.addAllThermalConnection(thermalConnectionList)
        for (load in electricalLoadList) Eln.simulator.addElectricalLoad(load)
        for (load in thermalLoadList) Eln.simulator.addThermalLoad(load)
        for (process in slowProcessList) Eln.simulator.addSlowProcess(process)
        for (process in electricalProcessList) Eln.simulator.addElectricalProcess(process)
        for (process in thermalProcessList) Eln.simulator.addThermalFastProcess(process)
        for (process in thermalSlowProcessList) Eln.simulator.addThermalSlowProcess(process)
    }

    open fun networkUnserialize(stream: DataInputStream) {}
    open fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP?) {
        networkUnserialize(stream)
    }

    open val lightValue: Int
        get() = 0

    open fun hasGui(): Boolean {
        return false
    }

    open val inventory: IInventory?
        get() = null

    open fun newContainer(side: Direction, player: EntityPlayer): Container? {
        return null
    }

    fun preparePacketForClient(stream: DataOutputStream?) {
        sixNode!!.preparePacketForClient(stream!!, this)
    }

    open fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? = null
    open fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null
    open fun getConnectionMask(lrdu: LRDU): Int = 0
    // reason to believe NodeConnection can be non-null asserted
    open fun newConnectionAt(connection: NodeConnection?, isA: Boolean) {}
    open fun multiMeterString(): String = ""
    open fun thermoMeterString(): String = ""

    @JvmField
    var front = LRDU.Up
    private val itemStackDamageId: Int
    open fun networkSerialize(stream: DataOutputStream) {
        try {
            stream.writeByte(sixNode!!.lrduElementMask[side]!!.mask + (front.dir shl 4))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun initialize() {}
    override fun stop(uuid: Int) {
        val bos = ByteArrayOutputStream(8)
        val stream = DataOutputStream(bos)
        try {
            stream.writeByte(Eln.packetDestroyUuid.toInt())
            stream.writeInt(uuid)
            sendPacketToAllClient(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun destroy(entityPlayer: EntityPlayerMP?) {
        if (useUuid()) {
            stop(uuid)
        }
        if (sixNodeElementDescriptor.hasGhostGroup()) {
            Eln.ghostManager.removeObserver(sixNode!!.coordinate)
            sixNodeElementDescriptor.getGhostGroup(side, front)!!.erase(sixNode!!.coordinate)
        }
        sixNode!!.dropInventory(inventory)
        if (mustDropItem(entityPlayer)) sixNode!!.dropItem(dropItemStack)
    }

    /**
     * Called when a player right-clicks the SixNode.
     * @param entityPlayer Player.
     * @param side Something to do with player viewpoint?
     * @param vx Ditto?
     * @param vy ?
     * @param vz ?
     * @return True if we've done something, otherwise false.
     */
    open fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return onBlockActivatedRotate(entityPlayer)
    }

    val dropItemStack: ItemStack
        get() = ItemStack(Eln.sixNodeBlock, 1, itemStackDamageId) //sixNode.sideElementIdList[side.getInt()]

    open fun readFromNBT(nbt: NBTTagCompound) {
        front = readFromNBT(nbt, "sixFront")
        val inv = inventory
        if (inv != null) {
            readFromNBT(nbt, "inv", inv)
        }
        for (electricalLoad in electricalLoadList) {
            electricalLoad.readFromNBT(nbt, "")
        }
        for (thermalLoad in thermalLoadList) {
            thermalLoad.readFromNBT(nbt, "")
        }
        for (c in electricalComponentList) if (c is INBTTReady) (c as INBTTReady).readFromNBT(nbt, "")
        for (process in slowProcessList) {
            if (process is INBTTReady) (process as INBTTReady).readFromNBT(nbt, "")
        }
        for (process in electricalProcessList) {
            if (process is INBTTReady) (process as INBTTReady).readFromNBT(nbt, "")
        }
        for (process in thermalProcessList) {
            if (process is INBTTReady) (process as INBTTReady).readFromNBT(nbt, "")
        }
        for (process in thermalSlowProcessList) {
            if (process is INBTTReady) (process as INBTTReady).readFromNBT(nbt, "")
        }
    }

    open fun writeToNBT(nbt: NBTTagCompound) {
        front.writeToNBT(nbt, "sixFront")
        val inv = inventory
        if (inv != null) {
            writeToNBT(nbt, "inv", inv)
        }
        for (electricalLoad in electricalLoadList) {
            electricalLoad.writeToNBT(nbt, "")
        }
        for (thermalLoad in thermalLoadList) {
            thermalLoad.writeToNBT(nbt, "")
        }
        for (c in electricalComponentList) if (c is INBTTReady) (c as INBTTReady).writeToNBT(nbt, "")
        for (process in slowProcessList) {
            if (process is INBTTReady) (process as INBTTReady).writeToNBT(nbt, "")
        }
        for (process in electricalProcessList) {
            if (process is INBTTReady) (process as INBTTReady).writeToNBT(nbt, "")
        }
        for (process in thermalProcessList) {
            if (process is INBTTReady) (process as INBTTReady).writeToNBT(nbt, "")
        }
        for (process in thermalSlowProcessList) {
            if (process is INBTTReady) (process as INBTTReady).writeToNBT(nbt, "")
        }
    }

    override fun reconnect() {
        sixNode!!.reconnect()
    }

    override fun needPublish() {
        sixNode!!.needPublish = true
    }

    open fun disconnectJob() {
        Eln.simulator.removeAllElectricalComponent(electricalComponentList)
        Eln.simulator.removeAllThermalConnection(thermalConnectionList)
        for (load in electricalLoadList) Eln.simulator.removeElectricalLoad(load)
        for (load in thermalLoadList) Eln.simulator.removeThermalLoad(load)
        for (process in slowProcessList) Eln.simulator.removeSlowProcess(process)
        for (process in electricalProcessList) Eln.simulator.removeElectricalProcess(process)
        for (process in thermalProcessList) Eln.simulator.removeThermalFastProcess(process)
        for (process in thermalSlowProcessList) Eln.simulator.removeThermalSlowProcess(process)
    }

    open fun canConnectRedstone(): Boolean {
        return false
    }

    override fun ghostDestroyed(UUID: Int) {
        if (UUID == sixNodeElementDescriptor.ghostGroupUuid) {
            selfDestroy()
        }
    }

    override fun ghostBlockActivated(UUID: Int, entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (UUID == sixNodeElementDescriptor.ghostGroupUuid) {
            sixNode!!.onBlockActivated(entityPlayer, this.side, vx, vy, vz)
        }
        return false
    }

    private fun selfDestroy() {
        sixNode!!.deleteSubBlock(null, side)
    }

    private var uuid = 0
    fun getUuid(): Int {
        if (uuid == 0) {
            uuid = Utils.uuid
        }
        return uuid
    }

    fun useUuid(): Boolean {
        return uuid != 0
    }

    open fun globalBoot() {}
    open fun unload() {}
    fun playerAskToBreak(): Boolean {
        return true
    }

    open fun getWaila(): Map<String, String>? {
        val wailaList: MutableMap<String, String> = HashMap()
        wailaList["Info"] = multiMeterString()
        return wailaList
    }

    init {
        itemStackDamageId = sixNode.sideElementIdList[side.int]
        if (descriptor.hasGhostGroup()) Eln.ghostManager.addObserver(this)
    }
}
