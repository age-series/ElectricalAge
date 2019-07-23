package mods.eln.fsm

import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.sim.core.IProcess

// TODO: make it impossible to not be in a state
open class StateMachine : IProcess {

    private var initialState: State? = null
    private var state: State? = null

    fun setInitialState(initialState: State) {
        this.initialState = initialState
    }

    fun reset() {
        state = initialState
        if (state != null) state!!.enter()
    }

    protected fun stop() {
        state = null
    }

    override fun process(dt: Double) {
        if (state == null) {
            DP.println(DPType.FSM, "INVALID STATE!!")
            return
        }

        val nextState = state!!.state(dt)

        if (nextState != null && nextState !== state) {
            DP.println(DPType.FSM, javaClass.toString() + ": " + state!!.javaClass.toString() + " -> " + nextState.javaClass.toString())
            state!!.leave()
            state = nextState
            state!!.enter()
        }
    }
}

interface State {
    fun enter()

    fun state(time: Double): State?

    fun leave()
}

open class CompositeState : StateMachine(), State {

    override fun enter() {
        reset()
    }

    override fun state(time: Double): State? {
        process(time)
        return null
    }

    override fun leave() {
        stop()
    }
}
