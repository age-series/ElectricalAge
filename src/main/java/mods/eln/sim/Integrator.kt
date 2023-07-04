package mods.eln.sim

import java.util.function.Function
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

class Integrator {
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

    fun nextStep(nextValue: Double, timeStep: Double): Double {
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
    val testFunction = Function { xa: Double -> sin(2 * xa) }
    val actualIntegralFunction = Function { x: Double -> sin(x) * sin(x) }
    val actualDerivativeFunction = Function { x: Double -> 2 * cos(2 * x) }
    val dx = 1.0 / 20
    val integrator = Integrator()
    val differentiator = Differentiator()
    val tEnd = (1 / dx).roundToLong() + 1
    for (t in 1 until tEnd) {
        val x: Double = t * dx
        val y = testFunction.apply(x)
        val derivativeY: Double = differentiator.nextStep(y, dx)
        val integralY: Double = integrator.nextStep(y, dx)
        val derivativeActual: Double = actualDerivativeFunction.apply(x)
        val integralActual: Double = actualIntegralFunction.apply(x)
        println(
            "x = %.5f, y = %.5f, Y ~= %.5f, Y = %.5f, dy ~= %.5f, dy = %.5f, errInt = %.3e, errDiff = %.3e".format(
                x,
                y,
                integralY,
                integralActual,
                derivativeY,
                derivativeActual,
                integralY - integralActual,
                derivativeY - derivativeActual
            )
        )
    }
}
