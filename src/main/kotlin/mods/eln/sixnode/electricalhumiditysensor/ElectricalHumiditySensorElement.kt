package mods.eln.sixnode.electricalhumiditysensor

import mods.eln.environment.BiomeClimateService
import mods.eln.i18n.I18N
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess

class ElectricalHumiditySensorElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, descriptor) {

    @JvmField
    val outputGate = NbtElectricalGateOutput("outputGate")

    @JvmField
    val outputGateProcess = NbtElectricalGateOutputProcess("outputGateProcess", outputGate)

    @JvmField
    val slowProcess = ElectricalHumiditySensorSlowProcess(this)

    init {
        electricalLoadList.add(outputGate)
        electricalComponentList.add(outputGateProcess)
        slowProcessList.add(slowProcess)
    }

    companion object {
        @JvmStatic
        fun canBePlacedOnSide(side: Direction, type: Int): Boolean = true
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
            info[I18N.tr("Ambient temperature")] = Utils.plotCelsius("", climate.temperatureCelsius)
            info[I18N.tr("Relative humidity")] = String.format("%.0f%%", climate.relativeHumidityPercent)
            info[I18N.tr("Precipitation")] = I18N.tr(
                when (climate.precipitationType) {
                    "rain" -> "Rain"
                    "snow" -> "Snow"
                    else -> "None"
                }
            )
        }
        return info
    }

    override fun thermoMeterString(): String = ""

    override fun initialize() {}
}
