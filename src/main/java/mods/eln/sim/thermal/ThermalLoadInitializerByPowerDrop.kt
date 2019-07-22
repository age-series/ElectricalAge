package mods.eln.sim.thermal

class ThermalLoadInitializerByPowerDrop(var warmLimit: Double, var coolLimit: Double, internal var heatingTao: Double, internal var TConductivityDrop: Double) {

    var Rs: Double = 0.toDouble()
    var Rp: Double = 0.toDouble()
    var C: Double = 0.toDouble()

    fun setMaximalPower(P: Double) {
        C = P * heatingTao / warmLimit
        Rp = warmLimit / P
        Rs = TConductivityDrop / P / 2.0

        ThermalLoad.checkThermalLoad(Rs, Rp, C)
    }

    fun applyTo(load: ThermalLoad) {
        load.set(Rs, Rp, C)
    }

    fun copy(): ThermalLoadInitializerByPowerDrop {
        val thermalLoad = ThermalLoadInitializerByPowerDrop(warmLimit, coolLimit, heatingTao, TConductivityDrop)
        thermalLoad.Rp = Rp
        thermalLoad.Rs = Rs
        thermalLoad.C = C
        return thermalLoad
    }
}
