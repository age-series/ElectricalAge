package mods.eln.node.six

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromIntMinecraftSide
import mods.eln.misc.Utils.generateHeightMap
import mods.eln.misc.Utils.isCreative
import mods.eln.misc.Utils.isRemote
import mods.eln.misc.Utils.println
import mods.eln.misc.Utils.updateAllLightTypes
import mods.eln.misc.Utils.updateSkylight
import mods.eln.node.NodeBase
import mods.eln.node.NodeBlock
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.IIcon
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import java.util.*

class SixNodeBlock  // public static ArrayList<Integer> repertoriedItemStackId = new ArrayList<Integer>();
// private IIcon icon;
(material: Material?, tileEntityClass: Class<*>?) : NodeBlock(material, tileEntityClass!!, 0) {
    override fun getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int, player: EntityPlayer): ItemStack {
        val entity = world.getTileEntity(x, y, z) as SixNodeEntity?
        if (entity != null) {
            val render = entity.elementRenderList[fromIntMinecraftSide(target.sideHit)!!.int]
            if (render != null) {
                return render.sixNodeDescriptor.newItemStack()
            }
        }
        return super.getPickBlock(target, world, x, y, z, player)
    }

    override fun registerBlockIcons(r: IIconRegister) {
        super.registerBlockIcons(r)
        blockIcon = r.registerIcon("eln:air")
    }

    override fun getCollisionBoundingBoxFromPool(par1World: World, par2: Int, par3: Int, par4: Int): AxisAlignedBB? {
        return if (nodeHasCache(par1World, par2, par3, par4) || hasVolume(par1World, par2, par3, par4)) super.getCollisionBoundingBoxFromPool(par1World, par2, par3, par4) else null
    }

    fun hasVolume(world: World, x: Int, y: Int, z: Int): Boolean {
        val entity = getEntity(world, x, y, z) ?: return false
        return entity.hasVolume(world, x, y, z)
    }

    override fun getBlockHardness(world: World, x: Int, y: Int, z: Int): Float {
        return 0.3f
    }

    override fun getDamageValue(world: World?, x: Int, y: Int, z: Int): Int {
        if (world == null) return 0
        val entity = getEntity(world, x, y, z)
        return entity?.getDamageValue(world, x, y, z) ?: 0
    }

    fun getEntity(world: IBlockAccess, x: Int, y: Int, z: Int): SixNodeEntity? {
        val tileEntity = world.getTileEntity(x, y, z)
        if (tileEntity != null && tileEntity is SixNodeEntity) return tileEntity
        println("ASSERTSixNodeEntity getEntity() null")
        return null
    }

    // @SideOnly(Side.CLIENT)
    override fun getSubBlocks(par1: Item, tab: CreativeTabs?, subItems: List<*>?) {
        /*
		 * for (Integer id : repertoriedItemStackId) { subItems.add(new ItemStack(this, 1, id)); }
		 */
        Eln.sixNodeItem.getSubItems(par1, tab, subItems)
    }

    override fun isOpaqueCube(): Boolean {
        return false
    }

    override fun renderAsNormalBlock(): Boolean {
        return true
    }

    override fun getRenderType(): Int {
        return 0
    }

    override fun colorMultiplier(p_149720_1_: IBlockAccess, p_149720_2_: Int, p_149720_3_: Int, p_149720_4_: Int): Int {
        val ent = getEntity(p_149720_1_, p_149720_2_, p_149720_3_, p_149720_4_)
        return if (ent != null && ent.sixNodeCacheBlock !== Blocks.air) {
            ent.sixNodeCacheBlock.colorMultiplier(p_149720_1_, p_149720_2_, p_149720_3_, p_149720_4_)
        } else super.colorMultiplier(p_149720_1_, p_149720_2_, p_149720_3_, p_149720_4_)
    }

    /*
	 * @Override public int getLightOpacity(World world, int x, int y, int z) {
	 *
	 * return 255; }
	 */
    override fun getItemDropped(p_149650_1_: Int, p_149650_2_: Random, p_149650_3_: Int): Item? {
        return null
    }

    override fun quantityDropped(par1Random: Random): Int {
        return 0
    }

    @SideOnly(Side.CLIENT)
    override fun getIcon(w: IBlockAccess, x: Int, y: Int, z: Int, side: Int): IIcon {
        val e = w.getTileEntity(x, y, z) ?: return blockIcon
        val sne = e as SixNodeEntity
        val b = sne.sixNodeCacheBlock
        return if (b === Blocks.air) blockIcon else try {
            b.getIcon(side, sne.sixNodeCacheBlockMeta.toInt())
        } catch (e2: Exception) {
            blockIcon
        }
        // return b.getIcon(w, x, y, z, side);

        // return Blocks.sand.getIcon(p_149673_1_, p_149673_2_, p_149673_3_, p_149673_4_, p_149673_5_);
        // return Blocks.stone.getIcon(w, x, y, z, side);
    }

    override fun isReplaceable(world: IBlockAccess, x: Int, y: Int, z: Int): Boolean {
        return false
    }

    override fun canPlaceBlockOnSide(par1World: World, par2: Int, par3: Int, par4: Int, par5: Int): Boolean {
        /* see canPlaceBlockAt; it needs changing if this method is fixed */
        return true /*
					 * if(par1World.isRemote) return true; SixNodeEntity tileEntity = (SixNodeEntity) par1World.getBlockTileEntity(par2, par3, par4); if(tileEntity == null || (tileEntity instanceof SixNodeEntity) == false) return true; Direction direction = Direction.fromIntMinecraftSide(par5); SixNode node = (SixNode) tileEntity.getNode(); if(node == null) return true; if(node.getSideEnable(direction))return false;
					 *
					 * return true;
					 */
    }

    override fun canPlaceBlockAt(par1World: World, par2: Int, par3: Int, par4: Int): Boolean {
        /* This should probably call canPlaceBlockOnSide with each
		 * appropriate side to see if it can go somewhere.
		 * (cf. BlockLever, BlockTorch, etc)

		 * Currently, canPlaceBlockOnSide returns true and defers
		 * check to other code.  The rest of the sixnode code isn't
		 * expecting blind canPlaceBlockAt to work, so things that
		 * call it (e.g. Rannuncarpus) confuse it terribly and leak
		 * cables and nodepieces.

		 * So for now, make the Rannuncarpus et al ignore it.
		 */
        return false
    }

    override fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, front: Direction?, entityLiving: EntityLivingBase?, metadata: Int): Boolean {
        return true
    }

    /*
     * @Override public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int minecraftSide, float vx, float vy, float vz) { SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
     *
     * return tileEntity.onBlockActivated(entityPlayer, Direction.fromIntMinecraftSide(minecraftSide),vx,vy,vz); }
     */
    override fun removedByPlayer(world: World, entityPlayer: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean): Boolean {
        if (world.isRemote) return false
        val tileEntity = world.getTileEntity(x, y, z) as SixNodeEntity
        val MOP = collisionRayTrace(world, x, y, z, entityPlayer) ?: return false
        val sixNode = tileEntity.node as SixNode? ?: return true
        if (sixNode.sixNodeCacheBlock !== Blocks.air) {
            if (isCreative((entityPlayer as EntityPlayerMP)) == false) {
                val stack = ItemStack(sixNode.sixNodeCacheBlock, 1, sixNode.sixNodeCacheBlockMeta.toInt())
                sixNode.dropItem(stack)
            }
            sixNode.sixNodeCacheBlock = Blocks.air
            val chunk = world.getChunkFromBlockCoords(x, z)
            generateHeightMap(chunk)
            updateSkylight(chunk)
            chunk.generateSkylightMap()
            updateAllLightTypes(world, x, y, z)
            sixNode.needPublish = true
            return false
        }
        if (false == sixNode.playerAskToBreakSubBlock(entityPlayer as EntityPlayerMP, fromIntMinecraftSide(MOP.sideHit)!!)) return false
        @Suppress("DEPRECATION")
        return if (sixNode.ifSideRemain) true else super.removedByPlayer(world, entityPlayer, x, y, z)
    }

    override fun breakBlock(par1World: World, x: Int, y: Int, z: Int, par5: Block, par6: Int) {
        if (!par1World.isRemote) {
            val tileEntity = par1World.getTileEntity(x, y, z) as SixNodeEntity
            val sixNode = tileEntity.node as SixNode? ?: return
            for (direction in Direction.values()) {
                if (sixNode.getSideEnable(direction)) {
                    sixNode.deleteSubBlock(null, direction)
                }
            }
        }
        super.breakBlock(par1World, x, y, z, par5, par6)
    }

    override fun onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, b: Block) {
        val tileEntity = world.getTileEntity(x, y, z) as SixNodeEntity
        val sixNode = tileEntity.node as SixNode? ?: return
        for (direction in Direction.values()) {
            if (sixNode.getSideEnable(direction)) {
                if (!getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                    sixNode.deleteSubBlock(null, direction)
                }
            }
        }
        if (!sixNode.ifSideRemain) {
            world.setBlockToAir(x, y, z)
        } else {
            super.onNeighborBlockChange(world, x, y, z, b)
        }
    }

    var w = 0.0
    var booltemp = BooleanArray(6)
    override fun collisionRayTrace(world: World, x: Int, y: Int, z: Int, start: Vec3, end: Vec3): MovingObjectPosition? {
        if (nodeHasCache(world, x, y, z)) return super.collisionRayTrace(world, x, y, z, start, end)
        val tileEntity = world.getTileEntity(x, y, z) as SixNodeEntity? ?: return null
        if (world.isRemote) {
            booltemp[0] = tileEntity.getSyncronizedSideEnable(Direction.XN)
            booltemp[1] = tileEntity.getSyncronizedSideEnable(Direction.XP)
            booltemp[2] = tileEntity.getSyncronizedSideEnable(Direction.YN)
            booltemp[3] = tileEntity.getSyncronizedSideEnable(Direction.YP)
            booltemp[4] = tileEntity.getSyncronizedSideEnable(Direction.ZN)
            booltemp[5] = tileEntity.getSyncronizedSideEnable(Direction.ZP)
            val entity = getEntity(world, x, y, z)
            if (entity != null) {
                val element = entity.elementRenderList[Direction.YN.int]
                // setBlockBounds(0, 0, 0, 1, 1, 1);
                if (element != null && element.sixNodeDescriptor.hasVolume()) {
                    return MovingObjectPosition(x, y, z, Direction.YN.toSideValue(), Vec3.createVectorHelper(0.5, 0.5, 0.5))
                }
            }
        } else {
            val sixNode = tileEntity.node as SixNode? ?: return null
            booltemp[0] = sixNode.getSideEnable(Direction.XN)
            booltemp[1] = sixNode.getSideEnable(Direction.XP)
            booltemp[2] = sixNode.getSideEnable(Direction.YN)
            booltemp[3] = sixNode.getSideEnable(Direction.YP)
            booltemp[4] = sixNode.getSideEnable(Direction.ZN)
            booltemp[5] = sixNode.getSideEnable(Direction.ZP)
            val entity = getEntity(world, x, y, z)
            if (entity != null) {
                val node: NodeBase? = entity.node
                if (node != null && node is SixNode) {
                    val element = node.sideElementList[Direction.YN.int]
                    if (element != null && element.sixNodeElementDescriptor.hasVolume()) return MovingObjectPosition(x, y, z, Direction.YN.toSideValue(), Vec3.createVectorHelper(0.5, 0.5, 0.5))
                }
            }
        }
        // XN
        if (isIn(x.toDouble(), end.xCoord, start.xCoord) && booltemp[0]) {
            val hitX: Double
            val hitY: Double
            val hitZ: Double
            val ratio: Double
            ratio = (x - start.xCoord) / (end.xCoord - start.xCoord)
            if (ratio <= 1.1) {
                hitX = start.xCoord + ratio * (end.xCoord - start.xCoord)
                hitY = start.yCoord + ratio * (end.yCoord - start.yCoord)
                hitZ = start.zCoord + ratio * (end.zCoord - start.zCoord)
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitZ, z + w, z + 1 - w)) return MovingObjectPosition(x, y, z, Direction.XN.toSideValue(), Vec3.createVectorHelper(hitX, hitY, hitZ))
            }
        }
        // XP
        if (isIn((x + 1).toDouble(), start.xCoord, end.xCoord) && booltemp[1]) {
            val hitX: Double
            val hitY: Double
            val hitZ: Double
            val ratio: Double
            ratio = (x + 1 - start.xCoord) / (end.xCoord - start.xCoord)
            if (ratio <= 1.1) {
                hitX = start.xCoord + ratio * (end.xCoord - start.xCoord)
                hitY = start.yCoord + ratio * (end.yCoord - start.yCoord)
                hitZ = start.zCoord + ratio * (end.zCoord - start.zCoord)
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitZ, z + w, z + 1 - w)) return MovingObjectPosition(x, y, z, Direction.XP.toSideValue(), Vec3.createVectorHelper(hitX, hitY, hitZ))
            }
        }
        // YN
        if (isIn(y.toDouble(), end.yCoord, start.yCoord) && booltemp[2]) {
            val hitX: Double
            val hitY: Double
            val hitZ: Double
            val ratio: Double
            ratio = (y - start.yCoord) / (end.yCoord - start.yCoord)
            if (ratio <= 1.1) {
                hitX = start.xCoord + ratio * (end.xCoord - start.xCoord)
                hitY = start.yCoord + ratio * (end.yCoord - start.yCoord)
                hitZ = start.zCoord + ratio * (end.zCoord - start.zCoord)
                if (isIn(hitX, x + w, x + 1 - w) && isIn(hitZ, z + w, z + 1 - w)) return MovingObjectPosition(x, y, z, Direction.YN.toSideValue(), Vec3.createVectorHelper(hitX, hitY, hitZ))
            }
        }
        // YP
        if (isIn((y + 1).toDouble(), start.yCoord, end.yCoord) && booltemp[3]) {
            val hitX: Double
            val hitY: Double
            val hitZ: Double
            val ratio: Double
            ratio = (y + 1 - start.yCoord) / (end.yCoord - start.yCoord)
            if (ratio <= 1.1) {
                hitX = start.xCoord + ratio * (end.xCoord - start.xCoord)
                hitY = start.yCoord + ratio * (end.yCoord - start.yCoord)
                hitZ = start.zCoord + ratio * (end.zCoord - start.zCoord)
                if (isIn(hitX, x + w, x + 1 - w) && isIn(hitZ, z + w, z + 1 - w)) return MovingObjectPosition(x, y, z, Direction.YP.toSideValue(), Vec3.createVectorHelper(hitX, hitY, hitZ))
            }
        }
        // ZN
        if (isIn(z.toDouble(), end.zCoord, start.zCoord) && booltemp[4]) {
            val hitX: Double
            val hitY: Double
            val hitZ: Double
            val ratio: Double
            ratio = (z - start.zCoord) / (end.zCoord - start.zCoord)
            if (ratio <= 1.1) {
                hitX = start.xCoord + ratio * (end.xCoord - start.xCoord)
                hitY = start.yCoord + ratio * (end.yCoord - start.yCoord)
                hitZ = start.zCoord + ratio * (end.zCoord - start.zCoord)
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitX, x + w, x + 1 - w)) return MovingObjectPosition(x, y, z, Direction.ZN.toSideValue(), Vec3.createVectorHelper(hitX, hitY, hitZ))
            }
        }
        // ZP
        if (isIn((z + 1).toDouble(), start.zCoord, end.zCoord) && booltemp[5]) {
            val hitX: Double
            val hitY: Double
            val hitZ: Double
            val ratio: Double
            ratio = (z + 1 - start.zCoord) / (end.zCoord - start.zCoord)
            if (ratio <= 1.1) {
                hitX = start.xCoord + ratio * (end.xCoord - start.xCoord)
                hitY = start.yCoord + ratio * (end.yCoord - start.yCoord)
                hitZ = start.zCoord + ratio * (end.zCoord - start.zCoord)
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitX, x + w, x + 1 - w)) return MovingObjectPosition(x, y, z, Direction.ZP.toSideValue(), Vec3.createVectorHelper(hitX, hitY, hitZ))
            }
        }
        return null
    }

    fun collisionRayTrace(world: World, x: Int, y: Int, z: Int, entityLiving: EntityPlayer): MovingObjectPosition? {

        // double distanceMax = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();
        val distanceMax = 5.0
        val start = Vec3.createVectorHelper(entityLiving.posX, entityLiving.posY, entityLiving.posZ)
        if (!world.isRemote) start.yCoord += 1.62
        val var5 = entityLiving.getLook(0.5f)
        val end = start.addVector(var5.xCoord * distanceMax, var5.yCoord * distanceMax, var5.zCoord * distanceMax)
        return collisionRayTrace(world, x, y, z, start, end)
    }

    fun getIfOtherBlockIsSolid(world: World, x: Int, y: Int, z: Int, direction: Direction): Boolean {
        val vect = IntArray(3)
        vect[0] = x
        vect[1] = y
        vect[2] = z
        direction.applyTo(vect, 1)
        val block = world.getBlock(vect[0], vect[1], vect[2])
        if (block === Blocks.air) return false
        return if (block.isOpaqueCube) true else false
    }

    fun nodeHasCache(world: IBlockAccess, x: Int, y: Int, z: Int): Boolean {
        if (isRemote(world)) {
            val tileEntity = world.getTileEntity(x, y, z)
            if (tileEntity != null && tileEntity is SixNodeEntity) return tileEntity.sixNodeCacheBlock !== Blocks.air else println("ASSERT B public boolean nodeHasCache(World world, int x, int y, int z) ")
        } else {
            val tileEntity = world.getTileEntity(x, y, z) as SixNodeEntity
            val sixNode = tileEntity.node as SixNode?
            if (sixNode != null) return sixNode.sixNodeCacheBlock !== Blocks.air else println("ASSERT A public boolean nodeHasCache(World world, int x, int y, int z) ")
        }
        return false
    }

    override fun getLightOpacity(w: IBlockAccess, x: Int, y: Int, z: Int): Int {
        val e = w.getTileEntity(x, y, z) ?: return 0
        val sne = e as SixNodeEntity
        val b = sne.sixNodeCacheBlock
        return if (b === Blocks.air) 0 else try {
            b.lightOpacity
        } catch (e2: Exception) {
            255
        }
        // return b.getIcon(w, x, y, z, side);
    }

    val nodeUuid: String
        get() = "s"

    @SideOnly(Side.CLIENT)
    override fun getSelectedBoundingBoxFromPool(w: World, x: Int, y: Int, z: Int): AxisAlignedBB {
        if (hasVolume(w, x, y, z)) return super.getSelectedBoundingBoxFromPool(w, x, y, z)
        val col = collisionRayTrace(w, x, y, z, Minecraft.getMinecraft().thePlayer)
        val h = 0.2
        val hn = 1 - h
        val b = 0.02
        val bn = 1 - 0.02
        if (col != null) {
            // Utils.println(Direction.fromIntMinecraftSide(col.sideHit));
            when (fromIntMinecraftSide(col.sideHit)) {
                Direction.XN -> return AxisAlignedBB.getBoundingBox(x.toDouble() + b, y.toDouble(), z.toDouble(), x.toDouble() + h, y.toDouble() + 1, z.toDouble() + 1)
                Direction.XP -> return AxisAlignedBB.getBoundingBox(x.toDouble() + hn, y.toDouble(), z.toDouble(), x.toDouble() + bn, y.toDouble() + 1, z.toDouble() + 1)
                Direction.YN -> return AxisAlignedBB.getBoundingBox(x.toDouble(), y.toDouble() + b, z.toDouble(), x.toDouble() + 1, y.toDouble() + h, z.toDouble() + 1)
                Direction.YP -> return AxisAlignedBB.getBoundingBox(x.toDouble(), y.toDouble() + hn, z.toDouble(), x.toDouble() + 1, y.toDouble() + bn, z.toDouble() + 1)
                Direction.ZN -> return AxisAlignedBB.getBoundingBox(x.toDouble(), y.toDouble(), z.toDouble() + b, x.toDouble() + 1, y.toDouble() + 1, z.toDouble() + h)
                Direction.ZP -> return AxisAlignedBB.getBoundingBox(x.toDouble(), y.toDouble(), z.toDouble() + hn, x.toDouble() + 1, y.toDouble() + 1, z.toDouble() + bn)
                null -> TODO()
            }
        }
        return AxisAlignedBB.getBoundingBox(0.5, 0.5, 0.5, 0.5, 0.5, 0.5) //super.getSelectedBoundingBoxFromPool(w, x, y, z);
        // return AxisAlignedBB.getBoundingBox((double)p_149633_2_ , (double)p_149633_3_ , (double)p_149633_4_ + this.minZ+0.2, (double)p_149633_2_ + this.maxX, (double)p_149633_3_ + this.maxY, (double)p_149633_4_ + this.maxZ);
        // return super.getSelectedBoundingBoxFromPool(w, x, y, z);
    }

    companion object {
        fun isIn(value: Double, min: Double, max: Double): Boolean {
            return if (value >= min && value <= max) true else false
        }
    }
}
