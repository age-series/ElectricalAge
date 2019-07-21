package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.gui.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ProcessType
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.genericcable.GenericCableDescriptor
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

class ResistorSinkDescriptor(name: String, val cable: GenericCableDescriptor, val obj: Obj3D): TransparentNodeDescriptor(name, ResistorSinkElement::class.java, ResistorSinkRender::class.java) {
    init {
        voltageLevelColor = VoltageLevelColor.None
    }

    override fun handleRenderType(item: ItemStack?, type: IItemRenderer.ItemRenderType?) = true

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType?, item: ItemStack?, helper: IItemRenderer.ItemRendererHelper?): Boolean =
        type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) =
        if (type != IItemRenderer.ItemRenderType.INVENTORY) draw() else super.renderItem(type, item, *data)

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("A device to create large power sinks."))
    }

    fun draw() {
        obj.draw("main")
    }

    companion object {
        const val CLIENT_MODE: Byte = 1
        const val CLIENT_RESISTANCE: Byte = 2
        const val CLIENT_POWER: Byte = 3
    }
}

class ResistorSinkElement(node: TransparentNode, desc_: TransparentNodeDescriptor): TransparentNodeElement(node, desc_) {
    val desc: ResistorSinkDescriptor = desc_ as ResistorSinkDescriptor

    private var inLoad: NbtElectricalLoad = NbtElectricalLoad("inLoad")
    private var groundLoad: NbtElectricalLoad = NbtElectricalLoad("groundLoad")
    private val resistor: Resistor
    private val ground: VoltageSource

    private val powerCalc = ResistorSinkPowerCalculator()
    private val powerCalcBand = 0.01

    var mode: String
    var powerSetpoint: Double

    init {
        resistor = Resistor(inLoad, groundLoad)
        ground = VoltageSource("ground", groundLoad, null)
        ground.u = 0.0
        resistor.r = 1.0
        electricalLoadList.add(inLoad)
        electricalLoadList.add(groundLoad)
        electricalComponentList.add(resistor)
        electricalComponentList.add(ground)
        mode = "R"
        powerSetpoint = 0.0
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front.up() -> null
            front.down() -> null
            else -> inLoad
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        if (lrdu != LRDU.Down) return 0
        return when (side) {
            front.up() -> 0
            front.down() -> 0
            else -> NodeBase.MASK_ELECTRICAL_POWER
        }
    }

    override fun multiMeterString(side: Direction): String {
        return Utils.plotOhm(resistor.r) + Utils.plotAmpere(resistor.getCurrent()) + Utils.plotPower(resistor.getPower())
    }

    override fun thermoMeterString(side: Direction?): String {
        return ""
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun getWaila(): MutableMap<String, String> {
        val info = mutableMapOf<String, String>()
        info["Resistance"] = Utils.plotOhm(resistor.r)
        info["Current"] = Utils.plotAmpere(resistor.getCurrent())
        info["Power"] = Utils.plotPower(resistor.getPower())
        info["Mode"] = if (mode == "R") "Resistance" else "Power"
        if (mode != "R") {
            info["Power Setpoint"] = Utils.plotPower(powerSetpoint)
        }
        return info
    }

    override fun initialize() {
        inLoad.rs = MnaConst.noImpedance
        groundLoad.rs = MnaConst.noImpedance
        connect()
    }

    override fun hasGui() = true

    private fun setResistance(resistance: Double) {
        when {
            resistance < 0.1 -> resistor.r = 0.1
            resistance > MnaConst.highImpedance -> resistor.r = MnaConst.highImpedance
            else -> resistor.r = resistance
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        mode = nbt.getString("mode")
        resistor.r = nbt.getDouble("resistance")
        powerSetpoint = nbt.getDouble("power")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setString("mode", mode)
        nbt.setDouble("resistance", resistor.r)
        nbt.setDouble("power", powerSetpoint)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            DP.println(DPType.TRANSPARENT_NODE, mode)
            stream.writeUTF(mode)
            stream.writeDouble(resistor.r)
            stream.writeDouble(powerSetpoint)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        when (stream.readByte()) {
            ResistorSinkDescriptor.CLIENT_MODE ->
                mode = stream.readUTF()
            ResistorSinkDescriptor.CLIENT_RESISTANCE ->
                if (mode == "R")
                    setResistance(stream.readDouble())
            ResistorSinkDescriptor.CLIENT_POWER ->
                powerSetpoint = stream.readDouble()
        }
        return 0
    }

    override fun connectJob() {
        super.connectJob()
        Eln.simulator.addProcess(ProcessType.SlowProcess, powerCalc)
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.removeProcess(ProcessType.SlowProcess, powerCalc)
    }

    inner class ResistorSinkPowerCalculator: IProcess {
        override fun process(time: Double) {
            if(ground.subSystem != null) {
                val i = Math.abs(ground.getSubSystem()!!.solve(ground.currentState))
                val predictedPower = i * i * resistor.r
                val new = resistor.r * (predictedPower / powerSetpoint)
                val diff = new / resistor.r
                when {
                    diff > 1 + powerCalcBand ->
                        if (mode == "P")
                            setResistance(new)
                    diff < 1 - powerCalcBand ->
                        if (mode == "P")
                            setResistance(new)
                }
            }
        }
    }
}

class ResistorSinkRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor): TransparentNodeElementRender(entity, desc_) {
    val desc: ResistorSinkDescriptor = desc_ as ResistorSinkDescriptor

