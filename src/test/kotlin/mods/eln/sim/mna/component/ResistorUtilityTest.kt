package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.VoltageState

class ResistorUtilityTest {
    @Test
    fun pullDownAndHighImpedanceUseConstants() {
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b)

        resistor.pullDown()
        assertEquals(MnaConst.pullDown, resistor.resistance)

        resistor.highImpedance()
        assertEquals(MnaConst.highImpedance, resistor.resistance)
    }

    @Test
    fun powerTracksVoltageAndCurrent() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 4.0
        b.state = 1.0

        val resistor = Resistor(a, b)
        resistor.resistance = 3.0

        assertEquals(3.0, resistor.voltage)
        assertEquals(1.0, resistor.current)
        assertEquals(3.0, resistor.power)
    }
}
