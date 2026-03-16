package mods.eln.item

import mods.eln.Eln

object LampTechnologies {
    val lampList = mutableListOf<LampData>()

    init {
        lampList.add(LampData("incandescent", 16.0, false, 15, 1.0, 0.5, 0.0, 0.0))
        lampList.add(LampData("carbonIncandescent", 6.0, false, 15, 1.0, 0.5, 0.0, 0.0))
        lampList.add(LampData("fluorescent", 64.0, false, 15, 1.0, 0.75, 0.75, 4.0))
        lampList.add(LampData("farming", 16.0, false, 15, 2.0, 0.5, 0.0, 0.0))
        lampList.add(LampData("led", 512.0, false, 15, 1.0, 0.75, 0.0, 0.0))
        lampList.add(LampData("halogen", 128.0, false, 15, 1.0, 0.5, 0.0, 0.0))
    }

    fun getLampData(lampType: String): LampData? {
        for (lampData in lampList) {
            if (lampData.lampType == lampType) return lampData
        }

        return null
    }
}

data class LampData(
    val lampType: String,
    var nominalLifeInHours: Double,
    var infiniteLifeEnabled: Boolean,
    val nominalLightValue: Int,
    val cropGrowthRateFactor: Double,
    val minimalUFactor: Double,
    val stableUFactor: Double,
    val timeUntilStableInSeconds: Double
) {

    fun loadConfig() {
        // This goofy-looking logic ensures that each lamp entry in the config file has the proper default values
        // (as defined in the init of LampTechnologies) assigned when the file is first created. On subsequent reads,
        // the LampTechnologies entries are updated from the config file.

        if (Eln.config.hasKey("lamp", lampType + "LampNominalLifeInHours")) {
            this.nominalLifeInHours = Eln.config["lamp", lampType + "LampNominalLifeInHours", 0.0].double
        }
        else {
            Eln.config["lamp", lampType + "LampNominalLifeInHours", 0.0].set(this.nominalLifeInHours)
        }

        if (Eln.config.hasKey("lamp", lampType + "LampInfiniteLifeEnabled")) {
            this.infiniteLifeEnabled = Eln.config["lamp", lampType + "LampInfiniteLifeEnabled", false].boolean
        }
        else {
            Eln.config["lamp", lampType + "LampInfiniteLifeEnabled", false].set(this.infiniteLifeEnabled)
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
