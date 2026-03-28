package mods.eln.config

import com.google.gson.JsonParser
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
}
