package mods.eln.sixnode.electricalrelay;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalRelayRender extends SixNodeElementRender {

    SixNodeElementInventory inventory = new SixNodeElementInventory(0, 64, this);
    ElectricalRelayDescriptor descriptor;
    long time;

    RcInterpolator interpolator;

    boolean boot = true;
    float switchAlpha = 0;
    public boolean switchState, defaultOutput;

    public ElectricalRelayRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalRelayDescriptor) descriptor;
        time = System.currentTimeMillis();
        interpolator = new RcInterpolator(this.descriptor.speed);
    }

    @Override
    public void draw() {
        super.draw();
        //UtilsClient.enableDepthTest();
        drawSignalPin(front, new float[]{2.5f, 2.5f, 2.5f, 2.5f});
        front.glRotateOnX();

        descriptor.draw(interpolator.get());
    }

    @Override
    public void refresh(float deltaT) {
        interpolator.step(deltaT);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            switchState = stream.readBoolean();
            defaultOutput = stream.readBoolean();

            interpolator.setTarget(switchState ? 1f : 0f);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (boot) {
            interpolator.setValueFromTarget();
        }
        boot = false;
    }

    public void clientToogleDefaultOutput() {
        clientSend(ElectricalRelayElement.toogleOutputDefaultId);
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new ElectricalRelayGui(player, this);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        if (lrdu == front) return Eln.instance.signalCableDescriptor.render;
        if (lrdu == front.left() || lrdu == front.right()) {
            if (descriptor.cable != null) return descriptor.cable.render;
            return getContactCableRender();
        }
        return null;
    }

    private CableRenderDescriptor getContactCableRender() {
        double voltage = descriptor.contactNominalVoltage;
        if (voltage < Eln.instance.lowVoltageCableDescriptor.electricalMaximalVoltage)
            return Eln.instance.lowVoltageCableDescriptor.render;
        if (voltage < Eln.instance.meduimVoltageCableDescriptor.electricalMaximalVoltage)
            return Eln.instance.meduimVoltageCableDescriptor.render;
        if (voltage < Eln.instance.highVoltageCableDescriptor.electricalMaximalVoltage)
            return Eln.instance.highVoltageCableDescriptor.render;
        return Eln.instance.veryHighVoltageCableDescriptor.render;
    }
}
