package mods.eln.sim.mna

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.disableLog4jJmx
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

class SubSystemCopperPrecisionTest {
    @Test
    fun hundredByHundredCopperLadderMatchesHighPrecisionReference() {
        disableLog4jJmx()

        val sourceVoltage = 120.0
        val voltageNodeCount = 99
        val copperResistance = copperResistance(lengthMeters = 1.0, crossSectionSquareMeters = 10e-6)
        val shuntResistance = 1.0e9

        val subSystem = SubSystem(null, 0.1)
        val nodes = List(voltageNodeCount) { VoltageState() }
        nodes.forEach(subSystem::addState)

        subSystem.addComponent(
            VoltageSource("mains").apply {
                setVoltage(sourceVoltage)
                connectTo(nodes.first(), null)
            }
        )

        for (idx in 0 until voltageNodeCount - 1) {
            subSystem.addComponent(
                Resistor(nodes[idx], nodes[idx + 1]).apply {
                    resistance = copperResistance
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

        subSystem.step()

        val snapshot = subSystem.captureDebugSnapshot()
        assertEquals(100, snapshot.conductanceMatrix.size)

        val expected = solveCopperLadderReference(
            sourceVoltage = sourceVoltage,
            nodeCount = voltageNodeCount,
            seriesResistance = copperResistance,
            shuntResistance = shuntResistance
        )

        val errors = nodes.indices.map { idx ->
            val actual = nodes[idx].state
            assertTrue(actual.isFinite(), "node $idx produced a non-finite voltage")
            assertTrue(actual <= sourceVoltage + 1e-9, "node $idx exceeded the source voltage: $actual")
            assertTrue(actual >= -1e-9, "node $idx dropped below ground: $actual")
            abs(actual - expected[idx].toDouble())
        }

        val maxError = errors.maxOrNull() ?: 0.0
        assertTrue(maxError < 1e-9, "max error was $maxError V")
        assertTrue(nodes.zipWithNext().all { (a, b) -> a.state >= b.state }, "ladder voltages should be monotonic")
        assertTrue(sourceVoltage - nodes.last().state > 1e-7, "tail node should show a measurable copper drop")
    }

    private fun copperResistance(lengthMeters: Double, crossSectionSquareMeters: Double): Double {
        val copperResistivityOhmMeters = 1.68e-8
        return copperResistivityOhmMeters * lengthMeters / crossSectionSquareMeters
    }

    private fun solveCopperLadderReference(
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
}
