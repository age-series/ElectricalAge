package mods.eln.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Eln;
import mods.eln.debug.DP;
import mods.eln.debug.DPType;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Recipe;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.List;

public class RegistryUtils {

    public static void addRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(new ShapedOreRecipe(output, params));
    }

    public static void addShapelessRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(output, params));
    }

    public static ItemStack findItemStack(String name, int stackSize) {
        ItemStack stack = GameRegistry.findItemStack("Eln", name, stackSize);
        if (stack == null) {
            stack = Eln.dictionnaryOreFromMod.get(name);
            stack = Utils.newItemStack(Item.getIdFromItem(stack.getItem()), stackSize, stack.getItemDamage());
        }
        return stack;
    }

    public static String firstExistingOre(String... oreNames) {
        for (String oreName : oreNames) {
            if (OreDictionary.doesOreNameExist(oreName)) {
                return oreName;
            }
        }
        return "";
    }

    public static ItemStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    public static void checkRecipe() {
        for (SixNodeDescriptor d : Eln.sixNodeItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                DP.println(DPType.SIX_NODE, "No recipe for " + d.name);
            }
        }
        for (TransparentNodeDescriptor d : Eln.transparentNodeItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                DP.println(DPType.TRANSPARENT_NODE, "No recipe for "+ d.name);
            }
        }
        for (GenericItemUsingDamageDescriptor d : Eln.sharedItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                DP.println(DPType.OTHER, "No recipe for " + d.name);
            }
        }
        for (GenericItemUsingDamageDescriptor d : Eln.sharedItemStackOne.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                DP.println(DPType.OTHER, "No recipe for " + d.name);
            }
        }
    }

    public static boolean recipeExists(ItemStack stack) {
        if (stack == null)
            return false;
        List list = CraftingManager.getInstance().getRecipeList();
        for (Object o : list) {
            if (o instanceof IRecipe) {
                IRecipe r = (IRecipe) o;
                if (r.getRecipeOutput() == null)
                    continue;
                if (Utils.areSame(stack, r.getRecipeOutput()))
                    return true;
            }
        }
        return false;
    }

    public static void addToOre(String name, ItemStack ore) {
        OreDictionary.registerOre(name, ore);
        Eln.dictionnaryOreFromMod.put(name, ore);
    }

    public static void recipeMaceratorModOre(float f, String inputName, String outputName, int outputCount) {
        if (!OreDictionary.doesOreNameExist(inputName)) {
            LogWrapper.info("No entries for oredict: " + inputName);
            return;
        }
        if (!OreDictionary.doesOreNameExist(outputName)) {
            LogWrapper.info("No entries for oredict: " + outputName);
            return;
        }
        ArrayList<ItemStack> inOres = OreDictionary.getOres(inputName);
        ArrayList<ItemStack> outOres = OreDictionary.getOres(outputName);
        if (inOres.size() == 0) {
            LogWrapper.info("No ores in oredict entry: " + inputName);
        }
        if (outOres.size() == 0) {
            LogWrapper.info("No ores in oredict entry: " + outputName);
            return;
        }
        ItemStack output = outOres.get(0).copy();
        output.stackSize = outputCount;
        LogWrapper.info("Adding mod recipe from " + inputName + " to " + outputName);
        for (ItemStack input : inOres) {
            Eln.maceratorRecipes.addRecipe(new Recipe(input, output, f));
        }
    }
}
