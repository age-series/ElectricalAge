package mods.eln.ghost

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import net.minecraft.block.Block
import net.minecraft.world.World
import java.util.*

data class GhostGroupElement(var x: Int, var y: Int, var z: Int, var block: Block, var meta: Int)

class GhostGroup {
    var elementList = ArrayList<GhostGroupElement>()

    fun addElement(x: Int, y: Int, z: Int) {
        elementList.add(GhostGroupElement(x, y, z, Eln.ghostBlock, GhostBlock.tCube))
    }

    fun addElement(x: Int, y: Int, z: Int, block: Block, meta: Int) {
        elementList.add(GhostGroupElement(x, y, z, block, meta))
    }

    fun removeElement(x: Int, y: Int, z: Int) {
        val i = elementList.iterator()
        var g: GhostGroupElement
        while (i.hasNext()) {
            g = i.next()
            if (g.x == x && g.y == y && g.z == z) {
                i.remove()
            }
        }
    }

    fun addRectangle(x1: Int, x2: Int, y1: Int, y2: Int, z1: Int, z2: Int) {
        for (x in x1..x2) {
            for (y in y1..y2) {
                for (z in z1..z2) {
                    addElement(x, y, z)
                }
            }
        }
    }

    fun removeRectangle(x1: Int, x2: Int, y1: Int, y2: Int, z1: Int, z2: Int) {
        for (x in x1..x2) {
            for (y in y1..y2) {
                for (z in z1..z2) {
                    removeElement(x, y, z)
                }
            }
        }
    }

    fun canBePloted(c: Coordinate): Boolean {
        return canBePloted(c.world(), c.x, c.y, c.z)
    }

    fun canBePloted(world: World, x: Int, y: Int, z: Int): Boolean {
        for (element in elementList) {
            if (!Eln.ghostManager.canCreateGhostAt(world, x + element.x, y + element.y, z + element.z)) return false
        }
        return true
    }

    fun plot(coordinate: Coordinate, observerCoordinate: Coordinate, UUID: Int): Boolean {
        if (!canBePloted(coordinate.world(), coordinate.x, coordinate.y, coordinate.z)) return false
        for (element in elementList) {
            val offsetCoordinate = coordinate.newWithOffset(element.x, element.y, element.z)
            Eln.ghostManager.createGhost(offsetCoordinate, observerCoordinate, UUID, element.block, element.meta)
        }
        return true
    }

    fun erase(observerCoordinate: Coordinate?) {
        Eln.ghostManager.removeGhostAndBlockWithObserver(observerCoordinate)
    }

    fun erase(observerCoordinate: Coordinate?, uuid: Int) {
        Eln.ghostManager.removeGhostAndBlockWithObserver(observerCoordinate, uuid)
    }

    fun eraseGeo(coordinate: Coordinate) {
        for (element in elementList) {
            Eln.ghostManager.removeGhostAndBlock(coordinate.newWithOffset(element.x, element.y, element.z))
        }
    }

    fun newRotate(dir: Direction?): GhostGroup {
        val other = GhostGroup()
        for (element in elementList) {
            var x: Int
            var y: Int
            var z: Int
            when (dir) {
                Direction.XN -> {
                    x = element.x
                    y = element.y
                    z = element.z
                }
                Direction.XP -> {
                    x = -element.x
                    y = element.y
                    z = -element.z
                }
                Direction.ZN -> {
                    x = -element.z
                    y = element.y
                    z = element.x
                }
                Direction.ZP -> {
                    x = element.z
                    y = element.y
                    z = -element.x
                }
                Direction.YN -> {
                    x = -element.y
                    y = element.x
                    z = element.z
                }
                Direction.YP -> {
                    x = element.y
                    y = -element.x
                    z = element.z
                }
                else -> {
                    x = -element.y
                    y = element.x
                    z = element.z
                }
            }
            other.addElement(x, y, z, element.block, element.meta)
        }
        return other
    }

    fun newRotate(dir: Direction?, @Suppress("UNUSED_PARAMETER")front: LRDU?): GhostGroup {
        return newRotate(dir)
    }

    fun size(): Int {
        return elementList.size
    }
}
