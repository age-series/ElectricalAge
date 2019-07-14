package mods.eln.sim.nbt

import mods.eln.Eln
import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Capacitor
import net.minecraft.nbt.NBTTagCompound

class NbtElectricalGateOutputProcess(name: String, positiveLoad: ElectricalLoad) : Capacitor(positiveLoad, null), INBTTReady {

    var u: Double = 0.toDouble()

    internal var highImpedance = false

    var isHighImpedance: Boolean
        get() = highImpedance
        set(enable) {
            this.highImpedance = enable
            val baseC = Eln.gateOutputCurrent / Eln.electricalFrequency / Eln.SVU
            c = if (enable) baseC / 1000 else  baseC
        }

    var outputNormalized: Double
        get() = u / Eln.SVU
        set(value) = setOutputNormalizedSafe(value)

    val outputOnOff: Boolean
        get() = u >= Eln.SVU / 2

    init {
        isHighImpedance = false
        this.name = name
    }

    override fun simProcessI(s: SubSystem) {
        if (!highImpedance)
            aPin!!.state = u
        super.simProcessI(s)
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        isHighImpedance = nbt.getBoolean(str + name + "highImpedance")
        u = nbt.getDouble(str + name + "U")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setBoolean(str + name + "highImpedance", highImpedance)
        nbt.setDouble(str + name + "U", u)
    }

    fun state(value: Boolean) {
        u = if (value) Eln.SVU else 0.0
    }

    fun setOutputNormalizedSafe(value: Double) {
        var value = value
        if (value > 1.0) value = 1.0
        if (value < 0.0) value = 0.0
        if (java.lang.Double.isNaN(value)) value = 0.0
        u = value * Eln.SVU
    }

    fun setUSafe(value: Double) {
        var value = value
        value = Utils.limit(value, 0.0, Eln.SVU)
        if (java.lang.Double.isNaN(value)) value = 0.0
        u = value
    }
}
