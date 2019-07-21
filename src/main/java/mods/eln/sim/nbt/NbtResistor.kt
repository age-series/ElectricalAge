package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class NbtResistor(name: String, aPin: State?, bPin: State?) : Resistor(aPin, bPin), INBTTReady {

    init {
        this.name = name
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        name += str
        r = nbt.getDouble(str + "R")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        name += str
        nbt.setDouble(str + "R", r)
    }
}
