package mods.eln.sixnode.electricalcable

import mods.eln.Eln
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.cable.CableRenderTypeMethodType
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.BrushDescriptor
import mods.eln.item.ItemMovingHelper
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.LRDUMask
import mods.eln.misc.Utils
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.Utils.isPlayerUsingWrench
import mods.eln.misc.Utils.plotAmpere
import mods.eln.misc.Utils.plotVolt
import mods.eln.misc.Utils.renderSubSystemWaila
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.misc.UtilsClient.bindTexture
import mods.eln.misc.UtilsClient.disableBlend
import mods.eln.misc.UtilsClient.disableLight
import mods.eln.misc.UtilsClient.enableBlend
import mods.eln.misc.UtilsClient.enableLight
import mods.eln.node.NodeBase
import mods.eln.node.NodeConnection
import mods.eln.node.six.*
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.DamageSource
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

enum class UtilityCableMaterial(val label: String, val meltingPointCelsius: Double) {
    COPPER("Copper", 1085.0),
    ALUMINUM("Aluminum", 660.0)
}

data class UtilityCablePalette(
    val id: String,
    val displayName: String,
    val conductorColors: IntArray
)

/**
 * This is primarily used with AutoAcceptInventoryProxy and the Config Copy Tool for determining whether to trim length
 * from an existing cable spool, accept the spool directly, or do nothing when implementing right-click-to-insert behavior.
 */
interface IUtilityCableInventory {
    val requiredCableLength: Double

    companion object {
        const val DEFAULT_REQUIRED_LENGTH = 1.0
    }
}

/**
 * This is used with the Config Copy Tool for shuffling cable spools around between inventories.
 */
class UtilityCableItemMovingHelper(val cableDesc: UtilityCableDescriptor, val cableLength: Double) : ItemMovingHelper() {

    override fun acceptsStack(stack: ItemStack?): Boolean {
        return if (cableDesc.checkSameItemStack(stack)) Utils.getItemObject(stack) is UtilityCableDescriptor
        else false
    }

    override fun newStackOfSize(items: Int): ItemStack {
        val newItemStack = cableDesc.newItemStack(items)

        val newItemDesc = Utils.getItemObject(newItemStack) as? UtilityCableDescriptor
        newItemDesc?.setRemainingLengthMeters(newItemStack, cableLength)

        return newItemStack
    }

    companion object {
        @JvmStatic
        fun trimCable(srcItemStack: ItemStack, dstInventory: IInventory, dstIndex: Int): Boolean {
            val srcCableDesc = Utils.getItemObject(srcItemStack)

            // This check MUST be performed to prevent NPEs from calling this function on a non-utility cable item!
            if (srcCableDesc is UtilityCableDescriptor) {
                val dstItemStack = srcCableDesc.newItemStack()

                val existingCableLength = srcCableDesc.getRemainingLengthMeters(srcItemStack)
                val requiredCableLength =
                    if (dstInventory is IUtilityCableInventory) dstInventory.requiredCableLength
                    else IUtilityCableInventory.DEFAULT_REQUIRED_LENGTH

                if (existingCableLength >= requiredCableLength) {
                    srcCableDesc.setRemainingLengthMeters(dstItemStack, requiredCableLength)
                    srcCableDesc.setRemainingLengthMeters(srcItemStack, existingCableLength - requiredCableLength)
                    if (srcCableDesc.getRemainingLengthMeters(srcItemStack) <= 0.0) srcItemStack.stackSize -= 1

                    dstInventory.setInventorySlotContents(dstIndex, dstItemStack)
                    dstInventory.markDirty()
                    return true
                }
            }

            return false
        }
    }

}

