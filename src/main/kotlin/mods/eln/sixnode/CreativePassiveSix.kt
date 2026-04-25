package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.i18n.I18N.tr
import mods.eln.item.IConfigurable
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.RealisticEnum
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Capacitor
import mods.eln.sim.mna.component.Inductor
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.BipoleVoltageWatchdog
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ResistorHeatThermalLoad
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.ParseException
import java.util.Collections
import java.util.HashMap

abstract class CreativePassiveDescriptor(
    name: String,
    elementClass: Class<out SixNodeElement>,
    renderClass: Class<out SixNodeElementRender>,
    protected val obj: Obj3D
) : SixNodeDescriptor(name, elementClass, renderClass) {
    protected val base = obj.getPart("Base")
    protected val capacitorCables = obj.getPart("CapacitorCables")
    protected val capacitorCore = obj.getPart("CapacitorCore")
    protected val inductorBaseExtension = obj.getPart("InductorBaseExtention")
    protected val inductorCables = obj.getPart("InductorCables")
    protected val inductorCore = obj.getPart("InductorCore")
    protected val resistorBaseExtension = obj.getPart("ResistorBaseExtention")
    protected val resistorCore = obj.getPart("ResistorCore")
    protected val resistorCables = obj.getPart("CapacitorCables")

    override fun canBePlacedOnSide(player: EntityPlayer?, side: Direction) = true

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player)!!.left()
    }

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }
}

class CreativePowerResistorDescriptor(name: String, obj: Obj3D) :
    CreativePassiveDescriptor(name, CreativePowerResistorElement::class.java, CreativePowerResistorRender::class.java, obj) {
    fun draw() {
        base?.draw()
        resistorBaseExtension?.draw()
        resistorCore?.draw()
        resistorCables?.draw()
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type != IItemRenderer.ItemRenderType.INVENTORY) {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f)
            GL11.glScalef(1.25f, 1.25f, 1.25f)
            GL11.glRotatef(-90f, 0f, 1f, 0f)
            draw()
        } else {
            super.renderItem(type, item, *data)
        }
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll(list, *tr("Creative resistor with direct value entry.").split("\n").toTypedArray())
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        super.addRealismContext(list)
        return RealisticEnum.REALISTIC
    }
}

class CreativePowerResistorElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, descriptor), IConfigurable {
    private val resistorDescriptor = descriptor as CreativePowerResistorDescriptor
    private val aLoad = NbtElectricalLoad("aLoad")
    private val bLoad = NbtElectricalLoad("bLoad")
    private val resistor = Resistor(aLoad, bLoad)
    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val heater = ResistorHeatThermalLoad(resistor, thermalLoad)
    private val thermalWatchdog = ambientAwareThermalWatchdog(ThermalLoadWatchDog(thermalLoad).asResistorHeatWatchdog())
    var resistance = 100.0

    override fun initialize() {
        aLoad.serialResistance = MnaConst.noImpedance
        bLoad.serialResistance = MnaConst.noImpedance
        thermalLoad.setAsSlow()
        val thermalC = 1000.0 * 120.0 / Eln.cableWarmLimit
        val thermalRp = Eln.cableWarmLimit / 1000.0
        val thermalRs = Eln.cableThermalConductionTao / thermalC / 2.0
        thermalLoad.set(thermalRs, thermalRp, thermalC)
        thermalWatchdog
            .setTemperatureLimits(Eln.cableWarmLimit, -100.0)
            .setDestroys(WorldExplosion(this).cableExplosion())
        applyResistance()
    }

    private fun applyResistance() {
        resistor.resistance = resistance.coerceAtLeast(1.0e-9)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        resistance = nbt.getDouble("resistance")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble("resistance", resistance)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (lrdu == front.right()) return aLoad
        return if (lrdu == front.left()) bLoad else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = thermalLoad

    override fun getConnectionMask(lrdu: LRDU): Int {
        return if (lrdu == front.right() || lrdu == front.left()) NodeBase.maskElectricalPower else 0
    }

    override fun multiMeterString(): String =
        Utils.plotOhm(Utils.plotUIP(-kotlin.math.abs(aLoad.voltage - bLoad.voltage), kotlin.math.abs(resistor.current)), resistor.resistance)

    override fun getWaila(): Map<String, String> {
        val info = HashMap<String, String>()
        info[tr("Resistance")] = Utils.plotValue(resistor.resistance, "\u2126")
        return info
    }

    override fun thermoMeterString(): String = plotAmbientCelsius("T", thermalLoad.temperatureCelsius)

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(resistance)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        resistance = stream.readDouble()
        applyResistance()
        needPublish()
    }

