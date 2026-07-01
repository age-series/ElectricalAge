package mods.eln.eventhandlers

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import mods.eln.environment.RoomThermalManager
import net.minecraft.world.ChunkPosition
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.event.world.ExplosionEvent

class RoomThermalBlockEventsHandler {
    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        RoomThermalManager.onBlockChanged(event.world, event.x, event.y, event.z)
    }

    @SubscribeEvent
    fun onBlockPlace(event: BlockEvent.PlaceEvent) {
        RoomThermalManager.onBlockChanged(event.world, event.x, event.y, event.z)
    }

    @SubscribeEvent
    fun onBlockMultiPlace(event: BlockEvent.MultiPlaceEvent) {
        RoomThermalManager.onBlockChanged(event.world, event.x, event.y, event.z)
    }

    @SubscribeEvent
    fun onExplosionDetonate(event: ExplosionEvent.Detonate) {
        val world = event.world
        if (world == null || world.isRemote) return

        for (obj in event.affectedBlocks) {
            val pos = obj as? ChunkPosition ?: continue
            RoomThermalManager.onBlockChanged(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ)
        }
    }
}
