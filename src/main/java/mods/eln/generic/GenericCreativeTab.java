package mods.eln.generic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class GenericCreativeTab extends CreativeTabs {

    private ItemStack iconStack;

    public GenericCreativeTab(String label, Item item) {
        this(label, new ItemStack(item));
    }

    public GenericCreativeTab(String label, ItemStack stack) {
        super(label);
        setIcon(stack);
    }

    public void setIcon(ItemStack stack) {
        if (stack == null) {
            this.iconStack = null;
        } else {
            this.iconStack = stack.copy();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        return iconStack != null ? iconStack.getItem() : Items.redstone;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getIconItemStack() {
        return iconStack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void displayAllReleventItems(List list) {
        super.displayAllReleventItems(list);
        if (this != Eln.creativeTabOther) {
            CreativeTabPopulator.addEntries(this, list);
        }
    }
}
