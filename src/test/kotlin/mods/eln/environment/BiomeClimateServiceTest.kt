package mods.eln.environment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BiomeClimateServiceTest {
    @Test
    fun dayBlendTracksThermalLagAndEveningCooldown() {
        val preSunrise = BiomeClimateService.dayBlendForWorldTicks(23000)
        val sunrise = BiomeClimateService.dayBlendForWorldTicks(0)
        val noon = BiomeClimateService.dayBlendForWorldTicks(6000)
        val afternoonPeak = BiomeClimateService.dayBlendForWorldTicks(10000)
        val sunset = BiomeClimateService.dayBlendForWorldTicks(12000)
        val evening = BiomeClimateService.dayBlendForWorldTicks(14000)
        val midnight = BiomeClimateService.dayBlendForWorldTicks(18000)

        assertTrue(preSunrise < 0.01, "Pre-sunrise blend should be near minimum, was $preSunrise")
        assertTrue(sunrise > preSunrise, "Sunrise should be warmer than pre-sunrise")
        assertTrue(afternoonPeak > noon, "Late-afternoon should be warmer than noon")
        assertTrue(afternoonPeak > sunset, "Temperature should start dropping after late-afternoon peak")
        assertTrue(evening < sunset, "Cooling should accelerate after sunset")
        assertTrue(midnight < evening, "Midnight should be cooler than evening")
    }

    @Test
    fun interpolationFollowsDayNightExtremes() {
        val tempNoon = BiomeClimateService.interpolateTemperature(28.0, 12.0, 1.0)
        val tempMidnight = BiomeClimateService.interpolateTemperature(28.0, 12.0, 0.0)
        val humidityNoon = BiomeClimateService.interpolateHumidity(40.0, 80.0, 1.0)
        val humidityMidnight = BiomeClimateService.interpolateHumidity(40.0, 80.0, 0.0)

        assertEquals(28.0, tempNoon)
        assertEquals(12.0, tempMidnight)
        assertEquals(40.0, humidityNoon)
        assertEquals(80.0, humidityMidnight)
    }

    @Test
    fun humidityBoostHandlesRainSnowThunderAndClamp() {
        val rain = BiomeClimateService.applyHumidityBoost(60.0, "rain", isRaining = true, isThundering = false)
        val snow = BiomeClimateService.applyHumidityBoost(60.0, "snow", isRaining = true, isThundering = false)
        val thunderRain = BiomeClimateService.applyHumidityBoost(82.0, "rain", isRaining = true, isThundering = true)
        val dry = BiomeClimateService.applyHumidityBoost(45.0, "rain", isRaining = false, isThundering = true)

        assertEquals(90.0, rain)
        assertEquals(85.0, snow)
        assertEquals(100.0, thunderRain)
        assertEquals(45.0, dry)
    }

    @Test
    fun cloudCoverCoolsAirDuringRainAndStorms() {
        val clear = BiomeClimateService.applyCloudTemperatureDelta(25.0, isRaining = false, isThundering = false)
        val rain = BiomeClimateService.applyCloudTemperatureDelta(25.0, isRaining = true, isThundering = false)
        val storm = BiomeClimateService.applyCloudTemperatureDelta(25.0, isRaining = true, isThundering = true)

        assertEquals(25.0, clear)
        assertEquals(23.0, rain)
        assertEquals(22.0, storm)
    }

    @Test
    fun fallbackAmbientUsesPlainsProfileCurve() {
        val preSunrise = BiomeClimateService.fallbackAmbientTemperatureCelsius(23000)
        val noon = BiomeClimateService.fallbackAmbientTemperatureCelsius(6000)
        val afternoonPeak = BiomeClimateService.fallbackAmbientTemperatureCelsius(10000)

        assertTrue(preSunrise in 15.0..27.0, "Fallback ambient should stay in fallback profile bounds.")
        assertTrue(noon in 15.0..27.0, "Fallback ambient should stay in fallback profile bounds.")
        assertTrue(afternoonPeak in 15.0..27.0, "Fallback ambient should stay in fallback profile bounds.")
        assertTrue(afternoonPeak > noon, "Fallback curve should still peak in late afternoon.")
        assertTrue(noon > preSunrise, "Fallback curve should warm from pre-sunrise to noon.")
    }
}
