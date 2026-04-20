package mods.eln.sim.mna

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertTrue
import mods.eln.disableLog4jJmx
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.QRDecomposition
import org.apache.commons.numbers.core.DD

class SubSystemDdBenchmarkTest {
    @Test
    fun compareQrAndDdVoltageError() {
        disableLog4jJmx()

        val sizes = propertyOrEnv("eln.voltage.compare.sizes", "ELN_VOLTAGE_COMPARE_SIZES")
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it > 1 }
            ?: listOf(128, 256, 512, 1024)
        val seriesResistance = propertyOrEnv("eln.voltage.compare.series", "ELN_VOLTAGE_COMPARE_SERIES")?.toDoubleOrNull() ?: 1.68e-7
        val shuntResistance = propertyOrEnv("eln.voltage.compare.shunt", "ELN_VOLTAGE_COMPARE_SHUNT")?.toDoubleOrNull() ?: 1.0e9
        val voltages = propertyOrEnv("eln.voltage.compare.voltages", "ELN_VOLTAGE_COMPARE_VOLTAGES")
            ?.split(',')
            ?.mapNotNull { it.trim().toDoubleOrNull() }
            ?.filter { it > 0.0 }
            ?: listOf(1.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 200000.0, 500000.0, 1000000.0)

        println("Voltage error compare sizes=$sizes series=$seriesResistance voltages=$voltages shunt=$shuntResistance")

