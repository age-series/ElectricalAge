package mods.eln.sixnode.currentcable

import mods.eln.Eln
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor.Companion.getDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.BrushDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.RealisticEnum
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.Utils.isPlayerUsingWrench
import mods.eln.misc.Utils.plotAmpere
import mods.eln.misc.Utils.plotCelsius
import mods.eln.misc.Utils.plotPower
import mods.eln.misc.Utils.plotUIP
import mods.eln.misc.Utils.plotValue
import mods.eln.misc.Utils.plotVolt
import mods.eln.misc.Utils.renderSubSystemWaila
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.misc.UtilsClient.bindTexture
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class CurrentCableDescriptor(
    name: String?,
    render: CableRenderDescriptor?,
    description: String
) :
    GenericCableDescriptor(name, CurrentCableElement::class.java, CurrentCableRender::class.java) {
    var description = "todo cable"

    init {
        electricalNominalVoltage = Eln.CCU
        thermalRp = 1.0
        thermalRs = 1.0
        thermalC = 1.0
        this.description = description
        this.render = render
        thermalWarmLimit = 100.0
        thermalCoolLimit = -100.0
        thermalWarmLimit = Eln.cableWarmLimit
        thermalCoolLimit = -10.0
        Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC)
    }

    fun setPhysicalConstantLikeNormalCable(
        electricalMaximalCurrent: Double
    ) {
        this.electricalMaximalCurrent = electricalMaximalCurrent
        this.electricalNominalPower = electricalMaximalCurrent * electricalNominalVoltage
        electricalRs = 0.01
        val thermalMaximalPowerDissipated = electricalRs * electricalMaximalCurrent * electricalMaximalCurrent * 2
        thermalC = thermalMaximalPowerDissipated * Eln.cableHeatingTime / thermalWarmLimit
        thermalRp = thermalWarmLimit / thermalMaximalPowerDissipated
        thermalRs = 0.5 / thermalC / 2
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun applyTo(electricalLoad: ElectricalLoad, rsFactor: Double) {
        electricalLoad.serialResistance = electricalRs * rsFactor
    }

    override fun applyTo(electricalLoad: ElectricalLoad) {
        applyTo(electricalLoad, 1.0)
    }

    override fun applyTo(resistor: Resistor) {
        applyTo(resistor, 1.0)
    }

    override fun applyTo(resistor: Resistor, factor: Double) {
        resistor.resistance = electricalRs * factor
    }

    override fun applyTo(thermalLoad: ThermalLoad) {
        thermalLoad.Rs = thermalRs
        thermalLoad.heatCapacity = thermalC
        thermalLoad.Rp = thermalRp
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Nominal Ratings:"))
        list.add("  " + tr("Voltage: %1\$V", plotValue(electricalNominalVoltage)))
        list.add("  " + tr("Current: %1\$A", plotValue(electricalNominalPower / electricalNominalVoltage)))
        list.add("  " + tr("Power: %1\$W", plotValue(electricalNominalPower)))
        list.add("  " + tr("Serial resistance: %1$\u2126", plotValue(electricalRs * 2)))
    }

    override fun addRealismContext(list: MutableList<String>): RealisticEnum? {
        list.add(tr("Has some caveats:"))
        list.add(tr("  * Wire resistance is much higher than normal"))
        list.add(tr("  * Wire resistance is not impacted by temperature"))
        list.add(tr("  * Wire voltage limits are arbitrary values, picked to within reasonable simulator error"))
        list.add(tr("  * Wire current limits are arbitrary values, added as a gameplay mechanic"))
        return RealisticEnum.REALISTIC
    }

    override fun getNodeMask(): Int {
        return NodeBase.maskElectricalPower
    }

    fun bindCableTexture() {
        render.bindCableTexture()
    }
}

