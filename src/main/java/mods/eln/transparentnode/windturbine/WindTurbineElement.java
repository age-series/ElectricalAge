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
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.MnaConst;
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
    final VoltageSource powerSource = new VoltageSource("powerSource", positiveLoad, null);
    private final WindTurbineSlowProcess slowProcess = new WindTurbineSlowProcess("slowProcess", this);
    private final WindTurbineElectricalProcess electricalProcess = new WindTurbineElectricalProcess();
    final WindTurbineDescriptor descriptor;
    private Direction cableFront = Direction.ZP;
    private double regulatorFilteredOutputVoltage = 0.0;
    private double regulatorCurrentLimitRequest = 0.0;

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
        return Utils.plotRads("", slowProcess.getRotorRads()) + Utils.plotUIP(powerSource.getVoltage(), powerSource.getCurrent());
    }

    @NotNull
    @Override
    public String thermoMeterString(@NotNull Direction side) {
        return "";
    }

    @Override
    public void initialize() {
        setPhysicalValue();
        connect();
    }

    private void setPhysicalValue() {
        descriptor.cable.applyTo(positiveLoad);
    }

    @Override
    public void connectJob() {
        super.connectJob();
        Eln.simulator.mna.addProcess(electricalProcess);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        Eln.simulator.mna.removeProcess(electricalProcess);
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
            stream.writeFloat((float) (slowProcess.getRotorRads() / descriptor.nominalRotorRads));
            node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        cableFront.writeToNBT(nbt, "cableFront");
        slowProcess.writeToNBT(nbt, "");
        powerSource.writeToNBT(nbt, "");
        nbt.setDouble("regulatorFilteredOutputVoltage", regulatorFilteredOutputVoltage);
        Utils.println(cableFront);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        cableFront = Direction.readFromNBT(nbt, "cableFront");
        slowProcess.readFromNBT(nbt, "");
        powerSource.readFromNBT(nbt, "");
        regulatorFilteredOutputVoltage = nbt.getDouble("regulatorFilteredOutputVoltage");
        Utils.println(cableFront);
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> wailaList = new HashMap<String, String>();
        wailaList.put(I18N.tr("Generating"), slowProcess.getWind() > 0 ? I18N.tr("Yes") : I18N.tr("No"));
        wailaList.put(I18N.tr("Wind"), Utils.plotValue(slowProcess.getWind(), "m/s"));
        wailaList.put(I18N.tr("Rotor speed"), Utils.plotRads("", slowProcess.getRotorRads()));
        wailaList.put(I18N.tr("Produced power"), Utils.plotPower("", Math.max(0.0, powerSource.getPower())));
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            wailaList.put(I18N.tr("Voltage"), Utils.plotVolt("", powerSource.getVoltage()));
            wailaList.put(I18N.tr("Current"), Utils.plotAmpere("", powerSource.getCurrent()));
            wailaList.put(I18N.tr("Current limit"), Utils.plotAmpere("", regulatorCurrentLimitRequest));
            wailaList.put(I18N.tr("Rotor energy"), Utils.plotEnergy("", slowProcess.getRotorEnergy()));
        }
        return wailaList;
    }

    private class WindTurbineElectricalProcess implements IRootSystemPreStepProcess, mods.eln.sim.IProcess {
        @Override
        public void process(double time) {
            double dt = time > 0.0 ? time : Eln.simulator.electricalPeriod;
            double noLoadVoltage = descriptor.nominalVoltage * (slowProcess.getRotorRads() / descriptor.nominalRotorRads);
            noLoadVoltage = Math.max(0.0, Math.min(descriptor.maxVoltage, noLoadVoltage));

            SubSystem.Thevenin th = positiveLoad.getSubSystem().getTh(positiveLoad, powerSource);
            if (Double.isNaN(th.voltage)) {
                th.voltage = 0.0;
                th.resistance = MnaConst.highImpedance;
            }

            double droopedTarget = Math.max(0.0,
                noLoadVoltage - Math.abs(powerSource.getCurrent()) * descriptor.regulatorDroopResistance);
            double commandedVoltage;
            if (th.isHighImpedance()) {
                regulatorCurrentLimitRequest = 0.0;
                commandedVoltage = droopedTarget;
            } else if (th.voltage >= droopedTarget) {
                regulatorCurrentLimitRequest = 0.0;
                commandedVoltage = th.voltage;
            } else {
                regulatorCurrentLimitRequest = descriptor.regulatorCurrentLimit;
                double currentLimitedVoltage = th.voltage + regulatorCurrentLimitRequest * th.resistance;
                commandedVoltage = Math.min(droopedTarget, currentLimitedVoltage);
            }

            if (regulatorFilteredOutputVoltage <= 0.0) {
                regulatorFilteredOutputVoltage = commandedVoltage;
            } else {
                double alpha = Math.max(0.0, Math.min(1.0, dt / descriptor.regulatorVoltageFilterTime));
                regulatorFilteredOutputVoltage += (commandedVoltage - regulatorFilteredOutputVoltage) * alpha;
            }
            if (!th.isHighImpedance() && regulatorFilteredOutputVoltage < th.voltage) {
                regulatorFilteredOutputVoltage = th.voltage;
            }
            powerSource.setVoltage(regulatorFilteredOutputVoltage);
        }

        @Override
        public void rootSystemPreStepProcess() {
            process(0.0);
        }
    }
}
