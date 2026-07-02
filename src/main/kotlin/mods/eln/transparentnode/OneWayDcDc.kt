package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.CaseItemDescriptor
import mods.eln.item.ConfigCopyToolDescriptor
import mods.eln.item.FerromagneticCoreDescriptor
import mods.eln.item.IConfigurable
import mods.eln.misc.BasicContainer
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.LRDUMask
import mods.eln.misc.Obj3D
import mods.eln.misc.PhysicalInterpolator
import mods.eln.misc.RealisticEnum
import mods.eln.misc.SlewLimiter
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.six.SixNodeEntity
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementInventory
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sound.LoopedSound
import mods.eln.wiki.Data
import net.minecraft.client.audio.ISound
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class OneWayDcDcMode {
    FIXED,
    BOOST,
    BUCK,
    BOOST_BUCK,
    ISOLATION
}

class OneWayDcDcDescriptor(
    name: String,
    objM: Obj3D,
    coreM: Obj3D,
    casingM: Obj3D,
    val mode: OneWayDcDcMode,
    val minimalLoadToHum: Float = 0.5f
) : TransparentNodeDescriptor(name, OneWayDcDcElement::class.java, OneWayDcDcRender::class.java) {

    companion object {
        const val COIL_SCALE = 4.0f
        const val COIL_SCALE_LIMIT = 16
        const val MAX_RATIO = 16.0
        const val MIN_RATIO = 1.0 / 16.0
    }

    val variable: Boolean
        get() = mode == OneWayDcDcMode.BOOST || mode == OneWayDcDcMode.BUCK || mode == OneWayDcDcMode.BOOST_BUCK

    val isolated: Boolean
        get() = mode == OneWayDcDcMode.ISOLATION

    private var main: Obj3D.Obj3DPart? = objM.getPart("main")
    private var core: Obj3D.Obj3DPart? = coreM.getPart("fero")
    private var coil: Obj3D.Obj3DPart? = objM.getPart("sbire")
    private var casing: Obj3D.Obj3DPart? = casingM.getPart("Case")
    private var casingLeftDoor: Obj3D.Obj3DPart? = casingM.getPart("DoorL")
    private var casingRightDoor: Obj3D.Obj3DPart? = casingM.getPart("DoorR")

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll(list, *tr("Moves power from the input side\nto the output side only.").split("\n").toTypedArray())
        if (variable) {
            Collections.addAll(list, *tr("The output voltage ratio is controlled\nfrom a signal input.").split("\n").toTypedArray())
        }
        if (isolated) {
            Collections.addAll(list, *tr("Front and back connections are isolated\nground references for each side.").split("\n").toTypedArray())
        }
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        list?.add(tr("This DC/DC uses an ideal power sink and source to move power in one direction."))
        return RealisticEnum.IDEAL
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean = true

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            draw(core, 1, 4, false, 0f)
        }
    }

    fun draw(core: Obj3D.Obj3DPart?, priCableNbr: Int, secCableNbr: Int, hasCasing: Boolean, doorOpen: Float) {
        main?.draw()
        core?.draw()
        if (core != null) {
            drawCoils(priCableNbr, false)
            drawCoils(secCableNbr, true)
        }

        if (hasCasing) {
            casing?.draw()
            casingLeftDoor?.draw(-doorOpen * 90, 0f, 1f, 0f)
            casingRightDoor?.draw(doorOpen * 90, 0f, 1f, 0f)
        }
    }

    private fun drawCoils(count: Int, secondary: Boolean) {
        if (count == 0) return
        var scale = COIL_SCALE
        if (count < COIL_SCALE_LIMIT) scale *= count.toFloat() / COIL_SCALE_LIMIT
        GL11.glPushMatrix()
        if (secondary) GL11.glRotatef(180f, 0f, 1f, 0f)
        GL11.glScalef(1f, scale * 2f / (count + 1), 1f)
        GL11.glTranslatef(0f, -0.125f * (count - 1) / COIL_SCALE, 0f)
        for (idx in 0 until count) {
            coil?.draw()
            GL11.glTranslatef(0f, 0.25f / COIL_SCALE, 0f)
        }
        GL11.glPopMatrix()
    }
}

