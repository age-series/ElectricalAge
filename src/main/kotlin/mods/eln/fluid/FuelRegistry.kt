package mods.eln.fluid

import mods.eln.config.JsonConfig
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import java.io.File

data class FuelEntry(
    val name: String,
    val heatValue: Double,
    val temperatureFactor: Double,
    val cleanlinessFactor: Double,
    val comment: String
)

object FuelRegistry {
    private const val heatValueFactorPath = "heatValueFactor"
    private const val defaultHeatValueFactor = 0.0000675
    private const val defaultTemperatureFactor = 0.5
    private const val defaultCleanlinessFactor = 0.3

    private const val fuelsPath = "fuels"
    private const val dieselPath = "fuels.diesel"
    private const val gasolinePath = "fuels.gasoline"
    private const val gasPath = "fuels.gas"
    private const val steamPath = "fuels.steam"
    private var config = JsonConfig(File("config/eln/fluids.cfg"), includeDefaultSpecs = false)

    private val dieselDefaults = listOf(
        fuel("creosote", 750000.0, 0.5, 0.3, "Railcraft, coal tar creosote, ~1.08 kg/L, LHV ~0.7 MJ/L."),
        fuel("hootch", 17826480.0 * 0.6, 0.5, 0.3, "EnderIO, roughly diluted ethanol."),
        fuel("nitroglycerin", 11600000.0, 0.70, 0.70, "HBM, nitroglycerin, ~1.60 kg/L, LHV ~7.27 MJ/kg (~11.6 MJ/L). Controlled-burn, high temp."),
        fuel("woodoil", 15000000.0, 0.5, 0.3, "HBM, pyrolysis oil, ~1.10 kg/L, LHV ~14 MJ/kg."),
        fuel("biodiesel", 32560000.0, 0.5, 0.3, "Immersive Engineering, biodiesel, 0.88 kg/L, LHV 37 MJ/kg."),
        fuel("sunfloweroil", 33000000.0, 0.5, 0.3, "HBM, sunflower oil, ~0.92 kg/L, LHV ~35.9 MJ/kg."),
        fuel("fishoil", 33200000.0, 0.5, 0.3, "HBM, fish oil, ~0.92 kg/L, LHV ~36.1 MJ/kg."),
        fuel("oliveoil", 33400000.0, 0.5, 0.3, "HBM, olive oil, ~0.91 kg/L, LHV ~36.7 MJ/kg."),
        fuel("reclaimed", 34000000.0, 0.5, 0.3, "HBM, reclaimed/waste oil, ~0.88 kg/L, LHV ~38.6 MJ/kg."),
        fuel("coalcreosote", 34300000.0, 0.5, 0.3, "HBM, coal creosote oil, ~1.05 kg/L, LHV ~32.7 MJ/kg."),
        fuel("bitumen", 34500000.0, 0.5, 0.3, "HBM, bitumen/asphalt, ~1.02 kg/L, LHV ~33.8 MJ/kg."),
        fuel("crackoil_ds", 34800000.0, 0.5, 0.3, "HBM, desulfurized cracked oil, ~0.85 kg/L, LHV ~40.9 MJ/kg."),
        fuel("hotcrackoil_ds", 34800000.0, 0.5, 0.3, "HBM, hot desulfurized cracked oil."),
        fuel("oil_coker", 34800000.0, 0.5, 0.3, "HBM, coker oil, ~0.92 kg/L, LHV ~37.8 MJ/kg."),
        fuel("crackoil", 35000000.0, 0.5, 0.3, "HBM, cracked oil, ~0.85 kg/L, LHV ~41.2 MJ/kg."),
        fuel("hotcrackoil", 35000000.0, 0.5, 0.3, "HBM, hot cracked oil."),
        fuel("coaloil", 35400000.0, 0.5, 0.30, "HBM, coal-derived middle distillate, ~0.95 kg/L, LHV ~37.3 MJ/kg. Sulfur-bearing, moderate soot."),
        fuel("oil_ds", 35600000.0, 0.5, 0.3, "HBM, desulfurized crude oil, ~0.87 kg/L, LHV ~40.9 MJ/kg."),
        fuel("hotoil_ds", 35600000.0, 0.5, 0.3, "HBM, hot desulfurized oil."),
        fuel("oil", 35800000.0, 0.5, 0.3, "HBM, crude oil, ~0.87 kg/L, LHV ~41.2 MJ/kg."),
        fuel("hotoil", 35800000.0, 0.5, 0.3, "HBM, hot crude oil."),
        fuel("smear", 35800000.0, 0.5, 0.40, "HBM, industrial oil (smear), ~0.88 kg/L, LHV ~40.7 MJ/kg. Contaminated, high wear."),
        fuel("lubricant", 36000000.0, 0.5, 0.3, "HBM, lubricating oil, ~0.88 kg/L, LHV ~40.9 MJ/kg."),
        fuel("heatingoil_vacuum", 36000000.0, 0.5, 0.3, "HBM, vacuum heating oil, ~0.88 kg/L, LHV ~40.9 MJ/kg."),
        fuel("heatingoil", 36500000.0, 0.5, 0.3, "HBM, No. 2 fuel oil, ~0.85 kg/L, LHV ~42.9 MJ/kg."),
        fuel("petroleum", 37000000.0, 0.5, 0.3, "HBM, petroleum base stock, ~0.85 kg/L, LHV ~43.5 MJ/kg."),
        fuel("diesel_crack", 37500000.0, 0.5, 0.3, "HBM, cracked diesel, ~0.81 kg/L, LHV ~46.3 MJ/kg."),
        fuel("diesel_crack_reform", 38200000.0, 0.5, 0.3, "HBM, cracked reformed diesel, ~0.82 kg/L, LHV ~46.6 MJ/kg."),
        fuel("heavyoil_vacuum", 38500000.0, 0.5, 0.3, "HBM, vacuum heavy oil, ~0.98 kg/L, LHV ~39.3 MJ/kg."),
        fuel("diesel", 38600000.0, 0.5, 0.3, "IE/PneumaticCraft/HBM diesel, ~0.83 kg/L, LHV ~45.5 MJ/kg (~38.6 MJ/L)."),
        fuel("heavyoil", 39100000.0, 0.5, 0.3, "Magneticraft/HBM heavy fuel oil, ~0.95 kg/L, LHV ~41.2 MJ/kg."),
        fuel("diesel_reform", 39200000.0, 0.5, 0.3, "HBM reformed diesel, ~0.84 kg/L, LHV ~46.7 MJ/kg.")
    )

