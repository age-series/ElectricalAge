package mods.eln.transparentnode.festive

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.lampsupply.LampSupplyElement
import net.minecraft.entity.player.EntityPlayer
import java.io.DataOutputStream
import java.io.IOException

class FestiveElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    val loadResistor = Resistor(electricalLoad, null)
    var powerChannel = "xmas" // TODO: Add a GUI in the render panes and allow the user to specify a different channel.

    init {
        loadResistor.resistance = 1000.0
        slowProcessList.add(FestiveElementProcess(this))
    }

    override fun thermoMeterString(side: Direction): String {
        return tr("Not as warm as it could be")
    }

    override fun multiMeterString(side: Direction): String {
        return tr("It probably works if you apply ~200v to the xmas wireless channel")
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return null
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun initialize() {
        connect()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeBoolean(node!!.lightValue > 4)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class FestiveElementProcess(val elem: FestiveElement): IProcess {
        var bestChannelHandle: Pair<Double, LampSupplyElement.PowerSupplyChannelHandle>? = null

        private fun findBestSupply(here: Coordinate, forceUpdate: Boolean = false): Pair<Double, LampSupplyElement.PowerSupplyChannelHandle>? {
            val chanMap = LampSupplyElement.channelMap[elem.powerChannel] ?: return null
            val bestChanHand = bestChannelHandle
            // Here's our cached value. We just check if it's null and if it's still a thing.
            if (!(bestChanHand == null || forceUpdate || !chanMap.contains(bestChanHand.second))) {
                return bestChanHand // we good!
            }
            val list = LampSupplyElement.channelMap[elem.powerChannel]?.filterNotNull() ?: return null
            val map = list.map { Pair(it.element.sixNode!!.coordinate.trueDistanceTo(here), it) }
            val sortedBy = map.sortedBy { it.first }
            val chanHand = sortedBy.first()
            bestChannelHandle = chanHand
            return bestChannelHandle
        }

        override fun process(time: Double) {
            val lampSupplyList = findBestSupply(elem.node!!.coordinate)
            val best = lampSupplyList?.second
            if (best != null && best.element.getChannelState(best.id)) {
                best.element.addToRp(elem.loadResistor.resistance)
                elem.electricalLoad.state = best.element.powerLoad.state
            } else {
                elem.electricalLoad.state = 0.0
            }
            var lightDouble = 12 * (Math.abs(elem.loadResistor.voltage) - 180.0) / 20.0
            lightDouble *= 16
            elem.node!!.lightValue = lightDouble.toInt().coerceIn(0, 15)
        }
    }
}
