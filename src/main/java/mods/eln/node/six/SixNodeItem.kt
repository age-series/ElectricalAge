@file:Suppress("NAME_SHADOWING")
package mods.eln.node.six

import mods.eln.Eln
import mods.eln.generic.GenericItemBlockUsingDamage
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction.Companion.fromIntMinecraftSide
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.addChatMessage
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11

class SixNodeItem(b: Block?) : GenericItemBlockUsingDamage<SixNodeDescriptor?>(b), IItemRenderer {
    override fun getMetadata(damageValue: Int): Int {
        return damageValue
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    override fun onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        var x = x
        var y = y
        var z = z
        var side = side
        val block = world.getBlock(x, y, z)
        if (block === Blocks.snow_layer && world.getBlockMetadata(x, y, z) and 0x7 < 1) {
            side = 1
        } else if (block !== Blocks.vine && block !== Blocks.tallgrass && block !== Blocks.deadbush && !block.isReplaceable(world, x, y, z)) {
            if (side == 0) y--
            if (side == 1) y++
            if (side == 2) z--
            if (side == 3) z++
            if (side == 4) x--
            if (side == 5) x++
        }
        if (stack.stackSize == 0) return false
        if (!player.canPlayerEdit(x, y, z, side, stack)) return false
        if (y == 255 && field_150939_a.material.isSolid) return false
        val i1 = getMetadata(stack.itemDamage)
        val metadata = field_150939_a.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, i1)
        if (placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
            world.playSoundEffect((x + 0.5f).toDouble(), (y + 0.5f).toDouble(), (z + 0.5f).toDouble(), field_150939_a.stepSound.func_150496_b(), (field_150939_a.stepSound.getVolume() + 1.0f) / 2.0f, field_150939_a.stepSound.pitch * 0.8f)
            stack.stackSize -= 1
        }
        return true
    }

    /**
     * Returns true if the given ItemBlock can be placed on the given side of the given block position.
     */
    // func_150936_a <= canPlaceItemBlockOnSide
    override fun func_150936_a(par1World: World, x: Int, y: Int, z: Int, par5: Int, par6EntityPlayer: EntityPlayer, par7ItemStack: ItemStack): Boolean {
        if (!isStackValidToPlace(par7ItemStack)) return false
        val vect = intArrayOf(x, y, z)
        fromIntMinecraftSide(par5)!!.applyTo(vect, 1)
        val descriptor = getDescriptor(par7ItemStack)
        if (!descriptor!!.canBePlacedOnSide(par6EntityPlayer, Coordinate(x, y, z, par1World), fromIntMinecraftSide(par5)!!.inverse)) {
            return false
        }
        if (par1World.getBlock(vect[0], vect[1], vect[2]) === Eln.sixNodeBlock) return true
        return super.func_150936_a(par1World, x, y, z, par5, par6EntityPlayer, par7ItemStack)
    }

    fun isStackValidToPlace(stack: ItemStack?): Boolean {
        val descriptor = getDescriptor(stack)
        return descriptor != null
    }

    override fun placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float, metadata: Int): Boolean {
        if (world.isRemote) return false
        if (!isStackValidToPlace(stack)) return false
        val direction = fromIntMinecraftSide(side)!!.inverse
        val blockOld = world.getBlock(x, y, z)
        val block = Block.getBlockFromItem(this) as SixNodeBlock
        if (blockOld === Blocks.air || blockOld.isReplaceable(world, x, y, z)) {
            val coord = Coordinate(x, y, z, world)
            val descriptor = getDescriptor(stack)
            var error: String?
            if (descriptor!!.checkCanPlace(coord, direction, LRDU.Up).also { error = it } != null) {
                addChatMessage(player, error)
                return false
            }
            if (block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                val ghostgroup = descriptor.getGhostGroup(direction, LRDU.Up)
                ghostgroup?.plot(coord, coord, descriptor.ghostGroupUuid)
                val sixNode = SixNode()
                sixNode.onBlockPlacedBy(Coordinate(x, y, z, world), direction, player, stack)
                sixNode.createSubBlock(stack, direction, player)
                world.setBlock(x, y, z, block, metadata, 0x03)
                block.getIfOtherBlockIsSolid(world, x, y, z, direction)
                block.onBlockPlacedBy(world, x, y, z, fromIntMinecraftSide(side)!!.inverse, player, metadata)
                return true
            }
        } else if (blockOld === block) {
            val sixNode = (world.getTileEntity(x, y, z) as SixNodeEntity).node as SixNode?
            if (sixNode == null) {
                world.setBlockToAir(x, y, z)
                return false
            }
            if (!sixNode.getSideEnable(direction) && block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                sixNode.createSubBlock(stack, direction, player)
                block.onBlockPlacedBy(world, x, y, z, fromIntMinecraftSide(side)!!.inverse, player, metadata)
                return true
            }
        } else {
            val sixNode = (world.getTileEntity(x, y, z) as SixNodeEntity).node as SixNode?
            if (sixNode == null) {
                world.setBlockToAir(x, y, z)
                return false
            }
        }
        return false
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        return if (getDescriptor(item) == null) false else getDescriptor(item)!!.handleRenderType(item, type)
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
        return if (!isStackValidToPlace(item)) false else getDescriptor(item)!!.shouldUseRenderHelper(type, item, helper)
    }

    fun shouldUseRenderHelperEln(type: ItemRenderType?, item: ItemStack?, helper: ItemRendererHelper?): Boolean {
        return if (!isStackValidToPlace(item)) false else getDescriptor(item)!!.shouldUseRenderHelperEln(type, item, helper)
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        if (!isStackValidToPlace(item)) return
        Minecraft.getMinecraft().mcProfiler.startSection("SixNodeItem")
        if (shouldUseRenderHelperEln(type, item, null)) {
            when (type) {
                ItemRenderType.ENTITY -> GL11.glRotatef(90f, 0f, 0f, 1f)
                ItemRenderType.EQUIPPED_FIRST_PERSON -> {
                    GL11.glRotatef(160f, 0f, 1f, 0f)
                    GL11.glTranslatef(-0.70f, 1f, -0.7f)
                    GL11.glScalef(1.8f, 1.8f, 1.8f)
                    GL11.glRotatef(-90f, 1f, 0f, 0f)
                }
                ItemRenderType.EQUIPPED -> {
                    GL11.glRotatef(180f, 0f, 1f, 0f)
                    GL11.glTranslatef(-0.70f, 1f, -0.7f)
                    GL11.glScalef(1.5f, 1.5f, 1.5f)
                }
                ItemRenderType.FIRST_PERSON_MAP -> {
                }
                ItemRenderType.INVENTORY -> {
                    GL11.glRotatef(-90f, 0f, 1f, 0f)
                    GL11.glRotatef(-90f, 1f, 0f, 0f)
                }
                else -> {
                }
            }
        }
        getDescriptor(item)!!.renderItem(type, item, *data)
        Minecraft.getMinecraft().mcProfiler.endSection()
    }

    init {
        setHasSubtypes(true)
        unlocalizedName = "SixNodeItem"
    }
}