class UtilityCableDescriptor(
    name: String,
    render: CableRenderDescriptor,
    description: String,
    @JvmField val sizeLabel: String,
    @JvmField val metricSizeLabel: String,
    @JvmField val material: UtilityCableMaterial,
    @JvmField val totalConductorAreaMm2: Double,
    @JvmField val conductorCount: Int,
    @JvmField val insulated: Boolean,
    @JvmField val insulationVoltageRating: Double,
    @JvmField val melted: Boolean,
    @JvmField val meltTemperatureCelsius: Double,
    @JvmField val flatStyle: Boolean,
    @JvmField val colorPalettes: Array<UtilityCablePalette>,
    @JvmField val poleEligible: Boolean
) : ElectricalCableDescriptor(name, render, description, false) {

    companion object {
        private const val creativeLengthMeters = 128.0
        private const val defaultMaterialMassKg = 10.0
        private const val placeLengthMeters = 1.0
        private const val nbtLengthMeters = "utilityLengthMeters"
        private const val nbtUniqueId = "utilityLengthUid"
        private val registry = mutableListOf<UtilityCableDescriptor>()

        @JvmStatic
        fun allDescriptors(): List<UtilityCableDescriptor> = registry

        @JvmStatic
        fun isGroundColorCode(color: Int): Boolean {
            return when (color and 0xF) {
                2, 5, 13 -> true
                else -> false
            }
        }

        @JvmStatic
        fun isNeutralColorCode(color: Int): Boolean {
            return when (color and 0xF) {
                7, 11, 15 -> true
                else -> false
            }
        }
    }

    @JvmField
    var meltedDescriptor: UtilityCableDescriptor? = null

    @JvmField
    var moltenPileDescriptor: MoltenMetalPileDescriptor? = null

    val supportsPaletteSelection: Boolean
        get() = colorPalettes.size > 1 && !melted && conductorCount > 1

    val actsAsSingleConductor: Boolean
        get() = melted || conductorCount <= 1

    init {
        registry.add(this)
    }

    private fun densityKgPerCubicMeter(): Double {
        return when (material) {
            UtilityCableMaterial.COPPER -> 8960.0
            UtilityCableMaterial.ALUMINUM -> 2700.0
        }
    }

    fun defaultLengthMeters(): Double {
        val conductorAreaSquareMeters = totalConductorAreaMm2 * 1.0e-6
        if (conductorAreaSquareMeters <= 0.0) return 0.0
        val rawLength = defaultMaterialMassKg / densityKgPerCubicMeter() / conductorAreaSquareMeters
        return floor(rawLength).coerceAtLeast(1.0)
    }

    private fun getOrCreateNbt(stack: ItemStack): NBTTagCompound {
        if (stack.tagCompound == null) {
            stack.tagCompound = getDefaultNBT()
        }
        return stack.tagCompound
    }

    fun getRemainingLengthMeters(stack: ItemStack): Double {
        val nbt = getOrCreateNbt(stack)
        return if (nbt.hasKey(nbtLengthMeters)) {
            nbt.getDouble(nbtLengthMeters).coerceAtLeast(0.0)
        } else {
            defaultLengthMeters()
        }
    }

    fun setRemainingLengthMeters(stack: ItemStack, meters: Double) {
        getOrCreateNbt(stack).setDouble(nbtLengthMeters, meters.coerceAtLeast(0.0))
    }

    fun hasLengthForPlacement(stack: ItemStack): Boolean {
        return getRemainingLengthMeters(stack) >= placeLengthMeters
    }

    fun consumeLengthForPlacement(stack: ItemStack) {
        val nbt = getOrCreateNbt(stack)
        val remaining = (getRemainingLengthMeters(stack) - placeLengthMeters).coerceAtLeast(0.0)
        nbt.setDouble(nbtLengthMeters, remaining)
    }

    fun placementLengthMeters(): Double = placeLengthMeters

    override fun getDefaultNBT(): NBTTagCompound {
        return NBTTagCompound().apply {
            setDouble(nbtLengthMeters, defaultLengthMeters())
            setString(nbtUniqueId, UUID.randomUUID().toString())
        }
    }

    override fun newCreativeTabStack(): ItemStack {
        return super.newCreativeTabStack().also { setRemainingLengthMeters(it, creativeLengthMeters) }
    }

    override fun getItemStackLimit(stack: ItemStack): Int = 1

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Conductor: %1$ %2$ (%3$ mm2)", material.label, sizeLabel, metricSizeLabel))
        list.add(tr("Conductors: %1$", conductorCount))
        list.add(tr("Insulation: %1$", if (insulated) tr("%1\$V", Utils.plotValue(insulationVoltageRating)) else tr("Bare")))
        list.add(tr("Max temperature: %1\$C", Utils.plotValue(meltTemperatureCelsius)))
        list.add(tr("Remaining length: %1$ m", Utils.plotValue(getRemainingLengthMeters(itemStack))))
        list.add(tr("Placement usage: %1$ m", Utils.plotValue(placeLengthMeters)))
        if (supportsPaletteSelection) {
            list.add(tr("Wrench: cycle conductor colors"))
        } else if (actsAsSingleConductor && insulated) {
            list.add(tr("Brush: paint conductor color"))
        }
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        super.renderItem(type, item, *data)
        if (type != ItemRenderType.INVENTORY) return

        val font = Minecraft.getMinecraft().fontRenderer
        val overlay = compactLengthLabel(item)
        val scale = 0.5f
        val scaledWidth = font.getStringWidth(overlay) * scale
        val x = ((16f - scaledWidth) / scale).toInt()
        val y = 24

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
        RenderHelper.disableStandardItemLighting()
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glPushMatrix()
        GL11.glScalef(scale, scale, 1f)
        font.drawStringWithShadow(overlay, x, y, 0xFFFFFF)
        GL11.glPopMatrix()
        GL11.glPopAttrib()
    }

    private fun compactLengthLabel(stack: ItemStack): String {
        val meters = getRemainingLengthMeters(stack)
        return when {
            meters >= 1000.0 -> {
                val kilometers = meters / 1000.0
                if (kilometers >= 10.0) "${kilometers.toInt()}k" else String.format("%.1fk", kilometers)
            }
            meters >= 100.0 -> "${meters.toInt()}m"
            meters >= 10.0 -> "${meters.toInt()}m"
            else -> String.format("%.1fm", meters)
        }
    }
}

