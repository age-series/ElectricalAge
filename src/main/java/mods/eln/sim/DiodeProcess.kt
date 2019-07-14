package mods.eln.sim

import mods.eln.sim.mna.component.ResistorSwitch

class DiodeProcess(internal var resistor: ResistorSwitch) : IProcess {

    override fun process(time: Double) {
        resistor.setState(resistor.getVoltage() > 0)
    }
}
