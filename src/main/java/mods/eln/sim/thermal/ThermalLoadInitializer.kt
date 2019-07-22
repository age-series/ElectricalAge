package mods.eln.sim.thermal

class ThermalLoadInitializer(var warmLimit: Double, var coolLimit: Double, internal var heatingTao: Double, internal var conductionTao: Double) {

    internal var Rs: Double = 0.toDouble()
    internal var Rp: Double = 0.toDouble()
    internal var C: Double = 0.toDouble()

    /*	public ThermalLoadInitializer (
            double warmLimit,double coolLimit,
			double heatingTao,double conductionTao,
			double P) {
		this.conductionTao = conductionTao;
		this.coolLimit = coolLimit;
		this.heatingTao = heatingTao;
		this.warmLimit = warmLimit;
		setMaximalPower(P);
	}*/

    fun setMaximalPower(P: Double) {
        C = P * heatingTao / warmLimit
        Rp = warmLimit / P
        Rs = conductionTao / C / 2.0

        ThermalLoad.checkThermalLoad(Rs, Rp, C)
    }

    fun applyTo(load: ThermalLoad) {
        load.set(Rs, Rp, C)
    }

    fun applyTo(doggy: ThermalLoadWatchDog) {
        doggy.set(this)
    }

    fun copy(): ThermalLoadInitializer {
        val thermalLoad = ThermalLoadInitializer(warmLimit, coolLimit, heatingTao, conductionTao)
        thermalLoad.Rp = Rp
        thermalLoad.Rs = Rs
        thermalLoad.C = C
        return thermalLoad
    }
}
