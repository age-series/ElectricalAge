package mods.eln.sim.mna.component;

import mods.eln.sim.mna.state.State;

public abstract class Bipole extends Component {

    public State aPin, bPin;

    public Bipole() {}

    public Bipole(State aPin, State bPin) {
        connectTo(aPin, bPin);
    }


    public Bipole connectTo(State aPin, State bPin) {
        breakConnection();

        this.aPin = aPin;
        this.bPin = bPin;

        if (aPin != null) aPin.addComponent(this);
        if (bPin != null) bPin.addComponent(this);
        return this;
    }

    public Bipole connectGhostTo(State aPin, State bPin) {
        breakConnection();

        this.aPin = aPin;
        this.bPin = bPin;
        return this;
    }

    @Override
    public void breakConnection() {
        if (aPin != null) aPin.removeComponent(this);
        if (bPin != null) bPin.removeComponent(this);
    }

    @Override
    public State[] getConnectedStates() {
        return new State[]{aPin, bPin};
    }

    public abstract double getCurrent();

    public double getVoltage() {
        return (aPin == null ? 0 : aPin.state) - (bPin == null ? 0 : bPin.state);
    }


    public String toString() {
        return "[" + aPin + " " + this.getClass().getSimpleName() + " " + bPin + "]";
    }
}
