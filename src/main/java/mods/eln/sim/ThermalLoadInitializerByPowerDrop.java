package mods.eln.sim;

import mods.eln.Eln;

public class ThermalLoadInitializerByPowerDrop {

    public double maximumTemperature, minimumTemperature;
    double heatingTao;
    double TConductivityDrop;

    public double Rs;
    public double Rp;
    /**
     * Thermal capacitance.
     */
    public double C;

    /**
     * @param maximumTemperature Intended maximum temperature in celsius.
     * @param minimumTemperature Intended minimum temperature in celsius.
     * @param heatingTao
     * @param TConductivityDrop
     */
    public ThermalLoadInitializerByPowerDrop(double maximumTemperature, double minimumTemperature, double heatingTao, double TConductivityDrop) {
        this.TConductivityDrop = TConductivityDrop;
        this.minimumTemperature = minimumTemperature;
        this.heatingTao = heatingTao;
        this.maximumTemperature = maximumTemperature;
    }

    public void setMaximalPower(double power) {
        C = power * heatingTao / maximumTemperature;
        Rp = maximumTemperature / power;
        Rs = TConductivityDrop / power / 2;

        Eln.simulator.checkThermalLoad(Rs, Rp, C);
    }

    public void applyToThermalLoad(ThermalLoad load) {
        load.set(Rs, Rp, C);
    }

    public ThermalLoadInitializerByPowerDrop copy() {
        ThermalLoadInitializerByPowerDrop thermalLoad = new ThermalLoadInitializerByPowerDrop(maximumTemperature, minimumTemperature, heatingTao, TConductivityDrop);
        thermalLoad.Rp = Rp;
        thermalLoad.Rs = Rs;
        thermalLoad.C = C;
        return thermalLoad;
    }
}
