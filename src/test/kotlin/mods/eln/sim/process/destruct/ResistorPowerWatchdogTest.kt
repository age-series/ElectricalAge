package mods.eln.sim.process.destruct

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class ResistorPowerWatchdogTest {
    @Test
    fun setMaximumPowerUpdatesLimits() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 6.0
        b.state = 0.0
        val resistor = Resistor(a, b).apply { resistance = 3.0 }
        val watchdog = ResistorPowerWatchdog(resistor)

        watchdog.setMaximumPower(10.0)

        assertEquals(10.0, watchdog.max)
        assertEquals(-1.0, watchdog.min)
        assertEquals(12.0, watchdog.getValue())
    }
}
