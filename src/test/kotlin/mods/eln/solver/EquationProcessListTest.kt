package mods.eln.solver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EquationProcessListTest {
    @Test
    fun processListOperatorsAdvanceWithDeltaT() {
        val eq = Equation()
        eq.setUpDefaultOperatorAndMapper()
        eq.iterationLimit = 200

        eq.preProcess("ramp(2)")
        assertTrue(eq.isValid)
        val first = eq.getValue(1.0)
        val second = eq.getValue(1.0)
        assertEquals(0.5, first)
        assertEquals(0.0, second)
    }
}
