package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessFlush;
import mods.eln.sim.mna.state.State;

import java.util.Iterator;
import java.util.LinkedList;

public class Line extends Resistor implements ISubSystemProcessFlush, IAbstractor {

    public LinkedList<Resistor> resistors = new LinkedList<Resistor>(); //from a to b
    public LinkedList<State> states = new LinkedList<State>(); //from a to b

    boolean ofInterSystem;

    boolean canAddComponent(Component c) {
        return (c instanceof Resistor);
    }

    void addResistor(Resistor c) {
        ofInterSystem |= c.canBeReplacedByInterSystem();
        resistors.add(c);
    }

    @Override
    public boolean canBeReplacedByInterSystem() {
        return ofInterSystem;
    }

    public void recalculateResistance() {
        double resistance = 0;
        for (Resistor r : resistors) {
            resistance += r.getResistance();
        }

        setResistance(resistance);
    }

    void restoreResistorIntoCircuit() {
        this.breakConnection();
    }

    void removeResistorFromCircuit() {
    }

    public static void newLine(RootSystem root, LinkedList<Resistor> resistors, LinkedList<State> states) {
        if (resistors.size() > 1) {
            Resistor first = resistors.getFirst();
            Resistor last = resistors.getLast();
            State stateBefore = first.aPin == states.getFirst() ? first.bPin : first.aPin;
            State stateAfter = last.aPin == states.getLast() ? last.bPin : last.aPin;

            Line line = new Line();
            line.resistors = resistors;
            line.states = states;
            line.recalculateResistance();
            root.addComponents.removeAll(resistors);
            root.addStates.removeAll(states);
            root.addComponents.add(line);
            line.connectTo(stateBefore, stateAfter);
            line.removeResistorFromCircuit();

            root.addProcess(line);

            for (Resistor r : resistors) {
                r.abstractedBy = line;
                line.ofInterSystem |= r.canBeReplacedByInterSystem();
            }

            for (State s : states) {
                s.abstractedBy = line;
            }
        }
    }

    @Override
    public void returnToRootSystem(RootSystem root) {
        for (Resistor r : resistors) {
            r.abstractedBy = null;
        }

        for (State s : states) {
            s.abstractedBy = null;
        }

        restoreResistorIntoCircuit();

        root.addStates.addAll(states);
        root.addComponents.addAll(resistors);

        root.removeProcess(this);
    }

    @Override
    public void simProcessFlush() {
        double current = (aPin.state - bPin.state) * getResistanceInverse();
        double voltage = aPin.state;
        Iterator<Resistor> ir = resistors.iterator();

        for (State s : states) {
            Resistor r = ir.next();
            voltage -= r.getResistance() * current;
            s.state = voltage;
        }
    }

    @Override
    public void addToSubsystem(SubSystem s) {
        s.addProcess(this);
        super.addToSubsystem(s);
    }

    @Override
    public void quitSubSystem() {
    }

    @Override
    public void dirty(Component component) {
        recalculateResistance();
        if (isAbstracted())
            abstractedBy.dirty(this);
    }

    @Override
    public SubSystem getAbstractorSubSystem() {
        return getSubSystem();
    }
}
