package mods.eln.sixnode.powersocket

import mods.eln.generic.GenericItemUsingDamageDescriptor.Companion.getDescriptor
import mods.eln.item.BrushDescriptor
import mods.eln.item.IConfigurable
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.plotUIP
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.sixnode.lampsupply.LampSupplyElement.PowerSupplyChannelHandle
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class PowerSocketElement(sixNode: SixNode?, side: Direction?, descriptor: SixNodeDescriptor?) : SixNodeElement(
    sixNode!!, side!!, descriptor!!
), IConfigurable {
    var descriptor: PowerSocketDescriptor?
    var outputLoad = NbtElectricalLoad("outputLoad")
    var loadResistor = Resistor(null, null) // Connected in process()
    private var powerSocketSlowProcess: IProcess = PowerSocketSlowProcess()
    var channel = "Default channel"
    var paintColor = 0
    var voltageWatchdog = VoltageStateWatchDog(outputLoad)

    init {
        electricalLoadList.add(outputLoad)
        electricalComponentList.add(loadResistor)
        slowProcessList.add(powerSocketSlowProcess)
        loadResistor.highImpedance()
        this.descriptor = descriptor as PowerSocketDescriptor?
        slowProcessList.add(voltageWatchdog)
        voltageWatchdog.setDestroys(WorldExplosion(this).cableExplosion())
        voltageWatchdog.setNominalVoltage(300.0)
    }

    internal inner class PowerSocketSlowProcess : IProcess {
        override fun process(time: Double) {
            val local = sixNode!!.coordinate
            var handle: PowerSupplyChannelHandle? = null
            var bestDist = 1e9f
            val handles: List<PowerSupplyChannelHandle>? = LampSupplyElement.channelMap[channel]
            if (handles != null) {
                for (hdl in handles) {
                    val dist = hdl.element.sixNode!!.coordinate.trueDistanceTo(local).toFloat()
                    if (dist < bestDist && dist <= hdl.element.range) {
                        bestDist = dist
                        handle = hdl
                    }
                }
            }
            loadResistor.breakConnection()
            loadResistor.highImpedance()
            if (handle != null && handle.element.getChannelState(handle.id)) {
                loadResistor.connectTo(handle.element.powerLoad, outputLoad)
                loadResistor.resistance = 0.1
            }
        }
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad {
        return outputLoad
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return NodeBase.maskElectricalPower + (1 shl NodeBase.maskColorCareShift) + (paintColor shl NodeBase.maskColorShift)
    }

    override fun multiMeterString(): String {
        return plotUIP(outputLoad.voltage, outputLoad.getCurrent())
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun initialize() {
        outputLoad.serialResistance = 0.1
    }

    override fun inventoryChanged() {
        super.inventoryChanged()
        sixNode!!.disconnect()
        sixNode!!.connect()
        needPublish()
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setString("channel", channel)
        nbt.setInteger("color", paintColor)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        channel = nbt.getString("channel")
        paintColor = nbt.getInteger("color")
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            when (stream.readByte()) {
                setChannelId -> {
                    channel = stream.readUTF()
                    needPublish()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeUTF(channel)
            stream.writeInt(paintColor)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBlockActivated(
        entityPlayer: EntityPlayer,
        side: Direction,
        vx: Float,
        vy: Float,
        vz: Float
    ): Boolean {
        val used = entityPlayer.currentEquippedItem
        if (used != null) {
            val desc = getDescriptor(used)
            if (desc != null && desc is BrushDescriptor) {
                val color: Int = desc.getColor(used)
                if (color != paintColor && desc.use(used, entityPlayer)) {
                    paintColor = color
                    sixNode!!.reconnect()
                }
                return true
            }
        }
        return false
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("powerChannels")) {
            val newChannel = compound.getTagList("powerChannels", 8).getStringTagAt(0)
            if (newChannel != null && !newChannel.isEmpty()) {
                channel = newChannel
                needPublish()
            }
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        val list = NBTTagList()
        list.appendTag(NBTTagString(channel))
        compound.setTag("powerChannels", list)
    }

    companion object {
        const val setChannelId: Byte = 1
    }
}
