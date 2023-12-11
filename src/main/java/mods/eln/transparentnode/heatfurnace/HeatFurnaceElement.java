package mods.eln.transparentnode.heatfurnace;

import mods.eln.i18n.I18N;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtFurnaceProcess;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* 5s/item @ 500 C
 */

public class HeatFurnaceElement extends TransparentNodeElement {

    public NbtElectricalGateInput electricalCmdLoad = new NbtElectricalGateInput("electricalCmdLoad");
    //public SignalRp electricalCmdRp = new SignalRp(electricalCmdLoad);
    public NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    public NbtFurnaceProcess furnaceProcess = new NbtFurnaceProcess("furnaceProcess", thermalLoad);
    public HeatFurnaceInventoryProcess inventoryProcess = new HeatFurnaceInventoryProcess(this);

    TransparentNodeElementInventory inventory = new HeatFurnaceInventory(4, 64, this);

    HeatFurnaceThermalProcess regulator = new HeatFurnaceThermalProcess("regulator", furnaceProcess, this);

    HeatFurnaceDescriptor descriptor;

    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog(thermalLoad);

    public static final byte unserializeGain = 1;
    public static final byte unserializeTemperatureTarget = 2;
    public static final byte unserializeToogleControlExternalId = 3;
    public static final byte unserializeToogleTakeFuelId = 4;

    public boolean controlExternal = false, takeFuel = false;

    public HeatFurnaceElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        //this.descriptor.alphaClose = 0;

        this.descriptor = (HeatFurnaceDescriptor) descriptor;

        furnaceProcess.setGainMin(0.1);

        thermalLoadList.add(thermalLoad);
        thermalFastProcessList.add(furnaceProcess);
        slowProcessList.add(inventoryProcess);
        thermalFastProcessList.add(regulator);
        electricalLoadList.add(electricalCmdLoad);
        //electricalComponentList.add(electricalCmdRp);
        slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 2.0, 1.0));

        slowProcessList.add(thermalWatchdog);

        thermalWatchdog
            .setTemperatureLimits(this.descriptor.thermal)
            .setDestroys(new WorldExplosion(this).machineExplosion());
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return electricalCmdLoad;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull Direction side, @NotNull LRDU lrdu) {
        if (side == front.getInverse() && lrdu == LRDU.Down) return thermalLoad;
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if ((side == front.left() || side == front.right()) && lrdu == LRDU.Down)
            return NodeBase.maskElectricalInputGate;
        if (side == front.getInverse() && lrdu == LRDU.Down) return NodeBase.maskThermal;
        return 0;
    }

    @NotNull
    @Override
    public String multiMeterString(@NotNull Direction side) {
        return "";
    }

    @NotNull
    @Override
    public String thermoMeterString(@NotNull Direction side) {
        return Utils.plotCelsius("T:", thermalLoad.temperatureCelsius);
    }

    @Override
    public void initialize() {
        descriptor.applyTo(thermalLoad);
        descriptor.applyTo(furnaceProcess);
        computeInventory();

        connect();

        inventoryProcess.process(1 / 20.0);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(getControlExternal());
            stream.writeBoolean(getTakeFuel());
            stream.writeShort((short) (thermalLoad.temperatureCelsius * NodeBase.networkSerializeTFactor));
            stream.writeFloat((float) furnaceProcess.getGain());
            stream.writeFloat((float) regulator.getTarget());
            stream.writeShort((int) furnaceProcess.getPower());

            serialiseItemStack(stream, inventory.getStackInSlot(HeatFurnaceContainer.combustibleId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Nullable
    @Override
    public Container newContainer(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new HeatFurnaceContainer(node, player, inventory, descriptor);
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte packetType = super.networkUnserialize(stream);
        try {
            switch (packetType) {
                case unserializeGain:
                    if (inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) == null) {
                        furnaceProcess.setGain(stream.readFloat());
                    }
                    needPublish();
                    break;
                case unserializeTemperatureTarget:
                    //if(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) == null)
                {
                    regulator.setTarget(stream.readFloat());
                }
                needPublish();
                break;
                case unserializeToogleControlExternalId:
                    regulator.setTarget(0);
                    setControlExternal(!getControlExternal());
                    break;
                case unserializeToogleTakeFuelId:
                    setTakeFuel(!getTakeFuel());
                    break;
                default:
                    return packetType;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return unserializeNulldId;
    }

    public boolean getControlExternal() {
        return controlExternal;
    }

    public void setControlExternal(boolean value) {
        if (value != controlExternal) needPublish();
        controlExternal = value;
        computeInventory();
    }

    public boolean getTakeFuel() {
        return takeFuel;
    }

    public void setTakeFuel(boolean value) {
        if (value != takeFuel) needPublish();
        takeFuel = value;
    }

    @Override
    public void inventoryChange(IInventory inventory) {
        super.inventoryChange(inventory);

        computeInventory();
        needPublish();
    }

    void computeInventory() {
        ItemStack regulatorStack = inventory.getStackInSlot(HeatFurnaceContainer.regulatorId);

        if (regulatorStack != null && !controlExternal) {
            IRegulatorDescriptor regulator = (IRegulatorDescriptor) Utils.getItemObject(regulatorStack);

            regulator.applyTo(this.regulator, 500.0, 10.0, 0.1, 0.1);
            //	furnace.regulator.target = 240;
        } else {
            regulator.setManual();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setBoolean("takeFuel", takeFuel);
        nbt.setBoolean("controlExternal", controlExternal);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        takeFuel = nbt.getBoolean("takeFuel");
        controlExternal = nbt.getBoolean("controlExternal");
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.temperatureCelsius));
        info.put(I18N.tr("Set temperature"), Utils.plotCelsius("", regulator.getTarget()));
        return info;
    }
}
