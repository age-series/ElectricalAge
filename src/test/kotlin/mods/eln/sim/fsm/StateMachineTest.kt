package mods.eln.sim.fsm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.disableLog4jJmx

private class TrackingState(
    private val next: State? = null
) : State {
    var entered = 0
    var left = 0
    var stepped = 0

    override fun enter() {
        entered++
    }

    override fun state(time: Double): State? {
        stepped++
        return next
    }

    override fun leave() {
        left++
    }
}

class StateMachineTest {
    @Test
    fun transitionCallsEnterLeave() {
        disableLog4jJmx()
        val a = TrackingState()
        val b = TrackingState()
        val machine = StateMachine()
        machine.setInitialState(a)
        machine.reset()

        a.stepped = 0
        machine.process(1.0)
        assertEquals(1, a.entered)

        val switching = TrackingState(b)
        val machine2 = StateMachine()
        machine2.setInitialState(switching)
        machine2.reset()
        machine2.process(1.0)
        assertEquals(1, switching.left)
        assertEquals(1, b.entered)
    }

    @Test
    fun compositeStateDelegatesToStateMachine() {
        disableLog4jJmx()
        val state = TrackingState()
        val composite = CompositeState()
        composite.setInitialState(state)
        composite.enter()
        composite.state(1.0)
        composite.leave()

        assertTrue(state.entered >= 1)
    }

    @Test
    fun resetHandlesNullInitialState() {
        disableLog4jJmx()
        val machine = StateMachine()
        machine.setInitialState(null)
        machine.reset()
        machine.process(1.0)
    }

    @Test
    fun noTransitionWhenStateReturnsSameInstance() {
        disableLog4jJmx()
        val state = object : State {
            var entered = 0
            var left = 0
            override fun enter() {
                entered++
            }

            override fun state(time: Double): State = this

            override fun leave() {
                left++
            }
        }
        val machine = StateMachine()
        machine.setInitialState(state)
        machine.reset()
        machine.process(1.0)

        assertEquals(1, state.entered)
        assertEquals(0, state.left)
    }
}
