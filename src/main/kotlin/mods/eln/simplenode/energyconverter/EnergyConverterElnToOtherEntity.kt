package mods.eln.simplenode.energyconverter

import cofh.api.energy.IEnergyHandler
import cpw.mods.fml.common.Optional
import cpw.mods.fml.common.Optional.InterfaceList
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import ic2.api.energy.tile.IEnergySource
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import mods.eln.Other
import mods.eln.misc.Direction
import mods.eln.node.simple.SimpleNodeEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import java.io.DataInputStream
import java.io.IOException

@InterfaceList(Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = Other.modIdIc2), Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = Other.modIdTe), Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = Other.modIdOc))
class EnergyConverterElnToOtherEntity : SimpleNodeEntity("ElnToOther"), IEnergySource, Environment, IEnergyHandler {
    @JvmField
    var selectedResistance = 0.0
    @JvmField
    var hasChanges = false
    var ocEnergy: EnergyConverterElnToOtherFireWallOc? = null
    @JvmField
    var addedToEnet = false
    var ic2tier = 1

    @SideOnly(Side.CLIENT)
    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return EnergyConverterElnToOtherGui(this)
    }

    override fun serverPublishUnserialize(stream: DataInputStream) {
        super.serverPublishUnserialize(stream)
        try {
            selectedResistance = stream.readDouble()
            ic2tier = stream.readInt()
            hasChanges = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // ********************IC2********************
    @Optional.Method(modid = Other.modIdIc2)
    override fun emitsEnergyTo(receiver: TileEntity, direction: ForgeDirection): Boolean {
        if (worldObj.isRemote) return false
        node ?: return false
        return true
    }

    @Optional.Method(modid = Other.modIdIc2)
    override fun getOfferedEnergy(): Double {
        if (worldObj.isRemote) return 0.0
        if (node == null) return 0.0
        val node = node as EnergyConverterElnToOtherNode
        return node.availableEnergyInModUnitsWithLimit(IC2Tiers.values().first { it.tier == node.ic2tier }.euPerTick.toDouble(), Other.getWattsToEu())
    }

    @Optional.Method(modid = Other.modIdIc2)
    override fun drawEnergy(amount: Double) {
        if (worldObj.isRemote) return
        if (node == null) return
        val node = node as EnergyConverterElnToOtherNode
        node.drawEnergy(amount, Other.getWattsToEu())
    }

    @Optional.Method(modid = Other.modIdIc2)
    override fun getSourceTier(): Int {
        // val node = node as EnergyConverterElnToOtherNode
        return 5
    }

    // ***************** OC **********************
    @Optional.Method(modid = Other.modIdOc)
    fun getOc(): EnergyConverterElnToOtherFireWallOc {
        if (ocEnergy == null) ocEnergy = EnergyConverterElnToOtherFireWallOc(this)
        return ocEnergy!!
    }

    @Optional.Method(modid = Other.modIdOc)
    override fun node(): Node {
        return getOc().node!!
    }

    @Optional.Method(modid = Other.modIdOc)
    override fun onConnect(node: Node) {
    }

    @Optional.Method(modid = Other.modIdOc)
    override fun onDisconnect(node: Node) {
    }

    @Optional.Method(modid = Other.modIdOc)
    override fun onMessage(message: Message) {
    }

    /*
     * @Override
	 * 
	 * @Optional.Method(modid = Other.modIdOc) public Node
	 * sidedNode(ForgeDirection side) { if(worldObj.isRemote){ if(front.back()
	 * == Direction.from(side)) return node(); return null; }else{
	 * if(getNode().getFront().back() == Direction.from(side)) return node();
	 * return null; } }
	 * 
	 * @Override
	 * 
	 * @SideOnly(Side.CLIENT)
	 * 
	 * @Optional.Method(modid = Other.modIdOc) public boolean
	 * canConnect(ForgeDirection side) { if(front == null) return false;
	 * if(front.back() == Direction.from(side)) return true; return false; }
	 */
    // *************** RF **************
    @Optional.Method(modid = Other.modIdTe)
    override fun canConnectEnergy(from: ForgeDirection): Boolean {
        // Utils.println("*****canConnectEnergy*****");
        if (worldObj.isRemote) return false
        if (node == null) return false
        return true
    }

    @Optional.Method(modid = Other.modIdTe)
    override fun receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean): Int {
        // Utils.println("*****receiveEnergy*****");
        return 0
    }

    @Optional.Method(modid = Other.modIdTe)
    override fun extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean): Int {
        // Utils.println("*****extractEnergy*****");
        if (worldObj.isRemote) return 0
        if (node == null) return 0
        val node = node as EnergyConverterElnToOtherNode
        val extract = Math.max(0, Math.min(maxExtract, node.availableEnergyInModUnits(Other.getWattsToRf()).toInt()))
        if (!simulate) node.drawEnergy(extract.toDouble(), Other.getWattsToRf())
        return extract
    }

    @Optional.Method(modid = Other.modIdTe)
    override fun getEnergyStored(from: ForgeDirection): Int {
        // Utils.println("*****getEnergyStored*****");
        return 0
    }

    @Optional.Method(modid = Other.modIdTe)
    override fun getMaxEnergyStored(from: ForgeDirection): Int {
        // Utils.println("*****getMaxEnergyStored*****");
        return 0
    }

    // ***************** Bridges ****************
    override fun updateEntity() {
        super.updateEntity()
        if (Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.updateEntity(this)
        if (Other.ocLoaded) getOc().updateEntity()
        if (Other.teLoaded) EnergyConverterElnToOtherFireWallRf.updateEntity(this)
    }

    fun onLoaded() {
        if (Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.onLoaded(this)
    }

    override fun invalidate() {
        super.invalidate()
        if (Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.invalidate(this)
        if (Other.ocLoaded) getOc().invalidate()
    }

    override fun onChunkUnload() {
        super.onChunkUnload()
        if (Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.onChunkUnload(this)
        if (Other.ocLoaded) getOc().onChunkUnload()
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        if (Other.ocLoaded) getOc().readFromNBT(nbt)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        if (Other.ocLoaded) getOc().writeToNBT(nbt)
    }

    init {
        if (Other.ocLoaded) getOc().constructor()
    }
}
