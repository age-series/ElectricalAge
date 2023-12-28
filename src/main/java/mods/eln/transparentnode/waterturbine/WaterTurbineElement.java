package mods.eln.transparentnode.waterturbine;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Coordinate;
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
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WaterTurbineElement extends TransparentNodeElement {

    NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");

    PowerSource powerSource = new PowerSource("powerSource", positiveLoad);

    WaterTurbineSlowProcess slowProcess = new WaterTurbineSlowProcess(this);

    WaterTurbineDescriptor descriptor;


    public WaterTurbineElement(TransparentNode transparentNode,
                               TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);


        this.descriptor = (WaterTurbineDescriptor) descriptor;

        electricalLoadList.add(positiveLoad);

        electricalComponentList.add(powerSource);
        slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 2, 2));
        slowProcessList.add(slowProcess);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == front) return positiveLoad;
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
        if (side == front) return NodeBase.maskElectricalPower;
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

    Coordinate waterCoord;

    @Override
    public void initialize() {

        setPhysicalValue();
        waterCoord = descriptor.getWaterCoordonate(node.coordinate.world());
        waterCoord.applyTransformation(front, node.coordinate);
        powerSource.setMaximumVoltage(descriptor.maxVoltage);
        powerSource.setMaximumCurrent(descriptor.nominalPower * 5 / descriptor.maxVoltage);
        connect();
    }


    private void setPhysicalValue() {
        descriptor.cable.applyTo(positiveLoad);
    }


    TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0, 64, this);

    @Override
    public IInventory getInventory() {

        return inventory;
    }

    @Override
    public boolean hasGui() {

        return false;
    }

    @Nullable
    @Override
    public Container newContainer(@NotNull Direction side, @NotNull EntityPlayer player) {

        return new WaterTurbineContainer(this.node, player, inventory);
    }


    @Override
    public void networkSerialize(DataOutputStream stream) {

        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) (powerSource.getPower() / descriptor.nominalPower));
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    @Override
    public boolean onBlockActivated(EntityPlayer player, Direction side, float vx, float vy, float vz) {

        return false;
    }


    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> wailaList = new HashMap<String, String>();
        wailaList.put(I18N.tr("Generating"), slowProcess.getWaterFactor() > 0 ? I18N.tr("Yes") : I18N.tr("No"));
        wailaList.put(I18N.tr("Produced power"), Utils.plotPower("", powerSource.getEffectivePower()));
        if (Eln.wailaEasyMode) {
            wailaList.put(I18N.tr("Voltage"), Utils.plotVolt("", powerSource.getVoltage()));
        }
        return wailaList;
    }
}
