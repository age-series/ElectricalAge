package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.PhysicalInterpolator
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import org.lwjgl.util.Color
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*


class ElectricalVuMeterDescriptor(name: String, objName: String, var onOffOnly: Boolean) : SixNodeDescriptor(name, ElectricalVuMeterElement::class.java, ElectricalVuMeterRender::class.java) {
    var obj: Obj3D?

    enum class ObjType {
        Rot, LedOnOff
    }

    var objType: ObjType? = null
    var vumeter: Obj3D.Obj3DPart? = null
    var pointer: Obj3D.Obj3DPart? = null
    var led: Obj3D.Obj3DPart? = null
    var halo: Obj3D.Obj3DPart? = null
    var main: Obj3D.Obj3DPart? = null
    @JvmField
    var pinDistance: FloatArray? = null
    val isRGB: Boolean
    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addSignal(newItemStack())
    }

    fun draw(factorArg: Float, entity: TileEntity?) {
        var factor = factorArg
        if (factor < 0.0) factor = 0.0f
        if (factor > 1.0) factor = 1.0f
        when (objType) {
            ObjType.LedOnOff -> {
                main!!.draw()
                if (isRGB) {
                    val ledColor = Color()
                    ledColor.fromHSB(factor, 1f, 1f)
                    if (factor > 0.005f) {
                        GL11.glColor3f(ledColor.red / 255f, ledColor.green / 255f, ledColor.blue / 255f)
                    } else {
                        GL11.glColor3f(0.5f, 0.5f, 0.5f)
                    }
                    UtilsClient.drawLight(led)
                    if (entity != null) {
                        if (factor > 0.005f) {
                            UtilsClient.drawHalo(halo, ledColor.red / 255f, ledColor.green / 255f, ledColor.blue / 255f, entity, false)
                        }
                    } else {
                        if (factor > 0.005f) {
                            UtilsClient.drawLight(halo)
                        }
                    }
                } else {
                    val s = factor > 0.5
                    val c = UtilsClient.ledOnOffColorC(s)
                    GL11.glColor3f(c.red / 255f, c.green / 255f, c.blue / 255f)
                    UtilsClient.drawLight(led)
                    if (entity != null) UtilsClient.drawHalo(halo, c.red / 255f, c.green / 255f, c.blue / 255f, entity, false) else UtilsClient.drawLight(halo)
                }
            }
            ObjType.Rot -> {
                vumeter!!.draw()
                val alphaOff: Float = pointer!!.getFloat("alphaOff")
                val alphaOn: Float = pointer!!.getFloat("alphaOn")
                pointer!!.draw(factor * (alphaOn - alphaOff) + alphaOff, 1.0f, 0f, 0f)
            }
            else -> {
            }
        }
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (isRGB)
            list.add(tr("Displays a color based on the value of a signal"))
        else
            list.add(tr("Displays the value of a signal."))
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper) = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true

    override fun shouldUseRenderHelperEln(type: IItemRenderer.ItemRenderType?, item: ItemStack?, helper: IItemRenderer.ItemRendererHelper?) = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            draw(0.0f, null)
        }
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player)!!.inverse()
    }

    init {
        this.name = name
        obj = Eln.obj.getObj(objName)
        if (obj != null) {
            if (obj!!.getString("type").lowercase() == "rot") {
                objType = ObjType.Rot
                vumeter = obj!!.getPart("Vumeter")
                pointer = obj!!.getPart("Pointer")
                pinDistance = Utils.getSixNodePinDistance(vumeter!!)
            }
            if (obj!!.getString("type") == "LedOnOff") {
                objType = ObjType.LedOnOff
                main = obj!!.getPart("main")
                halo = obj!!.getPart("halo")
                led = obj!!.getPart("Led")
                pinDistance = Utils.getSixNodePinDistance(main!!)
            }
        }
        isRGB = super.name == "Multicolor LED vuMeter"
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    override fun canBePlacedOnSide(player: EntityPlayer?, side: Direction) = true
}


class ElectricalVuMeterElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {
    @JvmField
    var inputGate = NbtElectricalGateInput("inputGate")
    var slowProcess = ElectricalVuMeterSlowProcess(this)
    @JvmField
    var descriptor: ElectricalVuMeterDescriptor = descriptor as ElectricalVuMeterDescriptor
    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val value = nbt.getByte("front")
        front = LRDU.fromInt(value.toInt() shr 0 and 0x3)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setByte("front", (front.toInt() shl 0).toByte())
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        return if (front == lrdu) inputGate else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return if (front == lrdu) NodeBase.maskElectricalInputGate else 0
    }

    override fun multiMeterString(): String {
        return Utils.plotVolt("U:", inputGate.voltage) + Utils.plotAmpere("I:", inputGate.current)
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        if (descriptor.isRGB)
            info[tr("Input")] = Utils.plotVolt(inputGate.signalVoltage)
        else
            info[tr("Input")] = if (inputGate.stateHigh()) tr("ON") else tr("OFF")
        return info
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeByte(front.toInt() shl 4)
            stream.writeFloat((inputGate.voltage / Eln.SVU).toFloat())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun initialize() {}

    init {
        electricalLoadList.add(inputGate)
        slowProcessList.add(slowProcess)
    }
}


class ElectricalVuMeterRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {
    var descriptor: ElectricalVuMeterDescriptor = descriptor as ElectricalVuMeterDescriptor
    var interpolator: PhysicalInterpolator = PhysicalInterpolator(0.4f, 2.0f, 1.5f, 0.2f)
    var factor = 0f
    var boot = true
    override fun draw() {
        super.draw()
        drawSignalPin(front, descriptor.pinDistance)
        if (side.isY) {
            front!!.right().glRotateOnX()
        }
        descriptor.draw(if (descriptor.onOffOnly) interpolator.target else interpolator.get(), tileEntity)
    }

    override fun refresh(deltaT: Float) {
        interpolator.step(deltaT)
    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            val b = stream.readByte()
            this.front = LRDU.fromInt(b.toInt() shr 4 and 3)
            if (boot) {
                interpolator.setPos(stream.readFloat())
            } else {
                interpolator.target = stream.readFloat()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (boot) {
            boot = false
        }
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor {
        return Eln.instance.signalCableDescriptor.render
    }
}


class ElectricalVuMeterSlowProcess(var element: ElectricalVuMeterElement) : IProcess {
    var timeCounter = 0.0
    var lastState: Boolean
    override fun process(time: Double) {
        if (element.descriptor.onOffOnly) {
            if (lastState) {
                if (element.inputGate.stateLow()) {
                    lastState = false
                    element.needPublish()
                }
            } else {
                if (element.inputGate.stateHigh()) {
                    lastState = true
                    element.needPublish()
                }
            }
        } else {
            timeCounter += time
            if (timeCounter >= refreshPeriod) {
                timeCounter -= refreshPeriod
                element.needPublish()
            }
        }
    }

    companion object {
        const val refreshPeriod = 0.25
    }

    init {
        lastState = element.inputGate.stateHigh()
    }
}
