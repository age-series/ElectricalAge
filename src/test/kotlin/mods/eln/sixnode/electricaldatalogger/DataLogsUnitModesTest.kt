package mods.eln.sixnode.electricaldatalogger

import kotlin.test.Test
import kotlin.test.assertTrue

class DataLogsUnitModesTest {
    @Test
    fun supportsTemperatureAndHumidityUnitStrings() {
        val temperatureF = DataLogs.getYstring(0.5f, 122f, -40f, DataLogs.temperatureType)
        val temperatureC = DataLogs.getYstring(0.5f, 50f, -40f, DataLogs.celsiusType)
        val humidity = DataLogs.getYstring(0.5f, 100f, 0f, DataLogs.humidityType)

        assertTrue(temperatureF.contains("F"), "Expected Fahrenheit unit string, got: $temperatureF")
        assertTrue(temperatureC.contains("C"), "Expected Celsius unit string, got: $temperatureC")
        assertTrue(humidity.contains("%"), "Expected humidity unit string, got: $humidity")
    }

    @Test
    fun decodesMostRecentRawSampleWithConfiguredUnits() {
        val midRaw = 0.toByte() // roughly midpoint for 8-bit packed sample
        val humidity = DataLogs.getValueString(midRaw, 100f, 0f, DataLogs.humidityType)
        val tempF = DataLogs.getValueString(midRaw, 122f, -40f, DataLogs.temperatureType)

        assertTrue(humidity.contains("%"), "Expected humidity formatting, got: $humidity")
        assertTrue(tempF.contains("F"), "Expected Fahrenheit formatting, got: $tempF")
    }
}
