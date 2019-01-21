package mods.eln.simplenode.energyconverter;

import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.energy.tile.IEnergySource;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import mods.eln.Vars;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.DataInputStream;
import java.io.IOException;

@Optional.InterfaceList({
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = Vars.modIdIc2),
    @Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = Vars.modIdTe),
    @Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = Vars.modIdOc)})
public class EnergyConverterElnToOtherEntity extends SimpleNodeEntity implements
    IEnergySource, Environment, IEnergyHandler /* ,SidedEnvironment, ISidedBatteryProvider, IPowerEmitter, IPipeConnection */ {

    float inPowerFactor;
    boolean hasChanges = false;
    public float inPowerMax;

    EnergyConverterElnToOtherFireWallOc oc;

    protected boolean addedToEnet;

    public EnergyConverterElnToOtherEntity() {
        if (Vars.ocLoaded)
            getOc().constructor();
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new EnergyConverterElnToOtherGui(player, this);
    }

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        try {
            inPowerFactor = stream.readFloat();
            inPowerMax = stream.readFloat();

            hasChanges = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNodeUuid() {
        return EnergyConverterElnToOtherNode.getNodeUuidStatic();
    }

    // ********************IC2********************

    @Optional.Method(modid = Vars.modIdIc2)
    @Override
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        if (worldObj.isRemote)
            return false;
        SimpleNode n = getNode();
        if (n == null)
            return false;
        return n.getFront().back() == Direction.from(direction);
    }

    @Optional.Method(modid = Vars.modIdIc2)
    @Override
    public double getOfferedEnergy() {
        if (worldObj.isRemote)
            return 0;
        if (getNode() == null)
            return 0;
        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        double pMax = node.getOtherModOutMax(node.descriptor.ic2.outMax,
            Vars.getElnToIc2ConversionRatio());
        return pMax;
    }

    @Optional.Method(modid = Vars.modIdIc2)
    @Override
    public void drawEnergy(double amount) {
        if (worldObj.isRemote)
            return;
        if (getNode() == null)
            return;

        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        node.drawEnergy(amount, Vars.getElnToIc2ConversionRatio());
    }

    @Optional.Method(modid = Vars.modIdIc2)
    // @Override
    public int getSourceTier() {
        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        if (node == null) return 0;
        return node.descriptor.ic2.tier;
    }

    // ***************** OC **********************

    @Optional.Method(modid = Vars.modIdOc)
    EnergyConverterElnToOtherFireWallOc getOc() {
        if (oc == null)
            oc = new EnergyConverterElnToOtherFireWallOc(this);
        return oc;
    }

    @Override
    @Optional.Method(modid = Vars.modIdOc)
    public Node node() {
        return getOc().node;
    }

    @Override
    @Optional.Method(modid = Vars.modIdOc)
    public void onConnect(Node node) {
    }

    @Override
    @Optional.Method(modid = Vars.modIdOc)
    public void onDisconnect(Node node) {
    }

    @Override
    @Optional.Method(modid = Vars.modIdOc)
    public void onMessage(Message message) {
    }

	/*
     * @Override
	 * 
	 * @Optional.Method(modid = Vars.modIdOc) public Node
	 * sidedNode(ForgeDirection side) { if(worldObj.isRemote){ if(front.back()
	 * == Direction.from(side)) return node(); return null; }else{
	 * if(getNode().getFront().back() == Direction.from(side)) return node();
	 * return null; } }
	 * 
	 * @Override
	 * 
	 * @SideOnly(Side.CLIENT)
	 * 
	 * @Optional.Method(modid = Vars.modIdOc) public boolean
	 * canConnect(ForgeDirection side) { if(front == null) return false;
	 * if(front.back() == Direction.from(side)) return true; return false; }
	 */

    // *************** RF **************
    @Override
    @Optional.Method(modid = Vars.modIdTe)
    public boolean canConnectEnergy(ForgeDirection from) {
        // Utils.println("*****canConnectEnergy*****");
        // return true;
        if (worldObj.isRemote)
            return false;
        if (getNode() == null)
            return false;
        SimpleNode n = getNode();
        return n.getFront().back() == Direction.from(from);
    }

    @Override
    @Optional.Method(modid = Vars.modIdTe)
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        // Utils.println("*****receiveEnergy*****");
        return 0;
    }

    @Override
    @Optional.Method(modid = Vars.modIdTe)
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        // Utils.println("*****extractEnergy*****");
        if (worldObj.isRemote)
            return 0;
        if (getNode() == null)
            return 0;
        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        int extract = Math.max(0, Math.min(maxExtract, (int) node.getOtherModEnergyBuffer(Vars.getElnToTeConversionRatio())));
        if (!simulate)
            node.drawEnergy(extract, Vars.getElnToTeConversionRatio());

        return extract;
    }

    @Override
    @Optional.Method(modid = Vars.modIdTe)
    public int getEnergyStored(ForgeDirection from) {
        // Utils.println("*****getEnergyStored*****");
        return 0;
    }

    @Override
    @Optional.Method(modid = Vars.modIdTe)
    public int getMaxEnergyStored(ForgeDirection from) {
        // Utils.println("*****getMaxEnergyStored*****");
        return 0;
    }

    // ***************** Bridges ****************

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (Vars.ic2Loaded)
            EnergyConverterElnToOtherFireWallIc2.updateEntity(this);
        if (Vars.ocLoaded)
            getOc().updateEntity();
        if (Vars.teLoaded)
            EnergyConverterElnToOtherFireWallRf.updateEntity(this);
    }

    public void onLoaded() {
        if (Vars.ic2Loaded)
            EnergyConverterElnToOtherFireWallIc2.onLoaded(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (Vars.ic2Loaded)
            EnergyConverterElnToOtherFireWallIc2.invalidate(this);
        if (Vars.ocLoaded)
            getOc().invalidate();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (Vars.ic2Loaded)
            EnergyConverterElnToOtherFireWallIc2.onChunkUnload(this);
        if (Vars.ocLoaded)
            getOc().onChunkUnload();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (Vars.ocLoaded)
            getOc().readFromNBT(nbt);
    }

    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (Vars.ocLoaded)
            getOc().writeToNBT(nbt);
    }
}
