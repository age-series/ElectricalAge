package mods.eln.sixnode.electricalcable;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sixnode.genericcable.GenericCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalCableDescriptor extends GenericCableDescriptor {

    public double electricalNominalPowerDropFactor;
    public boolean signalWire;

    public double electricalRp = Double.POSITIVE_INFINITY;

    public double electricalRsPerCelcius = 0;

    public double dielectricBreakOhmPerVolt = 0;
    public double dielectricBreakOhm = Double.POSITIVE_INFINITY;
    public double dielectricVoltage = Double.POSITIVE_INFINITY;
    public double dielectricBreakOhmMin = Double.POSITIVE_INFINITY;

    String description = "todo cable";

    public ElectricalCableDescriptor(String name, CableRenderDescriptor render, String description, boolean signalWire) {
        super(name, ElectricalCableElement.class, ElectricalCableRender.class);
        thermalRp = 1;
        thermalRs = 1;
        thermalC = 1;
        this.description = description;
        this.render = render;
        this.signalWire = signalWire;
        this.thermalWarmLimit = 100;
        this.thermalCoolLimit = -100;

        this.thermalWarmLimit = Eln.cableWarmLimit;
        this.thermalCoolLimit = -10;

        Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);

        if (signalWire) {
            setPhysicalConstantLikeNormalCable(
                Eln.SVU,
                Eln.SVP,
                0.02 / 50 * Eln.gateOutputCurrent / Eln.SVII,
                Eln.SVU * 1.3,
                Eln.SVP * 1.2,
                0.5
            );
        }
    }

    public void setPhysicalConstantLikeNormalCable(
        double electricalNominalVoltage, double electricalNominalPower, double electricalNominalPowerDropFactor,
        double electricalMaximalVoltage, double electricalMaximalPower,
        double electricalOverVoltageStartPowerLost) {
        this.electricalNominalVoltage = electricalNominalVoltage;
        this.electricalNominalPower = electricalNominalPower;
        this.electricalNominalPowerDropFactor = electricalNominalPowerDropFactor;
        this.electricalMaximalVoltage = electricalMaximalVoltage;
        electricalRp = MnaConst.highImpedance;
        double electricalNorminalI = electricalNominalPower / electricalNominalVoltage;
        electricalRs = (electricalNominalPower * electricalNominalPowerDropFactor) / electricalNorminalI / electricalNorminalI / 2;
        Utils.println("Cable Resistance for the " + name + ": " + electricalRs);

        electricalNominalPower = electricalMaximalPower / electricalNominalVoltage;
        double thermalMaximalPowerDissipated = electricalNominalPower * electricalNominalPower * electricalRs * 2;
        thermalC = thermalMaximalPowerDissipated * Eln.cableHeatingTime / (this.thermalWarmLimit);
        thermalRp = this.thermalWarmLimit / thermalMaximalPowerDissipated;
        thermalRs = 0.5 / thermalC / 2;

        electricalRsPerCelcius = 0;

        dielectricBreakOhmPerVolt = 0.95;
        dielectricBreakOhm = electricalMaximalVoltage * electricalMaximalVoltage / electricalOverVoltageStartPowerLost;
        dielectricVoltage = electricalMaximalVoltage;
        dielectricBreakOhmMin = dielectricBreakOhm;

        this.electricalMaximalCurrent = electricalMaximalPower / electricalNominalVoltage;

        if (this.electricalNominalVoltage > 4000.0) {
            voltageLevelColor = VoltageLevelColor.Grid;
        } else {
            voltageLevelColor = VoltageLevelColor.fromCable(this);
        }
    }

    public void applyTo(ElectricalLoad electricalLoad, double rsFactor) {
        electricalLoad.setRs(electricalRs * rsFactor);
    }

    public void applyTo(ElectricalLoad electricalLoad) {
        applyTo(electricalLoad, 1);
    }

    public void applyTo(Resistor resistor) {
        applyTo(resistor, 1);
    }

    public void applyTo(Resistor resistor, double factor) {
        resistor.setR(electricalRs * factor);
    }

    public void applyTo(ThermalLoad thermalLoad) {
        thermalLoad.Rs = this.thermalRs;
        thermalLoad.C = this.thermalC;
        thermalLoad.Rp = this.thermalRp;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        if (signalWire) {
            Collections.addAll(list, tr("Cable is adapted to conduct\nelectrical signals.").split("\n"));
            Collections.addAll(list, tr("A signal is electrical information\nwhich must be between 0V and %1$", Utils.plotVolt(Eln.SVU)).split("\n"));
            list.add(tr("Not adapted to transport power."));

        } else {
            list.add(tr("Nominal Ratings:"));
            list.add("  " + tr("Voltage: %1$V", Utils.plotValue(electricalNominalVoltage)));
            list.add("  " + tr("Current: %1$A", Utils.plotValue(electricalNominalPower / electricalNominalVoltage)));
            list.add("  " + tr("Power: %1$W", Utils.plotValue(electricalNominalPower)));
            list.add("  " + tr("Serial resistance: %1$\u2126", Utils.plotValue(electricalRs * 2)));
        }
    }

    @Override
    public int getNodeMask() {
        if (signalWire)
            return NodeBase.maskElectricalGate;
        else
            return NodeBase.maskElectricalPower;
    }

    public void bindCableTexture() {
        this.render.bindCableTexture();
    }
}
