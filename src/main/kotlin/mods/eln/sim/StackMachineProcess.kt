package mods.eln.sim

import mods.eln.misc.RecipesList
import mods.eln.misc.Utils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class StackMachineProcess(
    var inventory: IInventory?,
    private var inputSlotId: Int,
    outputSlotId: Int,
    outputSlotNbr: Int,
    private var recipesList: RecipesList?,
    val energyProvidedFunction: () -> Double,
    val energyConsumerFunction: (energy: Double) -> Unit
) : IProcess {

    private var outSlotIdList = IntArray(outputSlotNbr)
    var efficiency = 1.0

    private var smeltInProcess = false
    private var energyNeeded = 0.0
    private var energyCounter = 0.0

    init {
        for (idx in 0 until outputSlotNbr) {
            outSlotIdList[idx] = idx + outputSlotId
        }
    }

    override fun process(time: Double) {
        if (!canSmelt() || !smeltInProcess) {
            smeltInit()
        }
        if (smeltInProcess) {

            val energyConsumed = getAvailablePower() * time
            energyCounter += energyConsumed
            energyConsumerFunction(energyConsumed)
            println("consuming power $energyConsumed")
            if (energyCounter > energyNeeded) {
                energyCounter -= energyNeeded
                smeltItem()
                smeltInit()
            }
        }
    }

    private fun getAvailablePower(): Double {
        return energyProvidedFunction() * efficiency
    }

    private fun smeltInit() {
        smeltInProcess = canSmelt()
        if (!smeltInProcess) {
            smeltInProcess = false
            energyNeeded = 1.0
            energyCounter = 0.0
        } else {
            smeltInProcess = true
            energyNeeded = recipesList!!.getRecipe(inventory!!.getStackInSlot(inputSlotId))!!.energy
            energyCounter = 0.0
        }
    }

    /**
     * Returns true if the furnace can smelt an item, i.e. has a source item, destination stack isn't full, etc.
     */
    fun canSmelt(): Boolean {
        return if (inventory!!.getStackInSlot(inputSlotId) == null) {
            false
        } else {
            getSmeltResult() ?: return false
            Utils.canPutStackInInventory(getSmeltResult()!!, inventory!!, outSlotIdList)
        }
    }

    private fun getSmeltResult(): Array<ItemStack>? {
        val recipe = recipesList!!.getRecipe(inventory!!.getStackInSlot(inputSlotId)) ?: return null
        return recipe.output
    }

    /**
     * Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack
     */
    private fun smeltItem() {
        if (canSmelt()) {
            val recipe = recipesList!!.getRecipe(inventory!!.getStackInSlot(inputSlotId))
            Utils.tryPutStackInInventory(recipe!!.outputCopy.requireNoNulls(), inventory!!, outSlotIdList)
            inventory!!.decrStackSize(inputSlotId, recipe.input.stackSize)
        }
    }

    fun getProcessState(): Double {
        if (!smeltInProcess) return 0.0
        var state = energyCounter / energyNeeded
        if (state > 1.0) state = 1.0
        return state
    }
}