    private val gasolineDefaults = listOf(
        fuel("chloromethane", 13400000.0, 0.40, 0.85, "HBM, chloromethane, ~0.911 kg/L, LHV ~14.7 MJ/kg (~13.4 MJ/L). HCl exhaust, severely corrosive."),
        fuel("methanol", 15760000.0, 0.40, 0.05, "HBM, methanol, 0.792 kg/L, LHV 19.9 MJ/kg (~15.76 MJ/L). Clean-burning."),
        fuel("bloodgas", 16000000.0, 0.40, 0.60, "HBM, blood fuel, ~1.06 kg/L, LHV ~15 MJ/kg. Organic contaminants, heavy fouling."),
        fuel("vinyl", 17170000.0, 0.45, 0.90, "HBM, vinyl chloride, ~0.911 kg/L, LHV ~18.85 MJ/kg (~17.17 MJ/L). HCl exhaust, extreme corrosion."),
        fuel("biofuel", 17826480.0, 0.40, 0.05, "MFR/HBM bioethanol, 0.786 kg/L, LHV ~22.68 MJ/L."),
        fuel("bioethanol", 17826480.0, 0.40, 0.05, "Forestry bioethanol, 0.786 kg/L, LHV ~22.68 MJ/L."),
        fuel("ic2biogas", 17826480.0, 0.40, 0.10, "IC2 biogas, treated as diluted ethanol."),
        fuel("hydrazine", 19810000.0, 0.60, 0.10, "HBM, hydrazine, 1.021 kg/L, LHV 19.4 MJ/kg (~19.81 MJ/L). N2 + H2O products, very clean."),
        fuel("chloroethane", 20240000.0, 0.45, 0.80, "HBM, chloroethane, ~0.92 kg/L, LHV ~22 MJ/kg (~20.24 MJ/L). HCl exhaust, severe corrosion."),
        fuel("rc ethanol", 21172000.0, 0.40, 0.05, "RotaryCraft ethanol, 0.79 kg/L, LHV 26.8 MJ/kg."),
        fuel("ethanol", 21340000.0, 0.40, 0.05, "HBM ethanol, 0.789 kg/L, LHV 26.8 MJ/kg (~21.1 MJ/L)."),
        fuel("lpg", 24840000.0, 0.45, 0.05, "PneumaticCraft/HBM LPG, ~0.54 kg/L, LHV ~46 MJ/kg (~24.8 MJ/L)."),
        fuel("dicyanoacetylene", 25000000.0, 0.95, 0.40, "HBM, dicyanoacetylene (C4N2), ~0.907 kg/L, LHV ~27.7 MJ/kg (~25 MJ/L). ~5260 K flame in O2, hottest known."),
        fuel("naphtha_coker", 30500000.0, 0.50, 0.40, "HBM coker naphtha, ~0.72 kg/L, LHV ~42.4 MJ/kg."),
        fuel("naphtha_crack", 30800000.0, 0.50, 0.35, "HBM cracked naphtha, ~0.71 kg/L, LHV ~43.4 MJ/kg."),
        fuel("unsaturateds", 31000000.0, 0.50, 0.25, "HBM unsaturateds (olefin mix), ~0.72 kg/L, LHV ~43.1 MJ/kg."),
        fuel("naphtha_ds", 31200000.0, 0.50, 0.08, "HBM desulfurized naphtha, ~0.73 kg/L, LHV ~42.7 MJ/kg."),
        fuel("naphtha", 31400000.0, 0.50, 0.25, "HBM petroleum naphtha, ~0.73 kg/L, LHV ~43.0 MJ/kg (~31.4 MJ/L)."),
        fuel("fuel", 31570000.0, 0.55, 0.10, "BuildCraft refined fuel, 0.77 kg/L, LHV 41 MJ/kg."),
        fuel("fuelgc", 31570000.0, 0.55, 0.10, "Galacticraft fuel, matched to BuildCraft."),
        fuel("petroil", 31800000.0, 0.55, 0.20, "HBM petroil blend, ~0.74 kg/L, LHV ~43.0 MJ/kg."),
        fuel("petroil_leaded", 31800000.0, 0.55, 0.50, "HBM leaded petroil."),
        fuel("gasoline", 32200000.0, 0.55, 0.10, "PneumaticCraft/HBM gasoline, ~0.745 kg/L, LHV ~43.2 MJ/kg (~32.2 MJ/L)."),
        fuel("gasoline_leaded", 32200000.0, 0.55, 0.40, "HBM leaded gasoline."),
        fuel("rocket_fuel", 17826480.0 * 1.866, 0.60, 0.05, "EnderIO rocket fuel, approx. ethanol-like."),
        fuel("reformate", 33500000.0, 0.55, 0.08, "HBM reformate, ~0.79 kg/L, LHV ~42.4 MJ/kg."),
        fuel("dhc", 34000000.0, 0.55, 0.20, "HBM, deuterated hydrocarbon, ~0.85 kg/L, LHV ~40 MJ/kg (~34 MJ/L)."),
        fuel("aromatics", 34200000.0, 0.55, 0.15, "HBM aromatics (BTX), ~0.87 kg/L, LHV ~39.3 MJ/kg."),
        fuel("lightoil_crack", 34500000.0, 0.60, 0.30, "HBM cracked light oil, ~0.80 kg/L, LHV ~43.1 MJ/kg."),
        fuel("kerosene", 34800000.0, 0.55, 0.10, "PneumaticCraft/HBM kerosene (Jet A-1), ~0.81 kg/L, LHV ~43.0 MJ/kg (~34.8 MJ/L)."),
        fuel("lightoil_vacuum", 34800000.0, 0.60, 0.25, "HBM vacuum light oil, ~0.84 kg/L, LHV ~41.4 MJ/kg."),
        fuel("xylene", 35000000.0, 0.55, 0.20, "HBM, BTX cut, ~0.87 kg/L, LHV ~40.2 MJ/kg (~35 MJ/L). Aromatic, sooty."),
        fuel("lightoil_ds", 35100000.0, 0.60, 0.05, "HBM desulfurized light oil, ~0.83 kg/L, LHV ~42.3 MJ/kg."),
        fuel("lightoil", 35358000.0, 0.60, 0.20, "Magneticraft/HBM light oil, ~0.83 kg/L, LHV ~42.6 MJ/kg."),
        fuel("kerosene_reform", 35400000.0, 0.55, 0.05, "HBM reformed kerosene, ~0.82 kg/L, LHV ~43.2 MJ/kg."),
        fuel("fire_water", 17826480.0 * 2, 0.60, 0.05, "EnderIO fire water, ~2x ethanol energy density."),
        fuel("nitan", 36000000.0, 0.65, 0.05, "HBM, NITAN 100 Octane, fictional premium gasoline, ~48 MJ/kg, ~36 MJ/L at ~0.75 kg/L."),
        fuel("nmass", 38000000.0, 0.70, 0.10, "HBM, N-MASS(II) Driver Fuel, fictional synthetic, ~38 MJ/L."),
        fuel("highgradekerosene", 39200000.0, 0.60, 0.05, "ContentTweaker refined kerosene, LHV 39.2 MJ/L."),
        fuel("nmasstetranol", 42000000.0, 0.80, 0.10, "HBM, N-MASS(III) Tetrahexaethanol, fictional synthetic, ~42 MJ/L."),
        fuel("balefire", 50000000.0, 0.85, 0.80, "HBM, BF Rocket Fuel, fictional sci-fi propellant, ~50 MJ/L. Radioactive exhaust, heavy wear.")
    )

