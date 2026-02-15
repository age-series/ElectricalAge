package mods.eln.sim.process.destruct

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class BipoleVoltageWatchdogTest {
    @Test
    fun setNominalVoltageUpdatesLimits() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 4.0
        b.state = 1.0
        val resistor = Resistor(a, b)
        val watchdog = BipoleVoltageWatchdog(resistor)

        watchdog.setNominalVoltage(10.0)

        assertEquals(13.0, watchdog.max)
        assertEquals(-13.0, watchdog.min)
        assertEquals(5.0, watchdog.timeoutReset)
        assertEquals(3.0, watchdog.getValue())
    }
}
