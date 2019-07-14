package mods.eln.sim

import mods.eln.Eln
import mods.eln.misc.INBTTReady
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.nbt.NBTTagCompound

abstract class NodeElectricalGateInputHysteresisProcess(internal var name: String, internal var gate: NbtElectricalGateInput) : IProcess, INBTTReady {

    internal var state = false

    protected abstract fun setOutput(value: Boolean)

    override fun process(time: Double) {
        if (state) {
            if (gate.u < Eln.SVU * 0.3) {
                state = false
                setOutput(false)
            } else
                setOutput(true)
        } else {
            if (gate.u > Eln.SVU * 0.7) {
                state = true
                setOutput(true)
            } else
                setOutput(false)
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        state = nbt.getBoolean(str + name + "state")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setBoolean(str + name + "state", state)
    }
}
