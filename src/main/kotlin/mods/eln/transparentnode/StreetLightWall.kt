package mods.eln.transparentnode

import mods.eln.ghost.GhostGroup
import mods.eln.gui.*
import mods.eln.i18n.I18N
import mods.eln.item.IConfigurable
import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.misc.*
import mods.eln.misc.Utils.plotPower
import mods.eln.misc.Utils.plotValue
import mods.eln.misc.Utils.plotVolt
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.lampsupply.AvailableSupply
import mods.eln.sixnode.lampsupply.IWirelessPower
import mods.eln.sixnode.lampsupply.LampSupplyConnectionHelper
import mods.eln.sixnode.lampsupply.PowerChannelTextboxHelper
import mods.eln.transparentnode.festive.FestiveGui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.pow

class StreetLightWallDescriptor(name: String, val obj: Obj3D) :
    TransparentNodeDescriptor(name, StreetLightWallElement::class.java, StreetLightWallRender::class.java) {

    private var fixture: Obj3D.Obj3DPart? = null
    private var part2: Obj3D.Obj3DPart? = null
    private var part3: Obj3D.Obj3DPart? = null

    init {
        fixture = obj.getPart("Structure_StreetLightWall_socket")
        part2 = obj.getPart("Glass_StreetLightWall_socket.002")
        part3 = obj.getPart("Light_StreetLightWall_socket.003")
        val gg = GhostGroup()
        gg.addElement(0, 1, 0)
        ghostGroup = gg
        mustHaveWall()
    }

    fun draw(front: Direction, powered: Boolean) {
        if (fixture != null && part2 != null && part3 != null) {
            front.glRotateZnRef()
            GL11.glTranslated(0.0, -0.5, -0.5)
            GL11.glRotated(90.0, 0.0, 0.0, 1.0)
            if (powered) {
                UtilsClient.drawLight(part2)
                UtilsClient.drawLight(part3)
            } else {
                part2?.draw()
                part3?.draw()
            }
            fixture?.draw()
        }
    }
}


class StreetLightWallElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElement(node, descriptor), IConfigurable {

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    val loadResistor = Resistor(electricalLoad, null)
    var lampSupplyChannel = PowerChannelTextboxHelper.DEFAULT_CHANNEL_STRING
    var activeLampSupplyConnection = false
    val nominalVoltage = NominalVoltage.V240
    val minVoltageFactor = 0.75

    init {
        loadResistor.resistance = 1152.0 // 50W at 240V
        slowProcessList.add(StreetLightWallElementProcess(this))
    }

    override fun thermoMeterString(side: Direction): String {
        return ""
    }

    override fun multiMeterString(side: Direction): String {
        return Utils.plotUIP(electricalLoad.voltage, electricalLoad.current)
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return null
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun initialize() {
        connect()
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = LinkedHashMap()

        info[I18N.tr("Power Consumption")] = plotPower("", electricalLoad.voltage.pow(2) / loadResistor.resistance)

        if (Utils.isWailaEasyModeEnabled()) {
            info[I18N.tr("Voltage")] = plotVolt("", electricalLoad.voltage)
            info[I18N.tr("Channel")] = lampSupplyChannel
        }

        if (Utils.isDebugEnabled()) {
            info[I18N.tr("Brightness")] = plotValue(node!!.lightValue.toDouble())
        }

        return info
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeBoolean(node!!.lightValue > 4)
            stream.writeUTF(lampSupplyChannel)
            stream.writeBoolean(activeLampSupplyConnection)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        when (super.networkUnserialize(stream)) {
            StreetLightWallGui.UPDATE_LAMP_SUPPLY_CHANNEL_EVENT -> {
                lampSupplyChannel = stream.readUTF()
                needPublish()
            }
        }
        return unserializeNulldId
    }

    override fun hasGui() = true

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        if (nbt.hasKey("lampSupplyChannel")) lampSupplyChannel = nbt.getString("lampSupplyChannel")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setString("lampSupplyChannel", lampSupplyChannel)
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("lampSupplyChannel")) {
            lampSupplyChannel = compound.getString("lampSupplyChannel")
            needPublish()
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setString("lampSupplyChannel", lampSupplyChannel)
    }

    class StreetLightWallElementProcess(val elem: StreetLightWallElement): IProcess, IWirelessPower {
        override var previousConnectedSupply: AvailableSupply? = null

        override val powerChannel: String
            get() = elem.lampSupplyChannel
        override val coordinate: Coordinate
            get() = elem.coordinate()
        override val loadResistance: Double
            get() = elem.loadResistor.resistance

        override fun updateLoadVoltage(newVoltage: Double) {
            elem.electricalLoad.voltage = newVoltage
        }

        override fun process(time: Double) {
            elem.activeLampSupplyConnection = LampSupplyConnectionHelper.connectToLampSupply(this)
            var newLightValue = 0
            val lampVoltage = abs(elem.loadResistor.voltage)

            if (lampVoltage > (elem.nominalVoltage * elem.minVoltageFactor)) {
                val num = lampVoltage - (elem.nominalVoltage * elem.minVoltageFactor)
                val den = elem.nominalVoltage - (elem.nominalVoltage * elem.minVoltageFactor)

                newLightValue = ((num / den) * BoilerplateLampData.V240_NOMINAL_LIGHT_VALUE).toInt()

                if (newLightValue < BoilerplateLampData.MIN_LIGHT_VALUE) newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                else if (newLightValue > BoilerplateLampData.MAX_LIGHT_VALUE) newLightValue = BoilerplateLampData.MAX_LIGHT_VALUE
            }

            elem.node!!.lightValue = newLightValue
            elem.needPublish()
        }
    }
}

class StreetLightWallRender(tileEntity: TransparentNodeEntity, transparentNodeDescriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(tileEntity, transparentNodeDescriptor) {

    var powered = false
    var lampSupplyChannel = PowerChannelTextboxHelper.DEFAULT_CHANNEL_STRING
    var activeLampSupplyConnection = false

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            powered = stream.readBoolean()
            lampSupplyChannel = stream.readUTF()
            activeLampSupplyConnection = stream.readBoolean()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun draw() {
        (transparentNodeDescriptor as StreetLightWallDescriptor).draw(front!!, powered)
    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return StreetLightWallGui(this)
    }
}

class StreetLightWallGui(val render: StreetLightWallRender) : GuiScreenEln() {

    companion object {
        const val UPDATE_LAMP_SUPPLY_CHANNEL_EVENT: Byte = 0
    }

    private lateinit var textboxLampSupplyChannel: GuiTextFieldEln

    override fun newHelper(): GuiHelper {
        return GuiHelperContainer(this, 154, 30, 0, 0)
    }

    override fun initGui() {
        super.initGui()
        textboxLampSupplyChannel = newGuiTextField(8, 8, 138)
        PowerChannelTextboxHelper.initPowerChannelTextbox(textboxLampSupplyChannel, render.lampSupplyChannel)
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        PowerChannelTextboxHelper.updatePowerChannelTextboxTooltip(
            textboxLampSupplyChannel,
            render.lampSupplyChannel,
            render.activeLampSupplyConnection
        )
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)
        if (obj === textboxLampSupplyChannel) {
            render.clientSendString(FestiveGui.UPDATE_LAMP_SUPPLY_CHANNEL_EVENT, textboxLampSupplyChannel.text)
        }
    }

}