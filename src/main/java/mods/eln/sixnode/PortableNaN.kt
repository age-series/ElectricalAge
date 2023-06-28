package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.util.HashMap

class PortableNaNDescriptor(name: String, renderIn: CableRenderDescriptor): GenericCableDescriptor(name, PortableNaNElement::class.java, PortableNaNRender::class.java) {

    init {
        this.name = name
        this.render = renderIn
    }

    override fun applyTo(electricalLoad: ElectricalLoad, rsFactor: Double) {
        electricalLoad.serialResistance = Double.NaN
    }

    override fun applyTo(electricalLoad: ElectricalLoad) {
        electricalLoad.serialResistance = Double.NaN
    }

    override fun applyTo(resistor: Resistor) {
        resistor.resistance = Double.NaN
    }

    override fun applyTo(resistor: Resistor, factor: Double) {
        resistor.resistance = Double.NaN
    }

    override fun applyTo(thermalLoad: ThermalLoad) = thermalLoad.set(Double.NaN, Double.NaN, Double.NaN)

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Nominal Ratings:"))
        list.add("  " + tr("Voltage: Yes"))
        list.add("  " + tr("Current: No"))
        list.add("  " + tr("Serial Resistance: OK Ω"))
    }

    override fun getNodeMask(): Int {
        return NodeBase.maskElectricalAll
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (icon == null)
            return
        val icon = icon.iconName.substring(4)
        UtilsClient.drawIcon(type, ResourceLocation("eln", "textures/blocks/$icon.png"))
    }
}

class PortableNaNElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor): SixNodeElement(sixNode, side, descriptor) {
    val electricalLoad = NbtElectricalLoad("Portable NaN")
    val thermalLoad = NbtThermalLoad("Portable NaN")
    val descriptor: PortableNaNDescriptor
    init {
        this.descriptor = descriptor as PortableNaNDescriptor
        electricalLoadList.add(electricalLoad)
        thermalLoadList.add(thermalLoad)
        thermalLoad.setAsSlow()
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad {
        return electricalLoad
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad {
        thermalLoad.movePowerTo(Double.NaN)
        return thermalLoad
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return descriptor.getNodeMask()
    }

    override fun initialize() {
        descriptor.applyTo(electricalLoad)
        descriptor.applyTo(thermalLoad)
    }

    override fun multiMeterString(): String {
        return Utils.plotUIP(electricalLoad.voltage, electricalLoad.current)
    }

    override fun thermoMeterString(): String {
        return Utils.plotCelsius("T", thermalLoad.temperatureCelsius)
    }

    override fun getWaila(): Map<String, String> {
        val info = HashMap<String, String>()

        info[tr("Current")] = Utils.plotAmpere("", electricalLoad.current)
        info[tr("Temperature")] = Utils.plotCelsius("", thermalLoad.temperature)
        if (Eln.wailaEasyMode) {
            info[tr("Voltage")] = Utils.plotVolt("", electricalLoad.voltage)
        }
        info[tr("Subsystem Matrix Size")] = Utils.renderSubSystemWaila(electricalLoad.subSystem)
        return info
    }
}

class PortableNaNRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor): SixNodeElementRender(tileEntity, side, descriptor) {

    val descriptor: PortableNaNDescriptor

    init {
        this.descriptor = descriptor as PortableNaNDescriptor
    }

    override fun drawCableAuto(): Boolean {
        return false
    }

    override fun draw() {
        Minecraft.getMinecraft().mcProfiler.startSection("ACable")

        UtilsClient.bindTexture(descriptor.render?.cableTexture)
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

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return descriptor.render
    }
}
