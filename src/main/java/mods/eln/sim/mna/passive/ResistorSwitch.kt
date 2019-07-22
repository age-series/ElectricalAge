package mods.eln.sim.mna.passive

import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.State

open class ResistorSwitch(name: String, aPin: State?, bPin: State?) : Resistor() {

    init {
        this.name = name
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

    fun mustUseUltraImpedance() {
        ultraImpedance = true
    }

    init {
        connectTo(aPin, bPin)
    }
}
