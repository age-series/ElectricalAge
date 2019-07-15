package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
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
import java.io.DataOutputStream
import java.awt.Color
import java.io.DataInputStream

enum class GMType(val type: Int) {
    GENERATOR(0),
    MOTOR(1)
}

class GenMotorDescriptor(
    name: String, //type inherited
    val type: GMType,
    override val obj: Obj3D,
    val cable: GenericCableDescriptor,
    val nominalRads: Double,
    val nominalU: Double,
    val nominalP: Double,
    val efficiency: Double,
    val thermalLoadInitializer: ThermalLoadInitializer
): SimpleShaftDescriptor(
    name,
    GenMotorElenemt::class,
    GenMotorRender::class,
    EntityMetaTag.Basic
){
    val radsToU: IFunction
    val customSound: String
    override val static = arrayOf(
        obj.getPart("Cowl"),
        obj.getPart("Stand")
    ).requireNoNulls()
    override val rotating = arrayOf(
        obj.getPart("Shaft")
    ).requireNoNulls()
    val leds: Array<Obj3D.Obj3DPart>

    init {
        leds = arrayOf(
            obj.getPart("LED_0"),
            obj.getPart("LED_1"),
            obj.getPart("LED_2"),
            obj.getPart("LED_3"),
            obj.getPart("LED_4"),
            obj.getPart("LED_5"),
            obj.getPart("LED_6")
        ).requireNoNulls()
        radsToU = LinearFunction(0f, 0f, nominalRads.toFloat(), nominalU.toFloat())
        if (this.type == GMType.GENERATOR) {
            customSound = "eln:shaft_motor"
        }else{
            customSound = "eln:generator"
        }
        thermalLoadInitializer.setMaximalPower(nominalP)
        voltageLevelColor = VoltageLevelColor.None
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add("Converts electricity into mechanical")
        list.add("energy and vice versa.")
        if (type == GMType.MOTOR) {
            list.add("  Efficiently converts electrical energy to mechanical energy")
            list.add("  Poorly converts mechanical energy to electrical energy")
        } else {
            list.add("  Efficiently converts mechanical energy to electrical energy")
            list.add("  Poorly converts electrical energy to mechanical energy")
        }
        list.add("  " + Utils.plotVolt("Operating Voltage: ", nominalU))
        list.add("  " + Utils.plotPower("Operating Power: ", nominalP))
        list.add("  " + Utils.plotRads("Maximum rad/s", absoluteMaximumShaftSpeed))
    }
}

class GenMotorElenemt(node: TransparentNode, desc_: TransparentNodeDescriptor): SimpleShaftElement(node, desc_) {
    val desc: GenMotorDescriptor
    val wireLoad: NbtElectricalLoad
    val shaftLoad: NbtElectricalLoad
    val wireShaftResistor: Resistor
    val powerSource: VoltageSource
    val electicalProcess: GenMotorElectricalProcess
    val shaftProcess: GenMotorShaftProcess
    val thermal: NbtThermalLoad
    val heater: ElectricalLoadHeatThermalLoad
    val thermalWatchdog: ThermalLoadWatchDog

    init {
        desc = desc_ as GenMotorDescriptor
        wireLoad = NbtElectricalLoad("wireLoad")
        desc.cable.applyTo(wireLoad)
        shaftLoad = NbtElectricalLoad("shaftLoad")
        desc.cable.applyTo(shaftLoad)
        wireShaftResistor = Resistor(wireLoad, shaftLoad)
        desc.cable.applyTo(wireShaftResistor)
        powerSource = VoltageSource("powerSource", shaftLoad, null)
        electicalProcess = GenMotorElectricalProcess()
        shaftProcess = GenMotorShaftProcess()
        thermal = NbtThermalLoad("thermal")
        thermal.setAsSlow()
        thermalLoadList.add(thermal)
        desc.thermalLoadInitializer.applyTo(thermal)
        thermalWatchdog = ThermalLoadWatchDog()
        thermalWatchdog.set(thermal).set(WorldExplosion(this).machineExplosion())
        desc.thermalLoadInitializer.applyTo(thermalWatchdog)
        slowProcessList.add(thermalWatchdog)
        electricalLoadList.addAll(arrayOf(wireLoad, shaftLoad))
        electricalComponentList.addAll(arrayOf(wireShaftResistor, powerSource))
        electricalProcessList.add(shaftProcess)
        heater = ElectricalLoadHeatThermalLoad(wireLoad, thermal)
        thermalFastProcessList.add(heater)
    }

    var lastP = 0.0
    fun maybePublishP(P: Double) {
        if (Math.abs(P-lastP) / desc.nominalP > 0.01) {
            lastP = P
            needPublish()
        }
    }

