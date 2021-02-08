package mods.eln.sim.electrical.mna

data class SubSystemMetrics(
    val averageInversionTimeNanoseconds: Double,
    val maximumInversionTimeNanoseconds: Double,
    val inversionCount: Int,
    val singularMatrixCount: Int
)
