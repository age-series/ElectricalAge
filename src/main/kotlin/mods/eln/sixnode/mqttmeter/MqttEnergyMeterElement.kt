package mods.eln.sixnode.mqttmeter

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.mqtt.MqttManager
import mods.eln.mqtt.MqttMeterCommand
import mods.eln.mqtt.MqttMeterInfo
import mods.eln.mqtt.MqttMeterManager
import mods.eln.mqtt.MqttMeterRegistry
import mods.eln.mqtt.MqttMeterSnapshot
import mods.eln.node.AutoAcceptInventoryProxy
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.ResistorSwitch
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import mods.eln.sound.SoundCommand
import mods.eln.i18n.I18N.tr
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs
import java.util.LinkedHashMap

class MqttEnergyMeterElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {

    private val inventoryProxy = AutoAcceptInventoryProxy(SixNodeElementInventory(1, 64, this))
        .acceptIfEmpty(
            MqttEnergyMeterContainer.cableSlotId,
            ElectricalCableDescriptor::class.java,
            CurrentCableDescriptor::class.java
        )
    val elementInventory: SixNodeElementInventory
        get() = inventoryProxy.inventory as SixNodeElementInventory
    val aLoad = NbtElectricalLoad("mqttA")
    val bLoad = NbtElectricalLoad("mqttB")
    private val shunt = ResistorSwitch("mqttMeterShunt", aLoad, bLoad)
    private val voltageWatchDogA = VoltageStateWatchDog(aLoad)
    private val voltageWatchDogB = VoltageStateWatchDog(bLoad)
    val slowProcess = SlowProcess()
    val descriptorRef = descriptor as EnergyMeterDescriptor

    private var cableDescriptor: GenericCableDescriptor? = null
    private var energyStack = 0.0
    private var timeCounter = 0.0
    private var energyUnit = 1
    private var timeUnit = 0
    private var meterInfo = MqttMeterInfo(meterName = "Meter")
    private var serverMatched = false
    private var lastPublishEnergy = 0.0
    companion object {
        const val CLIENT_SET_NAME: Byte = 1
        const val CLIENT_SET_SERVER: Byte = 2
        const val CLIENT_RESET_ENERGY: Byte = 3
        const val CLIENT_TOGGLE_STATE: Byte = 4
        const val CLIENT_SET_ENERGY_UNIT: Byte = 5
        const val CLIENT_SET_TIME_UNIT: Byte = 6
        const val SERVER_STATS: Byte = 1
    }

    init {
        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        electricalComponentList.add(shunt)
        electricalComponentList.add(Resistor(bLoad, null).pullDown())
        electricalComponentList.add(Resistor(aLoad, null).pullDown())
        slowProcessList.add(slowProcess)
        val explosion = WorldExplosion(this).cableExplosion()
        slowProcessList.add(voltageWatchDogA)
        slowProcessList.add(voltageWatchDogB)
        voltageWatchDogA.setDestroys(explosion)
        voltageWatchDogB.setDestroys(explosion)
    }

