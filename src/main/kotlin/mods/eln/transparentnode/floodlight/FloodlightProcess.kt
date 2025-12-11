package mods.eln.transparentnode.floodlight

import mods.eln.Eln
import mods.eln.item.LampDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.HybridNodeDirection
import mods.eln.misc.HybridNodeDirection.*
import mods.eln.misc.Utils.getItemObject
import mods.eln.server.SaveConfig
import mods.eln.sim.IProcess
import mods.eln.sixnode.lampsocket.LightBlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class FloodlightProcess(var element: FloodlightElement) : IProcess {

    override fun process(time: Double) {
        if (element.motorized) {
            element.swivelAngle = (element.swivelControl.normalized) * FloodlightGui.MAX_HORIZONTAL_ANGLE
            element.headAngle = (element.headControl.normalized) * FloodlightGui.MAX_VERTICAL_ANGLE
            element.shutterAngle = (element.shutterControl.normalized) * FloodlightGui.MAX_SHUTTER_ANGLE
        }

        val lampStacks = mutableListOf<ItemStack?>()
        val lampLightValues = mutableListOf<Int>()
        val lampLightRanges = mutableListOf<Int>()

        lampStacks.add(element.inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_1_ID))
        lampStacks.add(element.inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_2_ID))


        for (idx in 0 until lampStacks.size) {
            if (lampStacks[idx] != null) {
                val lampDescriptor = getItemObject(lampStacks[idx]) as LampDescriptor

                val lampVoltage = element.electricalLoad.voltage

                val num: Double = abs(lampVoltage) - lampDescriptor.minimalU
                val den: Double = lampDescriptor.nominalU - lampDescriptor.minimalU

                lampLightValues.add(((num / den) * lampDescriptor.nominalLight * LampDescriptor.MAX_LIGHT_VALUE).toInt())

                if (lampLightValues[idx] < LampDescriptor.MIN_LIGHT_VALUE) lampLightValues[idx] = LampDescriptor.MIN_LIGHT_VALUE
                else if (lampLightValues[idx] > LampDescriptor.MAX_LIGHT_VALUE) lampLightValues[idx] = LampDescriptor.MAX_LIGHT_VALUE

                lampLightRanges.add(lampDescriptor.range)

                val bulbCanAge = !Eln.halogenLampInfiniteLife && SaveConfig.instance!!.electricalLampAging

                if (bulbCanAge) {
                    val currentLife = lampDescriptor.ageLamp(lampStacks[idx]!!, lampVoltage, time)

                    if (currentLife <= 0.0) {
                        element.inventory.setInventorySlotContents(idx, null)
                        element.inventoryChange(element.inventory)
                    }
                }
            }
            else {
                lampLightValues.add(LampDescriptor.MIN_LIGHT_VALUE)
                lampLightRanges.add(0)
            }
        }

        val newLightValue = max(lampLightValues[FloodlightContainer.LAMP_SLOT_1_ID], lampLightValues[FloodlightContainer.LAMP_SLOT_2_ID])

        element.powered = newLightValue > LampDescriptor.MIN_LIGHT_VALUE
        element.lightRange = lampLightRanges[FloodlightContainer.LAMP_SLOT_1_ID] + lampLightRanges[FloodlightContainer.LAMP_SLOT_2_ID]

        updateBlockLight(newLightValue)
        placeSpots(newLightValue)
    }

    private fun updateBlockLight(newLight: Int) {
        element.node!!.lightValue = newLight

        val bos = ByteArrayOutputStream(64)
        val packet = DataOutputStream(bos)

        element.preparePacketForClient(packet)

        try {
            packet.writeInt(element.node!!.lightValue)
        }
        catch (e: IOException) {
            e.printStackTrace()
        }

        element.sendPacketToAllClient(bos)
    }

    private fun placeSpots(lightValue: Int) {
        val offsetAngle = (element.shutterAngle / 2.0)

        val rotationVectors = mutableListOf<Vec3>()
        val lightVectors = mutableListOf<Vec3>()
        val lbCoords = mutableListOf<Coordinate>()

        val (h1, k1) = calculateAdjustments(element.headAngle, offsetAngle)
        val (h2, k2) = calculateAdjustments(element.headAngle, (offsetAngle / 2.0))

        // Central spot
        rotationVectors.add(createRotationVector(element.swivelAngle, element.headAngle, element.rotationAxis, element.blockFacing))

        // Horizontal spots
        rotationVectors.add(createRotationVector((element.swivelAngle + h1), k1, element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector((element.swivelAngle - h1), k1, element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector((element.swivelAngle + h2), k2, element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector((element.swivelAngle - h2), k2, element.rotationAxis, element.blockFacing))

        // Vertical spots
        rotationVectors.add(createRotationVector(element.swivelAngle, (element.headAngle + offsetAngle), element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector(element.swivelAngle, (element.headAngle - offsetAngle), element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector(element.swivelAngle, (element.headAngle + (offsetAngle / 2.0)), element.rotationAxis, element.blockFacing))
        rotationVectors.add(createRotationVector(element.swivelAngle, (element.headAngle - (offsetAngle / 2.0)), element.rotationAxis, element.blockFacing))

        for (idx in 0 until rotationVectors.size) {
            lightVectors.add(element.node!!.coordinate.toVec3())

            lbCoords.add(Coordinate(lightVectors[idx]))

            for (jdx in 0 until element.lightRange) {
                if (lbCoords[idx].block.isOpaqueCube) break

                lightVectors[idx].xCoord += rotationVectors[idx].xCoord
                lightVectors[idx].yCoord += rotationVectors[idx].yCoord
                lightVectors[idx].zCoord += rotationVectors[idx].zCoord
                lbCoords[idx].setPosition(lightVectors[idx])

                if (!lbCoords[idx].blockExist) break

                if (jdx % 2 == 1) LightBlockEntity.addLight(Coordinate(lbCoords[idx]), lightValue, 5)
            }
        }
    }

    private fun toRadians(angle: Double): Double {
        return angle * (Math.PI / 180.0)
    }

    private fun toDegrees(angle: Double): Double {
        return angle * (180.0 / Math.PI)
    }

    private fun calculateAdjustments(vertAngle: Double, offsetAngle: Double): Pair<Double, Double> {
        val a = sqrt(cos(toRadians(offsetAngle)).pow(2) / (1 + tan(toRadians(vertAngle)).pow(2)))

        val h = toRadians(90.0) - atan(sign(cos(toRadians(vertAngle))) * (a / sin(toRadians(offsetAngle))))

        val num = cos(toRadians(offsetAngle)).pow(2) - a.pow(2)
        val den = sin(toRadians(offsetAngle)).pow(2) + a.pow(2)

        val k = atan(sign(sin(toRadians(vertAngle))) * sqrt(num / den))

        return Pair(toDegrees(h), toDegrees(k))
    }

    private fun getRawRotationVector(horzAngle: Double, vertAngle: Double): Vec3 {
        val horzSin = sin(toRadians(horzAngle))
        val horzCos = cos(toRadians(horzAngle))

        val vertSin = sin(toRadians(vertAngle))
        val vertCos = cos(toRadians(vertAngle))

        val v = Vec3.createVectorHelper(0.0, 0.0, 0.0)

        v.xCoord = vertCos * horzSin
        v.yCoord = vertSin
        v.zCoord = vertCos * horzCos

        return v
    }

    private fun createRotationVector(horzAngle: Double, vertAngle: Double, axis: HybridNodeDirection, facing: HybridNodeDirection): Vec3 {
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