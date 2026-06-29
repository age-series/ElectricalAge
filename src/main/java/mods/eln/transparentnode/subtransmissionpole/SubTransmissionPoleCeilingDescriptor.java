package mods.eln.transparentnode.subtransmissionpole;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Obj3D;

public class SubTransmissionPoleCeilingDescriptor extends SubTransmissionPoleDescriptorBase {
    public SubTransmissionPoleCeilingDescriptor(String name, Obj3D obj, GhostGroup ghostGroup) {
        super(
            name,
            obj,
            ghostGroup,
            SubTransmissionPoleCeilingElement.class,
            SubTransmissionPoleCeilingRender.class,
            new String[] {"foot", "p0", "main", "g0"}
        );
    }
}
