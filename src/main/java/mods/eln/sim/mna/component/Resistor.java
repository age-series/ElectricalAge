package mods.eln.sim.mna.component;

import mods.eln.misc.Utils;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;

public class Resistor extends Bipole {

    public Resistor() {
    }

    public Resistor(State aPin, State bPin) {
        super(aPin, bPin);
    }

    private double resistance = MnaConst.highImpedance;
    private double resistanceInverse = 1 / MnaConst.highImpedance;


    public double getResistanceInverse() {
        return resistanceInverse;
    }

    public double getResistance() {
        return resistance;
    }

    public double getPower() {
        return getVoltage() * getCurrent();
    }

    public Resistor setResistance(double resistance) {
        if (Double.isNaN(resistance) || Double.isInfinite(resistance)) {
            Utils.println("Error! Resistor cannot be set to " + resistance);
            // Call stack for debugging
            //new Throwable().printStackTrace();
            return this;
        }
        if (this.resistance != resistance) {
            this.resistance = resistance;
            this.resistanceInverse = 1 / resistance;
            dirty();
        }
        return this;
    }

    public void highImpedance() {
        setResistance(MnaConst.highImpedance);
    }

    public void ultraImpedance() {
        setResistance(MnaConst.ultraImpedance);
    }

    public Resistor pullDown() {
        setResistance(MnaConst.pullDown);
        return this;
    }

    @Override
    public void applyToSubsystem(SubSystem s) {
        s.addToA(aPin, aPin, resistanceInverse);
        s.addToA(aPin, bPin, -resistanceInverse);
        s.addToA(bPin, bPin, resistanceInverse);
        s.addToA(bPin, aPin, -resistanceInverse);
    }

    @Override
    public double getCurrent() {
        return getVoltage() * resistanceInverse;
    }
}
