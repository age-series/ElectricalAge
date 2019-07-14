package mods.eln.sim

open class FurnaceProcess(var load: ThermalLoad) : IProcess {
    var combustibleEnergy = 0.0
    var nominalCombustibleEnergy = 1.0
    var nominalPower = 1.0
    var gain = 1.0
        set(gain) {
            var gain = gain
            if (gain < gainMin) gain = gainMin
            if (gain > 1.0) gain = 1.0
            field = gain
        }
    private var gainMin = 0.0

    val p: Double
        get() = combustibleEnergy / nominalCombustibleEnergy * nominalPower * this.gain

    override fun process(time: Double) {
        val energyConsumed = p * time
        combustibleEnergy -= energyConsumed
        load.PcTemp += energyConsumed / time
    }

    fun setGainMin(gainMin: Double) {
        this.gainMin = gainMin
        gain = gain
    }
}