class OneWayDcDcElement(
    transparentNode: TransparentNode,
    descriptor: TransparentNodeDescriptor
) : TransparentNodeElement(transparentNode, descriptor), IConfigurable {
    private val oneWayDescriptor = descriptor as OneWayDcDcDescriptor

    val isolated: Boolean
        get() = oneWayDescriptor.isolated

    val primaryLoad = NbtElectricalLoad("primaryLoad")
    val secondaryLoad = NbtElectricalLoad("secondaryLoad")
    val primaryReferenceLoad = NbtElectricalLoad("primaryReferenceLoad")
    val secondaryReferenceLoad = NbtElectricalLoad("secondaryReferenceLoad")
    val control = NbtElectricalGateInput("control")

    val inputSink = VoltageSource("inputSink")
    val outputSource = VoltageSource("outputSource")
    private val primaryThermalLoad = NbtThermalLoad("primaryThermalLoad")
    private val secondaryThermalLoad = NbtThermalLoad("secondaryThermalLoad")
    private val primaryThermalProcess = WindingThermalProcess(
        thermalLoad = primaryThermalLoad,
        slot = OneWayDcDcContainer.primaryCableSlotId,
        current = { inputSink.current },
        label = "Primary"
    )
    private val secondaryThermalProcess = WindingThermalProcess(
        thermalLoad = secondaryThermalLoad,
        slot = OneWayDcDcContainer.secondaryCableSlotId,
        current = { outputSource.current },
        label = "Secondary"
    )
    private val transferProcess = OneWayDcDcProcess(this)

    override val inventory = TransparentNodeElementInventory(4, 64, this)

    var primaryMeltCurrent = 0.0
    var secondaryMeltCurrent = 0.0
    var ratioControl = 1.0
    var activeRatio = 1.0
    var movedPower = 0.0
    var populated = false

    private val primaryVoltageWatchdog = VoltageStateWatchDog(primaryLoad)
    private val secondaryVoltageWatchdog = VoltageStateWatchDog(secondaryLoad)

    init {
        electricalLoadList.add(primaryLoad)
        electricalLoadList.add(secondaryLoad)
        if (oneWayDescriptor.isolated) {
            electricalLoadList.add(primaryReferenceLoad)
            electricalLoadList.add(secondaryReferenceLoad)
        }
        if (oneWayDescriptor.variable) electricalLoadList.add(control)
        electricalComponentList.add(inputSink)
        electricalComponentList.add(outputSource)
        thermalLoadList.add(primaryThermalLoad)
        thermalLoadList.add(secondaryThermalLoad)
        slowProcessList.add(primaryThermalProcess)
        slowProcessList.add(secondaryThermalProcess)
        primaryThermalLoad.setAsSlow()
        secondaryThermalLoad.setAsSlow()

        val exp = WorldExplosion(this).machineExplosion()
        slowProcessList.add(primaryVoltageWatchdog.setDestroys(exp))
        slowProcessList.add(secondaryVoltageWatchdog.setDestroys(exp))
        slowProcessList.add(NodePeriodicPublishProcess(node!!, 1.0, .5))
    }

    override fun connectJob() {
        Eln.simulator.mna.addProcess(transferProcess)
        super.connectJob()
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.mna.removeProcess(transferProcess)
    }

    override fun initialize() {
        inputSink.connectTo(primaryLoad, if (oneWayDescriptor.isolated) primaryReferenceLoad else null)
        outputSource.connectTo(secondaryLoad, if (oneWayDescriptor.isolated) secondaryReferenceLoad else null)
        computeInventory()
        connect()
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front.left() -> primaryLoad
            front.right() -> secondaryLoad
            front -> if (oneWayDescriptor.isolated) primaryReferenceLoad else if (oneWayDescriptor.variable) control else null
            front.back() -> if (oneWayDescriptor.isolated) secondaryReferenceLoad else if (oneWayDescriptor.variable) control else null
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front.left() -> primaryThermalLoad
            front.right() -> secondaryThermalLoad
            front -> if (oneWayDescriptor.isolated) primaryThermalLoad else null
            front.back() -> if (oneWayDescriptor.isolated) secondaryThermalLoad else null
            else -> null
        }
    }

    override fun thermoMeterString(side: Direction): String {
        return when (side) {
            front.left() -> plotAmbientCelsius("T", primaryThermalLoad.temperatureCelsius)
            front.right() -> plotAmbientCelsius("T", secondaryThermalLoad.temperatureCelsius)
            else -> tr(
                "P: %1$ S: %2$",
                plotAmbientCelsius("", primaryThermalLoad.temperatureCelsius).trim(),
                plotAmbientCelsius("", secondaryThermalLoad.temperatureCelsius).trim()
            )
        }
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        return when (side) {
            front.left(), front.right() -> NodeBase.maskElectricalPower
            front, front.back() -> when {
                oneWayDescriptor.isolated -> NodeBase.maskElectricalPower
                oneWayDescriptor.variable -> NodeBase.maskElectricalInputGate
                else -> 0
            }
            else -> 0
        }
    }

    override fun multiMeterString(side: Direction): String {
        return when (side) {
            front.left() -> Utils.plotVolt("IN:", primaryLoad.voltage) + Utils.plotAmpere("I:", -inputSink.current)
            front.right() -> Utils.plotVolt("OUT:", secondaryLoad.voltage) + Utils.plotAmpere("I:", outputSource.current)
            else -> Utils.plotVolt("IN:", primaryLoad.voltage) + Utils.plotVolt(" OUT:", secondaryLoad.voltage) + Utils.plotPower(" P:", movedPower)
        }
    }

    override fun hasGui(): Boolean = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return OneWayDcDcContainer(player, inventory, oneWayDescriptor.variable)
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean = false

    override fun getLightOpacity(): Float = 1.0f

    override fun onGroundedChangedByClient() {
        super.onGroundedChangedByClient()
        computeInventory()
        reconnect()
    }

    override fun inventoryChange(inventory: IInventory?) {
        disconnect()
        computeInventory()
        connect()
        needPublish()
    }

    private fun computeInventory() {
        val primaryCable = inventory.getStackInSlot(OneWayDcDcContainer.primaryCableSlotId)
        val secondaryCable = inventory.getStackInSlot(OneWayDcDcContainer.secondaryCableSlotId)
        val core = inventory.getStackInSlot(OneWayDcDcContainer.ferromagneticSlotId)
        val primaryWinding = dcDcWinding(primaryCable)
        val secondaryWinding = dcDcWinding(secondaryCable)

        primaryVoltageWatchdog.setNominalVoltage(120_000.0)
        secondaryVoltageWatchdog.setNominalVoltage(120_000.0)
        primaryMeltCurrent = dcDcWindingMeltCurrent(primaryCable)
        secondaryMeltCurrent = dcDcWindingMeltCurrent(secondaryCable)
        primaryThermalProcess.configure(primaryCable)
        secondaryThermalProcess.configure(secondaryCable)

        val coreDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(
            core, FerromagneticCoreDescriptor::class.java
        ) as? FerromagneticCoreDescriptor
        val coreFactor = coreDescriptor?.cableMultiplicator ?: 1.0
        val hasValidCore = coreDescriptor != null

        if (primaryWinding == null || !hasValidCore) {
            primaryLoad.highImpedance()
            if (oneWayDescriptor.isolated) primaryReferenceLoad.highImpedance()
        } else {
            primaryLoad.serialResistance = coreFactor * 0.01
            if (oneWayDescriptor.isolated) primaryReferenceLoad.serialResistance = coreFactor * 0.01
        }

        if (secondaryWinding == null || !hasValidCore) {
            secondaryLoad.highImpedance()
            if (oneWayDescriptor.isolated) secondaryReferenceLoad.highImpedance()
        } else {
            secondaryLoad.serialResistance = coreFactor * 0.01
            if (oneWayDescriptor.isolated) secondaryReferenceLoad.serialResistance = coreFactor * 0.01
        }

        populated = primaryWinding != null && secondaryWinding != null && hasValidCore

        ratioControl = if (populated && primaryWinding != null && secondaryWinding != null) {
            secondaryWinding.amount / primaryWinding.amount
        } else {
            1.0
        }
    }

    fun computeRatio(): Double {
        if (!populated) return 1.0
        val normalized = Utils.limit(control.normalized, 0.0, 1.0)
        val windingRatio = Utils.limit(ratioControl, OneWayDcDcDescriptor.MIN_RATIO, OneWayDcDcDescriptor.MAX_RATIO)
        return when (oneWayDescriptor.mode) {
            OneWayDcDcMode.FIXED -> windingRatio
            OneWayDcDcMode.BOOST -> 1.0 + normalized * (max(1.0, windingRatio) - 1.0)
            OneWayDcDcMode.BUCK -> min(1.0, windingRatio) + normalized * (1.0 - min(1.0, windingRatio))
            OneWayDcDcMode.BOOST_BUCK -> if (normalized < 0.5) {
                min(1.0, windingRatio) + normalized * 2.0 * (1.0 - min(1.0, windingRatio))
            } else {
                1.0 + (normalized - 0.5) * 2.0 * (max(1.0, windingRatio) - 1.0)
            }
            OneWayDcDcMode.ISOLATION -> windingRatio
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeByte(dcDcRenderedWindingCount(inventory.getStackInSlot(0)))
            stream.writeByte(dcDcRenderedWindingCount(inventory.getStackInSlot(1)))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(OneWayDcDcContainer.ferromagneticSlotId))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(OneWayDcDcContainer.primaryCableSlotId))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(OneWayDcDcContainer.secondaryCableSlotId))
            node!!.lrduCubeMask.getTranslate(front.down()).serialize(stream)
            val load = if (primaryMeltCurrent != 0.0 && secondaryMeltCurrent != 0.0) {
                Utils.limit(max(-inputSink.current / primaryMeltCurrent, outputSource.current / secondaryMeltCurrent).toFloat(), 0f, 1f)
            } else {
                0f
            }
            stream.writeFloat(load)
            stream.writeBoolean(inventory.getStackInSlot(OneWayDcDcContainer.CasingSlotId) != null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getWaila(): Map<String, String> {
        val info = linkedMapOf<String, String>()
        info[tr("Ratio")] = Utils.plotValue(activeRatio)
        info[tr("Transferred power")] = Utils.plotPower("", movedPower)
        info[tr("Primary temperature")] = plotAmbientCelsius("", primaryThermalLoad.temperatureCelsius)
        info[tr("Secondary temperature")] = plotAmbientCelsius("", secondaryThermalLoad.temperatureCelsius)
        if (oneWayDescriptor.variable) info[tr("Control Voltage")] = Utils.plotVolt(control.voltage)
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false) || oneWayDescriptor.variable) {
            val primaryVoltage = primaryLoad.voltage - if (oneWayDescriptor.isolated) primaryReferenceLoad.voltage else 0.0
            val secondaryVoltage = secondaryLoad.voltage - if (oneWayDescriptor.isolated) secondaryReferenceLoad.voltage else 0.0
            info[tr("Voltages")] = "\u00A7a" + Utils.plotVolt("", primaryVoltage) + " " +
                "\u00A7e" + Utils.plotVolt("", secondaryVoltage)
        }
        info[tr("Subsystem Matrix Size")] = Utils.renderDoubleSubsystemWaila(primaryLoad.subSystem, secondaryLoad.subSystem)
        return info
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "primary", inventory, OneWayDcDcContainer.primaryCableSlotId, invoker))
            inventoryChange(inventory)
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "secondary", inventory, OneWayDcDcContainer.secondaryCableSlotId, invoker))
            inventoryChange(inventory)
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "core", inventory, OneWayDcDcContainer.ferromagneticSlotId, invoker))
            inventoryChange(inventory)
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "primary", inventory.getStackInSlot(OneWayDcDcContainer.primaryCableSlotId))
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "secondary", inventory.getStackInSlot(OneWayDcDcContainer.secondaryCableSlotId))
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "core", inventory.getStackInSlot(OneWayDcDcContainer.ferromagneticSlotId))
    }

    private inner class WindingThermalProcess(
        private val thermalLoad: NbtThermalLoad,
        private val slot: Int,
        private val current: () -> Double,
        private val label: String
    ) : IProcess {
        private var descriptor: UtilityCableDescriptor? = null
        private var lastPublishedTemperatureCelsius = Double.NaN

        fun configure(stack: ItemStack?) {
            val utilityStackDescriptor = if (stack == null) {
                null
            } else {
                ElectricalCableDescriptor.getDescriptor(
                    stack,
                    ElectricalCableDescriptor::class.java
                ) as? UtilityCableDescriptor
            }
            val nextDescriptor = utilityStackDescriptor?.takeUnless { it.melted }
            if (descriptor != nextDescriptor) {
                if (utilityStackDescriptor?.melted != true) {
                    thermalLoad.temperatureCelsius = 0.0
                }
                lastPublishedTemperatureCelsius = Double.NaN
            }
            descriptor = nextDescriptor
            val utility = descriptor
            if (utility == null) {
                thermalLoad.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
            } else {
                utility.applyTo(thermalLoad)
            }
        }

        override fun process(time: Double) {
            val utility = descriptor ?: return
            if (utility.melted) return

            val amps = current()
            var power = amps * amps * utility.electricalRs * 2.0
            val limit = utility.thermalSelfHeatingRateLimit
            if (limit.isFinite() && limit > 0.0 && thermalLoad.heatCapacity > 0.0) {
                power = power.coerceIn(-limit * thermalLoad.heatCapacity, limit * thermalLoad.heatCapacity)
            }
            thermalLoad.movePowerTo(power)

            val absoluteTemperatureCelsius = thermalLoad.temperatureCelsius + getAmbientTemperatureCelsius()
            maybePublishTemperature(absoluteTemperatureCelsius, utility)

            if (absoluteTemperatureCelsius >= utility.material.meltingPointCelsius ||
                utility.insulated && absoluteTemperatureCelsius >= utility.meltTemperatureCelsius
            ) {
                meltInsertedWire(utility)
            }
        }

        private fun maybePublishTemperature(absoluteTemperatureCelsius: Double, utility: UtilityCableDescriptor) {
            val previous = lastPublishedTemperatureCelsius
            val crossedGlowThreshold = previous <= 550.0 && absoluteTemperatureCelsius > 550.0 ||
                previous > 550.0 && absoluteTemperatureCelsius <= 550.0
            val smokeThreshold = utility.meltTemperatureCelsius * 0.8
            val crossedSmokeThreshold = utility.insulated && !utility.melted && (
                previous <= smokeThreshold && absoluteTemperatureCelsius > smokeThreshold ||
                    previous > smokeThreshold && absoluteTemperatureCelsius <= smokeThreshold
                )
            val changedEnough = previous.isNaN() || abs(absoluteTemperatureCelsius - previous) >= 10.0
            if (changedEnough || crossedGlowThreshold || crossedSmokeThreshold) {
                needPublish()
                lastPublishedTemperatureCelsius = absoluteTemperatureCelsius
            }
        }

        private fun meltInsertedWire(utility: UtilityCableDescriptor) {
            val stack = inventory.getStackInSlot(slot) ?: return
            val melted = utility.meltedDescriptor ?: return
            val replacement = melted.newItemStack(1)
            melted.setRemainingLengthMeters(replacement, utility.getRemainingLengthMeters(stack))
            inventory.setInventorySlotContents(slot, replacement)
            inventory.markDirty()
            Utils.println("One-way DC/DC $label winding melted at ${coordinate()}")
            computeInventory()
            reconnect()
            needPublish()
        }
    }
}

