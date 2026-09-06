package mods.eln.sixnode.lampsupply

import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx
import mods.eln.sixnode.wirelesssignal.WirelessUtils

class LampSupplyProcess(val element: LampSupplyElement) : IProcess {

    private var sleepTimer = 0.0

    private val txSet = hashMapOf<String, HashSet<IWirelessSignalTx>>()
    private val txStrength = hashMapOf<IWirelessSignalTx, Double>()

    override fun process(time: Double) {
        element.loadResistor.setResistance(1.0 / element.connectedConductance.coerceIn(MnaConst.noImpedance, MnaConst.highImpedance))
        element.connectedConductance = 0.0

        sleepTimer -= time

        if (sleepTimer < 0) {
            sleepTimer += Utils.rand(1.2, 2.0)

            val spot = WirelessUtils.buildSpot(element.coordinate, null, 0)
            WirelessUtils.getTx(spot, txSet, txStrength)
        }

        for (idx in 0..<LampSupplyDescriptor.CHANNEL_COUNT) {
            val localEntry = element.localEntries[idx]

            when (localEntry.wirelessChannel.lowercase()) {
                "" -> element.localChannelStates[idx] = true
                "true" -> element.localChannelStates[idx] = true
                "false" -> element.localChannelStates[idx] = false
                else -> {
                    val txs = txSet[localEntry.wirelessChannel]
                    element.localChannelStates[idx] = (txs != null) && (element.localAggregators[idx][localEntry.aggregator].aggregate(txs) >= 0.5)
                }
            }
        }
    }

}