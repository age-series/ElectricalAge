package mods.eln.sim

import mods.eln.misc.Recipe
import mods.eln.misc.RecipesList
import mods.eln.misc.Utils
import mods.eln.sim.mna.component.Resistor
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class ElectricalStackMachineProcess(var inventory: IInventory, internal var inputSlotId: Int, internal var OutputSlotId: Int, internal var outputSlotNbr: Int,
                                    internal var electricalResistor: Resistor, internal var resistorValue: Double, internal var recipesList: RecipesList) : IProcess {

    internal var observer: ElectricalStackMachineProcessObserver? = null
    internal var outSlotIdList: IntArray
    internal var efficiency = 1.0
    internal var speedUp = 1.0

    internal var itemStackInOld: ItemStack? = null

    internal var smeltInProcess = false
    internal var energyNeeded = 0.0
    internal var energyCounter = 0.0

    val power: Double
        get() = electricalResistor.getPower() * efficiency

    val smeltResult: Array<ItemStack>?
        get() {
            return recipesList.getRecipe(inventory.getStackInSlot(inputSlotId)).output ?: return null
        }

    interface ElectricalStackMachineProcessObserver {
        fun done(who: ElectricalStackMachineProcess)
    }

    fun setObserver(observer: ElectricalStackMachineProcessObserver) {
        this.observer = observer
    }

    init {
        outSlotIdList = IntArray(outputSlotNbr)

        for (idx in 0 until outputSlotNbr) {
            outSlotIdList[idx] = idx + OutputSlotId
        }
    }

    fun setEfficiency(efficiency: Double) {
        this.efficiency = efficiency
    }

    fun setSpeedUp(speedUp: Double) {
        this.speedUp = speedUp
        setResistorValue(resistorValue)
    }

    override fun process(time: Double) {
        val itemStackIn = inventory.getStackInSlot(inputSlotId)

        val itemTypeChanged = itemStackIn == null && itemStackInOld != null ||
                itemStackIn != null && itemStackInOld == null ||
                itemStackIn != null && itemStackInOld != null && itemStackIn.unlocalizedName != itemStackInOld!!.unlocalizedName

        if (itemTypeChanged || !smeltCan() || !smeltInProcess) {
            smeltInit()
            itemStackInOld = itemStackIn
        }

        if (smeltInProcess) {
            energyCounter += power * time
            if (energyCounter > energyNeeded) {
                energyCounter -= energyNeeded
                smeltItem()
                smeltInit()
            }
        }
    }

    fun smeltInit() {
        smeltInProcess = smeltCan()
        if (!smeltInProcess) {
            smeltInProcess = false
            energyNeeded = 1.0
            energyCounter = 0.0
            electricalResistor.highImpedance()
        } else {
            smeltInProcess = true
            energyNeeded = recipesList.getRecipe(inventory.getStackInSlot(inputSlotId))!!.energy
            energyCounter = 0.0
            electricalResistor.r = resistorValue / speedUp
        }
    }

    fun setResistorValue(value: Double) {
        resistorValue = value
        if (smeltInProcess) electricalResistor.r = resistorValue / speedUp
    }

    /**
     * Returns true if the furnace can smelt an item, i.e. has a source item, destination stack isn't full, etc.
     */
    fun smeltCan(): Boolean {
        if (inventory.getStackInSlot(inputSlotId) == null) {
            return false
        } else {
            val output = smeltResult ?: return false
            return Utils.canPutStackInInventory(smeltResult!!, inventory, outSlotIdList)
        }
    }

    /**
     * Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack
     */
    fun smeltItem() {
        if (this.smeltCan()) {
            val recipe = recipesList.getRecipe(inventory.getStackInSlot(inputSlotId))
            Utils.tryPutStackInInventory(recipe!!.outputCopy, inventory, outSlotIdList)
            inventory.decrStackSize(inputSlotId, recipe.input.stackSize)
            if (observer != null) observer!!.done(this)
        }
    }

    fun processState(): Double {
        if (!smeltInProcess) return 0.0
        var state = energyCounter / energyNeeded
        if (state > 1.0) state = 1.0
        return state
    }

    fun processStatePerSecond(): Double {
        if (!smeltInProcess) return 0.0
        val power = power + 0.1
        return power / energyNeeded
    }
}
