package mods.eln.lightblock

import mods.eln.misc.Coordinate
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import java.util.*

class LightBlock : BlockContainer(Material.air) {

    override fun collisionRayTrace(world: World, x: Int, y: Int, z: Int, start: Vec3, end: Vec3): MovingObjectPosition? {
        return null
    }

    override fun getCollisionBoundingBoxFromPool(par1World: World, par2: Int, par3: Int, par4: Int): AxisAlignedBB? {
        return null
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

    override fun getItemDropped(p_149650_1_: Int, p_149650_2_: Random, p_149650_3_: Int): Item? {
        return null
    }

    override fun quantityDropped(par1Random: Random): Int {
        return 0
    }

    override fun isReplaceable(access: IBlockAccess, x: Int, y: Int, z: Int): Boolean {
        return true
    }

    override fun getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int): Int {
        return world.getBlockMetadata(x, y, z)
    }

    override fun createNewTileEntity(arg0: World, arg1: Int): TileEntity {
        return LightBlockEntity()
    }

    override fun breakBlock(world: World, x: Int, y: Int, z: Int, arg4: Block, arg5: Int) {
        val coord = Coordinate(x, y, z, world)

        for (o in LightBlockEntity.observers) {
            o.lightBlockDestructor(coord)
        }

        super.breakBlock(world, x, y, z, arg4, arg5)
    }

    override fun getLightOpacity(): Int {
        return 0
    }

}