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

    private double r = MnaConst.highImpedance, rInv = 1 / MnaConst.highImpedance;


    public double getRInv() {
        return rInv;
    }

    public double getR() {
        return r;
    }

    public double getI() {
        return getCurrent();
    }

    public double getP() {
        return getU() * getCurrent();
    }

    public double getU() {
        return (aPin == null ? 0 : aPin.state) - (bPin == null ? 0 : bPin.state);
    }

    public Resistor setR(double r) {
        if (Double.isNaN(r) || Double.isInfinite(r)) {
            Utils.println("Error! Resistor cannot be set to " + r );
            // Call stack for debugging
            //new Throwable().printStackTrace();
            return this;
        }
        if (this.r != r) {
            this.r = r;
            this.rInv = 1 / r;
            dirty();
        }
        return this;
    }

    public void highImpedance() {
        setR(MnaConst.highImpedance);
    }

    public void ultraImpedance() {
        setR(MnaConst.ultraImpedance);
    }

    public Resistor pullDown() {
        setR(MnaConst.pullDown);
        return this;
    }

    boolean canBridge() {
        return false;
    }

    @Override
    public void applyTo(SubSystem s) {
        s.addToA(aPin, aPin, rInv);
        s.addToA(aPin, bPin, -rInv);
        s.addToA(bPin, bPin, rInv);
        s.addToA(bPin, aPin, -rInv);
    }

    @Override
    public double getCurrent() {
        return getU() * rInv;
    }
}
