package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sim.mna.SubSystem.Th;

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
        double u = (aState.state + bState.state) / 2;
        aNewDelay.setU(u);
        bNewDelay.setU(u);

        double r = interSystemResistor.getR() / 2;
        aNewResistor.setR(r);
        bNewResistor.setR(r);
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
        Th a = aNewDelay.getSubSystem().getTh(aState,aNewDelay);
        Th b = bNewDelay.getSubSystem().getTh(bState,bNewDelay);

        double U = (a.U - b.U) * b.R / (a.R + b.R) + b.U;
        if (Double.isNaN(U)) {
            U = 0;
        }

        aNewDelay.setU(U);
        bNewDelay.setU(U);
    }
}
