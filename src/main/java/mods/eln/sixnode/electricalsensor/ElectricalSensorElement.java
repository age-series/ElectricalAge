package mods.eln.sixnode.electricalsensor;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.ConfigCopyToolDescriptor;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricaldatalogger.DataLogs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalSensorElement extends SixNodeElement implements IConfigurable {

    VoltageStateWatchDog voltageWatchDog = new VoltageStateWatchDog();
    //ResistorCurrentWatchdog currentWatchDog = new ResistorCurrentWatchdog();

    public ElectricalSensorDescriptor descriptor;
    public NbtElectricalLoad aLoad, bLoad;
    public NbtElectricalLoad outputGate = new NbtElectricalLoad("outputGate");
    public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
    public ElectricalSensorProcess slowProcess = new ElectricalSensorProcess(this);

    public Resistor resistor;

    private AutoAcceptInventoryProxy inventory = (new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this)))
        .acceptIfEmpty(0, ElectricalCableDescriptor.class);

    static final byte dirNone = 0, dirAB = 1, dirBA = 2;
    byte dirType = dirNone;
    public static final byte powerType = 0, currantType = 1, voltageType = 2;
    int typeOfSensor = voltageType;
    float lowValue = 0, highValue = 50;

    public static final byte setTypeOfSensorId = 1;
    public static final byte setValueId = 2;
    public static final byte setDirType = 3;

    public ElectricalSensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (ElectricalSensorDescriptor) descriptor;

        aLoad = new NbtElectricalLoad("aLoad");
        electricalLoadList.add(aLoad);
        WorldExplosion exp = new WorldExplosion(this).cableExplosion();

        if (!this.descriptor.voltageOnly) {
            bLoad = new NbtElectricalLoad("bLoad");
            resistor = new Resistor(aLoad, bLoad);
            electricalLoadList.add(bLoad);
            electricalComponentList.add(resistor);

            //	slowProcessList.add(currentWatchDog);
            //	currentWatchDog.set(resistor).set(exp);

        }
        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);
        electricalProcessList.add(slowProcess);

        slowProcessList.add(voltageWatchDog);
        voltageWatchDog.set(aLoad).set(exp);
    }

    public IInventory getInventory() {
        if (inventory != null)
            return inventory.getInventory();
        else
            return null;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        typeOfSensor = nbt.getByte("typeOfSensor");
        lowValue = nbt.getFloat("lowValue");
        highValue = nbt.getFloat("highValue");
        dirType = nbt.getByte("dirType");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) (front.toInt() << 0));
        nbt.setByte("typeOfSensor", (byte) typeOfSensor);
        nbt.setFloat("lowValue", lowValue);
        nbt.setFloat("highValue", highValue);
        nbt.setByte("dirType", dirType);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (!descriptor.voltageOnly) {
            if (front.left() == lrdu) return aLoad;
            if (front.right() == lrdu) return bLoad;
            if (front == lrdu) return outputGate;
        } else {
            if (front.inverse() == lrdu) return aLoad;
            if (front == lrdu) return outputGate;
        }
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        boolean cable = getInventory().getStackInSlot(ElectricalSensorContainer.cableSlotId) != null;
        if (!descriptor.voltageOnly) {
            if (front.left() == lrdu && cable) return NodeBase.maskElectricalAll;
            if (front.right() == lrdu && cable) return NodeBase.maskElectricalAll;
            if (front == lrdu) return NodeBase.maskElectricalOutputGate;
        } else {
            if (front.inverse() == lrdu && cable) return NodeBase.maskElectricalAll;
            if (front == lrdu) return NodeBase.maskElectricalOutputGate;
        }
        return 0;
    }

    @Override
    public String multiMeterString() {
        if (!descriptor.voltageOnly)
            return Utils.plotUIP(aLoad.getU(), aLoad.getCurrent());
        else
            return Utils.plotVolt("Uin:", aLoad.getU()) + Utils.plotVolt("Uout:", outputGate.getU());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", outputGate.getU()));
        if (Eln.wailaEasyMode) {
            switch (typeOfSensor) {
                case voltageType:
                    info.put(I18N.tr("Measured voltage"), Utils.plotVolt("", aLoad.getU()));
                    break;

                case currantType:
                    info.put(I18N.tr("Measured current"), Utils.plotAmpere("", aLoad.getI()));
                    break;

                case powerType:
                    info.put(I18N.tr("Measured power"), Utils.plotPower("", aLoad.getU() * aLoad.getI()));
                    break;
            }
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
            stream.writeByte(typeOfSensor);
            stream.writeFloat(lowValue);
            stream.writeFloat(highValue);
            stream.writeByte(dirType);
            Utils.serialiseItemStack(stream, getInventory().getStackInSlot(ElectricalSensorContainer.cableSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        Eln.instance.signalCableDescriptor.applyTo(outputGate);
        computeElectricalLoad();
        Eln.applySmallRs(aLoad);
        if (bLoad != null) Eln.applySmallRs(bLoad);
    }

    @Override
    protected void inventoryChanged() {
        computeElectricalLoad();
        reconnect();
    }

    public void computeElectricalLoad() {
        //if (!descriptor.voltageOnly)
        {
            ItemStack cable = getInventory().getStackInSlot(ElectricalSensorContainer.cableSlotId);
            ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);

            if (cableDescriptor == null) {
                if (resistor != null) resistor.highImpedance();
                //	currentWatchDog.setIAbsMax(100000);
                voltageWatchDog.setUNominal(1000000000);
            } else {
                if (resistor != null) cableDescriptor.applyTo(resistor, 2);
                //	currentWatchDog.setIAbsMax(cableDescriptor.electricalMaximalCurrent);
                voltageWatchDog.setUNominal(cableDescriptor.electricalNominalVoltage);
            }
        }
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(entityPlayer)) return true;
        return inventory.take(entityPlayer.getCurrentEquippedItem(), this, false, true);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setTypeOfSensorId:
                    typeOfSensor = stream.readByte();
                    needPublish();
                    break;
                case setValueId:
                    lowValue = stream.readFloat();
                    highValue = stream.readFloat();
                    if (lowValue == highValue) highValue += 0.0001;
                    needPublish();
                    break;
                case setDirType:
                    dirType = stream.readByte();
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ElectricalSensorContainer(player, inventory.getInventory(), descriptor);
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("min"))
            lowValue = compound.getFloat("min");
        if(compound.hasKey("max"))
            highValue = compound.getFloat("max");
        if (lowValue == highValue) highValue += 0.0001;
        if(compound.hasKey("unit")) {
            switch (compound.getByte("unit")) {
                case DataLogs.powerType:
                    typeOfSensor = powerType;
                    break;
                case DataLogs.currentType:
                    typeOfSensor = currantType;
                    break;
                case DataLogs.voltageType:
                    typeOfSensor = voltageType;
                    break;
            }
        }
        if(compound.hasKey("dir") && !descriptor.voltageOnly)
            dirType = compound.getByte("dir");
        ConfigCopyToolDescriptor.readCableType(compound, getInventory(), 0, invoker);
        reconnect();
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        compound.setFloat("min", lowValue);
        compound.setFloat("max", highValue);
        switch(typeOfSensor) {
            case powerType:
                compound.setByte("unit", DataLogs.powerType);
                break;
            case currantType:
                compound.setByte("unit", DataLogs.currentType);
                break;
            case voltageType:
                compound.setByte("unit", DataLogs.voltageType);
                break;
        }
        compound.setByte("dir", dirType);
        ConfigCopyToolDescriptor.writeCableType(compound, getInventory().getStackInSlot(0));
    }
}
