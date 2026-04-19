package mods.eln.sim

import mods.eln.Eln
import mods.eln.sim.process.destruct.IDestructible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalLoadSupportTest {
    @Test
    fun clampsExternalSignalVoltagesIntoInternalRange() {
        assertEquals(0.0, SignalLoadSupport.clampSignalVoltage(Eln.signalVoltageAcceptNegative), 1e-12)
        assertEquals(Eln.SVU, SignalLoadSupport.clampSignalVoltage(Eln.signalVoltageAcceptPositive), 1e-12)
        assertEquals(0.0, SignalLoadSupport.toNormalized(-0.5), 1e-12)
        assertEquals(1.0, SignalLoadSupport.toNormalized(5.5), 1e-12)
    }

    @Test
    fun clampsNormalizedOutputsIntoSignalVoltageRange() {
        assertEquals(0.0, SignalLoadSupport.toVoltage(-1.0), 1e-12)
        assertEquals(Eln.SVU, SignalLoadSupport.toVoltage(2.0), 1e-12)
        assertEquals(Eln.SVU * 0.25, SignalLoadSupport.toVoltage(0.25), 1e-12)
    }

    @Test
    fun destructorAwareClampOnlyTriggersOutsideAcceptedOverdriveWindow() {
        val destructible = TestDestructible()

        assertEquals(0.0, SignalLoadSupport.clampSignalVoltage(Eln.signalVoltageAcceptNegative, destructible), 1e-12)
        assertFalse(destructible.destroyed)

        assertEquals(Eln.SVU, SignalLoadSupport.clampSignalVoltage(Eln.signalVoltageAcceptPositive, destructible), 1e-12)
        assertFalse(destructible.destroyed)

        assertEquals(0.0, SignalLoadSupport.clampSignalVoltage(Eln.signalVoltageAcceptNegative - 0.01, destructible), 1e-12)
        assertTrue(destructible.destroyed)
    }

    private class TestDestructible : IDestructible {
        var destroyed = false

        override fun destructImpl() {
            destroyed = true
        }

        override fun describe(): String = "test"
    }
}
