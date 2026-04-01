package mods.eln.server

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.node.GhostNode
import mods.eln.node.NodeBase
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import mods.eln.node.transparent.TransparentNode
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.IInventory
import net.minecraft.world.World

data class ElnDestroySummary(
    val nodesDestroyed: Int,
    val blocksCleared: Int
)

object ElnDestroyHelper {
    fun destroyAroundPlayer(world: World, player: EntityPlayerMP, radius: Int): ElnDestroySummary? {
        val nodeManager = NodeManager.instance ?: return null
        val center = Coordinate(player.posX.toInt(), player.posY.toInt(), player.posZ.toInt(), world)
        return destroyWithinBounds(
            world = world,
            nodeManager = nodeManager,
            minX = center.x - radius,
            maxX = center.x + radius,
            minY = center.y - radius,
            maxY = center.y + radius,
            minZ = center.z - radius,
            maxZ = center.z + radius,
            player = player
        )
    }

    fun destroyWithinBounds(
        world: World,
        nodeManager: NodeManager,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        minZ: Int,
        maxZ: Int,
        player: EntityPlayerMP
    ): ElnDestroySummary {
        val dim = world.provider.dimensionId
        val targetNodes = nodeManager.nodeList.filter {
            val c = it.coordinate
            c.dimension == dim &&
                c.x in minX..maxX &&
                c.y in minY..maxY &&
                c.z in minZ..maxZ
        }

        var nodesDestroyed = 0
        for (node in targetNodes) {
            if (destroyElnNodeWithoutDrops(world, nodeManager, node, player)) {
                nodesDestroyed++
            }
        }

        var blocksCleared = 0
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    if (clearElnBlock(world, x, y, z)) {
                        blocksCleared++
                    }
                }
            }
        }

        return ElnDestroySummary(nodesDestroyed, blocksCleared)
    }

    fun clearElnBlock(world: World, x: Int, y: Int, z: Int): Boolean {
        val block = world.getBlock(x, y, z)
        val isElnBlock =
            block == Eln.sixNodeBlock ||
                block == Eln.transparentNodeBlock ||
                block == Eln.ghostBlock
        if (!isElnBlock) return false
        world.setBlockToAir(x, y, z)
        return true
    }

    private fun clearInventoryWithoutDrops(inventory: IInventory?) {
        if (inventory == null) return
        for (slot in 0 until inventory.sizeInventory) {
            inventory.setInventorySlotContents(slot, null)
        }
        inventory.markDirty()
    }

    private fun destroyElnNodeWithoutDrops(world: World, nodeManager: NodeManager, node: NodeBase, player: EntityPlayerMP): Boolean {
        val coord = node.coordinate
        val expectedBlock = when (node) {
            is SixNode -> Eln.sixNodeBlock
            is TransparentNode -> Eln.transparentNodeBlock
            is GhostNode -> Eln.ghostBlock
            else -> null
        }
        return try {
            when (node) {
                is SixNode -> {
                    node.sideElementList.forEach { element ->
                        clearInventoryWithoutDrops(element?.inventory)
                    }
                    node.sixNodeCacheBlock = net.minecraft.init.Blocks.air
                    for (direction in Direction.values()) {
                        if (node.getSideEnable(direction)) {
                            node.deleteSubBlock(player, direction)
                        }
                    }
                }
                is TransparentNode -> {
                    clearInventoryWithoutDrops(node.element?.inventory)
                    node.removedByPlayer = player
                }
            }
            if (expectedBlock != null && world.getBlock(coord.x, coord.y, coord.z) == expectedBlock) {
                world.setBlockToAir(coord.x, coord.y, coord.z)
            } else {
                node.onBreakBlock()
                nodeManager.removeNode(node)
            }
            true
        } catch (e: Exception) {
            println("zonedestroy: removal failed for $coord : ${e.message}")
            false
        }
    }
}
