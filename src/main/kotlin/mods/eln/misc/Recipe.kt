package mods.eln.misc

import net.minecraft.item.ItemStack
import java.util.*

class Recipe {
    @JvmField
    var input: ItemStack
    @JvmField
    var output: Array<ItemStack>
    @JvmField
    var energy: Double

    constructor(input: ItemStack, output: Array<ItemStack>, energy: Double) {
        this.input = input
        this.output = output
        this.energy = energy
    }

    constructor(input: ItemStack, output: ItemStack, energy: Double) {
        this.input = input
        this.output = arrayOf(output)
        this.energy = energy
    }

    fun canBeCraftedBy(stack: ItemStack?): Boolean {
        return if (stack == null) false else input.stackSize <= stack.stackSize && Utils.areSame(stack, input)
    }

    val outputCopy: Array<ItemStack?>
        get() {
            val cpy = arrayOfNulls<ItemStack>(output.size)
            for (idx in output.indices) {
                cpy[idx] = output[idx].copy()
            }
            return cpy
        }

    @JvmField
    var machineList = ArrayList<ItemStack>()
    fun setMachineList(machineList: ArrayList<ItemStack>) {
        this.machineList = machineList
    }
}
