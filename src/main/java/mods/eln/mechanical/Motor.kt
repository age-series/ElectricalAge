package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.debug.DP
import mods.eln.debug.DPType
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
import mods.eln.sixnode.genericcable.GenericCableDescriptor
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
    cable: GenericCableDescriptor,
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
        list.add("Converts electricity into")
        list.add("mechanical energy, or (badly) vice versa.")
        list.add("Nominal usage ->")
        list.add(Utils.plotVolt("  Voltage in: ", nominalU.toDouble()))
        list.add(Utils.plotPower("  Power in: ", nominalP.toDouble()))
        list.add(Utils.plotRads("  rad/s: ", nominalRads.toDouble()))
        list.add(Utils.plotRads("Max rad/s: ", absoluteMaximumShaftSpeed))
    }
}

class MotorRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor) : ShaftRender(entity, desc_) {
    val entity = entity

    override val cableRender = Eln.stdCableRender3200V
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

    inner class MotorLoopedSound(sound: String, coord: Coordonate) :
        LoopedSound(sound, coord) {
        override fun getPitch() = Math.max(0.05, rads / desc.nominalRads).toFloat()
        override fun getVolume() = volumeSetting.position
    }

    init {
        addLoopedSound(MotorLoopedSound(desc.customSound, coordonate()))
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

    override fun getCableRender(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        if(lrdu == LRDU.Down && side == front) return Eln.stdCableRender3200V
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
    internal val thermalWatchdog = ThermalLoadWatchDog()

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
        thermalWatchdog.set(thermal).set(WorldExplosion(this).machineExplosion())
        //slowProcessList.add(thermalWatchdog)

        heater = ElectricalLoadHeatThermalLoad(wireLoad, thermal)
        thermalFastProcessList.add(heater)
    }

    inner class MotorElectricalProcess : IProcess, IRootSystemPreStepProcess {

        var last = -1;

        override fun process(time: Double) {
            val noTorqueU = desc.radsToU.getValue(shaft.rads)

            // Most of this was copied from Generator.kt, and bears the same
            // admonition: I don't actually know how this works.
            val th = wireLoad.subSystem!!.getTh(wireLoad, powerSource)
            if (th.U.isNaN()) {
                th.U = noTorqueU
                th.R = MnaConst.highImpedance
            }
            var U: Double
            if(noTorqueU < th.U) {
                if (last != 0)
                    DP.println(DPType.MECHANICAL, "ntU < thU")
                // Input is greater than our output, spin up the shaft
                U = th.U * 0.9999 + noTorqueU * 0.0001
                last = 0
            } else if(th.isHighImpedance) {
                if (last != 1)
                    DP.println(DPType.MECHANICAL, "thR high imp")
                // No actual connection, let the system float
                U = noTorqueU
                last = 1
            } else {
                if (last != 2)
                    DP.println(DPType.MECHANICAL, "ntU > thU")
                // Provide an output voltage by
                // solving a quadratic, I guess?
                val a = 1 / th.R
                val b = desc.elecPPerDU - th.U / th.R
                val c = -desc.elecPPerDU * noTorqueU
                U = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a)
                last = 2
            }
            //DP.println(DPType.MECHANICAL, "Voltage: " + U)
            powerSource.u = U
            //DP.println(DPType.MECHANICAL, "Ohms: " + th.R)
            //DP.println(DPType.MECHANICAL, "Current: " + powerSource.currentState.state)

        }

        override fun rootSystemPreStepProcess() {
            process(0.0)
        }
    }

    inner class MotorShaftProcess : IProcess {
        override fun process(time: Double) {
            val p = powerSource.getPower()
            var E = -p * time
            if (E.isNaN()) {
                E = 0.0
            }
            if(E < 0) {
                // Pushing power--this is very inefficient
                E = E * 10.0
            }
            maybePublishP(E / time)
            E = E - defaultDrag * Math.max(shaft.rads, 10.0)
            shaft.energy += E * desc.efficiency
            val tPower = E * (1 - desc.efficiency)
            //DP.println(DPType.MECHANICAL, "thermal power: " + tPower)
            //thermal.movePowerTo(tPower)
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

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        if(lrdu == LRDU.Down && (side == front || side == front.back())) return NodeBase.MASK_ELECTRICAL_POWER
        return 0
    }

    override fun multiMeterString(side: Direction?) =
        Utils.plotER(shaft.energy, shaft.rads) +
            Utils.plotUIP(powerSource.u, powerSource.getCurrent())

    override fun thermoMeterString(side: Direction?) = Utils.plotCelsius("T", thermal.t)

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) =
        false

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(lastP)
    }

    override fun getWaila(): MutableMap<String, String> {
        var info = mutableMapOf<String, String>()
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        info.put("Speed", Utils.plotRads("", shaft.rads))
        if(Eln.wailaEasyMode) {
            info.put("Voltage", Utils.plotVolt("", powerSource.u))
            info.put("Current", Utils.plotAmpere("", powerSource.getCurrent()))
            info.put("Temperature", Utils.plotCelsius("", thermal.t))
        }
        return info
    }
}
