package mods.eln.sixnode.electricalmath;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.UtilsClient;
import mods.eln.node.Node;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.util.Arrays;


public class AdvancedElectricalMathRender extends ElectricalMathRender{

    int[] sideConnectionMask = new int[2];

    public AdvancedElectricalMathRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
    }

    @Override
    public void draw() {
        float[] pinDistances;
        {
            float[] pins = Arrays.copyOf(descriptor.pinDistance,4);
            pins[2] -=1;
            pinDistances = side.isY() ? front.rotate4PinDistances(pins) :  pins;
        }

        float busWidth = Eln.instance.signalBusCableDescriptor.render.width * 12;
        float busHeight = Eln.instance.signalBusCableDescriptor.render.height * 12;

        float powerWidth = Eln.instance.lowVoltageCableDescriptor.render.width * 16;
        float powerHeight = Eln.instance.lowVoltageCableDescriptor.render.height * 16;

        if (UtilsClient.distanceFromClientPlayer(tileEntity) < 15) {
            GL11.glColor3f(0, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front, pinDistances, busWidth, busHeight);
            GL11.glColor3f(0, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front.inverse(), pinDistances, powerWidth, powerHeight);
            GL11.glColor3f(1, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front.right(), pinDistances, busWidth, busHeight);
            GL11.glColor3f(0, 0, 1);
            UtilsClient.drawConnectionPinSixNode(front.left(), pinDistances, busWidth, busHeight);
            GL11.glColor3f(1, 1, 1);
        }

        //draw Cables offCenter
        {
            for (int i = 0; i < pinDistances.length; i++) {
                pinDistances[i] /= 16;
            }
            drawOffCenter(null, pinDistances);
        }

        if (side.isY()) {
            front.left().glRotateOnX();
        }

        descriptor.draw(interpolator.get(), ledOn);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            sideConnectionMask[0] = stream.readInt();
            sideConnectionMask[1] = stream.readInt();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //todo rewrite that, all sides is relative from A side.
    @Nullable
    @Override
    public CableRenderDescriptor getCableRender(@NotNull LRDU lrdu) {
        if (lrdu == front) return Eln.instance.signalBusCableDescriptor.render;
        else if (lrdu == front.inverse()) return Eln.instance.lowVoltageCableDescriptor.render;

        if (lrdu == front.left()) return this.sideConnectionMask[1] > 1 ? Eln.instance.signalBusCableDescriptor.render : Eln.instance.signalCableDescriptor.render;
        else if (lrdu == front.right()) return this.sideConnectionMask[0] > 1 ? Eln.instance.signalBusCableDescriptor.render : Eln.instance.signalCableDescriptor.render;

        return Eln.instance.signalCableDescriptor.render;
    }
}