    var mode: String
    var r: Double
    var p: Double

    init {
        mode = "R"
        r = 1.0
        p = 1.0
    }

    override fun draw() {
        GL11.glColor3f(5f, 0.5f, 0.2f)
        desc.draw()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return ResistorSinkGui(this)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            mode = stream.readUTF()
            r = stream.readDouble()
            p = stream.readDouble()
            DP.println(DPType.TRANSPARENT_NODE, "Recieved packet! mode: $mode r: $r p: $p")
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class ResistorSinkGui(internal val render: ResistorSinkRender): GuiScreenEln() {
    private var stateBt: GuiButtonEln? = null
    private var resistanceField: GuiTextFieldEln? = null
    private var powerField: GuiTextFieldEln? = null

    override fun initGui() {
        super.initGui()

        stateBt = newGuiButton(16, 6, 70, "")
        if (render.mode == "R") {
            stateBt!!.displayString = "RESISTANCE"
        } else {
            stateBt!!.displayString = "POWER"
        }
        resistanceField = newGuiTextField(6, 30, 90)
        powerField = newGuiTextField(6, 44, 90)

        resistanceField!!.setComment(0, "Set the resistance")
        powerField!!.setComment(0, "Set the power")

        resistanceField!!.text = "%.3f".format(render.r)
        powerField!!.text = "%.3f".format(render.p)
    }

    override fun guiObjectEvent(`object`: IGuiObject?) {
        super.guiObjectEvent(`object`)

        if (`object` === stateBt) {
            if (render.mode == "R") {
                render.mode = "P"
                stateBt!!.displayString = "POWER"
            } else {
                render.mode = "R"
                stateBt!!.displayString = "RESISTANCE"
            }

            try {
                val bos = ByteArrayOutputStream()
                val stream = DataOutputStream(bos)

                render.preparePacketForServer(stream)

                stream.writeByte(ResistorSinkDescriptor.CLIENT_MODE.toInt())
                stream.writeUTF(render.mode)

                render.sendPacketToServer(bos)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (`object` === resistanceField) {
            render.r = resistanceField!!.text.toDouble()

            try {
                val bos = ByteArrayOutputStream()
                val stream = DataOutputStream(bos)

                render.preparePacketForServer(stream)

                stream.writeByte(ResistorSinkDescriptor.CLIENT_RESISTANCE.toInt())
                stream.writeDouble(render.r)

                render.sendPacketToServer(bos)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (`object` === powerField) {
            render.p = powerField!!.text.toDouble()

            try {
                val bos = ByteArrayOutputStream()
                val stream = DataOutputStream(bos)

                render.preparePacketForServer(stream)

                stream.writeByte(ResistorSinkDescriptor.CLIENT_POWER.toInt())
                stream.writeDouble(render.p)

                render.sendPacketToServer(bos)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 102, 64)
    }

    override fun keyTyped(key: Char, code: Int) {
        if (key == 'e') {
            mc.thePlayer.closeScreen()
        }
        super.keyTyped(key, code)
    }
}