    override fun hasGui() = true

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return onBlockActivatedRotate(entityPlayer)
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("resistance")) {
            resistance = compound.getDouble("resistance")
            applyResistance()
            needPublish()
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setDouble("resistance", resistance)
    }

    init {
        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        electricalComponentList.add(resistor)
        thermalLoadList.add(thermalLoad)
        thermalSlowProcessList.add(heater)
        slowProcessList.add(thermalWatchdog)
    }
}

class CreativePowerResistorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, descriptor) {
    private val creativeDescriptor = descriptor as CreativePowerResistorDescriptor
    var resistance = 100.0

    override fun draw() {
        GL11.glRotatef(90f, 1f, 0f, 0f)
        front!!.glRotateOnX()
        creativeDescriptor.draw()
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        resistance = stream.readDouble()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen = CreativePassiveValueGui(
        this,
        tr("Creative Power Resistor"),
        tr("Resistance"),
        tr("Resistance in ohms"),
        resistance
    )
}

class CreativePowerCapacitorDescriptor(name: String, obj: Obj3D) :
    CreativePassiveDescriptor(name, CreativePowerCapacitorElement::class.java, CreativePowerCapacitorRender::class.java, obj) {
    fun draw() {
        base?.draw()
        capacitorCables?.draw()
        capacitorCore?.draw()
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type != IItemRenderer.ItemRenderType.INVENTORY) {
            GL11.glRotatef(90f, 1f, 0f, 0f)
            draw()
        } else {
            super.renderItem(type, item, *data)
        }
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll(list, *tr("Creative capacitor with direct value entry.").split("\n").toTypedArray())
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        super.addRealismContext(list)
        list?.add(tr("Timestep-dependent behavior can make reactive simulation inaccurate."))
        list?.add(tr("MNA capacitor solving can produce unrealistic results."))
        return RealisticEnum.UNREALISTIC
    }
}

class CreativePowerCapacitorElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, descriptor), IConfigurable {
    private val positiveLoad = NbtElectricalLoad("positiveLoad")
    private val negativeLoad = NbtElectricalLoad("negativeLoad")
    private val capacitor = Capacitor(positiveLoad, negativeLoad)
    private val voltageWatchdog = BipoleVoltageWatchdog(capacitor).setNominalVoltage(1000.0).setDestroys(WorldExplosion(this).cableExplosion())
    var capacitance = 1.0e-5

    private fun applyCapacitance() {
        capacitor.coulombs = capacitance.coerceAtLeast(1.0e-12)
    }

    override fun initialize() {
        Eln.applySmallRs(positiveLoad)
        Eln.applySmallRs(negativeLoad)
        applyCapacitance()
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        capacitance = nbt.getDouble("capacitance")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble("capacitance", capacitance)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (lrdu == front.right()) return positiveLoad
        return if (lrdu == front.left()) negativeLoad else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU): Int {
        return if (lrdu == front.right() || lrdu == front.left()) NodeBase.maskElectricalPower else 0
    }

    override fun multiMeterString(): String = Utils.plotVolt("U", kotlin.math.abs(capacitor.voltage)) + Utils.plotAmpere("I", capacitor.current)

    override fun getWaila(): Map<String, String> {
        val info = HashMap<String, String>()
        info[tr("Capacity")] = Utils.plotValue(capacitor.coulombs, "F")
        return info
    }

    override fun thermoMeterString(): String = ""

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(capacitance)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        capacitance = stream.readDouble()
        applyCapacitance()
        needPublish()
    }

    override fun hasGui() = true

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return onBlockActivatedRotate(entityPlayer)
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("capacitance")) {
            capacitance = compound.getDouble("capacitance")
            applyCapacitance()
            needPublish()
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setDouble("capacitance", capacitance)
    }

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(negativeLoad)
        electricalComponentList.add(capacitor)
        slowProcessList.add(voltageWatchdog)
        positiveLoad.setAsMustBeFarFromInterSystem()
    }
}

class CreativePowerCapacitorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, descriptor) {
    private val creativeDescriptor = descriptor as CreativePowerCapacitorDescriptor
    var capacitance = 1.0e-5

