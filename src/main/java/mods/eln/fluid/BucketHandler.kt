/**
 *
 * Copyright © SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 *
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 *
 * 1.0, or MMPL. Please check the contents of the license located in
 *
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 *
 */
package mods.eln.fluid

import cpw.mods.fml.common.eventhandler.Event
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemBucket
import net.minecraft.item.ItemStack
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.FillBucketEvent

object BucketHandler {
    @JvmField
    var buckets: MutableMap<Block, ItemBucket> = mutableMapOf()
    @SubscribeEvent
    fun onBucketFill(event: FillBucketEvent) {
        val result = fillCustomBucket(event.world, event.target) ?: return
        event.result = result
        event.setResult(Event.Result.ALLOW)
    }

    private fun fillCustomBucket(world: World, pos: MovingObjectPosition): ItemStack? {
        val block = world.getBlock(pos.blockX, pos.blockY, pos.blockZ)
        val bucket = buckets[block]
        return if (bucket != null && world.getBlockMetadata(pos.blockX, pos.blockY, pos.blockZ) == 0) {
            world.setBlockToAir(pos.blockX, pos.blockY, pos.blockZ)
            ItemStack(bucket)
        } else null
    }
}
