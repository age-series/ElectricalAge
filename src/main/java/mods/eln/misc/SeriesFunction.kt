package mods.eln.misc

import kotlin.math.pow

class SeriesFunction(startExp: Double, eValue: DoubleArray): IFunction {
    var startExp = 1.0
    var eValue: DoubleArray

    fun seriesSize(): Int {
        return eValue.size
    }

    override fun getValue(x: Double): Double {
        var count = x
        val rot = count / seriesSize()
        count -= rot * seriesSize()
        val countInt = count.toInt().coerceIn(0, seriesSize())
        return 10.0.pow(startExp) * 10.0.pow(rot) * eValue[countInt]
    }

    companion object {
        @JvmStatic
        fun newE12(startExp: Double): SeriesFunction {
            return SeriesFunction(startExp, doubleArrayOf(1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2))
        }

        @JvmStatic
        fun newE6(startExp: Double): SeriesFunction {
            return SeriesFunction(startExp, doubleArrayOf(1.0, 1.5, 2.2, 3.3, 4.7, 6.8))
        }
    }

    init {
        this.startExp = startExp
        this.eValue = eValue
    }
}

