package mods.eln.ghost

import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import net.minecraft.entity.player.EntityPlayer

interface GhostObserver {
    val ghostObserverCoordonate: Coordinate?
    fun ghostDestroyed(UUID: Int)
    fun ghostBlockActivated(UUID: Int, entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean
}
