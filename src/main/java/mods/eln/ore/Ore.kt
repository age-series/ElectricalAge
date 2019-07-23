package mods.eln.ore

import cpw.mods.fml.common.IWorldGenerator
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import mods.eln.generic.GenericItemBlockUsingDamage
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.world.World
import net.minecraft.world.WorldType
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.gen.feature.WorldGenMinable
import java.util.*

class OreBlock : Block(Material.rock) {
    init {
        setHardness(3.0f)
        setResistance(5.0f)
    }

    override fun damageDropped(i: Int): Int { //Makes sure pick block works right
        return i
    }

    override fun getSubBlocks(i: Item, tab: CreativeTabs?, l: List<*>) { //Puts all sub blocks into the creative inventory
        Eln.oreItem.getSubItems(i, tab, l)
    }

    @SideOnly(Side.CLIENT)
    override fun getIcon(par1: Int, par2: Int): IIcon? {
        val desc = Eln.oreItem.getDescriptor(par2) ?: return null
        return desc.getBlockIconId(par1, par2)
    }

    override fun breakBlock(par1World: World, par2: Int, par3: Int, par4: Int, par5: Block?, par6: Int) {
        super.breakBlock(par1World, par2, par3, par4, par5, par6)
        if (par1World.isRemote) return
    }
}

class OreDescriptor(name: String,
                    internal var metadata: Int,
                    internal var spawnRate: Int,
                    internal var spawnSizeMin: Int,
                    internal var spawnSizeMax: Int,
                    internal var spawnHeightMin: Int,
                    internal var spawnHeightMax: Int
) : GenericItemBlockUsingDamageDescriptor(name), IWorldGenerator {

    fun getBlockIconId(side: Int, damage: Int): IIcon {
        return icon
    }

    fun getBlockDropped(fortune: Int): ArrayList<ItemStack> {
        val list = ArrayList<ItemStack>()
        list.add(ItemStack(Eln.oreItem, 1, metadata))
        return list
    }

    override fun generate(random: Random, chunkX: Int, chunkZ: Int, world: World,
                          chunkGenerator: IChunkProvider, chunkProvider: IChunkProvider) {
        if (world.provider.isSurfaceWorld) {
            generateSurface(random, chunkX * 16, chunkZ * 16, world) //This makes it gen overworld (the *16 is important)
        }
    }

    fun generateSurface(random: Random, x: Int, z: Int, w: World) {
        if (w.worldInfo.terrainType === WorldType.FLAT) return
        //for(int i = 0;i<4;i++){ //This goes through the ore metadata
        for (ii in 0 until spawnRate) { //This makes it gen multiple times in each chunk
            val posX = x + random.nextInt(16) //X coordinate to gen at
            val posY = spawnHeightMin + random.nextInt(spawnHeightMax - spawnHeightMin) //Y coordinate less than 40 to gen at
            val posZ = z + random.nextInt(16) //Z coordinate to gen at
            val size = spawnSizeMin + random.nextInt(spawnSizeMax - spawnSizeMin)
            WorldGenMinable(Eln.oreBlock, metadata, size, Blocks.stone).generate(w, random, posX, posY, posZ) //The gen call
        }
    }
}

class OreItem(b: Block) : GenericItemBlockUsingDamage<OreDescriptor>(b) {

    override fun getMetadata(par1: Int): Int {
        return par1
    }

    override fun addDescriptor(damage: Int, descriptor: OreDescriptor) {
        super.addDescriptor(damage, descriptor)
        GameRegistry.registerWorldGenerator(descriptor, 0)
    }
}
