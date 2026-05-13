package mods.eln.sim.mna

import mods.eln.Eln
import mods.eln.disableLog4jJmx
import mods.eln.metrics.MetricsSubsystem
import mods.eln.sim.mna.component.CurrentSource
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RootSystemSimMetricsProfilingTest {
    private enum class ScenarioShape {
        SINGLE_SYSTEM,
        MANY_SUBSYSTEMS
    }

    data class ScenarioResult(
        val elapsedRunsNanos: List<Long>,
        val matrixRows: Int,
        val matrixCols: Int,
        val subsystemCount: Int,
        val dummyPublishes: Int
    )

    @Test
    fun keepsSimulatorMetricsInactiveWhenMqttIsDisabled() {
        disableLog4jJmx()
        Eln.debugEnabled = false
        Eln.mqttEnabled = false
        Eln.simMetricsEnabled = true
        Eln.simMetricsMqttServer = "dummy"
        Eln.simMetricsId = "disabled-mqtt"
        Eln.simMetricsPublishIntervalTicks = 1
        MetricsSubsystem.shutdown()
        MetricsSubsystem.refreshFromConfig()

        assertFalse(MetricsSubsystem.isSimulatorMetricsActive())

        val root = buildComplexRootSystem(40)
        root.generate()
        repeat(50) { root.step() }
        Thread.sleep(50)

        assertEquals(0, MetricsSubsystem.getDummyPublishCountForTests())
        assertEquals(0, MetricsSubsystem.getDummyInversionCountForTests())
        assertEquals(0, MetricsSubsystem.getDummySingularCountForTests())

        MetricsSubsystem.shutdown()
    }

    @Test
    fun profilesMnaWithAndWithoutDummyMqttMetrics() {
        disableLog4jJmx()
        Eln.debugEnabled = false

        val withoutMetrics = runScenario(
            enableMetrics = false,
            shape = ScenarioShape.SINGLE_SYSTEM,
            matrixSize = 100,
            repetitions = 3,
            warmupSteps = 30,
            measuredSteps = 250
        )
        val withMetrics = runScenario(
            enableMetrics = true,
            shape = ScenarioShape.SINGLE_SYSTEM,
            matrixSize = 100,
            repetitions = 3,
            warmupSteps = 30,
            measuredSteps = 250
        )

        assertEquals(100, withoutMetrics.matrixRows)
        assertEquals(100, withoutMetrics.matrixCols)
        assertEquals(1, withoutMetrics.subsystemCount)
        assertEquals(100, withMetrics.matrixRows)
        assertEquals(100, withMetrics.matrixCols)
        assertEquals(1, withMetrics.subsystemCount)
        assertEquals(0, withoutMetrics.dummyPublishes)
        assertTrue(withMetrics.dummyPublishes > 0, "Expected dummy MQTT metrics sink to receive publishes")

        val ratio = bestNanos(withMetrics).toDouble() / bestNanos(withoutMetrics).toDouble()
        assertTrue(
            ratio <= 2.25,
            "Sim metrics overhead is too high. ratio=$ratio without=${bestNanos(withoutMetrics)}ns with=${bestNanos(withMetrics)}ns"
        )
    }

    @Test
    fun profilesLargeMatrixWithMultiIterationSummary() {
        disableLog4jJmx()
        Eln.debugEnabled = false

        val matrixSize = 220
        val withoutMetrics = runScenario(
            enableMetrics = false,
            shape = ScenarioShape.SINGLE_SYSTEM,
            matrixSize = matrixSize,
            repetitions = 12,
            warmupSteps = 50,
            measuredSteps = 750
        )
        val withMetrics = runScenario(
            enableMetrics = true,
            shape = ScenarioShape.SINGLE_SYSTEM,
            matrixSize = matrixSize,
            repetitions = 12,
            warmupSteps = 50,
            measuredSteps = 750
        )

        assertEquals(matrixSize, withoutMetrics.matrixRows)
        assertEquals(matrixSize, withoutMetrics.matrixCols)
        assertEquals(1, withoutMetrics.subsystemCount)
        assertEquals(matrixSize, withMetrics.matrixRows)
        assertEquals(matrixSize, withMetrics.matrixCols)
        assertEquals(1, withMetrics.subsystemCount)
        assertEquals(0, withoutMetrics.dummyPublishes)
        assertTrue(withMetrics.dummyPublishes > 0, "Expected dummy MQTT metrics sink to receive publishes")

        val ratios = pairedRatios(withoutMetrics.elapsedRunsNanos, withMetrics.elapsedRunsNanos)
        val ratioMean = mean(ratios)
        val ratioMedian = percentile(ratios, 0.5)
        val ratioP95 = percentile(ratios, 0.95)
        println(
            "SIM_METRICS_BENCH matrix=${matrixSize}x${matrixSize} runs=${ratios.size} " +
                "baseline_mean_ns=${meanLong(withoutMetrics.elapsedRunsNanos)} baseline_median_ns=${percentileLong(withoutMetrics.elapsedRunsNanos, 0.5)} " +
                "instrumented_mean_ns=${meanLong(withMetrics.elapsedRunsNanos)} instrumented_median_ns=${percentileLong(withMetrics.elapsedRunsNanos, 0.5)} " +
                "ratio_mean=$ratioMean ratio_median=$ratioMedian ratio_p95=$ratioP95 " +
                "dummyPublishes=${withMetrics.dummyPublishes}"
        )

        assertTrue(ratios.isNotEmpty(), "Expected ratio measurements")
        assertTrue(ratioP95 <= 2.50, "P95 simulator overhead too high: p95=$ratioP95 ratios=$ratios")
    }

    @Test
    fun comparesSmallAndLargeMatrixOverhead() {
        disableLog4jJmx()
        Eln.debugEnabled = false

        val sizes = listOf(4, 10, 40, 220)
        val summaries = ArrayList<Pair<Int, Triple<Double, Double, Double>>>()

        sizes.forEach { size ->
            val withoutMetrics = runScenario(
                enableMetrics = false,
                shape = ScenarioShape.SINGLE_SYSTEM,
                matrixSize = size,
                repetitions = 12,
                warmupSteps = 50,
                measuredSteps = 750
            )
            val withMetrics = runScenario(
                enableMetrics = true,
                shape = ScenarioShape.SINGLE_SYSTEM,
                matrixSize = size,
                repetitions = 12,
                warmupSteps = 50,
                measuredSteps = 750
            )

            assertEquals(size, withoutMetrics.matrixRows)
            assertEquals(size, withoutMetrics.matrixCols)
            assertEquals(1, withoutMetrics.subsystemCount)
            assertEquals(size, withMetrics.matrixRows)
            assertEquals(size, withMetrics.matrixCols)
            assertEquals(1, withMetrics.subsystemCount)
            assertEquals(0, withoutMetrics.dummyPublishes)
            assertTrue(withMetrics.dummyPublishes > 0)

            val ratios = pairedRatios(withoutMetrics.elapsedRunsNanos, withMetrics.elapsedRunsNanos)
            val ratioMean = mean(ratios)
            val ratioMedian = percentile(ratios, 0.5)
            val ratioP95 = percentile(ratios, 0.95)
            summaries += size to Triple(ratioMean, ratioMedian, ratioP95)
            println(
                "SIM_METRICS_SWEEP matrix=${size}x${size} runs=${ratios.size} " +
                    "ratio_mean=$ratioMean ratio_median=$ratioMedian ratio_p95=$ratioP95 " +
                    "dummyPublishes=${withMetrics.dummyPublishes}"
            )
        }

        val tinyP95 = summaries.first { it.first == 4 }.second.third
        val smallP95 = summaries.first { it.first == 10 }.second.third
        val mediumP95 = summaries.first { it.first == 40 }.second.third
        val largeP95 = summaries.first { it.first == 220 }.second.third
        println("SIM_METRICS_SWEEP_COMPARE tiny_p95=$tinyP95 small_p95=$smallP95 medium_p95=$mediumP95 large_p95=$largeP95")
    }

    @Test
    fun profilesManySubsystemWorldOverhead() {
        disableLog4jJmx()
        Eln.debugEnabled = false

        val withoutMetrics = runScenario(
            enableMetrics = false,
            shape = ScenarioShape.MANY_SUBSYSTEMS,
            matrixSize = 10,
            repetitions = 10,
            warmupSteps = 40,
            measuredSteps = 600
        )
        val withMetrics = runScenario(
            enableMetrics = true,
            shape = ScenarioShape.MANY_SUBSYSTEMS,
            matrixSize = 10,
            repetitions = 10,
            warmupSteps = 40,
            measuredSteps = 600
        )

        assertEquals(10, withoutMetrics.matrixRows)
        assertEquals(10, withoutMetrics.matrixCols)
        assertTrue(withoutMetrics.subsystemCount >= 24, "Expected fragmented subsystem scenario")
        assertEquals(withoutMetrics.subsystemCount, withMetrics.subsystemCount)
        assertEquals(0, withoutMetrics.dummyPublishes)
        assertTrue(withMetrics.dummyPublishes > 0)

        val ratios = pairedRatios(withoutMetrics.elapsedRunsNanos, withMetrics.elapsedRunsNanos)
        val ratioMean = mean(ratios)
        val ratioMedian = percentile(ratios, 0.5)
        val ratioP95 = percentile(ratios, 0.95)
        println(
            "SIM_METRICS_FRAGMENTED subsystems=${withMetrics.subsystemCount} matrix=10x10 runs=${ratios.size} " +
                "baseline_mean_ns=${meanLong(withoutMetrics.elapsedRunsNanos)} baseline_median_ns=${percentileLong(withoutMetrics.elapsedRunsNanos, 0.5)} " +
                "instrumented_mean_ns=${meanLong(withMetrics.elapsedRunsNanos)} instrumented_median_ns=${percentileLong(withMetrics.elapsedRunsNanos, 0.5)} " +
                "ratio_mean=$ratioMean ratio_median=$ratioMedian ratio_p95=$ratioP95 " +
                "dummyPublishes=${withMetrics.dummyPublishes}"
        )

        assertTrue(ratios.isNotEmpty(), "Expected ratio measurements")
        assertTrue(ratioP95 <= 1.75, "Fragmented-world simulator overhead too high: p95=$ratioP95 ratios=$ratios")
    }

    private fun runScenario(
        enableMetrics: Boolean,
        shape: ScenarioShape,
        matrixSize: Int,
        repetitions: Int,
        warmupSteps: Int,
        measuredSteps: Int
    ): ScenarioResult {
        Eln.mqttEnabled = enableMetrics
        Eln.simMetricsEnabled = enableMetrics
        Eln.simMetricsMqttServer = "dummy"
        Eln.simMetricsId = if (enableMetrics) "perf-on-$matrixSize" else "perf-off-$matrixSize"
        Eln.simMetricsPublishIntervalTicks = 1
        MetricsSubsystem.shutdown()
        MetricsSubsystem.refreshFromConfig()

        val elapsedRuns = ArrayList<Long>(repetitions)
        var matrixRows = 0
        var matrixCols = 0
        var subsystemCount = 0

        repeat(repetitions) {
            val root = when (shape) {
                ScenarioShape.SINGLE_SYSTEM -> buildComplexRootSystem(matrixSize)
                ScenarioShape.MANY_SUBSYSTEMS -> buildFragmentedRootSystem(matrixSize, subsystemCount = 24)
            }
            root.generate()
            assertTrue(root.systems.isNotEmpty())
            subsystemCount = root.systems.size

            val snapshot = root.systems[0].captureDebugSnapshot()
            assertFalse(snapshot.isSingular)
            val matrix = snapshot.conductanceMatrix
            matrixRows = matrix.size
            matrixCols = if (matrix.isNotEmpty()) matrix[0].size else 0

            repeat(warmupSteps) { root.step() }
            val start = System.nanoTime()
            repeat(measuredSteps) { root.step() }
            elapsedRuns.add(System.nanoTime() - start)
        }

        if (enableMetrics) {
            Thread.sleep(150)
        }
        val publishes = MetricsSubsystem.getDummyPublishCountForTests()
        MetricsSubsystem.shutdown()
        return ScenarioResult(elapsedRuns, matrixRows, matrixCols, subsystemCount, publishes)
    }

    private fun buildComplexRootSystem(matrixSize: Int): RootSystem {
        require(matrixSize >= 4)
        val root = RootSystem(0.1, 1)
        val nodeCount = matrixSize - 1
        val nodes = List(nodeCount) { VoltageState() }
        nodes.forEach { root.addState(it) }

        for (i in 0 until nodeCount) {
            val next = (i + 1) % nodeCount
            root.addComponent(Resistor(nodes[i], nodes[next]).setResistance(8.0 + (i % 9)))

            if (i % 3 == 0) {
                val cross = (i + 13) % nodeCount
                root.addComponent(Resistor(nodes[i], nodes[cross]).setResistance(20.0 + (i % 11)))
            }

            if (i % 2 == 0) {
                root.addComponent(Resistor(nodes[i], null).setResistance(900.0 + i))
            }
        }

        root.addComponent(VoltageSource("v-main", nodes[0], null).setVoltage(120.0))

        var i = 1
        while (i < nodeCount) {
            val b = (i + 17) % nodeCount
            root.addComponent(CurrentSource("i-$i", nodes[i], nodes[b]).setCurrent(0.3 + (i % 5) * 0.05))
            i += 4
        }
        i = 2
        while (i < nodeCount) {
            root.addComponent(CurrentSource("ig-$i", nodes[i], null).setCurrent(0.1 + i * 0.01))
            i += 3
        }

        return root
    }

    private fun buildFragmentedRootSystem(matrixSize: Int, subsystemCount: Int): RootSystem {
        require(matrixSize >= 4)
        require(subsystemCount >= 2)
        val root = RootSystem(0.1, 1)
        repeat(subsystemCount) {
            val subsystem = buildComplexRootSystem(matrixSize)
            subsystem.addStates.forEach { root.addState(it) }
            subsystem.addComponents.forEach { root.addComponent(it) }
        }
        return root
    }

    private fun bestNanos(result: ScenarioResult): Long {
        return result.elapsedRunsNanos.minOrNull() ?: Long.MAX_VALUE
    }

    private fun pairedRatios(baseline: List<Long>, instrumented: List<Long>): List<Double> {
        val size = minOf(baseline.size, instrumented.size)
        val ratios = ArrayList<Double>(size)
        for (idx in 0 until size) {
            val b = baseline[idx]
            val i = instrumented[idx]
            if (b <= 0L) continue
            ratios.add(i.toDouble() / b.toDouble())
        }
        return ratios
    }

    private fun mean(values: List<Double>): Double {
        if (values.isEmpty()) return Double.NaN
        return values.sum() / values.size.toDouble()
    }

    private fun meanLong(values: List<Long>): Double {
        if (values.isEmpty()) return Double.NaN
        return values.sum().toDouble() / values.size.toDouble()
    }

    private fun percentile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return Double.NaN
        val sorted = values.sorted()
        val clamped = p.coerceIn(0.0, 1.0)
        val index = ((sorted.size - 1) * clamped).toInt()
        return sorted[index]
    }

    private fun percentileLong(values: List<Long>, p: Double): Long {
        if (values.isEmpty()) return Long.MIN_VALUE
        val sorted = values.sorted()
        val clamped = p.coerceIn(0.0, 1.0)
        val index = ((sorted.size - 1) * clamped).toInt()
        return sorted[index]
    }
}
