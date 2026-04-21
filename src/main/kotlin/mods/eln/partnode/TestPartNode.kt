package mods.eln.partnode

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.partnode.PartNode
import mods.eln.node.partnode.PartNodeBlock
import mods.eln.node.partnode.PartNodeDescriptor
import mods.eln.node.partnode.PartNodeEntity
import mods.eln.node.simple.SimpleNode
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.block.material.Material
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class TestPartNodeDescriptor : PartNodeDescriptor(DESCRIPTOR_KEY) {
    companion object {
        const val DESCRIPTOR_KEY = "eln.partnode.test"
    }
}

class TestPartNodeBlock(descriptor: TestPartNodeDescriptor) : PartNodeBlock(Material.rock, descriptor) {
    override fun createNewTileEntity(world: World?, meta: Int): TileEntity {
        return TestPartNodeEntity()
    }

    override fun newNode(): SimpleNode {
        return TestPartNode()
    }
}

class TestPartNode : PartNode() {
    override val nodeUuid: String
        get() = nodeUuidStatic

    override fun initialize() {
        connect()
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? {
        return null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    companion object {
        const val nodeUuidStatic = "eln.partnode.test"
    }
}

class TestPartNodeEntity : PartNodeEntity(TestPartNode.nodeUuidStatic)
