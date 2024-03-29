package mods.eln.transparentnode.electricalantennarx;

import mods.eln.sim.IProcess;

public class ElectricalAntennaRxSlowProcess implements IProcess {

    ElectricalAntennaRxElement element;

    public ElectricalAntennaRxSlowProcess(ElectricalAntennaRxElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        if (element.powerSrc.getPower() > element.descriptor.electricalMaximalPower) {
            element.node.physicalSelfDestruction(2.0f);
        } else if (element.powerOut.getVoltage() > element.descriptor.electricalMaximalVoltage) {
            element.node.physicalSelfDestruction(2.0f);
        }
    }
}
