package mods.eln.sixnode.fibercable;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

public class FiberCableRender extends SixNodeElementRender {

    FiberCableDescriptor descriptor;

    public FiberCableRender(SixNodeEntity entity, Direction side, SixNodeDescriptor descriptor) {
        super(entity, side, descriptor);
        this.descriptor = (FiberCableDescriptor) descriptor;
    }

    @Override
    public void draw() {
        UtilsClient.bindTexture(descriptor.render.cableTexture);
        glListCall();
    }

    @Override
    public void glListDraw() {
        CableRender.drawCable(descriptor.render, connectedSide, CableRender.connectionType(this, side));
        CableRender.drawNode(descriptor.render, connectedSide, CableRender.connectionType(this, side));
    }

    @Override
    public boolean glListEnable() {
        return true;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return descriptor.render;
    }
}
