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

        val vv = createRotationVector(element.swivelAngle, element.headAngle, element.rotationAxis, element.blockFacing)

        val vp = Utils.getVec05(element.node!!.coordinate)

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
                // TODO: Offset negative directions by 1
                // TODO: Also fix air detection to not be offset by 1
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

    private fun createRotationVector(horzAngle: Float, vertAngle: Float, axis: HybridNodeDirection, facing: HybridNodeDirection): Vec3 {
        val oldV = getRawRotationVector(horzAngle, vertAngle)

        val newV = Vec3.createVectorHelper(0.0, 0.0, 0.0)

        when (axis) {
            XN -> {
                newV.xCoord = -oldV.yCoord

                when (facing) {
                    XN -> TODO("unused - impossible facing direction")
                    XP -> TODO("unused - impossible facing direction")
                    YN -> {
                        newV.yCoord = -oldV.zCoord
                        newV.zCoord = oldV.xCoord
                    }
                    YP -> {
                        newV.yCoord = oldV.zCoord
                        newV.zCoord = -oldV.xCoord
                    }
                    ZN -> {
                        newV.yCoord = -oldV.xCoord
                        newV.zCoord = -oldV.zCoord
                    }
                    ZP -> {
                        newV.yCoord = oldV.xCoord
                        newV.zCoord = oldV.zCoord
                    }
                }
            }
            XP -> {
                newV.xCoord = oldV.yCoord

                when (facing) {
                    XN -> TODO("unused - impossible facing direction")
                    XP -> TODO("unused - impossible facing direction")
                    YN -> {
                        newV.yCoord = -oldV.zCoord
                        newV.zCoord = -oldV.xCoord
                    }
                    YP -> {
                        newV.yCoord = oldV.zCoord
                        newV.zCoord = oldV.xCoord
                    }
                    ZN -> {
                        newV.yCoord = oldV.xCoord
                        newV.zCoord = -oldV.zCoord
                    }
                    ZP -> {
                        newV.yCoord = -oldV.xCoord
                        newV.zCoord = oldV.zCoord
                    }
                }
            }
            YN -> {
                newV.yCoord = -oldV.yCoord

                when (facing) {
                    XN -> {
                        newV.xCoord = -oldV.zCoord
                        newV.zCoord = -oldV.xCoord
                    }
                    XP -> {
                        newV.xCoord = oldV.zCoord
                        newV.zCoord = oldV.xCoord
                    }
                    YN -> TODO("unused - impossible facing direction")
                    YP -> TODO("unused - impossible facing direction")
                    ZN -> {
                        newV.xCoord = oldV.xCoord
                        newV.zCoord = -oldV.zCoord
                    }
                    ZP -> {
                        newV.xCoord = -oldV.xCoord
                        newV.zCoord = oldV.zCoord
                    }
                }
            }
            YP -> {
                newV.yCoord = oldV.yCoord

                when (facing) {
                    XN -> {
                        newV.xCoord = -oldV.zCoord
                        newV.zCoord = oldV.xCoord
                    }
                    XP -> {
                        newV.xCoord = oldV.zCoord
                        newV.zCoord = -oldV.xCoord
                    }
                    YN -> TODO("unused - impossible facing direction")
                    YP -> TODO("unused - impossible facing direction")
                    ZN -> {
                        newV.xCoord = -oldV.xCoord
                        newV.zCoord = -oldV.zCoord
                    }
                    ZP -> {
                        newV.xCoord = oldV.xCoord
                        newV.zCoord = oldV.zCoord
                    }
                }
            }
            ZN -> {
                newV.zCoord = -oldV.yCoord

                when (facing) {
                    XN -> {
                        newV.xCoord = -oldV.zCoord
                        newV.yCoord = oldV.xCoord
                    }
                    XP -> {
                        newV.xCoord = oldV.zCoord
                        newV.yCoord = -oldV.xCoord
                    }
                    YN -> {
                        newV.xCoord = -oldV.xCoord
                        newV.yCoord = -oldV.zCoord
                    }
                    YP -> {
                        newV.xCoord = oldV.xCoord
                        newV.yCoord = oldV.zCoord
                    }
                    ZN -> TODO("unused - impossible facing direction")
                    ZP -> TODO("unused - impossible facing direction")
                }
            }
            ZP -> {
                newV.zCoord = oldV.yCoord

                when (facing) {
                    XN -> {
                        newV.xCoord = -oldV.zCoord
                        newV.yCoord = -oldV.xCoord
                    }
                    XP -> {
                        newV.xCoord = oldV.zCoord
                        newV.yCoord = oldV.xCoord
                    }
                    YN -> {
                        newV.xCoord = oldV.xCoord
                        newV.yCoord = -oldV.zCoord
                    }
                    YP -> {
                        newV.xCoord = -oldV.xCoord
                        newV.yCoord = oldV.zCoord
                    }
                    ZN -> TODO("unused - impossible facing direction")
                    ZP -> TODO("unused - impossible facing direction")
                }
            }
        }

        return newV
    }

    private fun getRawRotationVector(horzAngle: Float, vertAngle: Float): Vec3 {
        val horzSin = MathHelper.sin(horzAngle * (Math.PI / 180.0).toFloat())
        val horzCos = MathHelper.cos(horzAngle * (Math.PI / 180.0).toFloat())

        val vertSin = MathHelper.sin(vertAngle * (Math.PI / 180.0).toFloat())
        val vertCos = MathHelper.cos(vertAngle * (Math.PI / 180.0).toFloat())

        val v = Vec3.createVectorHelper(0.0, 0.0, 0.0)

        v.xCoord = vertCos.toDouble() * horzSin.toDouble()
        v.yCoord = vertSin.toDouble()
        v.zCoord = vertCos.toDouble() * horzCos.toDouble()

        return v
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
            /*
            if ((oldLbCoord == element.node!!.coordinate) || ((newLbCoord == element.node!!.coordinate) && (newLight != oldLight))) {
                element.node!!.lightValue = newLight
            }
            */
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