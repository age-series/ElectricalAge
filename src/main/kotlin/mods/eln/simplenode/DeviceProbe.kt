package mods.eln.simplenode

import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.misc.LRDU
import mods.eln.node.simple.SimpleNode
import mods.eln.node.simple.SimpleNodeBlock
import mods.eln.node.simple.SimpleNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInputOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.block.material.Material
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.world.World
import java.io.DataInputStream
import java.io.DataOutputStream


class DeviceProbeBlock: SimpleNodeBlock(Material.ice) {
    var icon: IIcon? = null

    override fun createNewTileEntity(world: World?, metadata: Int): TileEntity {
        return DeviceProbeEntity()
    }

    override fun newNode(): SimpleNode {
        return DeviceProbeNode()
    }

    override fun registerBlockIcons(register: IIconRegister?) {
        icon = register!!.registerIcon("eln:deviceprobe")
    }
}

class DeviceProbeNode: SimpleNode() {

    val pinInformation = mutableListOf<ServerPinInformation>()
    val serialPort = ""

    init {
        (0 until 6).forEach {
            idx ->
            val name = Direction.fromInt(idx)!!.name
            val pin = NbtElectricalGateInputOutput(name)
            val process = NbtElectricalGateOutputProcess(name, pin)
            process.isHighImpedance = true
            val pInfo = ServerPinInformation(pin, process, null, DirectionalMode.INPUT, PortMode.DIGITAL)
            pinInformation.add(pInfo)
        }
    }

    override val nodeUuid: String
        get() = getNodeUuidStatic()

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        return maskElectricalGate
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad {
        return pinInformation[side.toSideValue()].electricalLoadPin
    }

    override fun initialize() {
        pinInformation.forEach {
            electricalLoadList.add(it.electricalLoadPin)
            electricalComponentList.add(it.electricalProcess)
        }
        connect()
    }

    companion object {
        fun getNodeUuidStatic(): String {
            return "ElnDeviceProbe"
        }
    }

    override fun hasGui(side: Direction): Boolean {
        return true
    }

    override fun publishSerialize(stream: DataOutputStream) {
        super.publishSerialize(stream)
        Direction.values().forEach {
            side ->
            pinInformation[side.toSideValue()].writeToNetwork(stream)
        }
    }

    override fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP?) {
        super.networkUnserialize(stream, player)
        Direction.values().forEach {
            side ->
            pinInformation[side.toSideValue()].readFromNetwork(stream)
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        if (nbt.hasKey("arduinoData")) {
            Direction.values().forEach {
                side ->
                pinInformation[side.toSideValue()].readFromNBT(nbt, side.name)
            }
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setBoolean("arduinoData", true)
        Direction.values().forEach {
            side ->
            pinInformation[side.toSideValue()].writeToNBT(nbt, side.name)
        }
    }
}

data class ServerPinInformation(
    var electricalLoadPin: NbtElectricalGateInputOutput,
    var electricalProcess: NbtElectricalGateOutputProcess,
    var arduinoPin: Int?,
    var direction: DirectionalMode,
    var portMode: PortMode
    ): INBTTReady {

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        electricalLoadPin.writeToNBT(nbt, str)
        electricalProcess.writeToNBT(nbt, str)
        if (arduinoPin != null)
            nbt.setInteger("${str}arduinoPin", arduinoPin!!)
        nbt.setInteger("${str}direction", direction.id)
        nbt.setInteger("${str}portMode", portMode.id)
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        electricalLoadPin.readFromNBT(nbt, str)
        electricalProcess.readFromNBT(nbt, str)
        arduinoPin = if (nbt.hasKey("${str}arduinoPin")) {
            nbt.getInteger("${str}arduinoPin")
        } else {
            null
        }
        direction = intToDirectionalMode(nbt.getInteger("${str}direction"))
        portMode = intToPortMode(nbt.getInteger("${str}portMode"))
    }

    fun writeToNetwork(stream: DataOutputStream) {
        stream.writeInt(arduinoPin?: -1)
        stream.writeInt(direction.id)
        stream.writeInt(portMode.id)
    }

    fun readFromNetwork(stream: DataInputStream) {
        val int = stream.readInt()
        arduinoPin = if (int == -1) null else int
        direction = intToDirectionalMode(stream.readInt())
        portMode = intToPortMode(stream.readInt())
    }
}

