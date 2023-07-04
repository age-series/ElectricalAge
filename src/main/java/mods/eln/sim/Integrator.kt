package mods.eln.sim

import java.util.function.Function
import kotlin.math.roundToLong
import kotlin.math.sin

class Integrator(private var timeStep: Double) {
    private var integrationPhase = 0
    private var stepsTaken = 0
    private var sample: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
    private var integrations: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0)

    init {
        reset()
    }

    fun reset() {
        stepsTaken = 0
        integrationPhase = 0
        sample = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
        integrations = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
    }

    fun nextStep(nextValue: Double): Double {
        var summation = 0.0
        stepsTaken++
        summation += sample[1].also { sample[0] = it } * 7
        summation += sample[2].also { sample[1] = it } * 32
        summation += sample[3].also { sample[2] = it } * 12
        summation += sample[4].also { sample[3] = it } * 32
        summation += nextValue.also { sample[4] = it } * 7
        summation *= 2 * timeStep / 45
        summation = summation.let { integrations[integrationPhase] += it; integrations[integrationPhase] }
        integrationPhase = integrationPhase + 1 and 3 // Effectively p = (p+1) mod 4
        return summation
    }
}

fun main() {
    val testFunction = Function { xa: Double -> sin(10 * xa) }
    val actualIntegrationFunction = Function { x: Double -> sin(5 * x) * sin(5 * x) / 5 }
    val dx = 1.0 / 20
    val integrator = Integrator(dx)
    val tEnd = (1 / dx).roundToLong() + 1
    for (t in 0 until tEnd) {
        val x: Double = t * dx
        val test: Double = integrator.nextStep(testFunction.apply(x))
        val actual: Double = actualIntegrationFunction.apply(x)
        val error: Double = test - actual
        System.out.printf(
            "x = %1f, Y ~= %2f, Y = %3f, E = %4e%n", x, test, actual, error
        )
    }
}
