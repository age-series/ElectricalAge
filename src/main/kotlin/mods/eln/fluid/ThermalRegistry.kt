package mods.eln.fluid

import mods.eln.config.JsonConfig
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import java.io.File

data class ThermalPairConfig(
    val inputFluid: String,
    val outputFluid: String,
    val joulesPerMb: Double,
    val maxMbInputPerTick: Int,
    val ratio: Double = 1.0,
    val reversible: Boolean = false,
    val minTemp: Double? = null,
    val maxTemp: Double? = null
)

object ThermalRegistry {
    private const val pairsPath = "thermalPairs"

    private var config = JsonConfig(File("config/eln/thermal.cfg"), includeDefaultSpecs = false)

    private val defaultPairs = listOf(
        ThermalPairConfig("blood", "blood_hot", -262.0, 8, 1.0, false, minTemp = 275.0, maxTemp = 313.0),
        ThermalPairConfig("blood_hot", "blood", 262.0, 8, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("coolant", "coolant_hot", -275.0, 12, 1.0, false, minTemp = 300.0, maxTemp = 900.0),
        ThermalPairConfig("coolant_hot", "coolant", 275.0, 12, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("heavywater", "heavywater_hot", -270.0, 30, 1.0, false, minTemp = 280.0, maxTemp = 370.0),
        ThermalPairConfig("heavywater_hot", "heavywater", 270.0, 30, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("ic2coolant", "ic2hotcoolant", -1920.0 / 7.0, 9, 1.0, false, minTemp = 300.0),
        ThermalPairConfig("ic2hotcoolant", "ic2coolant", 1920.0 / 7.0, 9, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("ic2hotwater", "water", 1.0 / 0.45 / 2.0, 36, 1.0, false, minTemp = 26.85, maxTemp = 76.85),
        ThermalPairConfig("lead", "lead_hot", -95.0, 14, 1.0, false, minTemp = 610.0, maxTemp = 900.0),
        ThermalPairConfig("lead_hot", "lead", 95.0, 14, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("mug", "mug_hot", -270.0, 32, 1.0, false, minTemp = 275.0, maxTemp = 370.0),
        ThermalPairConfig("mug_hot", "mug", 270.0, 32, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("perfluoromethyl", "perfluoromethyl_hot", -100.0, 22, 1.0, false, minTemp = 240.0, maxTemp = 340.0),
        ThermalPairConfig("perfluoromethyl_hot", "perfluoromethyl", 100.0, 22, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("sodium", "sodium_hot", -64.0, 48, 1.0, false, minTemp = 400.0, maxTemp = 850.0),
        ThermalPairConfig("sodium_hot", "sodium", 64.0, 48, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("thorium_salt", "thorium_salt_hot", -275.0, 10, 1.0, false, minTemp = 740.0, maxTemp = 1000.0),
        ThermalPairConfig("thorium_salt_hot", "thorium_salt", 275.0, 10, 1.0, false, maxTemp = 600.0),
        ThermalPairConfig("water", "steam", -1.0 / 0.45, 36, 10.0, false, minTemp = 100.0)
    )

    @JvmStatic
    fun init(baseConfigFile: File) {
        val configDirectory = baseConfigFile.parentFile ?: File("config")
        config = JsonConfig(File(configDirectory, "thermal.cfg"), includeDefaultSpecs = false)
        registerConfigEntries(config)
        config.load()
        config.save()
        config.writeExampleFile()
    }

    private fun registerConfigEntries(config: JsonConfig) {
        config.registerGroupComment(pairsPath, "Thermal fluid pair data for the thermal heat exchanger. Each entry maps an input fluid to an output fluid with thermal properties.")

        for (pair in defaultPairs) {
            val entryPath = "$pairsPath.${pair.inputFluid}"
            config.registerGroupComment(entryPath, "${pair.inputFluid} -> ${pair.outputFluid}")
            config.registerEntry("$entryPath.outputFluid", pair.outputFluid)
            config.registerEntry("$entryPath.joulesPerMb", pair.joulesPerMb)
            config.registerEntry("$entryPath.maxMbInputPerTick", pair.maxMbInputPerTick)
            config.registerEntry("$entryPath.ratio", pair.ratio)
            config.registerEntry("$entryPath.reversible", pair.reversible)
            if (pair.minTemp != null) config.registerEntry("$entryPath.minTemp", pair.minTemp)
            if (pair.maxTemp != null) config.registerEntry("$entryPath.maxTemp", pair.maxTemp)
        }
    }

    fun getThermalPairConfigs(): List<ThermalPairConfig> {
        val names = config.getChildKeys(pairsPath)
        if (names.isEmpty()) return defaultPairs

        return names.mapNotNull { name ->
            val path = "$pairsPath.$name"
            val outputFluid = config.getStringOrElse("$path.outputFluid", "")
            if (outputFluid.isEmpty()) return@mapNotNull null
            ThermalPairConfig(
                inputFluid = name,
                outputFluid = outputFluid,
                joulesPerMb = config.getDoubleOrElse("$path.joulesPerMb", 0.0),
                maxMbInputPerTick = config.getIntOrElse("$path.maxMbInputPerTick", 0),
                ratio = config.getDoubleOrElse("$path.ratio", 1.0),
                reversible = config.getBooleanOrElse("$path.reversible", false),
                minTemp = if (config.readPath("$path.minTemp") != null) config.getDoubleOrElse("$path.minTemp", 0.0) else null,
                maxTemp = if (config.readPath("$path.maxTemp") != null) config.getDoubleOrElse("$path.maxTemp", 0.0) else null
            )
        }
    }

    fun resolveFluid(name: String): Fluid? {
        val fluid = FluidRegistry.getFluid(name)
        if (fluid != null) return fluid
        if (name == "steam") return FluidRegistry.getFluid("ic2steam")
        if (name == "water") return FluidRegistry.WATER
        return null
    }

}
