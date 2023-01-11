package mods.eln.sixnode.electricalmath;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalMathRender extends SixNodeElementRender {

    ElectricalMathDescriptor descriptor;
    Coordinate coord;
    PhysicalInterpolator interpolator;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    String expression;

    public int redstoneRequired;
    public boolean equationIsValid;

    float ledTime = 0f;
    boolean[] ledOn = new boolean[8];

    public ElectricalMathRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalMathDescriptor) descriptor;
        interpolator = new PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f);
        coord = new Coordinate(tileEntity);
        ledOn[0] = true;
        ledOn[4] = true;
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new ElectricalMathGui(player, inventory, this);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            expression = stream.readUTF();
            redstoneRequired = stream.readInt();
            equationIsValid = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw() {
        super.draw();

        float[] pinDistances = null;
        if (side.isY()) {
            pinDistances = front.rotate4PinDistances(descriptor.pinDistance);
        } else {
            pinDistances = descriptor.pinDistance;
        }

        if (UtilsClient.distanceFromClientPlayer(tileEntity) < 15) {
            GL11.glColor3f(0, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front, pinDistances, 1.8f, 1.35f);
            GL11.glColor3f(1, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front.right(), pinDistances, 1.8f, 1.35f);
            GL11.glColor3f(0, 1, 0);
            UtilsClient.drawConnectionPinSixNode(front.inverse(), pinDistances, 1.8f, 1.35f);
            GL11.glColor3f(0, 0, 1);
            UtilsClient.drawConnectionPinSixNode(front.left(), pinDistances, 1.8f, 1.35f);
            GL11.glColor3f(1, 1, 1);
        }

        if (side.isY()) {
            front.left().glRotateOnX();
        }

        descriptor.draw(interpolator.get(), ledOn);
    }

    @Override
    public void refresh(float deltaT) {
        ledTime += deltaT;

        if (ledTime > 0.4) {
            for (int idx = 1; idx <= 3; idx++) {
                ledOn[idx] = Math.random() < 0.3;
            }
            for (int idx = 5; idx <= 7; idx++) {
                ledOn[idx] = Math.random() < 0.3;
            }
            ledTime = 0;
        }

        if (!Utils.isPlayerAround(tileEntity.getWorldObj(), coord.getAxisAlignedBB(0)))
            interpolator.setTarget(0f);
        else
            interpolator.setTarget(1f);

        interpolator.step(deltaT);
    }

    @Nullable
    @Override
    public CableRenderDescriptor getCableRender(@NotNull LRDU lrdu) {
        return Eln.instance.signalCableDescriptor.render;
    }
}
