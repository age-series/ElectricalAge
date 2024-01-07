package mods.eln.sixnode.currentrelay

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N.tr
import mods.eln.item.IConfigurable
import mods.eln.misc.*
import mods.eln.misc.LRDU.Companion.fromInt
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.Utils.plotAmpere
import mods.eln.misc.Utils.plotVolt
import mods.eln.misc.Utils.renderSubSystemWaila
import mods.eln.misc.UtilsClient.disableCulling
import mods.eln.misc.UtilsClient.enableCulling
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.NodeElectricalGateInputHysteresisProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalrelay.ElectricalRelayElement
import mods.eln.sound.SoundCommand
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class CurrentRelayDescriptor(
    name: String?,
    val obj: Obj3D,
    val cable: CurrentCableDescriptor
): SixNodeDescriptor(name, CurrentRelayElement::class.java, CurrentRelayRender::class.java) {
    var speed: Float = 0f

    private val relay1: Obj3DPart = obj.getPart("relay1")
    private val relay0: Obj3DPart = obj.getPart("relay0")
    private val main: Obj3DPart = obj.getPart("main")
    private val backplate: Obj3DPart = obj.getPart("backplate")

    private var r0rOff = 0f
    private var r0rOn = 0f
    private var r1rOff = 0f
    private var r1rOn = 0f

    var thermalRp = 1.0
    var thermalC = 1.0
    var thermalRs = 1.0

    private val electricalRs = 0.01

    init {
        r0rOff = relay0.getFloat("rOff")
        r0rOn = relay0.getFloat("rOn")
        speed = relay0.getFloat("speed")
        r1rOff = relay1.getFloat("rOff")
        r1rOn = relay1.getFloat("rOn")
        this.voltageLevelColor = VoltageLevelColor.Neutral
    }

    fun setPhysicalConstantLikeNormalCable(
        electricalMaximalCurrent: Double
    ) {
        val thermalMaximalPowerDissipated = electricalRs * electricalMaximalCurrent * electricalMaximalCurrent * 2
        thermalC = thermalMaximalPowerDissipated * Eln.cableHeatingTime / Eln.cableWarmLimit
        thermalRp = Eln.cableWarmLimit / thermalMaximalPowerDissipated
        thermalRs = 0.25 / thermalC
    }

    fun applyTo(load: ElectricalLoad) {
        cable.applyTo(load)
    }

    fun applyTo(load: Resistor) {
        cable.applyTo(load)
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.addAll(
            tr("A relay is an electrical\ncontact that conducts\ncurrent when a signal\nvoltage is applied.")
                .split("\n".toRegex()).dropLastWhile { it.isEmpty() }
        )
        list.addAll(
            tr("The relay's input behaves\nlike a Schmitt Trigger.").split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
        )
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        return true
    }

    override fun shouldUseRenderHelperEln(type: ItemRenderType?, item: ItemStack?, helper: ItemRendererHelper?): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data)
        } else {
            draw(0f)
        }
    }

    fun draw(factor: Float) {
        disableCulling()
        GL11.glScalef(0.5f, 0.5f, 0.5f)
        main.draw()
        relay0.draw(factor * (r0rOn - r0rOff) + r0rOff, 0f, 0f, 1f)
        relay1.draw(factor * (r1rOn - r1rOff) + r1rOff, 0f, 0f, 1f)
        GL11.glPushMatrix()
        voltageLevelColor.setGLColor()
        backplate.draw()
        GL11.glPopMatrix()
        enableCulling()
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player)!!.left()
    }
}

class CurrentRelayElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor): SixNodeElement(sixNode, side, descriptor), IConfigurable {

    private val currentRelayDescriptor = descriptor as CurrentRelayDescriptor

    var aLoad = NbtElectricalLoad("aLoad")
    var bLoad = NbtElectricalLoad("bLoad")
    var thermalLoad = NbtThermalLoad("thermalLoad")
    private var switchResistor = Resistor(aLoad, bLoad)
    var gate = NbtElectricalGateInput("gate")
    private var gateProcess = CurrentRelayGateProcess(this, "GP", gate)

    private var voltageWatchDogA = VoltageStateWatchDog(aLoad)
    private var voltageWatchDogB = VoltageStateWatchDog(bLoad)
    private var thermalWatchdog = ThermalLoadWatchDog(thermalLoad)

    var switchState = false
        set(value) {
            if (value == switchState) return
            field = value
            refreshSwitchResistor()
            play(SoundCommand("random.click").mulVolume(0.1f, 2.0f).smallRange())
            needPublish()
        }

    var defaultOutput = false

    val cableDescriptor = currentRelayDescriptor.cable

    init {
        configThermalLoad(thermalLoad)

        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        electricalComponentList.add(switchResistor)
        electricalProcessList.add(gateProcess)
        electricalLoadList.add(gate)
        thermalLoadList.add(thermalLoad)

        electricalComponentList.add(Resistor(bLoad, null).pullDown())
        electricalComponentList.add(Resistor(aLoad, null).pullDown())

        slowProcessList.add(voltageWatchDogA)
        slowProcessList.add(voltageWatchDogB)

        voltageWatchDogA.setNominalVoltage(cableDescriptor.electricalNominalVoltage)
        voltageWatchDogB.setNominalVoltage(cableDescriptor.electricalNominalVoltage)

        val heater = ElectricalLoadHeatThermalLoad(aLoad, thermalLoad)
        thermalSlowProcessList.add(heater)

        thermalLoad.setAsSlow()

        slowProcessList.add(thermalWatchdog)
        thermalWatchdog
            .setTemperatureLimits(Eln.cableWarmLimit, -10.0)
            .setDestroys(WorldExplosion(this).cableExplosion())
    }

