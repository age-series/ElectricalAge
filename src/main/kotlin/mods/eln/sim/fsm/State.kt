package mods.eln.sim.fsm

interface State {
    fun enter()
    fun state(time: Double): State?
    fun leave()
}
