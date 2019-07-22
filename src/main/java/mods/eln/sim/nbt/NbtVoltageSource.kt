package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.passive.VoltageSource
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class NbtVoltageSource: VoltageSource, INBTTReady {

    constructor(name: String) : super(name)

    constructor(name: String, aPin: State?, bPin: State?) : super(name, aPin, bPin)

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        u = (nbt.getDouble(str + name + "U"))
        currentState.state = nbt.getDouble(str + name + "Istate")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + name + "U", getVoltage())
        nbt.setDouble(str + name + "Istate", currentState.state)
    }
}
