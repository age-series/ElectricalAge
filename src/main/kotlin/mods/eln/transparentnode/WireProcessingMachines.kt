package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.IGuiObject
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkinAndComment
import mods.eln.i18n.I18N.tr
import mods.eln.item.RollerWheelDescriptor
import mods.eln.item.WoundWireBundleDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.misc.BasicContainer
import mods.eln.node.INodeContainer
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementInventory
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.oredict.OreDictionary
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs

enum class WireMachineKind(
    val displayName: String,
    val nominalPowerWatts: Double,
    val metersPerSecond: Double
) {
    ROLLER("Wire Roller", 300.0, 6.0),
    INSULATOR("Wire Insulator", 160.0, 4.0),
    COMBINER("Wire Combiner", 120.0, 5.0)
}

class WireMachineDescriptor(
    name: String,
    val kind: WireMachineKind,
    val obj: Obj3D
) : TransparentNodeDescriptor(name, WireMachineElement::class.java, WireMachineRender::class.java) {

    fun drawModel() {
        when (kind) {
            WireMachineKind.ROLLER -> {
                obj.getPart("main")?.draw()
                obj.getPart("rot1")?.draw()
                obj.getPart("rot2")?.draw()
            }
            WireMachineKind.INSULATOR -> {
                obj.getPart("Cube.002_Cube.013")?.draw()
            }
            WireMachineKind.COMBINER -> {
                obj.getPart("main")?.draw()
                obj.getPart("rot1")?.draw()
                obj.getPart("rot2")?.draw()
            }
        }
    }

    override fun renderItem(type: net.minecraftforge.client.IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        GL11.glPushMatrix()
        if (type == net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY) {
            GL11.glRotatef(15f, 1f, 0f, 0f)
            GL11.glRotatef(225f, 0f, 1f, 0f)
            GL11.glScalef(0.95f, 0.95f, 0.95f)
            objItemScale(obj)
            when (kind) {
                WireMachineKind.ROLLER, WireMachineKind.COMBINER -> GL11.glTranslated(0.0, -0.68, 0.0)
                WireMachineKind.INSULATOR -> GL11.glTranslated(0.0, -0.62, 0.0)
            }
        } else {
            objItemScale(obj)
            when (kind) {
                WireMachineKind.ROLLER, WireMachineKind.COMBINER -> GL11.glTranslated(0.0, -0.42, 0.0)
                WireMachineKind.INSULATOR -> GL11.glTranslated(0.0, -0.36, 0.0)
            }
        }
        drawModel()
        GL11.glPopMatrix()
    }

    override fun handleRenderType(item: ItemStack, type: net.minecraftforge.client.IItemRenderer.ItemRenderType): Boolean = true
    override fun shouldUseRenderHelper(type: net.minecraftforge.client.IItemRenderer.ItemRenderType, item: ItemStack, helper: net.minecraftforge.client.IItemRenderer.ItemRendererHelper) =
        type != net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY
}

private enum class WireMachineNetwork(val id: Byte) {
    SET_OPTION(1),
    SET_LENGTH(2)
}

data class WireMachineOption(
    val name: String,
    val descriptor: UtilityCableDescriptor? = null
)

class WireMachineElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) : TransparentNodeElement(node, descriptor) {
    val machineDescriptor = descriptor as WireMachineDescriptor
    override val inventory = WireMachineInventory(machineDescriptor.kind.slotCount(), 64, this)

    private val powerLoad = NbtElectricalLoad("wireMachinePower")
    private val powerResistor = Resistor(powerLoad, null)
    private val process = WireMachineProcess(this)

    var selectedOption = 0
    var targetLengthMeters = 32
    var progressMeters = 0.0
    var progressTargetMeters = 0.0
    var loadedMaterial: UtilityCableMaterial? = null
    var loadedMassKg = 0.0
    var insulationMetersBuffer = 0.0
    var running = false

    override fun initialize() {
        electricalLoadList.add(powerLoad)
        electricalComponentList.add(powerResistor)
        powerLoad.serialResistance = Eln.getSmallRs()
        powerResistor.resistance = MnaConst.highImpedance
        slowProcessList.add(process)
        connect()
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return if (lrdu == LRDU.Down) powerLoad else null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? = null

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return if (lrdu == LRDU.Down) NodeBase.maskElectricalPower else 0
    }

    override fun multiMeterString(side: Direction): String {
        return Utils.plotUIP(powerResistor.voltage, powerResistor.current)
    }

    override fun thermoMeterString(side: Direction): String = ""

