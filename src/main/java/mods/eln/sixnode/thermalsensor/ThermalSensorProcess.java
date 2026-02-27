package mods.eln.sixnode.thermalsensor;

import mods.eln.Eln;
import mods.eln.sim.IProcess;

public class ThermalSensorProcess implements IProcess {

    ThermalSensorElement sensor;

    public ThermalSensorProcess(ThermalSensorElement sensor) {
        this.sensor = sensor;
    }

    @Override
    public void process(double time) {
        if (sensor.typeOfSensor == sensor.temperatureType) {
            setOutput(toMeasuredTemperature(sensor.thermalLoad.temperatureCelsius, sensor.getAmbientTemperatureCelsius()));
        } else if (sensor.typeOfSensor == sensor.powerType) {
            setOutput(sensor.thermalLoad.getPower());
        }
    }

    static double toMeasuredTemperature(double thermalDeltaCelsius, double ambientCelsius) {
        return thermalDeltaCelsius + ambientCelsius;
    }

    void setOutput(double physical) {
        double U = (physical - sensor.lowValue) / (sensor.highValue - sensor.lowValue) * Eln.SVU;
        if (U > Eln.SVU) U = Eln.SVU;
        if (U < 0) U = 0;
        sensor.outputGateProcess.setVoltage(U);
    }
}
