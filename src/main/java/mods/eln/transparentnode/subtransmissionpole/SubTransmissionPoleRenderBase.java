package mods.eln.transparentnode.subtransmissionpole;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

abstract class SubTransmissionPoleRenderBase extends TransparentNodeElementRender {

    private final SubTransmissionPoleDescriptorBase descriptor;

    protected SubTransmissionPoleRenderBase(TransparentNodeEntity entity, TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
        this.descriptor = (SubTransmissionPoleDescriptorBase) descriptor;
    }

    @Override
    public void draw() {
        descriptor.draw(front);
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }
}
