package mods.eln.sim.nbt

import kotlin.test.Test
import kotlin.test.assertEquals
import net.minecraft.nbt.NBTTagCompound

class NbtThermalLoadTest {
    @Test
    fun readWritePreservesTemperature() {
        val load = NbtThermalLoad("t")
        load.temperatureCelsius = 42.0

        val nbt = NBTTagCompound()
        load.writeToNBT(nbt, "pfx")

        val other = NbtThermalLoad("t")
        other.readFromNBT(nbt, "pfx")
        assertEquals(42.0, other.temperatureCelsius)
    }

    @Test
    fun readFromNbtHandlesInvalidValues() {
        val nbt = NBTTagCompound()
        nbt.setFloat("pfxtTc", Float.NaN)
        val load = NbtThermalLoad("t")
        load.readFromNBT(nbt, "pfx")
        assertEquals(0.0, load.temperatureCelsius)

        nbt.setFloat("pfxtTc", Float.NEGATIVE_INFINITY)
        load.readFromNBT(nbt, "pfx")
        assertEquals(0.0, load.temperatureCelsius)

        nbt.setFloat("pfxtTc", Float.POSITIVE_INFINITY)
        load.readFromNBT(nbt, "pfx")
        assertEquals(0.0, load.temperatureCelsius)
    }
}
