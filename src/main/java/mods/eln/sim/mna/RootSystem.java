package mods.eln.sim.mna;

import mods.eln.misc.Profiler;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.*;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.ISubSystemProcessFlush;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;

import java.util.*;

public class RootSystem {

    double dt;
    int interSystemOverSampling;

    public ArrayList<SubSystem> systems = new ArrayList<SubSystem>();

    public Set<Component> addComponents = new HashSet<Component>();
    public HashSet<State> addStates = new HashSet<State>();

    static final int maxSubSystemSize = 100;

    ArrayList<ISubSystemProcessFlush> processF = new ArrayList<ISubSystemProcessFlush>();

    ArrayList<IRootSystemPreStepProcess> processPre = new ArrayList<IRootSystemPreStepProcess>();

    public RootSystem(double dt, int interSystemOverSampling) {
        this.dt = dt;
        this.interSystemOverSampling = interSystemOverSampling;
    }

    public void addComponent(Component c) {
        addComponents.add(c);
        c.onAddToRootSystem();

        for (State s : c.getConnectedStates()) {
            if (s == null) continue;
            if (s.getSubSystem() != null) {
                breakSystems(s.getSubSystem());
            }
        }
    }

    public void removeComponent(Component c) {
        SubSystem system = c.getSubSystem();

        if (system != null) {
            breakSystems(system);
        }

        addComponents.remove(c);
        c.onRemoveFromRootSystem();
    }

    public void addState(State s) {
        for (Component c : (ArrayList<Component>) s.getConnectedComponentsNotAbstracted().clone()) {
            if (c.getSubSystem() != null)
                breakSystems(c.getSubSystem());
        }
        addStates.add(s);
    }

    public void removeState(State s) {
        SubSystem system = s.getSubSystem();
        if (system != null) {
            breakSystems(system);
        }
        addStates.remove(s);
    }

    public void generate() {
        if (!addComponents.isEmpty() || !addStates.isEmpty()) {
            Profiler p = new Profiler();
            p.add("*** Generate ***");
            generateLine();
            generateSystems();
            generateInterSystems();

            int stateCnt = 0, componentCnt = 0;

            for (SubSystem s : systems) {
                stateCnt += s.states.size();
                componentCnt += s.component.size();
            }
            p.stop();
            Utils.println(p + " **** " + stateCnt + "   " + componentCnt);
        }
    }

    private boolean isValidForLine(State s) {
        if (!s.canBeSimplifiedByLine()) return false;
        List<Component> sc = s.getConnectedComponentsNotAbstracted();
        if (sc.size() != 2) return false;
        for (Component c : sc) {
            if (!(c instanceof Resistor)) {
                return false;
            }
        }

        return true;
    }

    private void generateLine() {
        Set<State> stateScope = new HashSet<State>();
        for (State s : addStates) {
            if (isValidForLine(s)) {
                stateScope.add(s);
            }
        }

        while (!stateScope.isEmpty()) {
            State sRoot = stateScope.iterator().next();

            State sPtr = sRoot;
            Resistor rPtr = (Resistor) sPtr.getConnectedComponentsNotAbstracted().get(0);
            while (true) {
                for (Component c : sPtr.getConnectedComponentsNotAbstracted()) {
                    if (c != rPtr) {
                        rPtr = (Resistor) c;
                        break;
                    }
                }
                State sNext = null;

                if (sPtr != rPtr.aPin)
                    sNext = rPtr.aPin;
                else if (sPtr != rPtr.bPin) sNext = rPtr.bPin;

                if (sNext == null || sNext == sRoot || !stateScope.contains(sNext)) break;

                sPtr = sNext;
            }

            LinkedList<State> lineStates = new LinkedList<State>();
            LinkedList<Resistor> lineResistors = new LinkedList<Resistor>();

            lineResistors.add(rPtr);
            while (true) {
                lineStates.add(sPtr);
                stateScope.remove(sPtr);
                for (Component c : sPtr.getConnectedComponentsNotAbstracted()) {
                    if (c != rPtr) {
                        rPtr = (Resistor) c;
                        break;
                    }
                }
                lineResistors.add(rPtr);

                State sNext = null;

                if (sPtr != rPtr.aPin)
                    sNext = rPtr.aPin;
                else if (sPtr != rPtr.bPin) sNext = rPtr.bPin;

                if (sNext == null || !stateScope.contains(sNext)) break;

                sPtr = sNext;
            }

            if (lineResistors.getFirst() == lineResistors.getLast()) {
                lineResistors.pop();
                lineStates.pop();
            }

            Line.newLine(this, lineResistors, lineStates);
        }
    }

    private void generateSystems() {
        LinkedList<State> firstState = new LinkedList<State>();
        for (State s : addStates) {
            if (s.mustBeFarFromInterSystem()) {
                firstState.add(s);
            }
        }

        for (State s : firstState) {
            if (s.getSubSystem() == null) {
                buildSubSystem(s);
            }
        }

        while (!addStates.isEmpty()) {
            State root = addStates.iterator().next();
            buildSubSystem(root);
        }
    }

