package mods.eln.sixnode.lampsocket

import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.item.lampitem.LampDescriptor
import mods.eln.lightblock.LightBlockEntity
import mods.eln.misc.Coordinate
import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import mods.eln.sixnode.lampsupply.LampSupplyElement
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs

// TODO: Revisit integration of this file with the rest of the six-node lamp socket code.
class LampSocketProcess(var element: LampSocketElement) : IProcess {

    private var processElapsedTime = 0.0

    var cachedBestChannelHandle: Pair<Double, LampSupplyElement.PowerSupplyChannelHandle>? = null
    var stableLightProbability = 0.0
    var fastLightValue = 0

    private var lampInInventory = false
    private var cableInInventory = false

    override fun process(time: Double) {
        val lampStack = element.inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID)
        val cableStack = element.inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID)

        var activeLampSupplyConnection = false
        var newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE

        if (lampStack != null && cableStack != null) {
            val lampDescriptor = Utils.getItemObject(lampStack) as LampDescriptor

            if (element.poweredByLampSupply) {
                findBestLampSupply(element.sixNode!!.coordinate)

                val bestLampSupply = cachedBestChannelHandle?.second

                if (bestLampSupply != null && bestLampSupply.element.getChannelState(bestLampSupply.id)) {
                    bestLampSupply.element.addToRp(lampDescriptor.lampData.resistance)
                    element.electricalLoad.state = bestLampSupply.element.powerLoad.state
                } else {
                    element.electricalLoad.state = 0.0
                }

                activeLampSupplyConnection = (bestLampSupply != null)
            }

            val lampData = lampDescriptor.lampData
            val lampVoltage = abs(element.lampResistor.voltage)

            if (lampVoltage > (lampData.nominalU * lampData.technology.minimalUFactor)) {
                val num: Double = lampVoltage - (lampData.nominalU * lampData.technology.minimalUFactor)
                val den: Double = lampData.nominalU - (lampData.nominalU * lampData.technology.minimalUFactor)

                newLightValue = ((num / den) * lampData.nominalLightValue).toInt()

                // This code makes the fluorescent lights blink, and the other lights are just "stable"
                if (lampData.technology.lampType == "fluorescent") {
                    if (newLightValue >= LampSocketRender.MIN_LIGHT_ON_VALUE && stableLightProbability <= 1.0) {
                        stableLightProbability += (lampVoltage / lampData.nominalU) * (time / lampData.technology.timeUntilStableInSeconds)
                        if (stableLightProbability < Math.random()) newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                        if (stableLightProbability > 1.0) stableLightProbability = 1.0
                    } else {
                        newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                        stableLightProbability = 0.0
                    }
                } else {
                    stableLightProbability = 1.0
                }

                if (newLightValue < BoilerplateLampData.MIN_LIGHT_VALUE) newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                else if (newLightValue > BoilerplateLampData.MAX_LIGHT_VALUE) newLightValue = BoilerplateLampData.MAX_LIGHT_VALUE
            } else {
                stableLightProbability = 0.0
            }

            if (element.coordinate!!.blockExist) {
                updateNearbyBlocks(lampData.technology.cropGrowthRateFactor, lampData.nominalLightValue, newLightValue, time)
            }

            /* Only decrease the life of a bulb once a second. This reduces the update rate at which the NBT is changed
             * to once per second from once per tick, reducing the probability of an NBT mismatch bug occurring when
             * shift-clicking. When the bug is eventually fixed, the processElapsedTime variable and supporting code can
             * be deleted. Also update the decreaseLampLife function definition according to the note there.
             */
            if (processElapsedTime in -0.001..0.001) {
                val lampLife = lampDescriptor.decreaseLampLife(lampStack, lampVoltage)

                if (lampLife <= 0.0) {
                    newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                    element.inventory.setInventorySlotContents(LampSocketContainer.LAMP_SLOT_ID, null)
                    element.inventory.markDirty()
                }
            }
        } else {
            stableLightProbability = 0.0
        }

        if (newLightValue > BoilerplateLampData.MIN_LIGHT_VALUE) placeSpot(newLightValue)

        updateFastLight(newLightValue)

        // This logic ensures that needPublish() is not called multiple times
        if (!updateInventory(lampStack, cableStack)) publishChanges(activeLampSupplyConnection, newLightValue)

        processElapsedTime += time
        if (processElapsedTime >= 1.0) processElapsedTime = 0.0
    }

    private fun findBestLampSupply(coordinate: Coordinate, forceUpdate: Boolean = false) {
        val channelMap = LampSupplyElement.channelMap[element.lampSupplyChannel]

        if (channelMap != null) {
            if (channelMap.contains(cachedBestChannelHandle?.second) && !forceUpdate) return
            else {
                channelMap.filterNotNull()
                cachedBestChannelHandle = channelMap
                    .map { Pair(it.element.sixNode!!.coordinate.trueDistanceTo(coordinate), it) }
                    .filter { it.first < it.second.element.range }
                    .minByOrNull { it.first }
            }
        } else cachedBestChannelHandle = null
    }

    private fun updateNearbyBlocks(growRate: Double, nominalLight: Int, actualLight: Int, deltaT: Double) {
        val randTarget = growRate * deltaT * (actualLight.toDouble() / nominalLight.toDouble())

        if (randTarget > Math.random()) {
            val rotationVector = Vec3.createVectorHelper(1.0, 0.0, 0.0)
            rotationVector.rotateAroundZ((element.projectionRotationAngle * (Math.PI / 180.0)).toFloat())
            rotationVector.rotateAroundY(((Math.random() - 0.5) * Math.PI / 2.0).toFloat())
            rotationVector.rotateAroundZ(((Math.random() - 0.5) * Math.PI / 2.0).toFloat())
            element.front.rotateOnXnLeft(rotationVector)
            element.side.rotateFromXN(rotationVector)

            val lbCoordinate = raytrace(rotationVector, actualLight)
            lbCoordinate.block.updateTick(lbCoordinate.world(), lbCoordinate.x, lbCoordinate.y, lbCoordinate.z, lbCoordinate.world().rand)
        }
    }

    private fun placeSpot(lightValue: Int) {
        val rotationVector = Vec3.createVectorHelper(1.0, 0.0, 0.0)
        rotationVector.rotateAroundZ((element.projectionRotationAngle * (Math.PI / 180.0)).toFloat())
        element.front.rotateOnXnLeft(rotationVector)
        element.side.rotateFromXN(rotationVector)

        val lbCoordinate = raytrace(rotationVector, 0)
        LightBlockEntity.addLight(lbCoordinate, lightValue, 5)
    }

    private fun raytrace(rotationVector: Vec3, vectorLengthModifier: Int): Coordinate {
        val lightVector = element.sixNode!!.coordinate.toVec3()
        val lbCoordinate = Coordinate(lightVector, element.sixNode!!.coordinate.dimension)

        for (idx in 0 until element.descriptor.range + vectorLengthModifier) {
            lightVector.xCoord += rotationVector.xCoord
            lightVector.yCoord += rotationVector.yCoord
            lightVector.zCoord += rotationVector.zCoord
            lbCoordinate.setPosition(lightVector)

            if (!lbCoordinate.blockExist || lbCoordinate.block.isOpaqueCube) {
                lightVector.xCoord -= rotationVector.xCoord
                lightVector.yCoord -= rotationVector.yCoord
                lightVector.zCoord -= rotationVector.zCoord
                lbCoordinate.setPosition(lightVector)
                break
            }
        }

        return lbCoordinate
    }

    /**
     * Sync "fast" light changes (fluorescent flicker)
     */
    private fun updateFastLight(newLightValue: Int) {
        if (fastLightValue != newLightValue) {
            fastLightValue = newLightValue

            val bos = ByteArrayOutputStream(64)
            val packet = DataOutputStream(bos)

            element.preparePacketForClient(packet)

            try {
                packet.writeInt(newLightValue)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            element.sendPacketToAllClient(bos)
        }
    }

    /**
     * Manually update the server-side inventory every time an item is inserted/removed from the GUI.
     * This should be happening automatically, but it is not.
     */
    private fun updateInventory(lampStack: ItemStack?, cableStack: ItemStack?): Boolean {
        var updateInventory = false

        if (lampInInventory != (lampStack != null)) {
            lampInInventory = (lampStack != null)
            updateInventory = true
        }

        if (cableInInventory != (cableStack != null)) {
            cableInInventory = (cableStack != null)
            updateInventory = true
        }

        // Prevent duplicate function calls
        return if (updateInventory) { element.inventoryChange(element.inventory); true } else false
    }

    private fun publishChanges(activeLampSupplyConnection: Boolean, newLightValue: Int) {
        var publishChanges = false

        if (element.activeLampSupplyConnection != activeLampSupplyConnection) {
            element.activeLampSupplyConnection = activeLampSupplyConnection
            publishChanges = true
        }

        if (element.sixNode!!.lightValue != newLightValue) {
            element.sixNode!!.lightValue = newLightValue
            publishChanges = true
        }

        // Prevent duplicate function calls
        if (publishChanges) element.needPublish()
    }

}