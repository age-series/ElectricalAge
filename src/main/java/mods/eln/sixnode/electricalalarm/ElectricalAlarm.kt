package mods.eln.sixnode.electricalalarm

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.core.IProcess
import mods.eln.sim.mna.state.ElectricalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.thermal.ThermalLoad
import mods.eln.sound.LoopedSound
import net.minecraft.client.audio.ISound
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class ElectricalAlarmDescriptor(name: String, internal var obj: Obj3D?, internal var light: Int, internal var soundName: String, internal var soundLevel: Float) : SixNodeDescriptor(name, ElectricalAlarmElement::class.java, ElectricalAlarmRender::class.java) {

    var pinDistance: FloatArray? = null
    internal var main: Obj3D.Obj3DPart? = null
    internal var rot: Obj3D.Obj3DPart? = null
    private var lightPart: Obj3D.Obj3DPart? = null

    private var onTexture: ResourceLocation? = null
    private var offTexture: ResourceLocation? = null
    var rotSpeed = 0f

    init {
        if (obj != null) {
            main = obj!!.getPart("main")
            rot = obj!!.getPart("rot")
            lightPart = obj!!.getPart("light")

            onTexture = obj!!.getModelResourceLocation(obj!!.getString("onTexture"))
            offTexture = obj!!.getModelResourceLocation(obj!!.getString("offTexture"))
            if (rot != null)
                rotSpeed = rot!!.getFloat("speed")
            pinDistance = Utils.getSixNodePinDistance(main!!)
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage
        setDefaultIcon("electricalalarm")
    }

    internal fun draw(warm: Boolean, rotAlpha: Float) {
        if (warm)
            UtilsClient.bindTexture(onTexture)
        else
            UtilsClient.bindTexture(offTexture)
        if (main != null) main!!.drawNoBind()
        if (rot != null) {
            GL11.glDisable(GL11.GL_CULL_FACE)
            GL11.glColor3f(1.0f, 1.0f, 1.0f)
            if (warm)
                UtilsClient.disableLight()
            else
                GL11.glDisable(GL11.GL_LIGHTING)
            rot!!.drawNoBind(rotAlpha, 1f, 0f, 0f)
            if (warm)
                UtilsClient.enableLight()
            else
                GL11.glEnable(GL11.GL_LIGHTING)
            GL11.glEnable(GL11.GL_CULL_FACE)
        }
        if (lightPart != null) {
            UtilsClient.drawLightNoBind(lightPart)
        }
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper?): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun shouldUseRenderHelperEln(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper?): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        }
        draw(true, 0.0f)
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll(list, *I18N.tr("Emits an acoustic alarm if\nthe input signal is high")!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU? {
        return super.getFrontFromPlace(side, player).inverse()
    }
}


class ElectricalAlarmElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {

    private val descriptor: ElectricalAlarmDescriptor

    var inputGate = NbtElectricalGateInput("inputGate")
    var slowProcess = ElectricalAlarmSlowProcess(this)

    private var warm = false
    private var mute = false

    init {
        electricalLoadList.add(inputGate)
        slowProcessList.add(slowProcess)
        this.descriptor = descriptor as ElectricalAlarmDescriptor
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        front = LRDU.fromInt(nbt.getInteger("front"))
        mute = nbt.getBoolean("mute")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setInteger("front", front.toInt())
        nbt.setBoolean("mute", mute)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        return if (front == lrdu) inputGate else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return if (front == lrdu) NodeBase.MASK_ELECTRICAL_INPUT_GATE else 0
    }

    override fun multiMeterString(): String {
        return Utils.plotVolt("U:", inputGate.u) + Utils.plotAmpere("I:", inputGate.current)
    }

    override fun getWaila(): Map<String, String>? {
        val info = HashMap<String, String>()
        info[I18N.tr("Engaged")] = if (inputGate.stateHigh()) I18N.tr("Yes") else I18N.tr("No")
        if (Eln.wailaEasyMode) {
            info[I18N.tr("Input Voltage")] = Utils.plotVolt("", inputGate.u)
        }
        return info
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeInt(front.toInt())
            stream.writeBoolean(warm)
            stream.writeBoolean(mute)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    internal fun setWarm(value: Boolean) {
        if (warm != value) {
            warm = value
            sixNode.recalculateLightValue()
            needPublish()
        }
    }

    override fun initialize() {}

    override fun getLightValue(): Int {
        return if (warm) descriptor.light else 0
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            when (stream.readByte()) {
                clientSoundToggle -> {
                    mute = !mute
                    needPublish()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val clientSoundToggle: Byte = 1
    }
}

class ElectricalAlarmSlowProcess internal constructor(private val element: ElectricalAlarmElement) : IProcess {
    override fun process(dt: Double) {
        val warm = element.inputGate.u > Eln.SVU / 2
        element.setWarm(warm)
    }
}

class ElectricalAlarmRender(tileEntity: SixNodeEntity, side: Direction, descriptor_: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor_) {

    internal var descriptor: ElectricalAlarmDescriptor = descriptor_ as ElectricalAlarmDescriptor
    private var interpol = RcInterpolator(0.4f)

    private var rotAlpha = 0f
    private var warm = false
    var mute = false
    private val sound = object : LoopedSound(this.descriptor.soundName, Coordinate(tileEntity), ISound.AttenuationType.LINEAR) {
        override fun getVolume() = if (warm and !mute) descriptor.soundLevel else 0f
        override fun getPitch() = 1f
    }

    init {
        addLoopedSound(sound)
    }

    override fun draw() {
        super.draw()
        if (side.isY) {
            front?.right()?.glRotateOnX()
            drawSignalPin(LRDU.Down, descriptor.pinDistance)
        } else {
            drawSignalPin(front, descriptor.pinDistance)
        }
        descriptor.draw(warm, rotAlpha)
    }

    override fun refresh(deltaT: Float) {
        interpol.target = if (warm) descriptor.rotSpeed else 0f
        interpol.step(deltaT)
        rotAlpha += interpol.get() * deltaT
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            front = LRDU.fromInt(stream.readInt())
            warm = stream.readBoolean()
            mute = stream.readBoolean()
            DP.println(DPType.SIX_NODE, "WARM : $warm")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return Eln.signalCableDescriptor.render
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? {
        return ElectricalAlarmGui(this)
    }
}

class ElectricalAlarmGui(internal var render: ElectricalAlarmRender) : GuiScreenEln() {

    private var toggleDefaultOutput: GuiButton? = null

    override fun initGui() {
        super.initGui()
        toggleDefaultOutput = newGuiButton(6, 32 / 2 - 10, 115, I18N.tr("Toggle switch"))
    }

    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)
        if (`object` === toggleDefaultOutput) {
            render.clientSend(ElectricalAlarmElement.clientSoundToggle.toInt())
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        if (!render.mute)
            toggleDefaultOutput?.displayString = I18N.tr("Sound is not muted")
        else
            toggleDefaultOutput?.displayString = I18N.tr("Sound is muted")
    }

    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 128, 32)
    }
}
