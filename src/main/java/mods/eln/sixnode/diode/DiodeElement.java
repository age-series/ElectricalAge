package mods.eln.sixnode.diode;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.DiodeHeatThermalLoad;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DiodeElement extends SixNodeElement {

    public DiodeDescriptor descriptor;
    public NbtElectricalLoad anodeLoad = new NbtElectricalLoad("anodeLoad");
    public NbtElectricalLoad catodeLoad = new NbtElectricalLoad("catodeLoad");
    public ResistorSwitch resistorSwitch = new ResistorSwitch("resistorSwitch", anodeLoad, catodeLoad);
    public NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    public DiodeHeatThermalLoad heater = new DiodeHeatThermalLoad(resistorSwitch, thermalLoad);
    public ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog(thermalLoad);
    public DiodeProcess diodeProcess = new DiodeProcess(resistorSwitch);

    public DiodeElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        this.descriptor = (DiodeDescriptor) descriptor;
        thermalLoad.setAsSlow();

        electricalLoadList.add(anodeLoad);
        electricalLoadList.add(catodeLoad);
        thermalLoadList.add(thermalLoad);
        electricalComponentList.add(resistorSwitch);
        electricalProcessList.add(diodeProcess);
        slowProcessList.add(thermalWatchdog.setThermalLoad(this.descriptor.thermal).setDestroys(new WorldExplosion(this).cableExplosion()));
        thermalSlowProcessList.add(heater);
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) (front.toInt() << 0));
    }

    @Nullable
    @Override
    public ElectricalLoad getElectricalLoad(@NotNull LRDU lrdu, int mask) {
        if (front == lrdu) return anodeLoad;
        if (front.inverse() == lrdu) return catodeLoad;
        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(@NotNull LRDU lrdu) {
        if (front == lrdu) return descriptor.cable.getNodeMask();
        if (front.inverse() == lrdu) return descriptor.cable.getNodeMask();
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U+:", anodeLoad.getVoltage()) + Utils.plotVolt("U-:", catodeLoad.getVoltage()) + Utils.plotAmpere("I:", anodeLoad.getCurrent());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Current"), Utils.plotAmpere("", anodeLoad.getCurrent()));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Forward Voltage"), Utils.plotVolt("", anodeLoad.getVoltage() - catodeLoad.getVoltage()));
            info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.getTemperature()));
        }
        return info;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return Utils.plotCelsius("T:", thermalLoad.temperatureCelsius);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(front.toInt() << 4);
            stream.writeShort((short) ((anodeLoad.getVoltage()) * NodeBase.networkSerializeUFactor));
            stream.writeShort((short) ((catodeLoad.getVoltage()) * NodeBase.networkSerializeUFactor));
            stream.writeShort((short) (anodeLoad.getCurrent() * NodeBase.networkSerializeIFactor));
            stream.writeShort((short) (thermalLoad.temperatureCelsius * NodeBase.networkSerializeTFactor));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        descriptor.applyTo(catodeLoad);
        descriptor.applyTo(anodeLoad);
        descriptor.applyTo(thermalLoad);
        descriptor.applyTo(resistorSwitch);
    }
}
