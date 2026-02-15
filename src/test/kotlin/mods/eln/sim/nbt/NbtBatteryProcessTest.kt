package mods.eln.sim.nbt

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.misc.FunctionTable
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import net.minecraft.nbt.NBTTagCompound

class NbtBatteryProcessTest {
    @Test
    fun nbtRoundTripPreservesChargeAndLife() {
        val table = FunctionTable(doubleArrayOf(1.0, 1.0), 1.0)
        val process = NbtBatteryProcess(null, null, table, 10.0, VoltageSource("b"), ThermalLoad())
        process.Q = 2.5
        process.life = 0.75

        val nbt = NBTTagCompound()
        process.writeToNBT(nbt, "pfx")

        val other = NbtBatteryProcess(null, null, table, 10.0, VoltageSource("b2"), ThermalLoad())
        other.readFromNBT(nbt, "pfx")

        assertEquals(2.5, other.Q)
        assertEquals(0.75, other.life)
    }

    @Test
    fun readFromNbtHandlesInvalidValues() {
        val table = FunctionTable(doubleArrayOf(1.0, 1.0), 1.0)
        val nbt = NBTTagCompound()
        nbt.setDouble("pfxNBPQ", Double.NaN)
        nbt.setDouble("pfxNBPlife", Double.POSITIVE_INFINITY)

        val other = NbtBatteryProcess(null, null, table, 10.0, VoltageSource("b"), ThermalLoad())
        other.readFromNBT(nbt, "pfx")

        assertEquals(0.0, other.Q)
        assertEquals(1.0, other.life)
    }
}
