package mods.eln.server

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.common.gameevent.TickEvent
import net.minecraftforge.common.MinecraftForge
import cpw.mods.fml.common.FMLCommonHandler
import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedQueue

class DelayedTaskManager {
    private val tasks = ConcurrentLinkedQueue<ITask>()
    fun clear() {
        tasks.clear()
    }

    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val cpy = ArrayList<ITask>()
        while (true) {
            val task = tasks.poll() ?: break
            cpy.add(task)
        }
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
