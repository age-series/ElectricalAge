package mods.eln.node.transparent

import mods.eln.Eln
import mods.eln.node.NodeBase
import mods.eln.node.NodeBlock
import mods.eln.node.NodeBlockEntity
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import java.lang.RuntimeException
import java.lang.reflect.InvocationTargetException
import java.util.*

class TransparentNodeBlock(material: Material?, tileEntityClass: Class<*>?) : NodeBlock(material, tileEntityClass!!, 0) {

    override fun getSubBlocks(par1: Item, tab: CreativeTabs, subItems: List<*>?) {
        Eln.transparentNodeItem.getSubItems(par1, tab, subItems)
    }

    override fun isOpaqueCube(): Boolean {
        return false
    }

    override fun renderAsNormalBlock(): Boolean {
        return false
    }

    override fun getRenderType(): Int {
        return -1
    }

    override fun removedByPlayer(world: World, entityPlayer: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean): Boolean {
        if (!world.isRemote) {
            val entity = world.getTileEntity(x, y, z) as? NodeBlockEntity
            if (entity != null) {
                val nodeBase: NodeBase? = entity.node
                if (nodeBase is TransparentNode) {
                    nodeBase.removedByPlayer = entityPlayer as EntityPlayerMP
                }
            }
        }
        @Suppress("DEPRECATION")
        return super.removedByPlayer(world, entityPlayer, x, y, z)
    }

    override fun getDamageValue(world: World, x: Int, y: Int, z: Int): Int {
        val tile = world.getTileEntity(x, y, z)
        return if (tile != null && tile is TransparentNodeEntity) (world.getTileEntity(x, y, z) as TransparentNodeEntity).getDamageValue(world, x, y, z) else 0
    }

    override fun getLightOpacity(world: IBlockAccess, x: Int, y: Int, z: Int): Int {
        return world.getBlockMetadata(x, y, z) and 3 shl 6
    }

    override fun getItemDropped(meta: Int, random: Random, fortune: Int): Item? {
        return null
    }

    override fun quantityDropped(par1Random: Random): Int {
        return 0
    }

    override fun canPlaceBlockOnSide(par1World: World, par2: Int, par3: Int, par4: Int, par5: Int): Boolean {
        return true
    }

    override fun addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, par5AxisAlignedBB: AxisAlignedBB, list: List<*>?, entity: Entity?) {
        val tileEntity = world.getTileEntity(x, y, z)
        if (tileEntity == null || tileEntity !is TransparentNodeEntity) {
            super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity)
        } else {
            @Suppress("UNCHECKED_CAST") tileEntity.addCollisionBoxesToList(par5AxisAlignedBB, list as MutableList<AxisAlignedBB?>, null)
        }
    }

    override fun createTileEntity(var1: World, meta: Int): TileEntity {
        try {
            for (tag in EntityMetaTag.values()) {
                if (tag.meta == meta) {
                    return tag.cls.getConstructor().newInstance() as TileEntity
                }
            }
            // Sadly, this will happen a lot with pre-metatag worlds.
            // Only real fix is to replace the blocks, but there should be no
            // serious downside to getting the wrong subclass so long as they really
            // wanted the superclass.
            println("Unknown block meta-tag: $meta")
            return EntityMetaTag.Basic.cls.getConstructor().newInstance() as TileEntity
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        throw RuntimeException("Unable to continue creating tile entity")
    }

    val nodeUuid: String
        get() = "t"
}
