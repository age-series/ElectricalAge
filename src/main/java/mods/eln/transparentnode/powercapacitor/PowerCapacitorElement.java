package mods.eln.transparentnode.powercapacitor;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.electrical.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.thermal.ThermalLoad;
import mods.eln.sim.electrical.mna.component.Capacitor;
import mods.eln.sim.electrical.mna.component.Resistor;
import mods.eln.sim.electrical.nbt.NbtElectricalLoad;
import mods.eln.sim.watchdogs.BipoleVoltageWatchdog;
import mods.eln.sim.watchdogs.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;

public class PowerCapacitorElement extends TransparentNodeElement {

    PowerCapacitorDescriptor descriptor;
    NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

    Capacitor capacitor = new Capacitor(positiveLoad, negativeLoad);
    Resistor dischargeResistor = new Resistor(positiveLoad, negativeLoad);
    PunkProcess punkProcess = new PunkProcess();
    BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);

    public PowerCapacitorElement(TransparentNode transparentNode,
                                 TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (PowerCapacitorDescriptor) descriptor;

        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);
        electricalComponentList.add(capacitor);
        electricalComponentList.add(dischargeResistor);
        electricalProcessList.add(punkProcess);
        slowProcessList.add(watchdog);

        watchdog.set(new WorldExplosion(this).machineExplosion());
        positiveLoad.setAsMustBeFarFromInterSystem();
    }


    class PunkProcess implements IProcess {
        double eLeft = 0;
        double eLegaliseResistor;

        @Override
        public void process(double time) {
            if (eLeft <= 0) {
                eLeft = 0;
                dischargeResistor.setR(stdDischargeResistor);
            } else {
                eLeft -= dischargeResistor.getP() * time;
                dischargeResistor.setR(eLegaliseResistor);
            }
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == front.left()) return positiveLoad;
        if (side == front.right()) return negativeLoad;
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
        if (side == front.left()) return node.maskElectricalPower;
        if (side == front.right()) return node.maskElectricalPower;
        return 0;
    }

    @NotNull
    @Override
    public String multiMeterString(@NotNull Direction side) {
        return Utils.plotAmpere("I", capacitor.getCurrent());
    }

    @NotNull
    @Override
    public String thermoMeterString(@NotNull Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        Eln.applySmallRs(positiveLoad);
        Eln.applySmallRs(negativeLoad);

        setupPhysical();


        connect();
    }

    @Override
    public void inventoryChange(IInventory inventory) {
        super.inventoryChange(inventory);
        setupPhysical();
    }

    double stdDischargeResistor;

    boolean fromNbt = false;

    public void setupPhysical() {
        double eOld = capacitor.getE();
        capacitor.setC(descriptor.getCValue(inventory));
        stdDischargeResistor = descriptor.dischargeTao / capacitor.getC();

        watchdog.setUNominal(descriptor.getUNominalValue(inventory));
        punkProcess.eLegaliseResistor = Math.pow(descriptor.getUNominalValue(inventory), 2) / 400;

        if (fromNbt) {
            dischargeResistor.setR(stdDischargeResistor);
            fromNbt = false;
        } else {
            double deltaE = capacitor.getE() - eOld;
            punkProcess.eLeft += deltaE;
            if (deltaE < 0) {
                dischargeResistor.setR(stdDischargeResistor);
            } else {
                dischargeResistor.setR(punkProcess.eLegaliseResistor);
            }
        }
    }

    @Override
    public boolean onBlockActivated(EntityPlayer player, Direction side,
                                    float vx, float vy, float vz) {

        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("punkELeft", punkProcess.eLeft);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        punkProcess.eLeft = nbt.getDouble("punkELeft");
        if (Double.isNaN(punkProcess.eLeft)) punkProcess.eLeft = 0;
        fromNbt = true;
    }

    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        /*
		 * try {
		 * 
		 * 
		 * } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 */
    }

    public static final byte unserializePannelAlpha = 0;

    public byte networkUnserialize(DataInputStream stream) {

        byte packetType = super.networkUnserialize(stream);
		/*
		 * try { switch(packetType) {
		 * 
		 * 
		 * default: return packetType; } } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 */
        return unserializeNulldId;
    }

    TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2, 64, this);

    @Override
    public IInventory getInventory() {

        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Nullable
    @Override
    public Container newContainer(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new PowerCapacitorContainer(player, inventory);
    }

}
