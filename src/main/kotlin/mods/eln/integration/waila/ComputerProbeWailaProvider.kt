package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mcp.mobius.waila.api.SpecialChars
import mods.eln.simplenode.computerprobe.ComputerProbeEntity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class ComputerProbeWailaProvider : IWailaDataProvider {
    override fun getWailaBody(
        itemStack: ItemStack?,
        currenttip: MutableList<String>,
        accessor: IWailaDataAccessor,
        config: IWailaConfigHandler?
    ): MutableList<String> {
        val nbt = accessor.nbtData
        val componentName = nbt.getString(TAG_COMPONENT_NAME)
        val address = nbt.getString(TAG_ADDRESS)

        if (componentName.isNotEmpty()) {
            currenttip.add("Component: ${SpecialChars.WHITE}$componentName")
        }
        if (address.isNotEmpty()) {
            currenttip.add("Address: ${SpecialChars.WHITE}$address")
        }

        return currenttip
    }

    override fun getWailaStack(accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): ItemStack? = null

    override fun getWailaTail(
        itemStack: ItemStack?,
        currenttip: MutableList<String>,
        accessor: IWailaDataAccessor?,
        config: IWailaConfigHandler?
    ): MutableList<String> = currenttip

    override fun getNBTData(
        player: EntityPlayerMP?,
        te: TileEntity?,
        tag: NBTTagCompound,
        world: World?,
        x: Int,
        y: Int,
        z: Int
    ): NBTTagCompound {
        val probe = te as? ComputerProbeEntity ?: return tag
        tag.setString(TAG_COMPONENT_NAME, probe.getComponentName())
        probe.getOpenComputersAddress()?.let { tag.setString(TAG_ADDRESS, it) }
        return tag
    }

    override fun getWailaHead(
        itemStack: ItemStack?,
        currenttip: MutableList<String>,
        accessor: IWailaDataAccessor?,
        config: IWailaConfigHandler?
    ): MutableList<String> = currenttip

    companion object {
        private const val TAG_COMPONENT_NAME = "eln.ocComponentName"
        private const val TAG_ADDRESS = "eln.ocAddress"
    }
}
