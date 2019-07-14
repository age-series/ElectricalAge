package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.debug.DebugType
import mods.eln.gui.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
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
import java.lang.Exception

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

        const val SERVER_MODE: Byte = 1
        const val SERVER_RESISTANCE: Byte = 2
        const val SERVER_POWER: Byte = 3
    }
}

class ResistorSinkElement(node: TransparentNode, desc_: TransparentNodeDescriptor): TransparentNodeElement(node, desc_) {
    val desc: ResistorSinkDescriptor

    var inLoad: NbtElectricalLoad
    var groundLoad: NbtElectricalLoad
    val resistor: Resistor
    val ground: VoltageSource

    var mode: String
    var power: Double

    init {
        desc = desc_ as ResistorSinkDescriptor
        inLoad = NbtElectricalLoad("inLoad")
        groundLoad = NbtElectricalLoad("groundLoad")
        resistor = Resistor(inLoad, groundLoad)
        ground = VoltageSource("ground", groundLoad, null)
        resistor.r = 1.0
        electricalLoadList.add(inLoad)
        electricalLoadList.add(groundLoad)
        electricalComponentList.add(resistor)
        electricalComponentList.add(ground)
        mode = "R"
        power = 0.0
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
            info["Power Setpoint"] = power.toString()
        }
        return info
    }

    override fun initialize() {
        inLoad.rs = MnaConst.noImpedance
        groundLoad.rs = MnaConst.noImpedance
        connect()
    }

    override fun hasGui() = true

    fun setResistance(resistance: Double) {
        if(resistance < 0.1) {
            resistor.r = 0.1
        } else {
            resistor.r = resistance
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        mode = nbt.getString("mode")
        resistor.r = nbt.getDouble("resistance")
        power = nbt.getDouble("power")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setString("mode", mode)
        nbt.setDouble("resistance", resistor.r)
        nbt.setDouble("power", power)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            Eln.dp.println(DebugType.TRANSPARENT_NODE, mode)
            stream.writeUTF(mode)
            stream.writeDouble(resistor.r)
            stream.writeDouble(power)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        try {
            when (stream.readByte()) {
                ResistorSinkDescriptor.CLIENT_MODE -> {
                    val str = stream.readUTF()
                    mode = str
                    Eln.dp.println(DebugType.TRANSPARENT_NODE, str)
                }
                ResistorSinkDescriptor.CLIENT_RESISTANCE -> {
                    setResistance(stream.readUTF().toDouble())
                }
                ResistorSinkDescriptor.CLIENT_POWER -> {
                    power = stream.readUTF().toDouble()
                }
            }
        }catch (e: Exception) {}
        return unserializeNulldId
    }

    fun sendItBack() {
        val bos = ByteArrayOutputStream(64)
        val bos2 = ByteArrayOutputStream(64)
        val bos3 = ByteArrayOutputStream(64)
        val mod = DataOutputStream(bos)
        val res = DataOutputStream(bos2)
        val pwr = DataOutputStream(bos3)

        preparePacketForClient(mod)
        preparePacketForClient(res)
        preparePacketForClient(pwr)

        try {
            mod.writeByte(ResistorSinkDescriptor.SERVER_MODE.toInt())
            mod.writeUTF(mode)
            res.writeByte(ResistorSinkDescriptor.SERVER_RESISTANCE.toInt())
            res.writeDouble(resistor.r)
            pwr.writeByte(ResistorSinkDescriptor.SERVER_POWER.toInt())
            pwr.writeDouble(power)
            sendPacketToAllClient(bos, 10.0)
            sendPacketToAllClient(bos2, 10.0)
            sendPacketToAllClient(bos3, 10.0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class ResistorSinkRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor): TransparentNodeElementRender(entity, desc_) {
    val desc: ResistorSinkDescriptor

    var mode: String
    var r: Double
    var p: Double
    var updated = false

    init {
        desc = desc_ as ResistorSinkDescriptor
        mode = "R"
        r = 1.0
        p = 1.0
    }

    override fun draw() {
        GL11.glColor3f(5f, 1f, 1f)
        desc.draw()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return ResistorSinkGui(player, this)
    }

    override fun serverPacketUnserialize(stream: DataInputStream) {
        super.serverPacketUnserialize(stream)
        when (stream.readByte()) {
            ResistorSinkDescriptor.SERVER_RESISTANCE -> {
                r = stream.readDouble()
                Eln.dp.println(DebugType.TRANSPARENT_NODE, "Got update! r: " + r)
                updated = true
            }
            ResistorSinkDescriptor.SERVER_POWER -> {
                p = stream.readDouble()
                Eln.dp.println(DebugType.TRANSPARENT_NODE, "Got update! p: " + p)
                updated = true
            }
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            mode = stream.readUTF()
            r = stream.readDouble()
            p = stream.readDouble()
            updated = true
            Eln.dp.println(DebugType.TRANSPARENT_NODE, "Recieved packet! mode: " + mode.toString() + " r: " + r + " p: " + p)
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class ResistorSinkGui(player: EntityPlayer, internal val render: ResistorSinkRender): GuiScreenEln() {
    private var stateBt: GuiButtonEln? = null
    private var resistanceField: GuiTextFieldEln? = null
    private var powerField: GuiTextFieldEln? = null

    override fun initGui() {
        super.initGui()

        stateBt = newGuiButton(6, 6, 70, "RESISTANCE")
        resistanceField = newGuiTextField(16, 30, 50)
        powerField = newGuiTextField(16, 44, 50)

        resistanceField!!.setComment(0, "Set the resistance")
        powerField!!.setComment(0, "Set the power")

        resistanceField!!.text = render.r.toString()
        powerField!!.text = render.p.toString()
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

            render.clientSendString(ResistorSinkDescriptor.CLIENT_MODE, render.mode)
        }
        if (`object` === resistanceField) {
            render.clientSendString(ResistorSinkDescriptor.CLIENT_RESISTANCE, resistanceField!!.text)
        }
        if (`object` === powerField) {
            render.clientSendString(ResistorSinkDescriptor.CLIENT_POWER, powerField!!.text)
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        if (render.updated) {
            Eln.dp.println(DebugType.RENDER, "Updated RS!")
            resistanceField!!.text = render.r.toString()
            powerField!!.text = render.p.toString()
            render.updated = false
        }
    }

    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 82, 64)
    }
}
