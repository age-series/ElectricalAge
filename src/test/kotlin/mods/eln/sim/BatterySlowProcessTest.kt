package mods.eln.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.misc.FunctionTable
import mods.eln.server.SaveConfig
import mods.eln.sim.mna.component.VoltageSource

private class TestBatterySlowProcess(
    batteryProcess: BatteryProcess,
    thermalLoad: ThermalLoad
) : BatterySlowProcess(batteryProcess, thermalLoad) {
    var destroyed = false

    override fun destroy() {
        destroyed = true
    }
}

class BatterySlowProcessTest {
    @Test
    fun destroyOnInvalidVoltageOrOverVoltage() {
        SaveConfig("test")
        val voltageFunction = FunctionTable(doubleArrayOf(1.0, 1.0), 1.0)
        val process = BatteryProcess(null, null, voltageFunction, 10.0, VoltageSource("b"), ThermalLoad())
        process.QNominal = 1.0
        process.uNominal = -10.0
        process.Q = 1.0
        val slow = TestBatterySlowProcess(process, ThermalLoad())

        slow.process(1.0)
        assertTrue(slow.destroyed)

        process.uNominal = 10.0
        process.voltageFunction = FunctionTable(doubleArrayOf(2.0, 2.0), 1.0)
        process.Q = 1.0
        slow.destroyed = false
        slow.process(1.0)
        assertTrue(slow.destroyed)
    }

    @Test
    fun agingReducesLifeWhenEnabled() {
        SaveConfig("test")
        val voltageFunction = FunctionTable(doubleArrayOf(1.0, 1.0), 1.0)
        val source = VoltageSource("b")
        val process = BatteryProcess(null, null, voltageFunction, 10.0, source, ThermalLoad())
        process.QNominal = 1.0
        process.uNominal = 5.0
        process.Q = 1.0
        process.life = 1.0

        val slow = TestBatterySlowProcess(process, ThermalLoad())
        slow.lifeNominalCurrent = 1.0
        slow.lifeNominalLost = 0.5
        source.currentState.state = 1.0

        slow.process(1.0)
        assertEquals(0.5, process.life)
    }
}
