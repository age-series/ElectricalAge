package mods.eln.sixnode.signalinductor;

import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

public class SignalInductorDescriptor extends SixNodeDescriptor {

    ElectricalCableDescriptor cable;
    String descriptor;
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
