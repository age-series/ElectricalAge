package mods.eln.sixnode.rs485cable;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.node.six.SixNodeDescriptor;

public class Rs485CableDescriptor extends SixNodeDescriptor {

    public CableRenderDescriptor render;

    public Rs485CableDescriptor(String name, CableRenderDescriptor render) {
        super(name, Rs485CableElement.class, Rs485CableRender.class);
        this.render = render;
    }
}
