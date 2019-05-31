package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import net.minecraft.item.Item;

public class GenericItemUsingDamageDescriptorResource extends GenericItemUsingDamageDescriptor {

    public GenericItemUsingDamageDescriptorResource(String name) {
        super(name);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }
}
