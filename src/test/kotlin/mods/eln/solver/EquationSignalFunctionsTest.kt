package mods.eln.solver

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class MutableSymbol(private val name: String, var current: Double) : ISymbole {
    override fun getValue(): Double = current
    override fun getName(): String = name
}

class EquationSignalFunctionsTest {
    private fun eval(expression: String): Double {
        val eq = Equation()
        eq.setUpDefaultOperatorAndMapper()
        eq.iterationLimit = 200
        eq.preProcess(expression)
        return eq.getValue()
    }

    @Test
    fun saturateAndDeadzone() {
        assertEquals(1.0, eval("saturate(1.2)"), 1e-9)
        assertEquals(0.0, eval("deadzone(0.5,0.4,0.6)"), 1e-9)
        assertEquals(0.8, eval("deadzone(0.8,0.4,0.6)"), 1e-9)
    }

    @Test
    fun pulseSquareSawTriangleImpulse() {
        assertEquals(1.0, eval("pulse(0.2,0.6,0.5)"), 1e-9)
        assertEquals(0.0, eval("pulse(0.2,0.6,0.1)"), 1e-9)
        assertEquals(1.0, eval("square(0.25,0.5)"), 1e-9)
        assertEquals(0.0, eval("square(0.75,0.5)"), 1e-9)
        assertEquals(0.25, eval("saw(1.25)"), 1e-9)
        assertEquals(0.5, eval("triangle(0.25)"), 1e-9)
        val expected = 4.0 * 0.5 * exp(1.0 - 4.0 * 0.5)
        assertEquals(expected, eval("impulse(4,0.5)"), 1e-9)
    }

    @Test
    fun generatorsAdvanceWithTime() {
        val freq = MutableSymbol("f", 1.0)
        val duty = MutableSymbol("d", 0.5)
        val eqSquare = Equation()
        eqSquare.setUpDefaultOperatorAndMapper()
        eqSquare.iterationLimit = 200
        eqSquare.addSymbol(arrayListOf(freq, duty))
        eqSquare.preProcess("gensquare(f,d)")
        assertEquals(1.0, eqSquare.getValue(0.1), 1e-9)
        assertEquals(0.0, eqSquare.getValue(0.5), 1e-9)

        val eqSaw = Equation()
        eqSaw.setUpDefaultOperatorAndMapper()
        eqSaw.iterationLimit = 200
        eqSaw.addSymbol(arrayListOf(freq))
        eqSaw.preProcess("gensaw(f)")
        assertEquals(0.25, eqSaw.getValue(0.25), 1e-9)

        val eqTri = Equation()
        eqTri.setUpDefaultOperatorAndMapper()
        eqTri.iterationLimit = 200
        eqTri.addSymbol(arrayListOf(freq))
        eqTri.preProcess("gentriangle(f)")
        assertEquals(0.5, eqTri.getValue(0.25), 1e-9)
    }

    @Test
    fun lowpassAndHighpassUseState() {
        val symbol = MutableSymbol("x", 0.0)
        val eqLow = Equation()
        eqLow.setUpDefaultOperatorAndMapper()
        eqLow.iterationLimit = 200
        eqLow.addSymbol(arrayListOf(symbol))
        eqLow.preProcess("lowpass(x,0.5)")
        symbol.current = 1.0
        assertEquals(0.5, eqLow.getValue(1.0), 1e-9)
        assertEquals(0.75, eqLow.getValue(1.0), 1e-9)

        val eqHigh = Equation()
        eqHigh.setUpDefaultOperatorAndMapper()
        eqHigh.iterationLimit = 200
        eqHigh.addSymbol(arrayListOf(symbol))
        eqHigh.preProcess("highpass(x,0.5)")
        symbol.current = 1.0
        val first = eqHigh.getValue(1.0)
        val second = eqHigh.getValue(1.0)
        assertTrue(first > second)
    }

    @Test
    fun hysteresisAndDebounce() {
        val symbol = MutableSymbol("x", 0.2)
        val eqH = Equation()
        eqH.setUpDefaultOperatorAndMapper()
        eqH.iterationLimit = 200
        eqH.addSymbol(arrayListOf(symbol))
        eqH.preProcess("hysteresis(x,0.3,0.7)")
        assertEquals(0.0, eqH.getValue(), 1e-9)
        symbol.current = 0.8
        assertEquals(1.0, eqH.getValue(), 1e-9)
        symbol.current = 0.5
        assertEquals(1.0, eqH.getValue(), 1e-9)
        symbol.current = 0.1
        assertEquals(0.0, eqH.getValue(), 1e-9)

        val eqD = Equation()
        eqD.setUpDefaultOperatorAndMapper()
        eqD.iterationLimit = 200
        eqD.addSymbol(arrayListOf(symbol))
        eqD.preProcess("debounce(x,1.0)")
        symbol.current = 1.0
        assertEquals(0.0, eqD.getValue(0.5), 1e-9)
        assertEquals(1.0, eqD.getValue(0.6), 1e-9)
    }

    @Test
    fun beepPassesThroughAndSetsState() {
        val beep = Equation.Beep()
        beep.setOperator(arrayOf(Constant(0.6), Constant(1.0)))
        assertEquals(0.6, beep.getValue(), 1e-9)
        assertEquals(true, beep.active)
        assertEquals(1.0, beep.pitch, 1e-9)
        assertEquals(1.0, beep.volume, 1e-9)

        beep.setOperator(arrayOf(Constant(0.2), Constant(0.0)))
        assertEquals(0.2, beep.getValue(), 1e-9)
        assertEquals(false, beep.active)
    }
}
