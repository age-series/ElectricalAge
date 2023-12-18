package mods.eln.ore

import cpw.mods.fml.common.IWorldGenerator
import mods.eln.Eln
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.wiki.Data
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.world.World
import net.minecraft.world.WorldType
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.gen.feature.WorldGenMinable
import java.util.*

class OreDescriptor(
    name: String?, var metadata: Int,
    var spawnRate: Int, var spawnSizeMin: Int, var spawnSizeMax: Int, var spawnHeightMin: Int, var spawnHeightMax: Int
) : GenericItemBlockUsingDamageDescriptor(name), IWorldGenerator {

    fun getBlockIconId(side: Int, damage: Int): IIcon {
        return icon
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addOre(newItemStack())
    }

    fun getBlockDropped(fortune: Int): ArrayList<ItemStack> {
        val list = ArrayList<ItemStack>()
        list.add(ItemStack(Eln.oreItem, 1, metadata))
        return list
    }

    override fun generate(
        random: Random, chunkX: Int, chunkZ: Int, world: World,
        chunkGenerator: IChunkProvider?, chunkProvider: IChunkProvider?
    ) {
        if (world.provider.isSurfaceWorld) {
            generateSurface(
                random,
                chunkX * 16,
                chunkZ * 16,
                world
            ) //This makes it gen overworld (the *16 is important)
        }
    }

    fun generateSurface(random: Random, x: Int, z: Int, w: World) {
        if (w.worldInfo.terrainType === WorldType.FLAT) return
        for (ii in 0 until spawnRate) { //This makes it gen multiple times in each chunk
            val posX = x + random.nextInt(16) //X coordinate to gen at
            val posY =
                spawnHeightMin + random.nextInt(spawnHeightMax - spawnHeightMin) //Y coordinate less than 40 to gen at
            val posZ = z + random.nextInt(16) //Z coordinate to gen at
            val size = spawnSizeMin + random.nextInt(spawnSizeMax - spawnSizeMin)
            WorldGenMinable(Eln.oreBlock, metadata, size, Blocks.stone).generate(
                w,
                random,
                posX,
                posY,
                posZ
            )
        }
    }
}
