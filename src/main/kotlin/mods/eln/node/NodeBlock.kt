package mods.eln.node

import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromIntMinecraftSide
import mods.eln.misc.Utils.isRemote
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

abstract class NodeBlock(material: Material?, tileEntityClass: Class<*>, blockItemNbr: Int) : Block(material) {

    var blockItemNbr: Int
    var tileEntityClass: Class<*>
    override fun getBlockHardness(world: World, x: Int, y: Int, z: Int): Float {
        return 1.0f
    }

    override fun isProvidingWeakPower(block: IBlockAccess, x: Int, y: Int, z: Int, side: Int): Int {
        val entity = block.getTileEntity(x, y, z) as NodeBlockEntity
        return entity.isProvidingWeakPower(fromIntMinecraftSide(side))
    }

    override fun canConnectRedstone(block: IBlockAccess, x: Int, y: Int, z: Int, side: Int): Boolean {
        val entity = block.getTileEntity(x, y, z) as NodeBlockEntity
        return entity.canConnectRedstone(Direction.XN)
    }

    override fun isOpaqueCube(): Boolean {
        return true
    }

    override fun renderAsNormalBlock(): Boolean {
        return false
    }

    override fun getRenderType(): Int {
        return -1
    }

    override fun getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int): Int {
        val entity = world.getTileEntity(x, y, z)
        if (entity == null || entity !is NodeBlockEntity) return 0
        return entity.lightValue
    }

    //client server
    open fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, front: Direction?, entityLiving: EntityLivingBase?, metadata: Int): Boolean {
        val tileEntity = world.getTileEntity(x, y, z) as NodeBlockEntity
        tileEntity.onBlockPlacedBy(front, entityLiving, metadata)
        return true
    }

    //server   
    override fun onBlockAdded(par1World: World, x: Int, y: Int, z: Int) {
        if (!par1World.isRemote) {
            val entity = par1World.getTileEntity(x, y, z) as NodeBlockEntity
            entity.onBlockAdded()
        }
    }

    //server
    override fun breakBlock(par1World: World, x: Int, y: Int, z: Int, par5: Block, par6: Int) {
        run {
            val entity = par1World.getTileEntity(x, y, z) as NodeBlockEntity
            entity.onBreakBlock()
            super.breakBlock(par1World, x, y, z, par5, par6)
        }
    }

    override fun onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, b: Block) {
        if (!isRemote(world)) {
            val entity = world.getTileEntity(x, y, z) as NodeBlockEntity
            entity.onNeighborBlockChange()
        }
    }

    override fun damageDropped(metadata: Int): Int {
        return metadata
    }

    //client server
    override fun onBlockActivated(world: World, x: Int, y: Int, z: Int, entityPlayer: EntityPlayer, side: Int, vx: Float, vy: Float, vz: Float): Boolean {
        val entity = world.getTileEntity(x, y, z) as NodeBlockEntity
        return entity.onBlockActivated(entityPlayer, fromIntMinecraftSide(side), vx, vy, vz)
    }

    override fun hasTileEntity(metadata: Int): Boolean {
        return true
    }

    override fun createTileEntity(var1: World, meta: Int): TileEntity {
        return tileEntityClass.getConstructor().newInstance() as TileEntity
    }

    init {
        setBlockName("NodeBlock")
        this.tileEntityClass = tileEntityClass
        useNeighborBrightness = true
        this.blockItemNbr = blockItemNbr
        setHardness(1.0f)
        setResistance(1.0f)
    }
}
