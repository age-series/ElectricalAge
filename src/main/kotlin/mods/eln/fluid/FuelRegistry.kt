package mods.eln.fluid

import mods.eln.Eln
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry

object FuelRegistry {
    /**
     * Diesel is a refined, heavy fuel, can only be used by the fuel heat furnace for the moment.
     *
     * The values represent the heating value (energy) for 1L of the fuel IRL.
     *
     * Entries sorted lowest to highest energy per liter.
     */
    private val dieselFuels = mapOf(
        "creosote" to 750000.0,              // Railcraft, coal tar creosote, density ≈ 1.08 kg/L, LHV ≈ 0.7 MJ/L (Engineering Toolbox)
        "hootch" to (17826480.0 * 0.6),      // EnderIO, hootch — roughly equivalent to diluted ethanol (omega)
        "woodoil" to 15000000.0,             // HBM Nuclear Tech, wood oil (pyrolysis oil), density ≈ 1.10 kg/L, LHV ≈ 14 MJ/kg (Bridgwater, "Biomass Fast Pyrolysis")
        "biodiesel" to 32560000.0,           // Immersive Engineering, biodiesel, density = 0.88 kg/L, LHV = 37 MJ/kg (EN 14214)
        "sunfloweroil" to 33000000.0,        // HBM Nuclear Tech, sunflower oil, density ≈ 0.92 kg/L, LHV ≈ 35.9 MJ/kg (Demirbas, "Energy Conversion and Management")
        "fishoil" to 33200000.0,             // HBM Nuclear Tech, fish oil, density ≈ 0.92 kg/L, LHV ≈ 36.1 MJ/kg (Barnwal & Sharma, "Bioresource Technology")
        "oliveoil" to 33400000.0,            // HBM Nuclear Tech, olive oil, density ≈ 0.91 kg/L, LHV ≈ 36.7 MJ/kg (Demirbas, "Energy Conversion and Management")
        "reclaimed" to 34000000.0,           // HBM Nuclear Tech, reclaimed/waste oil, density ≈ 0.88 kg/L, LHV ≈ 38.6 MJ/kg (EPA waste oil guidance)
        "coalcreosote" to 34300000.0,        // HBM Nuclear Tech, coal creosote oil, density ≈ 1.05 kg/L, LHV ≈ 32.7 MJ/kg (Perry's Chemical Engineers' Handbook)
        "bitumen" to 34500000.0,             // HBM Nuclear Tech, bitumen/asphalt, density ≈ 1.02 kg/L, LHV ≈ 33.8 MJ/kg (Engineering Toolbox)
        "crackoil_ds" to 34800000.0,         // HBM Nuclear Tech, desulfurized cracked oil, density ≈ 0.85 kg/L, LHV ≈ 40.9 MJ/kg
        "hotcrackoil_ds" to 34800000.0,      // HBM Nuclear Tech, hot desulfurized cracked oil (same energy as crackoil_ds)
        "oil_coker" to 34800000.0,           // HBM Nuclear Tech, coker oil (delayed coking residue), density ≈ 0.92 kg/L, LHV ≈ 37.8 MJ/kg (Gary & Handwerk, "Petroleum Refining")
        "crackoil" to 35000000.0,            // HBM Nuclear Tech, cracked oil, density ≈ 0.85 kg/L, LHV ≈ 41.2 MJ/kg
        "hotcrackoil" to 35000000.0,         // HBM Nuclear Tech, hot cracked oil (same energy as crackoil)
        "oil_ds" to 35600000.0,              // HBM Nuclear Tech, desulfurized crude oil, density ≈ 0.87 kg/L, LHV ≈ 40.9 MJ/kg (~0.5% less than crude)
        "hotoil_ds" to 35600000.0,           // HBM Nuclear Tech, hot desulfurized oil (same energy as oil_ds)
        "oil" to 35800000.0,                 // HBM Nuclear Tech, crude oil, density ≈ 0.87 kg/L, LHV ≈ 41.2 MJ/kg (Engineering Toolbox)
        "hotoil" to 35800000.0,              // HBM Nuclear Tech, hot crude oil (same energy as oil)
        "lubricant" to 36000000.0,           // HBM Nuclear Tech, lubricating oil, density ≈ 0.88 kg/L, LHV ≈ 40.9 MJ/kg (Engineering Toolbox)
        "heatingoil_vacuum" to 36000000.0,   // HBM Nuclear Tech, vacuum heating oil (heavier cut), density ≈ 0.88 kg/L, LHV ≈ 40.9 MJ/kg
        "heatingoil" to 36500000.0,          // HBM Nuclear Tech, heating oil (No. 2 fuel oil), density ≈ 0.85 kg/L, LHV ≈ 42.9 MJ/kg (Engineering Toolbox)
        "petroleum" to 37000000.0,           // HBM Nuclear Tech, petroleum (refined base stock), density ≈ 0.85 kg/L, LHV ≈ 43.5 MJ/kg (Engineering Toolbox)
        "diesel_crack" to 37500000.0,        // HBM Nuclear Tech, cracked diesel (lighter cut from cracking), density ≈ 0.81 kg/L, LHV ≈ 46.3 MJ/kg
        "diesel_crack_reform" to 38200000.0, // HBM Nuclear Tech, cracked then reformed diesel, density ≈ 0.82 kg/L, LHV ≈ 46.6 MJ/kg
        "heavyoil_vacuum" to 38500000.0,     // HBM Nuclear Tech, vacuum heavy oil (vacuum distillation residue), density ≈ 0.98 kg/L, LHV ≈ 39.3 MJ/kg (Gary & Handwerk)
        "diesel" to 38600000.0,              // Immersive Petroleum + PneumaticCraft + HBM Nuclear Tech, diesel, density ≈ 0.83 kg/L, LHV ≈ 45.5 MJ/kg, ≈ 38.6 MJ/L (Engineering Toolbox)
        "heavyoil" to 39100000.0,            // Magneticraft + HBM Nuclear Tech, heavy fuel oil, density ≈ 0.95 kg/L, LHV ≈ 41.2 MJ/kg (Engineering Toolbox)
        "diesel_reform" to 39200000.0        // HBM Nuclear Tech, reformed diesel (catalytic reforming increases density slightly), density ≈ 0.84 kg/L, LHV ≈ 46.7 MJ/kg
    )
    val dieselList = dieselFuels.keys.toTypedArray()

