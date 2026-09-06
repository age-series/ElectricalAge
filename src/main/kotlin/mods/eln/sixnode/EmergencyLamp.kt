package mods.eln.sixnode

import mods.eln.cable.CableRenderDescriptor
import mods.eln.gui.*
import mods.eln.i18n.I18N.tr
import mods.eln.item.IConfigurable
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.published
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.ResistorSwitch
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.lampsupply.AvailableSupply
import mods.eln.sixnode.lampsupply.IWirelessPower
import mods.eln.sixnode.lampsupply.LampSupplyConnectionHelper
import mods.eln.sixnode.lampsupply.PowerChannelTextboxHelper
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs
import kotlin.math.pow

class EmergencyLampDescriptor(name: String, val cable: ElectricalCableDescriptor, val batteryCapacity: Double, val nominalVoltage: Double,
                              val chargePower: Double, val consumption: Double, val lightLevel: Int, model: Obj3D)
    : SixNodeDescriptor(name, EmergencyLampElement::class.java, EmergencyLampRender::class.java) {

    val mainCeiling: Obj3D.Obj3DPart = model.getPart("coreCeil")
    val panelCeiling: Obj3D.Obj3DPart = model.getPart("panelCeil")
    val lightCeiling: Obj3D.Obj3DPart = model.getPart("lightCeil")
    val mainWall: Obj3D.Obj3DPart = model.getPart("coreWall")
    val mainWallR: Obj3D.Obj3DPart = model.getPart("coreWallR")
    val mainWallL: Obj3D.Obj3DPart = model.getPart("coreWallL")
    val lightWall: Obj3D.Obj3DPart = model.getPart("lightWall")

    init {
        voltageLevelColor = VoltageLevelColor.fromVoltage(nominalVoltage)
        setDefaultIcon("emergencylamp")
    }

    fun draw(onCeiling: Boolean = false, on: Boolean = false, mirrorSign: Boolean = false) {
        if (onCeiling) {
            mainCeiling.draw()

            if (on) {
                preserveMatrix {
                    UtilsClient.drawLight(panelCeiling)
                    GL11.glColor3f(0.3f, 0.3f, 0.3f)
                    UtilsClient.drawLight(lightCeiling)
                }
            } else {
                panelCeiling.draw()
            }
        } else {
            if (on) {
                preserveMatrix {
                    UtilsClient.drawLight(mainWall)
                    UtilsClient.drawLight(if (mirrorSign) mainWallL else mainWallR)
                    GL11.glColor3f(0.3f, 0.3f, 0.3f)
                    UtilsClient.drawLight(lightWall)
                }
            } else {
                preserveMatrix {
                    mainWall.draw()
                    (if (mirrorSign) mainWallL else mainWallR).draw()
                }
            }
        }
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer)
        = super.getFrontFromPlace(side, player)!!.inverse()

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>,
                                par4: Boolean) {
        with(list) {
            add(tr("As long as power is provided, the internal battery"))
            add(tr("is charged and the lamp is off. On a power failure,"))
            add(tr("the lamp turns on and runs on batteries."))
            add(Utils.plotVolt(tr("Nominal voltage:"), nominalVoltage))
            add(Utils.plotEnergy(tr("Battery capacity:"), batteryCapacity))
        }
    }
}

class EmergencyLampElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElement(sixNode, side, descriptor), IConfigurable {

    enum class Event(val value: Byte) {
        TOGGLE_POWERED_BY_CABLE(1),
        SET_CHANNEL(2)
    }

    val desc = descriptor as EmergencyLampDescriptor
    val load = NbtElectricalLoad("load")
    val chargingResistor = ResistorSwitch("chargingResistor", load, null)
    var on by published(false, {
        sixNode.lightValue = if (it) desc.lightLevel else 0
    })
    var charge = desc.batteryCapacity / 2
    var poweredByCable by published(false, {
        if (it) isConnectedToLampSupply = false
    }, triggerReconnect = true)
    var channel by published(PowerChannelTextboxHelper.DEFAULT_CHANNEL_STRING)
    var isConnectedToLampSupply by published(false)

    val process = EmergencyLampProcess(this)

    override fun initialize() {
        chargingResistor.resistance =
            desc.nominalVoltage * desc.nominalVoltage / desc.chargePower
        desc.cable.applyTo(load)

        electricalLoadList.add(load)
        electricalComponentList.add(chargingResistor)
        slowProcessList.add(process)
        slowProcessList.add(NodePeriodicPublishProcess(sixNode!!, 2.0, 0.5))
        slowProcessList.add(VoltageStateWatchDog(load).setNominalVoltage(desc.nominalVoltage)
            .setDestroys(WorldExplosion(this).cableExplosion()))
    }

