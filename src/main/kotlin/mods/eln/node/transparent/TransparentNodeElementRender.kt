@file:Suppress("NAME_SHADOWING")
package mods.eln.node.transparent

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.client.ClientProxy
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.Direction.Companion.fromInt
import mods.eln.misc.LRDU
import mods.eln.misc.LRDUMask
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.misc.Utils.unserializeItemStackToEntityItem
import mods.eln.misc.UtilsClient
import mods.eln.sound.LoopedSound
import mods.eln.sound.LoopedSoundManager
import mods.eln.sound.SoundCommand
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import org.lwjgl.opengl.GL11
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

abstract class TransparentNodeElementRender(var tileEntity: TransparentNodeEntity, var transparentNodedescriptor: TransparentNodeDescriptor) {
    @JvmField
    var front: Direction? = null
    var grounded = false
    @Throws(IOException::class)
    protected fun unserializeItemStackToEntityItem(stream: DataInputStream?, old: EntityItem?): EntityItem? {
        return unserializeItemStackToEntityItem(stream!!, old, tileEntity)
    }

    fun drawEntityItem(entityItem: EntityItem?, x: Double, y: Double, z: Double, roty: Float, scale: Float) {
        UtilsClient.drawEntityItem(entityItem, x, y, z, roty, scale)
    }

    fun glCableTransform(inverse: Direction) {
        inverse.glTranslate(0.5f)
        inverse.glRotateXnRef()
    }

    abstract fun draw()
    open fun networkUnserialize(stream: DataInputStream) {
        try {
            val b = stream.readByte()
            front = fromInt(b.toInt() and 0x7)
            grounded = b.toInt() and 8 != 0
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? {
        return null
    }

    open val inventory: IInventory?
        get() = null

    fun preparePacketForServer(stream: DataOutputStream?) {
        tileEntity.preparePacketForServer(stream!!)
    }

    fun sendPacketToServer(bos: ByteArrayOutputStream?) {
        tileEntity.sendPacketToServer(bos)
    }

    fun clientSetGrounded(value: Boolean) {
        clientSendBoolean(TransparentNodeElement.unserializeGroundedId, value)
    }

    fun clientSendBoolean(id: Byte, value: Boolean) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeByte(if (value) 1 else 0)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSendId(id: Byte) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSendString(id: Byte, str: String) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeUTF(str)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSendFloat(id: Byte, str: Float) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeFloat(str)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSendInt(id: Byte, str: Int) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeInt(str)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun cameraDrawOptimisation(): Boolean {
        return true
    }

    open fun getCableRenderSide(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        return null
    }

    fun drawCable(side: Direction, render: CableRenderDescriptor?, connection: LRDUMask, renderPreProcess: CableRenderType?): CableRenderType? {
        return this.drawCable(side, render, connection, renderPreProcess, false)
    }

    fun drawCable(side: Direction, render: CableRenderDescriptor?, connection: LRDUMask, renderPreProcess: CableRenderType?, drawBottom: Boolean): CableRenderType? {
        var renderPreProcess = renderPreProcess
        if (render == null) return renderPreProcess
        if (renderPreProcess == null) renderPreProcess = CableRender.connectionType(tileEntity, connection, side)
        GL11.glPushMatrix()
        glCableTransform(side)
        render.bindCableTexture()
        for (lrdu in LRDU.values()) {
            setGlColorFromDye(renderPreProcess!!.otherdry[lrdu.toInt()])
            if (!connection[lrdu]) continue
            maskTempDraw.set(1 shl lrdu.toInt())
            CableRender.drawCable(render, maskTempDraw, renderPreProcess, render.widthDiv2 / 2f, drawBottom)
        }
        GL11.glPopMatrix()
        GL11.glColor3f(1f, 1f, 1f)
        return renderPreProcess
    }

    open fun notifyNeighborSpawn() {}
    open fun serverPacketUnserialize(stream: DataInputStream?) {}
    protected fun coordinate(): Coordinate {
        return Coordinate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity.worldObj)
    }

    private var uuid = 0
    fun getUuid(): Int {
        if (uuid == 0) {
            uuid = UtilsClient.uuid
        }
        return uuid
    }

    fun usedUuid(): Boolean {
        return uuid != 0
    }

    fun play(s: SoundCommand) {
        s.addUuid(getUuid())
        s.set(tileEntity)
        s.play()
    }

    private val loopedSoundManager = LoopedSoundManager()
    @SideOnly(Side.CLIENT)
    protected fun addLoopedSound(loopedSound: LoopedSound?) {
        loopedSoundManager.add(loopedSound)
    }

    fun destructor() {
        if (usedUuid()) ClientProxy.uuidManager.kill(uuid)
        loopedSoundManager.dispose()
    }

    open fun refresh(deltaT: Float) {
        loopedSoundManager.process(deltaT)
    }

    companion object {
        val maskTempDraw = LRDUMask()
    }
}