open class CurrentCableElement(sixNode: SixNode?, side: Direction?, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode!!, side!!, descriptor) {
    var descriptor: CurrentCableDescriptor
    var electricalLoad = NbtElectricalLoad("electricalLoad")
    var thermalLoad = NbtThermalLoad("thermalLoad")
    var heater = ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad)
    var thermalWatchdog = ThermalLoadWatchDog(thermalLoad)
    var voltageWatchdog = VoltageStateWatchDog(electricalLoad)
    var color: Int
    var colorCare: Int

    init {
        this.descriptor = descriptor as CurrentCableDescriptor
        color = 0
        colorCare = 1
        electricalLoad.setCanBeSimplifiedByLine(true)
        electricalLoadList.add(electricalLoad)
        thermalLoadList.add(thermalLoad)
        thermalSlowProcessList.add(heater)
        thermalLoad.setAsSlow()
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog
            .setTemperatureLimits(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
            .setDestroys(WorldExplosion(this).cableExplosion())
        slowProcessList.add(voltageWatchdog)
        voltageWatchdog
            .setNominalVoltage(this.descriptor.electricalNominalVoltage)
            .setDestroys(WorldExplosion(this).cableExplosion())
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val b = nbt.getByte("color")
        color = b.toInt() and 0xF
        colorCare = b.toInt() shr 4 and 1
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setByte("color", (color + (colorCare shl 4)).toByte())
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        return electricalLoad
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return thermalLoad
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return descriptor.nodeMask /*+ NodeBase.maskElectricalWire*/ + (color shl NodeBase.maskColorShift) + (colorCare shl NodeBase.maskColorCareShift)
    }

    override fun multiMeterString(): String {
        return plotUIP(
            electricalLoad.voltage,
            electricalLoad.current
        ) + " " + plotPower(
            "Cable Power Loss",
            electricalLoad.current * electricalLoad.current * electricalLoad.serialResistance
        )
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Current")] = plotAmpere("", electricalLoad.current)
        info[tr("Temperature")] = plotCelsius("", thermalLoad.temperature)
        if (Eln.wailaEasyMode) {
            info[tr("Voltage")] = plotVolt("", electricalLoad.voltage)
        }
        info[tr("Subsystem Matrix Size")] = renderSubSystemWaila(electricalLoad.subSystem)
        return info
    }

    override fun thermoMeterString(): String {
        return plotCelsius("T", thermalLoad.temperatureCelsius)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeByte(color shl 4)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun initialize() {
        descriptor.applyTo(electricalLoad)
        descriptor.applyTo(thermalLoad)
    }

    override fun onBlockActivated(
        entityPlayer: EntityPlayer,
        side: Direction,
        vx: Float,
        vy: Float,
        vz: Float
    ): Boolean {
        val currentItemStack = entityPlayer.currentEquippedItem
        if (isPlayerUsingWrench(entityPlayer)) {
            colorCare = colorCare xor 1
            addChatMessage(entityPlayer, "Wire color care $colorCare")
            sixNode!!.reconnect()
        } else if (currentItemStack != null) {
            val gen = getDescriptor(currentItemStack)
            if (gen is BrushDescriptor) {
                val brush = gen
                val brushColor = brush.getColor(currentItemStack)
                if (brushColor != color && brush.use(currentItemStack, entityPlayer)) {
                    color = brushColor
                    sixNode!!.reconnect()
                }
            }
        }
        return false
    }
}


class CurrentCableRender(tileEntity: SixNodeEntity?, side: Direction?, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity!!, side!!, descriptor) {
    var descriptor: CurrentCableDescriptor
    var color = 0

    init {
        this.descriptor = descriptor as CurrentCableDescriptor
    }

    override fun drawCableAuto(): Boolean {
        return false
    }

    override fun draw() {
        Minecraft.getMinecraft().mcProfiler.startSection("ECable")
        setGlColorFromDye(color, 1.0f)
        bindTexture(descriptor.render.cableTexture)
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
            val b: Byte
            b = stream.readByte()
            color = b.toInt() shr 4 and 0xF
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return descriptor.render
    }

    override fun getCableDry(lrdu: LRDU?): Int {
        return color
    }
}
