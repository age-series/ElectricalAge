package mods.eln.sixnode.electricalredstoneoutput;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalRedstoneOutputElement extends SixNodeElement {

    public NbtElectricalGateInput inputGate = new NbtElectricalGateInput("inputGate");
    public ElectricalRedstoneOutputSlowProcess slowProcess = new ElectricalRedstoneOutputSlowProcess(this);

    int redstoneValue = 0;

    public ElectricalRedstoneOutputElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(inputGate);
        slowProcessList.add(slowProcess);
    }

    @Override
    public int isProvidingWeakPower() {
        return redstoneValue;
    }

    public boolean refreshRedstone() {
        int newValue = (int) (inputGate.getVoltage() * 15.0 / Eln.SVU + 0.5);
        if (newValue != redstoneValue) {
            redstoneValue = newValue;
            notifyNeighbor();
            needPublish();
            return true;
        }
        return false;
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        redstoneValue = nbt.getInteger("redstoneValue");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) (front.toInt() << 0));
        nbt.setInteger("redstoneValue", redstoneValue);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (front == lrdu.left()) return inputGate;
        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu.left()) return NodeBase.maskElectricalInputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U:", inputGate.getVoltage()) + Utils.plotAmpere("I:", inputGate.getCurrent());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Redstone value"), Utils.plotValue(redstoneValue));
        info.put(I18N.tr("Input voltage"), Utils.plotVolt("", inputGate.getVoltage()));
        return info;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(redstoneValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
    }
}
