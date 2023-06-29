@file:Suppress("NAME_SHADOWING")
package mods.eln.node.transparent

import mods.eln.generic.GenericItemBlockUsingDamage
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction.Companion.fromIntMinecraftSide
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.Utils.nullCheck
import mods.eln.node.NodeBlock
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11

class TransparentNodeItem(b: Block?) : GenericItemBlockUsingDamage<TransparentNodeDescriptor?>(b), IItemRenderer {
    override fun placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float, metadata: Int): Boolean {
        var x = x
        var y = y
        var z = z
        if (world.isRemote) return false
        val descriptor = getDescriptor(stack)
        val direction = fromIntMinecraftSide(side)!!.inverse
        val front = descriptor!!.getFrontFromPlace(direction, player)
        val v = intArrayOf(descriptor.spawnDeltaX, descriptor.spawnDeltaY, descriptor.spawnDeltaZ)
        front!!.rotateFromXN(v)
        x += v[0]
        y += v[1]
        z += v[2]
        val coord = Coordinate(x, y, z, world)
        var error: String?
        if (descriptor.checkCanPlace(coord, front).also { error = it } != null) {
            addChatMessage(player, error)
            return false
        }
        val ghostgroup = descriptor.getGhostGroupFront(front)
        ghostgroup?.plot(coord, coord, descriptor.ghostGroupUuid)
        val node = TransparentNode()
        node.onBlockPlacedBy(coord, front, player, stack)
        world.setBlock(x, y, z, Block.getBlockFromItem(this), node.blockMetadata, 0x03) //caca1.5.1
        (Block.getBlockFromItem(this) as NodeBlock).onBlockPlacedBy(world, x, y, z, direction, player, metadata)
        node.checkCanStay(true)
        return true
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        val d = getDescriptor(item)
        return if (nullCheck(d)) false else d!!.handleRenderType(item, type)
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack,
                                       helper: ItemRendererHelper): Boolean {
        return getDescriptor(item)!!.shouldUseRenderHelper(type, item, helper)
    }

    fun shouldUseRenderHelperEln(type: ItemRenderType?, item: ItemStack?, helper: ItemRendererHelper?): Boolean {
        return getDescriptor(item)!!.shouldUseRenderHelperEln(type, item, helper)
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        Minecraft.getMinecraft().mcProfiler.startSection("TransparentNodeItem")
        if (shouldUseRenderHelperEln(type, item, null)) {
            when (type) {
                ItemRenderType.ENTITY -> GL11.glTranslatef(0.00f, 0.3f, 0.0f)
                ItemRenderType.EQUIPPED_FIRST_PERSON -> GL11.glTranslatef(0.50f, 1f, 0.5f)
                ItemRenderType.EQUIPPED -> GL11.glTranslatef(0.50f, 1f, 0.5f)
                ItemRenderType.FIRST_PERSON_MAP -> {
                }
                ItemRenderType.INVENTORY -> GL11.glRotatef(90f, 0f, 1f, 0f)
                else -> {
                }
            }
        }
        getDescriptor(item)!!.renderItem(type, item, *data)
        Minecraft.getMinecraft().mcProfiler.endSection()
    }

    init {
        setHasSubtypes(true)
        unlocalizedName = "TransparentNodeItem"
    }
}
