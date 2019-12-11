package mods.eln.item

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.wiki.Data
import net.minecraft.item.Item

open class GenericItemUsingDamageDescriptorUpgrade : GenericItemUsingDamageDescriptor {
    constructor(name: String?) : super(name!!) {}
    constructor(name: String?, iconName: String?) : super(name!!, iconName!!) {}

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addUpgrade(newItemStack())
    }
}
