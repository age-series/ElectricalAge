package mods.eln.environment

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mods.eln.Eln
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeGenBase
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

object BiomeClimateService {
    private const val TICKS_PER_DAY = 24000.0
    private const val DEFAULT_WORLD_TIME_TICKS = 6000L
    private const val RAIN_HUMIDITY_BOOST_PERCENT = 20.0
    private const val SNOW_HUMIDITY_BOOST_PERCENT = 15.0
    private const val THUNDER_HUMIDITY_BOOST_PERCENT = 10.0
    private const val MIN_RAIN_HUMIDITY_PERCENT = 90.0
    private const val MIN_SNOW_HUMIDITY_PERCENT = 85.0
    private const val MIN_THUNDER_HUMIDITY_PERCENT = 95.0
    private const val RAIN_CLOUD_TEMP_DELTA_C = -2.0
    private const val THUNDER_CLOUD_TEMP_DELTA_C = -1.0

    private val profilesByBiomeName = HashMap<String, BiomeClimateProfile>()
    private val profilesByBiomeId = HashMap<Int, BiomeClimateProfile>()
    private val missingProfileBiomeKeys = HashSet<String>()
    private val weatherByDimension = HashMap<Int, WeatherSnapshot>()
    @Volatile private var loaded = false
    @Volatile private var startupAuditComplete = false

    @JvmStatic
    fun sample(world: World, x: Int, y: Int, z: Int): ClimateState {
        ensureLoaded()

        val weather = getWeatherSnapshot(world)
        val biome = world.getBiomeGenForCoords(x, z)
        val profile = resolveProfileForBiome(biome)

        val dayBlend = dayBlendForWorldTicks(world.worldTime)
        var temperatureC = interpolateTemperature(profile.dayHighCelsius, profile.nightLowCelsius, dayBlend)
        var humidity = interpolateHumidity(profile.dayHumidityPercent, profile.nightHumidityPercent, dayBlend)

        var activePrecipitationType = "none"
        if (weather.raining) {
            temperatureC = applyCloudTemperatureDelta(temperatureC, isRaining = true, isThundering = weather.thundering)
            if (supportsPrecipitation(profile)) {
                activePrecipitationType = resolvePrecipitationType(biome, x, y, z, profile)
                humidity = applyHumidityBoost(humidity, activePrecipitationType, isRaining = true, isThundering = weather.thundering)
            }
        }

        humidity = humidity.coerceIn(0.0, 100.0)
        return ClimateState(
            temperatureCelsius = temperatureC,
            relativeHumidityPercent = humidity,
            dayBlend = dayBlend,
            precipitationActive = activePrecipitationType != "none",
            precipitationType = activePrecipitationType
        )
    }

    private fun supportsPrecipitation(profile: BiomeClimateProfile): Boolean {
        return profile.precipitationType != "none"
    }

    private fun resolvePrecipitationType(
        biome: BiomeGenBase?,
        x: Int,
        y: Int,
        z: Int,
        profile: BiomeClimateProfile
    ): String {
        if (profile.precipitationType == "snow" || isSnowBiome(biome, x, y, z)) {
            return "snow"
        }
        return "rain"
    }

    private fun resolveProfileForBiome(biome: BiomeGenBase?): BiomeClimateProfile {
        if (biome != null) {
            synchronized(this) {
                profilesByBiomeId[biome.biomeID]?.let { return it }
            }
        }

        val biomeName = biome?.biomeName
        val resolved = if (!biomeName.isNullOrBlank()) {
            findProfileForBiomeName(biomeName) ?: run {
                synchronized(this) {
                    missingProfileBiomeKeys.add(normalizeKey(biomeName))
                }
                createFallbackProfile()
            }
        } else {
            createFallbackProfile()
        }

        if (biome != null) {
            synchronized(this) {
                profilesByBiomeId[biome.biomeID] = resolved
            }
        }
        return resolved
    }

    private fun createFallbackProfile(): BiomeClimateProfile {
        return profilesByBiomeName[normalizeKey("Plains")]
            ?: BiomeClimateProfile(
                dayHighCelsius = 27.0,
                nightLowCelsius = 15.0,
                dayHumidityPercent = 45.0,
                nightHumidityPercent = 65.0,
                precipitationType = "rain"
            )
    }

    @JvmStatic
    @JvmOverloads
    fun fallbackAmbientTemperatureCelsius(worldTime: Long = DEFAULT_WORLD_TIME_TICKS): Double {
        ensureLoaded()
        val profile = createFallbackProfile()
        val dayBlend = dayBlendForWorldTicks(worldTime)
        return interpolateTemperature(profile.dayHighCelsius, profile.nightLowCelsius, dayBlend)
    }

