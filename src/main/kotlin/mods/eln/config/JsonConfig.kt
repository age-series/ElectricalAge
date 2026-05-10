package mods.eln.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mods.eln.Other
import mods.eln.item.TurbineBladeLists
import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.item.lampitem.LampLists
import mods.eln.misc.Utils
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.UUID
import java.util.regex.PatternSyntaxException
import kotlin.math.max
import kotlin.math.min

private data class LegacyKey(val category: String, val key: String)

private data class ConfigSpec(
    val path: List<String>,
    val defaultValue: Any,
    val comment: String? = null
)

/**
 * JSON-backed configuration store for Electrical Age.
 *
 * To add a new persisted configuration entry:
 * 1. Add a new `spec(...)` entry in [buildSpecs] using the canonical JSON path, default value, and optional comment.
 * 2. Read or write the value through the typed path helpers such as [getBooleanOrElse], [getIntOrElse],
 *    [getDoubleOrElse], [getStringOrElse], [setBoolean], [setInt], [setDouble], or [setString].
 * 3. If the value is derived at runtime and should not be persisted, cache it under a `runtime.*` path instead.
 */
class JsonConfig @JvmOverloads constructor(
    private val legacyCfgFile: File,
    private val includeDefaultSpecs: Boolean = true
) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val configDirectory = legacyCfgFile.parentFile?.let {
        if (it.name.equals("eln", ignoreCase = true)) it else File(it, "eln")
    } ?: File("config/eln")
    private val baseName = legacyCfgFile.nameWithoutExtension.lowercase(Locale.ROOT)
    private val jsonFile = File(configDirectory, "$baseName.json")
    private val exampleJsonFile = File(configDirectory, "$baseName.example.json")
    private val migratedCfgFile = File(configDirectory, "$baseName.migrated.cfg")
    private val values = linkedMapOf<String, Any>()
    private val runtimeValues = linkedMapOf<String, Any>()
    private val pathComments = linkedMapOf<String, String>()
    private val groupComments = linkedMapOf<String, String>()
    private val specs = if (includeDefaultSpecs) buildSpecs().toMutableList() else mutableListOf()
    private val mapPaths = mutableSetOf<String>()

    private fun registerStaticComments() {
        groupComments["integrations"] = "Cross-mod integrations and external protocol bridges."
        groupComments["debug"] = "Debug logging and audio diagnostics."
        groupComments["gameplay"] = "Gameplay toggles and seasonal behavior."
        groupComments["updates"] = "Version check behavior."
        groupComments["analytics"] = "Anonymous usage telemetry settings."
        groupComments["world"] = "World interaction toggles and pole placement behavior."
        groupComments["balance"] = "Balance multipliers used across machines and recipes."
        groupComments["items"] = "Item-specific tuning values."
        groupComments["entities"] = "Mob spawning and replicator behavior."
        groupComments["worldgen"] = "Ore generation controls."
        groupComments["machines"] = "Machine-specific settings."
        groupComments["tools"] = "Tool behavior settings."
        groupComments["simulation"] = "Electrical, thermal, and room simulation settings."
        groupComments["wireless"] = "Wireless range settings."
        groupComments["ui"] = "HUD and item icon presentation."
        groupComments["lighting"] = "Lamp technology settings generated from registered lamp types."
        groupComments["lighting.lamps"] = "Per-lamp-type durability tuning."
    }

    private fun buildSpecs(): List<ConfigSpec> = listOf(
        spec(path = "integrations.modbus.enabled", defaultValue = false, comment = "Enable Modbus RTU."),
        spec(path = "integrations.modbus.port", defaultValue = 1502, comment = "TCP port for Modbus RTU."),
        spec(path = "integrations.mqtt.enabled", defaultValue = false, comment = "Enable MQTT devices. Server endpoints live in config/eln/mqtt.json."),
        spec(path = "integrations.computerProbe.enabled", defaultValue = true, comment = "Enable the OC/CC to Eln computer probe."),
        spec(path = "integrations.energyExporter.enabled", defaultValue = true, comment = "Enable the Eln energy exporter."),
        spec(path = "integrations.oredict.tungstenEnabled", defaultValue = false, comment = "Use shared ore dictionary entries for tungsten."),
        spec(path = "integrations.oredict.chipsEnabled", defaultValue = true, comment = "Use shared ore dictionary entries for chips."),
        spec(path = "debug.logging.enabled", defaultValue = false, comment = "Enable debug printing spam."),
        spec(path = "debug.logging.simSnapshot", defaultValue = false, comment = "Enable circuit snapshot logging. Requires debug.logging.enabled."),
        spec(path = "ui.audio.maxSoundDistance", defaultValue = 16.0, comment = "Lower this if you hear clipping in dense generator rooms."),
        spec(path = "ui.audio.soundChannels", defaultValue = 200, comment = "Override the number of sound channels. Use -1 for the Minecraft default."),
        spec(path = "gameplay.hazards.explosionsEnabled", defaultValue = false, comment = "Make explosions a bit bigger."),
        spec(path = "gameplay.seasonal.enableFestiveItems", defaultValue = true, comment = "Set false to disable festive items."),
        spec(path = "gameplay.crafting.verticalIronCableCrafting", defaultValue = false, comment = "Craft iron cables with vertical ingots instead of horizontal ones."),
        spec(path = "gameplay.cables.creativeFreeLength", defaultValue = true, comment = "When enabled, creative players do not consume utility cable length when placing cables or connecting poles."),
        spec(path = "simulation.watchdog.destruction.thermal", defaultValue = true, comment = "Allow thermal watchdogs to destroy blocks except resistor heat watchdogs."),
        spec(path = "simulation.watchdog.destruction.resistorHeat", defaultValue = false, comment = "Allow resistor heat watchdogs to destroy blocks."),
        spec(path = "simulation.watchdog.destruction.current", defaultValue = true, comment = "Allow current watchdogs to destroy blocks."),
        spec(path = "simulation.watchdog.destruction.voltage", defaultValue = true, comment = "Allow voltage watchdogs to destroy blocks."),
        spec(path = "simulation.watchdog.destruction.shaftSpeed", defaultValue = true, comment = "Allow shaft speed watchdogs to destroy blocks."),
        spec(path = "simulation.watchdog.destruction.other", defaultValue = true, comment = "Allow all other watchdog types to destroy blocks."),
        spec(path = "updates.versionCheck.enabled", defaultValue = true, comment = "Enable the version checker."),
        spec(path = "analytics.enabled", defaultValue = true, comment = "Enable Electrical Age analytics."),
        spec(path = "analytics.endpointUrl", defaultValue = "http://eln.ja13.org/stat", comment = "Analytics endpoint URL."),
        spec(path = "analytics.playerUuidOptIn", defaultValue = false, comment = "Opt into sending the player UUID with analytics."),
        spec(path = "analytics.playerUuid", defaultValue = "", comment = "Locally generated UUID used when analytics.playerUuidOptIn is enabled."),
        spec(path = "world.poles.directPlacementEnabled", defaultValue = true, comment = "Enable direct air-to-ground pole placement."),
        spec(path = "balance.generators.heatTurbinePowerFactor", defaultValue = 1.0),
        spec(path = "balance.generators.solarPanelPowerFactor", defaultValue = 1.0),
        spec(path = "balance.generators.windTurbinePowerFactor", defaultValue = 1.0),
        spec(path = "balance.generators.waterTurbinePowerFactor", defaultValue = 1.0),
        spec(path = "balance.generators.fuelGeneratorPowerFactor", defaultValue = 1.0),
        spec(path = "balance.generators.fuelHeatFurnacePowerFactor", defaultValue = 1.0),
        spec(path = "machines.autominer.maxRangeBlocks", defaultValue = 10, comment = "Maximum horizontal distance from the autominer that will be mined."),
        spec(path = "balance.integrationConversion.wattsToEu", defaultValue = 1.0 / 3.0, comment = "Watts to EU conversion ratio."),
        spec(path = "balance.integrationConversion.wattsToOc", defaultValue = 1.0 / 3.0 / 2.5, comment = "Watts to OpenComputers power conversion ratio."),
        spec(path = "balance.integrationConversion.wattsToRf", defaultValue = 1.0 / 3.0 * 4.0, comment = "Watts to RF conversion ratio."),
        spec(path = "balance.materials.platesPerIngot", defaultValue = 1, comment = "Plates made per ingot."),
        spec(path = "balance.mechanics.shaftEnergyFactor", defaultValue = 0.05),
        spec(path = "items.batteries.standardHalfLifeDays", defaultValue = 2.0, comment = "Number of Minecraft days for a standard battery to decay halfway."),
        spec(path = "items.batteries.infinite.standard", defaultValue = false, comment = "Disable wear-out for standard batteries."),
        spec(path = "items.batteries.infinite.portable", defaultValue = false, comment = "Disable wear-out for portable batteries."),
        spec(path = "balance.storage.batteryCapacityFactor", defaultValue = 1.0),
        spec(path = "entities.replicator.enabled", defaultValue = false, comment = "Enable the replicator mob."),
        spec(path = "entities.replicator.thunderSpawnPerSecondPerPlayer", defaultValue = 1.0 / 120.0),
        spec(path = "entities.replicator.entityId", defaultValue = -1),
        spec(path = "entities.mobSpawning.preventNearLamps", defaultValue = true, comment = "Prevent monsters from spawning near lamps."),
        spec(path = "entities.mobSpawning.preventNearLampsRange", defaultValue = 9),
        spec(path = "entities.replicator.maxCount", defaultValue = 100),
        spec(path = "worldgen.ores.copper.enabled", defaultValue = true),
        spec(path = "worldgen.ores.lead.enabled", defaultValue = true),
        spec(path = "worldgen.ores.tungsten.enabled", defaultValue = true),
        spec(path = "worldgen.ores.cinnabar.enabled", defaultValue = true),
        spec(path = "machines.fuelGenerator.tankCapacitySecondsAtNominalPower", defaultValue = 20.0 * 60.0),
        spec(path = "machines.heatFurnace.consumeFuel", defaultValue = true, comment = "Controls whether heat furnaces consume fuel."),
        spec(path = "tools.xrayScanner.autoDiscoverOreDictionaryOres", defaultValue = true, comment = "Auto-discover ores from the Ore Dictionary that are not in oreFactors."),
        spec(path = "tools.xrayScanner.rangeBlocks", defaultValue = 5.0, comment = "X-ray scanner range in blocks. Intended range is 4 to 10."),
        spec(path = "tools.xrayScanner.canBeCrafted", defaultValue = true),
        spec(path = "tools.xrayScanner.autoDiscoveryOreFactor", defaultValue = 0.15, comment = "Default factor for auto-discovered ores not listed in oreFactors."),
        spec(
            path = "tools.xrayScanner.oreFactors",
            defaultValue = linkedMapOf(
                "minecraft:coal_ore" to 0.05,
                "minecraft:iron_ore" to 0.15,
                "minecraft:gold_ore" to 0.40,
                "minecraft:lapis_ore" to 0.40,
                "minecraft:redstone_ore" to 0.40,
                "minecraft:diamond_ore" to 1.00,
                "minecraft:emerald_ore" to 0.40,
                "Eln:Eln.Ore:1" to 0.10,
                "Eln:Eln.Ore:4" to 0.20,
                "Eln:Eln.Ore:5" to 0.20,
                "Eln:Eln.Ore:6" to 0.20
            ),
            comment = "Ore scanner detection factors. Keys with ':' are block references (modid:name or modid:name:meta), keys without ':' are OreDictionary names. The ELN ores at the top are, in order: Copper Ore, Lead Ore, Tungsten Ore, and Cinnabar Ore."
        ),
        spec(path = "simulation.electrical.frequency", defaultValue = 20.0, comment = "Set to a clean divisor of 20."),
        spec(path = "simulation.electrical.interSystemOverSampling", defaultValue = 50, comment = "Avoid setting this below 50."),
        spec(path = "simulation.thermal.frequency", defaultValue = 400.0, comment = "Thermal simulation update frequency."),
        spec(path = "simulation.roomDetection.maxAxisSpanBlocks", defaultValue = 24, comment = "Maximum room span on any axis for room detection."),
        spec(path = "simulation.roomDetection.maxVolumeBlocks", defaultValue = 4096, comment = "Maximum enclosed-air room volume for room detection."),
        spec(path = "wireless.transmitter.maxRangeBlocks", defaultValue = 32, comment = "Maximum range for wireless transmitters and lamp supplies."),
        spec(path = "ui.waila.easyMode", defaultValue = false, comment = "Display more detailed WAILA info on some machines."),
        spec(path = "balance.cables.powerFactor", defaultValue = 1.0, comment = "Multiplier for cable power capacity."),
        spec(path = "simulation.thermal.cableSpikeLimiter.enabled", defaultValue = true, comment = "Clamp how fast cables and transformers heat up."),
        spec(path = "simulation.thermal.cableSpikeLimiter.factor", defaultValue = 20.0, comment = "Multiplier applied to nominal heating rate when the cable spike limiter is enabled."),
        spec(path = "simulation.thermal.ambient.lavaRampEnabled", defaultValue = true, comment = "Blend ambient temperature toward 40C between Y20 and Y12, then clamp below Y12."),
        spec(path = "simulation.thermal.ambient.undergroundBiomeTemperatureMultiplier", defaultValue = 0.2, comment = "Multiplier applied to biome ambient temperature for the Y50 underground baseline."),
        spec(path = "ui.icons.noSymbols", defaultValue = false, comment = "Show the item instead of the electrical symbol as an icon."),
        spec(path = "ui.icons.noVoltageBackground", defaultValue = false, comment = "Disable colored backgrounds for items."),
        spec(path = "balance.mechanics.flywheelMass", defaultValue = 50.0, comment = "Flywheel mass.")
    )

    private fun spec(
        path: String,
        defaultValue: Any,
        comment: String? = null
    ) = ConfigSpec(path.split('.'), defaultValue, comment)

    init {
        if (includeDefaultSpecs) {
            registerStaticComments()
        }
        registerSpecComments()
        populateMapPaths()
    }

    fun load() {
        values.clear()
        runtimeValues.clear()
        populateDefaults()

        when {
            jsonFile.exists() -> {
                try {
                    loadFromJson()
                    archiveLegacyCfg()
                } catch (_: Exception) {
                    values.clear()
                    populateDefaults()
                }
            }
            legacyCfgFile.exists() -> {
                loadFromLegacyCfg()
                save()
                archiveLegacyCfg()
                return
            }
        }

        populateDefaults()
    }

    fun save() {
        populateDefaults()
        jsonFile.parentFile?.mkdirs()
        FileWriter(jsonFile, false).use { writer ->
            gson.toJson(buildJsonTree(), writer)
        }
    }

    /**
     * Writes an example JSON file containing the latest default schema for comparison during updates.
     */
    @Suppress("unused")
    fun writeExampleFile() {
        exampleJsonFile.parentFile?.mkdirs()
        FileWriter(exampleJsonFile, false).use { writer ->
            gson.toJson(buildJsonTree(buildDefaultValues()), writer)
        }
    }

    fun loadConfig() {
        load()

        LampLists.loadAllLampConfigs()
        for (bladeData in TurbineBladeLists.bladeConfigList) bladeData.loadConfig()

        val analyticsEnabled = getBooleanOrElse("analytics.enabled", true)
        if (analyticsEnabled) {
            val path = "analytics.playerUuid"
            if (getStringOrElse(path, "").isEmpty()) {
                setString(path, UUID.randomUUID().toString())
            }
        }

        Other.wattsToEu = getDoubleOrElse("balance.integrationConversion.wattsToEu", 1.0 / 3.0)
        Other.wattsToOC = getDoubleOrElse("balance.integrationConversion.wattsToOc", 1.0 / 3.0 / 2.5)
        Other.wattsToRf = getDoubleOrElse("balance.integrationConversion.wattsToRf", 1.0 / 3.0 * 4)

        setRuntimeValue(
            "runtime.items.batteries.standardHalfLifeTicks",
            getDoubleOrElse("items.batteries.standardHalfLifeDays", 2.0) * Utils.minecraftDay
        )
        setRuntimeValue("runtime.worldgen.ores.cinnabar.enabled", false)

        val oredictTungsten = getBooleanOrElse("integrations.oredict.tungstenEnabled", false)
        val dictTungstenOre: String
        val dictTungstenDust: String
        val dictTungstenIngot: String
        if (oredictTungsten) {
            dictTungstenOre = "oreTungsten"
            dictTungstenDust = "dustTungsten"
            dictTungstenIngot = "ingotTungsten"
        } else {
            dictTungstenOre = "oreElnTungsten"
            dictTungstenDust = "dustElnTungsten"
            dictTungstenIngot = "ingotElnTungsten"
        }
        val oredictChips = getBooleanOrElse("integrations.oredict.chipsEnabled", true)
        val dictCheapChip: String
        val dictAdvancedChip: String
        if (oredictChips) {
            dictCheapChip = "circuitBasic"
            dictAdvancedChip = "circuitAdvanced"
        } else {
            dictCheapChip = "circuitElnBasic"
            dictAdvancedChip = "circuitElnAdvanced"
        }
        setRuntimeValue("runtime.dictionary.tungstenOre", dictTungstenOre)
        setRuntimeValue("runtime.dictionary.tungstenDust", dictTungstenDust)
        setRuntimeValue("runtime.dictionary.tungstenIngot", dictTungstenIngot)
        setRuntimeValue("runtime.dictionary.cheapChip", dictCheapChip)
        setRuntimeValue("runtime.dictionary.advancedChip", dictAdvancedChip)

        setRuntimeValue(
            "runtime.tools.xrayScanner.rangeBlocks.clamped",
            max(min(getDoubleOrElse("tools.xrayScanner.rangeBlocks", 5.0), 10.0), 4.0).toFloat()
        )
        min(max(getDoubleOrElse("balance.mechanics.flywheelMass", 50.0), 1.0), 1000.0).also {
            setRuntimeValue("runtime.balance.mechanics.flywheelMass.clamped", it)
        }

        save()
    }

    /**
     * Wildcard config paths are only possible if the parent paths contain at least three fields (example: "xx.yy.zz").
     * In this example, a wildcard path of the form "xx.*.zz" is only generated if at least two paths "xx.yy1.zz" and
     * "xx.yy2.zz" exist. Otherwise, a wildcard path is unnecessary. Note that wildcard paths can only be generated for
     * the second-to-last field in an existing path (in other words, the wildcard path "aa.*.cc.dd" is not possible).
     */
    private fun populateWildcardPathEntries(): List<String> {
        val existingKeys = values.keys.sorted().toMutableList()
        val possibleWildcardKeys = mutableListOf<String>()

        existingKeys.forEach {
            // Only proceed if path has more than one field
            if (it.lastIndexOf(".") != -1) {
                // Pick off the final field of the path
                val suffix = it.substring(it.lastIndexOf(".")..it.lastIndexOf(it.last()))
                val itNoSuffix = it.removeSuffix(suffix)

                // Only proceed if more than one field remains in the path
                if (itNoSuffix.lastIndexOf(".") != -1) {
                    // Pick off the second-to-last field of the path
                    val wildcardCandidate = itNoSuffix.substring(itNoSuffix.lastIndexOf(".")..itNoSuffix.lastIndexOf(itNoSuffix.last()))
                    val prefix = itNoSuffix.removeSuffix(wildcardCandidate)

                    // Concatenate the first field(s) and the last field, replacing the second-to-last field with the wildcard symbol (*)
                    possibleWildcardKeys.add("${prefix}.*${suffix}")
                }
            }
        }

        // This expression sorts the list of possible wildcard paths and counts duplicates. Wildcard paths are only
        // added to the final list of all paths if they occur more than once (otherwise they are pointless to have).
        possibleWildcardKeys.groupingBy { it }.eachCount().forEach { if (it.value > 1) existingKeys.add(it.key) }
        return existingKeys.sorted()
    }

    fun listPaths(prefix: String? = null): List<String> {
        populateDefaults()
        val normalizedPrefix = prefix?.trim()?.takeIf { it.isNotEmpty() }

        // Path list with wildcards is presorted
        return populateWildcardPathEntries().filter { normalizedPrefix == null || it.startsWith(normalizedPrefix, ignoreCase = true) }
    }

    fun readPath(path: String): Any? {
        return resolveValue(path.trim())
    }

    /**
     * Reads every persisted config path whose full dotted path matches [pathPattern].
     * Each dot-delimited path segment is treated as a regex segment, except `*` which expands to `.*`.
     */
    @Suppress("unused")
    fun readPathsMatching(pathPattern: String): LinkedHashMap<String, Any> {
        populateDefaults()
        val canonicalPattern = pathPattern.trim()
        val compiledPattern = try {
            compilePathPattern(canonicalPattern)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Invalid config path regex '$canonicalPattern': ${e.message}")
        }
        val result = linkedMapOf<String, Any>()
        values.keys
            .filter { it.matches(compiledPattern) }
            .sorted()
            .forEach { path -> result[path] = values.getValue(path) }
        if (result.isEmpty()) {
            throw IllegalArgumentException("No config paths matched '$canonicalPattern'")
        }
        return result
    }

    /**
     * Registers an additional persisted config entry owned by another subsystem.
     */
    @Suppress("unused")
    fun registerEntry(path: String, defaultValue: Any, comment: String? = null) {
        val canonicalPath = path.trim()
        val spec = ConfigSpec(canonicalPath.split('.'), defaultValue, comment)
        val existingIndex = specs.indexOfFirst { joinPath(it.path) == canonicalPath }
        if (existingIndex >= 0) {
            specs[existingIndex] = spec
        } else {
            specs.add(spec)
        }
        if (comment != null) {
            pathComments[canonicalPath] = comment
        } else {
            pathComments.remove(canonicalPath)
        }
        values.putIfAbsent(canonicalPath, defaultValue)
        if (defaultValue is Map<*, *>) {
            mapPaths.add(canonicalPath)
        }
        clearCollectionCaches()
    }

    /**
     * Registers an optional group-level JSON comment for a subtree path.
     */
    @Suppress("unused")
    fun registerGroupComment(path: String, comment: String) {
        groupComments[path.trim()] = comment
    }

    /**
     * Removes a single persisted config path if present.
     */
    @Suppress("unused")
    fun removePath(path: String): Boolean {
        val canonicalPath = path.trim()
        val removed = values.remove(canonicalPath) != null
        pathComments.remove(canonicalPath)
        if (removed) {
            clearCollectionCaches()
        }
        return removed
    }

    /**
     * Removes every persisted config path with the given dotted prefix.
     */
    @Suppress("unused")
    fun removePathsWithPrefix(prefix: String): List<String> {
        val canonicalPrefix = prefix.trim().trim('.')
        val fullPrefix = if (canonicalPrefix.isEmpty()) "" else "$canonicalPrefix."
        val removed = values.keys
            .filter { it == canonicalPrefix || it.startsWith(fullPrefix) }
            .sorted()
        if (removed.isEmpty()) return emptyList()
        removed.forEach {
            values.remove(it)
            pathComments.remove(it)
        }
        clearCollectionCaches()
        return removed
    }

    /**
     * Parses and writes a string value to a persisted config path, then reloads derived runtime caches.
     */
    @Suppress("unused")
    fun writePath(path: String, rawValue: String): String {
        populateDefaults()
        val canonicalPath = path.trim()
        val existing = values[canonicalPath] ?: throw IllegalArgumentException("Unknown config path '$canonicalPath'")

        // Prevent propagation of invalid lamp life values to the config file
        if (canonicalPath.startsWith("lighting.lamps") && canonicalPath.contains("nominalLifeInHours")) {
            val formattedValue = parseRawValue(rawValue, 0.0) as Double // Second argument can be any double
            if (!BoilerplateLampData.isValidNominalLife(formattedValue)) return "lighting.lamps"
        }

        values[canonicalPath] = parseRawValue(rawValue, existing)
        save()
        loadConfig()
        return formatValue(values.getValue(canonicalPath))
    }

    /**
     * Writes a value to every persisted config path whose full dotted path matches [pathPattern].
     * Each dot-delimited path segment is treated as a regex segment, except `*` which expands to `.*`.
     */
    @Suppress("unused")
    fun writePathsMatching(pathPattern: String, rawValue: String): List<String> {
        populateDefaults()
        val canonicalPattern = pathPattern.trim()
        val compiledPattern = try {
            compilePathPattern(canonicalPattern)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Invalid config path regex '$canonicalPattern': ${e.message}")
        }
        val matchedPaths = values.keys
            .filter { it.matches(compiledPattern) }
            .sorted()
        if (matchedPaths.isEmpty()) {
            throw IllegalArgumentException("No config paths matched '$canonicalPattern'")
        }

        // Prevent propagation of invalid lamp life values to the config file
        if (canonicalPattern.startsWith("lighting.lamps") && canonicalPattern.contains("nominalLifeInHours")) {
            val formattedValue = parseRawValue(rawValue, 0.0) as Double // Second argument can be any double
            if (!BoilerplateLampData.isValidNominalLife(formattedValue)) return listOf("lighting.lamps")
        }

        matchedPaths.forEach { path ->
            val existing = values.getValue(path)
            values[path] = parseRawValue(rawValue, existing)
        }
        save()
        loadConfig()
        return matchedPaths
    }

    /**
     * Reads a boolean config value from the given path, returning [defaultValue] if the path is missing or invalid.
     */
    @Suppress("unused")
    fun getBooleanOrElse(path: String, defaultValue: Boolean): Boolean = asBoolean(resolveValue(path), defaultValue)

    /**
     * Reads a boolean config value from the given path, throwing [IllegalArgumentException] with [message] if missing.
     */
    @Suppress("unused")
    fun getBooleanOrException(path: String, message: String): Boolean =
        resolveValue(path)?.let { asBoolean(it, false) } ?: throw IllegalArgumentException(message)

    /**
     * Reads an integer config value from the given path, returning [defaultValue] if the path is missing or invalid.
     */
    @Suppress("unused")
    fun getIntOrElse(path: String, defaultValue: Int): Int = asInt(resolveValue(path), defaultValue)

    /**
     * Reads an integer config value from the given path, throwing [IllegalArgumentException] with [message] if missing.
     */
    @Suppress("unused")
    fun getIntOrException(path: String, message: String): Int =
        resolveValue(path)?.let { asInt(it, 0) } ?: throw IllegalArgumentException(message)

    /**
     * Reads a double config value from the given path, returning [defaultValue] if the path is missing or invalid.
     */
    @Suppress("unused")
    fun getDoubleOrElse(path: String, defaultValue: Double): Double = asDouble(resolveValue(path), defaultValue)

    /**
     * Reads a double config value from the given path, throwing [IllegalArgumentException] with [message] if missing.
     */
    @Suppress("unused")
    fun getDoubleOrException(path: String, message: String): Double =
        resolveValue(path)?.let { asDouble(it, 0.0) } ?: throw IllegalArgumentException(message)

    /**
     * Reads a string config value from the given path, returning [defaultValue] if the path is missing.
     */
    @Suppress("unused")
    fun getStringOrElse(path: String, defaultValue: String): String = asString(resolveValue(path), defaultValue)

    /**
     * Reads the direct child numeric values under [path], preserving config order.
     */
    @Suppress("unused")
    fun getDoubleMap(path: String): LinkedHashMap<String, Double> {
        populateDefaults()
        val canonicalPath = path.trim().trim('.')
        val cachePath = "cache.maps.$canonicalPath"
        @Suppress("UNCHECKED_CAST")
        val cached = runtimeValues[cachePath] as? LinkedHashMap<String, Double>
        if (cached != null) return LinkedHashMap(cached)

        val prefix = if (canonicalPath.isEmpty()) "" else "$canonicalPath."
        val result = linkedMapOf<String, Double>()
        for ((entryPath, value) in values) {
            if (!entryPath.startsWith(prefix)) continue
            val childKey = entryPath.removePrefix(prefix)
            if (childKey.isEmpty() || '.' in childKey) continue
            result[childKey] = asDouble(value, 0.0)
        }
        runtimeValues[cachePath] = LinkedHashMap(result)
        return result
    }

    /**
     * Reads the direct child keys under [path], preserving config order.
     */
    @Suppress("unused")
    fun getChildKeys(path: String): List<String> {
        populateDefaults()
        val canonicalPath = path.trim().trim('.')
        val cachePath = "cache.keys.$canonicalPath"
        @Suppress("UNCHECKED_CAST")
        val cached = runtimeValues[cachePath] as? List<String>
        if (cached != null) return cached.toList()

        val prefix = if (canonicalPath.isEmpty()) "" else "$canonicalPath."
        val seen = linkedSetOf<String>()
        for (entryPath in values.keys) {
            if (!entryPath.startsWith(prefix)) continue
            val childKey = entryPath.removePrefix(prefix).substringBefore('.', "")
            if (childKey.isNotEmpty()) {
                seen.add(childKey)
            }
        }
        val result = seen.toList()
        runtimeValues[cachePath] = result
        return result
    }

    /**
     * Reads a map-valued config entry whose keys are free-form strings (e.g. block references)
     * and whose values are doubles. Returns an empty map if the path is missing or not a map.
     */
    fun getStringDoubleMap(path: String): LinkedHashMap<String, Double> {
        populateDefaults()
        val canonicalPath = path.trim().trim('.')
        val value = values[canonicalPath]
        if (value is Map<*, *>) {
            val result = linkedMapOf<String, Double>()
            for ((k, v) in value) {
                if (k is String) result[k] = asDouble(v, 0.0)
            }
            return result
        }
        return linkedMapOf()
    }

    /**
     * Reads a string config value from the given path, throwing [IllegalArgumentException] with [message] if missing.
     */
    @Suppress("unused")
    fun getStringOrException(path: String, message: String): String =
        resolveValue(path)?.let { asString(it, "") } ?: throw IllegalArgumentException(message)

    /**
     * Writes a persisted boolean config value to the given path.
     */
    @Suppress("unused")
    fun setBoolean(path: String, value: Boolean) = setTypedValue(path, value)

    /**
     * Writes a persisted integer config value to the given path.
     */
    @Suppress("unused")
    fun setInt(path: String, value: Int) = setTypedValue(path, value)

    /**
     * Writes a persisted double config value to the given path.
     */
    @Suppress("unused")
    fun setDouble(path: String, value: Double) = setTypedValue(path, value)

    /**
     * Writes a persisted string config value to the given path.
     */
    @Suppress("unused")
    fun setString(path: String, value: String) = setTypedValue(path, value)

    /**
     * Overrides a runtime-only boolean value without writing it to disk.
     */
    @Suppress("unused")
    fun setRuntimeBoolean(path: String, value: Boolean) = setRuntimeValue(path, value)

    /**
     * Overrides a runtime-only integer value without writing it to disk.
     */
    @Suppress("unused")
    fun setRuntimeInt(path: String, value: Int) = setRuntimeValue(path, value)

    /**
     * Overrides a runtime-only double value without writing it to disk.
     */
    @Suppress("unused")
    fun setRuntimeDouble(path: String, value: Double) = setRuntimeValue(path, value)

    /**
     * Overrides a runtime-only string value without writing it to disk.
     */
    @Suppress("unused")
    fun setRuntimeString(path: String, value: String) = setRuntimeValue(path, value)

    private fun resolveValue(path: String): Any? {
        return runtimeValues[path] ?: values[path]
    }

    private fun setRuntimeValue(path: String, value: Any) {
        runtimeValues[path] = value
    }

    private fun setTypedValue(path: String, value: Any) {
        populateDefaults()
        clearCollectionCaches()
        if (runtimeValues.containsKey(path) && !values.containsKey(path)) {
            runtimeValues[path] = value
        } else {
            values[path] = value
        }
    }

    private fun loadFromJson() {
        FileReader(jsonFile).use { reader ->
            val rootElement = JsonParser().parse(reader)
            if (!rootElement.isJsonObject) return
            flattenJson(rootElement.asJsonObject, emptyList())
        }
    }

    private fun flattenJson(obj: JsonObject, prefix: List<String>) {
        for ((key, value) in obj.entrySet()) {
            if (key.startsWith("_comment") || key == "schemaVersion") continue
            val currentPath = prefix + key
            val flatPath = joinPath(currentPath)
            when {
                mapPaths.contains(flatPath) && value.isJsonObject -> {
                    val map = linkedMapOf<String, Double>()
                    for ((mapKey, mapVal) in value.asJsonObject.entrySet()) {
                        if (mapKey.startsWith("_comment")) continue
                        if (mapVal.isJsonPrimitive) {
                            val primitive = mapVal.asJsonPrimitive
                            map[mapKey] = when {
                                primitive.isNumber -> primitive.asDouble
                                else -> primitive.asString.toDoubleOrNull() ?: 0.0
                            }
                        }
                    }
                    values[flatPath] = map
                }
                value.isJsonObject -> flattenJson(value.asJsonObject, currentPath)
                value.isJsonPrimitive -> values[flatPath] = primitiveToAny(value)
            }
        }
    }

    private fun primitiveToAny(value: JsonElement): Any {
        val primitive = value.asJsonPrimitive
        if (primitive.isBoolean) return primitive.asBoolean
        if (primitive.isNumber) {
            val number = primitive.asString
            return if (number.contains('.') || number.contains('e', ignoreCase = true)) primitive.asDouble else primitive.asInt
        }
        return primitive.asString
    }

    private fun loadFromLegacyCfg() {
        val legacy = readLegacyEntries()
        for (spec in specs) {
            val keyMatch = findLegacyKey(legacy, spec) ?: continue
            values[joinPath(spec.path)] = readLegacyValue(legacy.getValue(keyMatch), spec.defaultValue)
        }
    }

    private fun readLegacyValue(rawValue: String, defaultValue: Any): Any {
        return when (defaultValue) {
            is Boolean -> asBoolean(rawValue, defaultValue)
            is Int -> rawValue.toIntOrNull() ?: defaultValue
            is Double -> rawValue.toDoubleOrNull() ?: defaultValue
            else -> rawValue
        }
    }

    private fun findLegacyKey(legacyEntries: LegacyEntries, spec: ConfigSpec): LegacyKey? {
        return legacyKeysFor(spec).firstOrNull {
            legacyEntries.hasKey(it.category, it.key)
        }?.let {
            val categoryName = legacyEntries.findCategoryName(it.category) ?: return@let null
            val propertyName = legacyEntries.findKeyName(categoryName, it.key) ?: return@let null
            LegacyKey(categoryName, propertyName)
        }
    }

    private fun readLegacyEntries(): LegacyEntries {
        val entries = LegacyEntries()
        var currentCategory: String? = null

        legacyCfgFile.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachLine
            if (line.endsWith("{")) {
                currentCategory = line.removeSuffix("{").trim()
                return@forEachLine
            }
            if (line == "}") {
                currentCategory = null
                return@forEachLine
            }
            val category = currentCategory ?: return@forEachLine
            val colonIndex = line.indexOf(':')
            val equalsIndex = line.indexOf('=')
            if (colonIndex <= 0 || equalsIndex <= colonIndex + 1) return@forEachLine
            val key = line.substring(colonIndex + 1, equalsIndex).trim()
            val value = line.substring(equalsIndex + 1).trim()
            entries.put(category, key, value)
        }

        return entries
    }

    private fun populateDefaults() {
        for ((path, value) in buildDefaultValues()) {
            values.putIfAbsent(path, value)
        }
    }

    private fun clearCollectionCaches() {
        val iterator = runtimeValues.keys.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().startsWith("cache.")) {
                iterator.remove()
            }
        }
    }

    private fun buildJsonTree(): JsonObject = buildJsonTree(values)

    private fun buildJsonTree(sourceValues: Map<String, Any>): JsonObject {
        val root = JsonObject()
        root.addProperty("schemaVersion", 1)
        root.addProperty("_comment", "Electrical Age configuration in JSON format. Files live in config/eln/.")

        for ((path, value) in sourceValues) {
            val parts = path.split('.')
            var current = root
            val parentParts = mutableListOf<String>()

            for (part in parts.dropLast(1)) {
                parentParts += part
                if (!current.has(part) || !current.get(part).isJsonObject) {
                    current.add(part, JsonObject())
                }
                current = current.getAsJsonObject(part)
                groupComments[joinPath(parentParts)]?.let { comment ->
                    if (!current.has("_comment")) current.addProperty("_comment", comment)
                }
            }

            val leaf = parts.last()
            addJsonProperty(current, leaf, value)
            pathComments[path]?.let { current.addProperty("_comment_$leaf", it) }
        }

        return root
    }

    private fun buildDefaultValues(): LinkedHashMap<String, Any> {
        val defaults = linkedMapOf<String, Any>()
        for (spec in specs) {
            defaults[joinPath(spec.path)] = spec.defaultValue
        }
        if (includeDefaultSpecs) {
            for (lamp in LampLists.lampTechnologyList) {
                defaults[lamp.nominalLifePath] = lamp.nominalLifeInHours
                defaults[lamp.infiniteLifePath] = lamp.infiniteLifeEnabled
            }
            for (blade in TurbineBladeLists.bladeConfigList) {
                defaults["items.turbineBlades.${blade.tierName}.nominalLifeHours"] = blade.nominalLifeInHours
                defaults["items.turbineBlades.${blade.tierName}.infiniteLifeEnabled"] = blade.infiniteLifeEnabled
            }
        }
        return defaults
    }

    private fun addJsonProperty(parent: JsonObject, key: String, value: Any) {
        when (value) {
            is Boolean -> parent.addProperty(key, value)
            is Int -> parent.addProperty(key, value)
            is Double -> parent.addProperty(key, value)
            is Map<*, *> -> {
                val obj = JsonObject()
                for ((mapKey, mapVal) in value) {
                    if (mapKey !is String) continue
                    when (mapVal) {
                        is Number -> obj.addProperty(mapKey, mapVal)
                        else -> obj.addProperty(mapKey, mapVal?.toString() ?: "")
                    }
                }
                parent.add(key, obj)
            }
            else -> parent.addProperty(key, value.toString())
        }
    }

    private fun archiveLegacyCfg() {
        if (!legacyCfgFile.exists()) return
        migratedCfgFile.parentFile?.mkdirs()
        Files.copy(
            legacyCfgFile.toPath(),
            migratedCfgFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
        legacyCfgFile.delete()
    }

    private fun joinPath(parts: List<String>): String = parts.joinToString(".")

    private fun compilePathPattern(pathPattern: String): Regex {
        val regex = pathPattern
            .split('.')
            .joinToString("\\.") { segment ->
                when (segment) {
                    "*" -> ".*"
                    else -> segment
                }
            }
        return Regex("^$regex$")
    }

    private fun populateMapPaths() {
        for (spec in specs) {
            if (spec.defaultValue is Map<*, *>) {
                mapPaths.add(joinPath(spec.path))
            }
        }
    }

    private fun registerSpecComments() {
        for (spec in specs) {
            val path = joinPath(spec.path)
            spec.comment?.let { pathComments[path] = it }
        }
        for (lamp in LampLists.lampTechnologyList) {
            pathComments["lighting.lamps.${lamp.lampType}.nominalLifeHours"] = "Nominal lamp lifetime in hours for ${lamp.lampType} bulbs."
            pathComments["lighting.lamps.${lamp.lampType}.infiniteLifeEnabled"] = "Disable bulb wear-out for ${lamp.lampType} bulbs."
        }
        for (blade in TurbineBladeLists.bladeConfigList) {
            pathComments["items.turbineBlades.${blade.tierName}.nominalLifeHours"] = "Nominal turbine blade lifetime in hours for the ${blade.tierName} tier."
            pathComments["items.turbineBlades.${blade.tierName}.infiniteLifeEnabled"] = "Disable wear-out for ${blade.tierName} turbine blades."
        }
    }

    private fun legacyKeysFor(spec: ConfigSpec): List<LegacyKey> = when (joinPath(spec.path)) {
        "integrations.modbus.enabled" -> listOf(LegacyKey("modbus", "enable"))
        "integrations.modbus.port" -> listOf(LegacyKey("modbus", "port"))
        "integrations.mqtt.enabled" -> listOf(LegacyKey("mqtt", "enable"))
        "integrations.computerProbe.enabled" -> listOf(LegacyKey("compatibility", "ComputerProbeEnable"))
        "integrations.energyExporter.enabled" -> listOf(LegacyKey("compatibility", "ElnToOtherEnergyConverterEnable"))
        "integrations.oredict.tungstenEnabled" -> listOf(LegacyKey("dictionary", "tungsten"))
        "integrations.oredict.chipsEnabled" -> listOf(LegacyKey("dictionary", "chips"))
        "debug.logging.enabled" -> listOf(LegacyKey("debug", "enable"))
        "debug.logging.simSnapshot" -> listOf(LegacyKey("debug", "simSnapshot"))
        "ui.audio.maxSoundDistance" -> listOf(LegacyKey("debug", "maxSoundDistance"))
        "ui.audio.soundChannels" -> listOf(LegacyKey("debug", "soundChannels"))
        "gameplay.hazards.explosionsEnabled" -> listOf(LegacyKey("gameplay", "explosion"))
        "gameplay.seasonal.enableFestiveItems" -> listOf(LegacyKey("general", "enableFestiveItems"))
        "gameplay.crafting.verticalIronCableCrafting" -> listOf(LegacyKey("general", "verticalIronCableCrafting"))
        "simulation.watchdog.destruction.thermal" -> listOf(LegacyKey("watchdog", "thermal"))
        "simulation.watchdog.destruction.resistorHeat" -> listOf(LegacyKey("watchdog", "resistorHeat"))
        "simulation.watchdog.destruction.current" -> listOf(LegacyKey("watchdog", "current"))
        "simulation.watchdog.destruction.voltage" -> listOf(LegacyKey("watchdog", "voltage"), LegacyKey("watchdog", "electrical"))
        "simulation.watchdog.destruction.shaftSpeed" -> listOf(LegacyKey("watchdog", "shaftSpeed"))
        "simulation.watchdog.destruction.other" -> listOf(LegacyKey("watchdog", "other"))
        "updates.versionCheck.enabled" -> listOf(LegacyKey("general", "versionCheckEnable"))
        "analytics.enabled" -> listOf(LegacyKey("general", "analyticsEnable"))
        "analytics.endpointUrl" -> listOf(LegacyKey("general", "analyticsURL"))
        "analytics.playerUuidOptIn" -> listOf(LegacyKey("general", "analyticsPlayerOptIn"))
        "analytics.playerUuid" -> listOf(LegacyKey("general", "playerUUID"))
        "world.poles.directPlacementEnabled" -> listOf(LegacyKey("general", "directPoles"))
        "balance.generators.heatTurbinePowerFactor" -> listOf(LegacyKey("balancing", "heatTurbinePowerFactor"))
        "balance.generators.solarPanelPowerFactor" -> listOf(LegacyKey("balancing", "solarPanelPowerFactor"))
        "balance.generators.windTurbinePowerFactor" -> listOf(LegacyKey("balancing", "windTurbinePowerFactor"))
        "balance.generators.waterTurbinePowerFactor" -> listOf(LegacyKey("balancing", "waterTurbinePowerFactor"))
        "balance.generators.fuelGeneratorPowerFactor" -> listOf(LegacyKey("balancing", "fuelGeneratorPowerFactor"))
        "balance.generators.fuelHeatFurnacePowerFactor" -> listOf(LegacyKey("balancing", "fuelHeatFurnacePowerFactor"))
        "machines.autominer.maxRangeBlocks" -> listOf(LegacyKey("balancing", "autominerRange"))
        "balance.integrationConversion.wattsToEu" -> listOf(LegacyKey("balancing", "ElnToIndustrialCraftConversionRatio"))
        "balance.integrationConversion.wattsToOc" -> listOf(LegacyKey("balancing", "ElnToOpenComputerConversionRatio"))
        "balance.integrationConversion.wattsToRf" -> listOf(LegacyKey("balancing", "ElnToThermalExpansionConversionRatio"))
        "balance.materials.platesPerIngot" -> listOf(LegacyKey("balancing", "platesPerIngot"))
        "balance.mechanics.shaftEnergyFactor" -> listOf(LegacyKey("balancing", "shaftEnergyFactor"))
        "items.batteries.standardHalfLifeDays" -> listOf(LegacyKey("battery", "batteryHalfLife"))
        "items.batteries.infinite.standard" -> listOf(LegacyKey("battery", "infiniteStandardBatteryLife"))
        "items.batteries.infinite.portable" -> listOf(LegacyKey("battery", "infinitePortableBatteryLife"))
        "balance.storage.batteryCapacityFactor" -> listOf(LegacyKey("balancing", "batteryCapacityFactor"))
        "entities.replicator.enabled" -> listOf(LegacyKey("entity", "replicatorPop"))
        "entities.replicator.thunderSpawnPerSecondPerPlayer" -> listOf(LegacyKey("entity", "replicatorPopWhenThunderPerSecond"))
        "entities.replicator.entityId" -> listOf(LegacyKey("entity", "replicatorId"))
        "entities.mobSpawning.preventNearLamps" -> listOf(LegacyKey("entity", "killMonstersAroundLamps"))
        "entities.mobSpawning.preventNearLampsRange" -> listOf(LegacyKey("entity", "killMonstersAroundLampsRange"))
        "entities.replicator.maxCount" -> listOf(LegacyKey("entity", "maxReplicators"))
        "worldgen.ores.copper.enabled" -> listOf(LegacyKey("mapgenerate", "cooper"), LegacyKey("mapgenerate", "copper"))
        "worldgen.ores.lead.enabled" -> listOf(LegacyKey("mapgenerate", "plumb"), LegacyKey("mapgenerate", "lead"))
        "worldgen.ores.tungsten.enabled" -> listOf(LegacyKey("mapgenerate", "tungsten"))
        "worldgen.ores.cinnabar.enabled" -> listOf(LegacyKey("mapgenerate", "cinnabar"))
        "machines.fuelGenerator.tankCapacitySecondsAtNominalPower" -> listOf(LegacyKey("fuelGenerator", "tankCapacityInSecondsAtNominalPower"))
        "machines.heatFurnace.consumeFuel" -> listOf(LegacyKey("heatFurnace", "heatFurnaceConsumesFuel"))
        "tools.xrayScanner.autoDiscoverOreDictionaryOres" -> listOf(LegacyKey("xrayscannerconfig", "addOtherModOreToXRay"))
        "tools.xrayScanner.rangeBlocks" -> listOf(LegacyKey("xrayscannerconfig", "rangeInBloc"))
        "tools.xrayScanner.canBeCrafted" -> listOf(LegacyKey("xrayscannerconfig", "canBeCrafted"))
        "simulation.electrical.frequency" -> listOf(LegacyKey("simulation", "electricalFrequency"), LegacyKey("simulation", "electricalFrequancy"))
        "simulation.electrical.interSystemOverSampling" -> listOf(LegacyKey("simulation", "electricalInterSystemOverSampling"))
        "simulation.thermal.frequency" -> listOf(LegacyKey("simulation", "thermalFrequency"), LegacyKey("simulation", "thermalFrequancy"))
        "simulation.roomDetection.maxAxisSpanBlocks" -> listOf(LegacyKey("simulation", "roomMaxAxisSpanBlocks"))
        "simulation.roomDetection.maxVolumeBlocks" -> listOf(LegacyKey("simulation", "roomMaxVolumeBlocks"))
        "wireless.transmitter.maxRangeBlocks" -> listOf(LegacyKey("wireless", "txRange"))
        "ui.waila.easyMode" -> listOf(LegacyKey("balancing", "wailaEasyMode"))
        "balance.cables.powerFactor" -> listOf(LegacyKey("balancing", "cablePowerFactor"))
        "simulation.thermal.cableSpikeLimiter.enabled" -> listOf(LegacyKey("simulation", "cableThermalSpikeLimiterEnabled"))
        "simulation.thermal.cableSpikeLimiter.factor" -> listOf(LegacyKey("simulation", "cableThermalSpikeLimitFactor"))
        "simulation.thermal.ambient.lavaRampEnabled" -> listOf(LegacyKey("simulation", "lavaAmbientRampEnabled"))
        "simulation.thermal.ambient.undergroundBiomeTemperatureMultiplier" -> listOf(LegacyKey("simulation", "undergroundBiomeTemperatureMultiplier"))
        "ui.icons.noSymbols" -> listOf(LegacyKey("general", "noSymbols"), LegacyKey("gameplay", "noSymbols"))
        "ui.icons.noVoltageBackground" -> listOf(LegacyKey("general", "noVoltageBackground"))
        "balance.mechanics.flywheelMass" -> listOf(LegacyKey("balancing", "flywheelMass"))
        else -> emptyList()
    }

    private fun asBoolean(value: Any?, fallback: Boolean): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> when (value.trim().lowercase(Locale.ROOT)) {
            "true", "yes", "on", "1", "enable", "enabled" -> true
            "false", "no", "off", "0", "disable", "disabled" -> false
            else -> fallback
        }
        else -> fallback
    }

    private fun asInt(value: Any?, fallback: Int): Int = when (value) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: fallback
        else -> fallback
    }

    private fun asDouble(value: Any?, fallback: Double): Double = when (value) {
        is Double -> value
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: fallback
        else -> fallback
    }

    private fun asString(value: Any?, fallback: String): String = when (value) {
        null -> fallback
        is String -> value
        else -> value.toString()
    }

    private fun parseRawValue(rawValue: String, template: Any): Any {
        return when (template) {
            is Boolean -> {
                val parsed = when (rawValue.trim().lowercase(Locale.ROOT)) {
                    "true", "yes", "on", "1", "enable", "enabled" -> true
                    "false", "no", "off", "0", "disable", "disabled" -> false
                    else -> null
                }
                parsed ?: throw IllegalArgumentException("Expected boolean for '$rawValue'")
            }
            is Int -> rawValue.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Expected integer for '$rawValue'")
            is Double -> rawValue.trim().toDoubleOrNull()
                ?: throw IllegalArgumentException("Expected number for '$rawValue'")
            else -> rawValue
        }
    }

    private fun formatValue(value: Any): String = when (value) {
        is String -> value
        is Boolean, is Int -> value.toString()
        is Double -> {
            val asLong = value.toLong()
            if (value == asLong.toDouble()) asLong.toString() else value.toString()
        }
        else -> value.toString()
    }
}

private class LegacyEntries {
    private val data = linkedMapOf<String, LinkedHashMap<String, String>>()

    fun put(category: String, key: String, value: String) {
        data.getOrPut(category) { linkedMapOf() }[key] = value
    }

    fun hasKey(category: String, key: String): Boolean {
        val actualCategory = findCategoryName(category) ?: return false
        return findKeyName(actualCategory, key) != null
    }

    fun findCategoryName(category: String): String? =
        data.keys.firstOrNull { it.equals(category, ignoreCase = true) }

    fun findKeyName(category: String, key: String): String? =
        data[category]?.keys?.firstOrNull { it.equals(key, ignoreCase = true) }

    fun getValue(legacyKey: LegacyKey): String =
        data.getValue(legacyKey.category).getValue(legacyKey.key)
}
