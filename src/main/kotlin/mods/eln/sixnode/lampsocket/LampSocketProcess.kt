package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamage
import mods.eln.item.LampDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.sixnode.lampsupply.LampSupplyElement.PowerSupplyChannelHandle
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs

// TODO: This file is a disaster. Major clean-up needed.
class LampSocketProcess(var lamp: LampSocketElement) : IProcess, INBTTReady /*,LightBlockObserver*/ {
    @JvmField
    var light = 0 // 0..15
    @JvmField
    var alphaZ = 0.0
    var stableProb = 0.0
    var lampStackLast: ItemStack? = null
    var boot = true
    var lbCoord: Coordinate

    var bestChannelHandle: Pair<Double, PowerSupplyChannelHandle>? = null

    private fun findBestSupply(here: Coordinate, forceUpdate: Boolean = false): Pair<Double, PowerSupplyChannelHandle>? {
        val chanMap = LampSupplyElement.channelMap[lamp.lampSupplyChannel] ?: return null
        val bestChanHand = bestChannelHandle
        // Here's our cached value. We just check if it's null and if it's still a thing.
        if (bestChanHand != null && !forceUpdate && chanMap.contains(bestChanHand.second)) {
            return bestChanHand // we good!
        }
        val list = LampSupplyElement.channelMap[lamp.lampSupplyChannel]?.filterNotNull() ?: return null
        val chanHand = list
            .map { Pair(it.element.sixNode?.coordinate?.trueDistanceTo(here)?: Double.MAX_VALUE, it) }
            .filter { it.first < it.second.element.range }
            .minByOrNull { it.first }
        bestChannelHandle = chanHand
        return bestChannelHandle
    }

    private fun updateNearbyBlocks(growRate: Double, nominalLight: Int, actualLight: Int, deltaT: Double) {
        val randTarget = growRate * deltaT * (actualLight.toDouble() / nominalLight.toDouble())
        if (randTarget > Math.random()) {
            var exit = false
            val vv = Vec3.createVectorHelper(1.0, 0.0, 0.0)
            val vp = Vec3.createVectorHelper(lamp.sixNode!!.coordinate.x + 0.5, lamp.sixNode!!.coordinate.y + 0.5, lamp.sixNode!!.coordinate.z + 0.5)
            vv.rotateAroundZ((alphaZ * Math.PI / 180.0).toFloat())
            vv.rotateAroundY(((Math.random() - 0.5) * 2 * Math.PI / 4).toFloat())
            vv.rotateAroundZ(((Math.random() - 0.5) * 2 * Math.PI / 4).toFloat())
            lamp.front.rotateOnXnLeft(vv)
            lamp.side.rotateFromXN(vv)
            val c = Coordinate(lamp.sixNode!!.coordinate)
            for (idx in 0 until lamp.descriptor.range + actualLight) { // newCoord.move(lamp.side.getInverse());
                vp.xCoord += vv.xCoord
                vp.yCoord += vv.yCoord
                vp.zCoord += vv.zCoord
                c.setPosition(vp)
                if (!c.blockExist) {
                    exit = true
                    break
                }
                if (isOpaque(c)) {
                    vp.xCoord -= vv.xCoord
                    vp.yCoord -= vv.yCoord
                    vp.zCoord -= vv.zCoord
                    c.setPosition(vp)
                    break
                }
            }
            if (!exit && c.block !== Blocks.air)
                c.block.updateTick(c.world(), c.x, c.y, c.z, c.world().rand)
        }
    }

    /**
     * Manually call inventory change when all items are added/removed from the GUI because it is not being called normally
     * TODO: This is (hopefully) temporary
     */
    private fun updateInventory(lampStack: ItemStack?, cableStack: ItemStack?) {
        var inventoryChanged = false

        if (lamp.lampInInventory != (lampStack != null)) {
            lamp.lampInInventory = lampStack != null
            inventoryChanged = true
        }

        if (lamp.cableInInventory != (cableStack != null)) {
            lamp.cableInInventory = cableStack != null
            inventoryChanged = true
        }

        if (inventoryChanged) lamp.inventoryChange(lamp.inventory)
    }

