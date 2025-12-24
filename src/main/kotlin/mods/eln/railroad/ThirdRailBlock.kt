package mods.eln.railroad

import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromIntMinecraftSide
import mods.eln.misc.Utils.entityLivingViewDirection
import mods.eln.misc.Utils.isRemote
import net.minecraft.block.Block
import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.BlockRail
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

/**
 * Rail block that owns a [ThirdRailNode]. Behaves like a normal rail so
 * minecarts can follow it, but also exposes ELN connection points.
 */
class ThirdRailBlock : BlockRail(), ITileEntityProvider {

    init {
        setStepSound(soundTypeMetal)
        setHardness(0.7f)
        setResistance(1.5f)
        useNeighborBrightness = true
        setBlockTextureName("rail_normal")
    }

    override fun hasTileEntity(metadata: Int) = true

    override fun createTileEntity(world: World?, meta: Int): TileEntity {
        return ThirdRailTileEntity()
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity? {
        return ThirdRailTileEntity()
    }

    fun newNode(): ThirdRailNode {
        return ThirdRailNode()
    }

    fun getFrontForPlacement(entity: EntityLivingBase?): Direction {
        return entityLivingViewDirection(entity!!).inverse
    }

    override fun onBlockAdded(world: World, x: Int, y: Int, z: Int) {
        super.onBlockAdded(world, x, y, z)
        if (!world.isRemote) {
            val entity = world.getTileEntity(x, y, z) as? ThirdRailTileEntity
            entity?.onBlockAdded()
        }
    }

    override fun breakBlock(world: World, x: Int, y: Int, z: Int, block: Block, metadata: Int) {
        val entity = world.getTileEntity(x, y, z) as? ThirdRailTileEntity
        entity?.onBreakBlock()
        super.breakBlock(world, x, y, z, block, metadata)
    }

    override fun removedByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean): Boolean {
        if (!world.isRemote) {
            val entity = world.getTileEntity(x, y, z) as? ThirdRailTileEntity
            val node = entity?.node
            if (node != null) {
                node.removedByPlayer = player as EntityPlayerMP
            }
        }
        return super.removedByPlayer(world, player, x, y, z, willHarvest)
    }

    override fun onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, neighbor: Block) {
        super.onNeighborBlockChange(world, x, y, z, neighbor)
        if (!isRemote(world)) {
            (world.getTileEntity(x, y, z) as? ThirdRailTileEntity)?.onNeighborBlockChange()
        }
    }

    override fun onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, vx: Float, vy: Float, vz: Float): Boolean {
        val entity = world.getTileEntity(x, y, z) as? ThirdRailTileEntity ?: return false
        return entity.onBlockActivated(player, fromIntMinecraftSide(side), vx, vy, vz)
    }

    override fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entityLiving: EntityLivingBase?, stack: ItemStack?) {
        super.onBlockPlacedBy(world, x, y, z, entityLiving, stack)
        if (entityLiving != null) {
            val te = world.getTileEntity(x, y, z) as? ThirdRailTileEntity ?: return
            val front = getFrontForPlacement(entityLiving)
            if (front != null) {
                te.front = front
                if (!world.isRemote) {
                    world.markBlockForUpdate(x, y, z)
                }
            }
        }
    }
}
