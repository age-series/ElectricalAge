package mods.eln.generic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of metadata driven items so we can render their descriptors inside secondary creative tabs.
 */
public final class CreativeTabPopulator {

    private static final List<GenericItemBlockUsingDamage<?>> BLOCK_ITEMS = new ArrayList<GenericItemBlockUsingDamage<?>>();
    private static final List<GenericItemUsingDamage<?>> GENERIC_ITEMS = new ArrayList<GenericItemUsingDamage<?>>();

    private CreativeTabPopulator() {
    }

    public static void register(GenericItemBlockUsingDamage<?> item) {
        if (!BLOCK_ITEMS.contains(item)) {
            BLOCK_ITEMS.add(item);
        }
    }

    public static void register(GenericItemUsingDamage<?> item) {
        if (!GENERIC_ITEMS.contains(item)) {
            GENERIC_ITEMS.add(item);
        }
    }

    @SideOnly(Side.CLIENT)
    public static void addEntries(CreativeTabs tab, List list) {
        for (GenericItemBlockUsingDamage<?> item : BLOCK_ITEMS) {
            item.getSubItems(item, tab, list);
        }
        for (GenericItemUsingDamage<?> item : GENERIC_ITEMS) {
            item.getSubItems(item, tab, list);
        }
    }
}
