package mods.eln.sim

import mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType
import mods.eln.misc.INBTTReady
import mods.eln.sim.core.IProcess
import net.minecraft.nbt.NBTTagCompound

abstract class RegulatorProcess(internal var name: String) : IProcess, INBTTReady {

    internal var type = RegulatorType.None
    var target: Double = 0.toDouble()
    internal var OnOffHysteresisDiv2: Double = 0.toDouble()
    internal var P: Double = 0.toDouble()
    internal var I: Double = 0.toDouble()
    internal var D: Double = 0.toDouble()
    internal var hitLast = 0.0
    internal var errorIntegrated = 0.0
    internal var boot = true

    protected abstract fun getHit(): Double

    fun setManual() {
        type = RegulatorType.Manual
    }

    fun setNone() {
        type = RegulatorType.None
    }

    fun setOnOff(OnOffHysteresisFactor: Double, workingPoint: Double) {
        type = RegulatorType.OnOff
        this.OnOffHysteresisDiv2 = OnOffHysteresisFactor * workingPoint / 2
        boot = false
        setCmd(0.0)
    }

    fun setAnalog(P: Double, I: Double, D: Double, workingPoint: Double) {
        var P = P
        var I = I
        var D = D
        P /= workingPoint
        I /= workingPoint
        D /= workingPoint

        if (!boot && (this.P != P || this.I != I || this.D != D || type != RegulatorType.Analog)) {
            errorIntegrated = 0.0
            hitLast = getHit()
        }

        this.P = P
        this.I = I
        this.D = D

        type = RegulatorType.Analog
        boot = false
    }

    protected abstract fun setCmd(cmd: Double)

    override fun process(time: Double) {
        val hit = getHit()

        when (type) {
            RegulatorType.Manual -> {}
            RegulatorType.None -> setCmd(1.0)
            RegulatorType.Analog -> {
                val error = target - hit
                val fP = error * P
                var cmd = fP - (hit - hitLast) * D * time

                errorIntegrated += error * time * I

                if (errorIntegrated > 1.0 - fP) {
                    errorIntegrated = 1.0 - fP
                    if (errorIntegrated < 0.0) errorIntegrated = 0.0
                } else if (errorIntegrated < -1.0 + fP) {
                    errorIntegrated = -1.0 + fP
                    if (errorIntegrated > 0.0) errorIntegrated = 0.0
                }

                cmd += errorIntegrated

                if (cmd > 1.0)
                    setCmd(1.0)
                else if (cmd < -1.0)
                    setCmd(-1.0)
                else
                    setCmd(cmd)

                hitLast = hit
            }

            RegulatorType.OnOff -> {
                if (hit > target + OnOffHysteresisDiv2) setCmd(0.0)
                if (hit < target - OnOffHysteresisDiv2) setCmd(1.0)
            }
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        errorIntegrated = nbt.getDouble(str + name + "errorIntegrated")
        if (java.lang.Double.isNaN(errorIntegrated)) errorIntegrated = 0.0
        target = nbt.getDouble(str + name + "target")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + name + "errorIntegrated", errorIntegrated)
        nbt.setDouble(str + name + "target", target)
    }
}
