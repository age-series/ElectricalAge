package mods.eln.solver

import kotlin.test.Test
import kotlin.test.assertEquals

class EquationOperatorTest {
    @Test
    fun binaryOperatorsReturnExpectedValues() {
        val add = Equation.Add()
        add.setOperator(arrayOf(Constant(1.0), Constant(2.0)))
        assertEquals(3.0, add.getValue())

        val mul = Equation.Mul()
        mul.setOperator(arrayOf(Constant(2.0), Constant(3.0)))
        assertEquals(6.0, mul.getValue())

        val div = Equation.Div()
        div.setOperator(arrayOf(Constant(6.0), Constant(2.0)))
        assertEquals(3.0, div.getValue())

        val mod = Equation.Mod()
        mod.setOperator(arrayOf(Constant(7.0), Constant(3.0)))
        assertEquals(1.0, mod.getValue())

        val pow = Equation.Pow()
        pow.setOperator(arrayOf(Constant(2.0), Constant(3.0)))
        assertEquals(8.0, pow.getValue())
    }

    @Test
    fun unaryOperatorsReturnExpectedValues() {
        val inv = Equation.Inv()
        inv.setOperator(arrayOf(Constant(2.0)))
        assertEquals(-2.0, inv.getValue())

        val not = Equation.Not()
        not.setOperator(arrayOf(Constant(1.0)))
        assertEquals(0.0, not.getValue())

        val abs = Equation.Abs()
        abs.setOperator(arrayOf(Constant(-3.0)))
        assertEquals(3.0, abs.getValue())
    }

    @Test
    fun logicalAndComparisonOperators() {
        val eq = Equation.Eguals()
        eq.setOperator(arrayOf(Constant(1.0), Constant(1.0)))
        assertEquals(1.0, eq.getValue())

        val neq = Equation.NotEguals()
        neq.setOperator(arrayOf(Constant(1.0), Constant(0.0)))
        assertEquals(1.0, neq.getValue())

        val bigger = Equation.Bigger()
        bigger.setOperator(arrayOf(Constant(2.0), Constant(1.0)))
        assertEquals(1.0, bigger.getValue())

        val smaller = Equation.Smaller()
        smaller.setOperator(arrayOf(Constant(1.0), Constant(2.0)))
        assertEquals(1.0, smaller.getValue())

        val andOp = Equation.And()
        andOp.setOperator(arrayOf(Constant(1.0), Constant(1.0)))
        assertEquals(1.0, andOp.getValue())

        val orOp = Equation.Or()
        orOp.setOperator(arrayOf(Constant(0.0), Constant(1.0)))
        assertEquals(1.0, orOp.getValue())
    }

    @Test
    fun scaleAndIfOperators() {
        val scale = Equation.Scale()
        scale.setOperator(arrayOf(Constant(2.0), Constant(0.0), Constant(4.0), Constant(0.0), Constant(10.0)))
        assertEquals(5.0, scale.getValue())

        val conditional = Equation.If()
        conditional.setOperator(arrayOf(Constant(1.0), Constant(3.0), Constant(9.0)))
        assertEquals(3.0, conditional.getValue())

        val minOp = Equation.Min()
        minOp.setOperator(arrayOf(Constant(2.0), Constant(1.0)))
        assertEquals(1.0, minOp.getValue())

        val maxOp = Equation.Max()
        maxOp.setOperator(arrayOf(Constant(2.0), Constant(1.0)))
        assertEquals(2.0, maxOp.getValue())
    }

    @Test
    fun trigOperatorsReturnExpectedValues() {
        val sin = Equation.Sin()
        sin.setOperator(arrayOf(Constant(Math.PI / 2)))
        assertEquals(1.0, sin.getValue(), 1e-9)

        val cos = Equation.Cos()
        cos.setOperator(arrayOf(Constant(0.0)))
        assertEquals(1.0, cos.getValue(), 1e-9)

        val asin = Equation.Asin()
        asin.setOperator(arrayOf(Constant(1.0)))
        assertEquals(Math.PI / 2, asin.getValue(), 1e-9)

        val acos = Equation.Acos()
        acos.setOperator(arrayOf(Constant(1.0)))
        assertEquals(0.0, acos.getValue(), 1e-9)
    }
}
