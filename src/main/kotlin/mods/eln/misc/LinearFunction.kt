package mods.eln.misc

class LinearFunction(private val x0: Float, private val y0: Float, private val x1: Float, private val y1: Float) : IFunction {
    override fun getValue(x: Double): Double {
        return (x - x0) / (x1 - x0) * (y1 - y0) + y0
    }
}
