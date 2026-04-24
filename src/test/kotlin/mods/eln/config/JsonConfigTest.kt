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
    fun writesExampleFileUsingCurrentDefaultModel() {
        val dir = Files.createTempDirectory("eln-json-example-test").toFile()
        val config = JsonConfig(dir.resolve("Eln.cfg"))
        config.load()
        config.setBoolean("ui.icons.noSymbols", true)
        config.save()
        config.writeExampleFile()

        FileReader(dir.resolve("eln/eln.example.json")).use { reader ->
            val root = JsonParser().parse(reader).asJsonObject
            assertFalse(root.getAsJsonObject("ui").getAsJsonObject("icons").get("noSymbols").asBoolean)
        }
    }

    @Test
    fun fuelRegistryEntriesLiveInJsonConfigWithComments() {
        val dir = Files.createTempDirectory("eln-json-fuels-test").toFile()
        val baseConfigFile = dir.resolve("Eln.cfg")
        val config = JsonConfig(baseConfigFile)
        Eln.config = config
        config.load()
        FuelRegistry.init(baseConfigFile)

        FileReader(dir.resolve("eln/fluids.json")).use { reader ->
            val root = JsonParser().parse(reader).asJsonObject
            val diesel = root.getAsJsonObject("fuels")
                .getAsJsonObject("diesel")
            val creosote = diesel.getAsJsonObject("creosote")
            assertEquals(750000.0, creosote.get("heatValue").asDouble)
            assertEquals(0.5, creosote.get("temperatureFactor").asDouble)
            assertEquals(0.3, creosote.get("cleanlinessFactor").asDouble)
            assertTrue(creosote.get("_comment").asString.isNotBlank())
            assertEquals(0.0000675, root.get("heatValueFactor").asDouble)
            val gasoline = root.getAsJsonObject("fuels")
                .getAsJsonObject("gasoline")
                .getAsJsonObject("gasoline")
            assertEquals(0.55, gasoline.get("temperatureFactor").asDouble)
            assertEquals(0.10, gasoline.get("cleanlinessFactor").asDouble)
        }

        assertTrue(FuelRegistry.dieselList.contains("creosote"))
        assertTrue(FuelRegistry.steamList.contains("steam"))
        assertEquals(0.55, FuelRegistry.temperatureFactor("gasoline"))
        assertEquals(0.10, FuelRegistry.cleanlinessFactor("gasoline"))
        assertEquals(0.0000675 * 750000.0, FuelRegistry.heatEnergyPerMilliBucket("creosote"))
        assertEquals(null, config.readPath("balance.heat.fuelHeatValueFactor"))

        FileReader(dir.resolve("eln/fluids.example.json")).use { reader ->
            val root = JsonParser().parse(reader).asJsonObject
            assertEquals(0.0000675, root.get("heatValueFactor").asDouble)
        }
    }

    @Test
    fun fuelRegistryWritesSeparateFluidsConfigFile() {
        val dir = Files.createTempDirectory("eln-json-fuel-migration-test").toFile()
        val baseConfigFile = dir.resolve("Eln.cfg")
        val config = JsonConfig(baseConfigFile)
        Eln.config = config
        config.load()
        FuelRegistry.init(baseConfigFile)

        FileReader(dir.resolve("eln/fluids.json")).use { reader ->
            val root = JsonParser().parse(reader).asJsonObject
            val creosote = root.getAsJsonObject("fuels")
                .getAsJsonObject("diesel")
                .getAsJsonObject("creosote")
            assertEquals(750000.0, creosote.get("heatValue").asDouble)
            assertEquals(0.5, creosote.get("temperatureFactor").asDouble)
            assertEquals(0.3, creosote.get("cleanlinessFactor").asDouble)
        }
        assertEquals(null, config.readPath("balance.heat.fuelHeatValueFactor"))
    }

    @Test
    fun mapConfigDefaultWritesToJsonAndReloads() {
        val dir = Files.createTempDirectory("eln-map-config-test").toFile()
        val config = JsonConfig(dir.resolve("Eln.cfg"))
        config.load()

        val map = config.getStringDoubleMap("tools.xrayScanner.oreFactors")
        assertTrue(map.isNotEmpty(), "Expected default oreFactors map to be populated.")
        assertEquals(0.05, map["minecraft:coal_ore"])
        assertEquals(1.00, map["minecraft:diamond_ore"])

        config.save()

        val reloaded = JsonConfig(dir.resolve("Eln.cfg"))
        reloaded.load()
        val reloadedMap = reloaded.getStringDoubleMap("tools.xrayScanner.oreFactors")
        assertEquals(map.keys.toList(), reloadedMap.keys.toList(), "Key order should be preserved after reload.")
        for ((key, value) in map) {
            assertEquals(value, reloadedMap[key]!!, 1e-9, "Value for key '$key' should match after reload.")
        }
    }

    @Test
    fun getStringDoubleMapPreservesKeyOrder() {
        val dir = Files.createTempDirectory("eln-map-order-test").toFile()
        val config = JsonConfig(dir.resolve("Eln.cfg"))
        config.load()

        val map = config.getStringDoubleMap("tools.xrayScanner.oreFactors")
        val keys = map.keys.toList()
        assertTrue(keys.indexOf("minecraft:coal_ore") < keys.indexOf("minecraft:iron_ore"))
        assertTrue(keys.indexOf("minecraft:diamond_ore") < keys.indexOf("Eln:Eln.Ore:1"))
    }

    @Test
    fun invalidMapValuesDoNotResetUnrelatedPaths() {
        val dir = Files.createTempDirectory("eln-map-invalid-test").toFile()
        val config = JsonConfig(dir.resolve("Eln.cfg"))
        config.load()
        config.save()

        val jsonFile = dir.resolve("eln/eln.json")
        val originalText = jsonFile.readText()
        val tamperedText = originalText.replace(
            Regex("\"minecraft:coal_ore\"\\s*:\\s*[0-9.]+"),
            "\"minecraft:coal_ore\": \"not_a_number\""
        )
        jsonFile.writeText(tamperedText)

        val reloaded = JsonConfig(dir.resolve("Eln.cfg"))
        reloaded.load()

        val map = reloaded.getStringDoubleMap("tools.xrayScanner.oreFactors")
        assertEquals(0.0, map["minecraft:coal_ore"], "Invalid entry should default to 0.0 instead of crashing.")
        assertEquals(1.00, map["minecraft:diamond_ore"], "Unrelated entries should not be affected.")
        assertTrue(reloaded.getBooleanOrElse("tools.xrayScanner.autoDiscoverOreDictionaryOres", false))
        assertEquals(5.0, reloaded.getDoubleOrElse("tools.xrayScanner.rangeBlocks", 0.0), 1e-9)
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
