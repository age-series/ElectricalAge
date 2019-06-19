package mods.eln.sim.mna.component

import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.CurrentState
import mods.eln.sim.mna.state.State

class Transformer : Bipole {

    var aCurrentState = CurrentState()
    var bCurrentState = CurrentState()

    var ratio = 1.0

    override fun getCurrent() = 0.0

    constructor() {}

    constructor(aPin: State, bPin: State) : super(aPin, bPin) {}

    override fun quitSubSystem() {
        getSubSystem()!!.states.remove(aCurrentState)
        getSubSystem()!!.states.remove(bCurrentState)
        super.quitSubSystem()
    }

    override fun addedTo(s: SubSystem) {
        super.addedTo(s)
        s.addState(aCurrentState)
        s.addState(bCurrentState)
    }

    override fun applyTo(s: SubSystem) {
        s.addToA(bPin, bCurrentState, 1.0)
        s.addToA(bCurrentState, bPin, 1.0)
        s.addToA(bCurrentState, aPin, -ratio)

        s.addToA(aPin, aCurrentState, 1.0)
        s.addToA(aCurrentState, aPin, 1.0)
        s.addToA(aCurrentState, bPin, -1 / ratio)

        s.addToA(aCurrentState, aCurrentState, 1.0)
        s.addToA(aCurrentState, bCurrentState, ratio)
        s.addToA(bCurrentState, aCurrentState, 1.0)
        s.addToA(bCurrentState, bCurrentState, ratio)
    }
}
