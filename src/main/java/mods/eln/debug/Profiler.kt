package mods.eln.debug

import java.util.LinkedList

class Profiler() {

    internal var list: LinkedList<ProfilerData> = LinkedList()

    val time: Long
        get() {
            if (list.size >= 2) {
                return (list.last.nano - list.first.nano) / 1000
            }
            return 0
        }

    fun reset() {
        list.clear()
    }

    fun add(name: String) {
        list.add(ProfilerData(name))
    }

    fun start() {
        list.add(ProfilerData("start"))
    }

    fun stop() {
        list.add(ProfilerData("stop"))
    }

    override fun toString(): String {
        return "${list.first.name} in ${this.time}ps"
    }

    data class ProfilerData(val name: String) {
        val nano: Long = System.nanoTime()
        override fun toString(): String = "ProfilerData($name, ${nano}ns"
    }
}

class ProfilerTester {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val p = Profiler()

            p.start()
            var x = 1.0
            for (i in (1 .. 100)) {
                if (x > 10e10) {
                    x /= i
                } else {
                    x *= i
                }
            }
            p.stop()
            DP.println(DPType.CONSOLE, "x: $x")
            DP.println(DPType.CONSOLE, "${p.list}")
            DP.println(DPType.CONSOLE, "$p")
            p.reset()
            if (p.list.size != 0) {
                DP.println(DPType.CONSOLE, "ASSERTION FAILURE! ${p.list.size} != 0")
            }
            p.stop()
            if (p.time != 0.toLong()) {
                DP.println(DPType.CONSOLE, "ASSERTION FAILURE! ${p.time} != 0")
            }
        }
    }
}
