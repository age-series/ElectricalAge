package mods.eln.sim.mna.state

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentStateTest {
    @Test
    fun currentStateIsPlainState() {
        val state = CurrentState()
        state.state = 2.25
        assertEquals(2.25, state.state)
    }
}