    @JvmStatic
    fun auditMissingBiomeProfilesAtStartup() {
        ensureLoaded()
        synchronized(this) {
            if (startupAuditComplete) {
                return
            }

            val missingEntries = ArrayList<Pair<Int, String>>()
            BiomeGenBase.getBiomeGenArray()
                .filterNotNull()
                .forEach { biome ->
                    val name = biome.biomeName ?: return@forEach
                    if (findProfileForBiomeName(name) == null) {
                        missingProfileBiomeKeys.add(normalizeKey(name))
                        missingEntries.add(biome.biomeID to name)
                    }
                }

            if (missingEntries.isEmpty()) {
                Eln.logger.info("Biome climate startup audit: all registered biomes have climate profiles.")
            } else {
                Eln.logger.warn(
                    "Biome climate startup audit: {} registered biomes have no climate profile and will use Plains fallback.",
                    missingEntries.size
                )
                missingEntries
                    .sortedBy { it.first }
                    .forEach { (id, name) ->
                        Eln.logger.warn("Missing biome climate profile for biome id={} name='{}'", id, name)
                    }
            }

            startupAuditComplete = true
        }
    }

    private fun findProfileForBiomeName(biomeName: String): BiomeClimateProfile? {
        val candidates = biomeNameCandidates(biomeName)
        candidates.forEach { key ->
            profilesByBiomeName[key]?.let { return it }
        }
        return null
    }

    private fun biomeNameCandidates(rawName: String): List<String> {
        val base = normalizeKey(rawName)
        if (base.isBlank()) return emptyList()

        val yielded = HashSet<String>()
        val out = ArrayList<String>()
        fun emit(candidate: String) {
            val normalized = normalizeKey(candidate)
            if (normalized.isNotBlank() && yielded.add(normalized)) {
                out.add(normalized)
            }
        }

        emit(base)
        emit(base.replace('_', ' '))

        if (base.endsWith(" m")) {
            emit(base.removeSuffix(" m"))
        } else {
            emit("$base m")
        }

        emit(base.replace("swampland", "swamp"))
        emit(base.replace("extreme hills edge", "high hills edge"))
        emit(base.replace("extreme hills", "high hills"))
        emit(base.replace("extreme hills edge", "mountains edge"))
        emit(base.replace("extreme hills", "mountains"))

        if (base.startsWith("mesa")) {
            emit("$base m")
        }
        if (base == "savanna" || base == "savanna plateau") {
            emit("$base m")
        }
        if (base == "cold taiga" || base == "cold taiga hills" || base == "mega taiga" || base == "mega taiga hills") {
            emit("$base m")
        }
        if (base == "desert hills") {
            emit("desert")
        }
        return out
    }

    private fun isSnowBiome(biome: BiomeGenBase?, x: Int, y: Int, z: Int): Boolean {
        if (biome == null) {
            return false
        }
        if (biome.enableSnow) {
            return true
        }
        return biome.safeTemperature(x, y, z) <= 0.15f
    }

    private fun BiomeGenBase.safeTemperature(x: Int, y: Int, z: Int): Float {
        return try {
            getFloatTemperature(x, y, z)
        } catch (_: Exception) {
            0.8f
        }
    }

    @JvmStatic
    fun dayBlendForWorldTicks(worldTime: Long): Double {
        val dayTime = normalizeDayTicks(worldTime)
        return when {
            dayTime < 10_000.0 -> interpolateSmooth(0.05, 1.0, dayTime / 10_000.0)
            dayTime < 12_000.0 -> interpolateSmooth(1.0, 0.9, (dayTime - 10_000.0) / 2_000.0)
            dayTime < 18_000.0 -> interpolateSmooth(0.9, 0.35, (dayTime - 12_000.0) / 6_000.0)
            dayTime < 23_000.0 -> interpolateSmooth(0.35, 0.0, (dayTime - 18_000.0) / 5_000.0)
            else -> interpolateSmooth(0.0, 0.05, (dayTime - 23_000.0) / 1_000.0)
        }.coerceIn(0.0, 1.0)
    }

    @JvmStatic
    fun interpolateTemperature(dayHighCelsius: Double, nightLowCelsius: Double, dayBlend: Double): Double {
        val blend = dayBlend.coerceIn(0.0, 1.0)
        return nightLowCelsius + (dayHighCelsius - nightLowCelsius) * blend
    }

    @JvmStatic
    fun interpolateHumidity(dayHumidityPercent: Double, nightHumidityPercent: Double, dayBlend: Double): Double {
        val blend = dayBlend.coerceIn(0.0, 1.0)
        return nightHumidityPercent + (dayHumidityPercent - nightHumidityPercent) * blend
    }

    @JvmStatic
    fun applyHumidityBoost(baseHumidityPercent: Double, precipitationType: String, isRaining: Boolean, isThundering: Boolean): Double {
        if (!isRaining) {
            return baseHumidityPercent.coerceIn(0.0, 100.0)
        }

        var humidity = baseHumidityPercent
        humidity += if (precipitationType == "snow") SNOW_HUMIDITY_BOOST_PERCENT else RAIN_HUMIDITY_BOOST_PERCENT
        if (isThundering) {
            humidity += THUNDER_HUMIDITY_BOOST_PERCENT
        }

        val weatherMinimum = when {
            isThundering -> MIN_THUNDER_HUMIDITY_PERCENT
            precipitationType == "snow" -> MIN_SNOW_HUMIDITY_PERCENT
            else -> MIN_RAIN_HUMIDITY_PERCENT
        }
        humidity = humidity.coerceAtLeast(weatherMinimum)
        return humidity.coerceIn(0.0, 100.0)
    }