    private val gasDefaults = listOf(
        fuel("oxyhydrogen", 7600.0 * 1000, 0.30, 0.0, "HBM oxyhydrogen (2:1 H2:O2), LHV ~7.6 MJ/m³. ELN gas multiplier applied."),
        fuel("hydrogen", 10800.0 * 1000, 0.20, 0.0, "HBM hydrogen, ~0.09 kg/m³, LHV 120 MJ/kg (~10.8 MJ/m³). ELN gas multiplier applied."),
        fuel("superheated_hydrogen", 10800.0 * 1000, 0.40, 0.0, "HBM superheated hydrogen, same LHV, higher inlet enthalpy. ELN gas multiplier applied."),
        fuel("ammonia", 14000.0 * 1000, 0.40, 0.20, "HBM ammonia, ~0.76 kg/m³ STP, LHV 18.6 MJ/kg (~14.1 MJ/m³). NOx but no soot. ELN gas multiplier applied."),
        fuel("coalgas", 18000.0 * 1000, 0.30, 0.30, "HBM coal gas, LHV ~18 MJ/m³. ELN gas multiplier applied."),
        fuel("coalgas_leaded", 18000.0 * 1000, 0.30, 0.60, "HBM leaded coal gas. ELN gas multiplier applied."),
        fuel("syngas", 20000.0 * 1000, 0.35, 0.10, "Advanced Generators/HBM syngas, LHV ~18-20 MJ/m³. ELN gas multiplier applied."),
        fuel("biogas", 22000.0 * 1000, 0.35, 0.15, "HBM biogas (~60% CH4, 40% CO2), LHV ~22 MJ/m³. ELN gas multiplier applied."),
        fuel("sourgas", 25000.0 * 1000, 0.45, 1.0, "HBM sour gas, reduced energy from impurities, LHV ~25 MJ/m³. ELN gas multiplier applied."),
        fuel("gas_coker", 28000.0 * 1000, 0.50, 0.50, "HBM coker gas, LHV ~28 MJ/m³. ELN gas multiplier applied."),
        fuel("reformgas", 30000.0 * 1000, 0.50, 0.20, "HBM reform gas, LHV ~30 MJ/m³. ELN gas multiplier applied."),
        fuel("gas", 36000.0 * 1000, 0.55, 0.10, "HBM natural/refinery gas, LHV ~36 MJ/m³. ELN gas multiplier applied."),
        fuel("naturalgas", 36000.0 * 1000, 0.55, 0.10, "Magneticraft natural gas, LHV ~36 MJ/m³. ELN gas multiplier applied.")
    )

