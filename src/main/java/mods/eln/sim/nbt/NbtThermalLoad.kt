package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.ThermalLoad
import net.minecraft.nbt.NBTTagCompound

class NbtThermalLoad : ThermalLoad, INBTTReady {

    internal var name: String

    constructor(name: String, Tc: Double, Rp: Double, Rs: Double, C: Double) : super(Tc, Rp, Rs, C) {
        this.name = name
    }

    constructor(name: String) : super() {
        this.name = name
    }

    override fun readFromNBT(nbttagcompound: NBTTagCompound, str: String) {
        Tc = nbttagcompound.getFloat(str + name + "Tc").toDouble()
        if (java.lang.Double.isNaN(Tc)) Tc = 0.0
        if (Tc == java.lang.Float.NEGATIVE_INFINITY.toDouble()) Tc = 0.0
        if (Tc == java.lang.Float.POSITIVE_INFINITY.toDouble()) Tc = 0.0
    }

    override fun writeToNBT(nbttagcompound: NBTTagCompound, str: String) {
        nbttagcompound.setFloat(str + name + "Tc", Tc.toFloat())
    }
}
