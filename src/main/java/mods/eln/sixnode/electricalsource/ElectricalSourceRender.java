package mods.eln.sixnode.electricalsource;

import mods.eln.Eln;
import mods.eln.Vars;
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

    ElectricalSourceDescriptor descriptor;

    double voltage = 0, current = 0;
    int color = 0;

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
        if (descriptor.isSignalSource()) return Vars.signalCableDescriptor.render;
        if (voltage < Vars.lowVoltageCableDescriptor.electricalMaximalVoltage)
            return Vars.lowVoltageCableDescriptor.render;
        if (voltage < Vars.meduimVoltageCableDescriptor.electricalMaximalVoltage)
            return Vars.meduimVoltageCableDescriptor.render;
        if (voltage > Vars.highVoltageCableDescriptor.electricalMaximalVoltage)
            return Vars.highVoltageCableDescriptor.render;
        return Vars.veryHighVoltageCableDescriptor.render;
    }
}
