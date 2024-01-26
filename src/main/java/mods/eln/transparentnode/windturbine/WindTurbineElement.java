package mods.eln.transparentnode.windturbine;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WindTurbineElement extends TransparentNodeElement {
    private final NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    final PowerSource powerSource = new PowerSource("powerSource", positiveLoad);
    private final WindTurbineSlowProcess slowProcess = new WindTurbineSlowProcess("slowProcess", this);
    final WindTurbineDescriptor descriptor;
    private Direction cableFront = Direction.ZP;

    public WindTurbineElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);

        this.descriptor = (WindTurbineDescriptor) descriptor;

        electricalLoadList.add(positiveLoad);
        electricalComponentList.add(powerSource);
        slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 4, 4));
        slowProcessList.add(slowProcess);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == cableFront.left()) return positiveLoad;
        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull Direction side, @NotNull LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == cableFront.left()) return NodeBase.maskElectricalPower;
        if (side == cableFront.right() && !grounded) return NodeBase.maskElectricalPower;
        return 0;
    }

    @NotNull
    @Override
    public String multiMeterString(@NotNull Direction side) {
        return null;
    }

    @NotNull
    @Override
    public String thermoMeterString(@NotNull Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        setPhysicalValue();
        powerSource.setMaximumCurrent(descriptor.nominalPower * 5 / descriptor.maxVoltage);
        connect();
    }

    private void setPhysicalValue() {
        descriptor.cable.applyTo(positiveLoad);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer player, Direction side, float vx, float vy, float vz) {
        if (Utils.isPlayerUsingWrench(player)) {
            cableFront = cableFront.right();
            reconnect();
        }
        return false;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) slowProcess.getWind());
            stream.writeFloat((float) (powerSource.getPower() / descriptor.nominalPower));
            node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        cableFront.writeToNBT(nbt, "cableFront");
        Utils.println(cableFront);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        cableFront = Direction.readFromNBT(nbt, "cableFront");
        Utils.println(cableFront);
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> wailaList = new HashMap<String, String>();
        wailaList.put(I18N.tr("Generating"), slowProcess.getWind() > 0 ? I18N.tr("Yes") : I18N.tr("No"));
        wailaList.put(I18N.tr("Produced power"), Utils.plotPower("", powerSource.getEffectivePower()));
        if (Eln.wailaEasyMode) {
            wailaList.put(I18N.tr("Voltage"), Utils.plotVolt("", powerSource.getVoltage()));
        }
        return wailaList;
    }
}
