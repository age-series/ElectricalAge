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
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max

class FloodlightProcess(var element: FloodlightElement) : IProcess {

    override fun process(time: Double) {
        if (element.motorized) {
            element.swivelAngle = (element.swivelControl.normalized).toFloat() * FloodlightGui.MAX_HORIZONTAL_ANGLE
            element.headAngle = (element.headControl.normalized).toFloat() * FloodlightGui.MAX_VERTICAL_ANGLE
            element.shutterAngle = (element.shutterControl.normalized.toFloat()) * FloodlightGui.MAX_SHUTTER_ANGLE
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

                lampLightValues.add(((num / den) * lampDescriptor.nominalLight * LampDescriptor.MC_MAX_LIGHT_VALUE).toInt())

                if (lampLightValues[idx] < LampDescriptor.MC_MIN_LIGHT_VALUE) lampLightValues[idx] = LampDescriptor.MC_MIN_LIGHT_VALUE
                else if (lampLightValues[idx] > LampDescriptor.MC_MAX_LIGHT_VALUE) lampLightValues[idx] = LampDescriptor.MC_MAX_LIGHT_VALUE

                lampLightRanges.add((lampDescriptor.nominalP * LampDescriptor.HALOGEN_RANGE_FACTOR).toInt())

                val bulbCanAge = !Eln.halogenLampInfiniteLife && SaveConfig.instance!!.electricalLampAging

                if (bulbCanAge) {
                    // if (!FloodlightContainer.lockLampAging) {
                        // FloodlightContainer.lockStackTransfer = true

                        val currentLife = lampDescriptor.ageLamp(lampStacks[idx]!!, lampVoltage, time)

                        if (currentLife <= 0.0) {
                            element.inventory.setInventorySlotContents(idx, null)
                            element.inventoryChange(element.inventory)
                        }
                    // }

                    // FloodlightContainer.lockStackTransfer = false
                }
            }
            else {
                lampLightValues.add(LampDescriptor.MC_MIN_LIGHT_VALUE)
                lampLightRanges.add((0.0 * LampDescriptor.HALOGEN_RANGE_FACTOR).toInt())
            }
        }

        val newLightValue = max(lampLightValues[FloodlightContainer.LAMP_SLOT_1_ID], lampLightValues[FloodlightContainer.LAMP_SLOT_2_ID])

        element.powered = newLightValue > LampDescriptor.MC_MIN_LIGHT_VALUE
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
        val offsetAngle = (element.shutterAngle / 2)

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

            for (jdx in 0 until element.lightRange) {
                if (lbCoords[idx].block.isOpaqueCube) break

                lightVectors[idx].xCoord += rotationVectors[idx].xCoord
                lightVectors[idx].yCoord += rotationVectors[idx].yCoord
                lightVectors[idx].zCoord += rotationVectors[idx].zCoord
                lbCoords[idx].setPosition(lightVectors[idx])

                if (!lbCoords[idx].blockExist) return

                if (jdx % 2 == 1) LightBlockEntity.addLight(Coordinate(lbCoords[idx]), lightValue, 5)
            }
        }
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