class OneWayDcDcProcess(private val element: OneWayDcDcElement) : IRootSystemPreStepProcess {
    override fun rootSystemPreStepProcess() {
        element.activeRatio = element.computeRatio()
        element.movedPower = 0.0

        if (!element.populated) {
            idleSourcesFromThevenin()
            return
        }

        val inputSystem = element.primaryLoad.subSystem ?: run {
            idleSources()
            return
        }
        val outputSystem = element.secondaryLoad.subSystem ?: run {
            idleSources()
            return
        }
        val inputTh = if (element.isolated) {
            getBipoleTh(inputSystem, element.primaryLoad, element.primaryReferenceLoad, element.inputSink)
        } else {
            val th = inputSystem.getTh(element.primaryLoad, element.inputSink)
            Thevenin(th.voltage, th.resistance)
        }
        val outputTh = if (element.isolated) {
            getBipoleTh(outputSystem, element.secondaryLoad, element.secondaryReferenceLoad, element.outputSource)
        } else {
            val th = outputSystem.getTh(element.secondaryLoad, element.outputSource)
            Thevenin(th.voltage, th.resistance)
        }

        if (!inputTh.voltage.isFinite() || !outputTh.voltage.isFinite()) {
            idleSources()
            return
        }
        if (inputTh.resistance >= MnaConst.highImpedance * 0.1) {
            idleSources(0.0, outputTh.voltage)
            return
        }
        if (inputTh.voltage <= 0.0) {
            idleSources()
            return
        }

        val transfer = OneWayDcDcMath.solve(
            inputTh = inputTh,
            outputTh = outputTh,
            ratio = element.activeRatio,
            maxOutputVoltage = 120_000.0
        )

        if (transfer == null) {
            val targetOutputVoltage = Utils.limit(inputTh.voltage * element.activeRatio, 0.0, 120_000.0)
            val idleOutputVoltage = if (outputTh.resistance >= MnaConst.highImpedance * 0.1) {
                min(outputTh.voltage, targetOutputVoltage)
            } else {
                outputTh.voltage
            }
            idleSources(inputTh.voltage, idleOutputVoltage)
            return
        }

        element.inputSink.setVoltage(transfer.inputSourceVoltage)
        element.outputSource.setVoltage(transfer.outputSourceVoltage)
        element.movedPower = transfer.power
    }

