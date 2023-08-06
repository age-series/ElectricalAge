package mods.eln.sim.fsm

import mods.eln.misc.Utils.println
import mods.eln.sim.IProcess

open class StateMachine : IProcess {
    fun setInitialState(initialState: State?) {
        this.initialState = initialState
    }

    fun reset() {
        state = initialState
        if (state != null) state!!.enter()
    }

    protected fun stop() {
        state = null
    }

    override fun process(time: Double) {
        if (state == null) {
            println("INVALID STATE!!")
            return
        }
        val nextState = state!!.state(time)
        if (nextState != null && nextState !== state) {
            println(javaClass.toString() + ": " + state!!.javaClass + " -> " + nextState.javaClass)
            state!!.leave()
            state = nextState
            state!!.enter()
        }
    }

    private var initialState: State? = null
    private var state: State? = null
}
