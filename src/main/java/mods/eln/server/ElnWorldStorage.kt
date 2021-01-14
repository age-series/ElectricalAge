package mods.eln.server

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraft.world.WorldSavedData

class ElnWorldStorage(str: String?) : WorldSavedData(str) {
    private var dim = 0
    override fun readFromNBT(nbt: NBTTagCompound) {
        dim = nbt.getInteger("dim")
        ServerEventListener.readFromEaWorldNBT(nbt)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        nbt.setInteger("dim", dim)
        ServerEventListener.writeToEaWorldNBT(nbt, dim)
    }

    override fun isDirty(): Boolean {
        return true
    }

    companion object {
        const val key = "eln.worldStorage"
        @JvmStatic
        fun forWorld(world: World): ElnWorldStorage {
            // Retrieves the MyWorldData instance for the given world, creating it if necessary
            val storage = world.perWorldStorage
            val dim = world.provider.dimensionId
            var result = storage.loadData(ElnWorldStorage::class.java, key + dim) as ElnWorldStorage?
            if (result == null) {
                result = storage.loadData(ElnWorldStorage::class.java, key + dim + "back") as ElnWorldStorage?
            }
            if (result == null) {
                result = ElnWorldStorage(key + dim)
                result.dim = dim
                storage.setData(key + dim, result)
            }
            return result
        }
    }
}
