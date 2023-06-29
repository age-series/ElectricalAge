package mods.eln.server

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.server.DelayedTaskManager.ITask
import net.minecraft.init.Blocks
import java.util.*

class DelayedBlockRemove private constructor(var c: Coordinate) : ITask {
    override fun run() {
        BLOCKS.remove(c)
        c.block = Blocks.air
    }

    companion object {
        private val BLOCKS: MutableSet<Coordinate> = HashSet()
        @JvmStatic
        fun clear() {
            BLOCKS.clear()
        }

        @JvmStatic
        fun add(c: Coordinate) {
            if (BLOCKS.contains(c)) return
            BLOCKS.add(c)
            Eln.delayedTask.add(DelayedBlockRemove(c))
        }
    }
}
