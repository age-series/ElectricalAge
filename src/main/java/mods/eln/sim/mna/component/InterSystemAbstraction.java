package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sim.mna.SubSystem.Thevenin;

public class InterSystemAbstraction implements IAbstractor, IDestructor, IRootSystemPreStepProcess {

    VoltageState aNewState;
    Resistor aNewResistor;
    VoltageSource aNewDelay;
    VoltageState bNewState;
    Resistor bNewResistor;
    VoltageSource bNewDelay;

    RootSystem root;
    Resistor interSystemResistor;

    State aState;
    State bState;
    SubSystem aSystem;
    SubSystem bSystem;

    public InterSystemAbstraction(RootSystem root, Resistor interSystemResistor) {
        this.interSystemResistor = interSystemResistor;
        this.root = root;

        aState = interSystemResistor.aPin;
        bState = interSystemResistor.bPin;
        aSystem = aState.getSubSystem();
        bSystem = bState.getSubSystem();

        aSystem.interSystemConnectivity.add(bSystem);
        bSystem.interSystemConnectivity.add(aSystem);

        aNewState = new VoltageState();
        aNewResistor = new Resistor();
        aNewDelay = new VoltageSource("aNewDelay");
        bNewState = new VoltageState();
        bNewResistor = new Resistor();
        bNewDelay = new VoltageSource("bNewDelay");

        aNewResistor.connectGhostTo(aState, aNewState);
        aNewDelay.connectTo(aNewState, null);
        bNewResistor.connectGhostTo(bState, bNewState);
        bNewDelay.connectTo(bNewState, null);

        calibrate();

        aSystem.addComponent(aNewResistor);
        aSystem.addState(aNewState);
        aSystem.addComponent(aNewDelay);
        bSystem.addComponent(bNewResistor);
        bSystem.addState(bNewState);
        bSystem.addComponent(bNewDelay);

        aSystem.breakDestructor.add(this);
        bSystem.breakDestructor.add(this);

        interSystemResistor.abstractedBy = this;

        root.addProcess(this);
    }

    void calibrate() {
        double voltage = (aState.state + bState.state) / 2;
        aNewDelay.setVoltage(voltage);
        bNewDelay.setVoltage(voltage);

        double resistance = interSystemResistor.getResistance() / 2;
        aNewResistor.setResistance(resistance);
        bNewResistor.setResistance(resistance);
    }

    @Override
    public void dirty(Component component) {
        calibrate();
    }

    @Override
    public SubSystem getAbstractorSubSystem() {
        return aSystem;
    }

    @Override
    public void destruct() {
        aSystem.breakDestructor.remove(this);
        aSystem.removeComponent(aNewDelay);
        aSystem.removeComponent(aNewResistor);
        aSystem.removeState(aNewState);
        bSystem.breakDestructor.remove(this);
        bSystem.removeComponent(bNewDelay);
        bSystem.removeComponent(bNewResistor);
        bSystem.removeState(bNewState);

        root.removeProcess(this);

        interSystemResistor.abstractedBy = null;

        aSystem.component.add(interSystemResistor);
    }

    @Override
    public void rootSystemPreStepProcess() {
        Thevenin a = aNewDelay.getSubSystem().getTh(aState,aNewDelay);
        Thevenin b = bNewDelay.getSubSystem().getTh(bState,bNewDelay);

        double voltage = (a.voltage - b.voltage) * b.resistance / (a.resistance + b.resistance) + b.voltage;
        if (Double.isNaN(voltage)) {
            voltage = 0;
        }

        aNewDelay.setVoltage(voltage);
        bNewDelay.setVoltage(voltage);
    }
}
