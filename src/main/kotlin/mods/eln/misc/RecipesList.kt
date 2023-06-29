@file:Suppress("NAME_SHADOWING")
package mods.eln.misc

import mods.eln.Eln
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.FurnaceRecipes
import java.util.*
import kotlin.collections.ArrayList

class RecipesList {
    val recipes = ArrayList<Recipe>()
    val machines = ArrayList<ItemStack>()
    fun addRecipe(recipe: Recipe) {
        recipes.add(recipe)
        recipe.setMachineList(machines)
    }

    fun addMachine(machine: ItemStack) {
        machines.add(machine)
    }

    fun getRecipe(input: ItemStack?): Recipe? {
        for (r in recipes) {
            if (r.canBeCraftedBy(input)) return r
        }
        return null
    }

    fun getRecipeFromOutput(output: ItemStack?): ArrayList<Recipe> {
        if (output == null) return ArrayList()
        val list = ArrayList<Recipe>()
        for (r in recipes) {
            for (stack in r.outputCopy) {
                if (stack != null) {
                    if (Utils.areSame(stack, output)) {
                        list.add(r)
                        break
                    }
                }
            }
        }
        return list
    }

    companion object {
        val listOfList = ArrayList<RecipesList>()
        @JvmStatic
        fun getGlobalRecipeWithOutput(output: ItemStack): ArrayList<Recipe> {
            var output = output
            output = output.copy()
            output.stackSize = 1
            val list = ArrayList<Recipe>()
            for (recipesList in listOfList) {
                list.addAll(recipesList.getRecipeFromOutput(output))
            }
            val furnaceRecipes = FurnaceRecipes.smelting()
            run {
                val it: Iterator<*> = furnaceRecipes.smeltingList.entries.iterator()
                while (it.hasNext()) {
                    try {
                        val pairs = it.next() as Map.Entry<*, *>
                        var recipe: Recipe // List<Integer>, ItemStack
                        val stack = pairs.value as ItemStack
                        val li = pairs.key as ItemStack
                        if (Utils.areSame(output, stack)) {
                            list.add(Recipe(li.copy(), output, ElectricalFurnaceProcess.energyNeededPerSmelt).also { recipe = it })
                            recipe.setMachineList(Eln.instance.furnaceList)
                        }
                    } catch (e: Exception) {
                        // TODO: handle exception
                    }
                }
            }
            return list
        }

        @JvmStatic
        fun getGlobalRecipeWithInput(input: ItemStack): ArrayList<Recipe> {
            var input = input
            input = input.copy()
            input.stackSize = 64
            val list = ArrayList<Recipe>()
            for (recipesList in listOfList) {
                val r = recipesList.getRecipe(input)
                if (r != null) list.add(r)
            }
            val furnaceRecipes = FurnaceRecipes.smelting()
            val smeltResult = furnaceRecipes.getSmeltingResult(input)
            var smeltRecipe: Recipe
            if (smeltResult != null) {
                try {
                    val input1 = input.copy()
                    input1.stackSize = 1
                    list.add(Recipe(input1, smeltResult, ElectricalFurnaceProcess.energyNeededPerSmelt).also { smeltRecipe = it })
                    smeltRecipe.machineList.addAll(Eln.instance.furnaceList)
                } catch (e: Exception) {
                    // TODO: handle exception
                }
            }
            return list
        }
    }

    init {
        listOfList.add(this)
    }
}
