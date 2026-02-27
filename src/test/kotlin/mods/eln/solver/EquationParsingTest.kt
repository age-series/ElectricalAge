package mods.eln.solver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquationParsingTest {
    @Test
    fun parsesOperatorsAndRespectsPrecedence() {
        val eq = Equation()
        eq.setUpDefaultOperatorAndMapper()
        eq.iterationLimit = 200

        eq.preProcess("1+2*3")
        assertTrue(eq.isValid)
        assertEquals(7.0, eq.getValue())
    }

    @Test
    fun parsesSymbolsAndConstants() {
        val eq = Equation()
        eq.setUpDefaultOperatorAndMapper()
        eq.iterationLimit = 200
        eq.addSymbol(arrayListOf(ConstSymbole("A", 2.5)))

        eq.preProcess("A+pi")
        assertTrue(eq.isValid)
        assertTrue(eq.isSymboleUsed(ConstSymbole("A", 2.5)))
        assertEquals(2.5 + Math.PI, eq.getValue())
    }

    @Test
    fun invalidExpressionLeavesRootNull() {
        val eq = Equation()
        eq.setUpDefaultOperatorAndMapper()
        eq.iterationLimit = 1

        eq.preProcess("(")
        assertFalse(eq.isValid)
        assertEquals(0.0, eq.getValue())
    }

    @Test
    fun depthMaxCountsNestedParentheses() {
        val eq = Equation()
        val list = java.util.LinkedList<Any>()
        list.add("(")
        list.add("(")
        list.add(Constant(1.0))
        list.add(")")
        list.add(")")

        assertEquals(2, eq.getDepthMax(list))
    }
}
