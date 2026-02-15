package mods.eln.solver

import net.minecraft.nbt.NBTTagCompound
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class MutableValue(var current: Double) : IValue {
    override fun getValue(): Double = current
}

class EquationProcessOperatorsTest {
    @Test
    fun rampWrapsAndPersists() {
        val ramp = Equation.Ramp()
        ramp.setOperator(arrayOf(Constant(2.0)))

        ramp.process(1.0)
        assertEquals(0.5, ramp.getValue())

        ramp.process(1.0)
        assertEquals(0.0, ramp.getValue())

        val nbt = NBTTagCompound()
        ramp.writeToNBT(nbt, "t")
        val other = Equation.Ramp()
        other.setOperator(arrayOf(Constant(2.0)))
        other.readFromNBT(nbt, "t")
        assertEquals(ramp.getValue(), other.getValue())
    }

    @Test
    fun integratorTracksProbeAndReset() {
        val probe = MutableValue(2.0)
        val reset = MutableValue(0.0)
        val integrator = Equation.Integrator()
        integrator.setOperator(arrayOf(probe, reset))

        integrator.process(0.5)
        assertEquals(1.0, integrator.getValue())

        reset.current = 1.0
        integrator.process(0.5)
        assertEquals(0.0, integrator.getValue())
    }

    @Test
    fun integratorMinMaxClampsWithinBounds() {
        val probe = MutableValue(2.0)
        val min = Constant(0.5)
        val max = Constant(1.0)
        val integrator = Equation.IntegratorMinMax()
        integrator.setOperator(arrayOf(probe, min, max))

        integrator.process(1.0)
        assertEquals(1.0, integrator.getValue())

        probe.current = -1.0
        integrator.process(1.0)
        assertEquals(0.5, integrator.getValue())

        val nbt = NBTTagCompound()
        integrator.writeToNBT(nbt, "i")
        val other = Equation.IntegratorMinMax()
        other.setOperator(arrayOf(probe, min, max))
        other.readFromNBT(nbt, "i")
        assertEquals(integrator.getValue(), other.getValue())
    }

    @Test
    fun derivatorUpdatesOldAndWritesNbt() {
        val probe = MutableValue(1.0)
        val derivator = Equation.Derivator()
        derivator.setOperator(arrayOf(probe))

        derivator.process(1.0)
        probe.current = 3.0
        derivator.process(2.0)
        assertEquals(1.0, derivator.getValue())

        val nbt = NBTTagCompound()
        derivator.writeToNBT(nbt, "d")
        val other = Equation.Derivator()
        other.setOperator(arrayOf(probe))
        other.readFromNBT(nbt, "d")
        assertEquals(derivator.getValue(), other.getValue())
    }

    @Test
    fun rcFiltersTowardInput() {
        val input = MutableValue(1.0)
        val tao = Constant(2.0)
        val rc = Equation.RC()
        rc.setOperator(arrayOf(tao, input))

        rc.process(1.0)
        assertTrue(rc.getValue() > 0.0)
        assertTrue(rc.getValue() < 1.0)

        val nbt = NBTTagCompound()
        rc.writeToNBT(nbt, "rc")
        val other = Equation.RC()
        other.setOperator(arrayOf(tao, input))
        other.readFromNBT(nbt, "rc")
        assertEquals(rc.getValue(), other.getValue())
    }

    @Test
    fun rsLatchSetsAndResets() {
        val set = MutableValue(1.0)
        val reset = MutableValue(0.0)
        val rs = Equation.Rs()
        rs.setOperator(arrayOf(reset, set))
        assertEquals(1.0, rs.getValue())

        set.current = 0.0
        reset.current = 1.0
        assertEquals(0.0, rs.getValue())

        val nbt = NBTTagCompound()
        rs.writeToNBT(nbt, "rs")
        val other = Equation.Rs()
        other.setOperator(arrayOf(reset, set))
        other.readFromNBT(nbt, "rs")
        assertEquals(rs.getValue(), other.getValue())
    }

    @Test
    fun pidClampsIntegralAndCombinesTerms() {
        val target = MutableValue(2.0)
        val hit = MutableValue(0.0)
        val p = Constant(1.0)
        val i = Constant(1.0)
        val d = Constant(1.0)
        val pid = Equation.Pid()
        pid.setOperator(arrayOf(target, hit, p, i, d))

        pid.process(2.0)
        val value = pid.getValue()
        assertTrue(value >= 0.0)
        assertTrue(pid.iStack <= 1.0)

        val nbt = NBTTagCompound()
        pid.writeToNBT(nbt, "pid")
        val other = Equation.Pid()
        other.setOperator(arrayOf(target, hit, p, i, d))
        other.readFromNBT(nbt, "pid")
        assertEquals(pid.getValue(), other.getValue())
    }

    @Test
    fun pidMinMaxClampsOutput() {
        val target = MutableValue(2.0)
        val hit = MutableValue(0.0)
        val p = Constant(10.0)
        val i = Constant(0.0)
        val d = Constant(0.0)
        val min = Constant(0.0)
        val max = Constant(1.0)
        val pid = Equation.PidMinMax()
        pid.setOperator(arrayOf(target, hit, p, i, d, min, max))

        pid.process(1.0)
        assertEquals(1.0, pid.getValue())
    }
}
