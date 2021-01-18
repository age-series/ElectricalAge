package mods.eln.node.simple

import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromIntMinecraftSide
import mods.eln.misc.Utils.entityLivingViewDirection
import mods.eln.misc.Utils.isRemote
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.world.World

abstract class SimpleNodeBlock protected constructor(material: Material?) : BlockContainer(material) {
    var descriptorKey: String? = null
    fun setDescriptorKey(descriptorKey: String?): SimpleNodeBlock {
        this.descriptorKey = descriptorKey
        return this
    }

    fun setDescriptor(descriptor: DescriptorBase): SimpleNodeBlock {
        descriptorKey = descriptor.descriptorKey
        return this
    }

    fun getFrontForPlacement(e: EntityLivingBase?): Direction {
        return entityLivingViewDirection(e!!).inverse
    }

    abstract fun newNode(): SimpleNode?

    fun getNode(world: World, x: Int, y: Int, z: Int): SimpleNode? {
        val entity = world.getTileEntity(x, y, z) as SimpleNodeEntity?
        return entity?.node
    }

    fun getEntity(world: World, x: Int, y: Int, z: Int): SimpleNodeEntity {
        return world.getTileEntity(x, y, z) as SimpleNodeEntity
    }

    override fun removedByPlayer(world: World, entityPlayer: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean): Boolean {
        if (!world.isRemote) {
            val node = getNode(world, x, y, z)
            if (node != null) {
                node.removedByPlayer = entityPlayer as EntityPlayerMP
            }
        }
        return super.removedByPlayer(world, entityPlayer, x, y, z, willHarvest)
    }

    // server
    override fun onBlockAdded(par1World: World, x: Int, y: Int, z: Int) {
        if (!par1World.isRemote) {
            val entity = par1World.getTileEntity(x, y, z) as SimpleNodeEntity
            entity.onBlockAdded()
        }
    }

    // server
    override fun breakBlock(par1World: World, x: Int, y: Int, z: Int, par5: Block, par6: Int) {
        val entity = par1World.getTileEntity(x, y, z) as SimpleNodeEntity
        entity.onBreakBlock()
        super.breakBlock(par1World, x, y, z, par5, par6)
    }

    override fun onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, b: Block) {
        if (!isRemote(world)) {
            val entity = world.getTileEntity(x, y, z) as SimpleNodeEntity
            entity.onNeighborBlockChange()
        }
    }

    // client server
    override fun onBlockActivated(world: World, x: Int, y: Int, z: Int, entityPlayer: EntityPlayer, side: Int, vx: Float, vy: Float, vz: Float): Boolean {
        val entity = world.getTileEntity(x, y, z) as SimpleNodeEntity
        return entity.onBlockActivated(entityPlayer, fromIntMinecraftSide(side), vx, vy, vz)
    }
}
