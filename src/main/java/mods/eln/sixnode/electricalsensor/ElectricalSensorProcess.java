package mods.eln.sixnode.electricalsensor;

import mods.eln.Eln;
import mods.eln.sim.IProcess;

public class ElectricalSensorProcess implements IProcess {

    ElectricalSensorElement sensor;

    public ElectricalSensorProcess(ElectricalSensorElement sensor) {
        this.sensor = sensor;
    }

    @Override
    public void process(double time) {
        if (sensor.typeOfSensor == sensor.voltageType) {
            setOutput(sensor.aLoad.getVoltage());
        } else if (sensor.typeOfSensor == sensor.currantType) {
            double output = 0;
            switch (sensor.dirType) {
                case ElectricalSensorElement.dirNone:
                    output = Math.abs(sensor.resistor.getCurrent());
                    break;
                case ElectricalSensorElement.dirAB:
                    output = (sensor.resistor.getCurrent());
                    break;
                case ElectricalSensorElement.dirBA:
                    output = (-sensor.resistor.getCurrent());
                    break;
            }

            setOutput(output);
        } else if (sensor.typeOfSensor == sensor.powerType) {
            double output = 0;
            switch (sensor.dirType) {
                case ElectricalSensorElement.dirNone:
                    output = Math.abs(sensor.resistor.getCurrent() * sensor.aLoad.getVoltage());
                    break;
                case ElectricalSensorElement.dirAB:
                    output = (sensor.resistor.getCurrent() * sensor.aLoad.getVoltage());
                    break;
                case ElectricalSensorElement.dirBA:
                    output = (-sensor.resistor.getCurrent() * sensor.aLoad.getVoltage());
                    break;
            }

            setOutput(output);
        }
    }

    void setOutput(double physical) {
        double U = (physical - sensor.lowValue) / (sensor.highValue - sensor.lowValue) * Eln.SVU;
        if (U > Eln.SVU) U = Eln.SVU;
        if (U < 0) U = 0;
        sensor.outputGateProcess.setVoltage(U);
    }
}
