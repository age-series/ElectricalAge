package mods.eln.simplenode.test

import mods.eln.node.simple.SimpleNode
import mods.eln.node.simple.SimpleNodeBlock
import net.minecraft.block.material.Material
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class TestBlock : SimpleNodeBlock(Material.packedIce) {
    override fun createNewTileEntity(world: World?, meta: Int): TileEntity {
        return TestEntity()
    }

    override fun newNode(): SimpleNode {
        return TestNode()
    }
}