    private fun idleSources(
        inputVoltage: Double = differentialVoltage(element.primaryLoad, element.primaryReferenceLoad),
        outputVoltage: Double = differentialVoltage(element.secondaryLoad, element.secondaryReferenceLoad)
    ) {
        element.inputSink.setVoltage(inputVoltage)
        element.outputSource.setVoltage(outputVoltage)
    }

    private fun idleSourcesFromThevenin() {
        val inputVoltage = element.primaryLoad.subSystem?.let { system ->
            if (element.isolated) {
                getBipoleTh(system, element.primaryLoad, element.primaryReferenceLoad, element.inputSink).voltage
            } else {
                val th = system.getTh(element.primaryLoad, element.inputSink)
                th.voltage
            }
        }?.takeIf { it.isFinite() } ?: 0.0

        val outputVoltage = element.secondaryLoad.subSystem?.let { system ->
            if (element.isolated) {
                getBipoleTh(system, element.secondaryLoad, element.secondaryReferenceLoad, element.outputSource).voltage
            } else {
                val th = system.getTh(element.secondaryLoad, element.outputSource)
                th.voltage
            }
        }?.takeIf { it.isFinite() } ?: 0.0

        idleSources(inputVoltage, outputVoltage)
    }

    private fun differentialVoltage(positive: State, negative: State): Double {
        return if (element.isolated) positive.state - negative.state else positive.state
    }

