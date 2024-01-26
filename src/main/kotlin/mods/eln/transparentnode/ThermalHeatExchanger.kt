package mods.eln.transparentnode.themralheatexchanger

import mods.eln.Eln
import mods.eln.fluid.ElementSidedFluidHandler
import mods.eln.fluid.TankData
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoadInitializerByPowerDrop
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidTank
import net.minecraftforge.fluids.IFluidHandler
import org.lwjgl.opengl.GL11
import java.lang.Math.ceil
import java.lang.Math.min

class ThermalHeatExchangerDescriptor(
    name: String,
    val thermal: ThermalLoadInitializerByPowerDrop
): TransparentNodeDescriptor(
    name,
    ThermalHeatExchangerElement::class.java,
    ThermalHeatExchangerRender::class.java,
    EntityMetaTag.Fluid
) {

    val main = Eln.obj.getObj("thermal_heat_exchanger").getPart("Plane_Plane.001")

    init {
        thermal.setMaximalPower(16_000.0)
        voltageLevelColor = VoltageLevelColor.Thermal
    }

    fun draw() {
        GL11.glTranslated(-0.5, -0.5, 0.5)
        main.draw()
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true
    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper) = true //type != IItemRenderer.ItemRenderType.INVENTORY
    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) =
        draw()//if (type == IItemRenderer.ItemRenderType.INVENTORY) super.renderItem(type, item, *data) else draw()

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Generates heat when supplied with ic2:hotcoolant"))
        list.add(tr("Ejects out ic2:coolant"))
        list.add(Utils.plotCelsius(tr("  Max. temperature: "), thermal.maximumTemperature))
    }

    override fun mustHaveFloor() = false
    override fun mustHaveCeiling() = false
    override fun mustHaveWall() = false
    override fun mustHaveWallFrontInverse() = false
}

data class ThermalPairing(val input: Fluid, val output: Fluid, val joulesPerMb: Double, val maxMbInputPerTick: Int, val ratio: Double = 1.0, val reversible: Boolean = false, val minTemp: Double? = null, val maxTemp: Double? = null)

