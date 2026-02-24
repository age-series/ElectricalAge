package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mods.eln.environment.RoomThermalManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

public class RoomThermalBlockEventsHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        RoomThermalManager.INSTANCE.onBlockChanged(event.world, event.x, event.y, event.z);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        RoomThermalManager.INSTANCE.onBlockChanged(event.world, event.x, event.y, event.z);
    }

    @SubscribeEvent
    public void onBlockMultiPlace(BlockEvent.MultiPlaceEvent event) {
        RoomThermalManager.INSTANCE.onBlockChanged(event.world, event.x, event.y, event.z);
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.world == null || event.world.isRemote) return;
        for (Object obj : event.getAffectedBlocks()) {
            if (!(obj instanceof net.minecraft.world.ChunkPosition)) continue;
            net.minecraft.world.ChunkPosition pos = (net.minecraft.world.ChunkPosition) obj;
            RoomThermalManager.INSTANCE.onBlockChanged(event.world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
        }
    }
}
