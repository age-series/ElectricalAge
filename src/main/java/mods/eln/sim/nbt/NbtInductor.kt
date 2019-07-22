package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.passive.Inductor
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class NbtInductor: Inductor, INBTTReady {

    constructor(name: String) : super(name) {
        this.name = name
    }

    constructor(name: String, aPin: State, bPin: State) : super(name, aPin, bPin) {
        this.name = name
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        strl += name
        currentState.state = nbt.getDouble(strl + "Istate")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        strl += name
        nbt.setDouble(strl + "Istate", currentState.state)
    }
}
