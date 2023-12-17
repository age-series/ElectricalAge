package mods.eln.simplenode

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.simple.SimpleNode
import mods.eln.node.simple.SimpleNodeBlock
import mods.eln.node.simple.SimpleNodeEntity
import mods.eln.sim.ElectricalLoad
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class ConduitBlock(): SimpleNodeBlock(Material.rock) {
    var icon: IIcon? = null

    override fun createNewTileEntity(worldIn: World?, meta: Int): TileEntity {
        return ConduitEntity()
    }

    override fun newNode(): SimpleNode {
        return ConduitNode()
    }

    override fun registerBlockIcons(reg: IIconRegister?) {
        icon = reg!!.registerIcon("eln:conduit")
    }

    override fun isBlockSolid(worldIn: IBlockAccess?, x: Int, y: Int, z: Int, side: Int): Boolean {
        return true
    }
}

class ConduitNode: SimpleNode() {

    override fun initialize() {
        connect()
    }

    override val nodeUuid: String
        get() = getNodeUuidStatic()

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        return maskConduit
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? {
        return null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int) = null


    companion object {
        fun getNodeUuidStatic(): String {
            return "ElnConduit"
        }
    }
}

class ConduitEntity(): SimpleNodeEntity(ConduitNode.getNodeUuidStatic()) {

}