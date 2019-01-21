package mods.eln.node.simple;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.misc.DescriptorManager;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.node.NodeBase;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.nbt.NbtThermalLoad;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class SimpleNode extends NodeBase {

    public EntityPlayerMP removedByPlayer;
    String descriptorKey = "";

    protected void setDescriptorKey(String key) {
        descriptorKey = key;
    }

    protected Object getDescriptor() {
        return DescriptorManager.get(descriptorKey);
    }

    private Direction front;

    public Direction getFront() {
        return front;
    }

    public void setFront(Direction front) {
        this.front = front;
        if (applayFrontToMetadata()) {
            coordonate.setMetadata(front.getInt());
        }
    }

    protected boolean applayFrontToMetadata() {
        return false;
    }

    @Override
    public void initializeFromThat(Direction front, EntityLivingBase entityLiving, ItemStack itemStack) {
        setFront(front);
        initialize();
    }

    @Override
    public void initializeFromNBT() {
        initialize();
    }

    public abstract void initialize();


    @Override
    public void publishSerialize(DataOutputStream stream) {
        super.publishSerialize(stream);
        try {
            stream.writeByte(front.getInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public ArrayList<IProcess> slowProcessList = new ArrayList<IProcess>(4);

    public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
    public ArrayList<Component> electricalComponentList = new ArrayList<Component>(4);
    public ArrayList<State> electricalLoadList = new ArrayList<State>(4);

    public ArrayList<IProcess> thermalFastProcessList = new ArrayList<IProcess>(4);
    public ArrayList<IProcess> thermalSlowProcessList = new ArrayList<IProcess>(4);
    public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
    public ArrayList<NbtThermalLoad> thermalLoadList = new ArrayList<NbtThermalLoad>(4);

    @Override
    public void connectJob() {
        super.connectJob();

        Vars.simulator.addAllSlowProcess(slowProcessList);

        Vars.simulator.addAllElectricalComponent(electricalComponentList);
        for (State load : electricalLoadList)
            Vars.simulator.addElectricalLoad(load);
        Vars.simulator.addAllElectricalProcess(electricalProcessList);

        Vars.simulator.addAllThermalConnection(thermalConnectionList);
        for (NbtThermalLoad load : thermalLoadList)
            Vars.simulator.addThermalLoad(load);
        Vars.simulator.addAllThermalFastProcess(thermalFastProcessList);
        Vars.simulator.addAllThermalSlowProcess(thermalSlowProcessList);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();

        Vars.simulator.removeAllSlowProcess(slowProcessList);

        Vars.simulator.removeAllElectricalComponent(electricalComponentList);
        for (State load : electricalLoadList)
            Vars.simulator.removeElectricalLoad(load);
        Vars.simulator.removeAllElectricalProcess(electricalProcessList);

        Vars.simulator.removeAllThermalConnection(thermalConnectionList);
        for (NbtThermalLoad load : thermalLoadList)
            Vars.simulator.removeThermalLoad(load);
        Vars.simulator.removeAllThermalFastProcess(thermalFastProcessList);
        Vars.simulator.removeAllThermalSlowProcess(thermalSlowProcessList);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        front = Direction.readFromNBT(nbt, "SNfront");

        setDescriptorKey(nbt.getString("SNdescriptorKey"));

        for (State electricalLoad : electricalLoadList) {
            if (electricalLoad instanceof INBTTReady) ((INBTTReady) electricalLoad).readFromNBT(nbt, "");
        }

        for (NbtThermalLoad thermalLoad : thermalLoadList) {
            thermalLoad.readFromNBT(nbt, "");
        }

        for (Component c : electricalComponentList)
            if (c instanceof INBTTReady)
                ((INBTTReady) c).readFromNBT(nbt, "");

        for (IProcess process : slowProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).readFromNBT(nbt, "");
        }
        for (IProcess process : electricalProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).readFromNBT(nbt, "");
        }
        for (IProcess process : thermalFastProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).readFromNBT(nbt, "");
        }
        for (IProcess process : thermalSlowProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).readFromNBT(nbt, "");
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        front.writeToNBT(nbt, "SNfront");

        nbt.setString("SNdescriptorKey", descriptorKey == null ? "" : descriptorKey);

        for (State electricalLoad : electricalLoadList) {
            if (electricalLoad instanceof INBTTReady) ((INBTTReady) electricalLoad).writeToNBT(nbt, "");
        }

        for (NbtThermalLoad thermalLoad : thermalLoadList) {
            thermalLoad.writeToNBT(nbt, "");
        }

        for (Component c : electricalComponentList)
            if (c instanceof INBTTReady)
                ((INBTTReady) c).writeToNBT(nbt, "");

        for (IProcess process : slowProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).writeToNBT(nbt, "");
        }
        for (IProcess process : electricalProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).writeToNBT(nbt, "");
        }
        for (IProcess process : thermalFastProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).writeToNBT(nbt, "");
        }
        for (IProcess process : thermalSlowProcessList) {
            if (process instanceof INBTTReady) ((INBTTReady) process).writeToNBT(nbt, "");
        }

    }
}
