package mods.eln.sim

class ThermalResistor
//double P = 0;

(internal var a: ThermalLoad, internal var b: ThermalLoad) : IProcess {

    var r: Double = 0.0
        get() = field
        set(r) {
            field = r
            Rinv = 1 / r
        }
    protected var Rinv: Double = 1 / r

    val p: Double
        get() = (a.Tc - b.Tc) * Rinv

    init {
        highImpedance()
    }

    override fun process(time: Double) {
        val P = (a.Tc - b.Tc) * Rinv
        a.PcTemp -= P
        b.PcTemp += P
    }
    /*
	public double getVoltage() {
		return P * R;
	}*/

    fun highImpedance() {
        r = 1000000000.0
    }
}