data class ClientPinInformation(
    var arduinoPin: Int?,
    var direction: DirectionalMode,
    var portMode: PortMode,
    var directionButton: GuiButtonEln?,
    var portModeButton: GuiButtonEln?,
    var arduinoPinField: GuiTextFieldEln?
) {
    fun writeToNetwork(stream: DataOutputStream) {
        stream.writeInt(arduinoPin?: -1)
        stream.writeInt(direction.id)
        stream.writeInt(portMode.id)
    }

    fun readFromNetwork(stream: DataInputStream) {
        val int = stream.readInt()
        arduinoPin = if (int == -1) null else int
        direction = intToDirectionalMode(stream.readInt())
        portMode = intToPortMode(stream.readInt())
    }
}

enum class DirectionalMode(val id: Int) {
    OUTPUT(0),
    INPUT(1),
    INPUT_PULLUP(2)
}

fun intToDirectionalMode(int: Int): DirectionalMode {
    return DirectionalMode.values().first { it.id == int}
}

enum class PortMode(val id: Int) {
    DIGITAL(0),
    ANALOG(1),
    PWM(2)
}

fun intToPortMode(int: Int): PortMode {
    return PortMode.values().first { it.id == int}
}

class DeviceProbeEntity : SimpleNodeEntity("ElnDeviceProbe") {

    val pinInformation = mutableListOf<ClientPinInformation>()

    init {
        (0 until 6).forEach {
            _ ->
            val pInfo = ClientPinInformation(null, DirectionalMode.INPUT, PortMode.DIGITAL, null, null, null)
            pinInformation.add(pInfo)
        }
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return DeviceProbeGui(this)
    }

    override fun serverPublishUnserialize(stream: DataInputStream) {
        super.serverPublishUnserialize(stream)
        Direction.values().forEach {
            side ->
            pinInformation[side.toSideValue()].readFromNetwork(stream)
        }
    }
}

class DeviceProbeGui(var render: DeviceProbeEntity): GuiScreenEln() {

    var port: GuiTextFieldEln? = null

    val pinWidth = 24
    val buttonWidth = 60
    val boxWidth = 2 + pinWidth + 2 + buttonWidth + 2
    val boxHeight = 44

    fun drawBox(x: Int, y: Int, pinWidth: Int, buttonWidth: Int, dir: Direction) {
        val pin = render.pinInformation[dir.toSideValue()]
        pin.directionButton = newGuiButton(x + 2 + pinWidth + 2, y + 2, buttonWidth, pin.direction.name)
        pin.directionButton!!.setComment(0, "Output to Arduino, input from world")
        pin.portModeButton = newGuiButton(x + 2 + pinWidth + 2, y + 2 + 20 + 2, buttonWidth, pin.portMode.name.replace("_", " "))
        pin.arduinoPinField = newGuiTextField(x + 2, y + 16, pinWidth)
        pin.arduinoPinField!!.text = if (pin.arduinoPin == null) "" else pin.arduinoPin.toString()
        helper.drawRect(x, y, x + 2 + pinWidth + 2 + buttonWidth + 2, 44, 255)
    }

    override fun initGui() {
        super.initGui()

        drawBox(6, 6, pinWidth, buttonWidth, Direction.ZN)
        drawBox(6 + boxWidth + 10, 6, pinWidth, buttonWidth, Direction.YP)
        drawBox(6 + boxWidth + 10 + boxWidth + 10, 6, pinWidth, buttonWidth, Direction.XN)

        drawBox(6, 6 + 10 + boxHeight, pinWidth, buttonWidth, Direction.XP)
        drawBox(6 + boxWidth + 10 + boxWidth + 10, 6 + 10 + boxHeight, pinWidth, buttonWidth, Direction.ZP)

        drawBox(6 + boxWidth + 10, 6 + boxHeight + 10 + boxHeight, pinWidth, buttonWidth, Direction.YN)
        port = newGuiTextField(6, 200, 50)
        port!!.enabled = true
    }

    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 6 * 2 + boxWidth * 3 + 10 * 2, 6 * 2 + boxHeight * 3 + 10 * 2)
    }
}
