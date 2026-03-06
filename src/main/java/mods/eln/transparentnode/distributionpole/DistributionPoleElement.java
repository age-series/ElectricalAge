package mods.eln.transparentnode.distributionpole;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import java.io.DataOutputStream;
import java.io.IOException;

public class DistributionPoleElement extends TransparentNodeElement {

    private final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(4, 64, this);

    public DistributionPoleElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    @Override
    public void initialize() {}

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new DistributionPoleContainer(player, inventory);
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(inventory.getStackInSlot(DistributionPoleContainer.braceSlot) != null);
            stream.writeBoolean(inventory.getStackInSlot(DistributionPoleContainer.transformerSlot) != null);
            stream.writeBoolean(inventory.getStackInSlot(DistributionPoleContainer.breakerSlot) != null);
            stream.writeBoolean(inventory.getStackInSlot(DistributionPoleContainer.fuseSlot) != null);
            stream.writeBoolean(true);
        } catch (IOException e) {

        }
    }

    public void inventoryChange(IInventory inventory) {
        needPublish();
    }
}
