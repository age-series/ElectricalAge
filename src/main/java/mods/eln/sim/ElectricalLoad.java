package mods.eln.sim;

import mods.eln.sim.mna.component.Bipole;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Line;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageStateLineReady;

public class ElectricalLoad extends VoltageStateLineReady {

    public static final State groundLoad = null;

    private double serialResistance = MnaConst.highImpedance;

    public ElectricalLoad() {
    }

    public void setSerialResistance(double serialResistance) {
        if (this.serialResistance != serialResistance) {
            this.serialResistance = serialResistance;
            for (Component c : getConnectedComponents()) {
                if (c instanceof ElectricalConnection) {
                    ((ElectricalConnection) c).notifyRsChange();
                }
            }
        }
    }

    public double getSerialResistance() {
        return serialResistance;
    }

    public void highImpedance() {
        setSerialResistance(MnaConst.highImpedance);
    }

    public double getCurrent() {
        double current = 0;
        for (Component c : getConnectedComponents()) {
            if (c instanceof Bipole && (!(c instanceof Line)))
                current += Math.abs(((Bipole) c).getCurrent());
        }
        return current * 0.5;
    }
}
