package mods.eln.sixnode.currentcable

import mods.eln.Eln
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.sim.mna.state.ElectricalLoad
import mods.eln.sim.thermal.ThermalLoad
import mods.eln.sim.mna.passive.Resistor
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import mods.eln.i18n.I18N.tr
import mods.eln.item.BrushDescriptor
import mods.eln.node.six.*
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.thermal.ThermalLoadWatchDog
import mods.eln.sim.destruct.WorldExplosion
import mods.eln.sim.thermal.ElectricalLoadHeatThermalLoad
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.HashMap
import kotlin.experimental.and

class CurrentCableDescriptor(name: String, render: CableRenderDescriptor) : GenericCableDescriptor(name, CurrentCableElement::class.java, CurrentCableRender::class.java) {

    init {
        this.render = render
        electricalNominalVoltage = 100_000.0
        electricalMaximalVoltage = electricalNominalVoltage
    }

    /**
     *
     * @param conductorArea size of conductor area (mm^2)
     * @param conductorType type of conductor (Copper, Iron, etc.)
     * @param insulationThickness thickness of insulation (mm)
     * @param insulatorType type of insulation (Rubber, Glass, etc.)
     * @param thermalWarmLimit maximum temperature (C)
     * @param thermalCoolLimit minimum temperature (C)
     * @param thermalNominalHeatTime
     */
    fun setCableProperties(
        conductorArea: Double,
        conductorType: MaterialType,
        insulationThickness: Double,
        insulatorType: MaterialType,
        thermalWarmLimit: Double,
        thermalCoolLimit: Double,
        thermalNominalHeatTime: Double
    ) {

        this.thermalWarmLimit = thermalWarmLimit
        this.thermalCoolLimit = thermalCoolLimit

        electricalMaximalCurrent = 0.355 * conductorArea // roughly (mm^2 / I) that is suggested by https://www.powerstream.com/Wire_Size.htm for power transmission lines

        this.electricalRs = MaterialProperties.getElectricalResistivity(conductorType) * (conductorArea / 1000000.0 / 1.0) * Eln.cableResistanceMultiplier // resistivity (ohms/meter)* (cross sectional area (m) / length (m))
        DP.println(DPType.SIX_NODE, "(" + this.name + ") Current Cable Resistance: " + electricalRs)

        //electricalNominalVoltage = insulationThickness * 100

        // begin odd thermal system code
        val thermalMaximalPowerDissipated = electricalMaximalCurrent * electricalMaximalCurrent * electricalRs * 2.0
        thermalC = thermalMaximalPowerDissipated * thermalNominalHeatTime / thermalWarmLimit
        thermalRp = thermalWarmLimit / thermalMaximalPowerDissipated
        thermalRs = MaterialProperties.getThermalConductivity(conductorType) / 385.0 / thermalC / 2.0
        // TODO: FIX WHEN REDOING THERMAL SYSTEM
        // I replaced thermalConductivityTao with (material.getThermalConductivity() / 385.0)
        // Since thermalConductivityTao is typically 1, I'm going to use Copper's thermal conductivity constant as a baseline.
        // When someone redoes the thermal system, please remove this shim and do it correctly.

        ThermalLoad.checkThermalLoad(thermalRs, thermalRp, thermalC)

        voltageLevelColor = VoltageLevelColor.None
    }

    override fun applyTo(electricalLoad: ElectricalLoad, rsFactor: Double) {
        electricalLoad.rs = electricalRs * rsFactor
    }

    override fun applyTo(electricalLoad: ElectricalLoad) {
        applyTo(electricalLoad, 1.0)
    }

    override fun applyTo(resistor: Resistor) {
        applyTo(resistor, 1.0)
    }

    override fun applyTo(resistor: Resistor, factor: Double) {
        resistor.r = electricalRs * factor
    }

    override fun applyTo(thermalLoad: ThermalLoad) {
        thermalLoad.Rs = this.thermalRs
        thermalLoad.C = this.thermalC
        thermalLoad.Rp = this.thermalRp
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Nominal Ratings:"))
        list.add("  " + tr("Current: %1\$A", Utils.plotValue(electricalMaximalCurrent))!!)
        list.add("  " + tr("Serial resistance: %1$\u2126", Utils.plotValue(electricalRs * 2))!!)
    }

    override fun getNodeMask(): Int {
        return NodeBase.MASK_ELECTRICAL_POWER
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (icon == null)
            return

        // remove "eln:" to add the full path replace("eln:", "textures/blocks/") + ".png";
        val icon = icon.iconName.substring(4)
        UtilsClient.drawIcon(type, ResourceLocation("eln", "textures/blocks/$icon.png"))
    }

}

class CurrentCableElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {

    var electricalLoad = NbtElectricalLoad("electricalLoad")
    private val thermalLoad = NbtThermalLoad("thermalLoad")

    private val descriptor: CurrentCableDescriptor = descriptor as CurrentCableDescriptor

    private var color: Int = 0
    private var colorCare: Int = 0

    init {

        color = 0
        colorCare = 1
        electricalLoad.setCanBeSimplifiedByLine(true)
        electricalLoadList.add(electricalLoad)

        thermalLoadList.add(thermalLoad)
        val heater = ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad)
        thermalSlowProcessList.add(heater)
        thermalLoad.setAsSlow()
        val thermalWatchdog = ThermalLoadWatchDog()
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog.set(thermalLoad).setLimit(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit).set(WorldExplosion(this).cableExplosion())
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val b = nbt.getByte("color")
        color = (b and 0xF).toInt()
        colorCare = (KSF.shiftRight(b, 4)) and 1
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setByte("color", (color + (colorCare shl 4)).toByte())
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad {
        return electricalLoad
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad {
        return thermalLoad
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return descriptor.nodeMask /*+ NodeBase.MASK_ELECTRICAL_WIRE*/ + (color shl NodeBase.MASK_COLOR_SHIFT) + (colorCare shl NodeBase.MASK_COLOR_CARE_SHIFT)
    }

    override fun initialize() {
        descriptor.applyTo(electricalLoad)
        descriptor.applyTo(thermalLoad)
    }

    override fun multiMeterString(): String {
        return Utils.plotUIP(electricalLoad.u, electricalLoad.i)
    }

    override fun thermoMeterString(): String {
        return Utils.plotCelsius("T", thermalLoad.Tc)
    }

    override fun getWaila(): Map<String, String>? {
        val info = HashMap<String, String>()

        info[tr("Current")] = Utils.plotAmpere("", electricalLoad.i)
        info[tr("Temperature")] = Utils.plotCelsius("", thermalLoad.t)
        if (Eln.wailaEasyMode) {
            info[tr("Voltage")] = Utils.plotVolt("", electricalLoad.u)
        }
        if (electricalLoad.subSystem != null) {
            val subSystemSize = electricalLoad.subSystem!!.matrixSize()
            val textColor: String
            textColor = if (subSystemSize <= 8) "§a" else if (subSystemSize <= 15) "§6" else "§c"
            info[tr("Subsystem Matrix Size: ")] = textColor + subSystemSize
        } else {
            info[tr("Subsystem Matrix Size: ")] = "§cNot part of a subsystem!?"
        }
        return info
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeByte(color shl 4)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        val currentItemStack = entityPlayer.currentEquippedItem
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            colorCare = colorCare xor 1
            Utils.addChatMessage(entityPlayer, "Wire color care $colorCare")
            sixNode.reconnect()
        } else if (currentItemStack != null) {
            val gen = GenericItemUsingDamageDescriptor.getDescriptor(currentItemStack)
            if (gen is BrushDescriptor) {
                val brushColor = gen.getColor(currentItemStack)
                if (brushColor != color && gen.use(currentItemStack, entityPlayer)) {
                    color = brushColor
                    sixNode.reconnect()
                }
            }
        }
        return false
    }
}


class CurrentCableRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {

    private val descriptor: CurrentCableDescriptor = descriptor as CurrentCableDescriptor

    private var color = 0

    override fun drawCableAuto(): Boolean {
        return false
    }

    override fun draw() {
        Minecraft.getMinecraft().mcProfiler.startSection("ACable")

        Utils.setGlColorFromDye(color, 1.0f)

        UtilsClient.bindTexture(descriptor.render.cableTexture)
        glListCall()

        GL11.glColor3f(1f, 1f, 1f)
        Minecraft.getMinecraft().mcProfiler.endSection()
    }

    override fun glListDraw() {
        CableRender.drawCable(descriptor.render, connectedSide, CableRender.connectionType(this, side))
        CableRender.drawNode(descriptor.render, connectedSide, CableRender.connectionType(this, side))
    }

    override fun glListEnable(): Boolean {
        return true
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            val b: Byte?
            b = stream.readByte()
            color = KSF.shiftRight(b, 4) and 0xF
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return descriptor.render
    }

    override fun getCableDry(lrdu: LRDU): Int {
        return color
    }
}