        for (matrixSize in sizes) {
            val benchmarkInput = createBenchmarkInput(matrixSize, seriesResistance, shuntResistance)
            benchmarkInput.subSystem.generateMatrix()
            val snapshot = benchmarkInput.subSystem.captureDebugSnapshot()
            val matrix = snapshot.conductanceMatrix
            val stateLabels = snapshot.stateLabels
            val rhsIndex = stateLabels.indexOfLast { it.contains("CurrentState") }.takeIf { it >= 0 } ?: (matrix.size - 1)
            val qrInverse = QRDecomposition(MatrixUtils.createRealMatrix(copyMatrix(matrix))).solver.inverse.data

            for (sourceVoltage in voltages) {
                benchmarkInput.source.setVoltage(sourceVoltage)
                benchmarkInput.subSystem.step()
                val expected = solveLadderReference(
                    sourceVoltage = sourceVoltage,
                    nodeCount = matrixSize - 1,
                    seriesResistance = seriesResistance,
                    shuntResistance = shuntResistance
                )

                val ddActual = benchmarkInput.nodes.map { it.state }
                val ddMaxError = ddActual.indices.maxOf { idx -> abs(ddActual[idx] - expected[idx].toDouble()) }

                val rhs = DoubleArray(matrix.size)
                rhs[rhsIndex] = sourceVoltage
                val qrActual = DoubleArray(matrix.size)
                multiply(qrInverse, rhs, qrActual)
                val qrMaxError = benchmarkInput.nodes.indices.maxOf { idx -> abs(qrActual[idx] - expected[idx].toDouble()) }

                println(
                    "Voltage error compare size=$matrixSize voltage=$sourceVoltage " +
                        "qrMaxError=${"%.3e".format(qrMaxError)} ddMaxError=${"%.3e".format(ddMaxError)}"
                )

                assertTrue(qrMaxError.isFinite(), "QR max error was not finite")
                assertTrue(ddMaxError.isFinite(), "DD max error was not finite")
            }
        }
    }

    @Test
    fun compareDoubleAndDdPerformance() {
        disableLog4jJmx()

        val matrixSizes = propertyOrEnv("eln.compare.sizes", "ELN_COMPARE_SIZES")
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it > 1 }
            ?: listOf(propertyOrEnv("eln.compare.size", "ELN_COMPARE_SIZE")?.toIntOrNull() ?: 1024)
        val seriesResistance = propertyOrEnv("eln.compare.series", "ELN_COMPARE_SERIES")?.toDoubleOrNull() ?: 1.68e-7
        val shuntResistance = propertyOrEnv("eln.compare.shunt", "ELN_COMPARE_SHUNT")?.toDoubleOrNull() ?: 1.0e9

        for (matrixSize in matrixSizes) {
            compareOneSize(matrixSize, seriesResistance, shuntResistance)
        }
    }

    private fun compareOneSize(matrixSize: Int, seriesResistance: Double, shuntResistance: Double) {
        val benchmarkInput = createBenchmarkInput(matrixSize, seriesResistance, shuntResistance)
        benchmarkInput.subSystem.generateMatrix()
        val matrix = benchmarkInput.subSystem.captureDebugSnapshot().conductanceMatrix
        val rhs = buildDeterministicRhs(matrix.size)

        forceGc()
        val qrInverseHolder = arrayOfNulls<DoubleArray>(matrix.size)
        val qrBuildNs = measureNanoTime {
            val inverse = QRDecomposition(MatrixUtils.createRealMatrix(copyMatrix(matrix))).solver.inverse.data
            for (idx in inverse.indices) {
                qrInverseHolder[idx] = inverse[idx]
            }
        }
        val qrInverse = Array(matrix.size) { idx -> qrInverseHolder[idx]!! }
        val qrSolveHolder = DoubleArray(matrix.size)
        val qrSolveNs = measureNanoTime {
            multiply(qrInverse, rhs, qrSolveHolder)
        }

        forceGc()
        val gaussDoubleInverseHolder = arrayOfNulls<DoubleArray>(matrix.size)
        val gaussDoubleBuildNs = measureNanoTime {
            val inverse = invertMatrixDouble(copyMatrix(matrix))
            for (idx in inverse.indices) {
                gaussDoubleInverseHolder[idx] = inverse[idx]
            }
        }
        val gaussDoubleInverse = Array(matrix.size) { idx -> gaussDoubleInverseHolder[idx]!! }
        val gaussDoubleSolveHolder = DoubleArray(matrix.size)
        val gaussDoubleSolveNs = measureNanoTime {
            multiply(gaussDoubleInverse, rhs, gaussDoubleSolveHolder)
        }

        forceGc()
        val ddInverseHolder = arrayOfNulls<Array<DD>>(matrix.size)
        val ddBuildNs = measureNanoTime {
            val inverse = invertMatrixDd(copyMatrix(matrix))
            for (idx in inverse.indices) {
                ddInverseHolder[idx] = inverse[idx]
            }
        }
        val ddInverse = Array(matrix.size) { idx -> ddInverseHolder[idx]!! }
        val ddSolveHolder = DoubleArray(matrix.size)
        val ddSolveNs = measureNanoTime {
            multiply(ddInverse, rhs, ddSolveHolder)
        }

        val qrVsDdMaxDelta = qrSolveHolder.indices.maxOf { idx -> abs(qrSolveHolder[idx] - ddSolveHolder[idx]) }
        val qrVsDdTailDelta = abs(qrSolveHolder.last() - ddSolveHolder.last())
        val gaussVsDdMaxDelta = gaussDoubleSolveHolder.indices.maxOf { idx -> abs(gaussDoubleSolveHolder[idx] - ddSolveHolder[idx]) }
        val gaussVsDdTailDelta = abs(gaussDoubleSolveHolder.last() - ddSolveHolder.last())

        println(
            "DD compare size=$matrixSize series=$seriesResistance shunt=$shuntResistance " +
                "qrBuildMs=${"%.1f".format(qrBuildNs / 1_000_000.0)} " +
                "gaussDoubleBuildMs=${"%.1f".format(gaussDoubleBuildNs / 1_000_000.0)} " +
                "ddBuildMs=${"%.1f".format(ddBuildNs / 1_000_000.0)} " +
                "qrSolveMs=${"%.1f".format(qrSolveNs / 1_000_000.0)} " +
                "gaussDoubleSolveMs=${"%.1f".format(gaussDoubleSolveNs / 1_000_000.0)} " +
                "ddSolveMs=${"%.1f".format(ddSolveNs / 1_000_000.0)} " +
                "ddVsQrBuildSlowdown=${"%.2f".format(ddBuildNs.toDouble() / qrBuildNs.toDouble())}x " +
                "ddVsQrSolveSlowdown=${"%.2f".format(ddSolveNs.toDouble() / qrSolveNs.toDouble())}x " +
                "ddVsGaussDoubleBuildSlowdown=${"%.2f".format(ddBuildNs.toDouble() / gaussDoubleBuildNs.toDouble())}x " +
                "ddVsGaussDoubleSolveSlowdown=${"%.2f".format(ddSolveNs.toDouble() / gaussDoubleSolveNs.toDouble())}x " +
                "qrVsDdMaxDelta=${"%.3e".format(qrVsDdMaxDelta)} qrVsDdTailDelta=${"%.3e".format(qrVsDdTailDelta)} " +
                "gaussVsDdMaxDelta=${"%.3e".format(gaussVsDdMaxDelta)} gaussVsDdTailDelta=${"%.3e".format(gaussVsDdTailDelta)}"
        )

        assertTrue(qrVsDdMaxDelta.isFinite(), "comparison produced a non-finite QR/DD delta")
        assertTrue(gaussVsDdMaxDelta.isFinite(), "comparison produced a non-finite double/DD delta")
    }

    @Test
    fun benchmarkConfiguredMatrixSizesAndResistances() {
        disableLog4jJmx()

        val sizes = propertyOrEnv("eln.benchmark.sizes", "ELN_BENCHMARK_SIZES")
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it > 1 }
            ?: listOf(128, 192, 256, 384, 512, 768, 1024)
        val seriesResistances = propertyOrEnv("eln.benchmark.series", "ELN_BENCHMARK_SERIES")
            ?.split(',')
            ?.mapNotNull { it.trim().toDoubleOrNull() }
            ?.filter { it > 0.0 }
            ?: listOf(1.68e-3, 1.68e-6, 1.68e-9)
        val sourceVoltages = propertyOrEnv("eln.benchmark.voltages", "ELN_BENCHMARK_VOLTAGES")
            ?.split(',')
            ?.mapNotNull { it.trim().toDoubleOrNull() }
            ?.filter { it > 0.0 }
            ?: listOf(120.0)
        val shuntResistance = propertyOrEnv("eln.benchmark.shunt", "ELN_BENCHMARK_SHUNT")?.toDoubleOrNull() ?: 1.0e9
        val failureThreshold = propertyOrEnv("eln.benchmark.maxError", "ELN_BENCHMARK_MAX_ERROR")?.toDoubleOrNull() ?: 1e-7

        println("DD benchmark sizes=$sizes series=$seriesResistances voltages=$sourceVoltages shunt=$shuntResistance")

        val results = ArrayList<BenchmarkResult>()
        for (size in sizes) {
            for (seriesResistance in seriesResistances) {
                val benchmarkInput = createBenchmarkInput(size, seriesResistance, shuntResistance)
                forceGc()
                val beforeBytes = usedMemoryBytes()
                val buildNs = measureNanoTime {
                    benchmarkInput.subSystem.generateMatrix()
                }
                forceGc()
                val afterBytes = usedMemoryBytes()

                for (sourceVoltage in sourceVoltages) {
                    val result = runVoltageCase(
                        benchmarkInput = benchmarkInput,
                        sourceVoltage = sourceVoltage,
                        buildNs = buildNs,
                        usedMemMiB = ((afterBytes - beforeBytes).coerceAtLeast(0L) / (1024 * 1024))
                    )
                    results.add(result)
                    println(
                        "DD benchmark size=${result.matrixSize} series=${result.seriesResistance} voltage=${result.sourceVoltage} " +
                            "condRatio=${"%.3e".format(result.conditionRatio)} buildMs=${"%.1f".format(result.buildMs)} " +
                            "stepMs=${"%.1f".format(result.stepMs)} maxError=${"%.3e".format(result.maxError)} " +
                            "tailError=${"%.3e".format(result.tailError)} usedMemMiB=${result.usedMemMiB}"
                    )
                    assertTrue(result.maxError.isFinite(), "max error was not finite for $result")
                    assertTrue(result.maxError <= failureThreshold, "stability threshold exceeded for $result")
                }
            }
        }

        assertTrue(results.isNotEmpty(), "benchmark did not run any cases")
    }

    private fun propertyOrEnv(property: String, env: String): String? {
        return System.getProperty(property) ?: System.getenv(env)
    }

    private fun createBenchmarkInput(matrixSize: Int, seriesResistance: Double, shuntResistance: Double): BenchmarkInput {
        val voltageNodeCount = matrixSize - 1

        val subSystem = SubSystem(null, 0.1)
        val nodes = List(voltageNodeCount) { VoltageState() }
        nodes.forEach(subSystem::addState)
        val source = VoltageSource("benchmark").apply {
            setVoltage(120.0)
            connectTo(nodes.first(), null)
        }
        subSystem.addComponent(source)
        for (idx in 0 until voltageNodeCount - 1) {
            subSystem.addComponent(
                Resistor(nodes[idx], nodes[idx + 1]).apply {
                    resistance = seriesResistance
                }
            )
        }
        for (idx in 1 until voltageNodeCount) {
            subSystem.addComponent(
                Resistor(nodes[idx], null).apply {
                    resistance = shuntResistance
                }
            )
        }

        return BenchmarkInput(matrixSize, subSystem, source, nodes, seriesResistance, shuntResistance)
    }

    private fun runVoltageCase(
        benchmarkInput: BenchmarkInput,
        sourceVoltage: Double,
        buildNs: Long,
        usedMemMiB: Long
    ): BenchmarkResult {
        val subSystem = benchmarkInput.subSystem
        val nodes = benchmarkInput.nodes
        benchmarkInput.source.setVoltage(sourceVoltage)

        val expected = solveLadderReference(
            sourceVoltage = sourceVoltage,
            nodeCount = benchmarkInput.matrixSize - 1,
            seriesResistance = benchmarkInput.seriesResistance,
            shuntResistance = benchmarkInput.shuntResistance
        )

        val stepNs = measureNanoTime {
            subSystem.step()
        }

        val actual = nodes.map { it.state }
        val maxError = actual.indices.maxOf { idx -> abs(actual[idx] - expected[idx].toDouble()) }
        val tailError = abs(actual.last() - expected.last().toDouble())

        return BenchmarkResult(
            matrixSize = benchmarkInput.matrixSize,
            seriesResistance = benchmarkInput.seriesResistance,
            sourceVoltage = sourceVoltage,
            conditionRatio = benchmarkInput.shuntResistance / benchmarkInput.seriesResistance,
            buildMs = buildNs / 1_000_000.0,
            stepMs = stepNs / 1_000_000.0,
            maxError = maxError,
            tailError = tailError,
            usedMemMiB = usedMemMiB
        )
    }

    private fun solveLadderReference(
        sourceVoltage: Double,
        nodeCount: Int,
        seriesResistance: Double,
        shuntResistance: Double
    ): List<BigDecimal> {
        val mc = MathContext(50, RoundingMode.HALF_EVEN)
        val gSeries = BigDecimal.ONE.divide(BigDecimal.valueOf(seriesResistance), mc)
        val gShunt = BigDecimal.ONE.divide(BigDecimal.valueOf(shuntResistance), mc)
        val source = BigDecimal.valueOf(sourceVoltage)

        if (nodeCount == 1) {
            return listOf(source)
        }

        val unknownCount = nodeCount - 1
        val lower = Array(unknownCount) { BigDecimal.ZERO }
        val diagonal = Array(unknownCount) { BigDecimal.ZERO }
        val upper = Array(unknownCount) { BigDecimal.ZERO }
        val rhs = Array(unknownCount) { BigDecimal.ZERO }
        val twiceSeries = gSeries.add(gSeries, mc)

        diagonal[0] = twiceSeries.add(gShunt, mc)
        upper[0] = gSeries.negate()
        rhs[0] = gSeries.multiply(source, mc)

        for (idx in 1 until unknownCount - 1) {
            lower[idx] = gSeries.negate()
            diagonal[idx] = twiceSeries.add(gShunt, mc)
            upper[idx] = gSeries.negate()
        }

        lower[unknownCount - 1] = gSeries.negate()
        diagonal[unknownCount - 1] = gSeries.add(gShunt, mc)

        for (idx in 1 until unknownCount) {
            val factor = lower[idx].divide(diagonal[idx - 1], mc)
            diagonal[idx] = diagonal[idx].subtract(factor.multiply(upper[idx - 1], mc), mc)
            rhs[idx] = rhs[idx].subtract(factor.multiply(rhs[idx - 1], mc), mc)
        }

        val solution = Array(unknownCount) { BigDecimal.ZERO }
        solution[unknownCount - 1] = rhs[unknownCount - 1].divide(diagonal[unknownCount - 1], mc)
        for (idx in unknownCount - 2 downTo 0) {
            solution[idx] = rhs[idx]
                .subtract(upper[idx].multiply(solution[idx + 1], mc), mc)
                .divide(diagonal[idx], mc)
        }

        val values = ArrayList<BigDecimal>(nodeCount)
        values.add(source)
        values.addAll(solution)
        return values
    }

    private fun usedMemoryBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun copyMatrix(matrix: Array<DoubleArray>): Array<DoubleArray> {
        return Array(matrix.size) { idx -> matrix[idx].clone() }
    }

    private fun buildDeterministicRhs(size: Int): DoubleArray {
        return DoubleArray(size) { idx ->
            when (idx % 4) {
                0 -> 120.0
                1 -> -3.0
                2 -> 0.25
                else -> 1.0e-3
            }
        }
    }

    private fun multiply(inverse: Array<DoubleArray>, rhs: DoubleArray, out: DoubleArray) {
        for (row in inverse.indices) {
            var acc = 0.0
            val inverseRow = inverse[row]
            for (col in rhs.indices) {
                acc += inverseRow[col] * rhs[col]
            }
            out[row] = acc
        }
    }

    private fun multiply(inverse: Array<Array<DD>>, rhs: DoubleArray, out: DoubleArray) {
        for (row in inverse.indices) {
            var acc = DD.ZERO
            val inverseRow = inverse[row]
            for (col in rhs.indices) {
                acc = acc.add(inverseRow[col].multiply(rhs[col]))
            }
            out[row] = acc.toDouble()
        }
    }

    private fun invertMatrixDd(matrix: Array<DoubleArray>): Array<Array<DD>> {
        val size = matrix.size
        val augmented = Array(size) { Array(size * 2) { DD.ZERO } }

        for (row in 0 until size) {
            for (col in 0 until size) {
                augmented[row][col] = DD.of(matrix[row][col])
                augmented[row][size + col] = if (row == col) DD.ONE else DD.ZERO
            }
        }

        for (pivotColumn in 0 until size) {
            var pivotRow = pivotColumn
            var pivotMagnitude = 0.0
            for (row in pivotColumn until size) {
                val magnitude = augmented[row][pivotColumn].abs().toDouble()
                if (magnitude > pivotMagnitude) {
                    pivotMagnitude = magnitude
                    pivotRow = row
                }
            }

            if (pivotMagnitude == 0.0) {
                throw IllegalStateException("Matrix is singular")
            }

            if (pivotRow != pivotColumn) {
                val swap = augmented[pivotColumn]
                augmented[pivotColumn] = augmented[pivotRow]
                augmented[pivotRow] = swap
            }

            val pivot = augmented[pivotColumn][pivotColumn]
            for (col in pivotColumn until size * 2) {
                augmented[pivotColumn][col] = augmented[pivotColumn][col].divide(pivot)
            }
            augmented[pivotColumn][pivotColumn] = DD.ONE

            for (row in 0 until size) {
                if (row == pivotColumn) {
                    continue
                }

                val factor = augmented[row][pivotColumn]
                if (factor.isZero) {
                    continue
                }

                for (col in pivotColumn until size * 2) {
                    augmented[row][col] = augmented[row][col].subtract(factor.multiply(augmented[pivotColumn][col]))
                }
                augmented[row][pivotColumn] = DD.ZERO
            }
        }

        return Array(size) { row ->
            Array(size) { col -> augmented[row][size + col] }
        }
    }

    private fun invertMatrixDouble(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val size = matrix.size
        val augmented = Array(size) { DoubleArray(size * 2) }

        for (row in 0 until size) {
            for (col in 0 until size) {
                augmented[row][col] = matrix[row][col]
                augmented[row][size + col] = if (row == col) 1.0 else 0.0
            }
        }

        for (pivotColumn in 0 until size) {
            var pivotRow = pivotColumn
            var pivotMagnitude = 0.0
            for (row in pivotColumn until size) {
                val magnitude = abs(augmented[row][pivotColumn])
                if (magnitude > pivotMagnitude) {
                    pivotMagnitude = magnitude
                    pivotRow = row
                }
            }

            if (pivotMagnitude == 0.0) {
                throw IllegalStateException("Matrix is singular")
            }

            if (pivotRow != pivotColumn) {
                val swap = augmented[pivotColumn]
                augmented[pivotColumn] = augmented[pivotRow]
                augmented[pivotRow] = swap
            }

            val pivot = augmented[pivotColumn][pivotColumn]
            for (col in pivotColumn until size * 2) {
                augmented[pivotColumn][col] /= pivot
            }
            augmented[pivotColumn][pivotColumn] = 1.0

            for (row in 0 until size) {
                if (row == pivotColumn) {
                    continue
                }

                val factor = augmented[row][pivotColumn]
                if (factor == 0.0) {
                    continue
                }

                for (col in pivotColumn until size * 2) {
                    augmented[row][col] -= factor * augmented[pivotColumn][col]
                }
                augmented[row][pivotColumn] = 0.0
            }
        }

        return Array(size) { row ->
            DoubleArray(size) { col -> augmented[row][size + col] }
        }
    }

    private fun forceGc() {
        repeat(3) {
            System.gc()
            Thread.sleep(50)
        }
    }

    private data class BenchmarkInput(
        val matrixSize: Int,
        val subSystem: SubSystem,
        val source: VoltageSource,
        val nodes: List<VoltageState>,
        val seriesResistance: Double,
        val shuntResistance: Double
    )

    private data class BenchmarkResult(
        val matrixSize: Int,
        val seriesResistance: Double,
        val sourceVoltage: Double,
        val conditionRatio: Double,
        val buildMs: Double,
        val stepMs: Double,
        val maxError: Double,
        val tailError: Double,
        val usedMemMiB: Long
    )
}
