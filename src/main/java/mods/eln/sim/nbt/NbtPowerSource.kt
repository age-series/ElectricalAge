package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.active.PowerSource
import net.minecraft.nbt.NBTTagCompound

class NbtPowerSource(name: String, aPin: State): PowerSource(name, aPin), INBTTReady {

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        var strl = str

        strl += name

        p = nbt.getDouble(strl + "P")
        Umax = nbt.getDouble(strl + "Umax")
        Imax = nbt.getDouble(strl + "Imax")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        var strl = str

        strl += name

        nbt.setDouble(strl + "P", p)
        nbt.setDouble(strl + "Umax", Umax)
        nbt.setDouble(strl + "Imax", Imax)
    }
}
