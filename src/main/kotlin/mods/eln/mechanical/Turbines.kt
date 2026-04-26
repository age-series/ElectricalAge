package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.fluid.FuelRegistry
import mods.eln.fluid.PreciseElementFluidHandler
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.HelperStdContainer
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.TurbineBladeLists
import mods.eln.item.TurbineBladeDescriptor
import mods.eln.misc.*
import mods.eln.node.INodeContainer
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.published
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElementInventory
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

abstract class TurbineDescriptor(baseName: String, obj: Obj3D) :
    SimpleShaftDescriptor(baseName, TurbineElement::class, TurbineRender::class, EntityMetaTag.Fluid) {
    // Overall time for steam input changes to take effect, in seconds.
    abstract val inertia: Float
    // Optimal fluid consumed per second, mB.
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    abstract val fluidConsumption: Float
    // How we describe the fluid in the tooltip.
    abstract val fluidDescription: String
    // The fluids actually accepted.
    abstract val fluidTypes: Array<String>
    // Width of the efficiency curve.
    // <1 means "Can't be started without power".
    abstract val efficiencyCurve: Float
    // If efficiency is below this fraction, do nothing.
    open val efficiencyCutoff = 0f
    val optimalRads = absoluteMaximumShaftSpeed * 0.8f

    // Set by TurbineRender before each draw. Skips the rotating parts when no blade is installed,
    // and controls whether the fan tint is applied.
    var bladeInstalled: Boolean = true

    // RGB tint applied to the Fan part. Set by TurbineRender based on the installed blade tier.
    // Defaults to white (no tint) so a missing blade or unknown tier renders neutrally.
    var bladeR = 1f
    var bladeG = 1f
    var bladeB = 1f

    override fun draw(angle: Double) {
        for (part in static) part.draw()
        if (!bladeInstalled) return
        // Draw the shaft and fan separately so the fan can be tinted by blade tier.
        // rotating[0] is Shaft, rotating[1] is Fan, both spin around the same center.
        preserveMatrix {
            val centre = rotating[0].boundingBox().centre()
            GL11.glTranslated(centre.xCoord, centre.yCoord, centre.zCoord)
            GL11.glRotatef(((angle * 360) / 2.0 / Math.PI).toFloat(), 0f, 0f, 1f)
            GL11.glTranslated(-centre.xCoord, -centre.yCoord, -centre.zCoord)
            rotating[0].draw()                                        // Shaft: inherits the caller's white color
            GL11.glColor3f(bladeR, bladeG, bladeB) // Switch to tier color for the fan
            rotating[1].draw()                                        // Fan: flat-shaded, no texture, pure tint
            GL11.glColor3f(1f, 1f, 1f)             // Reset so nothing drawn after us is tinted
        }
    }
    // Power stats
    val power: List<Double> by lazy {
        fluidTypes.map { FuelRegistry.heatEnergyPerMilliBucket(it) * fluidConsumption }
    }
    val maxFluidPower: Double by lazy {
        power.max()
    }
    val minFluidPower: Double by lazy {
        power.min()
    }
    open val displayFluidUsageInBuckets = false

    override val obj = obj
    override val static = arrayOf(
        obj.getPart("Cowl"),
        obj.getPart("Stand")
    )
    override val rotating = arrayOf(
        obj.getPart("Shaft"),
        obj.getPart("Fan")
    )

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Converts %1$ into mechanical energy.",fluidDescription))
        list.add(tr("Requires a turbine blade to operate."))
        list.add(tr("Nominal usage ->"))
        val formattedRate = formatFluidRate(fluidConsumption)
        list.add("  "+tr("%1$ input: %2$",fluidDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },formattedRate))
        if (power.isEmpty()) {
            list.add("  "+tr("No valid fluids for this turbine!"))
        } else if (power.size == 1) {
            list.add(Utils.plotPower(tr("  Power out: "),power[0]))
        } else {
            list.add("  "+tr("Power out: %1$- %2$",Utils.plotPower(minFluidPower),Utils.plotPower(maxFluidPower)))
        }
        list.add(Utils.plotRads(tr("  Optimal rads: "), optimalRads))
        list.add(Utils.plotRads(tr("Max rads:  "),absoluteMaximumShaftSpeed))

    }

    open fun formatFluidRate(rate: Float): String {
        val scaled = if (displayFluidUsageInBuckets) rate / 1000f else rate
        val unit = if (displayFluidUsageInBuckets) "B/s" else "mB/s"
        return Utils.plotValue(scaled.toDouble()) + " " + unit
    }
}

class SteamTurbineDescriptor(baseName: String, obj: Obj3D, private val capacityScale: Float = 1f) :
    TurbineDescriptor(baseName, obj) {
    // Steam turbines are for baseload.
    override val inertia = 20f
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    override val fluidConsumption = 7200f * capacityScale
    // Computed to equal what you'd get from Railcraft steam engines, plus a small
    // bonus because you're using Electrical Age you crazy person you.
    // This pretty much fills up a VHV line. The generator drag gives us a bit of leeway.
    override val fluidDescription = "steam"
    override val fluidTypes = FuelRegistry.steamList
    // Steam turbines can, just barely, be started without power.
    override val efficiencyCurve = 1.1f
    override val sound = "eln:steam_turbine"
    override val displayFluidUsageInBuckets = true
}

