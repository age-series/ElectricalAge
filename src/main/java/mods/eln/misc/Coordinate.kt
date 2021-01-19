package mods.eln.misc

import cpw.mods.fml.common.FMLCommonHandler
import mods.eln.node.NodeBlockEntity
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraft.world.World
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.DimensionManager
import kotlin.math.abs

class Coordinate : INBTTReady {
    @JvmField
    var x = 0
    @JvmField
    var y = 0
    @JvmField
    var z = 0
    @JvmField
    var dimension = 0

    constructor() {
        x = 0
        y = 0
        z = 0
        dimension = 0
    }

    constructor(coord: Coordinate) {
        x = coord.x
        y = coord.y
        z = coord.z
        dimension = coord.dimension
    }

    constructor(nbt: NBTTagCompound, str: String) {
        readFromNBT(nbt, str)
    }

    override fun hashCode(): Int {
        return (x + y) * 0x10101010 + z
    }

    fun worldDimension(): Int {
        return dimension
    }

    private var w: World? = null
    fun world(): World {
        return if (w == null) {
            FMLCommonHandler.instance().minecraftServerInstance.worldServerForDimension(worldDimension())
        } else w!!
    }

    constructor(entity: NodeBlockEntity) {
        x = entity.xCoord
        y = entity.yCoord
        z = entity.zCoord
        dimension = entity.worldObj.provider.dimensionId
    }

    constructor(x: Int, y: Int, z: Int, dimention: Int) {
        this.x = x
        this.y = y
        this.z = z
        this.dimension = dimention
    }

    constructor(x: Int, y: Int, z: Int, world: World) {
        this.x = x
        this.y = y
        this.z = z
        dimension = world.provider.dimensionId
        if (world.isRemote) w = world
    }

    constructor(entity: TileEntity) {
        x = entity.xCoord
        y = entity.yCoord
        z = entity.zCoord
        dimension = entity.worldObj.provider.dimensionId
        if (entity.worldObj.isRemote) w = entity.worldObj
    }

    fun newWithOffset(x: Int, y: Int, z: Int): Coordinate {
        return Coordinate(this.x + x, this.y + y, this.z + z, dimension)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Coordinate) return false
        return other.x == x && other.y == y && other.z == z && other.dimension == dimension
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        x = nbt.getInteger(str + "x")
        y = nbt.getInteger(str + "y")
        z = nbt.getInteger(str + "z")
        dimension = nbt.getInteger(str + "d")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setInteger(str + "x", x)
        nbt.setInteger(str + "y", y)
        nbt.setInteger(str + "z", z)
        nbt.setInteger(str + "d", dimension)
    }

    override fun toString(): String {
        return "X : $x Y : $y Z : $z D : $dimension"
    }

    fun move(dir: Direction) {
        when (dir) {
            Direction.XN -> x--
            Direction.XP -> x++
            Direction.YN -> y--
            Direction.YP -> y++
            Direction.ZN -> z--
            Direction.ZP -> z++
        }
    }

    fun moved(direction: Direction): Coordinate {
        val moved = Coordinate(this)
        moved.move(direction)
        return moved
    }

    var block: Block
        get() = world().getBlock(x, y, z)
        set(b) {
            world().setBlock(x, y, z, b)
        }

    fun getAxisAlignedBB(ray: Int): AxisAlignedBB {
        return AxisAlignedBB.getBoundingBox((
            x - ray).toDouble(), (y - ray).toDouble(), (z - ray).toDouble(), (
            x + ray + 1).toDouble(), (y + ray + 1).toDouble(), (z + ray + 1).toDouble())
    }

    fun distanceTo(e: Entity): Double {
        return abs(e.posX - (x + 0.5)) + abs(e.posY - (y + 0.5)) + abs(e.posZ - (z + 0.5))
    }

    val meta: Int
        get() = world().getBlockMetadata(x, y, z)
    val blockExist: Boolean
        get() {
            val w = DimensionManager.getWorld(dimension) ?: return false
            return w.blockExists(x, y, z)
        }
    val worldExist: Boolean
        get() = DimensionManager.getWorld(dimension) != null

    fun copyTo(v: DoubleArray) {
        v[0] = x + 0.5
        v[1] = y + 0.5
        v[2] = z + 0.5
    }

    fun setPosition(vp: DoubleArray) {
        x = vp[0].toInt()
        y = vp[1].toInt()
        z = vp[2].toInt()
    }

    fun setPosition(vp: Vec3) {
        x = vp.xCoord.toInt()
        y = vp.yCoord.toInt()
        z = vp.zCoord.toInt()
    }

    val tileEntity: TileEntity?
        get() = world().getTileEntity(x, y, z)

    fun invalidate() {
        x = -1
        y = -1
        z = -1
        dimension = -5123
    }

    val isValid: Boolean
        get() = dimension != -5123

    fun trueDistanceTo(c: Coordinate): Double {
        val dx = (x - c.x).toLong()
        val dy = (y - c.y).toLong()
        val dz = (z - c.z).toLong()
        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    fun setDimension(dimension: Int) {
        this.dimension = dimension
        w = null
    }

    fun copyFrom(c: Coordinate) {
        x = c.x
        y = c.y
        z = c.z
        dimension = c.dimension
    }

    fun applyTransformation(front: Direction, coordinate: Coordinate) {
        front.rotateFromXN(this)
        x += coordinate.x
        y += coordinate.y
        z += coordinate.z
    }

    fun setWorld(worldObj: World) {
        if (worldObj.isRemote) w = worldObj
        dimension = worldObj.provider.dimensionId
    }

    fun setMetadata(meta: Int) {
        world().setBlockMetadataWithNotify(x, y, z, meta, 0)
    }

    operator fun compareTo(o: Coordinate): Int {
        return when {
            dimension != o.dimension ->
                dimension - o.dimension
            x != o.x ->
                x - o.x
            y != o.y ->
                y - o.y
            z != o.z ->
                z - o.z
            else -> 0
        }
    }

    fun subtract(b: Coordinate): Coordinate {
        return newWithOffset(-b.x, -b.y, -b.z)
    }

    fun negate(): Coordinate {
        return Coordinate(-x, -y, -z, dimension)
    }

    companion object {
        @JvmStatic
        fun getAxisAlignedBB(a: Coordinate, b: Coordinate): AxisAlignedBB {
            return AxisAlignedBB.getBoundingBox(
                a.x.coerceAtMost(b.x).toDouble(), a.y.coerceAtMost(b.y).toDouble(), a.z.coerceAtMost(b.z).toDouble(),
                a.x.coerceAtLeast(b.x) + 1.0, a.y.coerceAtLeast(b.y) + 1.0, a.z.coerceAtLeast(b.z) + 1.0)
        }
    }
}
