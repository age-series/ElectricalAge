package mods.eln.sim.process.destruct

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import mods.eln.sim.ThermalLoad
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.ThermalLoadInitializerByPowerDrop

class ThermalLoadWatchDogTest {
    @Test
    fun settersUpdateLimits() {
        val load = ThermalLoad()
        val watchdog = ThermalLoadWatchDog(load)

        watchdog.setMaximumTemperature(100.0)
        assertEquals(100.0, watchdog.max)
        assertEquals(-40.0, watchdog.min)
        assertEquals(100.0, watchdog.timeoutReset)

        val initializer = ThermalLoadInitializer(120.0, -20.0, 1.0, 1.0)
        watchdog.setThermalLoad(initializer)
        assertEquals(120.0, watchdog.max)
        assertEquals(-20.0, watchdog.min)

        watchdog.setTemperatureLimits(90.0, -10.0)
        assertEquals(90.0, watchdog.max)
        assertEquals(-10.0, watchdog.min)

        val byDrop = ThermalLoadInitializerByPowerDrop(80.0, -5.0, 1.0, 1.0)
        watchdog.setTemperatureLimits(byDrop)
        assertEquals(80.0, watchdog.max)
        assertEquals(-5.0, watchdog.min)
    }

    @Test
    fun dumpMatrixOnTripReturnsSelf() {
        val load = ThermalLoad()
        val watchdog = ThermalLoadWatchDog(load)
        val returned = watchdog.dumpMatrixOnTrip("reason") { load }
        assertSame(watchdog, returned)
    }

    @Test
    fun getValueIncludesAmbientWhenConfigured() {
        val load = ThermalLoad().apply { temperatureCelsius = 40.0 }
        val watchdog = ThermalLoadWatchDog(load)
            .setAmbientTemperatureProvider { 25.0 }

        assertEquals(65.0, watchdog.getValue())
    }

    @Test
    fun ambientProviderCanMakeWatchdogTripAtAbsoluteLimit() {
        val load = ThermalLoad().apply { temperatureCelsius = 80.0 }
        val watchdog = ThermalLoadWatchDog(load)
            .setAmbientTemperatureProvider { 40.0 }
            .setTemperatureLimits(100.0, -40.0)

        assertTrue(watchdog.getValue() > watchdog.max, "Absolute temperature should exceed configured max.")
    }
}
