package mods.eln.sixnode.electricalsource;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalSourceRender extends SixNodeElementRender {

    private ElectricalSourceDescriptor descriptor;

    double voltage = 0;
    private int color = 0;

    public ElectricalSourceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalSourceDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();

        front.glRotateOnX();

        descriptor.draw(voltage >= 25);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();

            color = (b >> 4) & 0xF;
            voltage = stream.readFloat();

            needRedrawCable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ElectricalSourceGui(this);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        if (descriptor.isSignalSource()) return Eln.signalCableDescriptor.render;
        if (voltage < Eln.lowVoltageCableDescriptor.electricalMaximalVoltage)
            return Eln.lowVoltageCableDescriptor.render;
        if (voltage < Eln.meduimVoltageCableDescriptor.electricalMaximalVoltage)
            return Eln.meduimVoltageCableDescriptor.render;
        if (voltage < Eln.highVoltageCableDescriptor.electricalMaximalVoltage)
            return Eln.highVoltageCableDescriptor.render;
        return Eln.veryHighVoltageCableDescriptor.render;
    }
}
