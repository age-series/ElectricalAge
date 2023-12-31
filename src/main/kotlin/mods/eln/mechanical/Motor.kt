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
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sound.LoopedSound
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.DataInputStream
import java.io.DataOutputStream

class MotorDescriptor(
    val name: String,
    obj: Obj3D,
    cable: ElectricalCableDescriptor,
    nominalRads: Float,
    nominalU: Float,
    nominalP: Float,
    elecPPerDU: Float,
    shaftPPerDU: Float,
    thermalLoadInitializer: ThermalLoadInitializer
) : SimpleShaftDescriptor(
    name,
    MotorElement::class,
    MotorRender::class,
    EntityMetaTag.Basic
) {
    val cable = cable
    val nominalRads = nominalRads
    val nominalU = nominalU
    val nominalP = nominalP
    val maxP = 32000f  // TODO (Grissess): Calculate?
    val elecPPerDU = elecPPerDU
    val shaftPPerDU = shaftPPerDU
    val thermalLoadInitializer = thermalLoadInitializer

    val radsToU = LinearFunction(0f, 0f, nominalRads, nominalU)

    val customSound = "eln:shaft_motor"
    val efficiency = 0.99

    override val obj = obj
    override val static = arrayOf(
        obj.getPart("Cowl"),
        obj.getPart("Stand")
    ).requireNoNulls()
    override val rotating = arrayOf(obj.getPart("Shaft")).requireNoNulls()
    val leds = arrayOf(
        obj.getPart("LED_0"),
        obj.getPart("LED_1"),
        obj.getPart("LED_2"),
        obj.getPart("LED_3"),
        obj.getPart("LED_4"),
        obj.getPart("LED_5"),
        obj.getPart("LED_6")
    ).requireNoNulls()

    init {
        thermalLoadInitializer.setMaximalPower(nominalP.toDouble())
        voltageLevelColor = VoltageLevelColor.VeryHighVoltage
    }

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Converts electricity into mechanical energy, or (badly) vice versa."))
        list.add(tr("Nominal usage ->"))
        list.add(Utils.plotVolt(tr("  Voltage in: "), nominalU.toDouble()))
        list.add(Utils.plotPower(tr("  Power in: "), nominalP.toDouble()))
        list.add(Utils.plotRads(tr("  rad/s: "), nominalRads.toDouble()))
        list.add(Utils.plotRads(tr("Max rad/s: "), absoluteMaximumShaftSpeed))
    }
}

class MotorRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor) : ShaftRender(entity, desc_) {
    val entity = entity

    override val cableRender = Eln.instance.stdCableRender3200V
    val desc = desc_ as MotorDescriptor

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

    inner class MotorLoopedSound(sound: String, coord: Coordinate) :
        LoopedSound(sound, coord) {
        override fun getPitch() = Math.max(0.05, rads * 2 / desc.nominalRads).toFloat()
        override fun getVolume() = volumeSetting.position
    }

    init {
        addLoopedSound(MotorLoopedSound(desc.customSound, coordinate()))
        mask.set(LRDU.Down, true)
    }

    fun setPower(power: Double) {
        if(power < 0) {
            for(i in 1..6) ledColors[i] = Color.black
            ledColors[0] = RED.adjustLuminanceClamped((-power / desc.nominalP * 400).toFloat(), 0f, 60f)
        } else {
            val slice = desc.maxP / 5
            var current = power
            for(i in 0..6) {
                ledColors[i] = ledColorBase[i].adjustLuminanceClamped((current / slice * 100).toFloat(), 0f, 65f)
                current -= slice
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
                desc.leds[i].draw()
            }
        }
    }

    override fun getCableRenderSide(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        if(lrdu == LRDU.Down && side == front) return Eln.instance.stdCableRender3200V
        return null
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        val power = stream.readDouble()

        setPower(power)
        volumeSetting.target = Math.min(1.0f, Math.abs(power / desc.maxP).toFloat()) / 4f
    }
}

class MotorElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    SimpleShaftElement(node, desc_) {
    val desc = desc_ as MotorDescriptor

    internal val wireLoad = NbtElectricalLoad("wireLoad")
    internal val shaftLoad = NbtElectricalLoad("shaftLoad")
    internal val wireShaftResistor = Resistor(wireLoad, shaftLoad)
    internal val powerSource = VoltageSource("powerSource", shaftLoad, null)

