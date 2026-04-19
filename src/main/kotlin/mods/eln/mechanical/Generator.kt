package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ResistorHeatThermalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.DataInputStream
import java.io.DataOutputStream

private const val MACHINE_INTERNAL_RESISTANCE_FACTOR = 0.1
private const val GENERATOR_COOLING_HEADROOM = 4.0


class GeneratorDescriptor(
    name: String,
    obj: Obj3D,
    cable: ElectricalCableDescriptor,
    nominalRads: Float,
    nominalU: Float,
    powerOutPerDeltaU: Float,
    nominalP: Float,
    thermalLoadInitializer: ThermalLoadInitializer,
    val bipolarTerminals: Boolean = false) :
    SimpleShaftDescriptor(name, GeneratorElement::class, GeneratorRender::class, EntityMetaTag.Basic) {

    val RtoU = LinearFunction(0f, 0f, nominalRads, nominalU)
    val cable = cable
    val thermalLoadInitializer = thermalLoadInitializer
    val powerOutPerDeltaU = powerOutPerDeltaU
    val nominalRads = nominalRads
    val nominalP = nominalP
    val nominalU = nominalU
    val generationEfficiency = 0.95
    val nominalCurrent = nominalP / nominalU
    val regulatorRampTime = 0.75
    val regulatorCurrentLimit = nominalCurrent * 1.5
    val regulatorDroopResistance = nominalU * 0.05 / nominalCurrent
    val regulatorVoltageFilterTime = 0.15
    override val sound = "eln:generator"

    init {
        // Generators should shed heat to ambient much better than the generic
        // six-node thermal profile. Increase the modeled cooling capacity
        // without changing the actual operating temperature limit.
        thermalLoadInitializer.setMaximalPower(
            nominalP.toDouble() * (1 - generationEfficiency) * GENERATOR_COOLING_HEADROOM
        )

        voltageLevelColor = VoltageLevelColor.VeryHighVoltage
    }

    override val obj = obj
    override val static = arrayOf(
        obj.getPart("Cowl"),
        obj.getPart("Stand")
    ).requireNoNulls()
    override val rotating = arrayOf(obj.getPart("Shaft")).requireNoNulls()
    val powerLights = arrayOf(
        obj.getPart("LED_0"),
        obj.getPart("LED_1"),
        obj.getPart("LED_2"),
        obj.getPart("LED_3"),
        obj.getPart("LED_4"),
        obj.getPart("LED_5"),
        obj.getPart("LED_6")
    ).requireNoNulls()

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Converts mechanical energy into electricity, or (badly) vice versa."))
        list.add(tr("Integrated regulator: ramps field and limits output current."))
        list.add(tr("Nominal usage ->"))
        list.add(Utils.plotVolt(tr("  Voltage out: "), nominalU.toDouble()))
        list.add(Utils.plotPower(tr("  Power out: "), nominalP.toDouble()))
        list.add(Utils.plotRads(tr("  Rads: "), nominalRads.toDouble()))
        list.add(Utils.plotAmpere(tr("Regulator current limit: "), regulatorCurrentLimit.toDouble()))
        list.add(Utils.plotRads(tr("Max rads:  "), absoluteMaximumShaftSpeed))
    }
}

class GeneratorRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor) : ShaftRender(entity, desc_) {
    val entity = entity

    val desc = desc_ as GeneratorDescriptor
    override val cableRender get() = desc.cable.render

    val ledColors: Array<Color> = arrayOf(
        java.awt.Color.black,
        java.awt.Color.black,
        java.awt.Color.black,
        java.awt.Color.black,
        java.awt.Color.black,
        java.awt.Color.black,
        java.awt.Color.black
    )
    val ledColorBase: Array<HSLColor> = arrayOf(
        GREEN,
        GREEN,
        GREEN,
        GREEN,
        YELLOW,
        RED,
        RED
    )

