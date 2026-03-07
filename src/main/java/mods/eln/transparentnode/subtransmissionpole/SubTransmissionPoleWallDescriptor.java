package mods.eln.transparentnode.subtransmissionpole;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Obj3D;

public class SubTransmissionPoleWallDescriptor extends SubTransmissionPoleDescriptorBase {
    public SubTransmissionPoleWallDescriptor(String name, Obj3D obj, GhostGroup ghostGroup) {
        super(
            name,
            obj,
            ghostGroup,
            SubTransmissionPoleWallElement.class,
            SubTransmissionPoleWallRender.class,
            new String[] {"holder_main.001", "foot", "p0", "main", "g0"}
        );
    }
}
