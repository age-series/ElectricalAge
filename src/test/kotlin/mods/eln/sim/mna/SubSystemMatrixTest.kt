package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.state.VoltageState

class SubSystemMatrixTest {
    @Test
    fun singularMatrixFlagsAndZerosStates() {
        disableLog4jJmx()
        val state = VoltageState()
        state.state = 5.0

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(state)

        val snapshot = subSystem.captureDebugSnapshot()
        assertTrue(snapshot.isSingular)

        subSystem.step()
        assertEquals(0.0, state.state)

        assertEquals(0.0, subSystem.solve(state))
    }
}
