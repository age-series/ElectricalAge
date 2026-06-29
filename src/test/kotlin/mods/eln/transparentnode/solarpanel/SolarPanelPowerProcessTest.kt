package mods.eln.transparentnode.solarpanel

import kotlin.test.Test
import kotlin.test.assertEquals

class SolarPanelPowerProcessTest {
    @Test
    fun curvePassesThroughStcPoints() {
        val voc = 37.0
        val vmp = 30.0
        val imp = 8.0
        val isc = 8.6

        assertEquals(isc, SolarPanelPowerProcess.currentAtVoltage(0.0, 1.0, voc, vmp, imp, isc), 1.0e-9)
        assertEquals(imp, SolarPanelPowerProcess.currentAtVoltage(vmp, 1.0, voc, vmp, imp, isc), 1.0e-9)
        assertEquals(0.0, SolarPanelPowerProcess.currentAtVoltage(voc, 1.0, voc, vmp, imp, isc), 1.0e-9)
        assertEquals(240.0, vmp * imp, 1.0e-9)
    }

    @Test
    fun currentScalesWithLight() {
        val current = SolarPanelPowerProcess.currentAtVoltage(0.0, 0.5, 37.0, 30.0, 8.0, 8.6)

        assertEquals(4.3, current, 1.0e-9)
    }
}
