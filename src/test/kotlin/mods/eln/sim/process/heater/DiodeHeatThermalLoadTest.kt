package mods.eln.sim.process.heater

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class DiodeHeatThermalLoadTest {
    @Test
    fun processTransfersPowerWhenResistanceStable() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 10.0
        b.state = 0.0
        val resistor = Resistor(a, b)
        resistor.resistance = 5.0
        val load = ThermalLoad()

        val process = DiodeHeatThermalLoad(resistor, load)
        process.process(1.0)

        assertEquals(20.0, load.PcTemp)

        resistor.resistance = 10.0
        process.process(1.0)
        assertEquals(20.0, load.PcTemp)
    }
}
