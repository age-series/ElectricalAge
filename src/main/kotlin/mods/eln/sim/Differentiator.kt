package mods.eln.sim

class Differentiator {
    private var stepsTaken = 0
    private var sample: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
    fun reset() {
        sample = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
    }

    fun nextStep(nextValue: Double, timeStep: Double): Double {
        var summation = 0.0

        if (stepsTaken >= 4) {
            summation += sample[1].also { sample[0] = it } * -2
            summation += sample[2].also { sample[1] = it } * 9
            summation += sample[3].also { sample[2] = it } * -18
            summation += nextValue.also { sample[3] = it } * 11
            summation /= (6 * timeStep)
        } else {
            sample[0] = sample[1]
            sample[1] = sample[2]
            summation += sample[3].also { sample[2] = it } * -1
            summation += nextValue.also { sample[3] = it }
            summation /= timeStep
        }

        stepsTaken++
        return summation
    }
}