package mods.eln.transparentnode.solarpanel;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class SolarPanelPowerProcess implements IRootSystemPreStepProcess, INBTTReady {
    private final CurrentSource source;
    private final Resistor shunt;
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
        CurrentSource source,
        Resistor shunt,
        double openCircuitVoltage,
        double optimumVoltage,
        double optimumCurrent,
        double shortCircuitCurrent
    ) {
        this.source = source;
        this.shunt = shunt;
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
        double panelVoltage = Math.max(0.0, aPin.state - bPin.state);
        applyNortonEquivalent(panelVoltage);
    }

    private void applyNortonEquivalent(double voltage) {
        double light = Math.max(0.0, Math.min(1.0, lightFactor));
        if (light <= 0.0 || openCircuitVoltage <= 0.0 || shortCircuitCurrent <= 0.0) {
            source.setCurrent(0.0);
            shunt.highImpedance();
            return;
        }

        double voc = openCircuitVoltage * Math.max(0.05, 0.92 + 0.08 * light);
        double vmp = Math.min(optimumVoltage * Math.max(0.05, 0.92 + 0.08 * light), voc * 0.98);
        double imp = optimumCurrent * light;
        double isc = shortCircuitCurrent * light;

        double slope;
        double intercept;
        if (voltage <= vmp) {
            slope = (isc - imp) / Math.max(vmp, 1.0e-6);
            intercept = isc;
        } else {
            slope = imp / Math.max(voc - vmp, 1.0e-6);
            intercept = imp + slope * vmp;
        }

        source.setCurrent(Math.max(0.0, intercept));
        if (slope <= 1.0e-12) {
            shunt.highImpedance();
        } else {
            shunt.setResistance(Math.min(MnaConst.highImpedance, 1.0 / slope));
        }
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
