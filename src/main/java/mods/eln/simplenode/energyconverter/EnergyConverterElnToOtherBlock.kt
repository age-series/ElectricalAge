package mods.eln.simplenode.energyconverter

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.misc.Direction
import mods.eln.node.simple.SimpleNode
import mods.eln.node.simple.SimpleNodeBlock
import mods.eln.node.simple.SimpleNodeEntity
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class EnergyConverterElnToOtherBlock(private val descriptor: EnergyConverterElnToOtherDescriptor) : SimpleNodeBlock(Material.packedIce) {

    private var sideIcon: IIcon? = null

    override fun createNewTileEntity(var1: World, var2: Int): TileEntity {
        return EnergyConverterElnToOtherEntity()
    }

    override fun newNode(): SimpleNode {
        return EnergyConverterElnToOtherNode()
    }

    @SideOnly(Side.CLIENT)
    override fun getIcon(w: IBlockAccess, x: Int, y: Int, z: Int, side: Int): IIcon {
        return sideIcon!!
    }

    override fun getIcon(side: Int, meta: Int): IIcon {
        return sideIcon!!
    }

    fun getElnIcon(@Suppress("UNUSED_PARAMETER") side: Int): IIcon? {
        return sideIcon
    }

    @SideOnly(Side.CLIENT)
    override fun registerBlockIcons(register: IIconRegister) {
        sideIcon = register.registerIcon("eln:elntoic2lvu_side")
    }

    init {
        setDescriptor(descriptor)
    }
}
