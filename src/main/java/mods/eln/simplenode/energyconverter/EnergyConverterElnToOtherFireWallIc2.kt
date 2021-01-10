package mods.eln.simplenode.energyconverter

import cpw.mods.fml.common.FMLCommonHandler
import ic2.api.energy.event.EnergyTileLoadEvent
import ic2.api.energy.event.EnergyTileUnloadEvent
import ic2.api.info.Info
import net.minecraftforge.common.MinecraftForge

object EnergyConverterElnToOtherFireWallIc2 {
    /**
     * Forward for the base TileEntity's updateEntity(), used for creating the energy net link.
     * Either updateEntity or onLoaded have to be used.
     */
    fun updateEntity(e: EnergyConverterElnToOtherEntity) {
        if (!e.addedToEnet) onLoaded(e)
    }

    /**
     * Notification that the base TileEntity finished loading, for advanced uses.
     * Either updateEntity or onLoaded have to be used.
     */
    fun onLoaded(e: EnergyConverterElnToOtherEntity) {
        if (!e.addedToEnet &&
            !FMLCommonHandler.instance().effectiveSide.isClient &&
            Info.isIc2Available()) {
            MinecraftForge.EVENT_BUS.post(EnergyTileLoadEvent(e))
            e.addedToEnet = true
        }
    }

    /**
     * Forward for the base TileEntity's invalidate(), used for destroying the energy net link.
     * Both invalidate and onChunkUnload have to be used.
     */
    fun invalidate(e: EnergyConverterElnToOtherEntity) {
        e.onChunkUnload()
    }

    /**
     * Forward for the base TileEntity's onChunkUnload(), used for destroying the energy net link.
     * Both invalidate and onChunkUnload have to be used.
     */
    fun onChunkUnload(e: EnergyConverterElnToOtherEntity) {
        if (e.addedToEnet && Info.isIc2Available()) {
            MinecraftForge.EVENT_BUS.post(EnergyTileUnloadEvent(e))
            e.addedToEnet = false
        }
    }
}
