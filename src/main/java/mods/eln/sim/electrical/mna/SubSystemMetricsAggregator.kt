package mods.eln.sim.electrical.mna

import mods.eln.metrics.SimpleMetric
import mods.eln.metrics.UnitTypes

object SubSystemMetricsAggregator {
    var matrixSizeAverage = SimpleMetric("matrix_size_avg", "mna", "Size of MNA A matrix", UnitTypes.NO_UNITS)
    var matrixSizeMax = SimpleMetric("matrix_size_max", "mna", "Size of MNA A matrix", UnitTypes.NO_UNITS)
    var singularCount = SimpleMetric("singular_matrix", "mna", "Number of singular matrices per tick", UnitTypes.NO_UNITS)
    var inverseCount = SimpleMetric("inverse_count", "mna", "Number of inverses per tick", UnitTypes.NO_UNITS)
    var inverseTimeAverage = SimpleMetric("inverse_count_avg", "mna", "Average time of inverse", UnitTypes.NANOSECONDS)
    var inverseTimeMaximum = SimpleMetric("inverse_count_max", "mna", "Maximum inverse time", UnitTypes.NANOSECONDS)

    fun aggregateMetrics(list: List<SubSystemMetrics>) {
        matrixSizeAverage.putMetric(list.map{it.matrixSize}.average())
        matrixSizeMax.putMetric(list.map{it.matrixSize.toDouble()}.max()?: 0.0)
        singularCount.putMetric(list.map {it.singularMatrixCount}.sum().toDouble())
        inverseCount.putMetric(list.map { it.inversionCount }.sum().toDouble())
        inverseTimeAverage.putMetric(list.map{it.averageInversionTimeNanoseconds}.average())
        inverseTimeMaximum.putMetric(list.map{it.maximumInversionTimeNanoseconds}.max()?: 0.0)
    }
}
