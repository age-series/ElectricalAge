package mods.eln.node

import net.minecraft.block.Block
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class NodeBlockItem(b: Block?) : ItemBlock(b) {

    override fun getMetadata(damageValue: Int): Int {
        return damageValue
    }

    val block: NodeBlock
        get() = Block.getBlockFromItem(this) as NodeBlock

    init {
        unlocalizedName = "NodeBlockItem"
    }
}
