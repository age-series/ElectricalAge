package mods.eln.node.simple

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.readFromNBT
import mods.eln.misc.INBTTReady
import mods.eln.node.NodeBase
import mods.eln.node.simple.DescriptorManager.get
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalConnection
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtThermalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

abstract class SimpleNode : NodeBase() {
    var removedByPlayer: EntityPlayerMP? = null

    open var descriptorKey: String? = ""

    open fun getDescriptor() = get<Any>(descriptorKey)

    var front: Direction? = null
        set(value) {
            field = value
            if (applayFrontToMetadata()) {
                if (front != null)
                    coordinate.setMetadata(front!!.int)
            }
        }

    protected fun applayFrontToMetadata(): Boolean {
        return false
    }

    override fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        this.front = front
        initialize()
    }

    override fun initializeFromNBT() {
        initialize()
    }

    abstract fun initialize()
    override fun publishSerialize(stream: DataOutputStream) {
        super.publishSerialize(stream)
        try {
            stream.writeByte(front!!.int)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmField
    var slowProcessList = ArrayList<IProcess>(4)
    var electricalProcessList = ArrayList<IProcess>(4)
    @JvmField
    var electricalComponentList = ArrayList<Component>(4)
    @JvmField
    var electricalLoadList = ArrayList<State>(4)
    var thermalFastProcessList = ArrayList<IProcess>(4)
    var thermalSlowProcessList = ArrayList<IProcess>(4)
    var thermalConnectionList = ArrayList<ThermalConnection>(4)
    var thermalLoadList = ArrayList<NbtThermalLoad>(4)
    override fun connectJob() {
        super.connectJob()
        Eln.simulator.addAllSlowProcess(slowProcessList)
        Eln.simulator.addAllElectricalComponent(electricalComponentList)
        for (load in electricalLoadList) Eln.simulator.addElectricalLoad(load)
        Eln.simulator.addAllElectricalProcess(electricalProcessList)
        Eln.simulator.addAllThermalConnection(thermalConnectionList)
        for (load in thermalLoadList) Eln.simulator.addThermalLoad(load)
        Eln.simulator.addAllThermalFastProcess(thermalFastProcessList)
        Eln.simulator.addAllThermalSlowProcess(thermalSlowProcessList)
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.removeAllSlowProcess(slowProcessList)
        Eln.simulator.removeAllElectricalComponent(electricalComponentList)
        for (load in electricalLoadList) Eln.simulator.removeElectricalLoad(load)
        Eln.simulator.removeAllElectricalProcess(electricalProcessList)
        Eln.simulator.removeAllThermalConnection(thermalConnectionList)
        for (load in thermalLoadList) Eln.simulator.removeThermalLoad(load)
        Eln.simulator.removeAllThermalFastProcess(thermalFastProcessList)
        Eln.simulator.removeAllThermalSlowProcess(thermalSlowProcessList)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        front = readFromNBT(nbt, "SNfront")
        descriptorKey = nbt.getString("SNdescriptorKey")
        for (electricalLoad in electricalLoadList) {
            if (electricalLoad is INBTTReady) (electricalLoad as INBTTReady).readFromNBT(nbt, "")
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
        for (process in thermalFastProcessList) {
            if (process is INBTTReady) (process as INBTTReady).readFromNBT(nbt, "")
        }
        for (process in thermalSlowProcessList) {
            if (process is INBTTReady) (process as INBTTReady).readFromNBT(nbt, "")
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        front!!.writeToNBT(nbt, "SNfront")
        nbt.setString("SNdescriptorKey", if (descriptorKey == null) "" else descriptorKey)
        for (electricalLoad in electricalLoadList) {
            if (electricalLoad is INBTTReady) (electricalLoad as INBTTReady).writeToNBT(nbt, "")
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
        for (process in thermalFastProcessList) {
            if (process is INBTTReady) (process as INBTTReady).writeToNBT(nbt, "")
        }
        for (process in thermalSlowProcessList) {
            if (process is INBTTReady) (process as INBTTReady).writeToNBT(nbt, "")
        }
    }
}
