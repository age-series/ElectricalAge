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
import kotlin.math.acos
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
            element.beamWidth = (element.beamControl.normalized) * FloodlightGui.MAX_BEAM_WIDTH
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
        val rotationVectors = mutableListOf<Pair<Vec3, Double>>()

        val rotationAxis = element.rotationAxis
        val facingDirection = element.blockFacing

        val horzAngle = element.swivelAngle
        val vertAngle = element.headAngle
        val offsetAngle = element.beamWidth / 2.0

        // Central spot
        rotationVectors.add(Pair(createRotationVector(horzAngle, vertAngle, rotationAxis, facingDirection), 0.0))

        // Vertical spots
        rotationVectors.add(Pair(createRotationVector(horzAngle, vertAngle + offsetAngle, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle, vertAngle - offsetAngle, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle, vertAngle + (offsetAngle / 2.0), rotationAxis, facingDirection), offsetAngle / 2.0))
        rotationVectors.add(Pair(createRotationVector(horzAngle, vertAngle - (offsetAngle / 2.0), rotationAxis, facingDirection), offsetAngle / 2.0))

        // Horizontal spots
        val (hAdj1, kAdj1) = calculateAngleAdjustments(vertAngle, offsetAngle, 0)
        val (hAdj2, kAdj2) = calculateAngleAdjustments(vertAngle, offsetAngle / 2.0, 0)

        rotationVectors.add(Pair(createRotationVector(horzAngle + hAdj1, kAdj1, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle - hAdj1, kAdj1, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle + hAdj2, kAdj2, rotationAxis, facingDirection), offsetAngle / 2.0))
        rotationVectors.add(Pair(createRotationVector(horzAngle - hAdj2, kAdj2, rotationAxis, facingDirection), offsetAngle / 2.0))

        // Upper diagonal spots
        val (hAdj3, kAdj3) = calculateAngleAdjustments(vertAngle, offsetAngle, 1)
        val (hAdj4, kAdj4) = calculateAngleAdjustments(vertAngle, offsetAngle / 2.0, 1)

        rotationVectors.add(Pair(createRotationVector(horzAngle + hAdj3, kAdj3, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle - hAdj3, kAdj3, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle + hAdj4, kAdj4, rotationAxis, facingDirection), offsetAngle / 2.0))
        rotationVectors.add(Pair(createRotationVector(horzAngle - hAdj4, kAdj4, rotationAxis, facingDirection), offsetAngle / 2.0))

        // Lower diagonal spots
        val (hAdj5, kAdj5) = calculateAngleAdjustments(vertAngle, offsetAngle, -1)
        val (hAdj6, kAdj6) = calculateAngleAdjustments(vertAngle, offsetAngle / 2.0, -1)

        rotationVectors.add(Pair(createRotationVector(horzAngle + hAdj5, kAdj5, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle - hAdj5, kAdj5, rotationAxis, facingDirection), offsetAngle))
        rotationVectors.add(Pair(createRotationVector(horzAngle + hAdj6, kAdj6, rotationAxis, facingDirection), offsetAngle / 2.0))
        rotationVectors.add(Pair(createRotationVector(horzAngle - hAdj6, kAdj6, rotationAxis, facingDirection), offsetAngle / 2.0))

        for (idx in 0 until rotationVectors.size) {
            val lightVector = element.node!!.coordinate.toVec3()
            val lbCoordinate = Coordinate(lightVector)

            val throwDistance = element.lightRange / cos(toRadians(rotationVectors[idx].second))

            for (jdx in 0 until throwDistance.toInt()) {
                if (!lbCoordinate.blockExist || lbCoordinate.block.isOpaqueCube) break

                lightVector.xCoord += rotationVectors[idx].first.xCoord
                lightVector.yCoord += rotationVectors[idx].first.yCoord
                lightVector.zCoord += rotationVectors[idx].first.zCoord
                lbCoordinate.setPosition(lightVector)

                if (jdx % 4 == 1) LightBlockEntity.addLight(Coordinate(lbCoordinate), lightValue, 5)
            }
        }
    }

    private fun toRadians(angle: Double): Double {
        return angle * (Math.PI / 180.0)
    }

    private fun toDegrees(angle: Double): Double {
        return angle * (180.0 / Math.PI)
    }

    private fun calculateAngleAdjustments(vertAngle: Double, offsetAngle: Double, diagonal: Int): Pair<Double, Double> {
        val k0 = toRadians(vertAngle)
        val o0 = toRadians(offsetAngle)

        var o = o0
        var b = 0.0

        if (diagonal != 0) {
            o = acos(sqrt((cos(o0).pow(2) + 1.0) / 2.0))
            b = atan(sqrt((cos(o).pow(2) / (cos(o).pow(2) - sin(o).pow(2))) - 1.0))

            b *= diagonal.toDouble()
        }

        val a = sqrt(cos(o).pow(2) / (1.0 + tan(k0 + b).pow(2)))

        val hAdj = toRadians(90.0) - atan(sign(cos(k0 + b)) * (a / sin(o)))
        val kAdj = atan(sign(sin(k0 + b)) * sqrt((cos(o).pow(2) - a.pow(2)) / (sin(o).pow(2) + a.pow(2))))

        return Pair(toDegrees(hAdj), toDegrees(kAdj))
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