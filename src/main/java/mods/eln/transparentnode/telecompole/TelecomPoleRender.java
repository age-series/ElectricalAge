package mods.eln.transparentnode.telecompole;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

public class TelecomPoleRender extends TransparentNodeElementRender {

    TelecomPoleDescriptor descriptor;

    public TelecomPoleRender(TransparentNodeEntity entity, TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
        this.descriptor = (TelecomPoleDescriptor) descriptor;
    }

    @Override
    public void draw() {
        descriptor.draw();
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }
}
