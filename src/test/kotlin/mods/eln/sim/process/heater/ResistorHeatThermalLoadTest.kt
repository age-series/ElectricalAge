package mods.eln.sim.process.heater

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class ResistorHeatThermalLoadTest {
    @Test
    fun processAlwaysTransfersPower() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 6.0
        b.state = 0.0
        val resistor = Resistor(a, b)
        resistor.resistance = 3.0
        val load = ThermalLoad()

        val process = ResistorHeatThermalLoad(resistor, load)
        process.process(1.0)

        assertEquals(12.0, load.PcTemp)
    }
}
