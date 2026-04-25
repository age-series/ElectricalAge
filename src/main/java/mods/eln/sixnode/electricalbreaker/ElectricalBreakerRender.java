package mods.eln.sixnode.electricalbreaker;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Coordinate;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalBreakerRender extends SixNodeElementRender {

    SixNodeElementInventory inventory = new SixNodeElementInventory(0, 64, this);
    ElectricalBreakerDescriptor descriptor;
    long time;

    RcInterpolator interpol;

    float uMin, uMax;

    boolean boot = true;
    float switchAlpha = 0;
    public boolean switchState;

    CableRenderDescriptor cableRender;

    public ElectricalBreakerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalBreakerDescriptor) descriptor;
        time = System.currentTimeMillis();
        interpol = new RcInterpolator(this.descriptor.speed);
    }

    @Override
    public void draw() {
        super.draw();

        front.glRotateOnX();
        drawInternalPins();
        descriptor.draw(interpol.get(), UtilsClient.distanceFromClientPlayer(getTileEntity()));
    }

    @Override
    public void refresh(float deltaT) {
        interpol.setTarget(switchState ? 1f : 0f);
        interpol.step(deltaT);
    }

    @Nullable
    @Override
    public CableRenderDescriptor getCableRender(@NotNull LRDU lrdu) {
        CableRenderDescriptor adjacentRender = resolveAdjacentCableRender(lrdu);
        return adjacentRender != null ? adjacentRender : cableRender;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        Utils.println("Front : " + front);
        try {
            switchState = stream.readBoolean();
            uMax = stream.readFloat();
            uMin = stream.readFloat();
            cableRender = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (boot) {
            interpol.setValue(switchState ? 1f : 0f);
        }
        boot = false;
    }

    public void clientSetVoltageMin(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalBreakerElement.setVoltageMinId);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientSetVoltageMax(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalBreakerElement.setVoltageMaxId);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientToogleSwitch() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalBreakerElement.toogleSwitchId);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new ElectricalBreakerGui(player, inventory, this);
    }

    @Nullable
    private CableRenderDescriptor resolveAdjacentCableRender(@NotNull LRDU lrdu) {
        Direction worldDirection = side.applyLRDU(lrdu);
        Coordinate neighborCoordinate = new Coordinate(getTileEntity()).moved(worldDirection);
        if (!neighborCoordinate.getBlockExist()) return null;
        if (!(neighborCoordinate.world().getTileEntity(neighborCoordinate.x, neighborCoordinate.y, neighborCoordinate.z) instanceof SixNodeEntity)) {
            return null;
        }

        SixNodeEntity neighbor = (SixNodeEntity) neighborCoordinate.world().getTileEntity(
            neighborCoordinate.x,
            neighborCoordinate.y,
            neighborCoordinate.z
        );
        Direction neighborSide = worldDirection.getInverse();
        if (neighbor.elementRenderList[neighborSide.getInt()] == null) return null;

        for (LRDU neighborLrdu : LRDU.values()) {
            CableRenderDescriptor render = neighbor.elementRenderList[neighborSide.getInt()].getCableRender(neighborLrdu);
            if (render != null) return render;
        }
        return null;
    }

    private void drawInternalPins() {
        if (descriptor.pinDistance == null) {
            return;
        }
        drawPowerPin(LRDU.Left, descriptor.pinDistance);
        drawPowerPin(LRDU.Right, descriptor.pinDistance);
    }
}
