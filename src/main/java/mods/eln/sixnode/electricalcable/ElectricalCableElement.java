package mods.eln.sixnode.electricalcable;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalCableElement extends SixNodeElement {

    public ElectricalCableDescriptor descriptor;

    public NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");

    ElectricalLoadHeatThermalLoad heater = new ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad);
    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog(thermalLoad);
    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog(electricalLoad);

    int color;
    int colorCare;

    public ElectricalCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (ElectricalCableDescriptor) descriptor;
        color = 0;
        colorCare = 1;
        electricalLoad.setCanBeSimplifiedByLine(true);
        electricalLoadList.add(electricalLoad);

        if (!this.descriptor.signalWire) {
            thermalLoadList.add(thermalLoad);
            thermalSlowProcessList.add(heater);
            thermalLoad.setAsSlow();
            slowProcessList.add(thermalWatchdog);
            thermalWatchdog
                .setTemperatureLimits(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
                .setDestroys(new WorldExplosion(this).cableExplosion());
        }

        slowProcessList.add(voltageWatchdog);
        voltageWatchdog
            .setNominalVoltage(this.descriptor.electricalNominalVoltage)
            .setDestroys(new WorldExplosion(this).cableExplosion());


    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte b = nbt.getByte("color");
        color = b & 0xF;
        colorCare = (b >> 4) & 1;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("color", (byte) (color + (colorCare << 4)));
    }

    @Nullable
    @Override
    public ElectricalLoad getElectricalLoad(@NotNull LRDU lrdu, int mask) {
        return electricalLoad;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        if (!descriptor.signalWire)
            return thermalLoad;
        else
            return null;
    }

    @Override
    public int getConnectionMask(@NotNull LRDU lrdu) {
        return descriptor.getNodeMask() /*+ NodeBase.maskElectricalWire*/ + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
    }

    @Override
    public String multiMeterString() {
        if (!descriptor.signalWire)
            return Utils.plotUIP(electricalLoad.getVoltage(), electricalLoad.getCurrent()) + " " + Utils.plotPower("Cable Power Loss", electricalLoad.getCurrent() * electricalLoad.getCurrent() * electricalLoad.getSerialResistance());
        else
            return Utils.plotSignal(electricalLoad.getVoltage());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        if (descriptor.signalWire) {
            info.put(I18N.tr("Signal Voltage"), Utils.plotVolt("", electricalLoad.getVoltage()));
        } else {
            info.put(I18N.tr("Current"), Utils.plotAmpere("", electricalLoad.getCurrent()));
            info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.getTemperature()));
            if (Eln.wailaEasyMode) {
                info.put(I18N.tr("Voltage"), Utils.plotVolt("", electricalLoad.getVoltage()));
            }
        }
        info.put(I18N.tr("Subsystem Matrix Size"), Utils.renderSubSystemWaila(electricalLoad.getSubSystem()));
        return info;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        if (!descriptor.signalWire)
            return Utils.plotCelsius("T", thermalLoad.temperatureCelsius);
        else
            return null;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(color << 4);
        /*	stream.writeShort((short) (electricalLoad.Uc * NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short) (electricalLoad.getCurrent() * NodeBase.networkSerializeIFactor));
	    	stream.writeShort((short) (thermalLoad.Tc * NodeBase.networkSerializeTFactor));*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        descriptor.applyTo(electricalLoad);
        descriptor.applyTo(thermalLoad);
        //heater.setDeltaTPerSecondMax(30);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
	/*	World w = sixNode.coordonate.world();
		boolean exist = w.blockExists(10000, 0, 0);
		int id = w.getBlockId(10000, 0, 0);*/
        ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
        //int i;
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            colorCare = colorCare ^ 1;
            Utils.addChatMessage(entityPlayer, "Wire color care " + colorCare);
            sixNode.reconnect();
        } else if (currentItemStack != null) {
            Item item = currentItemStack.getItem();

            GenericItemUsingDamageDescriptor gen = GenericItemUsingDamageDescriptor.getDescriptor(currentItemStack);
            if (gen instanceof BrushDescriptor) {
                BrushDescriptor brush = (BrushDescriptor) gen;
                int brushColor = brush.getColor(currentItemStack);
                if (brushColor != color && brush.use(currentItemStack, entityPlayer)) {
                    color = brushColor;
                    sixNode.reconnect();
                }
            }
        }
        return false;
    }
}