    /**
     * Gasoline-equivalents: Light oils, the type which can reasonably be burned by internal combustion engines
     * or gas turbines. The ones on this list are all pretty close to each other in energy content.
     *
     * The values represent the heating value (energy) for 1L of the fuel IRL.
     *
     * Entries sorted lowest to highest energy per liter.
     */
    private val gasolineFuels = mapOf(
        "biofuel" to 17826480.0,               // Minefactory Reloaded + HBM Nuclear Tech, bioethanol, density = 0.786 kg/L, LHV ≈ 22.68 MJ/L (NIST)
        "bioethanol" to 17826480.0,            // Forestry, bioethanol, density = 0.786 kg/L, LHV ≈ 22.68 MJ/L
        "ic2biogas" to 17826480.0,             // IC2 biogas (omega)
        "rc ethanol" to 21172000.0,            // RotaryCraft, ethanol, density = 0.79 kg/L, LHV = 26.8 MJ/kg
        "ethanol" to 21340000.0,               // HBM Nuclear Tech, ethanol, density = 0.789 kg/L, LHV = 26.8 MJ/kg, ≈ 21.1 MJ/L (Engineering Toolbox)
        "lpg" to 24840000.0,                   // PneumaticCraft + HBM Nuclear Tech, LPG, density ≈ 0.54 kg/L, LHV ≈ 46 MJ/kg, ≈ 24.8 MJ/L (Engineering Toolbox)
        "naphtha_coker" to 30500000.0,         // HBM Nuclear Tech, coker naphtha (lower quality from delayed coking), density ≈ 0.72 kg/L, LHV ≈ 42.4 MJ/kg (Gary & Handwerk)
        "naphtha_crack" to 30800000.0,         // HBM Nuclear Tech, cracked naphtha (lighter fractions), density ≈ 0.71 kg/L, LHV ≈ 43.4 MJ/kg
        "unsaturateds" to 31000000.0,          // HBM Nuclear Tech, unsaturateds (olefin mixture: propylene/butylene), density ≈ 0.72 kg/L, LHV ≈ 43.1 MJ/kg (Engineering Toolbox)
        "naphtha_ds" to 31200000.0,            // HBM Nuclear Tech, desulfurized naphtha, density ≈ 0.73 kg/L, LHV ≈ 42.7 MJ/kg (~0.7% less than naphtha)
        "naphtha" to 31400000.0,               // HBM Nuclear Tech, petroleum naphtha, density ≈ 0.73 kg/L, LHV ≈ 43.0 MJ/kg, ≈ 31.4 MJ/L (Engineering Toolbox)
        "fuel" to 31570000.0,                  // Buildcraft, refined fuel, density = 0.77 kg/L, LHV = 41 MJ/kg
        "fuelgc" to 31570000.0,                // GalactiCraft, see "fuel"
        "petroil" to 31800000.0,               // HBM Nuclear Tech, petroil (petrol blend), density ≈ 0.74 kg/L, LHV ≈ 43.0 MJ/kg
        "petroil_leaded" to 31800000.0,        // HBM Nuclear Tech, leaded petroil (same energy as petroil)
        "gasoline" to 32200000.0,              // PneumaticCraft + HBM Nuclear Tech, gasoline, density ≈ 0.745 kg/L, LHV ≈ 43.2 MJ/kg, ≈ 32.2 MJ/L (Engineering Toolbox)
        "gasoline_leaded" to 32200000.0,       // HBM Nuclear Tech, leaded gasoline (same energy as gasoline)
        "rocket_fuel" to (17826480.0 * 1.866), // EnderIO, rocket fuel — something like ethanol (omega), ≈ 33.3 MJ/L
        "reformate" to 33500000.0,             // HBM Nuclear Tech, reformate (high-octane catalytic reforming product), density ≈ 0.79 kg/L, LHV ≈ 42.4 MJ/kg (Gary & Handwerk)
        "aromatics" to 34200000.0,             // HBM Nuclear Tech, aromatics (BTX mixture), density ≈ 0.87 kg/L, LHV ≈ 39.3 MJ/kg (Engineering Toolbox, toluene/xylene avg)
        "lightoil_crack" to 34500000.0,        // HBM Nuclear Tech, cracked light oil, density ≈ 0.80 kg/L, LHV ≈ 43.1 MJ/kg
        "kerosene" to 34800000.0,              // PneumaticCraft + HBM Nuclear Tech, kerosene (Jet A-1), density ≈ 0.81 kg/L, LHV ≈ 43.0 MJ/kg, ≈ 34.8 MJ/L (Engineering Toolbox)
        "lightoil_vacuum" to 34800000.0,       // HBM Nuclear Tech, vacuum light oil (heavier cut), density ≈ 0.84 kg/L, LHV ≈ 41.4 MJ/kg
        "lightoil_ds" to 35100000.0,           // HBM Nuclear Tech, desulfurized light oil, density ≈ 0.83 kg/L, LHV ≈ 42.3 MJ/kg
        "lightoil" to 35358000.0,              // Magneticraft + HBM Nuclear Tech, light oil, density ≈ 0.83 kg/L, LHV ≈ 42.6 MJ/kg
        "kerosene_reform" to 35400000.0,       // HBM Nuclear Tech, reformed kerosene (slight increase from reforming), density ≈ 0.82 kg/L, LHV ≈ 43.2 MJ/kg
        "fire_water" to (17826480.0 * 2),      // EnderIO, fire water — something like ethanol (omega), ≈ 35.7 MJ/L
        "highgradekerosene" to 39200000.0      // Silfryi's ContentTweaker scripts, refined kerosene, LHV = 39.2 MJ/L
    )
    val gasolineList = gasolineFuels.keys.toTypedArray()

