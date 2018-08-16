package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.GenericItemUsingDamage
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.HelperStdContainer
import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream

class ClutchPlateItem(name: String) : GenericItemUsingDamageDescriptor(name) {
    override fun getDefaultNBT() = NBTTagCompound()

    fun setWear(stack: ItemStack, wear: Double) {
        if (!stack.hasTagCompound()) {
            stack.tagCompound = defaultNBT
        }
        stack.tagCompound.setDouble("wear", wear)
    }

    fun getWear(stack: ItemStack): Double {
        if (!stack.hasTagCompound()) return 0.0;
        return stack.tagCompound.getDouble("wear")
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<Any?>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if(itemStack != null) {
            val wear = getWear(itemStack)
            if(wear < 0.2) {
                list?.add(tr("Condition:") + " " + tr("New"))
            } else if(wear < 0.5) {
                list?.add(tr("Condition:") + " " + tr("Good"))
            } else if(wear < 0.8) {
                list?.add(tr("Condition:") + " " + tr("Used"))
            } else if(wear < 0.9) {
                list?.add(tr("Condition:") + " " + tr("End of life"))
            } else {
                list?.add(tr("Condition:") + " " + tr("Bad"))
            }
        }
    }
}

class ClutchDescriptor(name: String, override val obj: Obj3D) : SimpleShaftDescriptor(name, ClutchElement::class, ClutchRender::class, EntityMetaTag.Basic) {
    companion object {
        val degToRad = 360.0 / (2 * Math.PI)
    }

    override val static: Array<out Obj3D.Obj3DPart> = arrayOf(
        obj.getPart("Stand"),
        obj.getPart("Cowl")
    )
    override val rotating: Array<out Obj3D.Obj3DPart> = emptyArray()

    val staticAllSides = arrayOf(obj.getPart("Cap"))
    val rotatingAllSides = arrayOf(obj.getPart("Shaft"))

    override fun draw(angle: Double) {
        draw(angle, Direction.XP, DirectionSet())
    }

    fun draw(angle: Double, front: Direction, connectedSides: DirectionSet) {
        static.forEach { it.draw() }

        val bb = rotatingAllSides[0].boundingBox()
        val center = bb.centre()
        var direction = front
        preserveMatrix {
            direction.glRotateXnRef()

            for (i in 0..3) {
                if (connectedSides.contains(direction)) {
                    val rotAngle = if (direction == Direction.XP || direction == Direction.ZN) {
                        angle
                    } else {
                        -angle
                    }
                    preserveMatrix {
                        direction.glRotateXnRef()
                        GL11.glTranslated(center.xCoord, center.yCoord, center.zCoord)
                        GL11.glRotated(degToRad * rotAngle, 1.0, 0.0, 0.0)
                        GL11.glTranslated(-center.xCoord, -center.yCoord, -center.zCoord)
                        rotatingAllSides.forEach { it.draw() }
                    }
                } else {
                    preserveMatrix {
                        direction.glRotateXnRef()
                        staticAllSides.forEach { it.draw() }
                    }
                }

                direction = direction.left()
            }
        }
    }
}

class ClutchElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_) {
    companion object {
        // rads -> rads
        val staticMarginF = LinearFunction(0f, 3f, 1000f, 10f)
        // wear -> Joules
        val maxStaticEnergyF = LinearFunction(0f, 256000f, 1f, 0f)
        // wear -> Joules
        val dynamicMaxTransferF = LinearFunction(0f, 12800f, 1f, 3200f)
        // rads -> wear
        val slipWearF = LinearFunction(0f, 0f, 1000f, 0.0001f)
    }

    override val shaftMass: Double = 0.5
    val connectedSides = DirectionSet()
    var leftShaft = ShaftNetwork()
    var rightShaft = ShaftNetwork()
    override fun initialize() {
        reconnect()
        leftShaft = ShaftNetwork(this, front.left())
        rightShaft = ShaftNetwork(this, front.right())
        leftShaft.connectShaft(this, front.left())
        rightShaft.connectShaft(this, front.right())
        if(getShaft(front.left()) != leftShaft)
            Utils.println("CE.init ERROR: getShaft(left) != leftShaft")
        if(getShaft(front.right()) != rightShaft)
            Utils.println("CE.init ERROR: getShaft(right) != rightShaft")
    }

    override fun onBreakElement() {
        destructing = true
        leftShaft.disconnectShaft(this)
        rightShaft.disconnectShaft(this)
    }

    override fun isDestructing() = destructing

    override fun getShaft(dir: Direction) = when(dir) {
        front.left() -> leftShaft
        front.right() -> rightShaft
        else -> null
    }
    override fun setShaft(dir: Direction, net: ShaftNetwork?) {
        if(net == null) return
        when(dir) {
            front.left() -> leftShaft = net
            front.right() -> rightShaft = net
            else -> Unit
        }
    }

    override fun isInternallyConnected(a: Direction, b: Direction) = false

    val inv = TransparentNodeElementInventory(1, 1, this)
    override fun getInventory() = inv
    override fun newContainer(side: Direction?, player: EntityPlayer?) = ClutchContainer(player, inv)
    override fun hasGui() = true

    val inputGate = NbtElectricalGateInput("clutchIn")
    var slipping = false
    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = inputGate
    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = NodeBase.maskElectricalInputGate

    val clutchPlateStack: ItemStack?
        get() {
            return inv.getStackInSlot(0)
        }
    @Suppress("UNCHECKED_CAST")
    val clutchPlateDescriptor: ClutchPlateItem?
        get() {
            val stack = clutchPlateStack
            if(stack == null) return null
            return (stack.item!! as GenericItemUsingDamage<GenericItemUsingDamageDescriptor>).getDescriptor(stack) as ClutchPlateItem
        }

