package mods.eln.sixnode.mqttsignal

import mods.eln.Eln
import mods.eln.mqtt.MqttManager
import mods.eln.mqtt.MqttSignalControllerCommand
import mods.eln.mqtt.MqttSignalControllerInfo
import mods.eln.mqtt.MqttSignalControllerManager
import mods.eln.mqtt.MqttSignalControllerRegistry
import mods.eln.mqtt.SignalPort
import mods.eln.mqtt.SignalPortMode
import mods.eln.mqtt.SignalPortSnapshot
import mods.eln.mqtt.MqttSignalControllerSnapshot
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInputOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import mods.eln.i18n.I18N.tr
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.entity.player.EntityPlayerMP
import java.util.LinkedHashMap
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class MqttSignalControllerElement(
    sixNode: SixNode,
    side: Direction,
    descriptor: SixNodeDescriptor
) : SixNodeElement(sixNode, side, descriptor) {

    private val elementInventory = SixNodeElementInventory(0, 64, this)
    private val signalPorts = SignalPort.values()
    private val portLoads = Array(signalPorts.size) { index ->
        NbtElectricalGateInputOutput("mqttSignal${signalPorts[index].label}")
    }
    private val portProcesses = Array(signalPorts.size) { index ->
        NbtElectricalGateOutputProcess("mqttSignalProcess${signalPorts[index].label}", portLoads[index])
    }
    private val desiredOutputs = DoubleArray(signalPorts.size) { 0.0 }
    private val portModes = Array(signalPorts.size) { idx -> defaultModeFor(signalPorts[idx]) }
    private val slowProcess = ControllerProcess()
    private var controllerInfo = MqttSignalControllerInfo(controllerName = "Signal Controller")
    private var serverMatched = false

    init {
        portLoads.forEach { load -> electricalLoadList.add(load) }
        portProcesses.forEach { process ->
            process.setHighImpedance(true)
            electricalComponentList.add(process)
        }
        slowProcessList.add(slowProcess)
        refreshAllPortModes(true)
    }

    override val inventory: IInventory?
        get() = elementInventory

    override fun hasGui(): Boolean = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return MqttSignalControllerContainer(player, elementInventory)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        val facing = front
        return when (lrdu) {
            facing -> portLoads[SignalPort.D.ordinal]
            facing.right() -> portLoads[SignalPort.A.ordinal]
            facing.inverse() -> portLoads[SignalPort.B.ordinal]
            facing.left() -> portLoads[SignalPort.C.ordinal]
            else -> null
        }
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU): Int {
        val facing = front
        return if (lrdu == facing || lrdu == facing.right() || lrdu == facing.inverse() || lrdu == facing.left()) {
            NodeBase.maskElectricalGate
        } else {
            0
        }
    }

    override fun multiMeterString(): String {
        val builder = StringBuilder()
        signalPorts.forEachIndexed { idx, port ->
            builder.append(port.label)
            builder.append("=")
            builder.append(Utils.plotVolt("", portLoads[idx].voltage))
            if (idx < signalPorts.lastIndex) {
                builder.append(" ")
            }
        }
        return builder.toString()
    }

    override fun getWaila(): Map<String, String> {
        val info = LinkedHashMap<String, String>()
        signalPorts.forEachIndexed { idx, port ->
            val mode = portModes[idx]
            val label = when (mode) {
                SignalPortMode.DISABLED -> tr("Disabled")
                SignalPortMode.READ -> tr("Read")
                SignalPortMode.WRITE -> tr("Write")
            }
            info["${port.label} - $label"] = Utils.plotVolt("", portLoads[idx].voltage)
        }
        info[tr("Controller ID")] = controllerInfo.controllerId
        return info
    }

    override fun initialize() {
        ensureControllerId()
        ensureDefaultServerName()
        updateServerMatch()
    }

    override fun destroy(entityPlayer: EntityPlayerMP?) {
        super.destroy(entityPlayer)
        mqttDisconnect()
        MqttSignalControllerRegistry.release(coordinate)
    }

    override fun unload() {
        super.unload()
        mqttDisconnect()
        MqttSignalControllerRegistry.release(coordinate)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val name = nbt.getString("mqttControllerName")
        val server = nbt.getString("mqttServerName")
        val id = nbt.getString("mqttControllerId")
        controllerInfo = MqttSignalControllerInfo(name, server, id)
        val storedModes = nbt.getIntArray("mqttPortModes")
        if (storedModes.isNotEmpty()) {
            signalPorts.forEachIndexed { idx, port ->
                val stored = storedModes.getOrNull(idx)
                val mode = stored?.let { SignalPortMode.fromOrdinal(it) } ?: defaultModeFor(port)
                portModes[idx] = mode
            }
        }
        signalPorts.forEach { port ->
            desiredOutputs[port.ordinal] = nbt.getDouble("mqttDesired${port.label}")
        }
        ensureControllerId()
        refreshAllPortModes(true)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setString("mqttControllerName", controllerInfo.controllerName)
        nbt.setString("mqttServerName", controllerInfo.serverName)
        nbt.setString("mqttControllerId", controllerInfo.controllerId)
        val array = IntArray(signalPorts.size) { idx -> portModes[idx].ordinal }
        nbt.setIntArray("mqttPortModes", array)
        signalPorts.forEach { port ->
            nbt.setDouble("mqttDesired${port.label}", desiredOutputs[port.ordinal])
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeUTF(controllerInfo.controllerName)
            stream.writeUTF(controllerInfo.serverName)
            stream.writeUTF(controllerInfo.controllerId)
            stream.writeBoolean(serverMatched)
            signalPorts.forEach { port ->
                stream.writeByte(portModes[port.ordinal].ordinal)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP?) {
        super.networkUnserialize(stream, player)
        try {
            when (stream.readByte().toInt()) {
                CLIENT_SET_NAME.toInt() -> {
                    controllerInfo = controllerInfo.copy(controllerName = stream.readUTF().trim().take(64))
                    needPublish()
                }
                CLIENT_SET_SERVER.toInt() -> {
                    controllerInfo = controllerInfo.copy(serverName = stream.readUTF().trim().take(64))
                    updateServerMatch()
                    ensureControllerId()
                    needPublish()
                }
                CLIENT_SET_PORT_MODE.toInt() -> {
                    val packed = stream.readInt()
                    val portIndex = packed and 0xFF
                    val modeIndex = (packed shr 8) and 0xFF
                    val port = SignalPort.fromOrdinal(portIndex) ?: return
                    val mode = SignalPortMode.fromOrdinal(modeIndex) ?: return
                    setPortMode(port, mode)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun ensureControllerId() {
        val coord = coordinate ?: return
        val requested = controllerInfo.controllerId.takeIf { it.isNotBlank() }
        val id = MqttSignalControllerRegistry.ensureControllerId(coord, requested)
        if (id != controllerInfo.controllerId) {
            controllerInfo = controllerInfo.copy(controllerId = id)
        }
    }

    private fun ensureDefaultServerName() {
        if (controllerInfo.serverName.isNotBlank()) return
        val config = MqttManager.getConfig()
        if (config.disable) return
        val servers = config.mqtt.filter { it.name.isNotBlank() }
        if (servers.size == 1) {
            controllerInfo = controllerInfo.copy(serverName = servers[0].name)
        }
    }

    private fun updateServerMatch() {
        val match = !MqttManager.getConfig().disable &&
            MqttManager.getServerByName(controllerInfo.serverName) != null
        if (match != serverMatched) {
            serverMatched = match
            if (!serverMatched) {
                mqttDisconnect()
            }
            needPublish()
        }
    }

    private fun refreshAllPortModes(silent: Boolean) {
        signalPorts.forEach { port ->
            setPortMode(port, portModes[port.ordinal], silent)
        }
    }

    private fun setPortMode(port: SignalPort, mode: SignalPortMode, silent: Boolean = false) {
        val index = port.ordinal
        val current = portModes[index]
        portModes[index] = mode
        val process = portProcesses[index]
        process.setHighImpedance(mode != SignalPortMode.WRITE)
        if (mode == SignalPortMode.WRITE) {
            process.setVoltageSafe(desiredOutputs[index])
        }
        if (!silent && current != mode) {
            needPublish()
        }
    }

    private fun setDesiredVoltage(port: SignalPort, value: Double) {
        val index = port.ordinal
        var clamped = Utils.limit(value, 0.0, Eln.SVU)
        if (clamped.isNaN()) {
            clamped = 0.0
        }
        desiredOutputs[index] = clamped
        if (portModes[index] == SignalPortMode.WRITE) {
            portProcesses[index].setVoltageSafe(clamped)
        }
    }

    private fun mqttDisconnect() {
        coordinate?.let { MqttSignalControllerManager.release(it) }
    }

    private fun publishToMqtt() {
        val coord = coordinate ?: return
        if (!serverMatched) return
        val server = MqttManager.getServerByName(controllerInfo.serverName) ?: return
        MqttSignalControllerManager.ensureSubscription(controllerInfo.controllerId, coord, server, this::handleCommand)
        val dimension = coord.world().provider.dimensionName
        val snapshots = signalPorts.mapIndexed { idx, port ->
            SignalPortSnapshot(port, portModes[idx], portLoads[idx].voltage)
        }
        val snapshot = MqttSignalControllerSnapshot(controllerInfo, coord, dimension, snapshots)
        MqttSignalControllerManager.publishSnapshot(snapshot)
    }

    private fun handleCommand(command: MqttSignalControllerCommand) {
        when (command) {
            is MqttSignalControllerCommand.SetPortVoltage -> setDesiredVoltage(command.port, command.voltage)
        }
    }

    private inner class ControllerProcess : IProcess {
        private val mqttInterval = 1.0
        private var timer = mqttInterval

        override fun process(time: Double) {
            timer -= time
            if (timer <= 0.0) {
                timer += mqttInterval
                publishToMqtt()
            }
        }
    }

    companion object {
        const val CLIENT_SET_NAME: Byte = 1
        const val CLIENT_SET_SERVER: Byte = 2
        const val CLIENT_SET_PORT_MODE: Byte = 3

        private fun defaultModeFor(@Suppress("UNUSED_PARAMETER") port: SignalPort): SignalPortMode =
            SignalPortMode.DISABLED
    }
}
