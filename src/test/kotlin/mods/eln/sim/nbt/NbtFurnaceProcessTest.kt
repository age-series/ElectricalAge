package mods.eln.sim.nbt

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.ThermalLoad
import net.minecraft.nbt.NBTTagCompound

class NbtFurnaceProcessTest {
    @Test
    fun nbtRoundTripPreservesValues() {
        val load = ThermalLoad()
        val process = NbtFurnaceProcess("f", load)
        process.combustibleEnergy = 15.0
        process.gain = 0.4

        val nbt = NBTTagCompound()
        process.writeToNBT(nbt, "pfx")

        val other = NbtFurnaceProcess("f", load)
        other.readFromNBT(nbt, "pfx")

        assertEquals(15.0, other.combustibleEnergy)
        assertEquals(0.4, other.gain)
    }

    @Test
    fun readFromNbtClampsGain() {
        val load = ThermalLoad()
        val nbt = NBTTagCompound()
        nbt.setFloat("pfxfQ", 5.0f)
        nbt.setDouble("pfxfgain", 2.0)

        val other = NbtFurnaceProcess("f", load)
        other.readFromNBT(nbt, "pfx")

        assertEquals(1.0, other.gain)
    }
}
