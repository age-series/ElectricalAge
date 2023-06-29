package mods.eln.sim

import mods.eln.misc.FunctionTable
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

open class BatteryProcess(
    var positiveLoad: VoltageState?,
    var negativeLoad: VoltageState?,
    var voltageFunction: FunctionTable,
    @JvmField var IMax: Double,
    var voltageSource: VoltageSource,
    private var thermalLoad: ThermalLoad
) : IProcess {

    @JvmField
    var Q = 0.0
    var QNominal = 0.0
    var uNominal = 0.0
    @JvmField
    var life = 1.0
    var isRechargeable = true

    override fun process(time: Double) {
        val lastQ = Q
        var wasteQ = 0.0
        Q = Math.max(Q - voltageSource.current * time / QNominal, 0.0)
        if (Q > lastQ && !isRechargeable) {
            println("Battery is recharging when it shouldn't!")
            wasteQ = Q - lastQ
            Q = lastQ
        }
        val voltage = computeVoltage()
        voltageSource.u = voltage
        if (wasteQ > 0) {
            thermalLoad.movePowerTo(Math.abs(voltageSource.current * voltage))
        }
    }

    fun computeVoltage(): Double {
        val voltage = voltageFunction.getValue(Q / life)
        return Math.max(0.0, voltage * uNominal)
    }

    fun changeLife(newLife: Double) {
        if (newLife < life) {
            Q *= newLife / life
        }
        life = newLife
    }

    var charge: Double
        get() = Q / life
        set(charge) {
            Q = life * charge
        }
    val energy: Double
        get() {
            val stepNbr = 50
            val chargeStep = charge / stepNbr
            var chargeIntegrator = 0.0
            var energy = 0.0
            val QperStep = QNominal * life * chargeStep
            for (step in 0 until stepNbr) {
                val voltage = voltageFunction.getValue(chargeIntegrator) * uNominal
                energy += voltage * QperStep
                chargeIntegrator += chargeStep
            }
            return energy
        }
    val energyMax: Double
        get() {
            val stepNbr = 50
            val chargeStep = 1.0 / stepNbr
            var chargeIntegrator = 0.0
            var energy = 0.0
            val QperStep = QNominal * life * 1.0 / stepNbr
            for (step in 0 until stepNbr) {
                val voltage = voltageFunction.getValue(chargeIntegrator) * uNominal
                energy += voltage * QperStep
                chargeIntegrator += chargeStep
            }
            return energy
        }
    val u: Double
        get() = computeVoltage()
    val dischargeCurrent: Double
        get() = voltageSource.i
}
