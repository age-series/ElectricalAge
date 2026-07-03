package mods.eln.generic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
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
    public static void addEntries(CreativeTabs tab, List<ItemStack> list) {
        for (GenericItemBlockUsingDamage<?> item : BLOCK_ITEMS) {
            item.getSubItems(item, tab, list);
        }
        for (GenericItemUsingDamage<?> item : GENERIC_ITEMS) {
            item.getSubItems(item, tab, list);
        }
        if (tab == Eln.creativeTabPowerElectronics) {
            moveRegulatorChipsAfterDcDcConverters(list);
        }
    }

    private static void moveRegulatorChipsAfterDcDcConverters(List<ItemStack> list) {
        List<ItemStack> regulators = new ArrayList<ItemStack>();

        for (Iterator<ItemStack> iterator = list.iterator(); iterator.hasNext(); ) {
            ItemStack stack = iterator.next();
            if (stack == null) continue;

            if (isRegulatorChip(stack)) {
                regulators.add(stack);
                iterator.remove();
            }
        }

        if (regulators.isEmpty()) return;
        int insertAfter = -1;
        for (int index = 0; index < list.size(); index++) {
            if (isDcDcConverter(list.get(index))) {
                insertAfter = index;
            }
        }
        int insertAt = insertAfter >= 0 ? insertAfter + 1 : list.size();
        list.addAll(insertAt, regulators);
    }

    private static boolean isRegulatorChip(ItemStack stack) {
        return stack.getItem() == Eln.sixNodeItem && (stack.getItemDamage() >> 6) == 5;
    }

    private static boolean isDcDcConverter(ItemStack stack) {
        if (stack.getItem() != Eln.transparentNodeItem) return false;
        int damage = stack.getItemDamage();
        int group = damage >> 6;
        int subId = damage & 63;
        return group == 2 && subId >= 1 && subId <= 7;
    }
}
