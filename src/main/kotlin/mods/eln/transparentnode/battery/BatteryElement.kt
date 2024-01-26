package mods.eln.transparentnode.battery

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtBatteryProcess
import mods.eln.sim.nbt.NbtBatterySlowProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class BatteryElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor) : TransparentNodeElement(transparentNode, descriptor) {
    override var descriptor: BatteryDescriptor = descriptor as BatteryDescriptor
    var positiveLoad = NbtElectricalLoad("positiveLoad")
    var negativeLoad = NbtElectricalLoad("negativeLoad")
    var voltageSource = VoltageSource("volSrc", positiveLoad, negativeLoad)
    var thermalLoad = NbtThermalLoad("thermalLoad")
    var negativeETProcess = ElectricalLoadHeatThermalLoad(negativeLoad, thermalLoad)
    var thermalWatchdog = ThermalLoadWatchDog(thermalLoad)
    var batteryProcess = NbtBatteryProcess(positiveLoad, negativeLoad, this.descriptor.UfCharge, 0.0, voltageSource, thermalLoad)
    var dischargeResistor = Resistor(positiveLoad, negativeLoad)
    var batterySlowProcess = NbtBatterySlowProcess(node!!, batteryProcess, thermalLoad)
    var fromItemStack = false
    var fromItemstackCharge = 0.0
    var fromItemstackLife = 0.0
    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        if (side == front.left()) return positiveLoad
        return if (side == front.right()) negativeLoad else null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        if (lrdu != LRDU.Down) return null
        if (side == front.left()) return thermalLoad
        return if (side == front.right()) thermalLoad else null
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        if (side == front.left()) return NodeBase.maskElectricalPower
        return if (side == front.right()) NodeBase.maskElectricalPower else 0
    }

    override fun multiMeterString(side: Direction): String {
        var str = ""
        str += Utils.plotVolt("Ubat:", batteryProcess.u)
        str += Utils.plotAmpere("I:", batteryProcess.dischargeCurrent)
        str += Utils.plotPercent("Charge:", batteryProcess.charge)
        // batteryProcess.life is a percentage from 1.0 to 0.0.
        str += Utils.plotPercent("Life:", batteryProcess.life)
        return str
    }

    override fun thermoMeterString(side: Direction): String {
        return Utils.plotCelsius("Tbat:", thermalLoad.temperatureCelsius)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeFloat((batteryProcess.u * batteryProcess.dischargeCurrent).toFloat())
            stream.writeFloat(batteryProcess.energy.toFloat())
            stream.writeShort((batteryProcess.life * 1000).toInt())
            node!!.lrduCubeMask.getTranslate(Direction.YN).serialize(stream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun initialize() {
        descriptor.applyTo(batteryProcess)
        descriptor.applyTo(thermalLoad)
        descriptor.applyTo(dischargeResistor)
        descriptor.applyTo(batterySlowProcess)
        positiveLoad.serialResistance = descriptor.electricalRs
        negativeLoad.serialResistance = descriptor.electricalRs
        dischargeResistor.resistance = MnaConst.highImpedance
        if (fromItemStack) {
            batteryProcess.life = fromItemstackLife
            batteryProcess.charge = fromItemstackCharge
            fromItemStack = false
        }
        connect()
    }

    override fun reconnect() {
        disconnect()
        connect()
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun readItemStackNBT(nbt: NBTTagCompound?) {
        super.readItemStackNBT(nbt)
        fromItemstackCharge = nbt?.getDouble("charge")?: descriptor.getChargeInTag(this.descriptor.newItemStack())
        fromItemstackLife = nbt?.getDouble("life")?: descriptor.getLifeInTag(this.descriptor.newItemStack())
        fromItemStack = true
    }

    override fun getItemStackNBT(): NBTTagCompound {
        val nbt = NBTTagCompound()
        nbt.setDouble("charge", batteryProcess.charge)
        nbt.setDouble("life", batteryProcess.life)
        return nbt
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Charge")] = Utils.plotPercent("", batteryProcess.charge)
        info[tr("Energy")] = Utils.plotEnergy("", batteryProcess.energy)
        info[tr("Life")] = Utils.plotPercent("", batteryProcess.life)
        if (Eln.wailaEasyMode) {
            info[tr("Voltage")] = Utils.plotVolt("", batteryProcess.u)
            info[tr("Current")] = Utils.plotAmpere("", batteryProcess.dischargeCurrent)
            info[tr("Temperature")] = Utils.plotCelsius("", thermalLoad.temperatureCelsius)
        }
        info[tr("Subsystem Matrix Size")] = Utils.renderSubSystemWaila(positiveLoad.subSystem)
        return info
    }

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(negativeLoad)
        electricalComponentList.add(Resistor(positiveLoad, null))
        electricalComponentList.add(Resistor(negativeLoad, null))
        electricalComponentList.add(dischargeResistor)
        electricalComponentList.add(voltageSource)
        thermalLoadList.add(thermalLoad)
        electricalProcessList.add(batteryProcess)
        thermalFastProcessList.add(negativeETProcess)
        slowProcessList.add(batterySlowProcess)
        slowProcessList.add(NodePeriodicPublishProcess(transparentNode, 1.0, 0.0))
        batteryProcess.IMax = this.descriptor.IMax
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog
            .setMaximumTemperature(this.descriptor.thermalWarmLimit)
            .setDestroys(WorldExplosion(this).machineExplosion())
    }
}
