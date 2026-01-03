package mods.eln.sixnode.mqttmeter

import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException

class MqttEnergyMeterRender(
    tileEntity: SixNodeEntity,
    side: Direction,
    descriptor: SixNodeDescriptor
) : SixNodeElementRender(tileEntity, side, descriptor) {

    private val meterInventory = SixNodeElementInventory(1, 64, this)
    private val descriptorRef = descriptor as EnergyMeterDescriptor
    private var timerCounter = 0.0
    private var energyStack = 0.0
    private var switchState = true
    var energyUnit = 1
        private set
    var timeUnit = 0
        private set
    private var cableRender: CableRenderDescriptor? = null
    private var power = 0.0
    private var error = 0.0
    private var serverPowerIdTimer = 10.0

    var meterName: String = ""
    var serverName: String = ""
    var meterId: String = ""
    var meterEnabled: Boolean = true
        private set
    private var serverMatched: Boolean = false

    override val inventory: IInventory?
        get() = meterInventory

    override fun draw() {
        super.draw()
        GL11.glPushMatrix()

        var pinDistances = descriptorRef.pinDistance
        val facing = front
        if (side.isY && facing != null) {
            pinDistances = facing.rotate4PinDistances(pinDistances)
            facing.left().glRotateOnX()
        }

        val energyDisplay = energyStack / Math.pow(10.0, (energyUnit * 3 - 1).toDouble())
        val timeDisplay = timerCounter / if (timeUnit == 0) 360.0 else 8640.0
        descriptorRef.draw(
            energyDisplay,
            timeDisplay,
            energyUnit,
            timeUnit,
            UtilsClient.distanceFromClientPlayer(tileEntity) < 20
        )

        GL11.glPopMatrix()

        GL11.glColor3f(0.9f, 0f, 0f)
        drawPowerPinWhite(front, pinDistances)
        GL11.glColor3f(0f, 0f, 0.9f)
        drawPowerPinWhite(front?.inverse(), pinDistances)
        GL11.glColor3f(1f, 1f, 1f)
    }

    override fun refresh(deltaT: Float) {
        val errorComp = error * deltaT
        energyStack += power * deltaT + errorComp
        error -= errorComp
        timerCounter += deltaT * 72.0
        serverPowerIdTimer += deltaT
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? = cableRender

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            switchState = stream.readBoolean()
            meterName = stream.readUTF()
            serverName = stream.readUTF()
            meterId = stream.readUTF()
            meterEnabled = stream.readBoolean()
            serverMatched = stream.readBoolean()
            timerCounter = stream.readDouble()
            val itemStack = Utils.unserialiseItemStack(stream)
            energyUnit = stream.readByte().toInt()
            timeUnit = stream.readByte().toInt()
            cableRender = GenericCableDescriptor.getCableRender(itemStack)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun serverPacketUnserialize(stream: DataInputStream?) {
        super.serverPacketUnserialize(stream)
        stream ?: return
        when (stream.readByte().toInt()) {
            MqttEnergyMeterElement.SERVER_STATS.toInt() -> {
                if (serverPowerIdTimer > 3.0) {
                    energyStack = stream.readDouble()
                    error = 0.0
                } else {
                    error = stream.readDouble() - energyStack
                }
                power = stream.readDouble()
                serverPowerIdTimer = 0.0
            }
        }
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return MqttEnergyMeterGui(player, meterInventory, this)
    }

    fun isServerKnown(): Boolean = serverMatched
}