    private data class Thevenin(
        override val voltage: Double,
        override val resistance: Double
    ) : OneWayDcDcThevenin

    private fun getBipoleTh(system: SubSystem, positive: State, negative: State, source: VoltageSource): Thevenin {
        val originalVoltage = positive.state - negative.state
        val testVoltage = originalVoltage + 5.0
        source.setVoltage(testVoltage)
        val testCurrent = system.solve(source.currentState)
        source.setVoltage(originalVoltage)
        val originalCurrent = system.solve(source.currentState)

        var resistance = (testVoltage - originalVoltage) / (originalCurrent - testCurrent)
        var voltage = if (resistance > 1.0e19 || resistance < 0.0 || resistance.isNaN()) {
            resistance = 1.0e20
            originalVoltage
        } else {
            testVoltage + resistance * testCurrent
        }

        source.setVoltage(originalVoltage)

        if (!voltage.isFinite()) voltage = originalVoltage
        if (!resistance.isFinite()) resistance = 1.0e20
        return Thevenin(voltage, resistance)
    }
}

internal interface OneWayDcDcThevenin {
    val voltage: Double
    val resistance: Double
}

internal data class OneWayDcDcTransfer(
    val inputSourceVoltage: Double,
    val outputSourceVoltage: Double,
    val power: Double
)

