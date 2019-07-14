package mods.eln.sim

import mods.eln.Eln
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.State

class SignalRp(aPin: State) : Resistor(aPin, null) {
    init {
        r = Eln.SVU / Eln.SVII
    }
}
