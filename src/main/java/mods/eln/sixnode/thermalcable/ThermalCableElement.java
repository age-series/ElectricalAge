package mods.eln.sixnode.thermalcable;

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
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThermalCableElement extends SixNodeElement {

    ThermalCableDescriptor descriptor;

    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");

    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog(thermalLoad);

    int color = 0;
    int colorCare = 1;

    public ThermalCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (ThermalCableDescriptor) descriptor;

        thermalLoadList.add(thermalLoad);

        slowProcessList.add(thermalWatchdog);

        thermalWatchdog
            .setTemperatureLimits(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
            .setDestroys(new WorldExplosion(this).cableExplosion());
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
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

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return NodeBase.maskThermalWire + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();

        info.put(I18N.tr("Thermic power"), Utils.plotPower("", thermalLoad.getPower()));
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.getTemperature()));
        return info;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return Utils.plotCelsius("T", thermalLoad.temperatureCelsius) + Utils.plotPower("P", thermalLoad.getPower());
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte((color << 4));
            stream.writeShort((short) (thermalLoad.temperatureCelsius * NodeBase.networkSerializeTFactor));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        descriptor.setThermalLoad(thermalLoad);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            colorCare = colorCare ^ 1;
            Utils.addChatMessage(entityPlayer, "Wire color care " + colorCare);
            sixNode.reconnect();
        } else if (currentItemStack != null) {
            Item item = currentItemStack.getItem();

            GenericItemUsingDamageDescriptor gen = BrushDescriptor.getDescriptor(currentItemStack);
            if (gen != null && gen instanceof BrushDescriptor) {
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
