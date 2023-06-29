package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.CurrentState;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;


public class VoltageSource extends Bipole implements ISubSystemProcessI, INBTTReady {

    String name;

    double voltage = 0;
    private final CurrentState currentState = new CurrentState();

    public VoltageSource(String name) {
        this.name = name;
    }

    public VoltageSource(String name, State aPin, State bPin) {
        super(aPin, bPin);
        this.name = name;
    }

    public VoltageSource setVoltage(double voltage) {
        this.voltage = voltage;
        return this;
    }

    public double getVoltage() {
        return voltage;
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

    @Override
    public void applyToSubsystem(SubSystem s) {
        s.addToA(aPin, getCurrentState(), 1.0);
        s.addToA(bPin, getCurrentState(), -1.0);
        s.addToA(getCurrentState(), aPin, 1.0);
        s.addToA(getCurrentState(), bPin, -1.0);
    }

    @Override
    public void simProcessI(SubSystem s) {
        s.addToI(getCurrentState(), voltage);
    }

    @Override
    public double getCurrent() {
        return -getCurrentState().state;
    }

    public CurrentState getCurrentState() {
        return currentState;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        str += name;
        setVoltage(nbt.getDouble(str + "U"));
        currentState.state = (nbt.getDouble(str + "Istate"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        str += name;
        nbt.setDouble(str + "U", voltage);
        nbt.setDouble(str + "Istate", currentState.state);
    }

    public double getPower() {
        return getVoltage() * getCurrent();
    }
}
