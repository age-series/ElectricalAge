package mods.eln.sim.process.destruct

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.state.VoltageState

class VoltageStateWatchDogTest {
    @Test
    fun setNominalVoltageUpdatesLimits() {
        val state = VoltageState()
        state.voltage = 5.0
        val watchdog = VoltageStateWatchDog(state)

        watchdog.setNominalVoltage(10.0)

        assertEquals(13.0, watchdog.max)
        assertEquals(-13.0, watchdog.min)
        assertEquals(2.5, watchdog.timeoutReset)
        assertEquals(5.0, watchdog.getValue())
    }
}
