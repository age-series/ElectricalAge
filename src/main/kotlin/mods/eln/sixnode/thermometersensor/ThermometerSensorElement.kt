package mods.eln.sixnode.thermometersensor

import mods.eln.environment.BiomeClimateService
import mods.eln.i18n.I18N
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream

class ThermometerSensorElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, descriptor) {

    @JvmField
    val outputGate = NbtElectricalGateOutput("outputGate")

    @JvmField
    val outputGateProcess = NbtElectricalGateOutputProcess("outputGateProcess", outputGate)

    private val serverInventory = SixNodeElementInventory(0, 64, this)

    @JvmField
    val slowProcess = ThermometerSensorSlowProcess(this)

    var lowValue = -40f
    var highValue = 50f

    init {
        electricalLoadList.add(outputGate)
        electricalComponentList.add(outputGateProcess)
        slowProcessList.add(slowProcess)
    }

    companion object {
        @JvmStatic
        fun canBePlacedOnSide(side: Direction, type: Int): Boolean = true

        const val setValueId = 1.toByte()
    }

    override val inventory: IInventory
        get() = serverInventory

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        lowValue = nbt.getFloat("lowValue")
        highValue = nbt.getFloat("highValue")
        if (highValue <= lowValue) highValue = lowValue + 0.0001f
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setFloat("lowValue", lowValue)
        nbt.setFloat("highValue", highValue)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        return if (front == lrdu.left()) outputGate else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU): Int {
        return if (front == lrdu.left()) NodeBase.maskElectricalOutputGate else 0
    }

    override fun multiMeterString(): String {
        return Utils.plotVolt("U:", outputGate.voltage) + Utils.plotAmpere("I:", outputGate.current)
    }

    override fun thermoMeterString(): String = ""

    override fun getWaila(): Map<String, String> {
        val info = HashMap<String, String>()
        info[I18N.tr("Output voltage")] = Utils.plotVolt("", outputGate.voltage)
        val node = sixNode ?: return info
        if (node.coordinate.worldExist) {
            val climate = BiomeClimateService.sample(
                node.coordinate.world(),
                node.coordinate.x,
                node.coordinate.y,
                node.coordinate.z
            )
            info[I18N.tr("Biome temperature")] = Utils.plotValue(climate.temperatureCelsius, "°C ")
            info[I18N.tr("Relative humidity")] = String.format("%.0f%%", climate.relativeHumidityPercent)
        }
        return info
    }

    override fun initialize() {}

    override fun hasGui(): Boolean = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return ThermometerSensorContainer(player, serverInventory)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(lowValue)
        stream.writeFloat(highValue)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        when (stream.readByte()) {
            setValueId -> {
                lowValue = stream.readFloat()
                highValue = stream.readFloat()
                if (highValue <= lowValue) highValue = lowValue + 0.0001f
                needPublish()
            }
        }
    }
}
