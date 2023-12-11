package mods.eln.transparentnode.electricalfurnace;

import mods.eln.Eln;
import mods.eln.client.FrameTime;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalFurnaceRender extends TransparentNodeElementRender {

    TransparentNodeElementInventory inventory = new ElectricalFurnaceInventory(5, 64, this);

    public float temperature = 0;
    public boolean powerOn, heatingCorpOn;
    //float temperatureTarget;
    EntityItem entityItemIn = null;

    long time;

    float processState, processStatePerSecond;

    float counter = 0;

    short heatingCorpResistorP = 0;

    public boolean temperatureTargetSyncNew = false;
    public float temperatureTargetSyncValue = -1234;

    public boolean autoShutDown;

    float voltage;

    public ElectricalFurnaceRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        time = System.currentTimeMillis();
    }

    @Override
    public void draw() {
        front.glRotateXnRef();

        Eln.obj.draw("ElectricFurnace", "furnace");
        //ClientProxy.obj.draw("ELFURNACE");

        drawEntityItem(entityItemIn, -0.1, -0.20, 0, counter, 0.8f);
    }


    @Override
    public void refresh(float deltaT) {
        processState += processStatePerSecond * FrameTime.getNotCaped2();
        if (processState > 1f) processState = 1f;
        counter += (System.currentTimeMillis() - time) * 0.001 * 360 / 4;
        if (counter > 360) counter -= 360;

        time = System.currentTimeMillis();
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new ElectricalFurnaceGuiDraw(player, inventory, this);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);

        short read;

        try {
            Byte b;

            b = stream.readByte();

            powerOn = (b & 1) != 0;
            heatingCorpOn = (b & 2) != 0;

            float temperatureTargetIncoming = stream.readShort();

            if (temperatureTargetIncoming != temperatureTargetSyncValue) {
                temperatureTargetSyncValue = temperatureTargetIncoming;
                temperatureTargetSyncNew = true;
            }

            temperature = stream.readShort();

            if ((read = stream.readShort()) == -1) {
                entityItemIn = null;
                stream.readShort();
            } else {
                entityItemIn = new EntityItem(getTileEntity().getWorldObj(), getTileEntity().xCoord + 0.5, getTileEntity().yCoord + 0.5, getTileEntity().zCoord + 1.2, Utils.newItemStack(read, 1, stream.readShort()));
            }

            heatingCorpResistorP = stream.readShort();
            voltage = stream.readFloat();
            processState = stream.readFloat();
            processStatePerSecond = stream.readFloat();

            autoShutDown = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientSetPowerOn(boolean value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalFurnaceElement.unserializePowerOnId);
            stream.writeByte(value ? 1 : 0);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientSetTemperatureTarget(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalFurnaceElement.unserializeTemperatureTarget);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getPowerOn() {
        return powerOn;
    }
}
