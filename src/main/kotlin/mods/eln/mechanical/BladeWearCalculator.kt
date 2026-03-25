package mods.eln.mechanical

import mods.eln.item.TurbineBladeDescriptor

object BladeWearCalculator {
    // Returns how much faster the blade wears relative to zero-factor conditions (multiplier >= 1.0).
    // Higher temperature or corrosion factors, or lower tier resistances, push this above 1.0.
    fun fuelModifier(
        temperatureFactor: Double,
        cleanlinessFactor: Double,
        blade: TurbineBladeDescriptor
    ): Double {
        val tempComponent = 1.0 + temperatureFactor * (2.0 / blade.temperatureResistance)
        val corrComponent = 1.0 + cleanlinessFactor * (2.0 / blade.corrosionResistance)
        return tempComponent * corrComponent
    }
}
