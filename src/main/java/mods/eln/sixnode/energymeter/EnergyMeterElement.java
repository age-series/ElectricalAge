package mods.eln.sixnode.energymeter;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnergyMeterElement extends SixNodeElement {

    VoltageStateWatchDog voltageWatchDogA;
    VoltageStateWatchDog voltageWatchDogB;
    // ResistorCurrentWatchdog currentWatchDog = new ResistorCurrentWatchdog();

    public SlowProcess slowProcess = new SlowProcess();
    public EnergyMeterDescriptor descriptor;
    public NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    public NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    public ResistorSwitch shunt = new ResistorSwitch("shunt", aLoad, bLoad);

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public float voltageMax = (float) Eln.SVU, voltageMin = 0;

    int energyUnit = 1, timeUnit = 0;

    public ElectricalCableDescriptor cableDescriptor = null;

    public static final byte clientEnergyStackId = 1;
    public static final byte clientModId = 2;
    public static final byte clientPasswordId = 3;
    public static final byte clientToggleStateId = 4;
    public static final byte clientTimeCounterId = 5;
    public static final byte clientEnergyUnitId = 6;
    public static final byte clientTimeUnitId = 7;
    public static final byte serverPowerId = 1;

    String password = "";
    double energyStack = 0;
    double timeCounter = 0;

    enum Mod {ModCounter, ModPrepay}

    Mod mod = Mod.ModCounter;

    public EnergyMeterElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        shunt.mustUseUltraImpedance();

        voltageWatchDogA = new VoltageStateWatchDog(aLoad);
        voltageWatchDogB = new VoltageStateWatchDog(bLoad);

        electricalLoadList.add(aLoad);
        electricalLoadList.add(bLoad);
        electricalComponentList.add(shunt);
        electricalComponentList.add(new Resistor(bLoad, null).pullDown());
        electricalComponentList.add(new Resistor(aLoad, null).pullDown());

        slowProcessList.add(slowProcess);

        WorldExplosion exp = new WorldExplosion(this).cableExplosion();

        // slowProcessList.add(currentWatchDog);
        slowProcessList.add(voltageWatchDogA);
        slowProcessList.add(voltageWatchDogB);

        // currentWatchDog.set(shunt).set(exp);
        voltageWatchDogA.setDestroys(exp);
        voltageWatchDogB.setDestroys(exp);
        this.descriptor = (EnergyMeterDescriptor) descriptor;
    }

    public SixNodeElementInventory getInventory() {
        return inventory;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (front == lrdu) return aLoad;
        if (front.inverse() == lrdu) return bLoad;
        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (inventory.getStackInSlot(EnergyMeterContainer.cableSlotId) == null) return 0;
        if (front == lrdu) return NodeBase.maskElectricalAll;
        if (front.inverse() == lrdu) return NodeBase.maskElectricalAll;

        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("Ua:", aLoad.getVoltage()) + Utils.plotVolt("Ub:", bLoad.getVoltage()) + Utils.plotVolt("I:", aLoad.getCurrent());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Power"), Utils.plotPower("", aLoad.getVoltage() * aLoad.getCurrent()));
        switch (mod) {
            case ModCounter:
                info.put(I18N.tr("Mode"), I18N.tr("Counter"));
                info.put(I18N.tr("Energy"), Utils.plotEnergy("", energyStack));
                break;

            case ModPrepay:
                info.put(I18N.tr("Mode"), I18N.tr("Prepay"));
                info.put(I18N.tr("Energy left"), Utils.plotEnergy("", energyStack));
                break;
        }

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
            stream.writeBoolean(shunt.getState());
            stream.writeUTF(password);
            stream.writeUTF(mod.toString());
            stream.writeDouble(timeCounter);

            // stream.writeDouble(energyStack);
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(EnergyMeterContainer.cableSlotId));

            stream.writeByte(energyUnit);
            stream.writeByte(timeUnit);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSwitchState(boolean state) {
        if (state == shunt.getState()) return;
        //	if (energyStack <= 0 && mod == Mod.ModPrepay) return;
        shunt.setState(state);
        play(new SoundCommand("random.click").mulVolume(0.3F, 0.6f).smallRange());
        needPublish();
    }

    @Override
    public void initialize() {
        computeElectricalLoad();
    }

    @Override
    public void inventoryChanged() {
        computeElectricalLoad();
        reconnect();
    }

    public void computeElectricalLoad() {
        ItemStack cable = inventory.getStackInSlot(EnergyMeterContainer.cableSlotId);

        cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
        if (cableDescriptor == null) {
            aLoad.highImpedance();
            bLoad.highImpedance();
        } else {
            cableDescriptor.applyTo(aLoad);
            cableDescriptor.applyTo(bLoad);

            voltageWatchDogA.setNominalVoltage(cableDescriptor.electricalNominalVoltage);
            voltageWatchDogB.setNominalVoltage(cableDescriptor.electricalNominalVoltage);
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case clientEnergyStackId:
                    energyStack = stream.readDouble();
                    slowProcess.publishTimeout = -1;
                    // needPublish();
                    break;
                case clientTimeCounterId:
                    timeCounter = 0;
                    needPublish();
                    break;
                case clientModId:
                    mod = Mod.valueOf(stream.readUTF());
                    needPublish();
                    break;
                case clientPasswordId:
                    password = stream.readUTF();
                    needPublish();
                    break;
                case clientToggleStateId:
                    setSwitchState(!shunt.getState());
                    break;
                case clientEnergyUnitId:
                    energyUnit++;
                    if (energyUnit > 3) energyUnit = 0;
                    needPublish();
                    break;
                case clientTimeUnitId:
                    timeUnit++;
                    if (timeUnit > 1) timeUnit = 0;
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Nullable
    @Override
    public Container newContainer(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new EnergyMeterContainer(player, inventory);
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        try {
            mod = Mod.valueOf(nbt.getString("mode"));
        } catch (Exception e) {
            mod = Mod.ModCounter;
        }
        energyStack = nbt.getDouble("energyStack");
        timeCounter = nbt.getDouble("timeCounter");
        password = nbt.getString("password");
        slowProcess.oldEnergyPublish = energyStack;
        energyUnit = nbt.getByte("energyUnit");
        timeUnit = nbt.getByte("timeUnit");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setString("mode", mod.toString());
        nbt.setDouble("energyStack", energyStack);
        nbt.setDouble("timeCounter", timeCounter);
        nbt.setString("password", password);
        nbt.setByte("energyUnit", (byte) energyUnit);
        nbt.setByte("timeUnit", (byte) timeUnit);
    }

    class SlowProcess implements IProcess {
        public static final double publishTimeoutReset = 1;
        public double publishTimeout = Math.random() * publishTimeoutReset;
        public double oldEnergyPublish;

        @Override
        public void process(double time) {
            timeCounter += time * 72.0;
            double p = aLoad.getCurrent() * aLoad.getVoltage() * (aLoad.getVoltage() > bLoad.getVoltage() ? 1.0 : -1.0);
            boolean highImp = false;
            switch (mod) {
                case ModCounter:
                    energyStack += p * time;
                    break;
                case ModPrepay:
                    energyStack -= p * time;
                    if (energyStack < 0) {
                        // energyStack = 0;
                        // setSwitchState(false);
                        if (p > 0) {
                            highImp = true;
                        }
                    }
                    break;
            }

            if (highImp) shunt.ultraImpedance();
            else Eln.applySmallRs(shunt);

            publishTimeout -= time;
            if (publishTimeout < 0) {
                publishTimeout += publishTimeoutReset;
                ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
                DataOutputStream packet = new DataOutputStream(bos);

                preparePacketForClient(packet);

                try {
                    packet.writeByte(serverPowerId);
                    packet.writeDouble(oldEnergyPublish);
                    packet.writeDouble((energyStack - oldEnergyPublish) / publishTimeoutReset);

                    sendPacketToAllClient(bos, 10);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                oldEnergyPublish = energyStack;
            }
        }
    }
}
