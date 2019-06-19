package mods.eln.sim.mna.state

import mods.eln.Eln
import mods.eln.debug.DebugType

open class VoltageState : State {

    constructor() : super()
    constructor(name: String) : super(name)

    var u: Double
        get() = state
        set(state) {
            if (state == Double.NaN)
                Eln.dp.println(DebugType.MNA, "state.VoltageState setU(double state) - state was NaN!")
            this.state = state
        }
}
