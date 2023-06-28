@file:Suppress("NAME_SHADOWING")
package mods.eln.ghost

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Utils.getTags
import mods.eln.node.NodeManager
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraft.world.WorldSavedData
import java.util.*

class GhostManager(par1Str: String?) : WorldSavedData(par1Str) {
    var ghostTable: MutableMap<Coordinate?, GhostElement> = Hashtable()
    var observerTable: MutableMap<Coordinate?, GhostObserver> = Hashtable()
    fun clear() {
        ghostTable.clear()
        observerTable.clear()
    }

    fun init() {}
    override fun isDirty(): Boolean {
        return true
    }

    fun getGhost(coordinate: Coordinate?): GhostElement? {
        return ghostTable[coordinate]
    }

    fun removeGhost(coordinate: Coordinate?) {
        removeGhostNode(coordinate)
        ghostTable.remove(coordinate)
    }

    fun addObserver(observer: GhostObserver) {
        observerTable[observer.ghostObserverCoordonate] = observer
    }

    fun getObserver(coordinate: Coordinate?): GhostObserver? {
        return observerTable[coordinate]
    }

    fun removeObserver(coordinate: Coordinate?) {
        observerTable.remove(coordinate)
    }

    fun removeGhostAndBlockWithObserver(observerCoordinate: Coordinate?) {
        val iterator: MutableIterator<Map.Entry<Coordinate?, GhostElement>> = ghostTable.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val element = entry.value
            if (element.observatorCoordonate!!.equals(observerCoordinate)) {
                iterator.remove()
                removeGhostNode(element.elementCoordinate)
                element.elementCoordinate!!.world().setBlockToAir(element.elementCoordinate!!.x, element.elementCoordinate!!.y, element.elementCoordinate!!.z)
            }
        }
    }

    fun removeGhostAndBlockWithObserver(observerCoordinate: Coordinate?, uuid: Int) {
        val iterator: MutableIterator<Map.Entry<Coordinate?, GhostElement>> = ghostTable.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val element = entry.value
            if (element.observatorCoordonate!!.equals(observerCoordinate) && element.uUID == uuid) {
                iterator.remove()
                removeGhostNode(element.elementCoordinate)
                element.elementCoordinate!!.world().setBlockToAir(element.elementCoordinate!!.x, element.elementCoordinate!!.y, element.elementCoordinate!!.z)
            }
        }
    }

    fun removeGhostAndBlockWithObserverAndNotUuid(observerCoordinate: Coordinate?, uuid: Int) {
        val iterator: MutableIterator<Map.Entry<Coordinate?, GhostElement>> = ghostTable.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val element = entry.value
            if (element.observatorCoordonate!!.equals(observerCoordinate) && element.uUID != uuid) {
                iterator.remove()
                removeGhostNode(element.elementCoordinate)
                element.elementCoordinate!!.world().setBlockToAir(element.elementCoordinate!!.x, element.elementCoordinate!!.y, element.elementCoordinate!!.z)
            }
        }
    }

    fun removeGhostNode(c: Coordinate?) {
        val node = NodeManager.instance!!.getNodeFromCoordonate(c) ?: return
        node.onBreakBlock()
    }

    fun removeGhostAndBlock(coordinate: Coordinate) {
        removeGhost(coordinate)
        coordinate.world().setBlockToAir(coordinate.x, coordinate.y, coordinate.z) //caca1.5.1
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
    }

    fun loadFromNBT(nbt: NBTTagCompound?) {
        for (o in getTags(nbt!!)) {
            val ghost = GhostElement()
            ghost.readFromNBT(o, "")
            ghostTable[ghost.elementCoordinate] = ghost
        }
    }

    fun saveToNBT(nbt: NBTTagCompound, dim: Int) {
        var nodeCounter = 0
        for (ghost in ghostTable.values) {
            if (dim != Int.MIN_VALUE && ghost.elementCoordinate!!.dimension != dim) continue
            val nbtGhost = NBTTagCompound()
            ghost.writeToNBT(nbtGhost, "")
            nbt.setTag("n" + nodeCounter++, nbtGhost)
        }
    }

    fun unload(dimensionId: Int) {
        val i = ghostTable.values.iterator()
        while (i.hasNext()) {
            val n = i.next()
            if (n.elementCoordinate!!.dimension == dimensionId) {
                i.remove()
            }
        }
    }

    fun canCreateGhostAt(world: World, x: Int, y: Int, z: Int): Boolean {
        return if (!world.chunkProvider.chunkExists(x shr 4, z shr 4)) {
            false
        } else !(world.getBlock(x, y, z) !== Blocks.air && !world.getBlock(x, y, z).isReplaceable(world, x, y, z))
    }

    @JvmOverloads
    fun createGhost(coordinate: Coordinate, observerCoordinate: Coordinate, UUID: Int, block: Block? = Eln.ghostBlock, meta: Int = GhostBlock.tCube) {
        var coordinate = coordinate
        coordinate.world().setBlockToAir(coordinate.x, coordinate.y, coordinate.z)
        if (coordinate.world().setBlock(coordinate.x, coordinate.y, coordinate.z, block, meta, 3)) {
            coordinate = Coordinate(coordinate)
            val element = GhostElement(coordinate, observerCoordinate, UUID)
            ghostTable[element.elementCoordinate] = element
        }
    }
}
