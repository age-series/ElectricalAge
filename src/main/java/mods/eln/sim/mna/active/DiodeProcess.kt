package mods.eln.sim.mna.active

import mods.eln.sim.core.IProcess
import mods.eln.sim.mna.passive.ResistorSwitch

class DiodeProcess(internal var resistor: ResistorSwitch) : IProcess {

    override fun process(time: Double) {
        resistor.setState(resistor.getVoltage() > 0)
    }
}