    private val steamDefaults = listOf(
        fuel("spentsteam", 2.257 * 0.1, 0.05, 0.0, "HBM spent/exhausted steam, ~10% residual energy before ELN scaling."),
        fuel("steam", 2.257, 0.15, 0.0, "Forge/HBM steam, heat of vaporization 2.257 J/g before ELN scaling."),
        fuel("ic2steam", 2.257, 0.15, 0.0, "IC2 steam, matched to standard steam."),
        fuel("hotsteam", 2.257 * 2, 0.35, 0.0, "HBM hot steam, ~2x base steam energy before ELN scaling."),
        fuel("ic2superheatedsteam", 2.257 * 3, 0.55, 0.0, "IC2 superheated steam, ~3x base before ELN scaling."),
        fuel("superhotsteam", 2.257 * 4, 0.75, 0.0, "HBM super hot steam, ~4x base before ELN scaling."),
        fuel("ultrahotsteam", 2.257 * 8, 1.0, 0.0, "HBM ultra hot steam, ~8x base before ELN scaling.")
    )

    @JvmStatic
    fun init(baseConfigFile: File) {
        val configDirectory = baseConfigFile.parentFile ?: File("config")
        config = JsonConfig(File(configDirectory, "fluids.cfg"), includeDefaultSpecs = false)
        registerConfigEntries(config)
        config.load()
        config.save()
        config.writeExampleFile()
    }

