package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public class WirelessSignalSourceRender extends SixNodeElementRender {

    WirelessSignalSourceDescriptor descriptor;

    RcInterpolator interpolator;

    String channel;
    boolean state = false;

    public WirelessSignalSourceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (WirelessSignalSourceDescriptor) descriptor;

        interpolator = new RcInterpolator(this.descriptor.render.speed);
    }

    @Override
    public void draw() {
        super.draw();
        descriptor.draw(interpolator.get(), UtilsClient.distanceFromClientPlayer(this.getTileEntity()), getTileEntity());
    }

    @Override
    public void refresh(float deltaT) {
        interpolator.setTarget((float) ((state ? 1 : 0)));
        interpolator.step(deltaT);
    }

    @Nullable
    @Override
    public CableRenderDescriptor getCableRender(@NotNull LRDU lrdu) {
        return Eln.instance.signalCableDescriptor.render;
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new WirelessSignalSourceGui(this);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            channel = stream.readUTF();
            state = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
