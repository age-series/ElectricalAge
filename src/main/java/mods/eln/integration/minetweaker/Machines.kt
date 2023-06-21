package mods.eln.integration.minetweaker

import minetweaker.api.item.IIngredient
import minetweaker.api.item.IItemStack
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ZenClass("mods.electricalage.Compressor")
object Compressor {
    @ZenMethod
    fun addRecipe(input: IIngredient, energy: Double, output: Array<IItemStack>) {
        MinetweakerMachine.COMPRESSOR.addRecipe(input, energy, output)
    }
    @ZenMethod
    fun removeRecipe(input: IIngredient) {
        MinetweakerMachine.COMPRESSOR.removeRecipe(input)
    }
}

@ZenClass("mods.electricalage.Macerator")
object Macerator {
    @ZenMethod
    fun addRecipe(input: IIngredient, energy: Double, output: Array<IItemStack>) {
        MinetweakerMachine.MACERATOR.addRecipe(input, energy, output)
    }
    @ZenMethod
    fun removeRecipe(input: IIngredient) {
        MinetweakerMachine.MACERATOR.removeRecipe(input)
    }
}

@ZenClass("mods.electricalage.Magnetizer")
object Magnetizer {
    @ZenMethod
    fun addRecipe(input: IIngredient, energy: Double, output: Array<IItemStack>) {
        MinetweakerMachine.MAGNETIZER.addRecipe(input, energy, output)
    }
    @ZenMethod
    fun removeRecipe(input: IIngredient) {
        MinetweakerMachine.MAGNETIZER.removeRecipe(input)
    }
}

@ZenClass("mods.electricalage.PlateMachine")
object PlateMachine {
    @ZenMethod
    fun addRecipe(input: IIngredient, energy: Double, output: Array<IItemStack>) {
        MinetweakerMachine.PLATEMACHINE.addRecipe(input, energy, output)
    }
    @ZenMethod
    fun removeRecipe(input: IIngredient) {
        MinetweakerMachine.PLATEMACHINE.removeRecipe(input)
    }
}


