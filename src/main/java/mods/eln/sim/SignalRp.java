package mods.eln.sim;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.state.State;

public class SignalRp extends Resistor {
    public SignalRp(State aPin) {
        super(aPin, null);
        setR(Vars.SVU / Vars.SVII);
    }
}
