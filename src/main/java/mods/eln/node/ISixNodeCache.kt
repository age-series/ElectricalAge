package mods.eln.node

import net.minecraft.item.ItemStack

interface ISixNodeCache {
    fun accept(stack: ItemStack): Boolean
    fun getMeta(stack: ItemStack): Int
}
