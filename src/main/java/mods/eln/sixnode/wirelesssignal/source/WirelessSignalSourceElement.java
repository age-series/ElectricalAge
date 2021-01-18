package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.i18n.I18N;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement.LightningGlitchProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WirelessSignalSourceElement extends SixNodeElement implements IWirelessSignalTx, IConfigurable {

    public static final HashMap<String, ArrayList<IWirelessSignalTx>> channelMap = new HashMap<String, ArrayList<IWirelessSignalTx>>();

    WirelessSignalSourceDescriptor descriptor;

    public AutoResetProcess autoResetProcess;
    boolean state = false;

    public String channel = "Default channel";
    private LightningGlitchProcess lightningGlitchProcess;

    public static final byte setChannelId = 1;

    public WirelessSignalSourceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        this.descriptor = (WirelessSignalSourceDescriptor) descriptor;
        WirelessSignalTxElement.channelRegister(this);
        slowProcessList.add(lightningGlitchProcess = new LightningGlitchProcess(getCoordinate()));
        if (this.descriptor.autoReset) {
            slowProcessList.add(autoResetProcess = new AutoResetProcess());
            autoResetProcess.reset();
        }
    }

    class AutoResetProcess implements IProcess {
        double timeout = 0;
        double timeoutDelay = 0.21;

        @Override
        public void process(double time) {
            if (timeout > 0) {
                if (timeout - time < 0) {
                    if (state) {
                        state = false;
                        needPublish();
                    }
                }
                timeout -= time;
            }
        }

        void reset() {
            timeout = timeoutDelay;
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public ElectricalLoad getElectricalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(@NotNull LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString() {
        return null;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return null;
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Channel"), channel);
        if(!descriptor.autoReset) {
            if (state) {
                info.put(I18N.tr("State"), "§a" + I18N.tr("On"));
            }else{
                info.put(I18N.tr("State"), "§c" + I18N.tr("Off"));
            }
        }
        return info;
    }

    @Override
    public void initialize() {
    }

    @Override
    public boolean onBlockActivated(@NotNull EntityPlayer entityPlayer, @NotNull Direction side, float vx, float vy, float vz) {
        if (Utils.isPlayerUsingWrench(entityPlayer))
            return false;

        state = !state;
        if (state && autoResetProcess != null) autoResetProcess.reset();
        needPublish();
        return true;
    }

    @Override
    public void destroy(EntityPlayerMP entityPlayer) {
        WirelessSignalTxElement.channelRemove(this);
        super.destroy(entityPlayer);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("channel", channel);
        nbt.setBoolean("state", state);
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        WirelessSignalTxElement.channelRemove(this);

        super.readFromNBT(nbt);
        channel = nbt.getString("channel");
        state = nbt.getBoolean("state");

        WirelessSignalTxElement.channelRegister(this);
    }

    @Override
    public Coordinate getCoordinate() {
        return sixNode.coordinate;
    }

    @Override
    public int getRange() {
        return descriptor.range;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public double getValue() {
        return (state ? 1.0 : 0.0) + lightningGlitchProcess.glitchOffset;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);

        try {
            switch (stream.readByte()) {
                case setChannelId:
                    WirelessSignalTxElement.channelRemove(this);
                    channel = stream.readUTF();
                    needPublish();
                    WirelessSignalTxElement.channelRegister(this);
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
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(channel);
            stream.writeBoolean(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("wirelessChannels")) {
            String newChannel = compound.getTagList("wirelessChannels", 8).getStringTagAt(0);
            if(newChannel != null && newChannel != "") {
                WirelessSignalTxElement.channelRemove(this);
                channel = newChannel;
                WirelessSignalTxElement.channelRegister(this);
                needPublish();
            }
        }
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString(channel));
        compound.setTag("wirelessChannels", list);
    }
}
