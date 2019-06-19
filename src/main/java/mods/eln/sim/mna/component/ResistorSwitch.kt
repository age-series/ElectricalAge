package mods.eln.sim.mna.component

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class ResistorSwitch : Resistor, INBTTReady {

    constructor(name: String, aPin: State?, bPin: State?) {
        this.name = name
        connectTo(aPin, bPin)
    }

    internal var ultraImpedance = false

    internal var state = false

    override var r = MnaConst.highImpedance
        set(r) {
            baseR = r
            field = if (state) r else if (ultraImpedance) MnaConst.ultraImpedance else MnaConst.highImpedance
        }

    protected var baseR = 1.0

    fun setState(state: Boolean) {
        this.state = state
        r = baseR
    }

    fun getState(): Boolean {
        return state
    }

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

    fun mustUseUltraImpedance() {
        ultraImpedance = true
    }
}