    fun calcPower(power: Double) {
        if (power < 0) {
            for (i in 1..6) {
                ledColors[i] = Color.black
            }
            ledColors[0] = RED.adjustLuminanceClamped((-power / desc.nominalP * 4 * 100).toFloat(), 0f, 60f)
        } else {
            val slice = desc.nominalP / 5
            var remainder = power
            for (i in 0..6) {
                ledColors[i] = ledColorBase[i].adjustLuminanceClamped((remainder / slice * 100).toFloat(), 0f, 65f)
                remainder -= slice
            }
        }
    }

    override fun draw() {
        draw {
            ledColors.forEachIndexed { i, color ->
                GL11.glColor3f(
                    color.red / 255f,
                    color.green / 255f,
                    color.blue / 255f
                )
                desc.powerLights[i].draw()
            }
        }
    }

    override fun getCableRenderSide(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        val f = front ?: return null
        if (lrdu == LRDU.Down && (side == f || (desc.bipolarTerminals && side == f.back()))) {
            return desc.cable.render
        }
        return null
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        val power = stream.readDouble()
        calcPower(power)
        volumeSetting.target = 0.05f + Math.abs(power / desc.nominalP).toFloat() / 4f
    }
}

class GeneratorElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    SimpleShaftElement(node, desc_) {
    val desc = desc_ as GeneratorDescriptor
    private var regulatorVoltageTarget = 0.0
    private var regulatorCurrentLimitRequest = 0.0
    private var regulatorFilteredOutputVoltage = 0.0

    internal val inputLoad = NbtElectricalLoad("inputLoad")
    internal val negativeLoad = NbtElectricalLoad("negativeLoad")
    internal val positiveLoad = NbtElectricalLoad("positiveLoad")
    internal val inputToPositiveResistor = Resistor(inputLoad, positiveLoad)
    internal val electricalPowerSource = VoltageSource("PowerSource", positiveLoad, if (desc.bipolarTerminals) negativeLoad else null)
    internal val electricalProcess = GeneratorElectricalProcess()
    internal val shaftProcess = GeneratorShaftProcess()

    internal val thermal = NbtThermalLoad("thermal")
    internal val heater: IProcess
    internal val thermalLoadWatchDog = ambientAwareThermalWatchdog(ThermalLoadWatchDog(thermal))

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(inputLoad)
        if (desc.bipolarTerminals) {
            electricalLoadList.add(negativeLoad)
            desc.cable.applyTo(negativeLoad)
        }
        electricalComponentList.add(electricalPowerSource)
        electricalComponentList.add(inputToPositiveResistor)

        electricalProcessList.add(shaftProcess)
        desc.cable.applyTo(inputLoad)
        desc.cable.applyTo(inputToPositiveResistor, MACHINE_INTERNAL_RESISTANCE_FACTOR)
        desc.cable.applyTo(positiveLoad)

        desc.thermalLoadInitializer.applyTo(thermal)
        desc.thermalLoadInitializer.applyTo(thermalLoadWatchDog)
        thermal.setAsSlow()
        thermalLoadList.add(thermal)
        thermalLoadWatchDog.setDestroys(WorldExplosion(this as ShaftElement).machineExplosion())
        slowProcessList.add(thermalLoadWatchDog)

        heater = ResistorHeatThermalLoad(inputToPositiveResistor, thermal)
        thermalFastProcessList.add(heater)

