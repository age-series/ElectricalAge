package mods.eln.sim.mna.component

import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState

abstract class Monopole : Component() {

    internal var pin: VoltageState? = null

    override fun getConnectedStates() = arrayOf<State?>(pin)

    fun connectTo(pin: VoltageState?): Monopole {
        this.pin = pin
        pin?.add(this)
        return this
    }
}
