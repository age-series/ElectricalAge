package mods.eln.ore

import cpw.mods.fml.common.registry.GameRegistry
import mods.eln.generic.GenericItemBlockUsingDamage
import net.minecraft.block.Block

class OreItem(b: Block?) : GenericItemBlockUsingDamage<OreDescriptor?>(b) {
    override fun getMetadata(par1: Int): Int {
        return par1
    }

    override fun addDescriptor(damage: Int, descriptor: OreDescriptor?) {
        super.addDescriptor(damage, descriptor)
        GameRegistry.registerWorldGenerator(descriptor, 0)
    }
}