    internal val electricalProcess = MotorElectricalProcess()
    internal val shaftProcess = MotorShaftProcess()

    internal val thermal = NbtThermalLoad("thermal")
    internal val heater: ElectricalLoadHeatThermalLoad
    internal val thermalWatchdog = ThermalLoadWatchDog(thermal)

    init {
        electricalLoadList.addAll(arrayOf(wireLoad, shaftLoad))
        electricalComponentList.addAll(arrayOf(wireShaftResistor, powerSource))

        electricalProcessList.add(shaftProcess)

        desc.cable.applyTo(wireLoad)
        desc.cable.applyTo(shaftLoad)
        desc.cable.applyTo(wireShaftResistor)

        desc.thermalLoadInitializer.applyTo(thermal)
        desc.thermalLoadInitializer.applyTo(thermalWatchdog)
        thermal.setAsSlow()
        thermalLoadList.add(thermal)
        thermalWatchdog.setDestroys(WorldExplosion(this as ShaftElement).machineExplosion())
        slowProcessList.add(thermalWatchdog)

        heater = ElectricalLoadHeatThermalLoad(wireLoad, thermal)
        thermalFastProcessList.add(heater)
    }

    inner class MotorElectricalProcess : IProcess, IRootSystemPreStepProcess {
        override fun process(time: Double) {
            val noTorqueU = desc.radsToU.getValue(shaft.rads)

            // Most of this was copied from Generator.kt, and bears the same
            // admonition: I don't actually know how this works.
            val th = wireLoad.subSystem.getTh(wireLoad, powerSource)
            if (th.voltage.isNaN()) {
                th.voltage = noTorqueU
                th.resistance = MnaConst.highImpedance
            }
            var U: Double
            if(noTorqueU < th.voltage) {
                // Input is greater than our output, spin up the shaft
                U = th.voltage * 0.997 + noTorqueU * 0.003
            } else if(th.isHighImpedance()) {
                // No actual connection, let the system float
                U = noTorqueU
            } else {
                // Provide an output voltage by
                // solving a quadratic, I guess?
                val a = 1 / th.resistance
                val b = desc.elecPPerDU - th.voltage / th.resistance
                val c = -desc.elecPPerDU * noTorqueU
                U = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a)
            }
            powerSource.setVoltage(U)
        }

        override fun rootSystemPreStepProcess() {
            process(0.0)
        }
    }

    inner class MotorShaftProcess : IProcess {
        override fun process(time: Double) {
            val p = powerSource.power
            var E = -p * time
            if (E.isNaN())
                E = 0.0
            if(E < 0) {
                // Pushing power--this is very inefficient
                E = E * 10.0
            }
            maybePublishP(E / time)
            E = E - defaultDrag * Math.max(shaft.rads, 1.0)
            shaft.energy += E * desc.efficiency
            thermal.movePowerTo(E * (1 - desc.efficiency))
        }
    }

    var lastP = 0.0
    fun maybePublishP(P: Double) {
        if(Math.abs(P - lastP) / desc.nominalP > 0.01) {
            lastP = P
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
        if(lrdu != LRDU.Down) return null;
        return when(side) {
            front -> wireLoad
            front.back() -> wireLoad
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU) = thermal

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if(lrdu == LRDU.Down && (side == front || side == front.back())) return NodeBase.maskElectricalPower
        return 0
    }

    override fun multiMeterString(side: Direction) =
        Utils.plotER(shaft.energy, shaft.rads) +
            Utils.plotUIP(powerSource.voltage, powerSource.current)

    override fun thermoMeterString(side: Direction): String = Utils.plotCelsius("T", thermal.temperature)

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float) =
        false

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(lastP)
    }

    override fun getWaila(): MutableMap<String, String> {
        var info = mutableMapOf<String, String>()
        info.put(tr("Energy"), Utils.plotEnergy("", shaft.energy))
        info.put(tr("Speed"), Utils.plotRads("", shaft.rads))
        if(Eln.wailaEasyMode) {
            info.put(tr("Voltage"), Utils.plotVolt("", powerSource.voltage))
            info.put(tr("Current"), Utils.plotAmpere("", powerSource.current))
            info.put(tr("Temperature"), Utils.plotCelsius("", thermal.temperature))
        }
        return info
    }

    override fun coordonate(): Coordinate {
        return node!!.coordinate
    }
}
