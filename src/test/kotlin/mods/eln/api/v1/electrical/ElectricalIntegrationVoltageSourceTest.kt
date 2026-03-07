package mods.eln.api.v1.electrical

import kotlin.test.Test
import kotlin.test.assertEquals

class ElectricalIntegrationVoltageSourceTest {
    @Test
    fun storesConfiguredVoltageForGroundReferencedSource() {
        val positive = ElectricalIntegration.Load("test.positive") {}
        val source = ElectricalIntegration.VoltageSource(positiveLoad = positive, initialVoltage = 120.0)

        assertEquals(120.0, source.voltage, 1e-12)

        source.setVoltage(48.0)
        assertEquals(48.0, source.voltage, 1e-12)
    }

    @Test
    fun storesConfiguredVoltageForBipolarSource() {
        val positive = ElectricalIntegration.Load("test.positive.bipolar") {}
        val negative = ElectricalIntegration.Load("test.negative.bipolar") {}
        val source = ElectricalIntegration.createVoltageSource(positive, negative, 230.0)

        assertEquals(230.0, source.voltage, 1e-12)
        assertEquals("test.positive.bipolar", source.positiveLoad.name)
        assertEquals("test.negative.bipolar", source.negativeLoad?.name)
    }
}