        // TODO: Add running lights. (More. Electrical sparks, perhaps?)
        // TODO: Add the thermal explosions—there should be some.
    }

    inner class GeneratorElectricalProcess : IProcess, IRootSystemPreStepProcess {
        override fun process(time: Double) {
            val targetU = desc.RtoU.getValue(shaft.rads).coerceAtLeast(0.0)
            val dt = if (time > 0.0) time else Eln.simulator.electricalPeriod
            val th = positiveLoad.getSubSystem().getTh(positiveLoad, electricalPowerSource)
            val rampRate = desc.nominalU / desc.regulatorRampTime
            regulatorVoltageTarget = when {
                regulatorVoltageTarget < targetU ->
                    (regulatorVoltageTarget + rampRate * dt).coerceAtMost(targetU)
                else ->
                    (regulatorVoltageTarget - rampRate * dt).coerceAtLeast(targetU)
            }

            val droopedTarget = (regulatorVoltageTarget -
                Math.abs(electricalPowerSource.current) * desc.regulatorDroopResistance).coerceAtLeast(0.0)

            val commandedVoltage = if (th.isHighImpedance()) {
                regulatorCurrentLimitRequest = 0.0
                droopedTarget
            } else {
                regulatorCurrentLimitRequest = desc.regulatorCurrentLimit.toDouble()
                val currentLimitedVoltage = th.voltage + regulatorCurrentLimitRequest * th.resistance
                minOf(droopedTarget, currentLimitedVoltage)
            }
            regulatorFilteredOutputVoltage = if (regulatorFilteredOutputVoltage <= 0.0) {
                commandedVoltage
            } else {
                val alpha = (dt / desc.regulatorVoltageFilterTime).coerceIn(0.0, 1.0)
                regulatorFilteredOutputVoltage + (commandedVoltage - regulatorFilteredOutputVoltage) * alpha
            }
            electricalPowerSource.setVoltage(regulatorFilteredOutputVoltage)
        }

        override fun rootSystemPreStepProcess() {
            process(0.0)
        }
    }

    inner class GeneratorShaftProcess() : IProcess {
        private var powerFraction = 0.0f

        override fun process(time: Double) {
            val electricalPower = electricalPowerSource.power
            powerFraction = (electricalPower / desc.nominalP).toFloat()
            maybePublishE(electricalPower)

            val dragPower = defaultDrag * Math.max(shaft.rads, 1.0)
            val shaftPower = if (electricalPower >= 0.0) {
                electricalPower / desc.generationEfficiency
            } else {
                electricalPower * desc.generationEfficiency
            }
            val conversionLossPower = if (electricalPower >= 0.0) {
                electricalPower * (1.0 / desc.generationEfficiency - 1.0)
            } else {
                -electricalPower * (1.0 - desc.generationEfficiency)
            }

            shaft.energy -= (shaftPower + dragPower) * time
            thermal.movePowerTo(conversionLossPower + dragPower)
        }
    }

    var lastE = 0.0
    fun maybePublishE(E: Double) {
        if (Math.abs(E - lastE) / desc.nominalP > 0.01) {
            lastE = E
            needPublish()
        }
    }

    override fun connectJob() {
        super.connectJob()
        Eln.simulator.mna.addProcess(electricalProcess)
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.mna.removeProcess(electricalProcess)
    }


    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null;
        return when (side) {
            front -> inputLoad
            front.back() -> if (desc.bipolarTerminals) negativeLoad else inputLoad
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU) = thermal

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu == LRDU.Down && (side == front || side == front.back())) return NodeBase.maskElectricalPower
        return 0
    }

    override fun multiMeterString(side: Direction) =
        Utils.plotER(shaft.energy, shaft.rads) + Utils.plotUIP(electricalPowerSource.getVoltage(), electricalPowerSource.getCurrent())

    override fun thermoMeterString(side: Direction): String = plotAmbientCelsius("T", thermal.getTemperature())

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(lastE)
    }

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put(tr("Energy"), Utils.plotEnergy("", shaft.energy))
        info.put(tr("Speed"), Utils.plotRads("", shaft.rads))
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info.put(tr("Voltage"), Utils.plotVolt("", electricalPowerSource.getVoltage()))
            info.put(tr("Current"), Utils.plotAmpere("", electricalPowerSource.getCurrent()))
            info.put(tr("Regulator target"), Utils.plotVolt("", regulatorVoltageTarget))
            info.put(tr("Regulator current limit"), Utils.plotAmpere("", regulatorCurrentLimitRequest))
            info.put(tr("Temperature"), plotAmbientCelsius("", thermal.temperature))
        }
        return info
    }

    override fun coordonate(): Coordinate {
        return node!!.coordinate
    }
}
