package mods.eln.solver

import kotlin.math.E
import kotlin.test.Test
import kotlin.test.assertEquals

class EquationAdditionalFunctionsTest {
    private fun eval(expression: String): Double {
        val eq = Equation()
        eq.setUpDefaultOperatorAndMapper()
        eq.iterationLimit = 200
        eq.preProcess(expression)
        return eq.getValue()
    }

    @Test
    fun trigAndInverseFunctions() {
        assertEquals(1.0, eval("tan(0.7853981633974483)"), 1e-6)
        assertEquals(0.7853981633974483, eval("atan(1)"), 1e-9)
        assertEquals(0.7853981633974483, eval("atan2(1,1)"), 1e-9)
    }

    @Test
    fun logExpSqrtFunctions() {
        assertEquals(1.0, eval("log($E)"), 1e-9)
        assertEquals(2.0, eval("log10(100)"), 1e-9)
        assertEquals(E, eval("exp(1)"), 1e-9)
        assertEquals(3.0, eval("sqrt(9)"), 1e-9)
    }

    @Test
    fun clampLerpStepSmoothstep() {
        assertEquals(2.0, eval("clamp(3,0,2)"), 1e-9)
        assertEquals(5.0, eval("lerp(0,10,0.5)"), 1e-9)
        assertEquals(0.0, eval("step(1,0.5)"), 1e-9)
        assertEquals(1.0, eval("step(1,2)"), 1e-9)
        assertEquals(0.5, eval("smoothstep(0,1,0.5)"), 1e-9)
    }

    @Test
    fun roundingAndSignFunctions() {
        assertEquals(2.0, eval("round(1.6)"), 1e-9)
        assertEquals(1.0, eval("floor(1.9)"), 1e-9)
        assertEquals(2.0, eval("ceil(1.1)"), 1e-9)
        assertEquals(0.25, eval("fract(2.25)"), 1e-9)
        assertEquals(1.0, eval("sign(3)"), 1e-9)
        assertEquals(-1.0, eval("sign(-3)"), 1e-9)
        assertEquals(0.0, eval("sign(0)"), 1e-9)
    }
}
