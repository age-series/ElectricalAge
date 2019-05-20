package mods.eln.sixnode.diode;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class DiodeRender extends SixNodeElementRender {

    private DiodeDescriptor descriptor;

    double voltageAnode = 0, voltageCatode = 0, current = 0, temperature = 0;
    LRDU front;

    public DiodeRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (DiodeDescriptor) descriptor;
    }

    @Override
    public void draw() {
        front.glRotateOnX();
        descriptor.draw();
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();
            front = LRDU.fromInt((b >> 4) & 3);
            voltageAnode = stream.readShort() / NodeBase.NETWORK_SERIALIZE_U_FACTOR;
            voltageCatode = stream.readShort() / NodeBase.NETWORK_SERIALIZE_U_FACTOR;
            current = stream.readShort() / NodeBase.NETWORK_SERIALIZE_I_FACTOR;
            temperature = stream.readShort() / NodeBase.NETWORK_SERIALIZE_T_FACTOR;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
