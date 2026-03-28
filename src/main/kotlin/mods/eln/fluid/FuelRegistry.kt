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
        fuel("creosote", 750000.0, 0.5, 0.3, "Railcraft, coal tar creosote, density approximately 1.08 kg/L, LHV approximately 0.7 MJ/L (Engineering Toolbox)."),
        fuel("hootch", 17826480.0 * 0.6, 0.5, 0.3, "EnderIO, hootch, roughly equivalent to diluted ethanol."),
        fuel("woodoil", 15000000.0, 0.5, 0.3, "HBM Nuclear Tech, wood oil (pyrolysis oil), density approximately 1.10 kg/L, LHV approximately 14 MJ/kg (Bridgwater, Biomass Fast Pyrolysis)."),
        fuel("biodiesel", 32560000.0, 0.5, 0.3, "Immersive Engineering, biodiesel, density 0.88 kg/L, LHV 37 MJ/kg (EN 14214)."),
        fuel("sunfloweroil", 33000000.0, 0.5, 0.3, "HBM Nuclear Tech, sunflower oil, density approximately 0.92 kg/L, LHV approximately 35.9 MJ/kg (Demirbas, Energy Conversion and Management)."),
        fuel("fishoil", 33200000.0, 0.5, 0.3, "HBM Nuclear Tech, fish oil, density approximately 0.92 kg/L, LHV approximately 36.1 MJ/kg (Barnwal and Sharma, Bioresource Technology)."),
        fuel("oliveoil", 33400000.0, 0.5, 0.3, "HBM Nuclear Tech, olive oil, density approximately 0.91 kg/L, LHV approximately 36.7 MJ/kg (Demirbas, Energy Conversion and Management)."),
        fuel("reclaimed", 34000000.0, 0.5, 0.3, "HBM Nuclear Tech, reclaimed or waste oil, density approximately 0.88 kg/L, LHV approximately 38.6 MJ/kg (EPA waste oil guidance)."),
        fuel("coalcreosote", 34300000.0, 0.5, 0.3, "HBM Nuclear Tech, coal creosote oil, density approximately 1.05 kg/L, LHV approximately 32.7 MJ/kg (Perry's Chemical Engineers' Handbook)."),
        fuel("bitumen", 34500000.0, 0.5, 0.3, "HBM Nuclear Tech, bitumen or asphalt, density approximately 1.02 kg/L, LHV approximately 33.8 MJ/kg (Engineering Toolbox)."),
        fuel("crackoil_ds", 34800000.0, 0.5, 0.3, "HBM Nuclear Tech, desulfurized cracked oil, density approximately 0.85 kg/L, LHV approximately 40.9 MJ/kg."),
        fuel("hotcrackoil_ds", 34800000.0, 0.5, 0.3, "HBM Nuclear Tech, hot desulfurized cracked oil with the same energy as crackoil_ds."),
        fuel("oil_coker", 34800000.0, 0.5, 0.3, "HBM Nuclear Tech, coker oil (delayed coking residue), density approximately 0.92 kg/L, LHV approximately 37.8 MJ/kg (Gary and Handwerk, Petroleum Refining)."),
        fuel("crackoil", 35000000.0, 0.5, 0.3, "HBM Nuclear Tech, cracked oil, density approximately 0.85 kg/L, LHV approximately 41.2 MJ/kg."),
        fuel("hotcrackoil", 35000000.0, 0.5, 0.3, "HBM Nuclear Tech, hot cracked oil with the same energy as crackoil."),
        fuel("oil_ds", 35600000.0, 0.5, 0.3, "HBM Nuclear Tech, desulfurized crude oil, density approximately 0.87 kg/L, LHV approximately 40.9 MJ/kg."),
        fuel("hotoil_ds", 35600000.0, 0.5, 0.3, "HBM Nuclear Tech, hot desulfurized oil with the same energy as oil_ds."),
        fuel("oil", 35800000.0, 0.5, 0.3, "HBM Nuclear Tech, crude oil, density approximately 0.87 kg/L, LHV approximately 41.2 MJ/kg (Engineering Toolbox)."),
        fuel("hotoil", 35800000.0, 0.5, 0.3, "HBM Nuclear Tech, hot crude oil with the same energy as oil."),
        fuel("lubricant", 36000000.0, 0.5, 0.3, "HBM Nuclear Tech, lubricating oil, density approximately 0.88 kg/L, LHV approximately 40.9 MJ/kg (Engineering Toolbox)."),
        fuel("heatingoil_vacuum", 36000000.0, 0.5, 0.3, "HBM Nuclear Tech, vacuum heating oil, density approximately 0.88 kg/L, LHV approximately 40.9 MJ/kg."),
        fuel("heatingoil", 36500000.0, 0.5, 0.3, "HBM Nuclear Tech, heating oil (No. 2 fuel oil), density approximately 0.85 kg/L, LHV approximately 42.9 MJ/kg (Engineering Toolbox)."),
        fuel("petroleum", 37000000.0, 0.5, 0.3, "HBM Nuclear Tech, petroleum base stock, density approximately 0.85 kg/L, LHV approximately 43.5 MJ/kg (Engineering Toolbox)."),
        fuel("diesel_crack", 37500000.0, 0.5, 0.3, "HBM Nuclear Tech, cracked diesel, density approximately 0.81 kg/L, LHV approximately 46.3 MJ/kg."),
        fuel("diesel_crack_reform", 38200000.0, 0.5, 0.3, "HBM Nuclear Tech, cracked then reformed diesel, density approximately 0.82 kg/L, LHV approximately 46.6 MJ/kg."),
        fuel("heavyoil_vacuum", 38500000.0, 0.5, 0.3, "HBM Nuclear Tech, vacuum heavy oil, density approximately 0.98 kg/L, LHV approximately 39.3 MJ/kg (Gary and Handwerk, Petroleum Refining)."),
        fuel("diesel", 38600000.0, 0.5, 0.3, "Immersive Petroleum, PneumaticCraft, and HBM Nuclear Tech diesel, density approximately 0.83 kg/L, LHV approximately 45.5 MJ/kg, about 38.6 MJ/L (Engineering Toolbox)."),
        fuel("heavyoil", 39100000.0, 0.5, 0.3, "Magneticraft and HBM Nuclear Tech heavy fuel oil, density approximately 0.95 kg/L, LHV approximately 41.2 MJ/kg (Engineering Toolbox)."),
        fuel("diesel_reform", 39200000.0, 0.5, 0.3, "HBM Nuclear Tech reformed diesel, density approximately 0.84 kg/L, LHV approximately 46.7 MJ/kg.")
    )

    private val gasolineDefaults = listOf(
        fuel("biofuel", 17826480.0, 0.40, 0.05, "MineFactory Reloaded and HBM Nuclear Tech bioethanol, density 0.786 kg/L, LHV approximately 22.68 MJ/L (NIST)."),
        fuel("bioethanol", 17826480.0, 0.40, 0.05, "Forestry bioethanol, density 0.786 kg/L, LHV approximately 22.68 MJ/L."),
        fuel("ic2biogas", 17826480.0, 0.40, 0.10, "IC2 biogas, treated here as diluted ethanol."),
        fuel("rc ethanol", 21172000.0, 0.40, 0.05, "RotaryCraft ethanol, density 0.79 kg/L, LHV 26.8 MJ/kg."),
        fuel("ethanol", 21340000.0, 0.40, 0.05, "HBM Nuclear Tech ethanol, density 0.789 kg/L, LHV 26.8 MJ/kg, about 21.1 MJ/L (Engineering Toolbox)."),
        fuel("lpg", 24840000.0, 0.45, 0.05, "PneumaticCraft and HBM Nuclear Tech LPG, density approximately 0.54 kg/L, LHV approximately 46 MJ/kg, about 24.8 MJ/L (Engineering Toolbox)."),
        fuel("naphtha_coker", 30500000.0, 0.50, 0.40, "HBM Nuclear Tech coker naphtha, density approximately 0.72 kg/L, LHV approximately 42.4 MJ/kg (Gary and Handwerk, Petroleum Refining)."),
        fuel("naphtha_crack", 30800000.0, 0.50, 0.35, "HBM Nuclear Tech cracked naphtha, density approximately 0.71 kg/L, LHV approximately 43.4 MJ/kg."),
        fuel("unsaturateds", 31000000.0, 0.50, 0.25, "HBM Nuclear Tech unsaturateds (olefin mixture), density approximately 0.72 kg/L, LHV approximately 43.1 MJ/kg (Engineering Toolbox)."),
        fuel("naphtha_ds", 31200000.0, 0.50, 0.08, "HBM Nuclear Tech desulfurized naphtha, density approximately 0.73 kg/L, LHV approximately 42.7 MJ/kg."),
        fuel("naphtha", 31400000.0, 0.50, 0.25, "HBM Nuclear Tech petroleum naphtha, density approximately 0.73 kg/L, LHV approximately 43.0 MJ/kg, about 31.4 MJ/L (Engineering Toolbox)."),
        fuel("fuel", 31570000.0, 0.55, 0.10, "BuildCraft refined fuel, density 0.77 kg/L, LHV 41 MJ/kg."),
        fuel("fuelgc", 31570000.0, 0.55, 0.10, "Galacticraft fuel, matched to BuildCraft refined fuel."),
        fuel("petroil", 31800000.0, 0.55, 0.20, "HBM Nuclear Tech petroil blend, density approximately 0.74 kg/L, LHV approximately 43.0 MJ/kg."),
        fuel("petroil_leaded", 31800000.0, 0.55, 0.50, "HBM Nuclear Tech leaded petroil with the same energy as petroil."),
        fuel("gasoline", 32200000.0, 0.55, 0.10, "PneumaticCraft and HBM Nuclear Tech gasoline, density approximately 0.745 kg/L, LHV approximately 43.2 MJ/kg, about 32.2 MJ/L (Engineering Toolbox)."),
        fuel("gasoline_leaded", 32200000.0, 0.55, 0.40, "HBM Nuclear Tech leaded gasoline with the same energy as gasoline."),
        fuel("rocket_fuel", 17826480.0 * 1.866, 0.60, 0.05, "EnderIO rocket fuel, approximated from ethanol-like energy density."),
        fuel("reformate", 33500000.0, 0.55, 0.08, "HBM Nuclear Tech reformate, density approximately 0.79 kg/L, LHV approximately 42.4 MJ/kg (Gary and Handwerk, Petroleum Refining)."),
        fuel("aromatics", 34200000.0, 0.55, 0.15, "HBM Nuclear Tech aromatics (BTX mixture), density approximately 0.87 kg/L, LHV approximately 39.3 MJ/kg (Engineering Toolbox)."),
        fuel("lightoil_crack", 34500000.0, 0.60, 0.30, "HBM Nuclear Tech cracked light oil, density approximately 0.80 kg/L, LHV approximately 43.1 MJ/kg."),
        fuel("kerosene", 34800000.0, 0.55, 0.10, "PneumaticCraft and HBM Nuclear Tech kerosene (Jet A-1), density approximately 0.81 kg/L, LHV approximately 43.0 MJ/kg, about 34.8 MJ/L (Engineering Toolbox)."),
        fuel("lightoil_vacuum", 34800000.0, 0.60, 0.25, "HBM Nuclear Tech vacuum light oil, density approximately 0.84 kg/L, LHV approximately 41.4 MJ/kg."),
        fuel("lightoil_ds", 35100000.0, 0.60, 0.05, "HBM Nuclear Tech desulfurized light oil, density approximately 0.83 kg/L, LHV approximately 42.3 MJ/kg."),
        fuel("lightoil", 35358000.0, 0.60, 0.20, "Magneticraft and HBM Nuclear Tech light oil, density approximately 0.83 kg/L, LHV approximately 42.6 MJ/kg."),
        fuel("kerosene_reform", 35400000.0, 0.55, 0.05, "HBM Nuclear Tech reformed kerosene, density approximately 0.82 kg/L, LHV approximately 43.2 MJ/kg."),
        fuel("fire_water", 17826480.0 * 2, 0.60, 0.05, "EnderIO fire water, approximated at roughly twice ethanol energy density."),
        fuel("highgradekerosene", 39200000.0, 0.60, 0.05, "ContentTweaker refined kerosene, LHV 39.2 MJ/L.")
    )

    private val gasDefaults = listOf(
        fuel("oxyhydrogen", 7600.0 * 1000, 0.30, 0.0, "HBM Nuclear Tech oxyhydrogen (2:1 H2:O2 mix), LHV approximately 7.6 MJ/m^3 (Engineering Toolbox). Stored with the ELN gas pressurization multiplier applied."),
        fuel("hydrogen", 10800.0 * 1000, 0.20, 0.0, "HBM Nuclear Tech hydrogen gas, density approximately 0.09 kg/m^3, LHV 120 MJ/kg, about 10.8 MJ/m^3 (Engineering Toolbox). Stored with the ELN gas pressurization multiplier applied."),
        fuel("coalgas", 18000.0 * 1000, 0.30, 0.30, "HBM Nuclear Tech coal gas, LHV approximately 18 MJ/m^3 (Engineering Toolbox). Stored with the ELN gas pressurization multiplier applied."),
        fuel("coalgas_leaded", 18000.0 * 1000, 0.30, 0.60, "HBM Nuclear Tech leaded coal gas with the same energy as coalgas. Stored with the ELN gas pressurization multiplier applied."),
        fuel("syngas", 20000.0 * 1000, 0.35, 0.10, "Advanced Generators and HBM Nuclear Tech syngas, LHV approximately 18 to 20 MJ/m^3 (Engineering Toolbox). Stored with the ELN gas pressurization multiplier applied."),
        fuel("biogas", 22000.0 * 1000, 0.35, 0.15, "HBM Nuclear Tech biogas, about 60 percent CH4 and 40 percent CO2, LHV approximately 22 MJ/m^3 (IEA Bioenergy). Stored with the ELN gas pressurization multiplier applied."),
        fuel("sourgas", 25000.0 * 1000, 0.45, 1.0, "HBM Nuclear Tech sour gas, reduced energy due to impurities, LHV approximately 25 MJ/m^3. Stored with the ELN gas pressurization multiplier applied."),
        fuel("gas_coker", 28000.0 * 1000, 0.50, 0.50, "HBM Nuclear Tech coker gas, LHV approximately 28 MJ/m^3 (Gary and Handwerk, Petroleum Refining). Stored with the ELN gas pressurization multiplier applied."),
        fuel("reformgas", 30000.0 * 1000, 0.50, 0.20, "HBM Nuclear Tech reform gas, LHV approximately 30 MJ/m^3. Stored with the ELN gas pressurization multiplier applied."),
        fuel("gas", 36000.0 * 1000, 0.55, 0.10, "HBM Nuclear Tech natural gas or refinery gas, LHV approximately 36 MJ/m^3 (Engineering Toolbox). Stored with the ELN gas pressurization multiplier applied."),
        fuel("naturalgas", 36000.0 * 1000, 0.55, 0.10, "Magneticraft natural gas, LHV approximately 36 MJ/m^3 (Engineering Toolbox). Stored with the ELN gas pressurization multiplier applied.")
    )

    private val steamDefaults = listOf(
        fuel("spentsteam", 2.257 * 0.1, 0.05, 0.0, "HBM Nuclear Tech spent or exhausted steam, about 10 percent residual energy before ELN scaling is applied."),
        fuel("steam", 2.257, 0.15, 0.0, "Forge standard and HBM Nuclear Tech steam, heat of vaporization 2.257 J/g before ELN scaling is applied (NIST)."),
        fuel("ic2steam", 2.257, 0.15, 0.0, "IC2 steam, matched to standard steam before ELN scaling is applied."),
        fuel("hotsteam", 2.257 * 2, 0.35, 0.0, "HBM Nuclear Tech hot steam, about twice base steam energy before ELN scaling is applied."),
        fuel("ic2superheatedsteam", 2.257 * 3, 0.55, 0.0, "IC2 superheated steam, triple condensed steam energy before ELN scaling is applied."),
        fuel("superhotsteam", 2.257 * 4, 0.75, 0.0, "HBM Nuclear Tech super hot steam, about four times base steam energy before ELN scaling is applied."),
        fuel("ultrahotsteam", 2.257 * 8, 1.0, 0.0, "HBM Nuclear Tech ultra hot steam, about eight times base steam energy before ELN scaling is applied.")
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
