package mods.eln.sixnode.electricalcable

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.BrushDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.Utils.isPlayerUsingWrench
import mods.eln.misc.Utils.plotAmpere
import mods.eln.misc.Utils.plotValue
import mods.eln.misc.Utils.plotVolt
import mods.eln.misc.Utils.renderSubSystemWaila
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.misc.UtilsClient.bindTexture
import mods.eln.node.NodeBase
import mods.eln.node.NodeConnection
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.DamageSource
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
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

class UtilityCableDescriptor(
    name: String,
    render: mods.eln.cable.CableRenderDescriptor,
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
) : mods.eln.node.six.SixNodeElement(sixNode!!, side!!, descriptor) {

    @JvmField
    val descriptor = descriptor as UtilityCableDescriptor

    private var singleColor = 0
    private var colorCare = 1
    private var paletteIndex = 0
    private var conductorsBound = false
    private var shockCooldown = 0.0

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
        if (mods.eln.misc.Utils.mustDropItem(entityPlayer)) {
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
                for (idx in conductorLoads.indices) {
                    if (idx == primaryThis) continue
                    val color = thisColors.getOrElse(idx) { continue }
                    val otherIdx = otherColors.indexOfFirst { it == color }
                    if (otherIdx < 0 || otherIdx == primaryOther || otherIdx > el.conductorLoads.lastIndex) continue
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

    private fun defaultConductorIndex(): Int {
        val colors = activePalette().conductorColors
        val groundIdx = colors.indexOfFirst { it == 5 }
        if (groundIdx >= 0) {
            return groundIdx.coerceIn(0, conductorLoads.lastIndex)
        }
        return 0
    }

    private fun conductorLabel(index: Int): String {
        val color = activePalette().conductorColors.getOrElse(index) { -1 }
        return if (color == 5) tr("Ground") else tr(dyeName(color))
    }

    private fun dyeName(color: Int): String {
        return when (color and 0xF) {
            0 -> "Black"
            1 -> "Blue"
            2 -> "Green"
            3 -> "Cyan"
            4 -> "Brown"
            5 -> "Ground"
            6 -> "Orange"
            7 -> "Gray"
            8 -> "Dark Gray"
            9 -> "Light Blue"
            10 -> "Lime"
            11 -> "White"
            12 -> "Red"
            13 -> "Magenta"
            14 -> "Yellow"
            else -> "White"
        }
    }

    private fun dyeToChat(color: Int): EnumChatFormatting {
        return when (color and 0xF) {
            0 -> EnumChatFormatting.BLACK
            1 -> EnumChatFormatting.DARK_BLUE
            2 -> EnumChatFormatting.DARK_GREEN
            3 -> EnumChatFormatting.DARK_AQUA
            4 -> EnumChatFormatting.DARK_RED
            5 -> EnumChatFormatting.DARK_GREEN
            6 -> EnumChatFormatting.GOLD
            7 -> EnumChatFormatting.GRAY
            8 -> EnumChatFormatting.DARK_GRAY
            9 -> EnumChatFormatting.BLUE
            10 -> EnumChatFormatting.GREEN
            11 -> EnumChatFormatting.AQUA
            12 -> EnumChatFormatting.RED
            13 -> EnumChatFormatting.DARK_PURPLE
            14 -> EnumChatFormatting.YELLOW
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
        node.disconnect()
        val newElement = replacement.ElementClass
            .getConstructor(SixNode::class.java, Direction::class.java, SixNodeDescriptor::class.java)
            .newInstance(node, side, replacement) as mods.eln.node.six.SixNodeElement
        newElement.front = front
        node.sideElementList[side.int] = newElement
        node.sideElementIdList[side.int] = replacement.parentItemDamage
        if (newElement is UtilityCableElement) {
            newElement.singleColor = singleColor
            newElement.colorCare = colorCare
            newElement.paletteIndex = paletteIndex
        }
        newElement.initialize()
        node.connect()
        node.needPublish = true
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

    private inner class UtilityCableFailureProcess : mods.eln.sim.IProcess {
        override fun process(time: Double) {
            shockCooldown = (shockCooldown - time).coerceAtLeast(0.0)
            if (thermalLoad.temperatureCelsius + getAmbientTemperatureCelsius() >= descriptor.material.meltingPointCelsius) {
                meltConductor()
                return
            }
            if (descriptor.insulated && !descriptor.melted) {
                if (thermalLoad.temperatureCelsius + getAmbientTemperatureCelsius() >= descriptor.meltTemperatureCelsius) {
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
        if (descriptor.insulated && descriptor.flatStyle && !descriptor.actsAsSingleConductor) {
            drawFlatConductors()
        }
        GL11.glColor3f(1f, 1f, 1f)
        Minecraft.getMinecraft().mcProfiler.endSection()
    }

    override fun glListDraw() {
        CableRender.drawCable(descriptor.render, connectedSide, CableRender.connectionType(this, side), descriptor.render.widthDiv2 / 2.0f, false)
        CableRender.drawNode(descriptor.render, connectedSide, CableRender.connectionType(this, side))
    }

    override fun glListEnable() = true

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            color = (stream.readByte().toInt() shr 4) and 0xF
            paletteIndex = stream.readByte().toInt().coerceAtLeast(0)
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
        val conductorBand = outerWidth * 0.78f / count
        val conductorWidth = conductorBand * 0.64f
        val conductorHeight = minOf(outerHeight * 0.55f, conductorWidth * 0.95f)
        val conductorRender = CableRenderDescriptor("eln", "sprites/cable.png", conductorWidth * 16f, conductorHeight * 16f)
        val connectionType = CableRender.connectionType(this, side)
        val centerOffset = -outerWidth / 2f + outerWidth * 0.11f + conductorBand / 2f

        for (idx in 0 until count) {
            GL11.glPushMatrix()
            GL11.glTranslatef(0f, centerOffset + idx * conductorBand, 0f)
            when (palette.conductorColors[idx] and 0xF) {
                0 -> GL11.glColor3f(0.12f, 0.12f, 0.12f)
                5 -> GL11.glColor3f(0.19f, 0.65f, 0.22f)
                11 -> GL11.glColor3f(0.26f, 0.49f, 0.86f)
                12 -> GL11.glColor3f(0.77f, 0.18f, 0.16f)
                14 -> GL11.glColor3f(0.92f, 0.88f, 0.22f)
                15 -> GL11.glColor3f(0.88f, 0.88f, 0.84f)
                else -> setGlColorFromDye(palette.conductorColors[idx], 1.0f)
            }
            CableRender.drawCable(conductorRender, connectedSide, connectionType, conductorRender.widthDiv2 / 2.0f, false)
            CableRender.drawNode(conductorRender, connectedSide, connectionType)
            GL11.glPopMatrix()
        }
    }
}
