package mods.eln.item.lampitem

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.misc.NominalVoltage
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

    @JvmStatic
    fun getLampData(lampType: String): BoilerplateLampData? {
        for (lampData in lampTechnologyList) {
            if (lampData.lampType == lampType) return lampData
        }
        return null
    }

    /**
     * This function should be called exactly ONCE during the loading process, BEFORE block/item registration occurs!
     */
    @JvmStatic
    fun translateLampTypes() {
        getLampData("incandescent")!!.translatedLampType = I18N.tr("incandescent")
        getLampData("carbon")!!.translatedLampType = I18N.tr("carbon")
        getLampData("fluorescent")!!.translatedLampType = I18N.tr("fluorescent")
        getLampData("farming")!!.translatedLampType = I18N.tr("farming")
        getLampData("led")!!.translatedLampType = I18N.tr("led")
        getLampData("halogen")!!.translatedLampType = I18N.tr("halogen")
    }
}

data class BoilerplateLampData(
    val lampType: String,
    var nominalLifeInHours: Double, // IRL hours (1 hour = 3600 seconds = 72,000 ticks)
    var infiniteLifeEnabled: Boolean,
    val cropGrowthRateFactor: Double,
    val minimalUFactor: Double,
    val timeUntilStableInSeconds: Double,
    val basePowerMultiplier: Double,
    var translatedLampType: String = "" // Initialized later in the loading process; used for WAILA and context menus
) {

    companion object {
        const val MIN_LIGHT_VALUE = 0
        const val MAX_LIGHT_VALUE = 15

        const val V12_NOMINAL_LIGHT_VALUE = 12
        const val V120_NOMINAL_LIGHT_VALUE = 14
        const val V240_NOMINAL_LIGHT_VALUE = 14

        const val LAMP_BASE_POWER = 60.0

        const val V12_POWER_MULTIPLIER = 0.2
        const val V120_POWER_MULTIPLIER = 1.0
        const val V240_POWER_MULTIPLIER = 1.5

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

data class SpecificLampData(
    val technology: BoilerplateLampData,
    val nominalU: Double,
    val nominalP: Double = when (nominalU) {
        NominalVoltage.V12 -> BoilerplateLampData.LAMP_BASE_POWER * BoilerplateLampData.V12_POWER_MULTIPLIER * technology.basePowerMultiplier
        NominalVoltage.V120 -> BoilerplateLampData.LAMP_BASE_POWER * BoilerplateLampData.V120_POWER_MULTIPLIER * technology.basePowerMultiplier
        NominalVoltage.V240 -> BoilerplateLampData.LAMP_BASE_POWER * BoilerplateLampData.V240_POWER_MULTIPLIER * technology.basePowerMultiplier
        else -> BoilerplateLampData.LAMP_BASE_POWER
    },
    val resistance: Double = nominalU.pow(2) / nominalP,
    val nominalLightValue: Int = when (nominalU) {
        NominalVoltage.V12 -> BoilerplateLampData.V12_NOMINAL_LIGHT_VALUE
        NominalVoltage.V120 -> BoilerplateLampData.V120_NOMINAL_LIGHT_VALUE
        NominalVoltage.V240 -> BoilerplateLampData.V240_NOMINAL_LIGHT_VALUE
        else -> BoilerplateLampData.MAX_LIGHT_VALUE
    }
)