class GasTurbineDescriptor(basename: String, obj: Obj3D, private val capacityScale: Float = 1f) :
    TurbineDescriptor(basename, obj) {
    // The main benefit of gas turbines.
    override val inertia = 5f
    // Provides about 8kW of power, given gasoline.
    // Less dense fuels will be proportionally less effective.
    override val fluidConsumption = 4f * capacityScale
    override val fluidDescription = "gasoline"
    // It runs on puns.
    override val fluidTypes = FuelRegistry.gasolineList + FuelRegistry.gasList
    // Gas turbines are finicky about turbine speed.
    override val efficiencyCurve = 0.5f
    // And need to be spun up before working.
    override val efficiencyCutoff = 0.5f
    override val sound = "eln:gas_turbine"
}

class TurbineElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    SimpleShaftElement(node, desc_) {
    val desc = desc_ as TurbineDescriptor

    val tank = PreciseElementFluidHandler(desc.fluidConsumption.toInt())
    var fluidRate = 0f
    var efficiency = 0f
    val turbineSlowProcess = TurbineSlowProcess()

    internal val throttle = NbtElectricalGateInput("throttle")

    internal var volume: Float by published(0f)

    // Blade slot
    override val inventory = TransparentNodeElementInventory(1, 1, this)

    companion object {
        const val BLADE_SLOT = 0
    }

    inner class TurbineSlowProcess() : IProcess, INBTTReady {
        val rc = RcInterpolator(desc.inertia)

        // Accumulated condition loss waiting to be flushed to the blade's NBT.
        // Written in batches (when >= 0.001) to keep NBT write rate sane.
        var wearAccumulator = 0.0

        override fun process(time: Double) {
            val bladeStack = inventory.getStackInSlot(BLADE_SLOT)
            val blade = TurbineBladeDescriptor.getDescriptor(bladeStack)

            // No blade installed means no power.
            if (blade == null || bladeStack == null) {
                efficiency = 0f
                rc.target = 0f
                rc.step(time.toFloat())
                fluidRate = rc.get()
                volume = 0f
                wearAccumulator = 0.0
                return
            }

            // Do anything at all?
            val target: Float
            val computedEfficiency = Math.pow(Math.cos((shaft.rads - desc.optimalRads) / (desc.optimalRads * desc.efficiencyCurve) * Math.PI / 2), 3.0)
            if (computedEfficiency >= desc.efficiencyCutoff) {
                efficiency = computedEfficiency.toFloat()
                val th = if (throttle.connectedComponents.count() > 0) throttle.normalized else 1.0
                target = (desc.fluidConsumption * th).toFloat()
            } else {
                efficiency = 0f
                target = 0f
            }

            val drained = tank.drain(target * time).toFloat()

            rc.target = (drained / time).toFloat()
            rc.step(time.toFloat())
            fluidRate = rc.get()

            val power = fluidRate * tank.heatEnergyPerMilliBucket * efficiency
            shaft.energy += power * time.toFloat()

            volume = power / desc.maxFluidPower.toFloat()

            // Blade wear: condition loss per second = fuelModifier / (nominalLifeInHours * 3600).
            // Accumulated in wearAccumulator and applied to NBT in batches to reduce write rate.
            if (power > 0.0 && !blade.infiniteLifeEnabled) {
                val fluid = tank.tank.fluid?.getFluid()
                val modifier = BladeWearCalculator.fuelModifier(
                    FuelRegistry.temperatureFactor(fluid),
                    FuelRegistry.cleanlinessFactor(fluid),
                    blade
                )
                wearAccumulator += modifier * time / (blade.nominalLifeInHours * 3600.0)

                if (wearAccumulator >= 0.001) {
                    val newCondition = blade.getCondition(bladeStack) - wearAccumulator
                    wearAccumulator = 0.0
                    if (newCondition <= 0.0) {
                        inventory.setInventorySlotContents(BLADE_SLOT, null)
                        needPublish()
                    } else {
                        blade.setCondition(bladeStack, newCondition)
                    }
                }
            }
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            rc.readFromNBT(nbt, str)
            wearAccumulator = nbt.getDouble(str + "wearAccum")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            rc.writeToNBT(nbt, str)
            nbt.setDouble(str + "wearAccum", wearAccumulator)
        }
    }

    init {
        tank.setFilter(FuelRegistry.fluidListToFluids(desc.fluidTypes))
        slowProcessList.add(turbineSlowProcess)
        electricalLoadList.add(throttle)
        slowProcessList.add(NodePeriodicPublishProcess(node, 2.0, 1.0))
    }

    override fun getFluidHandler() = tank

