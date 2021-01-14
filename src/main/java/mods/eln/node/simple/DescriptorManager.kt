package mods.eln.node.simple

import java.util.*

object DescriptorManager {
    val map = HashMap<Any, Any>()

    @JvmStatic
    fun put(key: Any, value: Any) {
        map[key] = value
    }

    @JvmStatic
    operator fun <T> get(key: Any?): T? {
        return if (key == null) null else map[key] as T?
    }
}