    /**
     * Burnable gases. Gas turbine is still happy, fuel generator is not.
     *
     * The values represent the heating value (energy) for 1L of the fuel IRL.
     * Note: values below are in MJ/m³ before the ×1000 pressurization multiplier.
     *
     * Entries sorted lowest to highest energy per liter (pre-multiplier).
     */
    private val gasFuels = mapOf(
        "oxyhydrogen" to 7600.0,     // HBM Nuclear Tech, oxyhydrogen (2:1 H2:O2 stoichiometric mix), LHV ≈ 7.6 MJ/m³ (Engineering Toolbox)
        "hydrogen" to 10800.0,       // HBM Nuclear Tech, hydrogen gas, density ≈ 0.09 kg/m³, LHV = 120 MJ/kg, ≈ 10.8 MJ/m³ (Engineering Toolbox)
        "coalgas" to 18000.0,        // HBM Nuclear Tech, coal gas (town gas), LHV ≈ 18 MJ/m³ (Engineering Toolbox)
        "coalgas_leaded" to 18000.0, // HBM Nuclear Tech, leaded coal gas (same energy as coal gas), LHV ≈ 18 MJ/m³
        "syngas" to 20000.0,         // Advanced Generators + HBM Nuclear Tech, syngas (synthesis gas), LHV ≈ 18–20 MJ/m³ (Engineering Toolbox)
        "biogas" to 22000.0,         // HBM Nuclear Tech, biogas (~60% CH4 / 40% CO2), LHV ≈ 22 MJ/m³ (IEA Bioenergy)
        "sourgas" to 25000.0,        // HBM Nuclear Tech, sour gas (natural gas with H2S/CO2), LHV ≈ 25 MJ/m³ (reduced due to impurities)
        "gas_coker" to 28000.0,      // HBM Nuclear Tech, coker gas (off-gas from delayed coking), LHV ≈ 28 MJ/m³ (Gary & Handwerk)
        "reformgas" to 30000.0,      // HBM Nuclear Tech, reform gas (catalytic reformer off-gas), LHV ≈ 30 MJ/m³
        "gas" to 36000.0,            // HBM Nuclear Tech, natural gas / refinery gas, LHV ≈ 36 MJ/m³ (Engineering Toolbox)
        "naturalgas" to 36000.0      // Magneticraft, natural gas, LHV = 36 MJ/m³ (Engineering Toolbox)
    ).mapValues {
        // Multiplied by 1000 because Minecraft gases are -- heavily pressurized.
        it.value * 1000
    }
    val gasList = gasFuels.keys.toTypedArray()

