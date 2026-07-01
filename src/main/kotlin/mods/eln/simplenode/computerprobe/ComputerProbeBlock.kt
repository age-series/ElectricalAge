package mods.eln.simplenode.computerprobe

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.node.simple.SimpleNode
import mods.eln.node.simple.SimpleNodeBlock
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.world.World

class ComputerProbeBlock : SimpleNodeBlock(Material.packedIce) {
    private val icon = arrayOfNulls<IIcon>(6)

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity {
        return ComputerProbeEntity()
    }

    override fun newNode(): SimpleNode {
        return ComputerProbeNode()
    }

    override fun getIcon(side: Int, meta: Int): IIcon? {
        return icon[side]
    }

    @SideOnly(Side.CLIENT)
    override fun registerBlockIcons(register: IIconRegister) {
        icon[4] = register.registerIcon("eln:computerprobe_xn")
        icon[5] = register.registerIcon("eln:computerprobe_xp")
        icon[2] = register.registerIcon("eln:computerprobe_zn")
        icon[3] = register.registerIcon("eln:computerprobe_zp")
        icon[0] = register.registerIcon("eln:computerprobe_yn")
        icon[1] = register.registerIcon("eln:computerprobe_yp")
    }
}