    @Suppress("UNUSED_PARAMETER")
    fun canBePlacedOnSide(side: Direction?, type: Int): Boolean {
        return true
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val value = nbt.getByte("front")
        front = fromInt(value.toInt() shr 0 and 0x3)
        switchState = nbt.getBoolean("switchState")
        defaultOutput = nbt.getBoolean("defaultOutput")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setByte("front", (front.toInt() shl 0).toByte())
        nbt.setBoolean("switchState", switchState)
        nbt.setBoolean("defaultOutput", defaultOutput)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (front.left() === lrdu) return aLoad
        if (front.right() === lrdu) return bLoad
        return if (front === lrdu) gate else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        if (front.left() === lrdu) return thermalLoad
        if (front.right() === lrdu) return thermalLoad
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        if (front.left() === lrdu) return cableDescriptor.nodeMask
        if (front.right() === lrdu) return cableDescriptor.nodeMask
        return if (front === lrdu) NodeBase.maskElectricalInputGate else 0
    }

    override fun multiMeterString(): String {
        return plotVolt("Ua:", aLoad.voltage) + plotVolt("Ub:", bLoad.voltage) + plotAmpere("I:", aLoad.current)
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Position")] = if (switchState) tr("Closed") else tr("Open")
        info[tr("Current")] = plotAmpere("", aLoad.current)
        info[tr("Temperature")] = Utils.plotCelsius("", thermalLoad.temperatureCelsius)
        if (Eln.wailaEasyMode) {
            info[tr("Default position")] = if (defaultOutput) tr("Closed") else tr("Open")
            info[tr("Voltages")] =
                plotVolt("", aLoad.voltage) + plotVolt(" ", bLoad.voltage)
        }
        info[tr("Subsystem Matrix Size")] = renderSubSystemWaila(switchResistor.subSystem)
        return info
    }

    override fun thermoMeterString(): String {
        return Utils.plotCelsius("T", thermalLoad.temperatureCelsius)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeBoolean(switchState)
            stream.writeBoolean(defaultOutput)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun refreshSwitchResistor() {
        if (!switchState) {
            switchResistor.ultraImpedance()
        } else {
            currentRelayDescriptor.applyTo(switchResistor)
        }
    }

    override fun initialize() {
        computeElectricalLoad()
        refreshSwitchResistor()
    }

    override fun inventoryChanged() {
        computeElectricalLoad()
    }

    fun computeElectricalLoad() {
        currentRelayDescriptor.applyTo(aLoad)
        currentRelayDescriptor.applyTo(bLoad)
        refreshSwitchResistor()
    }

    private fun configThermalLoad(thermalLoad: ThermalLoad) {
        thermalLoad.Rs = currentRelayDescriptor.thermalRs
        thermalLoad.heatCapacity = currentRelayDescriptor.thermalC
        thermalLoad.Rp = currentRelayDescriptor.thermalRp
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            when (stream.readByte()) {
                ElectricalRelayElement.toogleOutputDefaultId -> {
                    defaultOutput = !defaultOutput
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

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer?) {
        if (compound.hasKey("nc")) {
            defaultOutput = compound.getBoolean("nc")
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer?) {
        compound.setBoolean("nc", defaultOutput)
    }
}

class CurrentRelayGateProcess(val element: CurrentRelayElement, name: String?, gateProcess: NbtElectricalGateInput): NodeElectricalGateInputHysteresisProcess(name, gateProcess) {
    override fun setOutput(value: Boolean) {
        element.switchState = value xor element.defaultOutput
    }

}

class CurrentRelayGui(val render: CurrentRelayRender): GuiScreenEln() {
    private lateinit var toggleDefaultOutput: GuiButton

    override fun initGui() {
        super.initGui()
        toggleDefaultOutput = newGuiButton(6, 32 / 2 - 10, 115, tr("Toggle switch"))
    }

    override fun guiObjectEvent(`object`: IGuiObject?) {
        super.guiObjectEvent(`object`)
        if (`object` === toggleDefaultOutput) {
            render.clientToggleDefaultOutput()
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        if (render.defaultOutput) toggleDefaultOutput.displayString =
            tr("Normally closed") else toggleDefaultOutput.displayString = tr("Normally open")
    }

    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 128, 32)
    }
}

class CurrentRelayRender(override var tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor): SixNodeElementRender(tileEntity, side, descriptor) {

    private val currentRelayDescriptor = descriptor as CurrentRelayDescriptor

    val interpolator = RcInterpolator(currentRelayDescriptor.speed)

    var boot = true
    var switchState = true
    var defaultOutput = true

    override fun draw() {
        super.draw()
        drawSignalPin(front, floatArrayOf(2.5f, 2.5f, 2.5f, 2.5f))
        front!!.glRotateOnX()
        currentRelayDescriptor.draw(interpolator.get())
    }

    override fun refresh(deltaT: Float) {
        interpolator.step(deltaT)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            switchState = stream.readBoolean()
            defaultOutput = stream.readBoolean()
            interpolator.target = if (switchState) 1f else 0f
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (boot) {
            interpolator.setValueFromTarget()
        }
        boot = false
    }

    fun clientToggleDefaultOutput() {
        clientSend(ElectricalRelayElement.toogleOutputDefaultId.toInt())
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return CurrentRelayGui(this)
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        if (lrdu === front) return Eln.instance.signalCableDescriptor.render
        return if (lrdu === front!!.left() || lrdu === front!!.right()) currentRelayDescriptor.cable.render else null
    }
}
