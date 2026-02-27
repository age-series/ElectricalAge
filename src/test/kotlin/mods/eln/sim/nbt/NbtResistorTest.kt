package mods.eln.sim.nbt

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.NBTTagCompound

class NbtResistorTest {
    @Test
    fun nbtRoundTripPreservesResistance() {
        val a = VoltageState()
        val b = VoltageState()
        val resistor = NbtResistor("r", a, b)
        resistor.resistance = 12.0

        val nbt = NBTTagCompound()
        resistor.writeToNBT(nbt, "pfx")
        assertEquals(12.0, nbt.getDouble("pfxR"))

        val other = NbtResistor("r", a, b)
        other.readFromNBT(nbt, "pfx")
        assertEquals(12.0, other.resistance)
    }
}
