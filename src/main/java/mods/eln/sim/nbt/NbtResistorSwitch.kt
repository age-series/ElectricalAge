package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.passive.ResistorSwitch
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class NbtResistorSwitch(name: String, aPin: State?, bPin: State?) : ResistorSwitch(name, aPin, bPin), INBTTReady {

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        strl += name
        r = nbt.getDouble(strl + "R")
        if (java.lang.Double.isNaN(baseR) || baseR == 0.0) {
            if (ultraImpedance)
                ultraImpedance()
            else
                highImpedance()
        }
        setState(nbt.getBoolean(strl + "State"))
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        strl += name
        nbt.setDouble(strl + "R", baseR)
        nbt.setBoolean(strl + "State", getState())
    }
}
