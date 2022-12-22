package mods.eln.transparentnode.railroad

import mods.eln.entity.carts.EntityElectricMinecart
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement

abstract class GenericRailroadPowerElement(
    node: TransparentNode?, transparentNodeDescriptor: TransparentNodeDescriptor
): TransparentNodeElement(node, transparentNodeDescriptor) {
    open fun registerCart(cart: EntityElectricMinecart) {}
    open fun deregisterCart(cart: EntityElectricMinecart) {}
}
