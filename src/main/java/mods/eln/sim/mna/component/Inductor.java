package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.CurrentState;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class Inductor extends Bipole implements ISubSystemProcessI, INBTTReady {

    String name;

    private double inductance = 0;
    double inductancePerStep;

    private final CurrentState currentState = new CurrentState();

    public Inductor(String name) {
        this.name = name;
    }

    public Inductor(String name, State aPin, State bPin) {
        super(aPin, bPin);
        this.name = name;
    }

    @Override
    public double getCurrent() {
        return currentState.state;
    }

    public double getInductance() {
        return inductance;
    }

    public void setInductance(double inductance) {
        this.inductance = inductance;
        dirty();
    }

    /**
     * getEnergy
     * (I^2 * inductance) / 2
     *
     * @return Energy, in joules
     */
    public double getEnergy() {
        final double current = getCurrent();
        return current * current * inductance / 2;
    }

    @Override
    public void applyToSubsystem(SubSystem s) {
        inductancePerStep = -inductance / s.getDt();

        s.addToA(aPin, currentState, 1);
        s.addToA(bPin, currentState, -1);
        s.addToA(currentState, aPin, 1);
        s.addToA(currentState, bPin, -1);
        s.addToA(currentState, currentState, inductancePerStep);
    }

    @Override
    public void simProcessI(SubSystem s) {
        s.addToI(currentState, inductancePerStep * currentState.state);
    }

    @Override
    public void quitSubSystem() {
        subSystem.states.remove(getCurrentState());
        subSystem.removeProcess(this);
        super.quitSubSystem();
    }

    @Override
    public void addToSubsystem(SubSystem s) {
        super.addToSubsystem(s);
        s.addState(getCurrentState());
        s.addProcess(this);
    }

    public CurrentState getCurrentState() {
        return currentState;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        str += name;
        currentState.state = (nbt.getDouble(str + "Istate"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        str += name;
        nbt.setDouble(str + "Istate", currentState.state);
    }

    public void resetStates() {
        currentState.state = 0;
    }
}