class ThermalHeatExchangerElement(
    transparentNode: TransparentNode,
    descriptor: TransparentNodeDescriptor
): TransparentNodeElement(transparentNode, descriptor) {

    companion object {
        val ic2hotcoolant: Fluid? = FluidRegistry.getFluid("ic2hotcoolant")
        val ic2coolant: Fluid? = FluidRegistry.getFluid("ic2coolant")
        val hotwater: Fluid? = FluidRegistry.getFluid("hot_water")
        val coldwater: Fluid? = FluidRegistry.getFluid("cold_water")
        val ic2hotwater: Fluid? = FluidRegistry.getFluid("ic2hotwater")
        // Use 'steam' but fall back on 'ic2steam'. Or, just die.
        val steam: Fluid? = FluidRegistry.getFluid("steam")?: FluidRegistry.getFluid("ic2steam")
        val INPUT_SIDE = ForgeDirection.DOWN
        val OUTPUT_SIDE = ForgeDirection.UP

    }

    val thermalPairs = mutableListOf<ThermalPairing>()

    private var joulesPerTick = 0.0
    private var inputMbPerTick = 0
    private var outputMbPerTick = 0

    private val electricalControlLoad = NbtElectricalGateInput("control")
    private val thermalLoad = NbtThermalLoad("thermalLoad")
    val tankMap = mapOf(Pair(INPUT_SIDE, TankData(FluidTank(1000))), Pair(OUTPUT_SIDE, TankData(FluidTank(1000))))
    private val tank = ElementSidedFluidHandler(tankMap)
    private val thermalWatchdog = ThermalLoadWatchDog(thermalLoad)
    private val fluidRegulatorProcess = IProcess {
        val inputFluid = tank.getFluidType(INPUT_SIDE)
        var joulesPerMb = 0.0
        if (thermalPairs.isNotEmpty() && tank.getFluidAmount(INPUT_SIDE) > 0 && inputFluid != null) {

            thermalPairs.filter { it.input.id == inputFluid.id || (it.reversible && it.output.id == inputFluid.id) }.forEach {
                if (it.input.id == inputFluid.id) {
                    // Normal Forwards conversion
                    inputMbPerTick = moveFluidProcess(it.output, it.maxMbInputPerTick, it.ratio, it.minTemp, it.maxTemp)
                    outputMbPerTick = (inputMbPerTick * it.ratio).toInt()
                    joulesPerMb = it.joulesPerMb
                } else {
                    // Reversed conversion
                    inputMbPerTick = moveFluidProcess(it.input, it.maxMbInputPerTick, it.ratio, it.minTemp, it.maxTemp)
                    outputMbPerTick = (inputMbPerTick * it.ratio).toInt()
                    joulesPerMb = -it.joulesPerMb
                }
            }

        }
        joulesPerTick = outputMbPerTick * joulesPerMb
    }

    fun moveFluidProcess(outputFluid: Fluid, maxMbInputPerTick: Int, ratio: Double, minTemp: Double?, maxTemp: Double?): Int {
        // Check that we can put the amount into the output, then pull what we can from the input and put to the output

        val maxMbOutputPerTick = ceil(maxMbInputPerTick * ratio).toInt()
        //println("maxMbInputPerTick: $maxMbInputPerTick")
        //println("maxMbOutputPerTick: $maxMbOutputPerTick")
        val canMoveOutputMb = tank.fill(OUTPUT_SIDE, FluidStack(outputFluid, maxMbOutputPerTick), false)
        //println("canMoveOutputMb: $canMoveOutputMb")
        var inTempRange = 1.0
        if (minTemp != null) {
            if (thermalLoad.temperatureCelsius < minTemp) {
                inTempRange = 0.0
            }
        }
        if (maxTemp != null) {
            if (thermalLoad.temperatureCelsius > maxTemp) {
                inTempRange = 0.0
            }
        }
        val shouldMoveOutputMb = min(maxMbOutputPerTick * electricalControlLoad.normalized, canMoveOutputMb.toDouble()) * inTempRange
        //println("shouldMoveOutputMb: $shouldMoveOutputMb")
        val predictedInputMb = ceil(shouldMoveOutputMb / ratio).toInt()
        //println("predictedInputMb: $predictedInputMb")
        if (predictedInputMb > 0) {
            val movedInputMb = tank.drain(INPUT_SIDE, predictedInputMb, true)?.amount?: 0
            tank.fill(OUTPUT_SIDE, FluidStack(outputFluid, (movedInputMb * ratio).toInt()), true)
            //println("movedInputMb: $movedInputMb")
            //println("movedOutputMb: $movedOutputMb")
            return movedInputMb
        }
        return 0
    }

    private val thermalRegulatorProcess = IProcess { time ->
        //Yes, it's magic number time. 1.25 is a rough estimate of the "what the fuck" measure I got from thermal power.
        val heatPower = joulesPerTick / (Eln.instance.thermalFrequency / Eln.instance.electricalFrequency) * 1.25 / time
        thermalLoad.movePowerTo(heatPower)
        //thermalLoad.PcTemp += heatPower
    }

    init {
        electricalLoadList.add(electricalControlLoad)
        thermalLoadList.add(thermalLoad)
        slowPreProcessList.add(fluidRegulatorProcess)
        thermalFastProcessList.add(thermalRegulatorProcess)
        slowProcessList.add(NodePeriodicPublishProcess(transparentNode, 2.0, 1.0))
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog.setTemperatureLimits((descriptor as ThermalHeatExchangerDescriptor).thermal)
            .setDestroys(WorldExplosion(this).machineExplosion())

        if (ic2hotcoolant != null && ic2coolant != null) {
            //println("IC2 Coolant Enabled in Thermal Heat Exchanger")
            thermalPairs.add(ThermalPairing(ic2coolant, ic2hotcoolant, -1920.0 / 7.0, 9, 1.0, false, minTemp = 300.0))
            thermalPairs.add(ThermalPairing(ic2hotcoolant, ic2coolant, 1920.0 / 7.0, 9, 1.0, false))
        }

        if (ic2hotwater != null) {
            thermalPairs.add(ThermalPairing(ic2hotwater,FluidRegistry.WATER,
                1 / 0.45 / 2, //Joules per mB
                36, //max mB input rate
                1.0, //ratio
                false,//reversible?
                minTemp = 26.85,
                maxTemp = 76.85))
        }

        if (steam != null) {
            //println("Steam Enabled in Thermal Heat Exchanger")
            thermalPairs.add(ThermalPairing(FluidRegistry.WATER, steam, -1/0.45, 36,10.0, false, minTemp = 100.0))
        }

        thermalPairs.forEach {
            tank.addFluidWhitelist(INPUT_SIDE, it.input)
            tank.addFluidWhitelist(OUTPUT_SIDE, it.output)
            if (it.reversible) {
                tank.addFluidWhitelist(OUTPUT_SIDE, it.input)
                tank.addFluidWhitelist(INPUT_SIDE, it.output)
            }
        }
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU) = when {
        side == front && lrdu == LRDU.Down -> electricalControlLoad
        else -> null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU) = when {
        side == front.inverse && lrdu == LRDU.Down -> thermalLoad
        else -> null
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU) = when (lrdu) {
        LRDU.Down -> when (side) {
            front.inverse -> NodeBase.maskThermal
            front -> NodeBase.maskElectricalGate
            else -> 0
        }
        else -> 0
    }

    // This would be thermalLoad.power but it's not accurate.
    override fun multiMeterString(side: Direction): String = Utils.plotPercent("Ctl:", electricalControlLoad.normalized)
    override fun thermoMeterString(side: Direction): String = Utils.plotCelsius("T:", thermalLoad.temperatureCelsius) + " " + Utils.plotPower(joulesPerTick * 20)

    override fun getWaila(): Map<String, String> = mutableMapOf(
        Pair(tr("Control"), Utils.plotPercent("", electricalControlLoad.normalized)),
        Pair(tr("input tank level"), tank.getFluidAmount(INPUT_SIDE).toString()),
        Pair(tr("output tank level"), tank.getFluidAmount(OUTPUT_SIDE).toString()),
        Pair(tr("input mB/t"), Utils.plotBuckets("", inputMbPerTick / 1000.0)),
        Pair(tr("output mB/t"), Utils.plotBuckets("", outputMbPerTick / 1000.0)),
        Pair(tr("joules per tick"), joulesPerTick.toString()),
        Pair(tr("thermal power"), Utils.plotPower(joulesPerTick * 20))
    )

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        tank.writeToNBT(nbt, "tank")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        tank.readFromNBT(nbt, "tank")
    }

    override fun initialize() {
        (descriptor as ThermalHeatExchangerDescriptor).thermal.applyToThermalLoad(thermalLoad)
        connect()
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float) = false

    override fun getFluidHandler(): IFluidHandler {
        return tank
    }
}

class ThermalHeatExchangerRender(
    tileEntity: TransparentNodeEntity,
    descriptor: TransparentNodeDescriptor
): TransparentNodeElementRender(tileEntity, descriptor) {
    override fun draw() {
        front!!.glRotateXnRef()
        (transparentNodedescriptor as ThermalHeatExchangerDescriptor).draw()
    }
}
