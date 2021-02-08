package mods.eln.sim.electrical.process;

import mods.eln.sim.IProcess;
import mods.eln.sim.electrical.mna.component.ResistorSwitch;

public class DiodeProcess implements IProcess {

    ResistorSwitch resistor;

    public DiodeProcess(ResistorSwitch resistor) {
        this.resistor = resistor;
    }

    @Override
    public void process(double time) {
        resistor.setState(resistor.getU() > 0);
    }
}