    override fun draw() {
        GL11.glRotatef(90f, 1f, 0f, 0f)
        front!!.glRotateOnX()
        creativeDescriptor.draw()
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        capacitance = stream.readDouble()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen = CreativePassiveValueGui(
        this,
        tr("Creative Power Capacitor"),
        tr("Capacitance"),
        tr("Capacitance in farads"),
        capacitance
    )
}

class CreativePowerInductorDescriptor(name: String, obj: Obj3D) :
    CreativePassiveDescriptor(name, CreativePowerInductorElement::class.java, CreativePowerInductorRender::class.java, obj) {
    fun draw() {
        base?.draw()
        inductorBaseExtension?.draw()
        inductorCables?.draw()
        inductorCore?.draw()
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type != IItemRenderer.ItemRenderType.INVENTORY) {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f)
            GL11.glScalef(1.25f, 1.25f, 1.25f)
            GL11.glRotatef(-90f, 0f, 1f, 0f)
            draw()
        } else {
            super.renderItem(type, item, *data)
        }
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll(list, *tr("Creative inductor with direct value entry.").split("\n").toTypedArray())
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        super.addRealismContext(list)
        list?.add(tr("Timestep-dependent behavior can make reactive simulation inaccurate."))
        list?.add(tr("MNA inductor solving can produce unrealistic results."))
        return RealisticEnum.UNREALISTIC
    }
}

class CreativePowerInductorElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, descriptor), IConfigurable {
    private val positiveLoad = NbtElectricalLoad("positiveLoad")
    private val negativeLoad = NbtElectricalLoad("negativeLoad")
    private val inductor = Inductor("inductor", positiveLoad, negativeLoad)
    var inductance = 1.0

    private fun applyInductance() {
        inductor.inductance = inductance.coerceAtLeast(1.0e-12)
        val rs = Eln.instance.lowVoltageCableDescriptor.electricalRs
        positiveLoad.serialResistance = rs
        negativeLoad.serialResistance = rs
    }

    override fun initialize() {
        applyInductance()
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        inductance = nbt.getDouble("inductance")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble("inductance", inductance)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (lrdu == front.right()) return positiveLoad
        return if (lrdu == front.left()) negativeLoad else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU): Int {
        return if (lrdu == front.right() || lrdu == front.left()) NodeBase.maskElectricalPower else 0
    }

    override fun multiMeterString(): String = Utils.plotVolt("U", kotlin.math.abs(inductor.voltage)) + Utils.plotAmpere("I", inductor.current)

    override fun getWaila(): Map<String, String> {
        val info = HashMap<String, String>()
        info[tr("Inductance")] = Utils.plotValue(inductor.inductance, "H")
        return info
    }

    override fun thermoMeterString(): String = ""

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(inductance)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        inductance = stream.readDouble()
        applyInductance()
        needPublish()
    }

    override fun hasGui() = true

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return onBlockActivatedRotate(entityPlayer)
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("inductance")) {
            inductance = compound.getDouble("inductance")
            applyInductance()
            needPublish()
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setDouble("inductance", inductance)
    }

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(negativeLoad)
        electricalComponentList.add(inductor)
        positiveLoad.setAsMustBeFarFromInterSystem()
    }
}

class CreativePowerInductorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, descriptor) {
    private val creativeDescriptor = descriptor as CreativePowerInductorDescriptor
    var inductance = 1.0

    override fun draw() {
        front!!.left().glRotateOnX()
        creativeDescriptor.draw()
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        inductance = stream.readDouble()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen = CreativePassiveValueGui(
        this,
        tr("Creative Power Inductor"),
        tr("Inductance"),
        tr("Inductance in henries"),
        inductance
    )
}

private class CreativePassiveValueGui(
    private val render: SixNodeElementRender,
    private val title: String,
    private val label: String,
    private val comment: String,
    private val initialValue: Double
) : GuiScreenEln() {
    private var field: GuiTextFieldEln? = null

    override fun newHelper(): GuiHelper = GuiHelper(this, 96, 32)

    override fun initGui() {
        super.initGui()
        field = newGuiTextField(6, 14, 84)
        field!!.setText(initialValue.toFloat())
        field!!.setObserver(this)
        field!!.setComment(arrayOf(comment))
    }

    override fun textFieldNewValue(textField: GuiTextFieldEln, value: String) {
        val parsed = try {
            NumberFormat.getInstance().parse(textField.text).toDouble()
        } catch (_: ParseException) {
            return
        }
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            render.preparePacketForServer(stream)
            stream.writeDouble(parsed)
            render.sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        drawString(6, 4, title)
        drawString(6, 30, label)
    }
}
