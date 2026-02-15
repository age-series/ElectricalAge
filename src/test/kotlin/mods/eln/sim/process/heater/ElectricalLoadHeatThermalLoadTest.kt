package mods.eln.sim.process.heater

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Resistor

class ElectricalLoadHeatThermalLoadTest {
    @Test
    fun processTransfersPowerWhenSimulated() {
        val load = ElectricalLoad()
        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(load)
        load.state = 10.0
        load.setSerialResistance(2.0)
        Resistor(load, null).apply { resistance = 5.0 }

        val thermal = ThermalLoad().apply { heatCapacity = 10.0 }
        val process = ElectricalLoadHeatThermalLoad(load, thermal)
        process.process(1.0)

        assertEquals(4.0, thermal.PcTemp)
    }

    @Test
    fun limitTemperatureRateClampsPower() {
        val load = ElectricalLoad()
        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(load)
        load.state = 10.0
        load.setSerialResistance(2.0)
        Resistor(load, null).apply { resistance = 5.0 }

        val thermal = ThermalLoad().apply { heatCapacity = 10.0 }
        val process = ElectricalLoadHeatThermalLoad(load, thermal).limitTemperatureRate(0.1)
        process.process(1.0)

        assertEquals(1.0, thermal.PcTemp)
    }

    @Test
    fun processSkipsWhenNotSimulated() {
        val load = ElectricalLoad()
        val thermal = ThermalLoad()
        val process = ElectricalLoadHeatThermalLoad(load, thermal)

        process.process(1.0)

        assertEquals(0.0, thermal.PcTemp)
    }
}