    override fun getConnectionMask(lrdu: LRDU) = when {
        poweredByCable && side == Direction.YP -> NodeBase.maskElectricalPower
        poweredByCable && (lrdu == front.left() || lrdu == front.right()) -> NodeBase.maskElectricalPower
        else -> 0
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad = load
    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null
    override fun multiMeterString() = buildString {
        append(Utils.plotVolt("U:", load.voltage))
        append(Utils.plotAmpere("I:", load.current))
        append(Utils.plotPercent("Charge:", charge / (sixNodeElementDescriptor as EmergencyLampDescriptor).batteryCapacity))
    }
    override fun thermoMeterString(): String = ""

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = LinkedHashMap()

        info[tr("State")] = when {
            on -> tr("On")
            chargingResistor.state -> tr("Charging...")
            charge <= 0.0 -> tr("Batteries empty")
            else -> tr("Fully charged")
        }

        info[tr("Charge")] = Utils.plotPercent("", charge / (sixNodeElementDescriptor as EmergencyLampDescriptor).batteryCapacity)
        info[tr("Power Consumption")] = Utils.plotPower("", chargingResistor.voltage.pow(2) / chargingResistor.resistance)

        if (Utils.isWailaEasyModeEnabled()) {
            info[tr("Voltage")] = Utils.plotVolt("", chargingResistor.voltage)
            info[tr("Channel")] = channel
        }

        return info
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(charge.toFloat() / desc.batteryCapacity.toFloat())
        stream.writeBoolean(on)
        stream.writeBoolean(poweredByCable)
        stream.writeUTF(channel)
        stream.writeBoolean(isConnectedToLampSupply)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        when (stream.readByte()) {
            Event.TOGGLE_POWERED_BY_CABLE.value -> poweredByCable = !poweredByCable
            Event.SET_CHANNEL.value -> channel = stream.readUTF()
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        on = nbt.getBoolean("on")
        charge = nbt.getDouble("charge")
        poweredByCable = nbt.getBoolean("poweredByCable")
        channel = nbt.getString("channel")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setBoolean("on", on)
        nbt.setDouble("charge", charge)
        nbt.setBoolean("poweredByCable", poweredByCable)
        nbt.setString("channel", channel)
    }

    override fun hasGui() = true

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        var publishChanges = false

        if (compound.hasKey("poweredByCable")) {
            poweredByCable = compound.getBoolean("poweredByCable")
            publishChanges = true
        }

        if (compound.hasKey("lampSupplyChannel")) {
            channel = compound.getString("lampSupplyChannel")
            publishChanges = true
        }

        if (publishChanges) needPublish()
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setBoolean("poweredByCable", poweredByCable)
        compound.setString("lampSupplyChannel", channel)
    }
}

class EmergencyLampProcess(val element: EmergencyLampElement) : IProcess, IWirelessPower {
    override var previousConnectedSupply: AvailableSupply? = null

    override val powerChannel: String
        get() = element.channel
    override val coordinate: Coordinate
        get() = element.coordinate!!
    override val loadResistance: Double
        get() = element.chargingResistor.resistance

    override fun updateLoadVoltage(newVoltage: Double) {
        element.load.voltage = newVoltage
    }

    override fun process(time: Double) {
        if (!element.poweredByCable) {
            element.isConnectedToLampSupply = LampSupplyConnectionHelper.connectToLampSupply(this)
            element.needPublish()
        }

        if (abs(element.chargingResistor.voltage) > 0.5 * element.desc.nominalVoltage) {
            element.on = false
            if (element.charge < element.desc.batteryCapacity) {
                // Use setter to update resistance along with the state.
                element.chargingResistor.setState(true)
                element.charge = minOf(element.charge + element.chargingResistor.power * time, element.desc.batteryCapacity)
            } else {
                element.chargingResistor.setState(false)
            }
        } else {
            element.chargingResistor.setState(false)
            if (element.charge > 0) {
                element.on = true
                element.charge = maxOf(element.charge - element.desc.consumption * time, 0.0)
            } else {
                element.on = false
            }
        }
    }
}

class EmergencyLampRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElementRender(entity, side, descriptor) {

    val desc = descriptor as EmergencyLampDescriptor
    var charge = 0f
    var on = false
    var poweredByCable = false
    var channel = PowerChannelTextboxHelper.DEFAULT_CHANNEL_STRING
    var isConnectedToLampSupply = false

    override fun draw() {
        super.draw()
        front!!.glRotateOnX()
        desc.draw(side == Direction.YP, on, front == LRDU.Up)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        charge = stream.readFloat()
        on = stream.readBoolean()
        poweredByCable = stream.readBoolean()
        channel = stream.readUTF()
        isConnectedToLampSupply = stream.readBoolean()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer) = EmergencyLampGui(this)

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? = if (poweredByCable) when {
        side == Direction.YP -> desc.cable.render
        lrdu == front!!.left() || lrdu == front!!.right() -> desc.cable.render
        else -> null
    } else null
}

class EmergencyLampGui(private var render: EmergencyLampRender)
    : GuiScreenEln() {
    private lateinit var buttonSupplyType: GuiButton
    private lateinit var channel: GuiTextFieldEln
    private lateinit var charge: GuiVerticalProgressBar

    override fun initGui() {
        super.initGui()
        buttonSupplyType = newGuiButton(18, 12, 140, "")
        channel = newGuiTextField(19, 38, 138)
        PowerChannelTextboxHelper.initPowerChannelTextbox(channel, render.channel)
        charge = newGuiVerticalProgressBar(166, 12, 16, 39)
        charge.setColor(0.2f, 0.5f, 0.8f)
    }

    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)
        if (`object` === buttonSupplyType) {
            render.clientSend(EmergencyLampElement.Event.TOGGLE_POWERED_BY_CABLE.value.toInt())
        } else if (`object` === channel) {
            render.clientSetString(EmergencyLampElement.Event.SET_CHANNEL.value, channel.text)
        }
    }

    override fun newHelper(): GuiHelperContainer = GuiHelperContainer(this, 196, 64, 8, 84)

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        if (!render.poweredByCable) {
            buttonSupplyType.displayString = tr("Powered by lamp supply")
            channel.visible = true
            PowerChannelTextboxHelper.updatePowerChannelTextboxTooltip(channel, render.channel, render.isConnectedToLampSupply)
        } else {
            channel.visible = false
            buttonSupplyType.displayString = tr("Powered by cable")
        }
        charge.setValue(render.charge)
        charge.setComment(0, Utils.plotPercent("Charge: ", render.charge.toDouble()))
    }
}
