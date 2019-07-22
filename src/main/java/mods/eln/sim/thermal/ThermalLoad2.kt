package mods.eln.sim.thermal

class ThermalLoad2
/**
 * ThermalLoad2
 *
 * Specific Heat for various materials:
 * Copper: 0.385
 * Iron: 0.450
 * Aluminum: 0.902
 *
 * @param tempEnv Environmental (Biome) Temperature (deg C)
 * @param mass Thermal Mass (kG)
 * @param specificHeat Specific Heat (kJ/kG)
 */
(// environment temperature (deg C)
        internal var tempEnv: Double, // mass (kG)
        internal var mass: Double, // specific heat (kJ/kG)
        internal var specificHeat: Double) {
    // energy (kJ)
    internal var energy: Double = 0.toDouble()
    var isSlow: Boolean = false
        internal set

    val t: Double
        get() = energy / (specificHeat * mass)

    val power: Double
        get() = 0.0

    init {
        this.energy = 0.0
        this.isSlow = true
    }

    fun movePower(power: Double) {
        if (power != Double.NaN)
            energy += power / 0.05
    }

    fun movePower(power: Double, other: ThermalLoad2) {
        if (power != Double.NaN)
            this.energy -= power / 0.05
            other.energy += power / 0.05
    }

    fun movePower(energy: Double, time: Double, other: ThermalLoad2) {
        if (energy != Double.NaN)
            this.energy -= energy / (time * 20)
            other.energy += energy / (time * 20)
    }

    fun updateTempEnv(tempEnv: Double) {
        if (tempEnv < 100 && tempEnv > -100)
            this.tempEnv = tempEnv
    }

    fun setEnergyFromTemp(temp: Double) {
        if (temp != Double.NaN)
            this.energy = specificHeat / energy
    }

    fun setAsSlow() {
        isSlow = true
    }

    fun setAsFast() {
        isSlow = false
    }
}
