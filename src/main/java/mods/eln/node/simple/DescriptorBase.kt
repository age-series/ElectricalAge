package mods.eln.node.simple

import mods.eln.node.simple.DescriptorManager.put

open class DescriptorBase(var descriptorKey: String) {
    init {
        put(descriptorKey, this)
    }
}
