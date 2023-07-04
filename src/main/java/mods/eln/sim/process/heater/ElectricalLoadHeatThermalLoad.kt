package mods.eln.sim.process.heater

import mods.eln.misc.Utils.println
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad

class ElectricalLoadHeatThermalLoad(var resistor: ElectricalLoad, var load: ThermalLoad) : IProcess {
    override fun process(time: Double) {
        if (resistor.isNotSimulated) return
        val current = resistor.current
        println("Moving heat: ${current * current * resistor.serialResistance * 2} watts at $resistor $load")
        load.movePowerTo(current * current * resistor.serialResistance * 2)
    }
}
