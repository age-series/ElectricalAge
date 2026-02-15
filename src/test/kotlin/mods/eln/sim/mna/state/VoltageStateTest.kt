package mods.eln.sim.mna.state

import kotlin.test.Test
import kotlin.test.assertEquals

class VoltageStateTest {
    @Test
    fun voltageAccessorTracksState() {
        val state = VoltageState()
        state.voltage = 4.5
        assertEquals(4.5, state.state)
        assertEquals(4.5, state.voltage)
    }
}
