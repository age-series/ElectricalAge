package mods.eln.block

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.Eln
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.util.IIcon
import net.minecraft.item.ItemBlock

class ArcClayBlock : Block(Material.rock) {
    private var icon: IIcon? = null

    init {
        setBlockName(name)
        setBlockTextureName("eln:$name")
        setCreativeTab(Eln.creativeTab)
    }

    @SideOnly(Side.CLIENT)
    override fun registerBlockIcons(iconRegister: IIconRegister) {
        icon = iconRegister.registerIcon("eln:$name")
    }

    @SideOnly(Side.CLIENT)
    override fun getIcon(side: Int, damage: Int): IIcon {
        return icon!!
    }

    companion object {
        private const val name = "arc_clay_block"
    }
}

class ArcMetalBlock : Block(Material.rock) {
    private var icon: IIcon? = null

    init {
        setBlockName(name)
        setBlockTextureName("eln:$name")
        setCreativeTab(Eln.creativeTab)
    }

    @SideOnly(Side.CLIENT)
    override fun registerBlockIcons(iconRegister: IIconRegister) {
        icon = iconRegister.registerIcon("eln:$name")
    }

    @SideOnly(Side.CLIENT)
    override fun getIcon(side: Int, damage: Int): IIcon {
        return icon!!
    }

    companion object {
        private const val name = "arc_metal_block"
    }
}

class ArcMetalItemBlock(block: Block?) : ItemBlock(block)

class ArcClayItemBlock(block: Block?) : ItemBlock(block)
