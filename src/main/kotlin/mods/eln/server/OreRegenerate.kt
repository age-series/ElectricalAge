package mods.eln.server

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import mods.eln.Eln
import mods.eln.misc.Utils
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.ChunkEvent
import java.util.*

class OreRegenerate {
    var jobs = LinkedList<ChunkRef>()
    var alreadyLoadedChunks = HashSet<ChunkRef>()

    fun clear() {
        jobs.clear()
        alreadyLoadedChunks.clear()
    }

    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        for (idx in 0..0) {
            if (!jobs.isEmpty()) {
                val j = jobs.pollLast()
                if (!Eln.saveConfig.reGenOre && !Eln.instance.forceOreRegen) return
                val server = FMLCommonHandler.instance().minecraftServerInstance.worldServerForDimension(j.worldId)
                val chunk = server.getChunkFromChunkCoords(j.x, j.z)
                var y = 0
                while (y < 60) {
                    var z = y and 1
                    while (z < 16) {
                        var x = y and 1
                        while (x < 16) {
                            if (chunk.getBlock(x, y, z) === Eln.oreBlock) {
                                return
                            }
                            x += 2
                        }
                        z += 2
                    }
                    y += 2
                }
                Utils.println("Regenerated! " + jobs.size)
                for (d in Eln.oreItem.descriptors) {
                    d.generate(server.rand, chunk.xPosition, chunk.zPosition, server, null, null)
                }
            }
        }
    }

    @SubscribeEvent
    fun chunkLoad(e: ChunkEvent.Load) {
        if (e.world.isRemote || Eln.saveConfig != null && !Eln.saveConfig.reGenOre) return
        val c = e.chunk
        val ref = ChunkRef(c.xPosition, c.zPosition, c.worldObj.provider.dimensionId)
        if (alreadyLoadedChunks.contains(ref)) {
            Utils.println("Already regenerated!")
            return
        }
        alreadyLoadedChunks.add(ref)
        jobs.addFirst(ref)
    }

    init {
        MinecraftForge.EVENT_BUS.register(this)
        FMLCommonHandler.instance().bus().register(this)
    }
}

class ChunkRef(var x: Int, var z: Int, var worldId: Int) {
    override fun hashCode(): Int {
        return x * z + (worldId shl 20)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ChunkRef) return false
        return other.x == x && other.z == z && other.worldId == worldId
    }
}
