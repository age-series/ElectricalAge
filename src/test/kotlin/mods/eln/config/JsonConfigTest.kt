package mods.eln.config

import com.google.gson.JsonParser
import mods.eln.Eln
import mods.eln.fluid.FuelRegistry
import java.io.FileReader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonConfigTest {
    @Test
    fun migratesLegacyCfgIntoNestedJson() {
        val dir = Files.createTempDirectory("eln-json-config-test").toFile()
        val legacyCfg = dir.resolve("Eln.cfg")

        legacyCfg.writeText(
            """
            modbus {
                B:enable=true
                I:port=1602
            }

            mapgenerate {
                B:cooper=false
            }
            """.trimIndent()
        )

        val config = JsonConfig(legacyCfg)
        config.load()

        val jsonFile = dir.resolve("eln/eln.json")
        val migratedCfgFile = dir.resolve("eln/eln.migrated.cfg")
        assertTrue(jsonFile.exists(), "Expected migrated JSON config to be written next to the legacy cfg.")
        assertTrue(migratedCfgFile.exists(), "Expected the legacy cfg to be archived after migration.")
        assertFalse(legacyCfg.exists(), "Expected the original cfg to be moved into the archive location.")
        assertTrue(config.getBooleanOrElse("integrations.modbus.enabled", false))
        assertEquals(1602, config.getIntOrElse("integrations.modbus.port", 1502))
        assertFalse(config.getBooleanOrElse("worldgen.ores.copper.enabled", true))

        FileReader(jsonFile).use { reader ->
            val root = JsonParser().parse(reader).asJsonObject
            assertTrue(root.getAsJsonObject("integrations").getAsJsonObject("modbus").get("enabled").asBoolean)
            assertEquals(1602, root.getAsJsonObject("integrations").getAsJsonObject("modbus").get("port").asInt)
            assertFalse(
                root.getAsJsonObject("worldgen")
                    .getAsJsonObject("ores")
                    .getAsJsonObject("copper")
                    .get("enabled")
                    .asBoolean
            )
        }
    }

    @Test
    fun legacyCategoryAliasesShareCanonicalJsonPath() {
        val dir = Files.createTempDirectory("eln-json-config-alias-test").toFile()
        val config = JsonConfig(dir.resolve("Eln.cfg"))
        config.load()

        config.setBoolean("ui.icons.noSymbols", true)
        config.save()

        val reloaded = JsonConfig(dir.resolve("Eln.cfg"))
        reloaded.load()

        assertTrue(reloaded.getBooleanOrElse("ui.icons.noSymbols", false))

        FileReader(dir.resolve("eln/eln.json")).use { reader ->
            val root = JsonParser().parse(reader).asJsonObject
            assertTrue(root.getAsJsonObject("ui").getAsJsonObject("icons").get("noSymbols").asBoolean)
        }
    }

    @Test
    fun fuelRegistryEntriesLiveInJsonConfigWithComments() {
        val dir = Files.createTempDirectory("eln-json-fuels-test").toFile()
        val config = JsonConfig(dir.resolve("Eln.cfg"))
        Eln.config = config
        FuelRegistry.registerConfigEntries(config)
        config.load()
        config.save()

        FileReader(dir.resolve("eln/eln.json")).use { reader ->
            val root = JsonParser().parse(reader).asJsonObject
            val diesel = root.getAsJsonObject("balance")
                .getAsJsonObject("heat")
                .getAsJsonObject("fuels")
                .getAsJsonObject("diesel")
            val creosote = diesel.getAsJsonObject("creosote")
            assertEquals(750000.0, creosote.get("heatValue").asDouble)
            assertEquals(0.5, creosote.get("temperatureFactor").asDouble)
            assertEquals(0.3, creosote.get("cleanlinessFactor").asDouble)
            assertTrue(creosote.get("_comment").asString.isNotBlank())
            val gasoline = root.getAsJsonObject("balance")
                .getAsJsonObject("heat")
                .getAsJsonObject("fuels")
                .getAsJsonObject("gasoline")
                .getAsJsonObject("gasoline")
            assertEquals(0.55, gasoline.get("temperatureFactor").asDouble)
            assertEquals(0.10, gasoline.get("cleanlinessFactor").asDouble)
        }

        assertTrue(FuelRegistry.dieselList.contains("creosote"))
        assertTrue(FuelRegistry.steamList.contains("steam"))
        assertEquals(0.55, FuelRegistry.temperatureFactor("gasoline"))
        assertEquals(0.10, FuelRegistry.cleanlinessFactor("gasoline"))
        assertEquals(
            config.getDoubleOrElse("balance.heat.fuelHeatValueFactor", 0.0000675) * 750000.0,
            FuelRegistry.heatEnergyPerMilliBucket("creosote")
        )
    }

    @Test
    fun regexReadAndWriteWorkForConfigPaths() {
        val dir = Files.createTempDirectory("eln-json-regex-test").toFile()
        val config = JsonConfig(dir.resolve("Eln.cfg"))
        Eln.config = config
        config.load()

        val lampPaths = config.readPathsMatching("lighting.lamps.*.infiniteLifeEnabled")
        assertTrue(lampPaths.isNotEmpty())
        assertTrue(lampPaths.values.all { it is Boolean })

        val updatedPaths = config.writePathsMatching(
            "lighting.lamps.*.infiniteLifeEnabled",
            "true"
        )
        assertEquals(lampPaths.keys.toSet(), updatedPaths.toSet())
        assertTrue(updatedPaths.all { config.getBooleanOrElse(it, false) })
    }
}
