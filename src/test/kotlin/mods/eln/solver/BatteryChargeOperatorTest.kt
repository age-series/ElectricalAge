package mods.eln.solver

import mods.eln.misc.FunctionTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatteryChargeOperatorTest {
    @Test
    fun batteryChargeUsesFunctionTable() {
        val table = FunctionTable(doubleArrayOf(0.0, 1.0), 1.0)
        ensureBatteryVoltageTable(table)

        val op = Equation.BatteryCharge()
        op.setOperator(arrayOf(Constant(0.5)))

        val value = op.getValue()
        assertTrue(value > 0.0)
        assertTrue(value < 1.0)
        assertEquals(0.25, value, 1e-2)
    }

    @Test
    fun batteryChargeCapsAtOne() {
        val table = FunctionTable(doubleArrayOf(0.0, 1.0), 1.0)
        ensureBatteryVoltageTable(table)

        val op = Equation.BatteryCharge()
        op.setOperator(arrayOf(Constant(2.0)))
        assertEquals(1.0, op.getValue())
    }
}