    override val inventory: IInventory?
        get() = inventoryProxy.inventory

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (onBlockActivatedRotate(entityPlayer)) return true
        if (inventoryProxy.take(entityPlayer.currentEquippedItem, this, false, true)) {
            return true
        }
        return false
    }

    override fun hasGui(): Boolean = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return MqttEnergyMeterContainer(player, elementInventory)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (front == lrdu) return aLoad
        if (front.inverse() == lrdu) return bLoad
        return null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU): Int {
        if (elementInventory.getStackInSlot(MqttEnergyMeterContainer.cableSlotId) == null) return 0
        if (front == lrdu || front.inverse() == lrdu) return NodeBase.maskElectricalAll
        return 0
    }

    override fun inventoryChanged() {
        updateCable()
        reconnect()
    }

    private fun updateCable() {
        val stack = elementInventory.getStackInSlot(MqttEnergyMeterContainer.cableSlotId)
        val descriptor = Eln.sixNodeItem.getDescriptor(stack)
        cableDescriptor = descriptor as? GenericCableDescriptor
        val cable = cableDescriptor
        if (cable == null) {
            aLoad.highImpedance()
            bLoad.highImpedance()
        } else {
            cable.applyTo(aLoad)
            cable.applyTo(bLoad)
            val nominal = cable.electricalNominalVoltage
            voltageWatchDogA.setNominalVoltage(nominal)
            voltageWatchDogB.setNominalVoltage(nominal)
        }
    }

    override fun initialize() {
        updateCable()
        ensureMeterId()
        ensureDefaultServerName()
        updateServerMatch()
    }

    override fun multiMeterString(): String {
        val flow = captureFlow()
        return Utils.plotVolt("U:", flow.voltage) +
            Utils.plotAmpere(" I:", flow.current) +
            Utils.plotPower(" P:", flow.power)
    }

    override fun getWaila(): Map<String, String> {
        val flow = captureFlow()
        val info = LinkedHashMap<String, String>()
        info[tr("Voltage")] = Utils.plotVolt("", flow.voltage)
        info[tr("Current")] = Utils.plotAmpere("", flow.current)
        info[tr("Power")] = Utils.plotPower("", flow.power)
        info[tr("Energy (kJ)")] = String.format("%.2f kJ", energyStack / 1000.0)
        info[tr("Elapsed time")] = Utils.plotTime("", timeCounter / 72.0)
        info[tr("Meter ID")] = meterInfo.meterId
        return info
    }

    override fun destroy(entityPlayer: EntityPlayerMP?) {
        super.destroy(entityPlayer)
        mqttDisconnect()
        MqttMeterRegistry.release(coordinate)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        energyStack = nbt.getDouble("mqttEnergy")
        lastPublishEnergy = energyStack
        timeCounter = nbt.getDouble("mqttTime")
        energyUnit = nbt.getByte("mqttEnergyUnit").toInt()
        timeUnit = nbt.getByte("mqttTimeUnit").toInt()
        val name = nbt.getString("mqttMeterName")
        val serverName = nbt.getString("mqttServerName")
        val id = nbt.getString("mqttMeterId")
        val enabled = nbt.getBoolean("mqttEnabled")
        meterInfo = MqttMeterInfo(name, serverName, id, enabled)
        ensureMeterId()
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble("mqttEnergy", energyStack)
        nbt.setDouble("mqttTime", timeCounter)
        nbt.setByte("mqttEnergyUnit", energyUnit.toByte())
        nbt.setByte("mqttTimeUnit", timeUnit.toByte())
        nbt.setString("mqttMeterName", meterInfo.meterName)
        nbt.setString("mqttServerName", meterInfo.serverName)
        nbt.setString("mqttMeterId", meterInfo.meterId)
        nbt.setBoolean("mqttEnabled", meterInfo.enabled)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeBoolean(shunt.getState())
            stream.writeUTF(meterInfo.meterName)
            stream.writeUTF(meterInfo.serverName)
            stream.writeUTF(meterInfo.meterId)
            stream.writeBoolean(meterInfo.enabled)
            stream.writeBoolean(serverMatched)
            stream.writeDouble(timeCounter)
            Utils.serialiseItemStack(stream, elementInventory.getStackInSlot(MqttEnergyMeterContainer.cableSlotId))
            stream.writeByte(energyUnit)
            stream.writeByte(timeUnit)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP?) {
        super.networkUnserialize(stream, player)
        try {
            when (stream.readByte().toInt()) {
                CLIENT_SET_NAME.toInt() -> {
                    meterInfo = meterInfo.copy(meterName = stream.readUTF().trim().take(64))
                    needPublish()
                }
                CLIENT_SET_SERVER.toInt() -> {
                    meterInfo = meterInfo.copy(serverName = stream.readUTF().trim().take(64))
                    updateServerMatch()
                    ensureMeterId()
                    needPublish()
                }
                CLIENT_RESET_ENERGY.toInt() -> resetCounters()
                CLIENT_TOGGLE_STATE.toInt() -> toggleState()
                CLIENT_SET_ENERGY_UNIT.toInt() -> {
                    energyUnit = (energyUnit + 1) % 4
                    needPublish()
                }
                CLIENT_SET_TIME_UNIT.toInt() -> {
                    timeUnit = (timeUnit + 1) % 2
                    needPublish()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun resetCounters() {
        energyStack = 0.0
        timeCounter = 0.0
        lastPublishEnergy = energyStack
        needPublish()
    }

    private fun toggleState() {
        val newState = !meterInfo.enabled
        setEnabledState(newState)
        play(SoundCommand("random.click").mulVolume(0.3f, 0.6f).smallRange())
    }

    private fun ensureMeterId() {
        val coord = coordinate ?: return
        val requested = meterInfo.meterId.takeIf { it.isNotBlank() }
        val id = MqttMeterRegistry.ensureMeterId(coord, requested)
        if (id != meterInfo.meterId) {
            meterInfo = meterInfo.copy(meterId = id)
        }
    }

    private fun ensureDefaultServerName() {
        if (meterInfo.serverName.isNotBlank()) return
        val config = MqttManager.getConfig()
        if (config.disable) return
        val servers = config.mqtt.filter { it.name.isNotBlank() }
        if (servers.size == 1) {
            meterInfo = meterInfo.copy(serverName = servers[0].name)
        }
    }

    private fun updateServerMatch() {
        val match = !MqttManager.getConfig().disable && MqttManager.getServerByName(meterInfo.serverName) != null
        if (match != serverMatched) {
            serverMatched = match
            if (!serverMatched) {
                mqttDisconnect()
            }
            needPublish()
        }
    }

    private fun mqttDisconnect() {
        coordinate?.let { MqttMeterManager.release(it) }
    }

    private fun handleCommand(command: MqttMeterCommand) {
        when (command) {
            is MqttMeterCommand.Reset -> resetCounters()
            is MqttMeterCommand.SetStatus -> setEnabledState(command.enabled)
        }
    }

    private fun setEnabledState(enabled: Boolean) {
        if (enabled == meterInfo.enabled) return
        meterInfo = meterInfo.copy(enabled = enabled)
        shunt.setState(enabled)
        needPublish()
    }

    inner class SlowProcess : IProcess {
        private val publishInterval = 2.0
        private val mqttInterval = 5.0
        private var publishTimer = publishInterval
        private var mqttTimer = mqttInterval

        override fun process(time: Double) {
            val flow = captureFlow()
            val power = flow.power

            if (meterInfo.enabled) {
                shunt.setState(true)
                energyStack += power * time
            } else {
                shunt.setState(false)
            }

            timeCounter += time * 72.0
            publishTimer -= time
            mqttTimer -= time

            if (publishTimer <= 0.0) {
                publishTimer += publishInterval
                sendStatsToClients()
            }

            if (mqttTimer <= 0.0) {
                mqttTimer += mqttInterval
                publishToMqtt(flow)
            }
        }
    }

    private fun sendStatsToClients() {
        val bos = ByteArrayOutputStream()
        val packet = DataOutputStream(bos)
        try {
            preparePacketForClient(packet)
            packet.writeByte(SERVER_STATS.toInt())
            packet.writeDouble(lastPublishEnergy)
            packet.writeDouble((energyStack - lastPublishEnergy) / 2.0)
            sendPacketToAllClient(bos, 10.0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        lastPublishEnergy = energyStack
    }

    private fun publishToMqtt(flow: FlowSample) {
        val coord = coordinate ?: return
        if (!serverMatched) return
        val server = MqttManager.getServerByName(meterInfo.serverName) ?: return
        MqttMeterManager.ensureSubscription(meterInfo.meterId, coord, server, this::handleCommand)
        val dimensionName = coord.world().provider.dimensionName
        val timeSeconds = timeCounter / 72.0
        val snapshot = MqttMeterSnapshot(
            meterInfo,
            coord,
            dimensionName,
            flow.power,
            energyStack / 1000.0,
            timeSeconds,
            flow.current,
            flow.voltage,
            meterInfo.enabled
        )
        MqttMeterManager.publishSnapshot(snapshot)
    }

    private fun captureFlow(): FlowSample {
        val inputVoltage = aLoad.voltage
        val outputVoltage = bLoad.voltage
        val direction = if (inputVoltage >= outputVoltage) 1.0 else -1.0
        val directedCurrent = abs(aLoad.current) * direction
        val power = directedCurrent * abs(inputVoltage)
        return FlowSample(inputVoltage, directedCurrent, power)
    }

    private data class FlowSample(
        val voltage: Double = 0.0,
        val current: Double = 0.0,
        val power: Double = 0.0
    )
}
