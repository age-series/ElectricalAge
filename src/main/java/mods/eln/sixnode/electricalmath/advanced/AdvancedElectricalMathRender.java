package mods.eln.sixnode.electricalmath.advanced;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AdvancedElectricalMathRender extends SixNodeElementRender {

    List<Gate.GateInfo> info = new ArrayList<>(16);

    int[] sideConnectionMask = new int[3];
    boolean isPowered = false;
    short isOverORUnderVoltage;
    double powerNeeded;

    PhysicalInterpolator interpolator = new PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f);
    Led[] leds;
    private float ledTime = 0;

    AdvancedElectricalMathDescriptor descriptor;
    Coordinate cord;

    public AdvancedElectricalMathRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (AdvancedElectricalMathDescriptor) descriptor;
        this.cord = new Coordinate(tileEntity);
        this.leds = this.descriptor.advanced_leds;
    }

    @Override
    public void draw() {
        float[] pins = Arrays.copyOf(descriptor.pinDistance,4);
        pins = side.isY() ? front.rotate4PinDistances(pins) :  pins;

        float busWidth = Eln.instance.signalBusCableDescriptor.render.width * 12;
        float busHeight = Eln.instance.signalBusCableDescriptor.render.height * 12;

        float powerWidth = Eln.instance.lowVoltageCableDescriptor.render.width * 16;
        float powerHeight = Eln.instance.lowVoltageCableDescriptor.render.height * 16;

        if (UtilsClient.distanceFromClientPlayer(tileEntity) < 15) {
            GL11.glColor3f(0, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front, pins, busWidth, busHeight);
            GL11.glColor3f(0, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front.inverse(), pins, powerWidth, powerHeight);
            GL11.glColor3f(1, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front.right(), pins, busWidth, busHeight);
            GL11.glColor3f(0, 0, 1);
            UtilsClient.drawConnectionPinSixNode(front.left(), pins, busWidth, busHeight);
            GL11.glColor3f(1, 1, 1);
        }

        //draw Cables offCenter
        {
            for (int i = 0; i < pins.length; i++) {
                pins[i] /= 16f;
            }
            drawOffCenter(null, pins);
        }

        if (side.isY()) {
            front.left().glRotateOnX();
        }

        switchLedsPower();
        descriptor.draw(interpolator.get());
    }

    @Override
    public void refresh(float deltaT) {
        if (isPowered) {
            ledTime += deltaT;

            if (ledTime > 0.4) {
                for (Led led : leds)
                    if (!led.isFixed) {
                        led.applyStrobeRandom(Math.random());
                    }
                ledTime = 0;
            }

        }

        if (!Utils.isPlayerAround(tileEntity.getWorldObj(), cord.getAxisAlignedBB(0)))
            interpolator.setTarget(0f);
        else
            interpolator.setTarget(1f);

        interpolator.step(deltaT);
    }

    private void switchLedsPower(){
        if (isPowered) {
            Led.applyConfigTo(leds, l -> l.isOn = true);
        } else {
            Led.applyConfigTo(leds, l -> l.isOn = false);
            return;
        }

        switch (isOverORUnderVoltage){
            case -1:{
                Led.applyConfigTo(leds, led -> {led.led_color = Led.color_yellow; led.isFixed = false;});
                Led.applyConfigTo(leds, 0, leds.length, 4, l -> {l.led_color = Led.color_red; l.isFixed = true;});
                break;
            }
            case 1:{
                Led.applyConfigTo(leds, led -> {led.led_color = Led.color_red; led.led_strobe_chance = 0.8f;});
                break;
            }
            default:{
                Led.applyConfigTo(leds, led -> {led.led_color = Led.color_green; led.isFixed = false;});
                Led.applyConfigTo(leds, 0, leds.length, 4, l -> {l.led_color = Led.color_red; l.isFixed = true;});
            }
        }
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            sideConnectionMask[0] = stream.readInt();
            sideConnectionMask[1] = stream.readInt();
            sideConnectionMask[2] = stream.readInt();

            info.clear();
            int size = stream.readInt();
            for (int i = 0; i < size; i++) {
                info.add(Gate.GateInfo.unSerialize(stream));
            }

            isPowered = stream.readBoolean();
            isOverORUnderVoltage = stream.readShort();
            powerNeeded = stream.readDouble();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(@Nullable DataInputStream stream) throws IOException {
        super.serverPacketUnserialize(stream);
        isPowered = stream.readBoolean();
        isOverORUnderVoltage = stream.readShort();
        powerNeeded = stream.readDouble();
    }

    @Nullable
    @Override
    public CableRenderDescriptor getCableRender(@NotNull LRDU lrdu) {
        if (lrdu == front.inverse()) return Eln.instance.lowVoltageCableDescriptor.render;

        if (lrdu == front) return this.sideConnectionMask[2] > 1 ? Eln.instance.signalBusCableDescriptor.render : Eln.instance.signalCableDescriptor.render;
        if (lrdu == front.left()) return this.sideConnectionMask[1] > 1 ? Eln.instance.signalBusCableDescriptor.render : Eln.instance.signalCableDescriptor.render;
        if (lrdu == front.right()) return this.sideConnectionMask[0] > 1 ? Eln.instance.signalBusCableDescriptor.render : Eln.instance.signalCableDescriptor.render;

        return null;
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new AdvancedElectricalMathGui(player, this);
    }
}
