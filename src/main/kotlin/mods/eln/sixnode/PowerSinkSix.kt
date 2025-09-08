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
import mods.eln.sim.mna.component.CurrentSource
import mods.eln.sim.nbt.NbtElectricalLoad
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
import java.util.*

class PowerSinkDescriptor(name: String, obj: Obj3D) : SixNodeDescriptor(name, PowerSinkElement::class.java, PowerSinkRender::class.java) {

    private var main: Obj3D.Obj3DPart = obj.getPart("main")
    fun draw() {
        main.draw()
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll<String>(list, *tr("Provides an ideal power sink\nwithout energy or power limitation.").split("\n").toTypedArray())
        list.add("")
        list.add(tr("Internal resistance: %1$\u2126", Utils.plotValue(Eln.instance.lowVoltageCableDescriptor.electricalRs)))
        list.add("")
        list.add(tr("Creative block."))
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        when (type) {
            IItemRenderer.ItemRenderType.ENTITY -> draw()
            IItemRenderer.ItemRenderType.EQUIPPED, IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON -> {
                GL11.glPushMatrix()
                GL11.glTranslatef(0.8f, 0.3f, 0.2f)
                GL11.glRotatef(150f, 0f, 0f, 1f)
                draw()
                GL11.glPopMatrix()
            }
            IItemRenderer.ItemRenderType.INVENTORY, IItemRenderer.ItemRenderType.FIRST_PERSON_MAP -> super.renderItem(type, item, *data)
        }
    }

    override fun canBePlacedOnSide(player: EntityPlayer?, side: Direction) = true

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }
}


class PowerSinkElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor), IConfigurable {
    var electricalLoad = NbtElectricalLoad("electricalLoad")
    var currentSource = CurrentSource("currSrc", electricalLoad, null)

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        currentSource.current = nbt.getDouble("current")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble("current", currentSource.current)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad {
        return electricalLoad
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return NodeBase.maskElectricalPower
    }

    override fun multiMeterString(): String {
        return Utils.plotUIP(electricalLoad.voltage, currentSource.current)
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Voltage")] = Utils.plotVolt("", electricalLoad.voltage)
        info[tr("Current")] = Utils.plotAmpere("", electricalLoad.current)
        if (Eln.wailaEasyMode) {
            info[tr("Power")] = Utils.plotPower("", electricalLoad.voltage * electricalLoad.current)
        }
        return info
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeDouble(currentSource.current)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            currentSource.current = stream.readDouble()
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun initialize() {
        Eln.applySmallRs(electricalLoad)
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return onBlockActivatedRotate(entityPlayer)
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("current")) {
            currentSource.current = compound.getDouble("current")
            needPublish()
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setDouble("current", currentSource.current)
    }

    init {
        electricalLoadList.add(electricalLoad)
        electricalComponentList.add(currentSource)
    }

    val setVoltageId: Byte = 1
}

class PowerSinkGui(var render: PowerSinkRender) : GuiScreenEln() {
    var current: GuiTextFieldEln? = null
    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 50 + 12, 12 + 12)
    }

    override fun initGui() {
        super.initGui()
        current = newGuiTextField(6, 6, 50)
        current!!.setText(render.current.toFloat())
        current!!.setObserver(this)
        current!!.setComment(arrayOf(tr("Power sourced")))
    }

    override fun textFieldNewValue(textField: GuiTextFieldEln, value: String) {

        val newCurrent = current!!.text.toDoubleOrNull()?: 0.0

        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            render.preparePacketForServer(stream)
            stream.writeDouble(newCurrent)
            render.sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class PowerSinkRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {
    var descriptor: CurrentSourceDescriptor = descriptor as CurrentSourceDescriptor
    var voltage = 0.0
    @JvmField
    var current = 0.0
    override fun draw() {
        super.draw()
        front!!.glRotateOnX()
        descriptor.draw()
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            current = stream.readFloat().toDouble()
            needRedrawCable()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return PowerSinkGui(this)
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor {
        return Eln.instance.veryHighVoltageCableDescriptor.render
    }
}
