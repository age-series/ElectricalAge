package mods.eln.sixnode.thermalsensor

import kotlin.test.Test
import kotlin.test.assertEquals

class ThermalSensorProcessTest {
    @Test
    fun measuredTemperatureAddsAmbientAndThermalDelta() {
        val measured = ThermalSensorProcess.toMeasuredTemperature(12.5, 18.0)
        assertEquals(30.5, measured, 1e-9)
    }
}
