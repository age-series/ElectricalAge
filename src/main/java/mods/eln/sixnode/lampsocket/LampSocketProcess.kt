package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamage
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.item.LampDescriptor
import mods.eln.misc.Coordonate
import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import mods.eln.server.SaveConfig
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

class LampSocketProcess(var lamp: LampSocketElement) : IProcess, INBTTReady /*,LightBlockObserver*/ {
    @JvmField
    var light = 0 // 0..15
    @JvmField
    var alphaZ = 0.0
    var stableProb = 0.0
    var lampStackLast: ItemStack? = null
    var boot = true
    var lbCoord: Coordonate

    var bestChannelHandle: Pair<Double, PowerSupplyChannelHandle>? = null

    private fun findBestSupply(here: Coordonate, forceUpdate: Boolean = false): Pair<Double, PowerSupplyChannelHandle>? {
        val chanMap = LampSupplyElement.channelMap[lamp.channel] ?: return null
        val bestChanHand = bestChannelHandle
        // Here's our cached value. We just check if it's null and if it's still a thing.
        if (!(bestChanHand == null || forceUpdate || !chanMap.contains(bestChanHand.second))) {
            return bestChanHand // we good!
        }
        val list = LampSupplyElement.channelMap[lamp.channel]?.filterNotNull() ?: return null
        val map = list.map { Pair(it.element.sixNode.coordonate.trueDistanceTo(here), it) }
        val sortedBy = map.sortedBy { it.first }
        val chanHand = sortedBy.first()
        bestChannelHandle = chanHand
        return bestChannelHandle
    }

