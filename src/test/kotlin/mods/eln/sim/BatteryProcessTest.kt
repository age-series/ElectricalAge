package mods.eln.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.misc.FunctionTable
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

class BatteryProcessTest {
    @Test
    fun processUpdatesVoltageAndWasteHeatOnRecharge() {
        val voltageFunction = FunctionTable(doubleArrayOf(1.0, 1.0), 1.0)
        val source = VoltageSource("batt")
        val thermal = ThermalLoad()
        val process = BatteryProcess(VoltageState(), VoltageState(), voltageFunction, 10.0, source, thermal)
        process.QNominal = 1.0
        process.uNominal = 10.0
        process.Q = 0.5
        process.isRechargeable = false
        source.currentState.state = 1.0

        process.process(1.0)

        assertEquals(10.0, source.voltage)
        assertTrue(thermal.PcTemp > 0.0)
    }

    @Test
    fun changeLifeScalesCharge() {
        val voltageFunction = FunctionTable(doubleArrayOf(1.0, 1.0), 1.0)
        val process = BatteryProcess(null, null, voltageFunction, 10.0, VoltageSource("b"), ThermalLoad())
        process.QNominal = 1.0
        process.uNominal = 5.0
        process.Q = 1.0
        process.life = 1.0

        process.changeLife(0.5)

        assertEquals(0.5, process.life)
        assertEquals(0.5, process.Q)
    }

    @Test
    fun energyComputationsArePositive() {
        val voltageFunction = FunctionTable(doubleArrayOf(1.0, 1.0), 1.0)
        val process = BatteryProcess(null, null, voltageFunction, 10.0, VoltageSource("b"), ThermalLoad())
        process.QNominal = 2.0
        process.uNominal = 4.0
        process.Q = 1.0
        process.life = 1.0

        assertTrue(process.energy > 0.0)
        assertTrue(process.energyMax > 0.0)
        assertEquals(process.computeVoltage(), process.u)
    }
}