    private fun registerConfigEntries(config: JsonConfig) {
        config.registerEntry(heatValueFactorPath, defaultHeatValueFactor, "Factor applied when converting real-world heat values to Minecraft heat values.")
        config.registerGroupComment(fuelsPath, "Fuel registry data. Each fuel entry stores heat value and wear properties together.")
        config.registerGroupComment(dieselPath, "Heavy liquid fuels used by fuel heat furnaces.")
        config.registerGroupComment(gasolinePath, "Light liquid fuels usable by combustion engines and gas turbines.")
        config.registerGroupComment(gasPath, "Burnable gases. Stored values are pre-pressurization real-world energy equivalents.")
        config.registerGroupComment(steamPath, "Steam-like working fluids before ELN's global heat-value scaling is applied.")

        registerFuelEntries(config, dieselPath, dieselDefaults)
        registerFuelEntries(config, gasolinePath, gasolineDefaults)
        registerFuelEntries(config, gasPath, gasDefaults)
        registerFuelEntries(config, steamPath, steamDefaults)
    }

    private fun registerFuelEntries(config: JsonConfig, basePath: String, entries: List<FuelEntry>) {
        for (entry in entries) {
            val entryPath = "$basePath.${entry.name}"
            config.registerGroupComment(entryPath, entry.comment)
            config.registerEntry("$entryPath.heatValue", entry.heatValue)
            config.registerEntry("$entryPath.temperatureFactor", entry.temperatureFactor)
            config.registerEntry("$entryPath.cleanlinessFactor", entry.cleanlinessFactor)
        }
    }

