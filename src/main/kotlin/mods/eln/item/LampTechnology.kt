package mods.eln.item

import mods.eln.Eln
import kotlin.math.pow
import kotlin.math.sqrt

object LampLists {
    const val MIN_LIGHT_VALUE: Int = 0
    const val MAX_LIGHT_VALUE: Int = 15

    const val LAMP_BASE_POWER: Int = 60

    val lampTechnologyList = mutableListOf<BoilerplateLampData>()
    val registeredLampList = mutableListOf<SpecificLampData>()

    // This will be used to generate a list of bulb names for the command handler.
    // val elnLampTypes = mutableListOf<String>()

    init {
        lampTechnologyList.add(BoilerplateLampData("incandescent", 16.0, false, 15, 1.0, 0.5, 0.0, 0.0, 1.0))
        lampTechnologyList.add(BoilerplateLampData("carbonIncandescent", 6.0, false, 15, 1.0, 0.5, 0.0, 0.0, 1.0))
        lampTechnologyList.add(BoilerplateLampData("fluorescent", 64.0, false, 15, 1.0, 0.75, 0.75, 4.0, 0.5))
        lampTechnologyList.add(BoilerplateLampData("farming", 16.0, false, 15, 2.0, 0.5, 0.0, 0.0, 1.5))
        lampTechnologyList.add(BoilerplateLampData("led", 512.0, false, 15, 1.0, 0.75, 0.0, 0.0, 0.5))
        lampTechnologyList.add(BoilerplateLampData("halogen", 128.0, false, 15, 1.0, 0.5, 0.0, 0.0, 2.0))

        // for (lamps in lampTechnologyList) elnLampTypes.add(lamps.lampType)
    }

    fun getLampData(lampType: String): BoilerplateLampData? {
        for (lampData in lampTechnologyList) {
            if (lampData.lampType == lampType) return lampData
        }

        return null
    }
}

data class BoilerplateLampData(
    val lampType: String,
    var nominalLifeInHours: Double,
    var infiniteLifeEnabled: Boolean,
    val nominalLightValue: Int,
    val cropGrowthRateFactor: Double,
    val minimalUFactor: Double,
    val stableUFactor: Double,
    val timeUntilStableInSeconds: Double,
    val basePowerMultiplier: Double
) {

    fun loadConfig() {
        // This goofy-looking logic ensures that each lamp entry in the config file has the proper default values
        // (as defined in the init of LampTechnologies) assigned when the file is first created. On subsequent reads,
        // the LampTechnologies entries are updated from the config file.
        if (!Eln.config.hasKey("lamp", this.lampType + "LampNominalLifeInHours")) {
            Eln.config["lamp", this.lampType + "LampNominalLifeInHours", 0.0].set(this.nominalLifeInHours)
        }
        else {
            this.nominalLifeInHours = Eln.config["lamp", this.lampType + "LampNominalLifeInHours", 0.0].double
        }

        if (!Eln.config.hasKey("lamp", this.lampType + "LampInfiniteLifeEnabled")) {
            Eln.config["lamp", this.lampType + "LampInfiniteLifeEnabled", false].set(this.infiniteLifeEnabled)
        }
        else {
            this.infiniteLifeEnabled = Eln.config["lamp", this.lampType + "LampInfiniteLifeEnabled", false].boolean
        }
    }

    fun updateNominalLifeConfig(newNominalLifeInHours: Double) {
        this.nominalLifeInHours = newNominalLifeInHours

        Eln.config.get("lamp", lampType + "LampNominalLifeInHours", 0.0).set(newNominalLifeInHours)
        Eln.config.save()
    }

    fun updateInfiniteLifeConfig(infiniteLifeEnabled: Boolean) {
        this.infiniteLifeEnabled = infiniteLifeEnabled

        Eln.config.get("lamp", lampType + "LampInfiniteLifeEnabled", false).set(infiniteLifeEnabled)
        Eln.config.save()
    }

}

data class SpecificLampData(
    val technology: BoilerplateLampData,
    val nominalU: Double,
    var nominalP: Double = LampLists.LAMP_BASE_POWER * technology.basePowerMultiplier * sqrt(nominalU / Eln.LVU),
    val resistance: Double = nominalU.pow(2) / nominalP
)