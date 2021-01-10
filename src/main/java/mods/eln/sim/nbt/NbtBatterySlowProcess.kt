package mods.eln.sim.nbt

import mods.eln.node.NodeBase
import mods.eln.sim.BatteryProcess
import mods.eln.sim.BatterySlowProcess
import mods.eln.sim.ThermalLoad

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
