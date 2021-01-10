package mods.eln.simplenode.energyconverter

import mods.eln.misc.DescriptorBase

class EnergyConverterElnToOtherDescriptor(
    key: String,
    var maxPower: Double
) : DescriptorBase(key) {

    fun applyTo(node: EnergyConverterElnToOtherNode) {
        node.energyBufferMax = maxPower * 2
        node.inPowerMax = maxPower
    }
}
