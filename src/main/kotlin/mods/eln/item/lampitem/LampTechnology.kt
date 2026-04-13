package mods.eln.item.lampitem

import mods.eln.Eln
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object LampLists {
    val lampTechnologyList = mutableListOf<BoilerplateLampData>()
    val registeredLampList = mutableListOf<SpecificLampData>()

    init {
        lampTechnologyList.add(BoilerplateLampData("incandescent", 16.0, false, 15, 1.0, 0.5, 0.0, 1.0))
        lampTechnologyList.add(BoilerplateLampData("carbon", 6.0, false, 15, 1.0, 0.5, 0.0, 1.0))
        lampTechnologyList.add(BoilerplateLampData("fluorescent", 64.0, false, 15, 1.0, 0.75, 4.0, 0.5))
        lampTechnologyList.add(BoilerplateLampData("farming", 16.0, false, 15, 2.0, 0.5, 0.0, 1.5))
        lampTechnologyList.add(BoilerplateLampData("led", 512.0, false, 15, 1.0, 0.75, 0.0, 0.5))
        lampTechnologyList.add(BoilerplateLampData("halogen", 128.0, false, 15, 1.0, 0.5, 0.0, 2.0))
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
    val timeUntilStableInSeconds: Double,
    val basePowerMultiplier: Double
) {

    companion object {
        const val MIN_LIGHT_VALUE: Int = 0
        const val MAX_LIGHT_VALUE: Int = 15
        const val LAMP_BASE_POWER: Int = 60
        const val MIN_LAMP_LIFE_IN_HOURS: Double = 0.016666666666666666 // 1 minute IRL
        const val MAX_LAMP_LIFE_IN_HOURS: Double = 8760.0 // 1 year IRL
    }

    private val nominalLifePath: String
        get() = "lighting.lamps.${lampType}.nominalLifeHours"

    private val infiniteLifePath: String
        get() = "lighting.lamps.${lampType}.infiniteLifeEnabled"

    fun loadConfig() {
        val configNominalLife = abs(Eln.config.getDoubleOrElse(nominalLifePath, this.nominalLifeInHours))

        if (configNominalLife !in MIN_LAMP_LIFE_IN_HOURS..MAX_LAMP_LIFE_IN_HOURS) {
            Eln.LOGGER.warn("ELN config: Nominal lamp life of type ${this.lampType} must be within the range (${MIN_LAMP_LIFE_IN_HOURS}, ${MAX_LAMP_LIFE_IN_HOURS})! Changes not applied!")
        } else {
            this.nominalLifeInHours = configNominalLife
        }

        this.infiniteLifeEnabled = Eln.config.getBooleanOrElse(infiniteLifePath, this.infiniteLifeEnabled)
    }

}

data class SpecificLampData(
    val technology: BoilerplateLampData,
    val nominalU: Double,
    var nominalP: Double = BoilerplateLampData.LAMP_BASE_POWER * technology.basePowerMultiplier * sqrt(nominalU / Eln.LVU),
    val resistance: Double = nominalU.pow(2) / nominalP
)
