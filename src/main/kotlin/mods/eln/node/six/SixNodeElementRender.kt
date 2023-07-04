package mods.eln.node.six

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.client.ClientProxy
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.LRDU.Companion.fromInt
import mods.eln.misc.LRDUMask
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.misc.UtilsClient
import mods.eln.misc.UtilsClient.bindTexture
import mods.eln.misc.UtilsClient.distanceFromClientPlayer
import mods.eln.misc.UtilsClient.drawConnectionPinSixNode
import mods.eln.misc.UtilsClient.glDeleteListsSafe
import mods.eln.misc.UtilsClient.glGenListsSafe
import mods.eln.sound.LoopedSound
import mods.eln.sound.LoopedSoundManager
import mods.eln.sound.SoundCommand
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import org.lwjgl.opengl.GL11
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

abstract class SixNodeElementRender(open var tileEntity: SixNodeEntity, @JvmField var side: Direction, @JvmField var sixNodeDescriptor: SixNodeDescriptor) {
    @JvmField
    var connectedSide = LRDUMask()
    var glList = 0
    var cableList: IntArray = IntArray(4)
    var cableListReady = booleanArrayOf(false, false, false, false)
    var glListReady = false
    fun needRedrawCable() {
        needRedraw = true
    }

    fun drawPowerPin(d: FloatArray?) {
        drawPowerPin(front, d)
    }

    fun drawPowerPin(front: LRDU?, d: FloatArray?) {
        if (distanceFromClientPlayer(tileEntity) > 20) return
        GL11.glColor3f(0f, 0f, 0f)
        drawConnectionPinSixNode(front!!, d!!, 1.8f, 0.9f)
        GL11.glColor3f(1f, 1f, 1f)
    }

    fun drawPowerPinWhite(front: LRDU?, d: FloatArray?) {
        if (distanceFromClientPlayer(tileEntity) > 20) return
        drawConnectionPinSixNode(front!!, d!!, 1.8f, 0.9f)
    }

    fun drawSignalPin(d: FloatArray?) {
        drawSignalPin(front, d)
    }

    fun drawSignalPin(front: LRDU?, d: FloatArray?) {
        if (distanceFromClientPlayer(tileEntity) > 20) return
        GL11.glColor3f(0f, 0f, 0f)
        drawConnectionPinSixNode(front!!, d!!, 0.9f, 0.9f)
        GL11.glColor3f(1f, 1f, 1f)
    }

    var needRedraw = false
    open fun newConnectionType(connectionType: CableRenderType?) {}
    open fun drawCables() {
        for (idx in 0..3) {
            val render = getCableRender(fromInt(idx))
            cableListReady[idx] = false
            if (render != null && connectedSide.mask and (1 shl idx) != 0) {
                GL11.glNewList(cableList[idx], GL11.GL_COMPILE)
                CableRender.drawCable(render, LRDUMask(1 shl idx), connectionType)
                GL11.glEndList()
                cableListReady[idx] = true
            }
        }
    }

    open fun draw() {
        if (needRedraw) {
            needRedraw = false
            connectionType = CableRender.connectionType(this, side)
            newConnectionType(connectionType)
            if (drawCableAuto()) {
                drawCables()
            }
        }
        for (idx in 0..3) {
            setGlColorFromDye(connectionType!!.otherdry[idx])
            if (cableListReady[idx]) {
                bindTexture(getCableRender(fromInt(idx))!!.cableTexture)
                GL11.glCallList(cableList[idx])
            }
        }
        GL11.glColor3f(1f, 1f, 1f)
    }

    open fun drawCableAuto(): Boolean {
        return true
    }

    open fun glListEnable(): Boolean {
        return true
    }

    fun glListCall() {
        if (!glListEnable()) return
        if (!glListReady) {
            GL11.glNewList(glList, GL11.GL_COMPILE)
            glListDraw()
            GL11.glEndList()
            glListReady = true
        }
        GL11.glCallList(glList)
    }

    open fun glListDraw() {}
    @JvmField
    var front: LRDU? = null
    var connectionType: CableRenderType? = null
    open fun isProvidingWeakPower(side: Direction?): Int {
        return 0
    }

    open fun publishUnserialize(stream: DataInputStream) {
        try {
            val b = stream.readByte()
            connectedSide.set(b.toInt() and 0xF)
            front = fromInt(b.toInt() shr 4 and 0x3)
            needRedraw = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        glListReady = false
    }

    fun singleUnserialize(@Suppress("UNUSED_PARAMETER") stream: DataInputStream?) {}
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

    fun destructor() {
        if (usedUuid()) ClientProxy.uuidManager.kill(uuid)
        if (glListEnable()) {
            glDeleteListsSafe(glList)
        }
        glDeleteListsSafe(cableList[0])
        glDeleteListsSafe(cableList[1])
        glDeleteListsSafe(cableList[2])
        glDeleteListsSafe(cableList[3])
        loopedSoundManager.dispose()
    }

    open fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? {
        return null
    }

    open val inventory: IInventory?
        get() = null

    fun preparePacketForServer(stream: DataOutputStream) {
        try {
            tileEntity.preparePacketForServer(stream)
            stream.writeByte(side.int)
            stream.writeShort(tileEntity.elementRenderIdList[side.int].toInt())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendPacketToServer(bos: ByteArrayOutputStream?) {
        tileEntity.sendPacketToServer(bos)
    }

    open fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return null
    }

    open fun getCableDry(lrdu: LRDU?): Int {
        return 0
    }

    fun clientSetFloat(id: Int, value: Float) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id)
            stream.writeFloat(value)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSetFloat(id: Int, value1: Float, value2: Float) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id)
            stream.writeFloat(value1)
            stream.writeFloat(value2)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSetDouble(id: Byte, value: Double) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeDouble(value)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSetString(id: Byte, text: String) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeUTF(text)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSetInt(id: Byte, value: Int) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeInt(value)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSetByte(id: Byte, value: Byte) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id.toInt())
            stream.writeByte(value.toInt())
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clientSend(id: Int) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            preparePacketForServer(stream)
            stream.writeByte(id)
            sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun cameraDrawOptimisation(): Boolean {
        return true
    }

    @Throws(IOException::class)
    open fun serverPacketUnserialize(stream: DataInputStream?) {
    }

    fun notifyNeighborSpawn() {
        needRedraw = true
    }

    private val loopedSoundManager = LoopedSoundManager()
    @SideOnly(Side.CLIENT)
    protected fun addLoopedSound(loopedSound: LoopedSound?) {
        loopedSoundManager.add(loopedSound)
    }

    open fun refresh(deltaT: Float) {
        loopedSoundManager.process(deltaT)
    }

    init {
        // this.descriptor = descriptor;
        if (glListEnable()) {
            glList = glGenListsSafe()
        }
        cableList[0] = glGenListsSafe()
        cableList[1] = glGenListsSafe()
        cableList[2] = glGenListsSafe()
        cableList[3] = glGenListsSafe()
    }
}
