package mods.eln.sim;

public class ThermalResistor implements IProcess {

    ThermalLoad a;
    ThermalLoad b;

    protected double thermalResistance;
    protected double thermalResistanceInverse;

    public ThermalResistor(ThermalLoad a, ThermalLoad b) {
        this.a = a;
        this.b = b;
        highImpedance();
    }

    @Override
    public void process(double time) {
        double power = (a.temperatureCelsius - b.temperatureCelsius) * thermalResistanceInverse;
        a.PcTemp -= power;
        b.PcTemp += power;
    }

    public double getPower() {
        return (a.temperatureCelsius - b.temperatureCelsius) * thermalResistanceInverse;
    }

    public void setThermalResistance(double thermalResistance) {
        this.thermalResistance = thermalResistance;
        thermalResistanceInverse = 1 / thermalResistance;
    }

    public double getThermalResistance() {
        return thermalResistance;
    }

    public void highImpedance() {
        setThermalResistance(1000000000.0);
    }
}