    public void generateInterSystems() {
        Iterator<Component> ic = addComponents.iterator();
        while (ic.hasNext()) {
            Component c = ic.next();
            Resistor r;
            try {
                r = (Resistor) c;
                // If a pin is disconnected, we can't be intersystem
                if(r.aPin == null || r.bPin == null) continue;
            } catch (ClassCastException cce) {
                Utils.println("WARN: RootSystem tried to treat a " + c.getClass() + " as a resistor");
                continue;
            }

            try {
                new InterSystemAbstraction(this, r);
            } catch(NullPointerException npe) {
                Utils.println("WARN: failed to create InterSystemAbstraction for Resistor: " + r);
                SubSystem sa = r.aPin.getSubSystem(), sb = r.bPin.getSubSystem();
                Utils.println("... with subsystems: " + sa + ", " + sb);
                Utils.println("WARN: Did you remember to add ALL electrical components to the simulation BEFORE connecting?");
            }
            ic.remove();
        }
    }

    public void step() {
        Profiler profiler = new Profiler();
        profiler.add("Generate");
        generate();
        profiler.add("interSystem");
        for (int idx = 0; idx < interSystemOverSampling; idx++) {
            for (IRootSystemPreStepProcess p : processPre) {
                p.rootSystemPreStepProcess();
            }
        }

        profiler.add("stepCalc");
        for (SubSystem s : systems) {
            s.stepCalc();
        }
        profiler.add("stepFlush");
        for (SubSystem s : systems) {
            s.stepFlush();
        }
        profiler.add("simProcessFlush");
        for (ISubSystemProcessFlush p : processF) {
            p.simProcessFlush();
        }

        profiler.stop();
    }

    private void buildSubSystem(State root) {
        Set<Component> componentSet = new HashSet<Component>();
        Set<State> stateSet = new HashSet<State>();

        LinkedList<State> roots = new LinkedList<State>();
        roots.push(root);
        buildSubSystem(roots, componentSet, stateSet);

        addComponents.removeAll(componentSet);
        addStates.removeAll(stateSet);

        SubSystem subSystem = new SubSystem(this, dt);
        subSystem.addState(stateSet);
        subSystem.addComponent(componentSet);

        systems.add(subSystem);
    }

    private void buildSubSystem(LinkedList<State> roots, Set<Component> componentSet, Set<State> stateSet) {
        boolean privateSystem = roots.getFirst().isPrivateSubSystem();

        while (!roots.isEmpty()) {
            State sExplored = roots.pollFirst();
            stateSet.add(sExplored);

            for (Component c : sExplored.getConnectedComponentsNotAbstracted()) {
                if (!privateSystem && roots.size() + stateSet.size() > maxSubSystemSize && c.canBeReplacedByInterSystem()) {
                    continue;
                }
                if (componentSet.contains(c)) continue;
                boolean noGo = false;
                for (State sNext : c.getConnectedStates()) {
                    if (sNext == null) continue;
                    if (sNext.getSubSystem() != null) {
                        noGo = true;
                        break;
                    }
                    if (sNext.isPrivateSubSystem() != privateSystem) {
                        noGo = true;
                        break;
                    }
                }

                if (noGo) continue;
                componentSet.add(c);
                for (State sNext : c.getConnectedStates()) {
                    if (sNext == null) continue;
                    if (stateSet.contains(sNext)) continue;
                    roots.addLast(sNext);
                }
            }
        }
    }

    private SubSystem findSubSystemWith(State state) {
        for (SubSystem s : systems) {
            if (s.containe(state)) return s;
        }

        return null;
    }

    public void breakSystems(SubSystem sub) {
        if (sub.breakSystem()) {
            for (SubSystem s : sub.interSystemConnectivity) {
                breakSystems(s);
            }
        }
    }

    public static void main(String[] args) {
        RootSystem s = new RootSystem(0.1, 1);

        VoltageState n1, n2;
        VoltageSource u1;
        Resistor r1, r2;

        s.addState(n1 = new VoltageState());
        s.addState(n2 = new VoltageState());

        s.addComponent((u1 = new VoltageSource("")).setVoltage(1).connectTo(n1, null));

        s.addComponent((r1 = new Resistor()).setResistance(10).connectTo(n1, n2));
        s.addComponent((r2 = new Resistor()).setResistance(20).connectTo(n2, null));

        VoltageState n11, n12;
        VoltageSource u11;
        Resistor r11, r12, r13;

        s.addState(n11 = new VoltageState());
        s.addState(n12 = new VoltageState());

        s.addComponent((u11 = new VoltageSource("")).setVoltage(1).connectTo(n11, null));

        s.addComponent((r11 = new Resistor()).setResistance(10).connectTo(n11, n12));
        s.addComponent((r12 = new Resistor()).setResistance(30).connectTo(n12, null));

        InterSystem i01;

        s.addComponent((i01 = new InterSystem()).setResistance(10).connectTo(n2, n12));

        for (int i = 0; i < 50; i++) {
            s.step();
        }

        s.addComponent((r13 = new Resistor()).setResistance(30).connectTo(n12, null));

        for (int i = 0; i < 50; i++) {
            s.step();
        }

        s.step();
    }

    public int getSubSystemCount() {
        return systems.size();
    }

    public void addProcess(ISubSystemProcessFlush p) {
        processF.add(p);
    }

    public void removeProcess(ISubSystemProcessFlush p) {
        processF.remove(p);
    }

    public void addProcess(IRootSystemPreStepProcess p) {
        processPre.add(p);
    }

    public void removeProcess(IRootSystemPreStepProcess p) {
        processPre.remove(p);
    }

    public boolean isRegistred(ElectricalLoad load) {
        return load.getSubSystem() != null || addStates.contains(load);
    }
}

//TODO: garbadge collector
//TODO: ghost suprresion
