package mods.eln.transparentnode.solarpanel;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class SolarPanelPowerProcess implements IRootSystemPreStepProcess, INBTTReady {
    private final VoltageSource aSrc;
    private final VoltageSource bSrc;
    private final State aPin;
    private final State bPin;

    private double lightFactor;
    private final double openCircuitVoltage;
    private final double optimumVoltage;
    private final double optimumCurrent;
    private final double shortCircuitCurrent;

    public SolarPanelPowerProcess(
        State aPin,
        State bPin,
        VoltageSource aSrc,
        VoltageSource bSrc,
        double openCircuitVoltage,
        double optimumVoltage,
        double optimumCurrent,
        double shortCircuitCurrent
    ) {
        this.aSrc = aSrc;
        this.bSrc = bSrc;
        this.aPin = aPin;
        this.bPin = bPin;
        this.openCircuitVoltage = openCircuitVoltage;
        this.optimumVoltage = optimumVoltage;
        this.optimumCurrent = optimumCurrent;
        this.shortCircuitCurrent = shortCircuitCurrent;
    }

    public void setLightFactor(double lightFactor) {
        this.lightFactor = Math.max(0.0, Math.min(1.0, lightFactor));
    }

    public double getLightFactor() {
        return lightFactor;
    }

    public double getPower() {
        double voltage = Math.max(0.0, aPin.state - bPin.state);
        return voltage * getGeneratedCurrent(voltage);
    }

    public double getGeneratedCurrent(double voltage) {
        return currentAtVoltage(voltage, lightFactor, openCircuitVoltage, optimumVoltage, optimumCurrent, shortCircuitCurrent);
    }

    public static double currentAtVoltage(
        double voltage,
        double lightFactor,
        double openCircuitVoltage,
        double optimumVoltage,
        double optimumCurrent,
        double shortCircuitCurrent
    ) {
        double light = Math.max(0.0, Math.min(1.0, lightFactor));
        if (light <= 0.0 || openCircuitVoltage <= 0.0 || shortCircuitCurrent <= 0.0) return 0.0;

        double voc = openCircuitVoltage * Math.max(0.05, 0.92 + 0.08 * light);
        double vmp = Math.min(optimumVoltage * Math.max(0.05, 0.92 + 0.08 * light), voc * 0.98);
        double imp = optimumCurrent * light;
        double isc = shortCircuitCurrent * light;
        double v = Math.max(0.0, voltage);

        if (v >= voc) return 0.0;
        if (v <= vmp) {
            double slope = (isc - imp) / Math.max(vmp, 1.0e-6);
            return Math.max(0.0, isc - slope * v);
        }

        double slope = imp / Math.max(voc - vmp, 1.0e-6);
        return Math.max(0.0, imp - slope * (v - vmp));
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
        double theveninResistance = Math.max(0.0, a.resistance + b.resistance);
        double panelVoltage = solveOperatingVoltage(theveninVoltage, theveninResistance);
        double internalCurrent = (theveninVoltage - panelVoltage) / Math.max(theveninResistance, 1.0e-9);

        aSrc.setVoltage(a.voltage - internalCurrent * a.resistance);
        bSrc.setVoltage(b.voltage + internalCurrent * b.resistance);
    }

    private double solveOperatingVoltage(double theveninVoltage, double theveninResistance) {
        double voc = openCircuitVoltage * Math.max(0.05, 0.92 + 0.08 * lightFactor);
        if (lightFactor <= 0.0) return Math.max(0.0, theveninVoltage);
        if (theveninResistance >= MnaConst.highImpedance * 0.1) return voc;
        if (theveninResistance <= 1.0e-9) return Math.max(0.0, Math.min(voc, theveninVoltage));

        double low = 0.0;
        double high = voc;
        for (int idx = 0; idx < 32; idx++) {
            double mid = (low + high) * 0.5;
            double requiredCurrent = (mid - theveninVoltage) / theveninResistance;
            double generatedCurrent = getGeneratedCurrent(mid);
            if (generatedCurrent > requiredCurrent) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (low + high) * 0.5;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        setLightFactor(nbt.getDouble(str + "light"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setDouble(str + "light", lightFactor);
    }
}
