package mods.eln.railroad

import mods.eln.misc.Coordinate
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.world.World

/**
 * Places [ThirdRailBlock] instances and wires them into the node system.
 */
class ThirdRailItem(block: Block) : ItemBlock(block) {
    private val thirdRailBlock = block as ThirdRailBlock

    override fun placeBlockAt(
        stack: ItemStack,
        player: EntityPlayer,
        world: World,
        x: Int,
        y: Int,
        z: Int,
        side: Int,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
        metadata: Int
    ): Boolean {
        var node: ThirdRailNode? = null
        if (!world.isRemote) {
            node = thirdRailBlock.newNode()
            node.onBlockPlacedBy(Coordinate(x, y, z, world), thirdRailBlock.getFrontForPlacement(player), player, stack)
        }
        val placed = super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)
        if (!placed) {
            node?.onBreakBlock()
        }
        return placed
    }
}
