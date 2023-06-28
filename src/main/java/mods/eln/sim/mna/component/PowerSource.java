package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class PowerSource extends VoltageSource implements IRootSystemPreStepProcess, INBTTReady {

    String name;

    double power, maximumVoltage, maximumCurrent;

    public PowerSource(String name, State aPin) {
        super(name, aPin, null);
        this.name = name;
    }

    public void setPower(double P) {
        this.power = P;
    }

    void setMaximums(double Umax, double Imax) {
        this.maximumVoltage = Umax;
        this.maximumCurrent = Imax;
    }

    public void setMaximumCurrent(double maximumCurrent) {
        this.maximumCurrent = maximumCurrent;
    }

    public void setMaximumVoltage(double maximumVoltage) {
        this.maximumVoltage = maximumVoltage;
    }

    public double getPower() {
        return power;
    }

    @Override
    public void quitSubSystem() {
        getSubSystem().getRoot().removeProcess(this);
        super.quitSubSystem();
    }

    @Override
    public void addToSubsystem(SubSystem s) {
        super.addToSubsystem(s);
        getSubSystem().getRoot().addProcess(this);
        s.addProcess(this);
    }

    @Override
    public void rootSystemPreStepProcess() {
        SubSystem.Thevenin t = aPin.getSubSystem().getTh(aPin, this);

        double U = (Math.sqrt(t.voltage * t.voltage + 4 * power * t.resistance) + t.voltage) / 2;
        U = Math.min(Math.min(U, maximumVoltage), t.voltage + t.resistance * maximumCurrent);
        if (Double.isNaN(U)) U = 0;
        if (U < t.voltage) U = t.voltage;

        setVoltage(U);
    }

    public double getEffectivePower() {
        return getVoltage() * getCurrent();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        super.readFromNBT(nbt, str);

        str += name;

        setPower(nbt.getDouble(str + "P"));
        setMaximumVoltage(nbt.getDouble(str + "Umax"));
        setMaximumCurrent(nbt.getDouble(str + "Imax"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        super.writeToNBT(nbt, str);

        str += name;

        nbt.setDouble(str + "P", getPower());
        nbt.setDouble(str + "Umax", maximumVoltage);
        nbt.setDouble(str + "Imax", maximumCurrent);
    }
}
