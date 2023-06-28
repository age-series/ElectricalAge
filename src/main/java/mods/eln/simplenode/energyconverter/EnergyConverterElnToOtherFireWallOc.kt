package mods.eln.simplenode.energyconverter

import li.cil.oc.api.Network
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import mods.eln.Other
import mods.eln.misc.Utils
import net.minecraft.nbt.NBTTagCompound

class EnergyConverterElnToOtherFireWallOc(var e: EnergyConverterElnToOtherEntity) {

    var node: Node? = null
    private var addedToNetwork = false

    fun updateEntity() {
        // On the first update, try to add our node to nearby networks. We do
        // this in the update logic, not in validate() because we need to access
        // neighboring tile entities, which isn't possible in validate().
        // We could alternatively check node != null && node.network() == null,
        // but this has somewhat better performance, and makes it clearer.
        if (e.worldObj.isRemote) return
        if (!addedToNetwork) {
            addedToNetwork = true
            Network.joinOrCreateNetwork(e)
        } else {
            if (node != null) {
                if (e.node == null) return
                val c = node as Connector
                val node = e.node as EnergyConverterElnToOtherNode
                var eMax = node.availableEnergyInModUnits(Other.getWattsToOC())
                eMax = Math.min(Math.min(eMax, c.globalBufferSize() - c.globalBuffer()), (node.getDescriptor() as EnergyConverterElnToOtherDescriptor).maxPower)
                if (c.tryChangeBuffer(eMax)) {
                    node.drawEnergy(eMax, Other.getWattsToOC())
                }
            }
        }
    }

    fun onChunkUnload() {
        // Make sure to remove the node from its network when its environment,
        // meaning this tile entity, gets unloaded.
        if (e.worldObj.isRemote) return
        if (node != null) node!!.remove()
    }

    fun invalidate() {
        // Make sure to remove the node from its network when its environment,
        // meaning this tile entity, gets unloaded.
        if (e.worldObj.isRemote) return
        if (node != null) node!!.remove()
    }

    // ----------------------------------------------------------------------- //
    fun readFromNBT(nbt: NBTTagCompound) {
        // Node check removed because it was invalid code? Doesn't seem important here - jrddunbr
        // The host check may be superfluous for you. It's just there to allow
        // some special cases, where getNode() returns some node managed by
        // some other instance (for example when you have multiple internal
        // nodes in this tile entity).
        if (node != null) {
            // This restores the node's address, which is required for networks
            // to continue working without interruption across loads. If the
            // node is a power connector this is also required to restore the
            // internal energy buffer of the node.
            node!!.load(nbt.getCompoundTag("oc:node"))
        }
    }

    fun writeToNBT(@Suppress("UNUSED_PARAMETER") nbt: NBTTagCompound?) {
        // See readFromNBT() regarding host check.
        if (node != null) {
            val nodeNbt = NBTTagCompound()
            node!!.save(nodeNbt)
            Utils.newNbtTagCompund(nodeNbt, "oc:node")
        }
    }

    fun constructor() {
        node = Network.newNode(e, Visibility.None).withConnector().create()
        Utils.println("******** C $node")
    }
}
