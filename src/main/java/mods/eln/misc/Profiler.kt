package mods.eln.misc

import java.util.*

class Profiler {

    var list = LinkedList<ProfilerData>()
    fun reset() {
        list.clear()
    }

    fun add(name: String?) {
        list.add(ProfilerData(name, System.nanoTime()))
    }

    fun stop() {
        add(null)
    }

    override fun toString(): String {
        var str = ""
        var last: ProfilerData? = null
        for (p in list) {
            if (last != null) {
                str += "${last.name} in ${(p.nano - last.nano) / 1000.0}  "
            }
            last = p
        }
        return str
    }
}

data class ProfilerData(var name: String?, var nano: Long)
