package mods.eln.server

import mods.eln.Eln
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.WorldSavedData

class SaveConfig(par1Str: String) : WorldSavedData(par1Str) {
    @JvmField
    var heatFurnaceFuel = true

    var infiniteIncandescentLife = Eln.incandescentLampInfiniteLife
    var infiniteEcoBulbLife = Eln.ecoLampInfiniteLife
    var infiniteLedBulbLife = Eln.ledLampInfiniteLife
    var infiniteHalogenBulbLife = Eln.halogenLampInfiniteLife

    var batteryAging = true
    @JvmField
    var infinitePortableBattery = false
    var reGenOre = false
    var cableRsFactor_lastUsed = 1.0
    override fun readFromNBT(nbt: NBTTagCompound) {
        heatFurnaceFuel = nbt.getBoolean("heatFurnaceFuel")

        infiniteIncandescentLife = nbt.getBoolean("infiniteIncandescentBulbLife")
        infiniteEcoBulbLife = nbt.getBoolean("infiniteEcoBulbLife")
        infiniteLedBulbLife = nbt.getBoolean("infiniteLedBulbLife")
        infiniteHalogenBulbLife = nbt.getBoolean("infiniteHalogenBulbLife")

        batteryAging = nbt.getBoolean("batteryAging")
        infinitePortableBattery = nbt.getBoolean("infinitPortableBattery")
        reGenOre = nbt.getBoolean("reGenOre")
        cableRsFactor_lastUsed = nbt.getDouble("cableRsFactor_lastUsed")
        Eln.wind.readFromNBT(nbt, "wind")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        nbt.setBoolean("heatFurnaceFuel", heatFurnaceFuel)

        nbt.setBoolean("infiniteIncandescentBulbLife", infiniteIncandescentLife)
        nbt.setBoolean("infiniteEcoBulbLife", infiniteEcoBulbLife)
        nbt.setBoolean("infiniteLedBulbLife", infiniteLedBulbLife)
        nbt.setBoolean("infiniteHalogenBulbLife", infiniteHalogenBulbLife)

        nbt.setBoolean("batteryAging", batteryAging)
        nbt.setBoolean("infinitPortableBattery", infinitePortableBattery)
        nbt.setBoolean("reGenOre", reGenOre)
        Eln.wind.writeToNBT(nbt, "wind")
    }

    override fun isDirty(): Boolean {
        return true
    }

    companion object {
        @JvmField
        var instance: SaveConfig? = null
    }

    init {
        instance = this
    }
}