class UtilityCableElement(
    sixNode: SixNode?,
    side: Direction?,
    descriptor: SixNodeDescriptor
) : SixNodeElement(sixNode!!, side!!, descriptor) {

    @JvmField
    val descriptor = descriptor as UtilityCableDescriptor

    private var singleColor = 0
    private var colorCare = 1
    private var paletteIndex = 0
    private var conductorsBound = false
    private var shockCooldown = 0.0
    private var lastPublishedTemperatureCelsius = Double.NaN

    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val conductorLoads: Array<NbtElectricalLoad>
    private val heaters: List<ElectricalLoadHeatThermalLoad>
    private val breakdownConnections = mutableListOf<ElectricalConnection>()

    init {
        conductorsBound = this.descriptor.actsAsSingleConductor
        conductorLoads = Array(if (this.descriptor.actsAsSingleConductor) 1 else this.descriptor.conductorCount) { idx ->
            NbtElectricalLoad("conductor$idx").also {
                it.setCanBeSimplifiedByLine(true)
                electricalLoadList.add(it)
            }
        }
        heaters = conductorLoads.map { ElectricalLoadHeatThermalLoad(it, thermalLoad).also { heater ->
            heater.limitTemperatureRate(this.descriptor.thermalSelfHeatingRateLimit)
            thermalSlowProcessList.add(heater)
        } }
        thermalLoadList.add(thermalLoad)
        thermalLoad.setAsSlow()
        slowProcessList.add(UtilityCableFailureProcess())
    }

    override fun initialize() {
        descriptor.applyTo(thermalLoad)
        conductorLoads.forEach { descriptor.applyTo(it) }
        if (conductorsBound && conductorLoads.size > 1) {
            bindConductors(force = true)
        }
    }

    override fun destroy(entityPlayer: EntityPlayerMP?) {
        if (useUuid()) {
            stop(getUuid())
        }
        if (sixNodeElementDescriptor.hasGhostGroup()) {
            Eln.ghostManager.removeObserver(sixNode!!.coordinate)
            sixNodeElementDescriptor.getGhostGroup(side, front)!!.erase(sixNode!!.coordinate)
        }
        sixNode!!.dropInventory(inventory)
        if (Utils.mustDropItem(entityPlayer)) {
            val scrapDescriptor = Eln.instance.wireScrapDescriptor
            val dropStack = scrapDescriptor?.createScrapStack(descriptor) ?: dropItemStack
            sixNode!!.dropItem(dropStack)
        }
    }

    override fun disconnectJob() {
        super.disconnectJob()
        breakdownConnections.clear()
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val b = nbt.getByte("color")
        singleColor = b.toInt() and 0xF
        colorCare = b.toInt() shr 4 and 1
        paletteIndex = nbt.getByte("palette").toInt().coerceAtLeast(0)
        conductorsBound = nbt.getBoolean("bound") || descriptor.actsAsSingleConductor
        shockCooldown = nbt.getDouble("shockCooldown")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setByte("color", (singleColor + (colorCare shl 4)).toByte())
        nbt.setByte("palette", paletteIndex.toByte())
        nbt.setBoolean("bound", conductorsBound)
        nbt.setDouble("shockCooldown", shockCooldown)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad {
        if (descriptor.actsAsSingleConductor || conductorsBound) {
            return conductorLoads[0]
        }
        if ((mask and NodeBase.maskColorCareData) == 0 && (mask and NodeBase.maskColorData) == 0) {
            return conductorLoads[defaultConductorIndex()]
        }
        val requestedColor = mask shr NodeBase.maskColorShift and 0xF
        val colors = activePalette().conductorColors
        val idx = colors.indexOf(requestedColor).takeIf { it >= 0 } ?: 0
        return conductorLoads[idx.coerceIn(0, conductorLoads.lastIndex)]
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad {
        return thermalLoad
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        if (!descriptor.insulated) {
            return descriptor.nodeMask
        }
        return if (descriptor.actsAsSingleConductor) {
            descriptor.nodeMask + (singleColor shl NodeBase.maskColorShift) + (colorCare shl NodeBase.maskColorCareShift)
        } else {
            descriptor.nodeMask
        }
    }

    override fun newConnectionAt(connection: NodeConnection?, isA: Boolean) {
        if (!isA || descriptor.actsAsSingleConductor || conductorsBound || connection == null) return
        val other = connection.N2
        if (other is SixNode) {
            val el = other.getElement(connection.dir2.applyLRDU(connection.lrdu2))
            if (el is UtilityCableElement && !el.descriptor.actsAsSingleConductor && !el.conductorsBound) {
                val thisColors = activePalette().conductorColors
                val otherColors = el.activePalette().conductorColors
                val primaryThis = defaultConductorIndex()
                val primaryOther = el.defaultConductorIndex()
                Utils.println(
                    "UtilityCable connect %s side=%s primary=%d colors=%s <-> %s side=%s primary=%d colors=%s",
                    coordinate,
                    side,
                    primaryThis,
                    thisColors.joinToString(","),
                    el.coordinate,
                    el.side,
                    primaryOther,
                    otherColors.joinToString(",")
                )
                for (idx in conductorLoads.indices) {
                    if (idx == primaryThis) continue
                    val color = thisColors.getOrElse(idx) { continue }
                    val otherIdx = otherColors.indexOfFirst { it == color }
                    if (otherIdx < 0 || otherIdx == primaryOther || otherIdx > el.conductorLoads.lastIndex) continue
                    Utils.println(
                        "UtilityCable extra conductor %s side=%s color=%d idx=%d -> %s side=%s otherIdx=%d",
                        coordinate,
                        side,
                        color,
                        idx,
                        el.coordinate,
                        el.side,
                        otherIdx
                    )
                    val econ = ElectricalConnection(conductorLoads[idx], el.conductorLoads[otherIdx])
                    Eln.simulator.addElectricalComponent(econ)
                    connection.addConnection(econ)
                }
            }
        }
    }

    override fun multiMeterString(): String {
        return if (descriptor.actsAsSingleConductor || conductorsBound) {
            Utils.plotUIP(conductorLoads[0].voltage, conductorLoads[0].current)
        } else {
            conductorLoads.mapIndexed { idx, load ->
                val color = activePalette().conductorColors.getOrElse(idx) { 15 }
                val power = abs(load.voltage * load.current)
                "${dyeToChat(color)}${idx + 1}\u00A7r ${plotVolt("V:", load.voltage).trim()} ${plotAmpere("I:", load.current).trim()} ${Utils.plotPower("P:", power).trim()}"
            }.joinToString("\n")
        }
    }

    override fun thermoMeterString(): String {
        return plotAmbientCelsius("T", thermalLoad.temperatureCelsius)
    }

    override fun getWaila(): Map<String, String> {
        val info = linkedMapOf<String, String>()
        info[tr("Temperature")] = plotAmbientCelsius("", thermalLoad.temperatureCelsius)
        if (descriptor.actsAsSingleConductor || conductorsBound) {
            val power = abs(conductorLoads[0].voltage * conductorLoads[0].current)
            info[tr("Conductor")] =
                "${plotVolt("", conductorLoads[0].voltage).trim()}, ${plotAmpere("", conductorLoads[0].current).trim()}, ${Utils.plotPower("", power).trim()}"
        } else {
            info[tr("Palette")] = activePalette().displayName
            conductorLoads.forEachIndexed { idx, load ->
                val power = abs(load.voltage * load.current)
                info[conductorLabel(idx)] =
                    "${plotVolt("", load.voltage).trim()}, ${plotAmpere("", load.current).trim()}, ${Utils.plotPower("", power).trim()}"
            }
        }
        info[tr("Subsystem Matrix Size")] = renderSubSystemWaila(conductorLoads[0].subSystem)
        return info
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeByte(singleColor shl 4)
            stream.writeByte(paletteIndex)
            stream.writeFloat((thermalLoad.temperatureCelsius + getAmbientTemperatureCelsius()).toFloat())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (isPlayerUsingWrench(entityPlayer)) {
            if (!descriptor.actsAsSingleConductor && descriptor.supportsPaletteSelection) {
                paletteIndex = (paletteIndex + 1) % descriptor.colorPalettes.size
                addChatMessage(entityPlayer, tr("Conductor colors: %1$", activePalette().displayName))
            } else {
                colorCare = colorCare xor 1
                addChatMessage(entityPlayer, "Wire color care $colorCare")
            }
            reconnect()
            return true
        }

        if (descriptor.actsAsSingleConductor && descriptor.insulated) {
            val currentItemStack = entityPlayer.currentEquippedItem
            val gen = GenericItemUsingDamageDescriptor.getDescriptor(currentItemStack)
            if (gen is BrushDescriptor) {
                val brushColor = gen.getColor(currentItemStack)
                if (brushColor != singleColor && gen.use(currentItemStack, entityPlayer)) {
                    singleColor = brushColor
                    reconnect()
                }
                return true
            }
        }

        return false
    }

    private fun activePalette(): UtilityCablePalette {
        return descriptor.colorPalettes[paletteIndex.coerceIn(0, descriptor.colorPalettes.lastIndex)]
    }

    fun activeConductorColors(): IntArray {
        return activePalette().conductorColors.copyOf()
    }

    fun hasActiveConductorColor(color: Int): Boolean {
        return activePalette().conductorColors.any { (it and 0xF) == (color and 0xF) }
    }

    fun maskForConductorColor(color: Int): Int {
        return descriptor.nodeMask + ((color and 0xF) shl NodeBase.maskColorShift) + (1 shl NodeBase.maskColorCareShift)
    }

    fun activeGroundConductorColor(): Int {
        return activePalette().conductorColors.firstOrNull { isGroundColor(it) } ?: -1
    }

    fun activeNeutralConductorColor(): Int {
        return activePalette().conductorColors.firstOrNull { isNeutralColor(it) } ?: -1
    }

    fun activeHotConductorColors(): IntArray {
        return activePalette().conductorColors.filterNot { isGroundColor(it) || isNeutralColor(it) }.toIntArray()
    }

    private fun isGroundColor(color: Int): Boolean {
        return UtilityCableDescriptor.isGroundColorCode(color)
    }

    private fun isNeutralColor(color: Int): Boolean {
        return UtilityCableDescriptor.isNeutralColorCode(color)
    }

    private fun defaultConductorIndex(): Int {
        val colors = activePalette().conductorColors
        val hotIdx = colors.indexOfFirst { !isGroundColor(it) && !isNeutralColor(it) }
        if (hotIdx >= 0) {
            return hotIdx.coerceIn(0, conductorLoads.lastIndex)
        }
        val neutralIdx = colors.indexOfFirst { isNeutralColor(it) }
        if (neutralIdx >= 0) {
            return neutralIdx.coerceIn(0, conductorLoads.lastIndex)
        }
        val groundIdx = colors.indexOfFirst { isGroundColor(it) }
        if (groundIdx >= 0) {
            return groundIdx.coerceIn(0, conductorLoads.lastIndex)
        }
        return 0
    }

    private fun conductorLabel(index: Int): String {
        val color = activePalette().conductorColors.getOrElse(index) { -1 }
        return if (isGroundColor(color)) tr("Ground") else tr(dyeName(color))
    }

    private fun dyeName(color: Int): String {
        return when (color and 0xF) {
            0 -> "Black"
            1 -> "Red"
            2 -> "Green"
            3 -> "Brown"
            4 -> "Blue"
            5 -> "Purple"
            6 -> "Cyan"
            7 -> "Silver"
            8 -> "Gray"
            9 -> "Pink"
            10 -> "Lime"
            11 -> "Yellow"
            12 -> "Light Blue"
            13 -> "Magenta"
            14 -> "Orange"
            else -> "White"
        }
    }

    private fun dyeToChat(color: Int): EnumChatFormatting {
        if (isGroundColor(color)) return EnumChatFormatting.DARK_GREEN
        return when (color and 0xF) {
            0 -> EnumChatFormatting.BLACK
            1 -> EnumChatFormatting.RED
            2 -> EnumChatFormatting.DARK_GREEN
            3 -> EnumChatFormatting.GOLD
            4 -> EnumChatFormatting.BLUE
            5 -> EnumChatFormatting.DARK_PURPLE
            6 -> EnumChatFormatting.AQUA
            7 -> EnumChatFormatting.GRAY
            8 -> EnumChatFormatting.DARK_GRAY
            9 -> EnumChatFormatting.LIGHT_PURPLE
            10 -> EnumChatFormatting.GREEN
            11 -> EnumChatFormatting.YELLOW
            12 -> EnumChatFormatting.BLUE
            13 -> EnumChatFormatting.DARK_PURPLE
            14 -> EnumChatFormatting.GOLD
            else -> EnumChatFormatting.WHITE
        }
    }

    private fun bindConductors(force: Boolean = false) {
        if ((!force && conductorsBound) || conductorLoads.size <= 1) return
        conductorsBound = true
        for (idx in 1 until conductorLoads.size) {
            val connection = ElectricalConnection(conductorLoads[0], conductorLoads[idx])
            breakdownConnections.add(connection)
            electricalComponentList.add(connection)
            Eln.simulator.addElectricalComponent(connection)
        }
        if (!force) {
            reconnect()
        }
    }

    private fun replaceWith(replacement: SixNodeDescriptor) {
        val node = sixNode ?: return
        val currentTemperature = thermalLoad.temperatureCelsius
        node.disconnect()
        val newElement = replacement.ElementClass
            .getConstructor(SixNode::class.java, Direction::class.java, SixNodeDescriptor::class.java)
            .newInstance(node, side, replacement) as SixNodeElement
        newElement.front = front
        node.sideElementList[side.int] = newElement
        node.sideElementIdList[side.int] = replacement.parentItemDamage
        if (newElement is UtilityCableElement) {
            newElement.singleColor = singleColor
            newElement.colorCare = colorCare
            newElement.paletteIndex = paletteIndex
        }
        newElement.initialize()
        if (newElement is UtilityCableElement) {
            newElement.thermalLoad.temperatureCelsius = currentTemperature
            newElement.lastPublishedTemperatureCelsius = currentTemperature
        }
        node.connect()
        node.needPublish = true
    }

    private fun maybePublishTemperature(absoluteTemperatureCelsius: Double) {
        val previous = lastPublishedTemperatureCelsius
        val crossedGlowThreshold = previous <= 550.0 && absoluteTemperatureCelsius > 550.0 ||
            previous > 550.0 && absoluteTemperatureCelsius <= 550.0
        val smokeThreshold = descriptor.meltTemperatureCelsius * 0.8
        val crossedSmokeThreshold = descriptor.insulated && !descriptor.melted && (
            previous <= smokeThreshold && absoluteTemperatureCelsius > smokeThreshold ||
                previous > smokeThreshold && absoluteTemperatureCelsius <= smokeThreshold
            )
        val changedEnough = previous.isNaN() || abs(absoluteTemperatureCelsius - previous) >= 10.0
        if (changedEnough || crossedGlowThreshold || crossedSmokeThreshold) {
            needPublish()
            lastPublishedTemperatureCelsius = absoluteTemperatureCelsius
        }
    }

    private fun meltCable() {
        replaceWith(descriptor.meltedDescriptor ?: return)
    }

    private fun meltConductor() {
        replaceWith(descriptor.moltenPileDescriptor ?: return)
    }

    private fun insulationStressVoltage(): Double {
        var maxStress = conductorLoads.maxOf { abs(it.voltage) }
        if (conductorLoads.size > 1) {
            for (idx in conductorLoads.indices) {
                for (other in idx + 1 until conductorLoads.size) {
                    maxStress = maxOf(maxStress, abs(conductorLoads[idx].voltage - conductorLoads[other].voltage))
                }
            }
        }
        return maxStress
    }

    private fun shockNearbyPlayers(voltage: Double) {
        val coord = coordinate ?: return
        val damage = when {
            voltage > 100.0 -> Float.MAX_VALUE
            voltage > 50.0 -> (voltage / 10.0).toFloat().coerceAtLeast(5.0f)
            else -> return
        }
        val world = coord.world()
        val players = world.getEntitiesWithinAABB(EntityPlayer::class.java, coord.getAxisAlignedBB(1))
        for (player in players) {
            (player as? EntityPlayer)?.attackEntityFrom(DamageSource("electrical_cable"), damage)
        }
    }

    private inner class UtilityCableFailureProcess : IProcess {
        override fun process(time: Double) {
            shockCooldown = (shockCooldown - time).coerceAtLeast(0.0)
            val absoluteTemperatureCelsius = thermalLoad.temperatureCelsius + getAmbientTemperatureCelsius()
            maybePublishTemperature(absoluteTemperatureCelsius)
            if (absoluteTemperatureCelsius >= descriptor.material.meltingPointCelsius) {
                meltConductor()
                return
            }
            if (descriptor.insulated && !descriptor.melted) {
                if (absoluteTemperatureCelsius >= descriptor.meltTemperatureCelsius) {
                    meltCable()
                    return
                }
                val stress = insulationStressVoltage()
                if (stress > descriptor.insulationVoltageRating) {
                    if (conductorLoads.size > 1) {
                        bindConductors()
                    }
                    if (shockCooldown <= 0.0) {
                        shockNearbyPlayers(stress)
                        shockCooldown = 1.0
                    }
                }
            }
        }
    }
}

class UtilityCableRender(
    tileEntity: SixNodeEntity?,
    side: Direction?,
    descriptor: SixNodeDescriptor
) : SixNodeElementRender(tileEntity!!, side!!, descriptor) {

    private val descriptor = descriptor as UtilityCableDescriptor
    private var color = 0
    private var paletteIndex = 0
    private var temperatureCelsius = 20f

    override fun drawCableAuto() = false

    override fun draw() {
        Minecraft.getMinecraft().mcProfiler.startSection("UtilityCable")
        if (descriptor.insulated && descriptor.flatStyle && !descriptor.actsAsSingleConductor) {
            val jacket = jacketColor()
            GL11.glColor3f(jacket[0], jacket[1], jacket[2])
        } else if (descriptor.insulated) {
            setGlColorFromDye(color, 1.0f)
        } else {
            when (descriptor.material) {
                UtilityCableMaterial.COPPER -> GL11.glColor3f(0.85f, 0.42f, 0.18f)
                UtilityCableMaterial.ALUMINUM -> GL11.glColor3f(0.78f, 0.80f, 0.84f)
            }
        }
        bindTexture(descriptor.render.cableTexture)
        glListCall()
        drawHotMetalGlow()
        if (shouldDrawExposedSingleConductor()) {
            drawSingleExposedConductor()
        }
        if (shouldDrawExposedFlatConductors()) {
            drawFlatConductors()
        }
        emitOverheatSmoke()
        GL11.glColor3f(1f, 1f, 1f)
        Minecraft.getMinecraft().mcProfiler.endSection()
    }

    override fun glListDraw() {
        val connectionMask = renderConnectionMask(jacket = true)
        val connectionType = renderConnectionType(jacket = true)
        CableRender.drawCable(descriptor.render, connectionMask, connectionType, descriptor.render.widthDiv2 / 2.0f, false)
        if (shouldDrawJunctionNode()) {
            CableRender.drawNode(descriptor.render, connectionMask, connectionType)
        }
    }

    override fun glListEnable() = true

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            color = (stream.readByte().toInt() shr 4) and 0xF
            paletteIndex = stream.readByte().toInt().coerceAtLeast(0)
            temperatureCelsius = stream.readFloat()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor {
        return descriptor.render
    }

    override fun getCableDry(lrdu: LRDU?): Int {
        return if (descriptor.insulated) color else 0
    }

    private fun activePalette(): UtilityCablePalette {
        return descriptor.colorPalettes[paletteIndex.coerceIn(0, descriptor.colorPalettes.lastIndex)]
    }

    private fun jacketColor(): FloatArray {
        return when (descriptor.sizeLabel) {
            "14/2" -> floatArrayOf(0.90f, 0.89f, 0.84f)
            "14/3" -> floatArrayOf(0.62f, 0.80f, 0.93f)
            "12/2" -> floatArrayOf(0.93f, 0.82f, 0.19f)
            "12/3" -> floatArrayOf(0.77f, 0.33f, 0.74f)
            "10/2" -> floatArrayOf(0.92f, 0.49f, 0.16f)
            "10/3" -> floatArrayOf(0.95f, 0.63f, 0.78f)
            else -> floatArrayOf(0.12f, 0.12f, 0.12f)
        }
    }

    private fun drawFlatConductors() {
        val palette = activePalette()
        val count = minOf(descriptor.conductorCount, palette.conductorColors.size)
        if (count <= 0) return

        val outerWidth = descriptor.render.width
        val outerHeight = descriptor.render.height
        val conductorBand = outerWidth * 0.82f / count
        val conductorWidth = conductorBand * 0.78f
        val conductorHeight = minOf(outerHeight * 0.72f, conductorWidth * 0.98f)
        val conductorRender = CableRenderDescriptor("eln", "sprites/cable.png", conductorWidth * 16f, conductorHeight * 16f)
        val connectionType = renderConnectionType(jacket = false)
        val connectionMask = renderConnectionMask(jacket = false)
        val centerOffset = -outerWidth / 2f + outerWidth * 0.11f + conductorBand / 2f
        val spreadOnZ = shouldSpreadConductorsOnZ()

        for (idx in 0 until count) {
            GL11.glPushMatrix()
            val offset = centerOffset + idx * conductorBand
            if (spreadOnZ) {
                GL11.glTranslatef(0f, 0f, offset)
            } else {
                GL11.glTranslatef(0f, offset, 0f)
            }
            when (palette.conductorColors[idx] and 0xF) {
                0 -> GL11.glColor3f(0.12f, 0.12f, 0.12f)
                1 -> GL11.glColor3f(0.77f, 0.18f, 0.16f)
                2 -> GL11.glColor3f(0.19f, 0.65f, 0.22f)
                5 -> GL11.glColor3f(0.19f, 0.65f, 0.22f)
                11 -> GL11.glColor3f(0.92f, 0.88f, 0.22f)
                12 -> GL11.glColor3f(0.26f, 0.49f, 0.86f)
                14 -> GL11.glColor3f(0.95f, 0.54f, 0.20f)
                15 -> GL11.glColor3f(0.88f, 0.88f, 0.84f)
                else -> setGlColorFromDye(palette.conductorColors[idx], 1.0f)
            }
            CableRender.drawCable(conductorRender, connectionMask, connectionType, conductorRender.widthDiv2 / 2.0f, false)
            if (shouldDrawJunctionNode()) {
                CableRender.drawNode(conductorRender, connectionMask, connectionType)
            }
            GL11.glPopMatrix()
        }
    }

    private fun drawSingleExposedConductor() {
        val conductorWidthPixels = (descriptor.render.widthPixel * 0.52f).coerceAtMost(descriptor.render.widthPixel * 0.9f).coerceAtLeast(0.35f)
        val conductorHeightPixels = (descriptor.render.heightPixel * 0.52f).coerceAtMost(descriptor.render.heightPixel * 0.9f).coerceAtLeast(0.2f)
        val conductorRender = CableRenderDescriptor("eln", "sprites/cable.png", conductorWidthPixels, conductorHeightPixels)
        val connectionMask = renderConnectionMask(jacket = false)
        val connectionType = renderConnectionType(jacket = false)

        when (descriptor.material) {
            UtilityCableMaterial.COPPER -> GL11.glColor3f(0.85f, 0.42f, 0.18f)
            UtilityCableMaterial.ALUMINUM -> GL11.glColor3f(0.78f, 0.80f, 0.84f)
        }
        bindTexture(conductorRender.cableTexture)
        CableRender.drawCable(conductorRender, connectionMask, connectionType, conductorRender.widthDiv2 / 2.0f, false)
    }

    private fun drawHotMetalGlow() {
        if (descriptor.insulated) {
            return
        }
        val glowAlpha = hotGlowAlpha(temperatureCelsius.toDouble())
        if (glowAlpha <= 0f) {
            return
        }
        val glowColor = hotGlowColor(temperatureCelsius.toDouble())
        disableLight()
        enableBlend()
        GL11.glColor4f(glowColor[0], glowColor[1], glowColor[2], glowAlpha)
        bindTexture(descriptor.render.cableTexture)
        glListCall()
        disableBlend()
        enableLight()
    }

    private fun emitOverheatSmoke() {
        if (!descriptor.insulated || descriptor.melted) {
            return
        }
        val startSmokingAt = descriptor.meltTemperatureCelsius * 0.8
        if (temperatureCelsius.toDouble() < startSmokingAt || temperatureCelsius.toDouble() >= descriptor.meltTemperatureCelsius) {
            return
        }
        val world = tileEntity.worldObj ?: return
        if (!world.isRemote) {
            return
        }
        val intensity = ((temperatureCelsius.toDouble() - startSmokingAt) / (descriptor.meltTemperatureCelsius - startSmokingAt)).coerceIn(0.0, 0.999)
        if (world.rand.nextDouble() > 0.08 + intensity * 0.22) {
            return
        }
        val baseX = tileEntity.xCoord + 0.5
        val baseY = tileEntity.yCoord + 0.5
        val baseZ = tileEntity.zCoord + 0.5
        val dx = (world.rand.nextDouble() - 0.5) * 0.22
        val dy = world.rand.nextDouble() * 0.08 + 0.02
        val dz = (world.rand.nextDouble() - 0.5) * 0.22
        world.spawnParticle("smoke", baseX + dx, baseY + dy, baseZ + dz, 0.0, 0.02 + intensity * 0.03, 0.0)
    }

    private fun shouldDrawJunctionNode(): Boolean {
        return connectionCount() >= 3
    }

    private fun shouldDrawExposedSingleConductor(): Boolean {
        return descriptor.insulated && descriptor.actsAsSingleConductor && connectionCount() <= 1
    }

    private fun shouldDrawExposedFlatConductors(): Boolean {
        if (!(descriptor.insulated && descriptor.flatStyle && !descriptor.actsAsSingleConductor)) {
            return false
        }
        val count = connectionCount()
        return count <= 1
    }

    private fun usesTerminalFlatRender(): Boolean {
        if (!usesTerminalEndRender()) {
            return false
        }
        val count = connectionCount()
        if (count == 0) {
            return true
        }
        return count == 1
    }

    private fun renderConnectionMask(jacket: Boolean): LRDUMask {
        val terminalRender = if (jacket) usesTerminalFlatRender() else shouldUseTerminalExposedRender()
        if (!terminalRender) {
            return connectedSide
        }
        return LRDUMask().apply {
            when (terminalStubDirection()) {
                LRDU.Left -> {
                    this[LRDU.Left] = true
                    this[LRDU.Right] = true
                }
                LRDU.Right -> {
                    this[LRDU.Right] = true
                    this[LRDU.Left] = true
                }
                LRDU.Up -> {
                    this[LRDU.Up] = true
                    this[LRDU.Down] = true
                }
                LRDU.Down -> {
                    this[LRDU.Down] = true
                    this[LRDU.Up] = true
                }
            }
        }
    }

    private fun usesTerminalEndRender(): Boolean {
        if (!descriptor.insulated) {
            return false
        }
        if (!descriptor.actsAsSingleConductor && !descriptor.flatStyle) {
            return false
        }
        return connectionCount() <= 1
    }

    private fun renderConnectionType(jacket: Boolean): CableRenderType {
        val terminalRender = if (jacket) usesTerminalFlatRender() else shouldUseTerminalExposedRender()
        if (!terminalRender) {
            return CableRender.connectionType(this, side)
        }
        val trimPixels = if (jacket) 4f else 2f
        return CableRenderType().apply {
            when (terminalStubDirection()) {
                LRDU.Left -> {
                    method[LRDU.Right.dir] = CableRenderTypeMethodType.Internal
                    endAt[LRDU.Right.dir] = trimPixels
                    if (connectionCount() == 0) {
                        method[LRDU.Left.dir] = CableRenderTypeMethodType.Internal
                        endAt[LRDU.Left.dir] = trimPixels
                    }
                }
                LRDU.Right -> {
                    method[LRDU.Left.dir] = CableRenderTypeMethodType.Internal
                    endAt[LRDU.Left.dir] = trimPixels
                    if (connectionCount() == 0) {
                        method[LRDU.Right.dir] = CableRenderTypeMethodType.Internal
                        endAt[LRDU.Right.dir] = trimPixels
                    }
                }
                LRDU.Up -> {
                    method[LRDU.Down.dir] = CableRenderTypeMethodType.Internal
                    endAt[LRDU.Down.dir] = trimPixels
                    if (connectionCount() == 0) {
                        method[LRDU.Up.dir] = CableRenderTypeMethodType.Internal
                        endAt[LRDU.Up.dir] = trimPixels
                    }
                }
                LRDU.Down -> {
                    method[LRDU.Up.dir] = CableRenderTypeMethodType.Internal
                    endAt[LRDU.Up.dir] = trimPixels
                    if (connectionCount() == 0) {
                        method[LRDU.Down.dir] = CableRenderTypeMethodType.Internal
                        endAt[LRDU.Down.dir] = trimPixels
                    }
                }
            }
        }
    }

    private fun shouldUseTerminalExposedRender(): Boolean {
        return shouldDrawExposedSingleConductor() || shouldDrawExposedFlatConductors()
    }

    private fun hotGlowAlpha(temperatureCelsius: Double): Float {
        if (temperatureCelsius <= 550.0) return 0f
        return ((temperatureCelsius - 550.0) / 250.0).toFloat().coerceIn(0.12f, 0.85f)
    }

    private fun hotGlowColor(temperatureCelsius: Double): FloatArray {
        val normalized = ((temperatureCelsius - 550.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
        val red = 1.0f
        val green = 0.22f + normalized * 0.55f
        val blue = 0.05f + normalized * 0.18f
        return floatArrayOf(red, green, blue)
    }

    private fun connectionCount(): Int {
        var count = 0
        if (connectedSide[LRDU.Left]) count++
        if (connectedSide[LRDU.Right]) count++
        if (connectedSide[LRDU.Up]) count++
        if (connectedSide[LRDU.Down]) count++
        return count
    }

    private fun terminalStubDirection(): LRDU {
        if (connectedSide[LRDU.Left]) return LRDU.Left
        if (connectedSide[LRDU.Right]) return LRDU.Right
        if (connectedSide[LRDU.Up]) return LRDU.Up
        if (connectedSide[LRDU.Down]) return LRDU.Down
        return LRDU.Left
    }

    private fun shouldSpreadConductorsOnZ(): Boolean {
        val mask = renderConnectionMask(jacket = false)
        return (mask[LRDU.Up] || mask[LRDU.Down]) && !(mask[LRDU.Left] || mask[LRDU.Right])
    }
}
