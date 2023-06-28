package mods.eln.node.transparent

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.FakeSideInventory.Companion.instance
import mods.eln.misc.LRDU
import mods.eln.node.NodeBlockEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.Container
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.InvocationTargetException

open class TransparentNodeEntity : NodeBlockEntity(), ISidedInventory {
    var elementRender: TransparentNodeElementRender? = null
    var elementRenderId: Short = 0

    override fun getCableRender(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        return if (elementRender == null) null else elementRender!!.getCableRenderSide(side, lrdu)
    }

    override fun serverPublishUnserialize(stream: DataInputStream) {
        super.serverPublishUnserialize(stream)
        try {
            val id = stream.readShort()
            if (id.toInt() == 0) {
                elementRenderId = 0.toShort()
                elementRender = null
            } else {
                if (id != elementRenderId) {
                    elementRenderId = id
                    val descriptor = Eln.transparentNodeItem.getDescriptor(id.toInt())
                    elementRender = descriptor!!.RenderClass.getConstructor(TransparentNodeEntity::class.java, TransparentNodeDescriptor::class.java).newInstance(this, descriptor) as TransparentNodeElementRender
                }
                elementRender!!.networkUnserialize(stream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
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
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container? {
        val n = node as TransparentNode? ?: return null
        return n.newContainer(side, player)
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? {
        return elementRender!!.newGuiDraw(side, player)
    }

    override fun preparePacketForServer(stream: DataOutputStream) {
        try {
            super.preparePacketForServer(stream)
            stream.writeShort(elementRenderId.toInt())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun sendPacketToServer(bos: ByteArrayOutputStream?) {
        super.sendPacketToServer(bos)
    }

    override fun cameraDrawOptimisation(): Boolean {
        return if (elementRender == null) super.cameraDrawOptimisation() else elementRender!!.cameraDrawOptimisation()
    }

    @Suppress("UNUSED_PARAMETER") fun getDamageValue(world: World, x: Int, y: Int, z: Int): Int {
        return if (world.isRemote) {
            elementRenderId.toInt()
        } else 0
    }

    override fun tileEntityNeighborSpawn() {
        if (elementRender != null) elementRender!!.notifyNeighborSpawn()
    }

    fun addCollisionBoxesToList(par5AxisAlignedBB: AxisAlignedBB, list: MutableList<AxisAlignedBB?>, blockCoord: Coordinate?) {
        val desc = if (worldObj.isRemote) {
            if (elementRender == null) null else elementRender!!.transparentNodedescriptor
        } else {
            val node = node as TransparentNode?
            if (node == null) null else node.element!!.transparentNodeDescriptor
        }
        val x: Int
        val y: Int
        val z: Int
        if (blockCoord != null) {
            x = blockCoord.x
            y = blockCoord.y
            z = blockCoord.z
        } else {
            x = xCoord
            y = yCoord
            z = zCoord
        }
        if (desc == null) {
            val bb = Blocks.stone.getCollisionBoundingBoxFromPool(worldObj, x, y, z)
            if (par5AxisAlignedBB.intersectsWith(bb)) list.add(bb)
        } else {
            desc.addCollisionBoxesToList(par5AxisAlignedBB, list, worldObj, x, y, z)
        }
    }

    override fun serverPacketUnserialize(stream: DataInputStream) {
        super.serverPacketUnserialize(stream)
        if (elementRender != null) elementRender!!.serverPacketUnserialize(stream)
    }

    override val nodeUuid: String
        get() = Eln.transparentNodeBlock.nodeUuid

    override fun destructor() {
        if (elementRender != null) elementRender!!.destructor()
        super.destructor()
    }

    override fun clientRefresh(deltaT: Float) {
        if (elementRender != null) {
            elementRender!!.refresh(deltaT)
        }
    }

    override fun isProvidingWeakPower(side: Direction?): Int {
        return 0
    }

    open val sidedInventory: ISidedInventory
        get() {
            if (worldObj.isRemote) {
                if (elementRender == null) return instance
                val i = elementRender!!.inventory
                if (i != null && i is ISidedInventory) {
                    return i
                }
            } else {
                val node = node
                if (node != null && node is TransparentNode) {
                    val i = node.getInventory(null)
                    if (i != null && i is ISidedInventory) {
                        return i
                    }
                }
            }
            return instance
        }

    override fun getSizeInventory(): Int {
        return sidedInventory.sizeInventory
    }

    override fun getStackInSlot(var1: Int): ItemStack? {
        return sidedInventory.getStackInSlot(var1)
    }

    override fun decrStackSize(var1: Int, var2: Int): ItemStack? {
        return sidedInventory.decrStackSize(var1, var2)
    }

    override fun getStackInSlotOnClosing(var1: Int): ItemStack? {
        return sidedInventory.getStackInSlotOnClosing(var1)
    }

    override fun setInventorySlotContents(var1: Int, var2: ItemStack?) {
        sidedInventory.setInventorySlotContents(var1, var2)
    }

    override fun getInventoryName(): String {
        return sidedInventory.inventoryName
    }

    override fun hasCustomInventoryName(): Boolean {
        return sidedInventory.hasCustomInventoryName()
    }

    override fun getInventoryStackLimit(): Int {
        return sidedInventory.inventoryStackLimit
    }

    override fun isUseableByPlayer(var1: EntityPlayer): Boolean {
        return sidedInventory.isUseableByPlayer(var1)
    }

    override fun openInventory() {
        sidedInventory.openInventory()
    }

    override fun closeInventory() {
        sidedInventory.closeInventory()
    }

    override fun isItemValidForSlot(var1: Int, var2: ItemStack): Boolean {
        return sidedInventory.isItemValidForSlot(var1, var2)
    }

    override fun getAccessibleSlotsFromSide(var1: Int): IntArray {
        return sidedInventory.getAccessibleSlotsFromSide(var1)
    }

    override fun canInsertItem(var1: Int, var2: ItemStack, var3: Int): Boolean {
        return sidedInventory.canInsertItem(var1, var2, var3)
    }

    override fun canExtractItem(var1: Int, var2: ItemStack, var3: Int): Boolean {
        return sidedInventory.canExtractItem(var1, var2, var3)
    }
}
