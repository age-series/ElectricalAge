package mods.eln.sixnode.electricalsensor;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.sim.IProcess;

public class ElectricalSensorProcess implements IProcess {

    ElectricalSensorElement sensor;

    public ElectricalSensorProcess(ElectricalSensorElement sensor) {
        this.sensor = sensor;
    }

    @Override
    public void process(double time) {
        if (sensor.typeOfSensor == sensor.voltageType) {
            setOutput(sensor.aLoad.getU());
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
                    output = Math.abs(sensor.resistor.getCurrent() * sensor.aLoad.getU());
                    break;
                case ElectricalSensorElement.dirAB:
                    output = (sensor.resistor.getCurrent() * sensor.aLoad.getU());
                    break;
                case ElectricalSensorElement.dirBA:
                    output = (-sensor.resistor.getCurrent() * sensor.aLoad.getU());
                    break;
            }

            setOutput(output);
        }
    }

    void setOutput(double physical) {
        double U = (physical - sensor.lowValue) / (sensor.highValue - sensor.lowValue) * Vars.SVU;
        if (U > Vars.SVU) U = Vars.SVU;
        if (U < 0) U = 0;
        sensor.outputGateProcess.setU(U);
    }
}
