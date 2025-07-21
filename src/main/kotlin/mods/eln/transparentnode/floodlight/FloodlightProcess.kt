package mods.eln.transparentnode.floodlight

import mods.eln.misc.Coordinate
import mods.eln.misc.HybridNodeDirection
import mods.eln.misc.HybridNodeDirection.*
import mods.eln.sim.IProcess
import mods.eln.sixnode.lampsocket.LightBlockEntity
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

        element.powered = newLightValue > 8

        placeSpot(newLightValue)
    }

    private fun placeSpot(lightValue: Int) {
        if (!element.lbCoord.blockExist) return

        val offsetAngle = (element.beamAngle / 2)

        val rotationVectors = mutableListOf<Vec3>()
        val lightVectors = mutableListOf<Vec3>()
        val lbCoords = mutableListOf<Coordinate>()

        rotationVectors.add(createRotationVector(element.swivelAngle, element.headAngle, element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector((element.swivelAngle + offsetAngle), element.headAngle, element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector((element.swivelAngle - offsetAngle), element.headAngle, element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector(element.swivelAngle, (element.headAngle + offsetAngle), element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector(element.swivelAngle, (element.headAngle - offsetAngle), element.rotationAxis, element.blockFacing))

        for (idx in 0 until rotationVectors.size) {
            lightVectors.add(element.node!!.coordinate.toVec3())

            lbCoords.add(Coordinate(lightVectors[idx]))

            //for (jdx in 0 until element.coneRange.int) {
            for (jdx in 0 until 16) {// TODO: not magic number
                if (lbCoords[idx].block.isOpaqueCube) break

                lightVectors[idx].xCoord += rotationVectors[idx].xCoord
                lightVectors[idx].yCoord += rotationVectors[idx].yCoord
                lightVectors[idx].zCoord += rotationVectors[idx].zCoord
                lbCoords[idx].setPosition(lightVectors[idx])

                if (!lbCoords[idx].blockExist) return

                if (jdx % 2 == 1) setLightAt(lbCoords[idx], lightValue)
            }
        }
    }

    private fun setLightAt(newLbCoord: Coordinate, newLight: Int) {
        element.lbCoord = Coordinate(newLbCoord)

        LightBlockEntity.addLight(element.lbCoord, newLight, 5)

        element.node!!.lightValue = newLight

        val bos = ByteArrayOutputStream(64)
        val packet = DataOutputStream(bos)

        element.preparePacketForClient(packet)

        try {
            packet.writeByte(element.node!!.lightValue)
        }
        catch (e: IOException) {
            e.printStackTrace()
        }

        element.sendPacketToAllClient(bos)
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

}