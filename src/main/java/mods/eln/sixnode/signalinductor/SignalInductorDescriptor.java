package mods.eln.sixnode.signalinductor;

import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.mna.state.ElectricalLoad;
import mods.eln.sim.mna.passive.Inductor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

public class SignalInductorDescriptor extends SixNodeDescriptor {

    ElectricalCableDescriptor cable;
    public double henri;

    public SignalInductorDescriptor(String name, double henri, ElectricalCableDescriptor cable) {
        super(name, SignalInductorElement.class, SignalInductorRender.class);
        this.henri = henri;
        this.cable = cable;
    }

    public void applyTo(ElectricalLoad load) {
        cable.applyTo(load);
    }

    public void applyTo(Inductor inductor) {
        inductor.setL(henri);
    }
}
