package mods.eln.integration.minetweaker

import minetweaker.IUndoableAction
import minetweaker.MineTweakerAPI
import minetweaker.api.item.IIngredient
import minetweaker.api.item.IItemStack
import minetweaker.api.minecraft.MineTweakerMC
import mods.eln.Eln
import mods.eln.misc.Recipe
import mods.eln.misc.RecipesList
import net.minecraft.item.ItemStack


class MinetweakerMachine(var recipes: RecipesList, var addDesc: String, var removeDesc: String) {
    fun addRecipe(input: IIngredient, energy: Double, output: Array<IItemStack>) {
        if (energy < 0) return
        require(output.size <= 4) { "Too much outputs" }
        val outs = ArrayList<ItemStack>()
        for (i in output) {
            val stack = MineTweakerMC.getItemStack(i)
            if (stack != null) {
                outs.add(stack)
            }
        }
        val recipe = Recipe(MineTweakerMC.getItemStack(input), outs.toArray(arrayOf()), energy)
        MineTweakerAPI.apply(AddRecipe(this, recipe))
    }

    fun removeRecipe(input: IIngredient) {
        MineTweakerAPI.apply(RemoveRecipe(this, input))
    }

    companion object {
        var MACERATOR =
            MinetweakerMachine(Eln.instance.maceratorRecipes, "Adding Macerator Recipe", "Removing Macerator Recipe")
        var COMPRESSOR =
            MinetweakerMachine(Eln.instance.compressorRecipes, "Adding Compressor Recipe", "Removing Compressor Recipe")
        var MAGNETIZER =
            MinetweakerMachine(Eln.instance.magnetiserRecipes, "Adding Magnetizer Recipe", "Removing Magnetizer Recipe")
        var PLATEMACHINE = MinetweakerMachine(
            Eln.instance.plateMachineRecipes,
            "Adding Plate Machine Recipe",
            "Removing Plate Machine Recipe"
        )
    }
}


abstract class BasicUndoableAction(val machine: MinetweakerMachine): IUndoableAction {

    override fun canUndo() = true

    override fun describe() = "Undo ${machine.addDesc}"

    override fun describeUndo() = "Undo ${machine.removeDesc}"

    override fun getOverrideKey() = null
}

class AddRecipe(machine: MinetweakerMachine, val recipe: Recipe): BasicUndoableAction(machine) {
    override fun apply() {
        machine.recipes.addRecipe(recipe)
    }

    override fun undo() {
        machine.recipes.recipes.remove(recipe)
    }
}

class RemoveRecipe(machine: MinetweakerMachine, val input: IIngredient): BasicUndoableAction(machine) {

    private var toRemove: List<Recipe> = listOf()

    override fun apply() {
        when (machine) {
            MinetweakerMachine.COMPRESSOR -> {
                toRemove = Eln.instance.compressorRecipes.recipes.filter {input.matches(MineTweakerMC.getIItemStack(it.input))}
            }
            MinetweakerMachine.MACERATOR -> {
                toRemove = Eln.instance.maceratorRecipes.recipes.filter {input.matches(MineTweakerMC.getIItemStack(it.input))}
            }
            MinetweakerMachine.MAGNETIZER -> {
                toRemove = Eln.instance.magnetiserRecipes.recipes.filter {input.matches(MineTweakerMC.getIItemStack(it.input))}
            }
            MinetweakerMachine.PLATEMACHINE -> {
                toRemove = Eln.instance.plateMachineRecipes.recipes.filter {input.matches(MineTweakerMC.getIItemStack(it.input))}
            }
        }
        machine.recipes.recipes.removeAll(toRemove.toSet())
    }

    override fun undo() {
        machine.recipes.recipes.addAll(toRemove)
    }
}
