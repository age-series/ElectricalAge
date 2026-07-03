package mods.eln.sixnode

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.disableLog4jJmx
import mods.eln.misc.VoltageLevelColor
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.CurrentSource
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.nbt.NbtElectricalLoad

private fun assertClose(expected: Double, actual: Double, eps: Double = 1e-3) {
    assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
}

class RegulatorChipsTest {
    @Test
    fun descriptorUsesOutputVoltageColor() {
        val descriptor = RegulatorChipDescriptor(
            "Test Regulator", null, "TEST", "test_regulator", RegulatorChipMode.LDO, 12.0, 0.25
        )

        assertEquals(VoltageLevelColor.fromVoltage(12.0), descriptor.voltageLevelColor)
    }

    @Test
    fun ldoDrivesNominalOutputWhenInputClearsDropout() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.LDO,
            inputVoltage = 4.5,
            outputVoltage = 3.3,
            currentLimit = 0.25,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(3.3, circuit.outputLoad.voltage)
        assertTrue(circuit.controller.outputCurrent > 0.0)
    }

    @Test
    fun ldoDropsOutputWhenInputDoesNotClearDropout() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.LDO,
            inputVoltage = 4.0,
            outputVoltage = 3.3,
            currentLimit = 0.25,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(2.8, circuit.outputLoad.voltage)
    }

    @Test
    fun ldoDrawsFiveMilliampsAtNominalMinimumInput() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.LDO,
            inputVoltage = 4.5,
            outputVoltage = 3.3,
            currentLimit = 0.25,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(0.005, circuit.quiescentLoad!!.current)
    }

    @Test
    fun ldoQuiescentLoadContinuesBelowBootVoltage() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.LDO,
            inputVoltage = 0.6,
            outputVoltage = 3.3,
            currentLimit = 0.25,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(0.0, circuit.outputLoad.voltage)
        assertTrue(circuit.quiescentLoad!!.current > 0.0)
    }

    @Test
    fun ldoIdlesAboveMaximumInputVoltage() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.LDO,
            inputVoltage = 6.7,
            outputVoltage = 3.3,
            currentLimit = 0.25,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(0.0, circuit.outputLoad.voltage)
        assertClose(0.0, circuit.inputSink.current)
        assertTrue(circuit.quiescentLoad!!.current > 0.0)
    }

    @Test
    fun boostDrivesOutputInsideInputRange() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.BOOST,
            inputVoltage = 3.2,
            outputVoltage = 5.0,
            currentLimit = 0.5,
            minimumInputVoltage = 2.0,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(5.0, circuit.outputLoad.voltage)
        assertTrue(circuit.inputSink.current < 0.0)
    }

    @Test
    fun boostDrawsFiveMilliampsAtNominalMinimumInput() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.BOOST,
            inputVoltage = 2.0,
            outputVoltage = 5.0,
            currentLimit = 0.5,
            minimumInputVoltage = 2.0,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(0.005, circuit.quiescentLoad!!.current)
    }

    @Test
    fun boostIdlesOutsideInputRange() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.BOOST,
            inputVoltage = 10.5,
            outputVoltage = 5.0,
            currentLimit = 0.5,
            minimumInputVoltage = 2.0,
            loadResistance = 1000.0
        )

        circuit.runSettled()

        assertClose(0.0, circuit.outputLoad.voltage)
        assertClose(0.0, circuit.inputSink.current)
        assertTrue(circuit.quiescentLoad!!.current > 0.0)
    }

    @Test
    fun outputCurrentLimitReducesOutputVoltage() {
        val circuit = regulatorCircuit(
            mode = RegulatorChipMode.BOOST,
            inputVoltage = 3.2,
            outputVoltage = 5.0,
            currentLimit = 0.5,
            minimumInputVoltage = 2.0,
            loadResistance = 1.0
        )

        circuit.runSettled()

        assertClose(0.5, circuit.outputLoad.voltage)
        assertClose(0.5, circuit.controller.outputCurrent)
    }

    private fun regulatorCircuit(
        mode: RegulatorChipMode,
        inputVoltage: Double,
        outputVoltage: Double,
        currentLimit: Double,
        minimumInputVoltage: Double = 0.0,
        maximumInputVoltage: Double? = null,
        loadResistance: Double
    ): RegulatorCircuit {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val inputLoad = NbtElectricalLoad("input")
        val outputLoad = NbtElectricalLoad("output")
        val inputSource = VoltageSource("inputSource", inputLoad, null).setVoltage(inputVoltage)
        val inputSink = CurrentSource("inputSink", inputLoad, null)
        val outputSource = VoltageSource("outputSource", outputLoad, null)
        val load = Resistor(outputLoad, null).setResistance(loadResistance)
        val descriptor = if (maximumInputVoltage == null) {
            RegulatorChipDescriptor(
                "Test Regulator", null, "TEST", "test_regulator", mode, outputVoltage, currentLimit,
                minimumInputVoltage = minimumInputVoltage
            )
        } else {
            RegulatorChipDescriptor(
                "Test Regulator", null, "TEST", "test_regulator", mode, outputVoltage, currentLimit,
                minimumInputVoltage = minimumInputVoltage,
                maximumInputVoltage = maximumInputVoltage
            )
        }
        val quiescentLoad = if (descriptor.quiescentInputResistance.isFinite()) {
            Resistor(inputLoad, null).setResistance(descriptor.quiescentInputResistance)
        } else {
            null
        }
        val controller = RegulatorChipController(descriptor, inputLoad, outputLoad, inputSink, outputSource)

        root.addState(inputLoad)
        root.addState(outputLoad)
        root.addComponent(inputSource)
        root.addComponent(inputSink)
        root.addComponent(outputSource)
        if (quiescentLoad != null) root.addComponent(quiescentLoad)
        root.addComponent(load)

        return RegulatorCircuit(root, inputLoad, outputLoad, inputSink, quiescentLoad, controller)
    }

    private data class RegulatorCircuit(
        val root: RootSystem,
        val inputLoad: NbtElectricalLoad,
        val outputLoad: NbtElectricalLoad,
        val inputSink: CurrentSource,
        val quiescentLoad: Resistor?,
        val controller: RegulatorChipController
    ) {
        fun runSettled() {
            repeat(8) {
                root.step()
                controller.process(0.05)
            }
            root.step()
            controller.process(0.05)
        }
    }
}
