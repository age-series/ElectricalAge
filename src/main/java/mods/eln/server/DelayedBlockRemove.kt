package mods.eln.server

import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.server.DelayedTaskManager.ITask
import net.minecraft.init.Blocks
import java.util.*

class DelayedBlockRemove private constructor(var c: Coordonate) : ITask {
    override fun run() {
        blocks.remove(c)
        c.block = Blocks.air
    }

    companion object {
        private val blocks: MutableSet<Coordonate> = HashSet()
        @JvmStatic
        fun clear() {
            blocks.clear()
        }

        @JvmStatic
        fun add(c: Coordonate) {
            if (blocks.contains(c)) return
            blocks.add(c)
            Eln.delayedTask.add(DelayedBlockRemove(c))
        }
    }
}
