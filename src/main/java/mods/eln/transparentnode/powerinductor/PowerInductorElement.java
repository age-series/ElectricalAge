package mods.eln.transparentnode.powerinductor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;

public class PowerInductorElement extends TransparentNodeElement {

    PowerInductorDescriptor descriptor;
    NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

    Inductor inductor = new Inductor("inductor", positiveLoad, negativeLoad);

    public PowerInductorElement(TransparentNode transparentNode,
                                TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (PowerInductorDescriptor) descriptor;

        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);
        electricalComponentList.add(inductor);
        positiveLoad.setAsMustBeFarFromInterSystem();
    }

    @Nullable
    @Override
    public ElectricalLoad getElectricalLoad(@NotNull Direction side, @NotNull LRDU lrdu) {
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
    public int getConnectionMask(@NotNull Direction side, @NotNull LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front.left()) return node.maskElectricalPower;
        if (side == front.right()) return node.maskElectricalPower;
        return 0;
    }

    @NotNull
    @Override
    public String multiMeterString(@NotNull Direction side) {
        return Utils.plotAmpere("I", inductor.getCurrent());
    }

    @NotNull
    @Override
    public String thermoMeterString(@NotNull Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        //Eln.applySmallRs(positiveLoad);
        //Eln.applySmallRs(negativeLoad);

        setupPhysical();

        connect();
    }

    @Override
    public void inventoryChange(IInventory inventory) {
        super.inventoryChange(inventory);
        setupPhysical();
    }


    boolean fromNbt = false;

    public void setupPhysical() {
        double rs = descriptor.getRsValue(inventory);
        inductor.setInductance(descriptor.getlValue(inventory));
        positiveLoad.setSerialResistance(rs);
        negativeLoad.setSerialResistance(rs);

        if (fromNbt) {
            fromNbt = false;
        } else {
            inductor.resetStates();
        }
    }

    @Override
    public boolean onBlockActivated(@NotNull EntityPlayer player, @NotNull Direction side,
                                    float vx, float vy, float vz) {

        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
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
        return new PowerInductorContainer(player, inventory);
    }

}
