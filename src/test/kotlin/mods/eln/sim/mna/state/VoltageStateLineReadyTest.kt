package mods.eln.sim.mna.state

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoltageStateLineReadyTest {
    @Test
    fun lineReadyFlagControlsSimplification() {
        val state = VoltageStateLineReady()
        assertFalse(state.canBeSimplifiedByLine())
        state.setCanBeSimplifiedByLine(true)
        assertTrue(state.canBeSimplifiedByLine())
    }
}