    override fun process(time: Double) {
        var newLight: Int
        val lampStack = lamp.inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID)
        val cableStack = lamp.inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID)
        updateInventory(lampStack, cableStack)
        // LampDescriptor? is *important* here. Otherwise, NPE.
        val lampDescriptor = (lampStack?.item as? GenericItemUsingDamage<*>)?.getDescriptor(lampStack) as? LampDescriptor
        if (cableStack == null || lampStack == null) {
            /*
            Cable slot and lamp slot are empty. This means no light, and disconnect from lamp supply.
             */
            stableProb = 0.0
            lamp.activeLampSupplyConnection = false
            lamp.needPublish()
        } else {
            /*
            Cable slot and lamp slot have stuff. This means there can be light.
             */
            if (lamp.poweredByLampSupply) {
                // Powered by a lamp supply.
                val lampSupplyList = findBestSupply(lamp.sixNode!!.coordinate)
                val bestLampSupply = lampSupplyList?.second
                if (bestLampSupply != null && lampDescriptor != null && bestLampSupply.element.getChannelState(bestLampSupply.id)) {
                    bestLampSupply.element.addToRp(lampDescriptor.lampData.resistance)
                    lamp.electricalLoad.state = bestLampSupply.element.powerLoad.state
                } else {
                    lamp.electricalLoad.state = 0.0
                }

                lamp.activeLampSupplyConnection = bestLampSupply != null
                lamp.needPublish()
            }
            else
            {
                // Not powered by a lamp supply.
                lamp.activeLampSupplyConnection = false
                lamp.needPublish()
            }
        }

        lamp.computeInventory()
        if (lampStack != lampStackLast) {
            stableProb = 0.0
        }

        if (stableProb < 0) stableProb = 0.0
        var lightDouble: Double

        if (lampDescriptor != null) {
            val lampData = lampDescriptor.lampData

            // This code makes the ECO lights blink, and the other lights are just "stable"
            if (lampData.technology.lampType == "fluorescent") {
                val voltage = abs(lamp.lampResistor.voltage)
                if (voltage < (lampData.nominalU * lampData.technology.minimalUFactor)) {
                    stableProb = 0.0
                    lightDouble = 0.0
                } else {
                    val powerFactor = voltage / lampData.nominalU
                    stableProb += voltage / (lampData.nominalU * lampData.technology.stableUFactor) * time / lampData.technology.timeUntilStableInSeconds * lampData.technology.stableUFactor
                    if (stableProb > voltage / (lampData.nominalU * lampData.technology.stableUFactor)) stableProb = voltage / (lampData.nominalU * lampData.technology.stableUFactor)
                    lightDouble = if (Math.random() > stableProb) 0.0 else lampData.technology.nominalLightValue * powerFactor
                }
            }
            else {
                if (lamp.lampResistor.voltage < (lampData.nominalU * lampData.technology.minimalUFactor)) {
                    lightDouble = 0.0
                } else {
                    val num: Double = abs(lamp.lampResistor.voltage) - (lampData.nominalU * lampData.technology.minimalUFactor)
                    val den: Double = lampData.nominalU - (lampData.nominalU * lampData.technology.minimalUFactor)

                    lightDouble = (num / den) * lampData.technology.nominalLightValue
                }
            }
        } else {
            // there is no light bulb. Light = 0
            lightDouble = 0.0
        }

        newLight = lightDouble.toInt()
        //light.coerceIn(0, 15) // IT F'ING LIES
        if (newLight > 15) newLight = 15
        if (newLight < 0) newLight = 0

        if (lampDescriptor != null) {
            // Only decrease the life of a bulb once a second. This reduces the update rate at which the NBT is changed
            // to once per second from once per tick, reducing the probability of an NBT mismatch bug occurring when
            // shift-clicking. When the bug is eventually fixed, the if() check is no longer necessary and the
            // processElapsedTime variable and supporting code can be deleted.
            if (lamp.processElapsedTime == 0.0) {
                val lampLife = lampDescriptor.decreaseLampLife(lampStack, lamp.lampResistor.voltage)

                if (lampLife <= 0.0) {
                    lamp.inventory.setInventorySlotContents(LampSocketContainer.LAMP_SLOT_ID, null)
                    lamp.inventory.markDirty()
                    newLight = 0
                }
            }
        } else {
            newLight = 0
        }

        if (lamp.coordinate!!.blockExist && lampDescriptor != null) {
            updateNearbyBlocks(lampDescriptor.lampData.technology.cropGrowthRateFactor, lampDescriptor.lampData.technology.nominalLightValue, newLight, time)
        }

        boot = false
        lampStackLast = lampStack
        placeSpot(newLight)
        if (light != newLight)
            lamp.needPublish()

        lamp.processElapsedTime += time
        if (lamp.processElapsedTime >= 1.0) lamp.processElapsedTime = 0.0
    }

    // ElectricalConnectionOneWay connection = null;
    fun rotateAroundZ(v: Vec3, par1: Float) {
        val f1 = MathHelper.cos(par1)
        val f2 = MathHelper.sin(par1)
        val d0 = v.xCoord * f1.toDouble() + v.yCoord * f2.toDouble()
        val d1 = v.yCoord * f1.toDouble() - v.xCoord * f2.toDouble()
        val d2 = v.zCoord
        v.xCoord = d0
        v.yCoord = d1
        v.zCoord = d2
    }

    fun placeSpot(newLight: Int) {
        var exit = false
        if (!lbCoord.blockExist) return
        val vv = Vec3.createVectorHelper(1.0, 0.0, 0.0)
        val vp = Utils.getVec05(lamp.sixNode!!.coordinate)
        rotateAroundZ(vv, (alphaZ * Math.PI / 180.0).toFloat())
        lamp.front.rotateOnXnLeft(vv)
        lamp.side.rotateFromXN(vv)
        val newCoord = Coordinate(lamp.sixNode!!.coordinate)
        for (idx in 0 until lamp.descriptor.range) { // newCoord.move(lamp.side.getInverse());
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
            var count = 0
            while (newCoord != lamp.sixNode!!.coordinate) {
                val block = newCoord.block
                if (block === Blocks.air || block === Eln.lightBlock) {
                    count++
                    if (count == 2) break
                }
                vp.xCoord -= vv.xCoord
                vp.yCoord -= vv.yCoord
                vp.zCoord -= vv.zCoord
                newCoord.setPosition(vp)
            }
        }
        if (!exit) setLightAt(newCoord, newLight)
    }

    private fun isOpaque(coord: Coordinate): Boolean {
        val block = coord.block
        return block !== Blocks.air && (block.isOpaqueCube && block !== Blocks.farmland)
    }

    fun setLightAt(coord: Coordinate, value: Int) {
        val oldLbCoord = lbCoord
        lbCoord = Coordinate(coord)
        val oldLight = light
        val same = coord == oldLbCoord
        light = value
        if (!same && oldLbCoord == lamp.sixNode!!.coordinate) {
            lamp.sixNode!!.recalculateLightValue()
        }
        if (lbCoord == lamp.sixNode!!.coordinate) {
            if (light != oldLight || !same) lamp.sixNode!!.recalculateLightValue()
        } else {
            LightBlockEntity.addLight(lbCoord, light, 5)
        }
        if (light != oldLight) {
            val bos = ByteArrayOutputStream(64)
            val packet = DataOutputStream(bos)
            lamp.preparePacketForClient(packet)
            try {
                packet.writeByte(light)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            lamp.sendPacketToAllClient(bos)
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        stableProb = nbt.getDouble(str + "LSP" + "stableProb")
        lbCoord.readFromNBT(nbt, str + "lbCoordInst")
        alphaZ = nbt.getFloat(str + "alphaZ").toDouble()
        light = nbt.getInteger(str + "light")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "LSP" + "stableProb", stableProb)
        lbCoord.writeToNBT(nbt, str + "lbCoordInst")
        nbt.setFloat(str + "alphaZ", alphaZ.toFloat())
        nbt.setInteger(str + "light", light)
    }

    val blockLight: Int
        get() = if (lbCoord == lamp.sixNode!!.coordinate) {
            light
        } else {
            0
        }

    init {
        lbCoord = Coordinate(lamp.sixNode!!.coordinate)
    }
}
