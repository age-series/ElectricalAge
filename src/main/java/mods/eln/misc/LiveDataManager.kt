package mods.eln.misc

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent
import java.util.*

/*

TODO: I'm not actually convinced this file does anything useful. It's only called in Eln.java for reasons unclear.

 */

class LiveDataManager {
    fun start() {}

    fun stop() {
        map.clear()
    }

    fun getData(key: Any?, timeout: Int): Any? {
        val e = map[key] ?: return null
        e.timeout = timeout
        return e.data
    }

    fun newData(key: Any, data: Any, timeout: Int): Any {
        map[key] = Element(data, timeout)
        Utils.println("NewLiveData")
        return data
    }

    var map: MutableMap<Any, Element> = HashMap()
    @SubscribeEvent
    fun tick(event: RenderTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        val keyToRemove: MutableList<Any> = ArrayList()
        for ((key, e) in map) {
            e.timeout--
            if (e.timeout < 0) {
                keyToRemove.add(key)
                Utils.println("LiveDeleted")
            }
        }
        for (key in keyToRemove) {
            map.remove(key)
        }
    }

    init {
        FMLCommonHandler.instance().bus().register(this)
    }
}

data class Element(var data: Any, var timeout: Int)
