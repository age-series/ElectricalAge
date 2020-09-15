package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.fluid.FuelRegistry
import mods.eln.fluid.PreciseElementFluidHandler
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.RcInterpolator
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.published
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream

class RotaryMotorDescriptor(baseName: String, obj: Obj3D) :
    SimpleShaftDescriptor(baseName, RotaryMotorElement::class, RotaryMotorRender::class, EntityMetaTag.Fluid) {

    override val sound = "eln:RotaryEngine"
    override val static = arrayOf(
        obj.getPart("Body_Cylinder.001")
    )
    override val rotating = arrayOf(
        obj.getPart("Shaft")
    )
    override fun preDraw() {
        GL11.glTranslated(-0.5, -1.5, 0.5)
    }
    // Overall time for steam input changes to take effect, in seconds.
    val inertia: Float = 3f
    // Optimal fluid consumed per second, mB.
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    val fluidConsumption: Float = 24f
    // How we describe the fluid in the tooltip.
    val fluidDescription: String = "gasoline"
    // The fluids actually accepted.
    val fluidTypes: Array<String> = FuelRegistry.gasolineList + FuelRegistry.gasList
    // Width of the efficiency curve.
    // <1 means "Can't be started without power".
    val efficiencyCurve: Float = 1.5f
    val optimalRads = absoluteMaximumShaftSpeed * 0.8f
    // Power stats
    val power: List<Double> by lazy {
        fluidTypes.map { FuelRegistry.heatEnergyPerMilliBucket(it) * fluidConsumption }
    }
    val maxFluidPower: Double by lazy {
        power.max() ?: 0.0
    }
    val minFluidPower: Double by lazy {
        power.min() ?: 0.0
    }

    @Suppress("CanBePrimaryConstructorProperty") // If you do that, it changes the constructor and BLAMO, Crash!
    override val obj: Obj3D = obj

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add("Converts ${fluidDescription} into mechanical energy.")
        list.add("Nominal usage ->")
        list.add("  ${fluidDescription.capitalize()} input: ${fluidConsumption} mB/s")
        if (power.isEmpty()) {
            list.add("  No valid fluids for this turbine!")
        } else if (power.size == 1) {
            list.add(Utils.plotPower("  Power out: ", power[0]))
        } else {
            list.add("  Power out: ${Utils.plotPower(minFluidPower)}- ${Utils.plotPower(maxFluidPower)}")
        }
        list.add(Utils.plotRads("  Optimal rads: ", optimalRads))
        list.add(Utils.plotRads("Max rads:  ", absoluteMaximumShaftSpeed))
    }
}

class RotaryMotorElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    SimpleShaftElement(node, desc_) {
    val desc = desc_ as RotaryMotorDescriptor

    val tank = PreciseElementFluidHandler(desc.fluidConsumption.toInt())
    var fluidRate = 0f
    var efficiency = 0f
    val rotaryMotorSlowProcess = RotaryMotorSlowProcess()

    internal val throttle = NbtElectricalGateInput("throttle")

    internal var volume: Float by published(0f)

    inner class RotaryMotorSlowProcess() : IProcess, INBTTReady {
        val rc = RcInterpolator(desc.inertia)

        override fun process(time: Double) {
            // Do anything at all?
            val target: Float
            val computedEfficiency = if (shaft.rads > 700) {
                 Math.max(Math.pow(Math.cos(((shaft.rads - desc.optimalRads) / (desc.optimalRads * desc.efficiencyCurve)) * (Math.PI / 2)), 3.0), 0.0)
            } else {
                0.25
            }
            efficiency = computedEfficiency.toFloat()
            val th = if (throttle.connectedComponents.count() > 0) throttle.normalized else 1.0
            target = (desc.fluidConsumption * th).toFloat()

            val drained = tank.drain(target * time).toFloat()

            rc.target = (drained / time).toFloat()
            rc.step(time.toFloat())
            fluidRate = rc.get()

            val power = fluidRate * tank.heatEnergyPerMilliBucket * efficiency
            shaft.energy += power * time.toFloat()

            volume = if (fluidRate > 0.25) {
                Math.max(0.75f, (power / desc.maxFluidPower).toFloat())
            } else {
                0.0f
            }
        }

        override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
            rc.readFromNBT(nbt, str)
        }

        override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
            rc.writeToNBT(nbt, str)
        }
    }

    init {
        tank.setFilter(FuelRegistry.fluidListToFluids(desc.fluidTypes))
        slowProcessList.add(rotaryMotorSlowProcess)
        electricalLoadList.add(throttle)
    }

    override fun getFluidHandler() = tank

    override fun getElectricalLoad(side: Direction, lrdu: LRDU) = throttle
    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = null
    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu == LRDU.Down && (side == front.up() || side == front.down())) return NodeBase.maskElectricalGate
        if (lrdu == LRDU.Up && (side == front.up() || side == front.down())) return NodeBase.maskElectricalGate
        if (lrdu == LRDU.Left && (side == front || side == front.back())) return NodeBase.maskElectricalGate
        if (lrdu == LRDU.Right && (side == front || side == front.back())) return NodeBase.maskElectricalGate
        return 0
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun thermoMeterString(side: Direction?) = Utils.plotPercent(" Eff:", efficiency.toDouble()) + fluidRate.toString() + "mB/s"

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        tank.writeToNBT(nbt, "tank")
        rotaryMotorSlowProcess.writeToNBT(nbt, "proc")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        tank.readFromNBT(nbt, "tank")
        rotaryMotorSlowProcess.readFromNBT(nbt, "proc")
    }

    override fun getWaila(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info.put("Speed", Utils.plotRads("", shaft.rads))
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        if (Eln.wailaEasyMode) {
            info.put("Efficiency", Utils.plotPercent("", efficiency.toDouble()))
            info.put("Fuel usage", Utils.plotBuckets("", fluidRate / 1000.0) + "/s")
        }
        return info
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(volume)
    }
}

// TODO: Particles flying out the exhaust pipe
class RotaryMotorRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    override val cableRender = Eln.instance.stdCableRenderSignal

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        volumeSetting.target = stream.readFloat()
    }
}