    private fun updateNearbyBlocks(growRate: Double, nominalLight: Double, deltaT: Double) {
        val randTarget = 1.0 / growRate * deltaT * (1.0 * light / nominalLight / 15.0)
        if (randTarget > Math.random()) {
            var exit = false
            val vv = Vec3.createVectorHelper(1.0, 0.0, 0.0)
            val vp = Vec3.createVectorHelper(lamp.sixNode.coordonate.x + 0.5, lamp.sixNode.coordonate.y + 0.5, lamp.sixNode.coordonate.z + 0.5)
            vv.rotateAroundZ((alphaZ * Math.PI / 180.0).toFloat())
            vv.rotateAroundY(((Math.random() - 0.5) * 2 * Math.PI / 4).toFloat())
            vv.rotateAroundZ(((Math.random() - 0.5) * 2 * Math.PI / 4).toFloat())
            lamp.front.rotateOnXnLeft(vv)
            lamp.side.rotateFromXN(vv)
            val c = Coordonate(lamp.sixNode.coordonate)
            for (idx in 0 until lamp.socketDescriptor.range + light) { // newCoord.move(lamp.side.getInverse());
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

    override fun process(time: Double) {
        val lampStack = lamp.inventory.getStackInSlot(0)
        if (!lamp.poweredByLampSupply || lamp.inventory.getStackInSlot(LampSocketContainer.cableSlotId) == null) {
            lamp.setIsConnectedToLampSupply(false)
        } else {
            val lampSupplyList = findBestSupply(lamp.sixNode.coordonate)
            val best = lampSupplyList?.second
            if (best != null && best.element.getChannelState(best.id)) {
                if (lampStack != null) {
                    val lampDescriptor = (lampStack.item as GenericItemUsingDamage<GenericItemUsingDamageDescriptor>).getDescriptor(lampStack) as LampDescriptor
                    best.element.addToRp(lampDescriptor.r)
                }
                lamp.positiveLoad.state = best.element.powerLoad.state
            } else {
                lamp.positiveLoad.state = 0.0
            }
            lamp.setIsConnectedToLampSupply(best != null)
        }
        lamp.computeElectricalLoad()
        if (!boot && (lampStack != lampStackLast || lampStack == null)) {
            stableProb = 0.0
        }
        if (lampStack != null) {
            val lampDescriptor = (lampStack.item as? GenericItemUsingDamage<GenericItemUsingDamageDescriptor>)?.getDescriptor(lampStack) as LampDescriptor
            if (stableProb < 0) stableProb = 0.0
            var lightDouble = 0.0

            // This code makes the ECO lights blink, and the other lights are just "stable"
            when (lampDescriptor.type) {
                LampDescriptor.Type.INCANDESCENT, LampDescriptor.Type.LED -> {
                    if (lamp.lampResistor.u < lampDescriptor.minimalU) {
                        lightDouble = 0.0
                    } else {
                        lightDouble = lampDescriptor.nominalLight * ((Math.abs(lamp.lampResistor.u) - lampDescriptor.minimalU) / (lampDescriptor.nominalU - lampDescriptor.minimalU))
                    }
                    //println(lampDescriptor.nominalLight)
                    //println(lightDouble)
                    lightDouble *= 15
                    //println(lightDouble)
                }
                LampDescriptor.Type.ECO -> {
                    val U = Math.abs(lamp.lampResistor.u)
                    if (U < lampDescriptor.minimalU) {
                        stableProb = 0.0
                        lightDouble = 0.0
                    } else {
                        val powerFactor = U / lampDescriptor.nominalU
                        stableProb += U / lampDescriptor.stableU * time / lampDescriptor.stableTime * lampDescriptor.stableUNormalised
                        if (stableProb > U / lampDescriptor.stableU) stableProb = U / lampDescriptor.stableU
                        if (Math.random() > stableProb) {
                            lightDouble = 0.0
                        } else {
                            lightDouble = lampDescriptor.nominalLight * powerFactor
                            lightDouble *= 16
                        }
                    }
                }
            }

            light = lightDouble.toInt()
            //light.coerceIn(0, 15) // IT F'ING LIES
            if (light > 15) light = 15
            if (light < 0) light = 0
            //println(light)

            fun lampAgeFactor(voltage: Double): Double {
                return 0.000008 * Math.pow(voltage, 3.0) - 0.003225 * Math.pow(voltage, 2.0) + 0.33 * voltage
            }

            val bulbCanAge = !(lampDescriptor.type == LampDescriptor.Type.LED && Eln.ledLampInfiniteLife) && SaveConfig.instance.electricalLampAging

            if (bulbCanAge) {
                val ageFactor = lampAgeFactor(lamp.lampResistor.u)
                // nominal life is in hours. We want lifeLost to be the duration of the number of ticks.
                // there are 72,000 ticks per hour.
                val lifeLost = (lampDescriptor.nominalLife / 72000.0) * ageFactor * (time * 20.0)
                println("Life Lost: $lifeLost")
                lampDescriptor.setLifeInTag(lampStack, lampDescriptor.getLifeInTag(lampStack) - lifeLost)
            }
            if (lampDescriptor.getLifeInTag(lampStack) <= 0.0) {
                lamp.inventory.setInventorySlotContents(0, null)
                light = 0
            }

            if (lamp.coordonate.blockExist && lampDescriptor.vegetableGrowRate != 0.0) {
                updateNearbyBlocks(lampDescriptor.vegetableGrowRate, lampDescriptor.nominalLight, time)
            }

            boot = false
        }else {
            light = 0
        }
        lampStackLast = lampStack
        placeSpot(light)
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
        val vp = Utils.getVec05(lamp.sixNode.coordonate)
        rotateAroundZ(vv, (alphaZ * Math.PI / 180.0).toFloat())
        lamp.front.rotateOnXnLeft(vv)
        lamp.side.rotateFromXN(vv)
        val newCoord = Coordonate(lamp.sixNode.coordonate)
        for (idx in 0 until lamp.socketDescriptor.range) { // newCoord.move(lamp.side.getInverse());
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
            while (newCoord != lamp.sixNode.coordonate) {
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

    private fun isOpaque(coord: Coordonate): Boolean {
        val block = coord.block
        return !(block === Blocks.air) && (block.isOpaqueCube && !(block === Blocks.farmland))
    }

    fun setLightAt(coord: Coordonate, value: Int) {
        val oldLbCoord = lbCoord
        lbCoord = Coordonate(coord)
        val oldLight = light
        val same = coord == oldLbCoord
        light = value
        if (!same && oldLbCoord == lamp.sixNode.coordonate) {
            lamp.sixNode.recalculateLightValue()
        }
        if (lbCoord == lamp.sixNode.coordonate) {
            if (light != oldLight || !same) lamp.sixNode.recalculateLightValue()
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
        get() = if (lbCoord == lamp.sixNode.coordonate) {
            light
        } else {
            0
        }

    init {
        lbCoord = Coordonate(lamp.sixNode.coordonate)
    }
}
