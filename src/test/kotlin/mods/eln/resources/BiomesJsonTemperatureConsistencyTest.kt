package mods.eln.resources

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mods.eln.disableLog4jJmx
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class BiomesJsonTemperatureConsistencyTest {
    @Test
    fun celsiusValuesAlignWithFahrenheitValues() {
        disableLog4jJmx()

        val jsonPath = Paths.get("src/main/resources/assets/eln/biomes.json")
        assertTrue(Files.exists(jsonPath), "Missing biome JSON: $jsonPath")

        val root = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8).use { reader ->
            JsonParser().parse(reader).asJsonArray
        }
        val warnings = mutableListOf<String>()
        val failures = mutableListOf<String>()

        for (entry in root) {
            val obj = entry.asJsonObject
            val biomeName = obj.get("Biome")?.asString ?: "<unknown>"
            checkPair(obj, biomeName, "DayHigh_C", "DayHigh_F", warnings, failures)
            checkPair(obj, biomeName, "NightLow_C", "NightLow_F", warnings, failures)
        }

        if (warnings.isNotEmpty()) {
            System.err.println(
                "Biome temperature conversion warnings (> $WARN_DELTA_F F):\n" +
                    warnings.joinToString(separator = "\n"),
            )
        }

        assertTrue(
            failures.isEmpty(),
            "Biome temperature conversion failures (> $FAIL_DELTA_F F):\n${failures.joinToString(separator = "\n")}",
        )
    }

    private fun checkPair(
        obj: JsonObject,
        biomeName: String,
        cKey: String,
        fKey: String,
        warnings: MutableList<String>,
        failures: MutableList<String>,
    ) {
        val c = obj.get(cKey)?.asDouble ?: run {
            failures.add("$biomeName missing $cKey")
            return
        }
        val f = obj.get(fKey)?.asDouble ?: run {
            failures.add("$biomeName missing $fKey")
            return
        }

        val expectedF = c * 9.0 / 5.0 + 32.0
        val delta = abs(expectedF - f)
        val message = "$biomeName $cKey/$fKey expected=${"%.2f".format(expectedF)} actual=${"%.2f".format(f)} delta=${"%.2f".format(delta)}"

        when {
            delta > FAIL_DELTA_F -> failures.add(message)
            delta > WARN_DELTA_F -> warnings.add(message)
        }
    }

    companion object {
        private const val WARN_DELTA_F = 0.25
        private const val FAIL_DELTA_F = 1.0
    }
}
