package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.state.ElectricalLoad
import net.minecraft.nbt.NBTTagCompound

open class NbtElectricalLoad(name: String) : ElectricalLoad(), INBTTReady {

    init {
        this.name = name
    }

    override fun readFromNBT(nbttagcompound: NBTTagCompound, str: String) {
        u = nbttagcompound.getDouble(str + name + "Uc")
        if (java.lang.Double.isNaN(u)) u = 0.0
        if (u == Double.NEGATIVE_INFINITY) u = 0.0
        if (u == Double.POSITIVE_INFINITY) u = 0.0
    }

    override fun writeToNBT(nbttagcompound: NBTTagCompound, str: String) {
        nbttagcompound.setDouble(str + name + "Uc", u)
    }
}
