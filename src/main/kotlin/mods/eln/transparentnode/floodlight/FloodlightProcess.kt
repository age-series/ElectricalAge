package mods.eln.transparentnode.floodlight

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.HybridNodeDirection
import mods.eln.misc.HybridNodeDirection.*
import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import mods.eln.sixnode.lampsocket.LightBlockEntity
import net.minecraft.init.Blocks
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class FloodlightProcess(var element: FloodlightElement) : IProcess {

    override fun process(time: Double) {
        if (element.motorized) {
            element.swivelAngle = (element.swivelControl.normalized).toFloat() * 360f
            element.headAngle = (element.headControl.normalized).toFloat() * 180f
        }

        val lamp1Stack = element.inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_1_ID)
        val lamp2Stack = element.inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_2_ID)

        val newLightValue = if (lamp1Stack != null || lamp2Stack != null) {
            (((abs(element.electricalLoad.voltage) - 150) / 3.3333).toInt()).coerceIn(0, 15)
        } else {
            0
        }

        placeSpot(newLightValue)

        element.powered = element.node!!.lightValue > 8
        // if ((lamp1Stack != null && lamp2Stack == null) || (lamp1Stack == null && lamp2Stack != null)) element.node!!.lightValue /= 2
    }

    private fun placeSpot(newLight: Int) {
        var exit = false

        if (!element.lbCoord.blockExist) return

        val vv = Vec3.createVectorHelper(1.0, 0.0, 0.0)
        val vp = Utils.getVec05(element.node!!.coordinate)

        element.blockFacing.rotateFromXP(vv)

        rotateAroundFacing(vv, element.rotationAxis, element.swivelAngle, element.headAngle)

        val newCoord = Coordinate(element.node!!.coordinate)
        for (idx in 0 until element.coneRange.int) {
            vp.xCoord += vv.xCoord
            vp.yCoord += vv.yCoord
            vp.zCoord += vv.zCoord
            newCoord.setPosition(vp)
            if (!newCoord.blockExist) {
                exit = true
                break
            }
            if (isOpaque(newCoord)) {
                vp.xCoord -= vv.xCoord
                vp.yCoord -= vv.yCoord
                vp.zCoord -= vv.zCoord
                newCoord.setPosition(vp)
                break
            }
        }
        if (!exit) {
            while (newCoord != element.node!!.coordinate) {
                val block = newCoord.block
                if (block === Blocks.air || block === Eln.lightBlock) break
                vp.xCoord -= vv.xCoord
                vp.yCoord -= vv.yCoord
                vp.zCoord -= vv.zCoord
                newCoord.setPosition(vp)
            }
        }
        if (!exit) setLightAt(newCoord, newLight)
    }

    private fun rotateAroundFacing(v: Vec3, axis: HybridNodeDirection, horzAngle: Float, vertAngle: Float) {
        val horzSin = MathHelper.sin(horzAngle * (Math.PI / 180.0).toFloat())
        val horzCos = MathHelper.cos(horzAngle * (Math.PI / 180.0).toFloat())

        val vertSin = MathHelper.sin(vertAngle * (Math.PI / 180.0).toFloat())
        val vertCos = MathHelper.cos(vertAngle * (Math.PI / 180.0).toFloat())

        when (axis) {
            XN -> {

            }
            XP -> {

            }
            YN -> {
                // TODO: Fix overlapping horizontal and vertical rotations
                // TODO: Offset negative directions by 1
                // TODO: Add other rotation directions
            }
            YP -> {
                v.xCoord = horzCos.toDouble()
                v.yCoord = vertSin.toDouble()
                v.zCoord = -horzSin.toDouble()
            }
            ZN -> {

            }
            ZP -> {

            }
        }
    }

    private fun rotateAroundZ(v: Vec3, par1: Float) {
        val f1 = MathHelper.cos(par1)
        val f2 = MathHelper.sin(par1)
        val d0 = v.xCoord * f1.toDouble() + v.yCoord * f2.toDouble()
        val d1 = v.yCoord * f1.toDouble() - v.xCoord * f2.toDouble()
        val d2 = v.zCoord
        v.xCoord = d0
        v.yCoord = d1
        v.zCoord = d2
    }

    private fun isOpaque(coord: Coordinate): Boolean {
        val block = coord.block
        return block !== Blocks.air && (block.isOpaqueCube && block !== Blocks.farmland)
    }

    private fun setLightAt(newLbCoord: Coordinate, newLight: Int) {
        val oldLbCoord = element.lbCoord
        val oldLight = element.node!!.lightValue

        // TODO: Issue here with detecting when to update light value of floodlight block
        if (newLbCoord != oldLbCoord) {
            element.lbCoord = Coordinate(newLbCoord)
            if ((oldLbCoord == element.node!!.coordinate) || ((newLbCoord == element.node!!.coordinate) && (newLight != oldLight))) {
                element.node!!.lightValue = newLight
            }
        }

        if (newLbCoord != element.node!!.coordinate) {
            LightBlockEntity.addLight(Coordinate(newLbCoord), newLight, 5)
        }

        if (newLight != oldLight) {
            val bos = ByteArrayOutputStream(64)
            val packet = DataOutputStream(bos)
            element.preparePacketForClient(packet)
            try {
                packet.writeByte(element.node!!.lightValue)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            element.sendPacketToAllClient(bos)
        }
    }

}