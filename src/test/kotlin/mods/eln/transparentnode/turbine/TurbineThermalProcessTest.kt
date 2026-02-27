package mods.eln.transparentnode.turbine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurbineThermalProcessTest {
    @Test
    fun efficiencyUsesProvidedAmbientKelvin() {
        val coldAmbient = TurbineThermalProcess.computeEfficiency(80.0, 20.0, 273.15)
        val warmAmbient = TurbineThermalProcess.computeEfficiency(80.0, 20.0, 313.15)

        assertTrue(coldAmbient > warmAmbient, "Expected cooler ambient to increase efficiency baseline.")
    }

    @Test
    fun efficiencyClampsToMinimum() {
        val efficiency = TurbineThermalProcess.computeEfficiency(0.0, 0.0, 273.15)
        assertEquals(0.05, efficiency, 1e-9)
    }

    @Test
    fun efficiencyMatchesCarnotStyleRatio() {
        val efficiency = TurbineThermalProcess.computeEfficiency(100.0, 20.0, 293.15)
        val expected = 1.0 - ((20.0 + 293.15) / (100.0 + 293.15))
        assertEquals(expected, efficiency, 1e-9)
    }
}