    private fun fuelNames(path: String): Array<String> = config.getChildKeys(path).toTypedArray()

    val dieselList: Array<String>
        get() = fuelNames(dieselPath)

    val gasolineList: Array<String>
        get() = fuelNames(gasolinePath)

    val gasList: Array<String>
        get() = fuelNames(gasPath)

    val steamList: Array<String>
        get() = fuelNames(steamPath)

    private fun heatValuePath(categoryPath: String, fuelName: String) = "$categoryPath.$fuelName.heatValue"

    private fun temperatureFactorPath(categoryPath: String, fuelName: String) = "$categoryPath.$fuelName.temperatureFactor"

    private fun cleanlinessFactorPath(categoryPath: String, fuelName: String) = "$categoryPath.$fuelName.cleanlinessFactor"

    private fun fuelCategoryPaths(): List<String> = listOf(dieselPath, gasolinePath, gasPath, steamPath)

    private fun findFuelCategoryPath(fuelName: String): String? =
        fuelCategoryPaths().firstOrNull { path -> config.readPath(heatValuePath(path, fuelName)) != null }

    private fun baseHeatValueForFuel(fuelName: String): Double {
        val categoryPath = findFuelCategoryPath(fuelName) ?: return 0.0
        val baseHeatValue = config.getDoubleOrElse(heatValuePath(categoryPath, fuelName), 0.0)
        return if (categoryPath == steamPath) {
            val factor = config.getDoubleOrElse(heatValueFactorPath, defaultHeatValueFactor)
            if (factor == 0.0) 0.0 else baseHeatValue / factor
        } else {
            baseHeatValue
        }
    }

    fun fluidListToFluids(fluidNames: Array<String>) =
        fluidNames.map { FluidRegistry.getFluid(it) }.filterNotNull().toTypedArray()

    fun heatEnergyPerMilliBucket(fuelName: String): Double =
        config.getDoubleOrElse(heatValueFactorPath, defaultHeatValueFactor) * baseHeatValueForFuel(fuelName)

    fun heatEnergyPerMilliBucket(fluid: Fluid?): Double = heatEnergyPerMilliBucket(fluid?.name ?: "")

    fun fuelEntry(fuelName: String): FuelEntry {
        val categoryPath = findFuelCategoryPath(fuelName)
        return if (categoryPath == null) {
            FuelEntry(
                name = fuelName,
                heatValue = 0.0,
                temperatureFactor = defaultTemperatureFactor,
                cleanlinessFactor = defaultCleanlinessFactor,
                comment = ""
            )
        } else {
            FuelEntry(
                name = fuelName,
                heatValue = config.getDoubleOrElse(heatValuePath(categoryPath, fuelName), 0.0),
                temperatureFactor = config.getDoubleOrElse(
                    temperatureFactorPath(categoryPath, fuelName),
                    defaultTemperatureFactor
                ),
                cleanlinessFactor = config.getDoubleOrElse(
                    cleanlinessFactorPath(categoryPath, fuelName),
                    defaultCleanlinessFactor
                ),
                comment = ""
            )
        }
    }

    fun fuelEntry(fluid: Fluid?): FuelEntry = fuelEntry(fluid?.name ?: "")

    fun temperatureFactor(fuelName: String): Double = fuelEntry(fuelName).temperatureFactor

    fun temperatureFactor(fluid: Fluid?): Double = fuelEntry(fluid).temperatureFactor

    fun cleanlinessFactor(fuelName: String): Double = fuelEntry(fuelName).cleanlinessFactor

    fun cleanlinessFactor(fluid: Fluid?): Double = fuelEntry(fluid).cleanlinessFactor

    private fun fuel(
        name: String,
        heatValue: Double,
        temperatureFactor: Double,
        cleanlinessFactor: Double,
        comment: String
    ) = FuelEntry(name, heatValue, temperatureFactor, cleanlinessFactor, comment)
}
