package mods.eln.misc

open class FunctionTable(var point: DoubleArray, var xMax: Double) : IFunction {

    open var xMaxInv: Double = 1.0 / xMax
    open var xDelta: Double = 1.0 / (point.size - 1) * xMax

    override fun getValue(x: Double): Double {
        var lx = x
        lx *= xMaxInv
        if (lx < 0f) return point[0] + (point[1] - point[0]) * (point.size - 1) * lx
        if (lx >= 1.0f) return point[point.size - 1] + (point[point.size - 1] - point[point.size - 2]) * (point.size - 1) * (lx - 1.0)
        lx *= (point.size - 1).toDouble()
        val idx = lx.toInt()
        lx -= idx.toDouble()
        return point[idx + 1] * lx + point[idx] * (1.0f - lx)
    }

    open fun duplicate(xFactor: Double, yFactor: Double): FunctionTable? {
        val pointCpy = DoubleArray(point.size)
        for (idx in point.indices) {
            pointCpy[idx] = point[idx] * yFactor
        }
        return FunctionTable(pointCpy, xMax * xFactor)
    }
}