    override fun getElectricalLoad(side: Direction, lrdu: LRDU) = throttle
    override fun getThermalLoad(side: Direction, lrdu: LRDU) = null
    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu == LRDU.Down && (side == front || side == front.back())) return NodeBase.maskElectricalInputGate
        return 0
    }

    override fun thermoMeterString(side: Direction): String =
        Utils.plotPercent(" Eff:", efficiency.toDouble()) + " " + desc.formatFluidRate(fluidRate)

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        tank.writeToNBT(nbt, "tank")
        turbineSlowProcess.writeToNBT(nbt, "proc")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        tank.readFromNBT(nbt, "tank")
        turbineSlowProcess.readFromNBT(nbt, "proc")
    }

    override fun getWaila(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info[tr("Speed")] = Utils.plotRads("", shaft.rads)
        info[tr("Energy")] = Utils.plotEnergy("", shaft.energy)
        val bladeStack = inventory.getStackInSlot(BLADE_SLOT)
        val blade = TurbineBladeDescriptor.getDescriptor(bladeStack)
        if (blade != null && bladeStack != null) {
            val condition = blade.getCondition(bladeStack)
            val lifeLeftH = condition * blade.nominalLifeInHours
            val lifeLeftStr = when {
                lifeLeftH <= 0.0 -> "0h"
                lifeLeftH < 1.0  -> "<1h"
                else             -> String.format("%.1fh", lifeLeftH)
            }
            info[tr("Blade")] = bladeStack.displayName
            info[tr("Life Left")] = lifeLeftStr
        } else {
            info[tr("Blade")] = tr("None")
        }
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info[tr("Efficiency")] = Utils.plotPercent("", efficiency.toDouble())
            info[tr("Fuel usage")] = desc.formatFluidRate(fluidRate)
        }
        return info
    }

    override fun coordonate(): Coordinate {
        return node!!.coordinate
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(volume)
        val bladeStack = inventory.getStackInSlot(BLADE_SLOT)
        val blade = TurbineBladeDescriptor.getDescriptor(bladeStack)
        if (blade != null && bladeStack != null) {
            stream.writeFloat(blade.getCondition(bladeStack).toFloat())
            // Tier index so the client can pick the right tint color. -1 if the blade
            // isn't in the registered list (shouldn't happen, but safe to handle).
            stream.writeByte(TurbineBladeLists.registeredBlades.indexOf(blade))
        } else {
            stream.writeFloat(-1f)
            stream.writeByte(-1)
        }
    }

    override fun hasGui() = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container =
        TurbineContainer(node, player, inventory)

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        val held = player.currentEquippedItem ?: return false
        if (Utils.getItemObject(held) !is TurbineBladeDescriptor) return false
        // Blade already installed, fall through so the GUI opens instead.
        if (inventory.getStackInSlot(BLADE_SLOT) != null) return false
        // Slot is empty, insert the blade, keeping its condition NBT intact.
        inventory.setInventorySlotContents(BLADE_SLOT, held.splitStack(1))
        inventoryChange(inventory)
        return true
    }

    override fun inventoryChange(inventory: IInventory?) {
        needPublish()
    }
}

class TurbineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    override val cableRender = Eln.instance.stdCableRenderSignal

    override val inventory = TransparentNodeElementInventory(1, 1, this)

    // Blade condition [0,1], or -1 if no blade installed.
    var bladeCondition = -1f
    // Index into TurbineBladeLists.registeredBlades (0=iron, 1=steel, 2=alloy, 3=tungsten), or -1.
    var bladeTier = -1

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        volumeSetting.target = stream.readFloat()
        bladeCondition = stream.readFloat()
        bladeTier = stream.readByte().toInt()
    }

    override fun draw() {
        val desc = transparentNodeDescriptor as TurbineDescriptor
        desc.bladeInstalled = bladeCondition >= 0f
        // Pick the tint color for the fan based on which tier of blade is installed.
        val (r, g, b) = when (bladeTier) {
            0 -> Triple(0.847f, 0.847f, 0.847f) // Iron     #D8D8D8
            1 -> Triple(0.659f, 0.690f, 0.722f) // Steel    #A8B0B8
            2 -> Triple(0.784f, 0.659f, 0.376f) // Alloy    #C8A860
            3 -> Triple(0.290f, 0.290f, 0.353f) // Tungsten #4A4A5A
            else -> Triple(1f, 1f, 1f)          // No blade or unknown
        }
        desc.bladeR = r; desc.bladeG = g; desc.bladeB = b
        super.draw()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer) =
        TurbineGui(player, inventory, this)
}

// Container
class TurbineContainer(
    val base: NodeBase?,
    player: EntityPlayer,
    inventory: IInventory
) : BasicContainer(
    player,
    inventory,
    arrayOf(
        GenericItemUsingDamageSlot(
            inventory,
            TurbineElement.BLADE_SLOT,
            80, 35,
            1,
            TurbineBladeDescriptor::class.java,
            SlotSkin.medium,
            arrayOf(tr("Turbine blade slot"))
        )
    )
), INodeContainer {
    override val node: NodeBase? = base
    override val refreshRateDivider = 1
}

// GUI
class TurbineGui(
    player: EntityPlayer,
    val inventory: IInventory,
    val render: TurbineRender
) : GuiContainerEln(TurbineContainer(null, player, inventory)) {

    override fun newHelper(): GuiHelperContainer = HelperStdContainer(this)
}
