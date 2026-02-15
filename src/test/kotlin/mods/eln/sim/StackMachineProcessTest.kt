package mods.eln.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.misc.Recipe
import mods.eln.misc.RecipesList
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

private class SimpleInventory(size: Int) : IInventory {
    private val stacks = arrayOfNulls<ItemStack>(size)
    override fun getSizeInventory(): Int = stacks.size
    override fun getStackInSlot(slot: Int): ItemStack? = stacks[slot]
    override fun decrStackSize(slot: Int, amount: Int): ItemStack? {
        val stack = stacks[slot] ?: return null
        val removed = stack.copy()
        removed.stackSize = amount.coerceAtMost(stack.stackSize)
        stack.stackSize -= removed.stackSize
        if (stack.stackSize <= 0) stacks[slot] = null
        return removed
    }

    override fun getStackInSlotOnClosing(slot: Int): ItemStack? = stacks[slot]
    override fun setInventorySlotContents(slot: Int, stack: ItemStack?) {
        stacks[slot] = stack
    }

    override fun getInventoryName(): String = "inv"
    override fun hasCustomInventoryName(): Boolean = false
    override fun getInventoryStackLimit(): Int = 64
    override fun markDirty() {}
    override fun isUseableByPlayer(player: net.minecraft.entity.player.EntityPlayer?): Boolean = true
    override fun openInventory() {}
    override fun closeInventory() {}
    override fun isItemValidForSlot(slot: Int, stack: ItemStack?): Boolean = true
}

class StackMachineProcessTest {
    @Test
    fun processSmeltsWhenEnergyAvailable() {
        val inventory = SimpleInventory(3)
        val inputItem = Item()
        val outputItem = Item()
        val input = ItemStack(inputItem, 1)
        inventory.setInventorySlotContents(0, input)

        val recipes = RecipesList()
        val output = ItemStack(outputItem, 1)
        recipes.addRecipe(Recipe(input.copy(), output.copy(), 5.0))

        val process = StackMachineProcess(
            inventory,
            inputSlotId = 0,
            outputSlotId = 1,
            outputSlotNbr = 2,
            recipesList = recipes,
            energyProvidedFunction = { 10.0 },
            energyConsumerFunction = {}
        )

        process.process(1.0)

        assertTrue(inventory.getStackInSlot(0) == null)
        assertEquals(1, inventory.getStackInSlot(1)!!.stackSize)
    }

    @Test
    fun getProcessStateReflectsProgress() {
        val inventory = SimpleInventory(2)
        val inputItem = Item()
        val outputItem = Item()
        val input = ItemStack(inputItem, 1)
        inventory.setInventorySlotContents(0, input)

        val recipes = RecipesList()
        val output = ItemStack(outputItem, 1)
        recipes.addRecipe(Recipe(input.copy(), output.copy(), 10.0))

        val process = StackMachineProcess(
            inventory,
            inputSlotId = 0,
            outputSlotId = 1,
            outputSlotNbr = 1,
            recipesList = recipes,
            energyProvidedFunction = { 2.0 },
            energyConsumerFunction = {}
        )

        process.process(1.0)

        val state = process.getProcessState()
        assertTrue(state > 0.0 && state < 1.0)
    }
}
