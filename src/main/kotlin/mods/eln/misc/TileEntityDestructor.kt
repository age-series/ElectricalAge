package mods.eln.misc

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraft.tileentity.TileEntity
import java.util.*

class TileEntityDestructor {
    var destroyList = ArrayList<TileEntity>()
    fun clear() {
        destroyList.clear()
    }

    fun add(tile: TileEntity) {
        destroyList.add(tile)
    }

    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        for (t in destroyList) {
            if (t.worldObj != null && t.worldObj.getTileEntity(t.xCoord, t.yCoord, t.zCoord) === t) {
                t.worldObj.setBlockToAir(t.xCoord, t.yCoord, t.zCoord)
                Utils.println("destroy light at " + t.xCoord + " " + t.yCoord + " " + t.zCoord)
            }
        }
        destroyList.clear()
    }

    init {
        FMLCommonHandler.instance().bus().register(this)
    }
}
