package mods.eln.environment

data class ClimateState(
    val temperatureCelsius: Double,
    val relativeHumidityPercent: Double,
    val dayBlend: Double,
    val precipitationActive: Boolean,
    val precipitationType: String
) {
    val normalizedHumidity: Double
        get() = relativeHumidityPercent / 100.0
}
