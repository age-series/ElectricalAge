package mods.eln.sim.electrical.nbt

import mods.eln.node.NodeBase
import mods.eln.sim.electrical.process.BatteryProcess
import mods.eln.sim.electrical.process.BatterySlowProcess
import mods.eln.sim.thermal.ThermalLoad

class NbtBatterySlowProcess(
    var node: NodeBase,
    batteryProcess: BatteryProcess,
    thermalLoad: ThermalLoad
) : BatterySlowProcess(batteryProcess, thermalLoad) {

    var explosionRadius = 2f

    override fun destroy() {
        node.physicalSelfDestruction(explosionRadius)
    }
}
