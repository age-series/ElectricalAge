package mods.eln.sixnode.currentsource;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CurrentSourceElement extends SixNodeElement implements IConfigurable {

    NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    CurrentSource currentSource = new CurrentSource("currSrc", electricalLoad, null);

    public static final int setVoltageId = 1;

    public CurrentSourceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(electricalLoad);
        electricalComponentList.add(currentSource);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte b = nbt.getByte("color");
        currentSource.setCurrent(nbt.getDouble("current"));
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("current", currentSource.getCurrent());
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return electricalLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return NodeBase.maskElectricalPower;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(electricalLoad.getU(), currentSource.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", electricalLoad.getU()));
        info.put(I18N.tr("Current"), Utils.plotAmpere("", electricalLoad.getCurrent()));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Power"), Utils.plotPower("", electricalLoad.getU() * electricalLoad.getI()));
        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) currentSource.getCurrent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        Eln.applySmallRs(electricalLoad);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        return onBlockActivatedRotate(entityPlayer);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            currentSource.setCurrent(stream.readFloat());
            needPublish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("current")) {
            currentSource.setCurrent(compound.getDouble("current"));
            needPublish();
        }
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        compound.setDouble("current", currentSource.getCurrent());
    }
}
