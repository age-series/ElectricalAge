package mods.eln.transparentnode.subtransmissionpole;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Obj3D;

public class SubTransmissionPoleDescriptor extends SubTransmissionPoleDescriptorBase {
    public SubTransmissionPoleDescriptor(String name, Obj3D obj, GhostGroup ghostGroup) {
        super(
            name,
            obj,
            ghostGroup,
            SubTransmissionPoleElement.class,
            SubTransmissionPoleRender.class,
            new String[] {
                "SubTransmissionPole.001",
                "Insulator.001",
                "Cross.001",
                "SubTransmissionPole",
                "Trans2",
                "FuseHolder",
                "Fuse",
                "Insulator",
                "Cross",
                "WoodPole"
            }
        );
    }
}
