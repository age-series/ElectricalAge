package mods.eln.node.simple

import mods.eln.misc.Coordinate
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class SimpleNodeItem(b: Block) : ItemBlock(b) {
    var block: SimpleNodeBlock
    override fun placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float, metadata: Int): Boolean {
        var node: SimpleNode? = null
        if (!world.isRemote) {
            node = block.newNode()
            node!!.descriptorKey = block.descriptorKey
            node.onBlockPlacedBy(Coordinate(x, y, z, world), block.getFrontForPlacement(player), player, stack)
        }
        if (!world.setBlock(x, y, z, field_150939_a, metadata, 3)) {
            node?.onBreakBlock()
            return false
        }
        if (world.getBlock(x, y, z) === field_150939_a) {
            field_150939_a.onBlockPlacedBy(world, x, y, z, player, stack)
            field_150939_a.onPostBlockPlaced(world, x, y, z, metadata)
        }
        return true
    }

    init {
        block = b as SimpleNodeBlock
    }
}
