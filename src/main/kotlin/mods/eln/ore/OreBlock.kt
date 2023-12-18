package mods.eln.ore

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.world.World

class OreBlock : Block(Material.rock) {
    init {
        setHardness(3.0f) //The block hardness
        setResistance(5.0f) //The explosion resistance
    }

    //Makes sure pick block works right
    override fun damageDropped(i: Int): Int {
        return i
    }

    //Puts all sub blocks into the creative inventory
    override fun getSubBlocks(
        i: Item,
        tab: CreativeTabs,
        l: List<*>?
    ) {
        Eln.oreItem.getSubItems(i, tab, l)
    }

    @SideOnly(Side.CLIENT)
    override fun getIcon(par1: Int, par2: Int): IIcon? {
        val desc = Eln.oreItem.getDescriptor(par2) ?: return null
        return desc.getBlockIconId(par1, par2)
    }

    fun getBlockDropped(
        w: World?,
        x: Int,
        y: Int,
        z: Int,
        meta: Int,
        fortune: Int
    ): ArrayList<ItemStack> {
        val desc = Eln.oreItem.getDescriptor(meta) ?: return ArrayList()
        return desc.getBlockDropped(fortune)
    }

    override fun breakBlock(par1World: World, par2: Int, par3: Int, par4: Int, par5: Block, par6: Int) {
        super.breakBlock(par1World, par2, par3, par4, par5, par6)
        if (par1World.isRemote) return
    }
}