    @JvmStatic
    fun applyCloudTemperatureDelta(baseTemperatureCelsius: Double, isRaining: Boolean, isThundering: Boolean): Double {
        if (!isRaining) {
            return baseTemperatureCelsius
        }
        var adjusted = baseTemperatureCelsius + RAIN_CLOUD_TEMP_DELTA_C
        if (isThundering) {
            adjusted += THUNDER_CLOUD_TEMP_DELTA_C
        }
        return adjusted
    }

    private fun ensureLoaded() {
        if (loaded) {
            return
        }
        synchronized(this) {
            if (loaded) {
                return
            }
            loadProfiles()
            loaded = true
        }
    }

    private fun getWeatherSnapshot(world: World): WeatherSnapshot {
        val dimension = world.provider.dimensionId
        val tick = world.totalWorldTime

        synchronized(this) {
            val cached = weatherByDimension[dimension]
            if (cached != null && cached.tick == tick) {
                return cached
            }

            val snapshot = WeatherSnapshot(
                tick = tick,
                raining = world.isRaining,
                thundering = world.isThundering
            )
            weatherByDimension[dimension] = snapshot
            return snapshot
        }
    }

    private fun normalizeDayTicks(worldTime: Long): Double {
        var dayTime = worldTime % TICKS_PER_DAY.toLong()
        if (dayTime < 0L) {
            dayTime += TICKS_PER_DAY.toLong()
        }
        return dayTime.toDouble()
    }

    private fun interpolateSmooth(start: Double, end: Double, rawT: Double): Double {
        val t = rawT.coerceIn(0.0, 1.0)
        val smooth = t * t * (3.0 - 2.0 * t)
        return start + (end - start) * smooth
    }

    private fun loadProfiles() {
        val path = "assets/eln/biomes.json"
        val stream = javaClass.classLoader.getResourceAsStream(path)
        if (stream == null) {
            Eln.logger.warn("Biome climate file not found at '{}'; falling back to generated climate curves.", path)
            return
        }

        InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
            try {
                val root = JsonParser().parse(reader)
                if (!root.isJsonArray) {
                    Eln.logger.warn("Biome climate file '{}' has invalid format (expected array).", path)
                    return
                }

                root.asJsonArray.forEach { entry ->
                    if (!entry.isJsonObject) {
                        return@forEach
                    }
                    val obj = entry.asJsonObject
                    val biomeNames = linkedSetOf<String>()
                    obj.stringArray("Biomes")
                        .filter { it.isNotBlank() }
                        .forEach { biomeNames.add(it) }
                    obj.string("Biome")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { biomeNames.add(it) }
                    if (biomeNames.isEmpty()) {
                        return@forEach
                    }

                    val profile = BiomeClimateProfile(
                        dayHighCelsius = obj.double("DayHigh_C", 22.0),
                        nightLowCelsius = obj.double("NightLow_C", 12.0),
                        dayHumidityPercent = obj.double("DayRH_%", 50.0).coerceIn(0.0, 100.0),
                        nightHumidityPercent = obj.double("NightRH_%", 70.0).coerceIn(0.0, 100.0),
                        precipitationType = normalizePrecipitation(obj.string("Precipitation") ?: "none")
                    )
                    biomeNames.forEach { biomeName ->
                        profilesByBiomeName[normalizeKey(biomeName)] = profile
                    }
                }

                Eln.logger.info("Loaded {} biome climate profiles from {}.", profilesByBiomeName.size, path)
            } catch (e: Exception) {
                Eln.logger.error("Failed to load biome climate profiles from {}.", path, e)
            }
        }
    }

    private fun normalizePrecipitation(value: String): String {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "rain" -> "rain"
            "snow" -> "snow"
            else -> "none"
        }
    }

    private fun normalizeKey(biomeName: String): String {
        return biomeName.trim().lowercase(Locale.ROOT)
    }

    private fun JsonObject.string(key: String): String? {
        val value = get(key) ?: return null
        return try {
            if (value.isJsonNull) null else value.asString
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.double(key: String, fallback: Double): Double {
        val value = get(key) ?: return fallback
        return try {
            if (value.isJsonNull) fallback else value.asDouble
        } catch (_: Exception) {
            fallback
        }
    }

    private fun JsonObject.stringArray(key: String): List<String> {
        val value = get(key) ?: return emptyList()
        return try {
            if (!value.isJsonArray) {
                emptyList()
            } else {
                value.asJsonArray.mapNotNull {
                    try {
                        if (it.isJsonNull) null else it.asString
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private data class WeatherSnapshot(
    val tick: Long,
    val raining: Boolean,
    val thundering: Boolean
)

private data class BiomeClimateProfile(
    val dayHighCelsius: Double,
    val nightLowCelsius: Double,
    val dayHumidityPercent: Double,
    val nightHumidityPercent: Double,
    val precipitationType: String
) {
}
