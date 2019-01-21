package mods.eln.sixnode.thermalsensor;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.sim.IProcess;

public class ThermalSensorProcess implements IProcess {

    ThermalSensorElement sensor;

    public ThermalSensorProcess(ThermalSensorElement sensor) {
        this.sensor = sensor;
    }

    @Override
    public void process(double time) {
        if (sensor.typeOfSensor == sensor.temperatureType) {
            setOutput(sensor.thermalLoad.Tc);
        } else if (sensor.typeOfSensor == sensor.powerType) {
            setOutput(sensor.thermalLoad.getPower());
        }
    }

    void setOutput(double physical) {
        double U = (physical - sensor.lowValue) / (sensor.highValue - sensor.lowValue) * Vars.SVU;
        if (U > Vars.SVU) U = Vars.SVU;
        if (U < 0) U = 0;
        sensor.outputGateProcess.setU(U);
    }
}
