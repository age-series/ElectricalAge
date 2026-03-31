package mods.eln.sixnode.lampsocket

import mods.eln.cable.CableRenderDescriptor
import mods.eln.item.LampDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.getItemObject
import mods.eln.misc.Utils.unserialiseItemStack
import mods.eln.misc.UtilsClient
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import mods.eln.sixnode.lampsocket.objrender.LampSocketSuspendedObjRender
import mods.eln.sound.SoundCommand
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.EnumSkyBlock
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// TODO: Revisit integration of this file with the rest of the six-node lamp socket code.
class LampSocketRender(tileEntity: SixNodeEntity, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, sixNodeDescriptor) {

    override val inventory = SixNodeElementInventory(2, 64, this)
    val descriptor = sixNodeDescriptor as LampSocketDescriptor

    var lampDescriptor: LampDescriptor? = null
    private var cableDescriptor: GenericCableDescriptor? = null

    var grounded = true
    var lampSupplyChannel = "Default channel"
    var poweredByLampSupply = true
    var activeLampSupplyConnection = false
    var paintColor = 15
    var alphaZ = 0.0

    var light = 0
        set(newLight) {
            field = newLight

            if (lampDescriptor != null && lampDescriptor!!.lampData.technology.lampType == "fluorescent" && oldLight != -1 && oldLight < 9 && light >= 9) {
                val rand = Math.random()
                if (rand > 0.1) play(
                    SoundCommand("eln:neon_lamp").mulVolume(0.7f, (1.0 + (rand / 6.0)).toFloat()).smallRange()
                )
                else play(SoundCommand("eln:NEON_LFNOISE").mulVolume(0.2f, 1f).verySmallRange())
            }

            oldLight = light
        }
    private var oldLight = -1

    var lampInInventory = false

    var perturbPy = 0.0
    var perturbPz = 0.0
    private var perturbVy = 0.0
    private var perturbVz = 0.0
    private var weatherAlphaY = 0.0
    private var weatherAlphaZ = 0.0

    private var entityTimeout = 0.0
    private var entityList: MutableList<Any?> = arrayListOf()

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)

        try {
            grounded = stream.readBoolean()
            lampSupplyChannel = stream.readUTF()
            poweredByLampSupply = stream.readBoolean()
            activeLampSupplyConnection = stream.readBoolean()
            paintColor = stream.readInt()
            alphaZ = stream.readDouble()
            light = stream.readInt()
            lampInInventory = stream.readBoolean()

            val lampStack = unserialiseItemStack(stream)
            if (lampStack != null) lampDescriptor = getItemObject(lampStack) as LampDescriptor

            val cableStack = unserialiseItemStack(stream)
            if (cableStack != null) cableDescriptor = getItemObject(cableStack) as GenericCableDescriptor
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun serverPacketUnserialize(stream: DataInputStream?) {
        super.serverPacketUnserialize(stream)
        light = stream!!.readByte().toInt()
    }

    override fun draw() {
        // Only for colored cables
        super.draw()

        GL11.glRotated(descriptor.initialRotateDeg.toDouble(), 1.0, 0.0, 0.0)
        descriptor.render.draw(this, UtilsClient.distanceFromClientPlayer(this.tileEntity).toDouble())
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return LampSocketGui(player, this)
    }

    override fun refresh(deltaT: Float) {
        if (descriptor.render is LampSocketSuspendedObjRender) {
            entityTimeout -= deltaT

            if (entityTimeout < 0) {
                entityList = tileEntity.getWorldObj().getEntitiesWithinAABB(Entity::class.java, Coordinate(
                    tileEntity.xCoord,
                    tileEntity.yCoord - 2,
                    tileEntity.zCoord,
                    tileEntity.getWorldObj()
                ).getAxisAlignedBB(2))
                entityTimeout = 0.1
            }

            for (o in entityList) {
                val e = o as Entity
                var eFactor = 0

                if (e is EntityArrow) eFactor = 1
                if (e is EntityLivingBase) eFactor = 4
                if (eFactor == 0) continue

                perturbVy += (e.motionZ * eFactor * deltaT)
                perturbVz += (e.motionX * eFactor * deltaT)
            }

            if (tileEntity.getWorldObj().getSavedLightValue(EnumSkyBlock.Sky, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord) > 3) {
                val weather = (UtilsClient.getWeather(tileEntity.getWorldObj()) * 0.9) + 0.1

                // TODO: Reduce swinging of lamps to some degree?
                weatherAlphaY += ((0.4 - Math.random()) * deltaT * (Math.PI / 0.2) * weather)
                weatherAlphaZ += ((0.4 - Math.random()) * deltaT * (Math.PI / 0.2) * weather)

                if (weatherAlphaY > (2 * Math.PI)) weatherAlphaY -= (2 * Math.PI)
                if (weatherAlphaZ > (2 * Math.PI)) weatherAlphaZ -= (2 * Math.PI)

                perturbVy += (Math.random() * sin(weatherAlphaY) * weather.pow(2) * deltaT * 3)
                perturbVz += (Math.random() * cos(weatherAlphaY) * weather.pow(2) * deltaT * 3)

                perturbVy += (0.4 * deltaT * weather * sign(perturbVy) * Math.random())
                perturbVz += (0.4 * deltaT * weather * sign(perturbVz) * Math.random())
            }

            perturbVy -= (perturbPy / 10.0) * deltaT
            perturbVy *= (1 - (0.2 * deltaT))
            perturbPy += perturbVy

            perturbVz -= (perturbPz / 10.0) * deltaT
            perturbVz *= (1 - (0.2 * deltaT))
            perturbPz += perturbVz
        }
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        if (cableDescriptor == null
            || (lrdu == front && !descriptor.cableFront)
            || (lrdu == front!!.left() && !descriptor.cableLeft)
            || (lrdu == front!!.right() && !descriptor.cableRight)
            || (lrdu == front!!.inverse() && !descriptor.cableBack)
        ) return null

        return cableDescriptor!!.render
    }

    override fun getRenderBoundingBox(tileEntity: SixNodeEntity): AxisAlignedBB? {
        if (!descriptor.extendedRenderBounds) return null

        return AxisAlignedBB.getBoundingBox(
            tileEntity.xCoord - 1.0,
            tileEntity.yCoord.toDouble(),
            tileEntity.zCoord - 1.0,
            tileEntity.xCoord + 2.0,
            tileEntity.yCoord + 4.0,
            tileEntity.zCoord + 2.0
        )
    }

    override fun cameraDrawOptimisation(): Boolean {
        return descriptor.cameraOpt
    }

}