internal object OneWayDcDcMath {
    fun solve(
        inputTh: OneWayDcDcThevenin,
        outputTh: OneWayDcDcThevenin,
        ratio: Double,
        maxOutputVoltage: Double
    ): OneWayDcDcTransfer? {
        if (!inputTh.voltage.isFinite() || !outputTh.voltage.isFinite()) return null
        if (!inputTh.resistance.isFinite() || !outputTh.resistance.isFinite()) return null
        if (inputTh.voltage <= 0.0 || ratio <= 0.0) return null

        val targetOutputVoltage = Utils.limit(inputTh.voltage * ratio, 0.0, maxOutputVoltage)
        if (targetOutputVoltage <= outputTh.voltage || targetOutputVoltage <= 0.0) return null

        val outputResistance = outputTh.resistance.coerceAtLeast(0.0)
        if (outputResistance <= 0.0) return null
        val demandedOutputCurrent = ((targetOutputVoltage - outputTh.voltage) / outputResistance).coerceAtLeast(0.0)

        var outputPower = targetOutputVoltage * demandedOutputCurrent
        val inputResistance = inputTh.resistance.coerceAtLeast(0.0)
        val inputMaxPower = if (inputResistance <= 0.0) {
            Double.POSITIVE_INFINITY
        } else {
            inputTh.voltage * inputTh.voltage / (4.0 * inputResistance)
        }
        outputPower = outputPower.coerceAtMost(inputMaxPower).coerceAtLeast(0.0)
        if (outputPower <= 0.0) return null

        val outputSourceVoltage = sourceVoltageForPower(
            theveninVoltage = outputTh.voltage,
            resistance = outputResistance,
            power = outputPower,
            maxVoltage = targetOutputVoltage
        )
        val actualOutputPower = outputPowerAtSource(outputTh.voltage, outputResistance, outputSourceVoltage)
            .coerceAtMost(inputMaxPower)
            .coerceAtLeast(0.0)
        if (actualOutputPower <= 0.0) return null

        val inputSourceVoltage = sinkVoltageForPower(
            theveninVoltage = inputTh.voltage,
            resistance = inputResistance,
            power = actualOutputPower
        )

        return OneWayDcDcTransfer(inputSourceVoltage, outputSourceVoltage, actualOutputPower)
    }

    private fun outputPowerAtSource(theveninVoltage: Double, resistance: Double, sourceVoltage: Double): Double {
        val current = if (resistance <= 0.0) {
            return 0.0
        } else {
            ((sourceVoltage - theveninVoltage) / resistance).coerceAtLeast(0.0)
        }
        return sourceVoltage * current
    }

    private fun sourceVoltageForPower(theveninVoltage: Double, resistance: Double, power: Double, maxVoltage: Double): Double {
        if (power <= 0.0) return theveninVoltage
        if (resistance <= 0.0) return min(maxVoltage, theveninVoltage).coerceAtLeast(theveninVoltage)
        val voltage = (sqrt(theveninVoltage * theveninVoltage + 4.0 * power * resistance) + theveninVoltage) / 2.0
        return min(maxVoltage, voltage).coerceAtLeast(theveninVoltage)
    }

