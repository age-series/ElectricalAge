package mods.eln.node.partnode

import mods.eln.node.simple.SimpleNodeBlock
import net.minecraft.block.material.Material

abstract class PartNodeBlock(
    material: Material,
    descriptor: PartNodeDescriptor
) : SimpleNodeBlock(material) {
    init {
        setDescriptor(descriptor)
    }
}
