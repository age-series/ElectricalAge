package mods.eln.sim.mna.process;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class PowerSourceBipole implements IRootSystemPreStepProcess, INBTTReady {

    private final VoltageSource aSrc;
    private final VoltageSource bSrc;
    private final State aPin;
    private final State bPin;

    double power;
    double maximumVoltage;
    double maximumCurrent;

    public PowerSourceBipole(State aPin, State bPin, VoltageSource aSrc, VoltageSource bSrc) {
        this.aSrc = aSrc;
        this.bSrc = bSrc;
        this.aPin = aPin;
        this.bPin = bPin;
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
    public void rootSystemPreStepProcess() {
        SubSystem.Thevenin a = aPin.getSubSystem().getTh(aPin, aSrc);
        SubSystem.Thevenin b = bPin.getSubSystem().getTh(bPin, bSrc);
        if (Double.isNaN(a.voltage)) {
            a.voltage = 0.0;
            a.resistance = MnaConst.highImpedance;
        }
        if (Double.isNaN(b.voltage)) {
            b.voltage = 0.0;
            b.resistance = MnaConst.highImpedance;
        }
        double theveninVoltage = a.voltage - b.voltage;
        double theveninResistance = a.resistance + b.resistance;
        if (theveninVoltage >= maximumVoltage) {
            aSrc.setVoltage(a.voltage);
            bSrc.setVoltage(b.voltage);
        } else {
            double voltage = (Math.sqrt(theveninVoltage * theveninVoltage + 4 * power * theveninResistance) + theveninVoltage) / 2;
            voltage = Math.min(Math.min(voltage, maximumVoltage), theveninVoltage + theveninResistance * maximumCurrent);
            if (Double.isNaN(voltage)) voltage = 0;

            double I = (theveninVoltage - voltage) / theveninResistance;
            aSrc.setVoltage(a.voltage - I * a.resistance);
            bSrc.setVoltage(b.voltage + I * b.resistance);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        setPower(nbt.getDouble(str + "P"));
        setMaximumVoltage(nbt.getDouble(str + "Umax"));
        setMaximumCurrent(nbt.getDouble(str + "Imax"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setDouble(str + "P", getPower());
        nbt.setDouble(str + "Umax", maximumVoltage);
        nbt.setDouble(str + "Imax", maximumCurrent);
    }
}
