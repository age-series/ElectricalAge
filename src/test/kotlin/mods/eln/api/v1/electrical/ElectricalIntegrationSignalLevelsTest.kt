package mods.eln.api.v1.electrical

import kotlin.test.Test
import kotlin.test.assertEquals

class ElectricalIntegrationSignalLevelsTest {
    @Test
    fun convertsNormalizedValuesToVoltageUsingPublicSignalLevel() {
        val maxVoltage = ElectricalIntegration.getSignalVoltageLevel()
        assertEquals(maxVoltage * 0.5, ElectricalIntegration.SignalLevels.toVoltage(0.5), 1e-12)
        assertEquals(maxVoltage, ElectricalIntegration.SignalLevels.MAX_VOLTAGE, 1e-12)
    }

    @Test
    fun clampsSignalConversionsToExpectedRange() {
        assertEquals(0.0, ElectricalIntegration.SignalLevels.toVoltage(-1.0), 1e-12)
        assertEquals(1.0, ElectricalIntegration.SignalLevels.toNormalized(9999.0), 1e-12)
        assertEquals(0.0, ElectricalIntegration.SignalLevels.toNormalized(Double.NaN), 1e-12)
    }
}
