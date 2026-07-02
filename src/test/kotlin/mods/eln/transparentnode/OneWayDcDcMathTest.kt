package mods.eln.transparentnode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class OneWayDcDcMathTest {
    @Test
    fun idealInputSourceCanDriveHundredOhmIsolatedLoad() {
        val transfer = OneWayDcDcMath.solve(
            inputTh = th(50.0, 0.0),
            outputTh = th(0.0, 100.0),
            ratio = 1.0,
            maxOutputVoltage = 120_000.0
        )

        assertNotNull(transfer)
        assertEquals(50.0, transfer.inputSourceVoltage, 1e-9)
        assertEquals(50.0, transfer.outputSourceVoltage, 1e-9)
        assertEquals(25.0, transfer.power, 1e-9)
    }

    @Test
    fun isolatedOutputUsesDifferentialTheveninVoltage() {
        val transfer = OneWayDcDcMath.solve(
            inputTh = th(50.0, 0.0),
            outputTh = th(-50.0, 100.0),
            ratio = 1.0,
            maxOutputVoltage = 120_000.0
        )

        assertNotNull(transfer)
        assertEquals(50.0, transfer.outputSourceVoltage, 1e-9)
        assertEquals(50.0, transfer.power, 1e-9)
    }

    @Test
    fun doesNotMovePowerWhenOutputIsAlreadyAtTarget() {
        val transfer = OneWayDcDcMath.solve(
            inputTh = th(50.0, 0.0),
            outputTh = th(50.0, 100.0),
            ratio = 1.0,
            maxOutputVoltage = 120_000.0
        )

        assertNull(transfer)
    }

    @Test
    fun windingCurrentDoesNotCapTransferredPower() {
        val transfer = OneWayDcDcMath.solve(
            inputTh = th(50.0, 0.0),
            outputTh = th(0.0, 100.0),
            ratio = 1.0,
            maxOutputVoltage = 120_000.0
        )

        assertNotNull(transfer)
        assertEquals(50.0, transfer.outputSourceVoltage, 1e-9)
        assertEquals(25.0, transfer.power, 1e-9)
    }

    @Test
    fun inputSourceResistanceCapsTransferredPower() {
        val transfer = OneWayDcDcMath.solve(
            inputTh = th(50.0, 100.0),
            outputTh = th(0.0, 100.0),
            ratio = 1.0,
            maxOutputVoltage = 120_000.0
        )

        assertNotNull(transfer)
        assertEquals(25.0, transfer.inputSourceVoltage, 1e-9)
        assertEquals(25.0, transfer.outputSourceVoltage, 1e-9)
        assertEquals(6.25, transfer.power, 1e-9)
    }

    private fun th(voltage: Double, resistance: Double): OneWayDcDcThevenin {
        return object : OneWayDcDcThevenin {
            override val voltage = voltage
            override val resistance = resistance
        }
    }
}
