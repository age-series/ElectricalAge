package mods.eln.node.transparent

import mods.eln.Eln
import mods.eln.item.IConfigurable
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.mustDropItem
import mods.eln.misc.Utils.newNbtTagCompund
import mods.eln.misc.Utils.println
import mods.eln.node.Node
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.IFluidHandler
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.InvocationTargetException

class TransparentNode : Node() {
    @JvmField
    var element: TransparentNodeElement? = null
    @JvmField
    var elementId = 0
    @JvmField
    var removedByPlayer: EntityPlayerMP? = null
    override fun nodeAutoSave(): Boolean {
        return false
    }

    override fun onNeighborBlockChange() {
        super.onNeighborBlockChange()
        element!!.onNeighborBlockChange()
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt.getCompoundTag("node"))
        elementId = nbt.getShort("eid").toInt()
        try {
            val descriptor = Eln.transparentNodeItem.getDescriptor(elementId)
            element = descriptor!!.ElementClass.getConstructor(TransparentNode::class.java, TransparentNodeDescriptor::class.java).newInstance(this, descriptor) as TransparentNodeElement
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        element!!.readFromNBT(nbt.getCompoundTag("element"))
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(newNbtTagCompund(nbt, "node"))
        nbt.setShort("eid", elementId.toShort())
        element!!.writeToNBT(newNbtTagCompund(nbt, "element"))
    }

    override fun onBreakBlock() {
        element!!.onBreakElement()
        super.onBreakBlock()
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? {
        return element!!.getElectricalLoad(side, lrdu)
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? {
        return element!!.getThermalLoad(side, lrdu)
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        return element!!.getConnectionMask(side, lrdu)
    }

    override fun multiMeterString(side: Direction): String {
        return element!!.multiMeterString(side)
    }

    override fun thermoMeterString(side: Direction): String {
        return element!!.thermoMeterString(side)
    }

    override fun readConfigTool(side: Direction?, tag: NBTTagCompound?, invoker: EntityPlayer?): Boolean {
        if (element is IConfigurable) {
            (element as IConfigurable).readConfigTool(tag, invoker)
            return true
        }
        return false
    }

    override fun writeConfigTool(side: Direction?, tag: NBTTagCompound?, invoker: EntityPlayer?): Boolean {
        if (element is IConfigurable) {
            (element as IConfigurable).writeConfigTool(tag, invoker)
            return true
        }
        return false
    }

    val fluidHandler: IFluidHandler?
        get() = element!!.getFluidHandler()

    override fun publishSerialize(stream: DataOutputStream) {
        super.publishSerialize(stream)
        try {
            stream.writeShort(elementId)
            element!!.networkSerialize(stream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    enum class FrontType {
        BlockSide, PlayerView, PlayerViewHorizontal, BlockSideInv
    }

    override fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        try {
            val descriptor = Eln.transparentNodeItem.getDescriptor(itemStack)
            val metadata = itemStack!!.itemDamage
            elementId = metadata
            element = descriptor!!.ElementClass.getConstructor(TransparentNode::class.java, TransparentNodeDescriptor::class.java).newInstance(this, descriptor) as TransparentNodeElement
            element!!.initializeFromThat(front, entityLiving, itemStack.tagCompound)
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        println("TN.iFT element = $element elId = $elementId")
    }

    override fun initializeFromNBT() {
        element!!.initialize()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return if (element!!.onBlockActivated(entityPlayer, side, vx, vy, vz)) true else super.onBlockActivated(entityPlayer, side, vx, vy, vz)
    }

    override fun hasGui(side: Direction): Boolean {
        return if (element == null) false else element!!.hasGui()
    }

    fun getInventory(@Suppress("UNUSED_PARAMETER") side: Direction?): IInventory? {
        return if (element == null) null else element!!.inventory
    }

    fun newContainer(side: Direction, player: EntityPlayer): Container? {
        return if (element == null) null else element!!.newContainer(side, player)
    }

    override val blockMetadata: Int
        get() {
            println("TN.gBM")
            println(element)
            println(element!!.transparentNodeDescriptor)
            return element!!.transparentNodeDescriptor.tileEntityMetaTag.meta
        }

    override fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP?) {
        super.networkUnserialize(stream, player)
        try {
            if (elementId == stream.readShort().toInt()) {
                element!!.networkUnserialize(stream, player)
            } else {
                println("Transparent node unserialize miss")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun connectJob() {
        super.connectJob()
        element!!.connectJob()
    }

    override fun disconnectJob() {
        super.disconnectJob()
        element!!.disconnectJob()
    }

    override fun checkCanStay(onCreate: Boolean) {
        super.checkCanStay(onCreate)
        element!!.checkCanStay(onCreate)
    }

    fun dropElement(entityPlayer: EntityPlayerMP?) {
        if (element != null) if (mustDropItem(entityPlayer)) dropItem(element!!.dropItemStack)
    }

    override val nodeUuid: String
        get() = Eln.transparentNodeBlock.nodeUuid

    override fun unload() {
        super.unload()
        if (element != null) element!!.unload()
    }
}
