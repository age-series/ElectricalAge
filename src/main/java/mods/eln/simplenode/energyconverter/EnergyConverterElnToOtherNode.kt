package mods.eln.simplenode.energyconverter

import mods.eln.Eln
import mods.eln.Other
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.simple.SimpleNode
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtResistor
import net.minecraft.client.renderer.texture.ITickable
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.max

class EnergyConverterElnToOtherNode : SimpleNode() {
    var descriptor: EnergyConverterElnToOtherDescriptor? = null
    var load = NbtElectricalLoad("load")
    var powerInResistor = NbtResistor("powerInResistor", load, null)
    var electricalProcess = ElectricalProcess()
    var energyBuffer = 0.0
    var energyBufferMax = 0.0
    var inStdVoltage = 0.0
    var inPowerMax = 0.0
    var selectedPower = 0.0
    var ic2tier = 1

    init {
        powerInResistor.r = MnaConst.highImpedance
    }

    override fun setDescriptorKey(key: String) {
        super.setDescriptorKey(key)
        descriptor = getDescriptor() as EnergyConverterElnToOtherDescriptor
    }

    override fun getSideConnectionMask(directionA: Direction, lrduA: LRDU): Int {
        return maskElectricalPower
    }

    override fun getThermalLoad(directionA: Direction, lrduA: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getElectricalLoad(directionB: Direction, lrduB: LRDU, mask: Int): ElectricalLoad {
        return load
    }

    override fun initialize() {
        electricalLoadList.add(load)
        electricalComponentList.add(powerInResistor)
        electricalProcessList.add(electricalProcess)
        Eln.applySmallRs(load)
        load.setAsPrivate()
        descriptor!!.applyTo(this)
        connect()
    }

    inner class ElectricalProcess : IProcess {
        var timeout = 0.0
        override fun process(time: Double) {
            var power = powerInResistor.p
            if (!power.isFinite()) power = 0.0
            energyBuffer += power * time
            timeout -= time
            if (timeout < 0) {
                timeout = 0.05
                val energyMiss = energyBufferMax - energyBuffer
                if (energyMiss <= 0) {
                    powerInResistor.highImpedance()
                } else {
                    val inP = selectedPower
                    if (inP <= 0.0) {
                        powerInResistor.r = MnaConst.highImpedance
                    } else {
                        powerInResistor.r = max(Eln.getSmallRs(), load.u * load.u / inP)
                    }
                }
            }
            println("Energy buffer: $energyBuffer/$energyBufferMax")
        }
    }

    fun getOtherModEnergyBuffer(conversionRatio: Double): Double {
        return energyBuffer * conversionRatio
    }

    fun drawEnergy(otherModEnergy: Double, conversionRatio: Double): Double {
        val drawEnergy = otherModEnergy / conversionRatio
        energyBuffer -= drawEnergy
        return drawEnergy
    }

    fun getOtherModOutMax(otherOutMax: Double, conversionRatio: Double): Double {
        return Math.min(getOtherModEnergyBuffer(conversionRatio), otherOutMax)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble("energyBuffer", energyBuffer)
        nbt.setDouble("selectedPower", selectedPower)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        energyBuffer = nbt.getDouble("energyBuffer")
        selectedPower = nbt.getDouble("selectedPower")
    }

    override fun hasGui(side: Direction): Boolean {
        return true
    }

    override fun publishSerialize(stream: DataOutputStream) {
        super.publishSerialize(stream)
        try {
            stream.writeDouble(selectedPower)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream, player: EntityPlayerMP) {
        try {
            when (stream.readByte()) {
                NetworkType.SET_POWER.id -> {
                    selectedPower = stream.readDouble()
                    needPublish()
                }
                NetworkType.SET_IC2_TIER.id -> {
                    val tier = stream.readInt()
                    if (tier in 1..5) {
                        val tierLimit = when(tier) {
                            1 -> 32
                            2 -> 128
                            3 -> 512
                            4 -> 2048
                            5 -> 8192
                            else -> 32
                        }
                        selectedPower = tierLimit * (1 / Other.getElnToIc2ConversionRatio())
                        needPublish()
                    }
                }
                else -> {}
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getNodeUuid(): String {
        return nodeUuidStatic
    }

    companion object {
        @JvmStatic
        val nodeUuidStatic = "ElnToOther"
    }
}

enum class NetworkType(val id: Byte) {
    SET_POWER(1),
    SET_IC2_TIER(2)
}
