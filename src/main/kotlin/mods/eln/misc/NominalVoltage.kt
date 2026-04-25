package mods.eln.misc

/**
 * Central nominal voltages for the new rebalance tiers.
 *
 * Legacy simulator constants such as `Eln.LVU` / `Eln.MVU` remain in place for
 * existing content and save compatibility. New rebalance work should prefer
 * these explicit voltages instead of scattering raw literals.
 */
object NominalVoltage {
    const val V5 = 5.0
    const val V12 = 12.0
    const val V24 = 24.0
    const val V48 = 48.0
    const val V120 = 120.0
    const val V240 = 240.0
    const val V480 = 480.0
    const val V7200 = 7200.0
}
