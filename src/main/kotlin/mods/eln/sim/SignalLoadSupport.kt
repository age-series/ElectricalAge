package mods.eln.sim

import mods.eln.Eln
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalGateInputOutput
import mods.eln.sim.process.destruct.IDestructible

object SignalLoadSupport {
    @JvmStatic
    fun clampSignalVoltage(voltage: Double): Double {
        if (voltage.isNaN()) return 0.0
        if (voltage < 0.0) return 0.0
        if (voltage > Eln.SVU) return Eln.SVU
        return voltage
    }

    @JvmStatic
    fun clampSignalVoltage(voltage: Double, destructible: IDestructible?): Double {
        if (!voltage.isNaN() && (voltage < Eln.signalVoltageAcceptNegative || voltage > Eln.signalVoltageAcceptPositive)) {
            destructible?.destructImpl()
        }
        return clampSignalVoltage(voltage)
    }

    @JvmStatic
    fun clampNormalized(value: Double): Double {
        if (value.isNaN()) return 0.0
        if (value < 0.0) return 0.0
        if (value > 1.0) return 1.0
        return value
    }

    @JvmStatic
    fun toNormalized(voltage: Double): Double = clampSignalVoltage(voltage) * Eln.SVUinv

    @JvmStatic
    fun toVoltage(normalizedValue: Double): Double = clampNormalized(normalizedValue) * Eln.SVU

    @JvmStatic
    fun isSignalInputLoad(load: State): Boolean =
        load is NbtElectricalGateInput || load is NbtElectricalGateInputOutput

    @JvmStatic
    fun createReadComponents(loads: Collection<State>): ArrayList<Component> {
        val components = ArrayList<Component>()
        loads.forEach { load ->
            if (isSignalInputLoad(load)) {
                components.add(SignalRp(load))
            }
        }
        return components
    }
}
