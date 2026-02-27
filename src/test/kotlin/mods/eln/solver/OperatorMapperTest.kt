package mods.eln.solver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OperatorMapperTest {
    @Test
    fun mapperABBuildsBinaryOperator() {
        val list = mutableListOf<Any>(Constant(1.0), "+", Constant(2.0))
        val mapper = OperatorMapperAB("+", Equation.Add::class.java)

        val op = mapper.newOperator("+", 0, list, 1)
        assertNotNull(op)
        assertTrue(list[0] is IValue)
        assertEquals(3.0, (list[0] as IValue).getValue())
    }

    @Test
    fun mapperABRejectsMissingOperands() {
        val list = mutableListOf<Any>("+", Constant(2.0))
        val mapper = OperatorMapperAB("+", Equation.Add::class.java)
        assertNull(mapper.newOperator("+", 0, list, 0))
    }

    @Test
    fun mapperAHandlesUnaryPrefix() {
        val list = mutableListOf<Any>("-", Constant(3.0))
        val mapper = OperatorMapperA("-", Equation.Inv::class.java)

        val op = mapper.newOperator("-", 0, list, 0)
        assertNotNull(op)
        assertEquals(-3.0, (list[0] as IValue).getValue())
    }

    @Test
    fun mapperFuncBuildsOperatorFromArguments() {
        val list = mutableListOf<Any>("min", "(", Constant(2.0), ",", Constant(3.0), ")")
        val mapper = OperatorMapperFunc("min", 2, Equation.Min::class.java)

        val op = mapper.newOperator("min", -1, list, 0)
        assertNotNull(op)
        assertTrue(list[0] is IValue)
        assertEquals(2.0, (list[0] as IValue).getValue())
    }

    @Test
    fun mapperBracketCollapsesParenthesis() {
        val list = mutableListOf<Any>("(", Constant(4.0), ")")
        val mapper = OperatorMapperBracket()

        val op = mapper.newOperator("(", -1, list, 0)
        assertNotNull(op)
        assertEquals(4.0, (list[0] as IValue).getValue())
    }
}
