package mods.eln.transparentnode.electricalantennatx;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;

public class ElectricalAntennaTxRender extends TransparentNodeElementRender {

    ElectricalAntennaTxDescriptor descriptor;

    LRDUMask maskTemp = new LRDUMask();
    LRDU rot;

    LRDUMask lrduConnection = new LRDUMask();
    CableRenderType connectionType;
    boolean cableRefresh = false;

    public ElectricalAntennaTxRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (ElectricalAntennaTxDescriptor) descriptor;
    }

    @Override
    public void draw() {
        GL11.glPushMatrix();
        front.glRotateXnRef();
        rot.glRotateOnX();
        descriptor.draw();
        GL11.glPopMatrix();

        glCableTransform(front.getInverse());
        descriptor.cable.bindCableTexture();

        if (cableRefresh) {
            cableRefresh = false;
            connectionType = CableRender.connectionType(getTileEntity(), lrduConnection, front.getInverse());
        }

        for (LRDU lrdu : LRDU.values()) {
            Utils.setGlColorFromDye(connectionType.otherdry[lrdu.toInt()]);
            if (!lrduConnection.get(lrdu)) continue;
            maskTemp.set(1 << lrdu.toInt());
            if (lrdu == rot)
                CableRender.drawCable(descriptor.cable.render, maskTemp, connectionType);
            else if (lrdu == rot.left() || lrdu == rot.right())
                CableRender.drawCable(Eln.instance.signalCableDescriptor.render, maskTemp, connectionType);
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        rot = LRDU.deserialize(stream);
        lrduConnection.deserialize(stream);
        cableRefresh = true;
    }

    @Override
    public CableRenderDescriptor getCableRenderSide(Direction side, LRDU lrdu) {
        if (front.getInverse() != side.applyLRDU(lrdu)) return null;

        if (side == front.applyLRDU(rot)) return descriptor.cable.render;
        if (side == front.applyLRDU(rot.left())) return Eln.instance.signalCableDescriptor.render;
        if (side == front.applyLRDU(rot.right())) return Eln.instance.signalCableDescriptor.render;
        return null;
    }

    @Override
    public void notifyNeighborSpawn() {
        super.notifyNeighborSpawn();
        cableRefresh = true;
    }
}