    override fun getWaila(): Map<String, String> {
        val info = linkedMapOf<String, String>()
        info[tr("Mode")] = currentOptions().getOrNull(selectedOption)?.name ?: tr("None")
        info[tr("Target Length")] = Utils.plotValue(targetLengthMeters.toDouble(), "m")
        info[tr("Progress")] = tr("%1$ / %2$", Utils.plotValue(progressMeters, "m"), Utils.plotValue(progressTargetMeters, "m"))
        when (machineDescriptor.kind) {
            WireMachineKind.ROLLER -> {
                info[tr("Loaded Material")] = loadedMaterial?.label ?: tr("None")
                info[tr("Metal Buffer")] = tr("%1$ kg", Utils.plotValue(loadedMassKg))
            }
            WireMachineKind.INSULATOR -> {
                info[tr("Insulation Buffer")] = Utils.plotValue(insulationMetersBuffer, "m")
            }
            WireMachineKind.COMBINER -> {}
        }
        return info
    }

    override fun hasGui() = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return WireMachineContainer(this.node, player, inventory, machineDescriptor.kind)
    }

    override fun inventoryChange(inventory: IInventory?) {
        needPublish()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeByte(machineDescriptor.kind.ordinal)
        stream.writeInt(selectedOption)
        stream.writeInt(targetLengthMeters)
        stream.writeFloat(progressMeters.toFloat())
        stream.writeFloat(progressTargetMeters.toFloat())
        stream.writeBoolean(running)
        stream.writeInt(loadedMaterial?.ordinal ?: -1)
        stream.writeFloat(loadedMassKg.toFloat())
        stream.writeFloat(insulationMetersBuffer.toFloat())
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        return when (val packetType = super.networkUnserialize(stream)) {
            WireMachineNetwork.SET_OPTION.id -> {
                selectedOption = stream.readInt().coerceAtLeast(0)
                needPublish()
                TransparentNodeElement.unserializeNulldId
            }
            WireMachineNetwork.SET_LENGTH.id -> {
                targetLengthMeters = stream.readInt().coerceAtLeast(1)
                needPublish()
                TransparentNodeElement.unserializeNulldId
            }
            else -> packetType
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        selectedOption = nbt.getInteger("selectedOption")
        targetLengthMeters = nbt.getInteger("targetLengthMeters").coerceAtLeast(1)
        progressMeters = nbt.getDouble("progressMeters")
        progressTargetMeters = nbt.getDouble("progressTargetMeters")
        loadedMassKg = nbt.getDouble("loadedMassKg")
        insulationMetersBuffer = nbt.getDouble("insulationMetersBuffer")
        loadedMaterial = nbt.getInteger("loadedMaterial").takeIf { it >= 0 }?.let { UtilityCableMaterial.values()[it] }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setInteger("selectedOption", selectedOption)
        nbt.setInteger("targetLengthMeters", targetLengthMeters)
        nbt.setDouble("progressMeters", progressMeters)
        nbt.setDouble("progressTargetMeters", progressTargetMeters)
        nbt.setDouble("loadedMassKg", loadedMassKg)
        nbt.setDouble("insulationMetersBuffer", insulationMetersBuffer)
        nbt.setInteger("loadedMaterial", loadedMaterial?.ordinal ?: -1)
    }

    private fun currentOptions(): List<WireMachineOption> {
        return when (machineDescriptor.kind) {
            WireMachineKind.ROLLER -> UtilityCableDescriptor.allDescriptors()
                .filter { !it.insulated && !it.melted && it.conductorCount == 1 }
                .sortedBy { it.totalConductorAreaMm2 }
                .distinctBy { it.sizeLabel }
                .map { WireMachineOption("${it.sizeLabel} (${it.metricSizeLabel} mm2)", null) }
            WireMachineKind.INSULATOR -> insulatorSelectedDescriptor()?.let { listOf(WireMachineOption(it.name, it)) } ?: emptyList()
            WireMachineKind.COMBINER -> combinerOptions()
        }
    }

    private fun selectedDescriptor(): UtilityCableDescriptor? {
        if (machineDescriptor.kind == WireMachineKind.INSULATOR) {
            return insulatorSelectedDescriptor()
        }
        if (machineDescriptor.kind == WireMachineKind.COMBINER) {
            return currentOptions().firstOrNull()?.descriptor
        }
        val options = currentOptions()
        if (options.isEmpty()) return null
        selectedOption = selectedOption.coerceIn(0, options.lastIndex)
        if (machineDescriptor.kind != WireMachineKind.ROLLER) {
            return options[selectedOption].descriptor
        }
        val selectedSize = options[selectedOption].name.substringBefore(" (")
        val material = loadedMaterial ?: pendingInputMaterial() ?: return null
        return UtilityCableDescriptor.allDescriptors()
            .filter { !it.insulated && !it.melted && it.conductorCount == 1 }
            .firstOrNull { it.sizeLabel == selectedSize && it.material == material }
    }

    private fun insulatorSelectedDescriptor(): UtilityCableDescriptor? {
        val input = inventory.getStackInSlot(0) ?: return null
        val inputWire = input.asUtilityCableDescriptor()
        if (inputWire != null) {
            if (inputWire.insulated || inputWire.conductorCount != 1) return null
            return UtilityCableDescriptor.allDescriptors()
                .filter { it.insulated && !it.melted && it.conductorCount == 1 }
                .firstOrNull {
                    it.material == inputWire.material &&
                        abs(it.totalConductorAreaMm2 - inputWire.totalConductorAreaMm2) <= 0.001
                }
        }
        val bundle = Eln.instance.woundWireBundleDescriptor
        if (bundle != null && bundle.checkSameItemStack(input)) {
            val material = bundle.getMaterial(input) ?: return null
            val targetLabel = bundle.getTargetLabel(input) ?: return null
            val conductorCount = bundle.getConductorCount(input)
            return UtilityCableDescriptor.allDescriptors()
                .filter { it.insulated && !it.melted && it.conductorCount == conductorCount }
                .firstOrNull { it.material == material && it.sizeLabel == targetLabel }
        }
        return null
    }

    private fun insulatorTargetLengthMeters(): Double {
        val input = inventory.getStackInSlot(0) ?: return 0.0
        val inputWire = input.asUtilityCableDescriptor()
        if (inputWire != null) return inputWire.getRemainingLengthMeters(input)
        val bundle = Eln.instance.woundWireBundleDescriptor
        if (bundle != null && bundle.checkSameItemStack(input)) {
            return bundle.getLengthMeters(input)
        }
        return 0.0
    }

    private fun combinerInputs(): List<ItemStack> {
        return (0 until 5).mapNotNull { inventory.getStackInSlot(it) }
    }

    private fun combinerOptions(): List<WireMachineOption> {
        val inputs = combinerInputs()
        if (inputs.size < 2) return emptyList()
        val wires = inputs.map { it.asUtilityCableDescriptor() ?: return emptyList() }
        val first = wires.first()
        if (!first.insulated || first.conductorCount != 1) return emptyList()
        if (wires.any { !it.insulated || it.conductorCount != 1 }) return emptyList()
        if (wires.any { it.material != first.material }) return emptyList()
        if (wires.any { abs(it.totalConductorAreaMm2 - first.totalConductorAreaMm2) > 0.001 }) return emptyList()
        return UtilityCableDescriptor.allDescriptors()
            .filter { it.insulated && !it.melted && it.conductorCount == wires.size }
            .filter { it.material == first.material }
            .filter { abs((it.totalConductorAreaMm2 / it.conductorCount) - first.totalConductorAreaMm2) <= 0.001 }
            .sortedBy { it.name }
            .map { WireMachineOption(it.name, it) }
    }

    private fun combinerTargetLengthMeters(): Double {
        val inputs = combinerInputs()
        if (inputs.size < 2) return 0.0
        val lengths = inputs.map { it.asUtilityCableDescriptor()?.getRemainingLengthMeters(it) ?: return 0.0 }
        return lengths.minOrNull() ?: 0.0
    }

    private fun activePowerFactor(): Double {
        if (!running) {
            powerResistor.resistance = MnaConst.highImpedance
            return 0.0
        }
        val nominalPower = machineDescriptor.kind.nominalPowerWatts
        powerResistor.resistance = (200.0 * 200.0 / nominalPower).coerceAtLeast(1.0)
        return (powerResistor.power / nominalPower).coerceIn(0.0, 1.0)
    }

    private inner class WireMachineProcess(private val element: WireMachineElement) : IProcess {
        override fun process(time: Double) {
            val option = selectedDescriptor()
            val targetLength = when (machineDescriptor.kind) {
                WireMachineKind.INSULATOR -> insulatorTargetLengthMeters()
                WireMachineKind.COMBINER -> combinerTargetLengthMeters()
                else -> targetLengthMeters.toDouble().coerceAtLeast(1.0)
            }
            if (progressTargetMeters <= 0.0) progressTargetMeters = targetLength

            val canRun = when (machineDescriptor.kind) {
                WireMachineKind.ROLLER -> prepareRoller(option, targetLength)
                WireMachineKind.INSULATOR -> prepareInsulator(option, targetLength)
                WireMachineKind.COMBINER -> prepareCombiner(option, targetLength)
            }
            running = canRun
            val powerFactor = activePowerFactor()
            if (!canRun || powerFactor <= 0.0) {
                progressMeters = 0.0
                progressTargetMeters = targetLength
                return
            }
            progressTargetMeters = targetLength
            progressMeters += time * machineDescriptor.kind.metersPerSecond * powerFactor
            if (progressMeters + 1.0e-6 >= targetLength) {
                when (machineDescriptor.kind) {
                    WireMachineKind.ROLLER -> finishRoller(option!!, targetLength)
                    WireMachineKind.INSULATOR -> finishInsulator(option!!, targetLength)
                    WireMachineKind.COMBINER -> finishCombiner(option!!, targetLength)
                }
                progressMeters = 0.0
                progressTargetMeters = when (machineDescriptor.kind) {
                    WireMachineKind.INSULATOR -> insulatorTargetLengthMeters()
                    WireMachineKind.COMBINER -> combinerTargetLengthMeters()
                    else -> targetLengthMeters.toDouble()
                }
                needPublish()
            }
        }
    }

    private fun prepareRoller(option: UtilityCableDescriptor?, targetLength: Double): Boolean {
        val outputSlot = inventory.getStackInSlot(3)
        if (outputSlot != null) return false
        val descriptor = option ?: return false
        if (!descriptor.checkSameItemStack(descriptor.newItemStack(1)) || descriptor.conductorCount != 1 || descriptor.insulated) return false
        if (!hasRollerWheels()) return false
        absorbMetalInput()
        val material = descriptor.material
        if (loadedMaterial != null && loadedMaterial != material) return false
        val requiredKg = requiredKgForLength(descriptor, targetLength)
        return loadedMassKg + 1.0e-6 >= requiredKg
    }

    private fun finishRoller(descriptor: UtilityCableDescriptor, targetLength: Double) {
        val stack = descriptor.newItemStack(1)
        descriptor.setRemainingLengthMeters(stack, targetLength)
        inventory.setInventorySlotContents(3, stack)
        loadedMassKg = (loadedMassKg - requiredKgForLength(descriptor, targetLength)).coerceAtLeast(0.0)
        if (loadedMassKg <= 1.0e-6) loadedMaterial = null
        inventory.markDirty()
    }

    private fun prepareInsulator(option: UtilityCableDescriptor?, targetLength: Double): Boolean {
        val input = inventory.getStackInSlot(0) ?: return false
        val output = inventory.getStackInSlot(2)
        if (output != null) return false
        val descriptor = option ?: return false
        if (targetLength <= 0.0) return false
        absorbInsulationInput()
        if (insulationMetersBuffer + 1.0e-6 < targetLength) return false

        val inputWire = input.asUtilityCableDescriptor()
        if (inputWire != null) {
            if (inputWire.insulated || inputWire.conductorCount != 1 || descriptor.conductorCount != 1 || !descriptor.insulated) return false
            if (inputWire.material != descriptor.material || abs(inputWire.totalConductorAreaMm2 - descriptor.totalConductorAreaMm2) > 0.001) return false
            return inputWire.getRemainingLengthMeters(input) + 1.0e-6 >= targetLength
        }

        val bundle = Eln.instance.woundWireBundleDescriptor
        if (bundle != null && bundle.checkSameItemStack(input)) {
            if (!descriptor.insulated || descriptor.conductorCount <= 1) return false
            if (bundle.getMaterial(input) != descriptor.material) return false
            if (bundle.getTargetLabel(input) != descriptor.sizeLabel) return false
            return bundle.getLengthMeters(input) + 1.0e-6 >= targetLength
        }
        return false
    }

    private fun finishInsulator(descriptor: UtilityCableDescriptor, targetLength: Double) {
        val input = inventory.getStackInSlot(0) ?: return
        val output = descriptor.newItemStack(1)
        descriptor.setRemainingLengthMeters(output, targetLength)
        inventory.setInventorySlotContents(2, output)
        insulationMetersBuffer = (insulationMetersBuffer - targetLength).coerceAtLeast(0.0)

        val inputWire = input.asUtilityCableDescriptor()
        if (inputWire != null) {
            val remaining = inputWire.getRemainingLengthMeters(input) - targetLength
            if (remaining <= 0.0) {
                inventory.setInventorySlotContents(0, null)
            } else {
                inputWire.setRemainingLengthMeters(input, remaining)
            }
        } else {
            val bundle = Eln.instance.woundWireBundleDescriptor
            if (bundle != null && bundle.checkSameItemStack(input)) {
                val remaining = bundle.getLengthMeters(input) - targetLength
                if (remaining <= 0.0) {
                    inventory.setInventorySlotContents(0, null)
                } else {
                    bundle.setLengthMeters(input, remaining)
                }
            }
        }
        inventory.markDirty()
    }

    private fun prepareCombiner(option: UtilityCableDescriptor?, targetLength: Double): Boolean {
        val descriptor = option ?: return false
        if (targetLength <= 0.0) return false
        if (!descriptor.insulated || descriptor.conductorCount <= 1) return false
        if (inventory.getStackInSlot(5) != null) return false

        val inputs = combinerInputs()
        if (inputs.size != descriptor.conductorCount) return false
        val first = inputs.first().asUtilityCableDescriptor() ?: return false
        if (!first.insulated || first.conductorCount != 1) return false
        if (inputs.any { it.asUtilityCableDescriptor() == null }) return false
        if (inputs.any { it.asUtilityCableDescriptor()!!.material != first.material }) return false
        if (inputs.any { abs(it.asUtilityCableDescriptor()!!.totalConductorAreaMm2 - descriptor.totalConductorAreaMm2 / descriptor.conductorCount) > 0.001 }) return false
        val minimumLength = inputs.minOf { it.asUtilityCableDescriptor()!!.getRemainingLengthMeters(it) }
        return minimumLength + 1.0e-6 >= targetLength
    }

    private fun finishCombiner(descriptor: UtilityCableDescriptor, targetLength: Double) {
        val bundleDescriptor = Eln.instance.woundWireBundleDescriptor ?: return
        for (slot in 0 until 5) {
            val stack = inventory.getStackInSlot(slot) ?: continue
            val cable = stack.asUtilityCableDescriptor() ?: continue
            val remaining = cable.getRemainingLengthMeters(stack) - targetLength
            if (remaining <= 0.0) {
                inventory.setInventorySlotContents(slot, null)
            } else {
                cable.setRemainingLengthMeters(stack, remaining)
            }
        }
        val bundle = bundleDescriptor.createBundleStack(
            descriptor.sizeLabel,
            descriptor.metricSizeLabel,
            descriptor.material,
            descriptor.conductorCount,
            descriptor.totalConductorAreaMm2 / descriptor.conductorCount,
            targetLength
        )
        inventory.setInventorySlotContents(5, bundle)
        inventory.markDirty()
    }

    private fun requiredKgForLength(descriptor: UtilityCableDescriptor, targetLength: Double): Double {
        val baseLength = descriptor.defaultLengthMeters().coerceAtLeast(1.0)
        return (targetLength / baseLength) * 10.0
    }

    private fun absorbMetalInput() {
        val input = inventory.getStackInSlot(0) ?: return
        val material = input.detectIngotMaterial() ?: return
        if (loadedMaterial != null && loadedMaterial != material) return
        loadedMaterial = material
        loadedMassKg += input.stackSize.toDouble()
        inventory.setInventorySlotContents(0, null)
        inventory.markDirty()
    }

    private fun pendingInputMaterial(): UtilityCableMaterial? {
        return inventory.getStackInSlot(0)?.detectIngotMaterial()
    }

    private fun absorbInsulationInput() {
        val input = inventory.getStackInSlot(1) ?: return
        if (!input.matchesOre("itemRubber")) return
        insulationMetersBuffer += input.stackSize * 32.0
        inventory.setInventorySlotContents(1, null)
        inventory.markDirty()
    }

    private fun hasRollerWheels(): Boolean {
        val left = inventory.getStackInSlot(1)
        val right = inventory.getStackInSlot(2)
        return left?.itemDescriptorAs<RollerWheelDescriptor>() != null && right?.itemDescriptorAs<RollerWheelDescriptor>() != null
    }

    private fun ItemStack.matchesOre(name: String): Boolean = OreDictionary.getOreIDs(this).any { OreDictionary.getOreName(it) == name }
    private fun ItemStack.matchesAnyOre(vararg names: String): Boolean = OreDictionary.getOreIDs(this).any { OreDictionary.getOreName(it) in names }
    private fun ItemStack.asUtilityCableDescriptor(): UtilityCableDescriptor? = Eln.sixNodeItem.getDescriptor(this) as? UtilityCableDescriptor
    private inline fun <reified T> ItemStack.itemDescriptorAs(): T? = GenericItemUsingDamageDescriptor.getDescriptor(this) as? T
    private fun ItemStack.detectIngotMaterial(): UtilityCableMaterial? = when {
        matchesOre("ingotCopper") -> UtilityCableMaterial.COPPER
        matchesAnyOre("ingotAluminum", "ingotAluminium") -> UtilityCableMaterial.ALUMINUM
        else -> null
    }
}

class WireMachineRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, descriptor) {
    private val machineDescriptor = descriptor as WireMachineDescriptor
    override val inventory = WireMachineInventory(machineDescriptor.kind.slotCount(), 64, this)

    var selectedOption = 0
    var targetLengthMeters = 32
    var progressMeters = 0f
    var progressTargetMeters = 0f
    private var running = false
    private var loadedMaterialOrdinal = -1
    private var loadedMassKg = 0f
    private var insulationMetersBuffer = 0f

    override fun draw() {
        GL11.glPushMatrix()
        if (machineDescriptor.kind == WireMachineKind.INSULATOR) {
            GL11.glTranslated(-0.5, -0.5, 0.5)
        }
        if (machineDescriptor.kind == WireMachineKind.COMBINER) {
            drawCombinerModel()
        } else {
            machineDescriptor.drawModel()
        }
        if (machineDescriptor.kind == WireMachineKind.INSULATOR) {
            drawInsulationBath()
        }
        GL11.glPopMatrix()
        if (machineDescriptor.kind == WireMachineKind.INSULATOR) {
            emitInsulatorSmoke()
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        stream.readUnsignedByte()
        selectedOption = stream.readInt()
        targetLengthMeters = stream.readInt()
        progressMeters = stream.readFloat()
        progressTargetMeters = stream.readFloat()
        running = stream.readBoolean()
        loadedMaterialOrdinal = stream.readInt()
        loadedMassKg = stream.readFloat()
        insulationMetersBuffer = stream.readFloat()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return WireMachineGui(player, inventory, this)
    }

    fun sendOption(index: Int) = clientSendInt(WireMachineNetwork.SET_OPTION.id, index)
    fun sendLength(length: Int) = clientSendInt(WireMachineNetwork.SET_LENGTH.id, length)

    fun optionName(): String {
        val options = renderOptions()
        if (options.isEmpty()) return tr("None")
        if (machineDescriptor.kind == WireMachineKind.COMBINER) {
            return options.first().name
        }
        return options[selectedOption.coerceIn(0, options.lastIndex)].name
    }

    fun renderOptions(): List<WireMachineOption> {
        return when (machineDescriptor.kind) {
            WireMachineKind.ROLLER -> {
                val descriptors = UtilityCableDescriptor.allDescriptors()
                    .filter { !it.insulated && !it.melted && it.conductorCount == 1 }
                    .sortedBy { it.totalConductorAreaMm2 }
                val material = loadedMaterialOrdinal.takeIf { it >= 0 }?.let { UtilityCableMaterial.values()[it] }
                val filtered = if (material != null) descriptors.filter { it.material == material } else descriptors
                filtered
                    .distinctBy { it.sizeLabel }
                    .map { WireMachineOption("${it.sizeLabel} (${it.metricSizeLabel} mm2)", null) }
            }
            WireMachineKind.INSULATOR -> inventory.getStackInSlot(0)?.let { input ->
                val inputWire = Eln.sixNodeItem.getDescriptor(input) as? UtilityCableDescriptor
                val descriptor = when {
                    inputWire != null -> UtilityCableDescriptor.allDescriptors()
                        .filter { it.insulated && !it.melted && it.conductorCount == 1 }
                        .firstOrNull {
                            !inputWire.insulated &&
                                inputWire.conductorCount == 1 &&
                                it.material == inputWire.material &&
                                abs(it.totalConductorAreaMm2 - inputWire.totalConductorAreaMm2) <= 0.001
                        }
                    Eln.instance.woundWireBundleDescriptor?.checkSameItemStack(input) == true -> {
                        val bundle = Eln.instance.woundWireBundleDescriptor
                        val material = bundle?.getMaterial(input)
                        val targetLabel = bundle?.getTargetLabel(input)
                        val conductorCount = bundle?.getConductorCount(input) ?: 0
                        UtilityCableDescriptor.allDescriptors()
                            .filter { it.insulated && !it.melted && it.conductorCount == conductorCount }
                            .firstOrNull { material != null && targetLabel != null && it.material == material && it.sizeLabel == targetLabel }
                    }
                    else -> null
                }
                descriptor?.let { listOf(WireMachineOption(it.name, it)) } ?: emptyList()
            } ?: emptyList()
            WireMachineKind.COMBINER -> {
                val inputs = (0 until 5).mapNotNull { inventory.getStackInSlot(it) }
                if (inputs.size < 2) {
                    emptyList()
                } else {
                    val wires = inputs.map { Eln.sixNodeItem.getDescriptor(it) as? UtilityCableDescriptor }
                    val first = wires.firstOrNull() ?: return emptyList()
                    if (wires.any { it == null || !it.insulated || it.conductorCount != 1 || it.material != first.material || abs(it.totalConductorAreaMm2 - first.totalConductorAreaMm2) > 0.001 }) {
                        emptyList()
                    } else {
                        UtilityCableDescriptor.allDescriptors()
                            .filter { it.insulated && !it.melted && it.conductorCount == wires.size }
                            .filter { it.material == first.material }
                            .filter { abs((it.totalConductorAreaMm2 / it.conductorCount) - first.totalConductorAreaMm2) <= 0.001 }
                            .sortedBy { it.name }
                            .map { WireMachineOption(it.name, it) }
                    }
                }
            }
        }
    }

    fun progressRatio(): Float = if (progressTargetMeters <= 0f) 0f else (progressMeters / progressTargetMeters).coerceIn(0f, 1f)
    fun statusLine(): String {
        return when (machineDescriptor.kind) {
            WireMachineKind.ROLLER -> {
                val material = loadedMaterialOrdinal.takeIf { it >= 0 }?.let { UtilityCableMaterial.values()[it].label } ?: tr("None")
                tr("Buffer: %1$ kg %2$", Utils.plotValue(loadedMassKg.toDouble()), material)
            }
            WireMachineKind.INSULATOR -> tr("Insulation Buffer: %1$", Utils.plotValue(insulationMetersBuffer.toDouble(), "m"))
            WireMachineKind.COMBINER -> tr("Builds wound bundles for later insulation")
        }
    }

    private fun drawInsulationBath() {
        if (insulationMetersBuffer <= 0f) return
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT or GL11.GL_CURRENT_BIT)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glDisable(GL11.GL_LIGHTING)
        val bathY = 0.40
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glColor4f(0.12f, 0.12f, 0.12f, 0.95f)
        GL11.glVertex3d(0.16, bathY, -0.16)
        GL11.glColor4f(0.15f, 0.15f, 0.15f, 0.95f)
        GL11.glVertex3d(0.84, bathY, -0.16)
        GL11.glColor4f(0.10f, 0.10f, 0.10f, 0.95f)
        GL11.glVertex3d(0.84, bathY, -0.84)
        GL11.glColor4f(0.14f, 0.14f, 0.14f, 0.95f)
        GL11.glVertex3d(0.16, bathY, -0.84)
        GL11.glEnd()
        GL11.glPopAttrib()
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawCombinerModel() {
        val obj = machineDescriptor.obj
        obj.getPart("main")?.draw()
        val angle = if (running) ((System.nanoTime() / 10_000_000L) % 360L).toFloat() else 0f
        obj.getPart("rot1")?.draw(angle, 0f, 0f, 1f)
        obj.getPart("rot2")?.draw()
    }

    private fun emitInsulatorSmoke() {
        if (!running) return
        val world = tileEntity.worldObj ?: return
        if (!world.isRemote) return
        if (world.rand.nextFloat() > 0.08f) return
        val baseX = tileEntity.xCoord + 0.5
        val baseY = tileEntity.yCoord + 0.68
        val baseZ = tileEntity.zCoord + 0.5
        val dx = (world.rand.nextDouble() - 0.5) * 0.18
        val dz = (world.rand.nextDouble() - 0.5) * 0.18
        val dy = world.rand.nextDouble() * 0.05
        world.spawnParticle("smoke", baseX + dx, baseY + dy, baseZ + dz, 0.0, 0.015, 0.0)
    }
}

class WireMachineGui(player: EntityPlayer, inventory: IInventory, private val render: WireMachineRender) :
    GuiContainerEln(WireMachineContainer(null, player, inventory, render.transparentNodeDescriptor.let { it as WireMachineDescriptor }.kind)) {

    private val descriptor get() = render.transparentNodeDescriptor as WireMachineDescriptor
    private lateinit var previous: GuiButtonEln
    private lateinit var next: GuiButtonEln
    private lateinit var lengthField: GuiTextFieldEln

    override fun newHelper(): GuiHelperContainer = GuiHelperContainer(this, 176, 228, 8, 146)

    override fun initGui() {
        super.initGui()
        previous = newGuiButton(8, 48, 20, "<")
        next = newGuiButton(148, 48, 20, ">")
        lengthField = newGuiTextField(8, 82, 64).apply {
            text = render.targetLengthMeters.toString()
            setComment(0, tr("Target length in meters"))
            enabled = descriptor.kind == WireMachineKind.ROLLER
            visible = descriptor.kind == WireMachineKind.ROLLER
        }
        val allowPicker = descriptor.kind == WireMachineKind.ROLLER
        previous.enabled = allowPicker
        previous.visible = allowPicker
        next.enabled = allowPicker
        next.visible = allowPicker
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        val options = render.renderOptions()
        when (obj) {
            previous -> if (options.isNotEmpty()) render.sendOption((render.selectedOption - 1).floorMod(options.size))
            next -> if (options.isNotEmpty()) render.sendOption((render.selectedOption + 1).floorMod(options.size))
        }
    }

    override fun textFieldNewValue(textField: GuiTextFieldEln, value: String) {
        super.textFieldNewValue(textField, value)
        if (textField === lengthField) {
            val length = value.toIntOrNull()?.coerceAtLeast(1) ?: return
            render.sendLength(length)
        }
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        super.postDraw(f, x, y)
        drawString(8, 6, tr(descriptor.kind.displayName))
        if (descriptor.kind == WireMachineKind.INSULATOR) {
            drawString(8, 54, tr("Output: %1$", render.optionName()))
        } else if (descriptor.kind == WireMachineKind.COMBINER) {
            drawString(8, 54, tr("Output: %1$", render.optionName()))
        } else {
            drawString(32, 54, render.optionName())
            drawString(8, 70, tr("Length (m)"))
        }
        drawString(8, 104, render.statusLine())
        val barWidth = 160
        val filled = (barWidth * render.progressRatio()).toInt()
        drawRect(guiLeft + 8, guiTop + 130, guiLeft + 8 + barWidth, guiTop + 138, 0xFF444444.toInt())
        if (filled > 0) {
            drawRect(guiLeft + 8, guiTop + 130, guiLeft + 8 + filled, guiTop + 138, 0xFF2FB34A.toInt())
        }
        drawString(8, 118, tr("Progress: %1$ / %2$", Utils.plotValue(render.progressMeters.toDouble(), "m"), Utils.plotValue(render.progressTargetMeters.toDouble(), "m")))
    }

    private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod
}

class WireMachineContainer(
    override val node: NodeBase?,
    player: EntityPlayer,
    inventory: IInventory,
    kind: WireMachineKind
) : BasicContainer(player, inventory, kind.slots(inventory)), INodeContainer {
    override val refreshRateDivider = 1
}

class WireMachineInventory : TransparentNodeElementInventory {
    constructor(size: Int, stackLimit: Int, element: WireMachineElement?) : super(size, stackLimit, element)
    constructor(size: Int, stackLimit: Int, render: TransparentNodeElementRender?) : super(size, stackLimit, render)
}

private fun WireMachineKind.slotCount(): Int = when (this) {
    WireMachineKind.ROLLER -> 4
    WireMachineKind.INSULATOR -> 3
    WireMachineKind.COMBINER -> 6
}

private fun WireMachineKind.slots(inventory: IInventory): Array<net.minecraft.inventory.Slot> = when (this) {
    WireMachineKind.ROLLER -> arrayOf(
        SlotWithSkinAndComment(inventory, 0, 8, 18, SlotSkin.medium, arrayOf(tr("Metal Ingot Input"))),
        SlotWithSkinAndComment(inventory, 1, 30, 18, SlotSkin.medium, arrayOf(tr("Left Roller Wheel"))),
        SlotWithSkinAndComment(inventory, 2, 52, 18, SlotSkin.medium, arrayOf(tr("Right Roller Wheel"))),
        SlotWithSkinAndComment(inventory, 3, 134, 18, SlotSkin.big, arrayOf(tr("Rolled Bare Wire")))
    )
    WireMachineKind.INSULATOR -> arrayOf(
        SlotWithSkinAndComment(inventory, 0, 8, 18, SlotSkin.medium, arrayOf(tr("Bare Wire or Bundle Input"))),
        SlotWithSkinAndComment(inventory, 1, 30, 18, SlotSkin.medium, arrayOf(tr("Rubber Insulation Input"))),
        SlotWithSkinAndComment(inventory, 2, 134, 18, SlotSkin.big, arrayOf(tr("Insulated Output")))
    )
    WireMachineKind.COMBINER -> arrayOf(
        SlotWithSkinAndComment(inventory, 0, 8, 18, SlotSkin.medium, arrayOf(tr("Input 1"))),
        SlotWithSkinAndComment(inventory, 1, 30, 18, SlotSkin.medium, arrayOf(tr("Input 2"))),
        SlotWithSkinAndComment(inventory, 2, 52, 18, SlotSkin.medium, arrayOf(tr("Input 3"))),
        SlotWithSkinAndComment(inventory, 3, 74, 18, SlotSkin.medium, arrayOf(tr("Input 4"))),
        SlotWithSkinAndComment(inventory, 4, 96, 18, SlotSkin.medium, arrayOf(tr("Input 5"))),
        SlotWithSkinAndComment(inventory, 5, 134, 18, SlotSkin.big, arrayOf(tr("Wound Bundle Output")))
    )
}

private fun WireMachineKind.renderOptions(): List<WireMachineOption> {
    return when (this) {
        WireMachineKind.ROLLER -> UtilityCableDescriptor.allDescriptors()
            .filter { !it.insulated && !it.melted && it.conductorCount == 1 }
            .sortedBy { it.totalConductorAreaMm2 }
            .map { WireMachineOption(it.name, it) }
        WireMachineKind.INSULATOR -> UtilityCableDescriptor.allDescriptors()
            .filter { it.insulated && !it.melted }
            .sortedWith(compareBy<UtilityCableDescriptor>({ it.conductorCount }, { it.totalConductorAreaMm2 }))
            .map { WireMachineOption(it.name, it) }
        WireMachineKind.COMBINER -> UtilityCableDescriptor.allDescriptors()
            .filter { it.insulated && !it.melted && it.conductorCount > 1 }
            .sortedWith(compareBy<UtilityCableDescriptor>({ it.conductorCount }, { it.totalConductorAreaMm2 }))
            .map { WireMachineOption(it.name, it) }
    }
}
