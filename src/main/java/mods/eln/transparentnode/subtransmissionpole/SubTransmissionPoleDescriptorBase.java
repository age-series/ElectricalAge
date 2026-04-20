package mods.eln.transparentnode.subtransmissionpole;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementRender;

import java.util.ArrayList;
import java.util.List;

abstract class SubTransmissionPoleDescriptorBase extends TransparentNodeDescriptor {
    private final List<Obj3D.Obj3DPart> parts = new ArrayList<Obj3D.Obj3DPart>();

    protected SubTransmissionPoleDescriptorBase(
        String name,
        Obj3D obj,
        GhostGroup ghostGroup,
        Class<? extends TransparentNodeElement> elementClass,
        Class<? extends TransparentNodeElementRender> renderClass,
        String[] partNames
    ) {
        super(name, elementClass, renderClass);
        this.ghostGroup = ghostGroup;
        for (String partName : partNames) {
            Obj3D.Obj3DPart part = obj.getPart(partName);
            if (part != null) {
                parts.add(part);
            }
        }
    }

    public void draw(Direction front) {
        front.glRotateZnRef();
        for (Obj3D.Obj3DPart part : parts) {
            part.draw();
        }
    }
}
