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

    boolean canAdd(Component c) {
        return (c instanceof Resistor);
    }

    void add(Resistor c) {
        ofInterSystem |= c.canBeReplacedByInterSystem();
        resistors.add(c);
    }

    @Override
    public boolean canBeReplacedByInterSystem() {
        return ofInterSystem;
    }

    public void recalculateR() {
        double R = 0;
        for (Resistor r : resistors) {
            R += r.getR();
        }

        setR(R);
    }

    void restoreResistorIntoCircuit() {
        this.breakConnection();
    }

    void removeResistorFromCircuit() {
    }

    public static void newLine(RootSystem root, LinkedList<Resistor> resistors, LinkedList<State> states) {
        if (resistors.isEmpty()) {
        } else if (resistors.size() == 1) {
        } else {
            Resistor first = resistors.getFirst();
            Resistor last = resistors.getLast();
            State stateBefore = first.aPin == states.getFirst() ? first.bPin : first.aPin;
            State stateAfter = last.aPin == states.getLast() ? last.bPin : last.aPin;

            Line l = new Line();
            l.resistors = resistors;
            l.states = states;
            l.recalculateR();
            root.addComponents.removeAll(resistors);
            root.addStates.removeAll(states);
            root.addComponents.add(l);
            l.connectTo(stateBefore, stateAfter);
            l.removeResistorFromCircuit();

            root.addProcess(l);

            for (Resistor r : resistors) {
                r.abstractedBy = l;
                l.ofInterSystem |= r.canBeReplacedByInterSystem();
            }

            for (State s : states) {
                s.abstractedBy = l;
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
        double i = (aPin.state - bPin.state) * getRInv();
        double u = aPin.state;
        Iterator<Resistor> ir = resistors.iterator();

        for (State s : states) {
            Resistor r = ir.next();
            u -= r.getR() * i;
            s.state = u;
        }
    }

    @Override
    public void addedTo(SubSystem s) {
        s.addProcess(this);
        super.addedTo(s);
    }

    @Override
    public void quitSubSystem() {
    }

    @Override
    public void dirty(Component component) {
        recalculateR();
        if (isAbstracted())
            abstractedBy.dirty(this);
    }

    @Override
    public SubSystem getAbstractorSubSystem() {
        return getSubSystem();
    }
}