    private fun sinkVoltageForPower(theveninVoltage: Double, resistance: Double, power: Double): Double {
        if (power <= 0.0) return theveninVoltage
        if (resistance <= 0.0) return theveninVoltage
        val clampedPower = min(power, theveninVoltage * theveninVoltage / (4.0 * resistance))
        val discriminant = (theveninVoltage * theveninVoltage - 4.0 * clampedPower * resistance).coerceAtLeast(0.0)
        val voltageByPower = (theveninVoltage + sqrt(discriminant)) / 2.0
        return max(0.0, voltageByPower)
    }
}

class OneWayDcDcRender(
    tileEntity: TransparentNodeEntity,
    private val descriptor: TransparentNodeDescriptor
) : TransparentNodeElementRender(tileEntity, descriptor) {
    override val inventory = TransparentNodeElementInventory(4, 64, this)

    private val oneWayDescriptor = descriptor as OneWayDcDcDescriptor
    private val load = SlewLimiter(0.5f)
    private var primaryStackSize: Byte = 0
    private var secondaryStackSize: Byte = 0
    private var priRender: CableRenderDescriptor? = null
    private var secRender: CableRenderDescriptor? = null
    private var feroPart: Obj3D.Obj3DPart? = null
    private var hasCasing = false
    private val coordinate = Coordinate(tileEntity)
    private val doorOpen = PhysicalInterpolator(0.4f, 4.0f, 0.9f, 0.05f)
    private val priConn = LRDUMask()
    private val secConn = LRDUMask()
    private val priRefConn = LRDUMask()
    private val secRefConn = LRDUMask()
    private val controlConn = LRDUMask()
    private val eConn = LRDUMask()
    private var cableRenderType: CableRenderType? = null

    init {
        addLoopedSound(object : LoopedSound("eln:Transformer", coordinate(), ISound.AttenuationType.LINEAR) {
            override fun getVolume(): Float {
                return if (load.position > oneWayDescriptor.minimalLoadToHum)
                    0.1f * (load.position - oneWayDescriptor.minimalLoadToHum) / (1 - oneWayDescriptor.minimalLoadToHum)
                else
                    0f
            }
        })
    }

    override fun draw() {
        GL11.glPushMatrix()
        front!!.glRotateXnRef()
        oneWayDescriptor.draw(feroPart, primaryStackSize.toInt(), secondaryStackSize.toInt(), hasCasing, doorOpen.get())
        GL11.glPopMatrix()
        cableRenderType = drawCable(front!!.down(), primaryRender(), priConn, cableRenderType)
        cableRenderType = drawCable(front!!.down(), secondaryRender(), secConn, cableRenderType)
        if (oneWayDescriptor.isolated) {
            cableRenderType = drawCable(front!!.down(), primaryReferenceRender(), priRefConn, cableRenderType)
            cableRenderType = drawCable(front!!.down(), secondaryReferenceRender(), secRefConn, cableRenderType)
        }
        if (oneWayDescriptor.variable) {
            cableRenderType = drawCable(front!!.down(), Eln.instance.stdCableRenderSignal, controlConn, cableRenderType)
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            primaryStackSize = stream.readByte()
            secondaryStackSize = stream.readByte()
            val feroStack = Utils.unserialiseItemStack(stream)
            feroPart = null
            if (feroStack != null) {
                val feroDesc = GenericItemUsingDamageDescriptor.getDescriptor(feroStack, FerromagneticCoreDescriptor::class.java)
                if (feroDesc != null) feroPart = (feroDesc as FerromagneticCoreDescriptor).feroPart
            }
            val priStack = Utils.unserialiseItemStack(stream)
            priRender = null
            if (priStack != null) {
                val priDesc: GenericItemBlockUsingDamageDescriptor? = ElectricalCableDescriptor.getDescriptor(priStack, ElectricalCableDescriptor::class.java)
                if (priDesc != null) priRender = (priDesc as ElectricalCableDescriptor).render
            }
            val secStack = Utils.unserialiseItemStack(stream)
            secRender = null
            if (secStack != null) {
                val secDesc: GenericItemBlockUsingDamageDescriptor? = ElectricalCableDescriptor.getDescriptor(secStack, ElectricalCableDescriptor::class.java)
                if (secDesc != null) secRender = (secDesc as ElectricalCableDescriptor).render
            }

            eConn.deserialize(stream)
            priConn.mask = 0
            secConn.mask = 0
            priRefConn.mask = 0
            secRefConn.mask = 0
            controlConn.mask = 0
            for (lrdu in LRDU.values()) {
                if (!eConn.get(lrdu)) continue
                when (front!!.down().applyLRDU(lrdu)) {
                    front!!.left() -> priConn.set(lrdu, true)
                    front!!.right() -> secConn.set(lrdu, true)
                    front -> if (oneWayDescriptor.isolated) priRefConn.set(lrdu, true) else controlConn.set(lrdu, true)
                    front!!.back() -> if (oneWayDescriptor.isolated) secRefConn.set(lrdu, true) else controlConn.set(lrdu, true)
                    else -> controlConn.set(lrdu, true)
                }
            }
            cableRenderType = null
            load.target = stream.readFloat()
            hasCasing = stream.readBoolean()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getCableRenderSide(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        if (lrdu == LRDU.Down) {
            if (side == front!!.left()) return primaryRender()
            if (side == front!!.right()) return secondaryRender()
            if (side == front && oneWayDescriptor.isolated) return primaryReferenceRender()
            if (side == front!!.back() && oneWayDescriptor.isolated) return secondaryReferenceRender()
            if (side == front && oneWayDescriptor.variable && !grounded) return Eln.instance.stdCableRenderSignal
            if (side == front!!.back() && oneWayDescriptor.variable && !grounded) return Eln.instance.stdCableRenderSignal
        }
        return null
    }

    private fun primaryRender(): CableRenderDescriptor? {
        return resolveAdjacentCableRender(front!!.left())
    }

    private fun secondaryRender(): CableRenderDescriptor? {
        return resolveAdjacentCableRender(front!!.right())
    }

    private fun primaryReferenceRender(): CableRenderDescriptor? {
        return resolveAdjacentCableRender(front!!)
    }

    private fun secondaryReferenceRender(): CableRenderDescriptor? {
        return resolveAdjacentCableRender(front!!.back())
    }

    private fun resolveAdjacentCableRender(side: Direction): CableRenderDescriptor? {
        val neighborCoordinate = Coordinate(tileEntity).moved(side)
        if (!neighborCoordinate.blockExist) return null
        val neighbor = neighborCoordinate.world().getTileEntity(
            neighborCoordinate.x,
            neighborCoordinate.y,
            neighborCoordinate.z
        )

        if (neighbor is SixNodeEntity) {
            val neighborSide = side.inverse
            val elementRender = neighbor.elementRenderList[neighborSide.int] ?: return null
            for (neighborLrdu in LRDU.values()) {
                val render = elementRender.getCableRender(neighborLrdu)
                if (render != null) return render
            }
        }

        return null
    }

    override fun notifyNeighborSpawn() {
        super.notifyNeighborSpawn()
        cableRenderType = null
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        load.step(deltaT)
        if (hasCasing) {
            doorOpen.target = if (!Utils.isPlayerAround(tileEntity.worldObj, coordinate.moved(front!!).getAxisAlignedBB(0))) 0f else 1f
            doorOpen.step(deltaT)
        }
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return OneWayDcDcGui(player, inventory, this, oneWayDescriptor.variable)
    }
}

class OneWayDcDcGui(
    player: EntityPlayer,
    inventory: IInventory,
    render: OneWayDcDcRender,
    variable: Boolean
) : GuiContainerEln(OneWayDcDcContainer(player, inventory, variable)) {
    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 194 - 33 + 20, 8, 84 + 194 - 166 - 33 + 20, "transformer.png")
    }
}