    inner class ClutchProcess : IProcess {
        override fun process(time: Double) {
            slipping = true
            val clutching = inputGate.normalized
            if (clutching == 0.0) {
                // Utils.println("CP.p: stop: no input")
                return
            }

            val plateDescriptor = clutchPlateDescriptor
            val stack = clutchPlateStack
            if(plateDescriptor == null || stack == null) {
                // Utils.println("CP.p: stop: no inventory")
                return
            }
            val wear = plateDescriptor.getWear(stack)
            if(wear >= 1.0) {
                // Utils.println("CP.p: stop: wear too high")
                return
            }

            if(leftShaft == rightShaft) Utils.println("WARN (ClutchProcess): Networks are the same!")

            val rdiff = Math.abs(leftShaft.rads - rightShaft.rads)
            val ediff = Math.abs(leftShaft.energy - rightShaft.energy)
            val rcrit = clutching * staticMarginF.getValue(Math.max(leftShaft.rads, rightShaft.rads))
            slipping = rdiff > rcrit

            if(!slipping) {
                val ecrit = clutching * maxStaticEnergyF.getValue(wear)
                slipping = ediff > ecrit
            }

            if(slipping) {
                val maxEXfer = clutching * dynamicMaxTransferF.getValue(wear)
                val eqre = (rdiff + rcrit / 2.0) * Math.min(leftShaft.joulePerRad, rightShaft.joulePerRad)
                val eXfer = Math.min(maxEXfer, eqre)
                if(leftShaft.rads < rightShaft.rads) {
                    leftShaft.energy += eXfer
                    rightShaft.energy -= eXfer
                } else {
                    rightShaft.energy += eXfer
                    leftShaft.energy -= eXfer
                }

                val addWear = clutching * slipWearF.getValue(rdiff)
                plateDescriptor.setWear(stack, wear + addWear)
            } else {
                val ravg = (leftShaft.rads + rightShaft.rads) / 2.0
                leftShaft.rads = ravg
                rightShaft.rads = ravg
            }

            // Utils.println("CP.p: processed")
        }
    }

    val clutchProcess = ClutchProcess()

    init {
        electricalLoadList.add(inputGate)
        slowProcessList.add(clutchProcess)
    }

    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(front.left(), front.right())

    override fun connectedOnSide(direction: Direction, net: ShaftNetwork) {
        connectedSides.add(direction)
        needPublish()
    }

    override fun disconnectedOnSide(direction: Direction, net: ShaftNetwork?) {
        connectedSides.remove(direction)
        needPublish()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        connectedSides.serialize(stream)
    }

    /*
    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        connectedNetworks.forEach {
            var shaftTag = NBTTagCompound()
            it.value.writeToNBT(shaftTag, "shaft")
            nbt.setTag("side" + it.key.toSideValue().toString(), shaftTag)
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        connectedNetworks.clear()
        nbt.func_150296_c().forEach {
            val str = it as String
            if(str.startsWith("side")) {
                val shaftTag = nbt.getCompoundTag(str)
                val net = ShaftNetwork()
                net.readFromNBT(shaftTag, "shaft")
                net.rebuildNetwork()
                connectedNetworks.put(
                    Direction.fromInt(str.substring(4).toInt()),
                    net
                )
            }
        }
    }
    */

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        connectedSides.writeToNBT(nbt, "sides")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        connectedSides.readFromNBT(nbt, "sides")
    }

    override fun getWaila(): MutableMap<String, String> {
        var info = mutableMapOf<String, String>()
        val entries = mapOf(Pair(front.left(), leftShaft), Pair(front.right(), rightShaft)).entries
        info.put("Speeds", entries.map {
            Utils.plotRads("", it.value.rads)
        }.joinToString(", "))
        info.put("Energies", entries.map {
            Utils.plotEnergy("", it.value.energy)
        }.joinToString(", "))
        info.put("Masses", entries.map {
            Utils.plotValue(it.value.mass, "kg")
        }.joinToString(", "))
        val desc = clutchPlateDescriptor
        val stack = clutchPlateStack
        if(desc != null && stack != null)
            info.put("Wear", String.format("%.6f", desc.getWear(stack)))
        info.put("Clutching", Utils.plotVolt(inputGate.bornedU))
        info.put("Slipping", if(slipping){ "YES" } else { "NO" })
        return info
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null
    override fun thermoMeterString(side: Direction?): String? = null
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean = false
}

class ClutchRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    val desc = desc as ClutchDescriptor
    val connectedSides = DirectionSet()
    val inv = TransparentNodeElementInventory(1, 1, this)
    override fun getInventory() = inv

    override fun draw() {
        front.glRotateXnRef()
        desc.draw(angle, front, connectedSides)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connectedSides.deserialize(stream)
    }

    override fun newGuiDraw(side: Direction?, player: EntityPlayer?): GuiScreen? = ClutchGui(player, inv, this)

    override val cableRender: CableRenderDescriptor?
        get() = Eln.instance.stdCableRenderSignal
}

class ClutchContainer(player: EntityPlayer?, inv: IInventory) : BasicContainer(
    player, inv, arrayOf(
        GenericItemUsingDamageSlot(inv, 0, 176 / 2 - 16 / 2 + 4, 42 - 16 / 2, 1, ClutchPlateItem::class.java, ISlotSkin.SlotSkin.medium, arrayOf(tr("Clutch Plate")))
    )
)

class ClutchGui(player: EntityPlayer?, inv: IInventory, val render: ClutchRender) : GuiContainerEln(ClutchContainer(player, inv)) {
    override fun newHelper() = HelperStdContainer(this)
}
