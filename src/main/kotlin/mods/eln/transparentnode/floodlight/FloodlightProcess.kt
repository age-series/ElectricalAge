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
import kotlin.math.*

class FloodlightProcess(var element: FloodlightElement) : IProcess {

    companion object {
        // Number of light rays to be produced in each direction. Must be a mathematical integer!
        const val MAX_LIGHT_BEAM_COUNT = 4.0
        // How often to create a light block within a given beam of light
        const val LIGHT_BLOCK_FREQUENCY = 2
    }

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

        // Only run raytracing when the floodlight is actually on.
        if (newLightValue != 0) placeSpots(newLightValue)
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

    /**
     * WARNING! DO NOT EDIT THIS FUNCTION!
     * The math is very complex and any small change will likely break EVERYTHING.
     * See the math docs (to be uploaded at some point) for a guide to the derivation process.
     * Also see https://www.desmos.com/3d/xqi6ov3fpn for a visualization of the raytracing results.
     */
    private fun placeSpots(lightValue: Int) {
        val rotationVectors = mutableListOf<Pair<Vec3, Double>>()
        val fractionTable = mutableListOf<Double>()

        val rotationAxis = element.rotationAxis
        val facingDirection = element.blockFacing

        val horzAngle = element.swivelAngle
        val vertAngle = element.headAngle
        val offsetAngle = element.beamWidth / 2.0

        // Number of light rays to be produced in each cardinal direction, also extrapolated into the empty spaces between them.
        val beamCount = ceil((offsetAngle * 2.0) * (MAX_LIGHT_BEAM_COUNT / FloodlightGui.MAX_BEAM_WIDTH)).toInt()

        if (beamCount != 0) {
            for (idx in beamCount downTo -beamCount) {
                fractionTable.add(idx.toDouble() / beamCount.toDouble())
            }
        }
        else fractionTable.add(0.0)

        for (idx in fractionTable.indices) {
            val offsetAngleFraction = offsetAngle * fractionTable[idx]

            // Unit vectors for the central and vertical spots
            rotationVectors.add(Pair(createRotationVector(horzAngle, vertAngle + offsetAngleFraction, rotationAxis, facingDirection), offsetAngleFraction))

            if (fractionTable[idx] > 0.0) {
                for (jdx in fractionTable.indices) {
                    if (abs(fractionTable[jdx]) != 1.0) {
                        val diagonalAngle = 90.0 * fractionTable[jdx]

                        val (hAdj, kAdj) = calculateAngleAdjustments(vertAngle, offsetAngleFraction, diagonalAngle)

                        // Unit vectors for the horizontal and diagonal spots (mirrored across vertical axis)
                        rotationVectors.add(Pair(createRotationVector(horzAngle + hAdj, kAdj, rotationAxis, facingDirection), offsetAngleFraction))
                        rotationVectors.add(Pair(createRotationVector(horzAngle - hAdj, kAdj, rotationAxis, facingDirection), offsetAngleFraction))
                    }
                }
            }
        }

        for (idx in 0 until rotationVectors.size) {
            val lightVector = element.node!!.coordinate.toVec3()
            val lbCoordinate = Coordinate(lightVector)

            // This forces the light cone to be "flat" on the end, instead of curved.
            val throwDistance = element.lightRange / cos(toRadians(rotationVectors[idx].second))

            for (jdx in 0 until throwDistance.toInt()) {
                lightVector.xCoord += rotationVectors[idx].first.xCoord
                lightVector.yCoord += rotationVectors[idx].first.yCoord
                lightVector.zCoord += rotationVectors[idx].first.zCoord
                lbCoordinate.setPosition(lightVector)

                if (!lbCoordinate.blockExist || lbCoordinate.block.isOpaqueCube) break

                // Place light blocks every few blocks along the path of a beam, as well as always at the beam's endpoint.
                if (jdx % LIGHT_BLOCK_FREQUENCY == (LIGHT_BLOCK_FREQUENCY - 1) || throwDistance.toInt() - jdx == 1) {
                    LightBlockEntity.addLight(Coordinate(lbCoordinate), lightValue, 5)
                }
            }
        }
    }

    private fun toRadians(angle: Double): Double {
        return angle * (Math.PI / 180.0)
    }

    private fun toDegrees(angle: Double): Double {
        return angle * (180.0 / Math.PI)
    }

    /**
     * DO NOT TOUCH THIS FUNCTION!!!!!
     * Any edits to the math require re-deriving nearly every equation from scratch!
     * See a visual of the equations/results at https://www.desmos.com/3d/xqi6ov3fpn.
     * Trust me, the math is right! Any bugs that may arise result from improper usage.
    */
    private fun calculateAngleAdjustments(vertAngle: Double, offsetAngle: Double, diagonalAngle: Double): Pair<Double, Double> {
        val k0 = toRadians(vertAngle)
        val o0 = toRadians(offsetAngle)
        val d0 = toRadians(diagonalAngle)

        val d = tan(d0)
        val o = acos(sqrt((cos(o0).pow(2) + d.pow(2)) / (1.0 + d.pow(2))))
        val b = atan(sqrt((cos(o).pow(2) / (cos(o).pow(2) - (d.pow(2) * sin(o).pow(2)))) - 1.0)) * sign(d0)
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
                    XN -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
                    XP -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
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
                    XN -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
                    XP -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
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
                    YN -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
                    YP -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
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
                    YN -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
                    YP -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
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
                    ZN -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
                    ZP -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
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
                    ZN -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
                    ZP -> TODO("Unused - impossible facing direction. If you get this message there's a bug in the code.")
                }
            }
        }

        return newV
    }

}