class OneWayDcDcContainer(player: EntityPlayer, inventory: IInventory, variable: Boolean) : BasicContainer(
    player,
    inventory,
    arrayOf(
        DcDcWindingSlot(
            inventory, primaryCableSlotId, 58, 30, 16,
            arrayOf(tr("Power cable or wire slot"))
        ),
        DcDcWindingSlot(
            inventory, secondaryCableSlotId, 100, 30, 16,
            arrayOf(tr("Power cable or wire slot"))
        ),
        GenericItemUsingDamageSlot(
            inventory, ferromagneticSlotId, 58 + (100 - 58) / 2, 30, 1,
            arrayOf<Class<*>>(FerromagneticCoreDescriptor::class.java),
            ISlotSkin.SlotSkin.medium, arrayOf(tr("Ferromagnetic core slot"))
        ),
        GenericItemUsingDamageSlot(
            inventory, CasingSlotId, 130, 74, 1,
            arrayOf<Class<*>>(CaseItemDescriptor::class.java),
            ISlotSkin.SlotSkin.medium, arrayOf(tr("Casing slot"))
        )
    )
) {
    companion object {
        const val primaryCableSlotId = 0
        const val secondaryCableSlotId = 1
        const val ferromagneticSlotId = 2
        const val CasingSlotId = 3
    }
}