    override fun connectJob() {
        super.connectJob()
        Eln.simulator.mna.addProcess(electicalProcess)
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.mna.removeProcess(electicalProcess)
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        return when(side) {
            front -> wireLoad
            front.back() -> wireLoad
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU) = thermal

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if(lrdu == LRDU.Down && (side == front || side == front.back())) return NodeBase.MASK_ELECTRICAL_POWER
        return 0
    }

    override fun multiMeterString(side: Direction?) =
        Utils.plotER(shaft.energy, shaft.rads) +
            Utils.plotUIP(powerSource.u, powerSource.getCurrent())

    override fun thermoMeterString(side: Direction) = Utils.plotCelsius("T", thermal.t)

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float) = false

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(lastP)
    }

    override fun getWaila(): MutableMap<String, String> {
        val info = mutableMapOf<String, String>()
        info["Energy"] = Utils.plotEnergy("", shaft.energy)
        info["Speed"] = Utils.plotRads("", shaft.rads)
        if(Eln.wailaEasyMode) {
            info["Voltage"] = Utils.plotVolt("", powerSource.u)
            info["Current"] = Utils.plotAmpere("", powerSource.getCurrent())
            info["Temperature"] = Utils.plotCelsius("", thermal.t)
        }
        return info
    }

    inner class GenMotorElectricalProcess: IProcess, IRootSystemPreStepProcess {
        override fun process(time: Double) {
            val noTorqueU = desc.radsToU.getValue(shaft.rads)
            val th = wireLoad.subSystem!!.getTh(wireLoad, powerSource)
            if (th.U.isNaN()) {
                th.U = noTorqueU
                th.R = MnaConst.highImpedance
            }
            val U: Double
            if(th.isHighImpedance) {
                U = noTorqueU
            } else if (noTorqueU < th.U) {
                U = th.U * 0.9999 + noTorqueU * 0.0001
            } else {
                val a = 1 / th.R
                val b = 25 - th.U / th.R
                val c = -25 * noTorqueU
                U = (-b + Math.sqrt(b * b -4 * a * c)) / (2 * a)
            }
            powerSource.u = U
        }

        override fun rootSystemPreStepProcess() {
            process(0.0)
        }

    }

    inner class GenMotorShaftProcess: IProcess {

        var last = -1

        override fun process(time: Double) {
            val p = powerSource.getPower()
            var E = -p * time
            if (E.isNaN()) {
                E = 0.0
            }
            //DP.println(DPType.MECHANICAL, "T: " + desc.type + "\tE: " + E)

            if (E < 0 && desc.type == GMType.MOTOR) {
                // terrible pushing electrical power
                E *= 10.0
            }
            if (E < 0 && desc.type == GMType.GENERATOR) {
                // terrible pushing shaft power
                E *= 0.1
            }
            maybePublishP(E / time)
            E = E - defaultDrag * Math.max(shaft.rads, 10.0)
            shaft.energy += E * desc.efficiency
            val tPower = E * (1 - desc.efficiency)
            thermal.movePowerTo(tPower)
        }
    }

}

class GenMotorRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor): ShaftRender(entity, desc_) {
    val desc: GenMotorDescriptor
    val ledColors: Array<Color>
    val ledColorBase: Array<HSLColor>

    init {
        desc = desc_ as GenMotorDescriptor
        ledColors = arrayOf(
            Color.black,
            Color.black,
            Color.black,
            Color.black,
            Color.black,
            Color.black,
            Color.black
        )
        ledColorBase = arrayOf(
            GREEN,
            GREEN,
            GREEN,
            GREEN,
            YELLOW,
            RED,
            RED
        )
        addLoopedSound(GenMotorLoopedSound(desc.customSound, coordonate()))
        mask.set(LRDU.Down, true)
    }

    private fun setPower(power: Double) {
        if (power < 0) {
            ledColors[0] = RED.adjustLuminanceClamped((-power / desc.nominalP * 400).toFloat(), 0f, 60f)
            for (i in 1..6) ledColors[i] = Color.black
        } else {
            val slice = desc.nominalP / 4
            var current = power
            for (i in 0 .. 6) {
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
        if (lrdu == LRDU.Down && side == front) return desc.cable.render
        return null
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        val power = stream.readDouble()

        setPower(power)
        volumeSetting.target = Math.min(1.0f, Math.abs(power /desc.nominalP).toFloat() / 4f)
    }

    inner class GenMotorLoopedSound(sound: String, coord: Coordonate) :
        LoopedSound(sound, coord) {
        override fun getPitch() = Math.max(0.05, rads / desc.nominalRads).toFloat()
        override fun getVolume() = volumeSetting.position
    }
}
