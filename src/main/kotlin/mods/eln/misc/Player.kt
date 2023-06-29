package mods.eln.misc

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.entity.player.EntityPlayerMP

/**
 * Contains utilities for dealing with player entities.
 */

fun EntityPlayer.totalItemsCarried(stack: ItemStack): Int {
    return inventory.mainInventory
        .filterNotNull()
        .filter { it.isItemEqual(stack) }
        .sumOf { it.stackSize }
}

fun EntityPlayer.removeMultipleItems(stack: ItemStack, count: Int) {
    if(Utils.isCreative(this as EntityPlayerMP)) return
    assert(count <= totalItemsCarried(stack))
    var left = count
    try {
        inventory.mainInventory.indices.reversed().forEach { i ->
            val invStack = inventory.mainInventory[i]
            if (invStack?.isItemEqual(stack) == true) {
                left -= invStack.splitStack(invStack.stackSize.coerceAtMost(left)).stackSize
                assert(invStack.stackSize >= 0)
                // Black magic used to synchronize immediately with the client.
                val slot = openContainer.getSlotFromInventory(inventory, i)
                playerNetServerHandler.sendPacket(S2FPacketSetSlot(openContainer.windowId, slot.slotNumber, invStack))
                if (left == 0) return
            }
        }
    } finally {
        inventory.markDirty()
    }
}
