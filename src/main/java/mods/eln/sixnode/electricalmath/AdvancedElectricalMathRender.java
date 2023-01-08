package mods.eln.sixnode.electricalmath;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
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


public class AdvancedElectricalMathRender extends ElectricalMathRender{

    public AdvancedElectricalMathRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        isDrawingPins = false;
    }

    //todo the cables are rending inside the model
    @Override
    public void draw() {
        super.draw();

        float[] pinDistances = side.isY() ? front.rotate4PinDistances(descriptor.pinDistance) :  descriptor.pinDistance;
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

        if (side.isY()) {
            front.left().glRotateOnX();
        }

        descriptor.draw(interpolator.get(), ledOn);
    }

    //todo rewrite that, all sides is relative from A side.
    @Nullable
    @Override
    public CableRenderDescriptor getCableRender(@NotNull LRDU lrdu) {
        if (lrdu == front) return Eln.instance.signalCableDescriptor.render;

        //todo clean, this is dirty.
        Node node;
        try{
            node = this.tileEntity.getNode();
        } catch (Exception e){
            return Eln.instance.signalCableDescriptor.render;
        }

        AdvancedElectricalMathElement element = (AdvancedElectricalMathElement) ((SixNode) node).getElement(this.side);
        if (element != null) {
            for (int i : element.sideConnectionMask) {
                if (lrdu == front.left()) return i > 1 ? Eln.instance.highVoltageCableDescriptor.render : Eln.instance.signalCableDescriptor.render;
                else if (lrdu == front.inverse()) return Eln.instance.lowVoltageCableDescriptor.render;
                else if (lrdu == front.right()) return i > 1 ? Eln.instance.highVoltageCableDescriptor.render : Eln.instance.signalCableDescriptor.render;
            }
        }
        return Eln.instance.signalCableDescriptor.render;
    }
}
