package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;

public class Capacitor extends Bipole implements ISubSystemProcessI {

    private double coulombs = 0;
    double coulombsPerStep;

    public Capacitor() {}

    public Capacitor(State aPin, State bPin) {
        connectTo(aPin, bPin);
    }

    @Override
    public double getCurrent() {
        return 0;
    }

    public void setCoulombs(double coulombs) {
        this.coulombs = coulombs;
        dirty();
    }

    @Override
    public void applyToSubsystem(SubSystem s) {
        coulombsPerStep = coulombs / s.getDt();

        s.addToA(aPin, aPin, coulombsPerStep);
        s.addToA(aPin, bPin, -coulombsPerStep);
        s.addToA(bPin, bPin, coulombsPerStep);
        s.addToA(bPin, aPin, -coulombsPerStep);
    }

    @Override
    public void simProcessI(SubSystem s) {
        double add = (s.getXSafe(aPin) - s.getXSafe(bPin)) * coulombsPerStep;
        s.addToI(aPin, add);
        s.addToI(bPin, -add);
    }

    @Override
    public void quitSubSystem() {
        subSystem.removeProcess(this);
        super.quitSubSystem();
    }

    @Override
    public void addToSubsystem(SubSystem s) {
        super.addToSubsystem(s);
        s.addProcess(this);
    }

    /**
     * getEnergy
     * (V^2 * C) / 2 = E
     *
     * @return energy, in joules
     */
    public double getEnergy() {
        double voltage = getVoltage();
        return voltage * voltage * coulombs / 2;
    }

    public double getCoulombs() {
        return coulombs;
    }
}