    /**
     * Steam. The value represents the heating value (energy) for 1L of steam IRL.
     *
     * We assume a density of 1g/L to harmonize with other mods.
     *
     * Entries sorted lowest to highest energy per liter.
     */
    private val steam = mapOf(
        "spentsteam" to (2.257 * 0.1),        // HBM Nuclear Tech, spent/exhausted steam, ~10% residual energy
        "steam" to 2.257,                     // Forge standard + HBM Nuclear Tech, heat of vaporization: 2.257 J/g (NIST)
        "ic2steam" to 2.257,                  // IC2 steam, same as standard steam
        "hotsteam" to (2.257 * 2),            // HBM Nuclear Tech, hot steam (~2× base, superheated to ~300°C)
        "ic2superheatedsteam" to (2.257 * 3), // IC2, triple condensed superheated steam
        "superhotsteam" to (2.257 * 4),       // HBM Nuclear Tech, super hot steam (~4× base, superheated to ~540°C)
        "ultrahotsteam" to (2.257 * 8)        // HBM Nuclear Tech, ultra hot steam (~8× base, supercritical conditions)
    ).mapValues {
        // Unusually, the commonly accepted value (2.2) is pretty much correct. Undo the usual mapping.
        it.value / Eln.config.getDoubleOrElse("balance.heat.fuelHeatValueFactor", 0.0000675)
    }
    val steamList = steam.keys.toTypedArray()

    // All fuels together.
    private val allFuels = dieselFuels + gasolineFuels + gasFuels + steam

    fun fluidListToFluids(fluidNames: Array<String>) =
        fluidNames.map { FluidRegistry.getFluid(it) }.filterNotNull().toTypedArray()

    fun heatEnergyPerMilliBucket(fuelName: String): Double = Eln.config.getDoubleOrElse("balance.heat.fuelHeatValueFactor", 0.0000675) * (allFuels[fuelName] ?: 0.0)
    fun heatEnergyPerMilliBucket(fluid: Fluid?): Double = heatEnergyPerMilliBucket(fluid?.name ?: "")
}