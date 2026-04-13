package mods.eln.sixnode.lampsocket

import mods.eln.cable.CableRenderDescriptor
import mods.eln.item.lampitem.LampDescriptor
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

    companion object {
        const val MIN_LIGHT_ON_VALUE = 8
        const val DEFAULT_PAINT_COLOR = 15
    }

    override val inventory = SixNodeElementInventory(2, 64, this)
    val descriptor = sixNodeDescriptor as LampSocketDescriptor

    var lampDescriptor: LampDescriptor? = null
    private var cableDescriptor: GenericCableDescriptor? = null

    var poweredByLampSupply = true
    var lampSupplyChannel = "Default channel"
    var activeLampSupplyConnection = false
    var projectionRotationAngle = 0.0
    var paintColor = DEFAULT_PAINT_COLOR
    var grounded = true

    private var boot = true

    var lightValue = 0
        set(newLight) {
            field = newLight

            if (lampDescriptor != null && lampDescriptor!!.lampData.technology.lampType == "fluorescent") {
                if (MIN_LIGHT_ON_VALUE in (oldLightValue + 1)..lightValue) {
                    val rand = Math.random()
                    if (rand > 0.1) play(SoundCommand("eln:neon_lamp").mulVolume(0.7f, (1.0 + (rand / 6.0)).toFloat()).smallRange())
                    else play(SoundCommand("eln:NEON_LFNOISE").mulVolume(0.2f, 1f).verySmallRange())
                }

                oldLightValue = lightValue
            }
        }
    private var oldLightValue = 0

    var perturbPy = 0.0
    var perturbPz = 0.0
    private var perturbVy = 0.0
    private var perturbVz = 0.0
    private var weatherAngleY = 0.0
    private var weatherAngleZ = 0.0

    private var entityTimeout = 0.0
    private var entityList: MutableList<Any?> = arrayListOf()

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)

        try {
            poweredByLampSupply = stream.readBoolean()
            lampSupplyChannel = stream.readUTF()
            activeLampSupplyConnection = stream.readBoolean()
            projectionRotationAngle = stream.readDouble()
            paintColor = stream.readInt()
            grounded = stream.readBoolean()
            lightValue = stream.readInt()

            if (boot) {
                oldLightValue = lightValue
                boot = false
            }

            val lampStack = unserialiseItemStack(stream)
            lampDescriptor = if (lampStack != null) getItemObject(lampStack) as LampDescriptor else null

            val cableStack = unserialiseItemStack(stream)
            cableDescriptor = if (cableStack != null) getItemObject(cableStack) as GenericCableDescriptor else null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun serverPacketUnserialize(stream: DataInputStream?) {
        super.serverPacketUnserialize(stream)
        lightValue = stream!!.readInt()
    }

    override fun draw() {
        super.draw() // Only for colored cables

        GL11.glRotated(descriptor.initialRenderAngleOffset, 1.0, 0.0, 0.0)
        descriptor.renderType.draw(this, UtilsClient.distanceFromClientPlayer(this.tileEntity).toDouble())
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return LampSocketGui(player, this)
    }

    override fun refresh(deltaT: Float) {
        if (descriptor.renderType is LampSocketSuspendedObjRender) {
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
                weatherAngleY += ((0.4 - Math.random()) * deltaT * (Math.PI / 0.2) * weather)
                weatherAngleZ += ((0.4 - Math.random()) * deltaT * (Math.PI / 0.2) * weather)

                if (weatherAngleY > (2 * Math.PI)) weatherAngleY -= (2 * Math.PI)
                if (weatherAngleZ > (2 * Math.PI)) weatherAngleZ -= (2 * Math.PI)

                perturbVy += (Math.random() * sin(weatherAngleY) * weather.pow(2) * deltaT * 3)
                perturbVz += (Math.random() * cos(weatherAngleY) * weather.pow(2) * deltaT * 3)

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
            || (lrdu == front!!.left() && !descriptor.renderSideCables)
            || (lrdu == front!!.right() && !descriptor.renderSideCables)
        ) return null

        return cableDescriptor!!.render
    }

    override fun getRenderBoundingBox(tileEntity: SixNodeEntity): AxisAlignedBB? {
        if (!descriptor.extendedRenderBounds) return null

        return AxisAlignedBB.getBoundingBox(
            (tileEntity.xCoord - 1).toDouble(),
            tileEntity.yCoord.toDouble(),
            (tileEntity.zCoord - 1).toDouble(),
            (tileEntity.xCoord + 2).toDouble(),
            (tileEntity.yCoord + 4).toDouble(),
            (tileEntity.zCoord + 2).toDouble()
        )
    }

}