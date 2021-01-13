package mods.eln.server

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.common.gameevent.TickEvent
import net.minecraftforge.common.MinecraftForge
import cpw.mods.fml.common.FMLCommonHandler
import java.util.*

class DelayedTaskManager {
    var tasks = LinkedList<ITask>()
    fun clear() {
        tasks.clear()
    }

    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val cpy: List<ITask> = ArrayList(tasks)
        tasks.clear()
        for (t in cpy) {
            t.run()
        }
    }

    interface ITask {
        fun run()
    }

    fun add(t: ITask) {
        tasks.add(t)
    }

    init {
        MinecraftForge.EVENT_BUS.register(this)
        FMLCommonHandler.instance().bus().register(this)
    }
}
