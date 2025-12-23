package mods.eln.sixnode.mqttsignal

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.PhysicalInterpolator
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.mqtt.SignalPort
import mods.eln.mqtt.SignalPortMode
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException

class MqttSignalControllerRender(
    tileEntity: SixNodeEntity,
    side: Direction,
    descriptor: SixNodeDescriptor
) : SixNodeElementRender(tileEntity, side, descriptor) {

    private val descriptorRef = descriptor as MqttSignalControllerDescriptor
    private val interpolator = PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f)
    private val controllerInventory = SixNodeElementInventory(0, 64, this)
    private val ledState = BooleanArray(8)
    private val signalPorts = SignalPort.values()
    private val portModes = Array(signalPorts.size) { SignalPortMode.DISABLED }
    private val coord = Coordinate(tileEntity)

    var controllerName: String = ""
        private set
    var serverName: String = ""
        private set
    var controllerId: String = ""
        private set
    private var serverMatched: Boolean = false

    private var ledTimer = 0f

    init {
        ledState[0] = true
        ledState[4] = true
    }

    override val inventory: SixNodeElementInventory?
        get() = controllerInventory

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            controllerName = stream.readUTF()
            serverName = stream.readUTF()
            controllerId = stream.readUTF()
            serverMatched = stream.readBoolean()
            signalPorts.forEach { port ->
                val ordinal = stream.readByte().toInt()
                portModes[port.ordinal] = SignalPortMode.fromOrdinal(ordinal) ?: SignalPortMode.DISABLED
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun draw() {
        super.draw()
        val facing = front
        var pinDistances = descriptorRef.pinDistance
        if (side.isY && facing != null && pinDistances != null) {
            pinDistances = facing.rotate4PinDistances(pinDistances)
        }

        if (UtilsClient.distanceFromClientPlayer(tileEntity) < 15 && facing != null && pinDistances != null) {
            GL11.glColor3f(0f, 0f, 0f)
            UtilsClient.drawConnectionPinSixNode(facing, pinDistances, 1.8f, 1.35f)
            GL11.glColor3f(1f, 0f, 0f)
            UtilsClient.drawConnectionPinSixNode(facing.right(), pinDistances, 1.8f, 1.35f)
            GL11.glColor3f(0f, 1f, 0f)
            UtilsClient.drawConnectionPinSixNode(facing.inverse(), pinDistances, 1.8f, 1.35f)
            GL11.glColor3f(0f, 0f, 1f)
            UtilsClient.drawConnectionPinSixNode(facing.left(), pinDistances, 1.8f, 1.35f)
            GL11.glColor3f(1f, 1f, 1f)
        }

        if (side.isY) {
            facing?.left()?.glRotateOnX()
        }

        descriptorRef.draw(interpolator.get(), ledState)
    }

    override fun refresh(deltaT: Float) {
        ledTimer += deltaT
        if (ledTimer > 0.4f) {
            for (i in 1..3) {
                ledState[i] = Math.random() < 0.3
            }
            for (i in 5..7) {
                ledState[i] = Math.random() < 0.3
            }
            ledTimer = 0f
        }
        if (!Utils.isPlayerAround(tileEntity.worldObj, coord.getAxisAlignedBB(0))) {
            interpolator.target = 0f
        } else {
            interpolator.target = 1f
        }
        interpolator.step(deltaT)
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return Eln.instance.signalCableDescriptor.render
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return MqttSignalControllerGui(player, controllerInventory, this)
    }

    fun getPortMode(port: SignalPort): SignalPortMode = portModes[port.ordinal]

    fun isServerKnown(): Boolean = serverMatched
}
