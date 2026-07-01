package mods.eln.simplenode.computerprobe

import cpw.mods.fml.common.Optional
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import mods.eln.Other
import mods.eln.node.simple.SimpleNodeEntity
import net.minecraft.nbt.NBTTagCompound

@Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = Other.modIdOc)
class ComputerProbeEntity : SimpleNodeEntity(ComputerProbeNode.getNodeUuidStatic()), Environment {
    private var ocNode: Node? = null
    private var addedToNetwork = false

    init {
        ensureOpenComputersNode()
    }

    fun getComponentName(): String {
        return "ElnProbe"
    }

    fun getOpenComputersAddress(): String? {
        return ensureOpenComputersNode()?.address()
    }

    override fun updateEntity() {
        super.updateEntity()
        if (worldObj.isRemote || !Other.ocLoaded) return
        val node = ensureOpenComputersNode() ?: return
        if (!addedToNetwork || node.network() == null) {
            addedToNetwork = true
            Network.joinOrCreateNetwork(this)
        }
    }

    @Optional.Method(modid = Other.modIdOc)
    override fun node(): Node {
        return ensureOpenComputersNode() ?: throw IllegalStateException("OpenComputers node is not available")
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

    override fun invalidate() {
        super.invalidate()
        if (!worldObj.isRemote && Other.ocLoaded) {
            ocNode?.remove()
        }
    }

    override fun onChunkUnload() {
        super.onChunkUnload()
        if (!worldObj.isRemote && Other.ocLoaded) {
            ocNode?.remove()
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        if (Other.ocLoaded) {
            ensureOpenComputersNode()?.load(nbt.getCompoundTag("oc:node"))
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        if (Other.ocLoaded) {
            val nodeNbt = NBTTagCompound()
            ocNode?.save(nodeNbt)
            nbt.setTag("oc:node", nodeNbt)
        }
    }

    private fun callProbe(action: (ComputerProbeNode) -> Array<Any?>?): Array<Any?>? {
        return try {
            val node = node as? ComputerProbeNode ?: return arrayOf(null, "ELN probe node is not ready")
            action(node)
        } catch (e: Exception) {
            arrayOf(null, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun ensureOpenComputersNode(): Node? {
        if (!Other.ocLoaded) return null
        if (ocNode == null) {
            ocNode = Network.newNode(this, Visibility.Network)
                ?.withComponent(getComponentName())
                ?.create()
        }
        return ocNode
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun signalSetDir(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.signalSetDir(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun signalGetDir(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.signalGetDir(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun signalSetOut(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.signalSetOut(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun signalGetOut(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.signalGetOut(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun signalGetIn(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.signalGetIn(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun wirelessSet(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.wirelessSet(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun wirelessRemove(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.wirelessRemove(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun wirelessRemoveAll(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.wirelessRemoveAll(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun wirelessGet(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.wirelessGet(context, args) }
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    fun version(context: Context?, args: Arguments?): Array<Any?>? {
        return callProbe { it.version(context, args) }
    }
}
