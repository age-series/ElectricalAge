package mods.eln.item.lampitem

import mods.eln.Eln
import mods.eln.i18n.I18N
import kotlin.math.abs
import kotlin.math.pow

object LampLists {
    val lampTechnologyList = mutableListOf<BoilerplateLampData>()
    val registeredLampList = mutableListOf<SpecificLampData>()

    init {
        lampTechnologyList.add(BoilerplateLampData("incandescent", 24.0, false, 1.0, 0.5, 0.0, 1.0))
        lampTechnologyList.add(BoilerplateLampData("carbon", 12.0, false, 1.0, 0.5, 0.0, 0.75))
        lampTechnologyList.add(BoilerplateLampData("fluorescent", 168.0, false, 2.0, 0.75, 4.0, 0.5))
        lampTechnologyList.add(BoilerplateLampData("farming", 48.0, false, 4.0, 0.5, 0.0, 2.0))
        lampTechnologyList.add(BoilerplateLampData("led", 672.0, false, 2.0, 0.75, 0.0, 0.25))
        lampTechnologyList.add(BoilerplateLampData("halogen", 96.0, false, 1.0, 0.5, 0.0, 3.0))
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
    var nominalLifeInHours: Double, // IRL hours (1 hour = 3600 seconds = 72,000 ticks)
    var infiniteLifeEnabled: Boolean,
    val cropGrowthRateFactor: Double,
    val minimalUFactor: Double,
    val timeUntilStableInSeconds: Double,
    val basePowerMultiplier: Double
) {

    companion object {
        const val MIN_LIGHT_VALUE = 0
        const val MAX_LIGHT_VALUE = 15

        const val T0_NOMINAL_LIGHT_VALUE = 12 // Small 50V
        const val T1_NOMINAL_LIGHT_VALUE = 14 // 50V
        const val T2_NOMINAL_LIGHT_VALUE = 14 // 200V

        const val LAMP_BASE_POWER = 60

        const val MIN_LAMP_LIFE_IN_HOURS = 1.0
        const val MAX_LAMP_LIFE_IN_HOURS = 8760.0 // 1 year (365 days) IRL
    }

    private val nominalLifePath: String
        get() = "lighting.lamps.${lampType}.nominalLifeInHours"

    private val infiniteLifePath: String
        get() = "lighting.lamps.${lampType}.infiniteLifeEnabled"

    fun loadConfig() {
        val configNominalLife = abs(Eln.config.getDoubleOrElse(nominalLifePath, this.nominalLifeInHours))

        if (configNominalLife !in MIN_LAMP_LIFE_IN_HOURS..MAX_LAMP_LIFE_IN_HOURS) {
            Eln.LOGGER.warn(
                I18N.tr("ELN config: Nominal lamp life of type %1$ must be within the range (%2$, %3$)! Changes not applied!",
                this.lampType, MIN_LAMP_LIFE_IN_HOURS, MAX_LAMP_LIFE_IN_HOURS)
            )
        } else {
            this.nominalLifeInHours = configNominalLife
        }

        this.infiniteLifeEnabled = Eln.config.getBooleanOrElse(infiniteLifePath, this.infiniteLifeEnabled)
    }

}

/**
 * The init{} block in the LampDescriptor class modifies some of these values for "small" bulb types.
 * After the voltage tiering update, this behavior should be removed from there and implemented here,
 * assuming "small" bulbs are given their own voltage tier.
 */
data class SpecificLampData(
    val technology: BoilerplateLampData,
    val nominalU: Double,
    var nominalP: Double = BoilerplateLampData.LAMP_BASE_POWER * technology.basePowerMultiplier,
    var resistance: Double = nominalU.pow(2) / nominalP,
    var nominalLightValue: Int = when (nominalU) {
        Eln.LVU -> BoilerplateLampData.T1_NOMINAL_LIGHT_VALUE
        Eln.MVU -> BoilerplateLampData.T2_NOMINAL_LIGHT_VALUE
        else -> BoilerplateLampData.MAX_LIGHT_VALUE
    }
)
