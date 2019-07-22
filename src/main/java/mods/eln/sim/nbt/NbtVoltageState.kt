package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.NBTTagCompound

class NbtVoltageState(name: String) : VoltageState(name), INBTTReady {

    override fun readFromNBT(nbttagcompound: NBTTagCompound, str: String) {
        u = nbttagcompound.getFloat(str + name + "Uc").toDouble()
        if (java.lang.Double.isNaN(u)) u = 0.0
        if (u == java.lang.Float.NEGATIVE_INFINITY.toDouble()) u = 0.0
        if (u == java.lang.Float.POSITIVE_INFINITY.toDouble()) u = 0.0
    }

    override fun writeToNBT(nbttagcompound: NBTTagCompound, str: String) {
        nbttagcompound.setFloat(str + name + "Uc", u.toFloat())
    }
}
