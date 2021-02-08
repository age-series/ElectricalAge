package mods.eln.sim.electrical.mna.component;

import mods.eln.sim.electrical.mna.state.State;
import mods.eln.sim.electrical.mna.state.VoltageState;

public abstract class Monopole extends Component {

    VoltageState pin;

    public Monopole connectTo(VoltageState pin) {
        this.pin = pin;
        if (pin != null) pin.add(this);
        return this;
    }

    @Override
    public State[] getConnectedStates() {
        return new State[]{pin};
    }
}
