package mods.eln.item

import mods.eln.entity.carts.EntityElectricMinecart
import mods.eln.generic.GenericItemUsingDamageDescriptor
import net.minecraft.block.BlockRailBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class ElectricMinecartItem(name: String) : GenericItemUsingDamageDescriptor(name) {
    override fun onItemUse(
        stack: ItemStack?,
        player: EntityPlayer?,
        world: World?,
        x: Int,
        y: Int,
        z: Int,
        side: Int,
        vx: Float,
        vy: Float,
        vz: Float
    ): Boolean {
        if (world == null || stack == null) return false
        return if (BlockRailBase.func_150051_a(world.getBlock(x, y, z))) {
            if (!world.isRemote) {
                val minecart = EntityElectricMinecart(
                    world,
                    (x.toFloat() + 0.5f).toDouble(),
                    (y.toFloat() + 0.5f).toDouble(),
                    (z.toFloat() + 0.5f).toDouble()
                )
                if (stack.hasDisplayName()) {
                    minecart.setMinecartName(stack.displayName)
                }
                world.spawnEntityInWorld(minecart)
            }
            --stack.stackSize
            true
        } else false
    }
}
