package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;

public class Delay extends Bipole implements ISubSystemProcessI {

    double impedance, conductance;

    double oldIa, oldIb;

    public Delay setImpedance(double impedance) {
        this.impedance = impedance;
        this.conductance = 1 / impedance;
        return this;
    }

    @Override
    public void addToSubsystem(SubSystem s) {
        super.addToSubsystem(s);
        s.addProcess(this);
    }

    @Override
    public void applyToSubsystem(SubSystem s) {
        s.addToA(aPin, aPin, conductance);
        s.addToA(bPin, bPin, conductance);
    }

    @Override
    public void simProcessI(SubSystem s) {
        double iA = aPin.state * conductance + oldIa;
        double iB = bPin.state * conductance + oldIb;
        double iTarget = (iA - iB) / 2;

        double aPinI = iTarget - (aPin.state + bPin.state) * 0.5 * conductance;
        double bPinI = -iTarget - (aPin.state + bPin.state) * 0.5 * conductance;

        s.addToI(aPin, -aPinI);
        s.addToI(bPin, -bPinI);

        oldIa = aPinI;
        oldIb = bPinI;
    }

    @Override
    public double getCurrent() {
        return oldIa - oldIb;
    }
}
