package mods.eln.sim.mna;

import mods.eln.misc.Profiler;
import mods.eln.misc.Utils;
import mods.eln.sim.mna.component.*;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.misc.ISubSystemProcessFlush;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import org.apache.commons.numbers.core.DD;

import mods.eln.Eln;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SubSystem {
    public ArrayList<Component> component = new ArrayList<Component>();
    public List<State> states = new ArrayList<State>();
    public LinkedList<IDestructor> breakDestructor = new LinkedList<IDestructor>();
    public ArrayList<SubSystem> interSystemConnectivity = new ArrayList<SubSystem>();
    ArrayList<ISubSystemProcessI> processI = new ArrayList<ISubSystemProcessI>();
    State[] statesTab;

    RootSystem root;

    double dt;
    boolean matrixValid = false;

    int stateCount;
    double[][] A;
    boolean singularMatrix;
    private int singularMatrixCountSinceLastDrain = 0;
    private int inversionCountSinceLastDrain = 0;
    private long inversionTotalNanosecondsSinceLastDrain = 0L;
    private long inversionMaximumNanosecondsSinceLastDrain = 0L;

    DD[][] AInvdata;
    double[] Idata;

    double[] XtempData;

    boolean breaked = false;

    ArrayList<ISubSystemProcessFlush> processF = new ArrayList<ISubSystemProcessFlush>();

    public RootSystem getRoot() {
        return root;
    }

    public SubSystem(RootSystem root, double dt) {
        this.dt = dt;
        this.root = root;
    }

    public void invalidate() {
        matrixValid = false;
    }

    public void addComponent(Component c) {
        component.add(c);
        c.addToSubsystem(this);
        invalidate();
    }

    public void addState(State s) {
        states.add(s);
        s.setSubsystem(this);
        invalidate();
    }

    public void removeComponent(Component c) {
        component.remove(c);
        c.quitSubSystem();
        invalidate();
    }

    public void removeState(State s) {
        states.remove(s);
        s.quitSubSystem();
        invalidate();
    }

    public void removeProcess(ISubSystemProcessI p) {
        processI.remove(p);
        invalidate();
    }

    public void addComponent(Iterable<Component> i) {
        for (Component c : i) {
            addComponent(c);
        }
    }

    public void addState(Iterable<State> i) {
        for (State s : i) {
            addState(s);
        }
    }

    public void addProcess(ISubSystemProcessI p) {
        processI.add(p);
    }

    public void generateMatrix() {
        stateCount = states.size();

        Profiler p = new Profiler();
        p.add("Inversse with " + stateCount + " state : ");

        A = new double[stateCount][stateCount];
        Idata = new double[stateCount];
        XtempData = new double[stateCount];
        {
            int idx = 0;
            for (State s : states) {
                s.setId(idx++);
            }
        }

        for (Component c : component) {
            c.applyToSubsystem(this);
        }

        //	org.apache.commons.math3.linear.

        long inversionStartNanoseconds = Eln.simMetricsEnabled ? System.nanoTime() : 0L;
        try {
            AInvdata = invertMatrix(A);
            singularMatrix = false;
            if (Eln.simMetricsEnabled) {
                long inversionTimeNanoseconds = System.nanoTime() - inversionStartNanoseconds;
                inversionCountSinceLastDrain++;
                inversionTotalNanosecondsSinceLastDrain += inversionTimeNanoseconds;
                if (inversionTimeNanoseconds > inversionMaximumNanosecondsSinceLastDrain) {
                    inversionMaximumNanosecondsSinceLastDrain = inversionTimeNanoseconds;
                }
            }
        } catch (Exception e) {
            singularMatrix = true;
            AInvdata = null;
            if (stateCount > 1) {
                singularMatrixCountSinceLastDrain++;
                Utils.println("//////////SingularMatrix////////////");
            }
        }

        statesTab = new State[stateCount];
        statesTab = states.toArray(statesTab);

        matrixValid = true;

        p.stop();
        Utils.println(p);
    }

    public synchronized SubSystemDebugSnapshot captureDebugSnapshot() {
        if (!matrixValid || A == null) {
            generateMatrix();
        }

        double[][] matrixCopy = copyMatrix(A);
        double[] rhsCopy = Idata != null ? Idata.clone() : new double[0];

        String[] stateDescriptions = new String[stateCount];
        String[] stateOwners = new String[stateCount];
        for (int idx = 0; idx < stateCount; idx++) {
            State state = states.get(idx);
            stateDescriptions[idx] = describeState(state);
            stateOwners[idx] = state != null ? state.getOwner() : null;
        }

        String[] componentDescriptions = new String[component.size()];
        String[] componentOwners = new String[component.size()];
        int[][] componentConnections = new int[component.size()][];
        for (int idx = 0; idx < component.size(); idx++) {
            Component c = component.get(idx);
            componentDescriptions[idx] = describeComponent(c);
            componentOwners[idx] = c != null ? c.getOwner() : null;
            State[] connected = c.getConnectedStates();
            if (connected == null) {
                componentConnections[idx] = new int[0];
            } else {
                int[] connectionIds = new int[connected.length];
                for (int sIdx = 0; sIdx < connected.length; sIdx++) {
                    State state = connected[sIdx];
                    connectionIds[sIdx] = state != null ? state.getId() : -1;
                }
                componentConnections[idx] = connectionIds;
            }
        }

        return new SubSystemDebugSnapshot(
                matrixCopy,
                rhsCopy,
                stateDescriptions,
                stateOwners,
                componentDescriptions,
                componentOwners,
                componentConnections,
                singularMatrix
        );
    }

    private String describeState(State state) {
        if (state == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('#').append(state.getId()).append(' ').append(state.getClass().getSimpleName());
        String owner = state.getOwner();
        if (owner != null && !owner.isEmpty()) {
            builder.append(" [").append(owner).append(']');
        }
        if (state instanceof VoltageState) {
            builder.append(String.format(" %.4fV", ((VoltageState) state).getVoltage()));
        }
        return builder.toString();
    }

    private String describeComponent(Component component) {
        if (component == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder(component.getClass().getSimpleName());
        String owner = component.getOwner();
        if (owner != null && !owner.isEmpty()) {
            builder.append(" [").append(owner).append(']');
        }
        return builder.toString();
    }

    public void addToA(State a, State b, double v) {
        if (a == null || b == null)
            return;
        A[a.getId()][b.getId()] += v;
    }

    public void addToI(State s, double v) {
        if (s == null) return;
        Idata[s.getId()] = v;
    }

    public void step() {
        stepCalc();
        stepFlush();
    }

    public void drainMnaMetrics(MnaStepMetricsAccumulator accumulator) {
        if (singularMatrixCountSinceLastDrain != 0) {
            accumulator.addSingular(singularMatrixCountSinceLastDrain);
            singularMatrixCountSinceLastDrain = 0;
        }
        if (inversionCountSinceLastDrain != 0) {
            accumulator.addInversions(
                    inversionCountSinceLastDrain,
                    inversionTotalNanosecondsSinceLastDrain,
                    inversionMaximumNanosecondsSinceLastDrain
            );
            inversionCountSinceLastDrain = 0;
            inversionTotalNanosecondsSinceLastDrain = 0L;
            inversionMaximumNanosecondsSinceLastDrain = 0L;
        }
    }

    public void stepCalc() {
        if (!matrixValid) {
            generateMatrix();
        }

        if (!singularMatrix) {
            for (int y = 0; y < stateCount; y++) {
                Idata[y] = 0;
            }
            for (ISubSystemProcessI p : processI) {
                p.simProcessI(this);
            }

            for (int idx2 = 0; idx2 < stateCount; idx2++) {
                DD stack = DD.ZERO;
                DD[] inverseRow = AInvdata[idx2];
                for (int idx = 0; idx < stateCount; idx++) {
                    stack = stack.add(inverseRow[idx].multiply(Idata[idx]));
                }
                XtempData[idx2] = stack.doubleValue();
            }
        }
    }

    public double solve(State pin) {
        if (!matrixValid) {
            generateMatrix();
        }

        if (!singularMatrix) {
            for (int y = 0; y < stateCount; y++) {
                Idata[y] = 0;
            }
            for (ISubSystemProcessI p : processI) {
                p.simProcessI(this);
            }

            int idx2 = pin.getId();
            DD stack = DD.ZERO;
            DD[] inverseRow = AInvdata[idx2];
            for (int idx = 0; idx < stateCount; idx++) {
                stack = stack.add(inverseRow[idx].multiply(Idata[idx]));
            }
            return stack.doubleValue();
        }
        return 0;
    }

    private static double[][] copyMatrix(double[][] source) {
        if (source == null) {
            return new double[0][0];
        }
        double[][] copy = new double[source.length][];
        for (int idx = 0; idx < source.length; idx++) {
            copy[idx] = source[idx].clone();
        }
        return copy;
    }

    private static DD[][] invertMatrix(double[][] matrix) {
        int size = matrix.length;
        DD[][] augmented = new DD[size][size * 2];

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                augmented[row][col] = DD.of(matrix[row][col]);
                augmented[row][size + col] = row == col ? DD.ONE : DD.ZERO;
            }
        }

        for (int pivotColumn = 0; pivotColumn < size; pivotColumn++) {
            int pivotRow = pivotColumn;
            double pivotMagnitude = 0.0;
            for (int row = pivotColumn; row < size; row++) {
                double magnitude = augmented[row][pivotColumn].abs().doubleValue();
                if (magnitude > pivotMagnitude) {
                    pivotMagnitude = magnitude;
                    pivotRow = row;
                }
            }

            if (pivotMagnitude == 0.0) {
                throw new IllegalStateException("Matrix is singular");
            }

            if (pivotRow != pivotColumn) {
                DD[] swap = augmented[pivotColumn];
                augmented[pivotColumn] = augmented[pivotRow];
                augmented[pivotRow] = swap;
            }

            DD pivot = augmented[pivotColumn][pivotColumn];
            for (int col = pivotColumn; col < size * 2; col++) {
                augmented[pivotColumn][col] = augmented[pivotColumn][col].divide(pivot);
            }
            augmented[pivotColumn][pivotColumn] = DD.ONE;

            for (int row = 0; row < size; row++) {
                if (row == pivotColumn) {
                    continue;
                }

                DD factor = augmented[row][pivotColumn];
                if (factor.isZero()) {
                    continue;
                }

                for (int col = pivotColumn; col < size * 2; col++) {
                    augmented[row][col] = augmented[row][col].subtract(factor.multiply(augmented[pivotColumn][col]));
                }
                augmented[row][pivotColumn] = DD.ZERO;
            }
        }

        DD[][] inverse = new DD[size][size];
        for (int row = 0; row < size; row++) {
            System.arraycopy(augmented[row], size, inverse[row], 0, size);
        }
        return inverse;
    }

    public void stepFlush() {
        if (!singularMatrix) {
            for (int idx = 0; idx < stateCount; idx++) {
                statesTab[idx].state = XtempData[idx];

            }
        } else {
            for (int idx = 0; idx < stateCount; idx++) {
                statesTab[idx].state = 0;
            }
        }

        for (ISubSystemProcessFlush p : processF) {
            p.simProcessFlush();
        }
    }

    public static void main(String[] args) {
//		SubSystem s = new SubSystem(null, 0.1);
//		VoltageState n1, n2;
//		VoltageSource u1;
//		Resistor r1, r2;
//
//		s.addState(n1 = new VoltageState());
//		s.addState(n2 = new VoltageState());
//
//		//s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));
//
//		s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
//		s.addComponent((r2 = new Resistor()).setR(20).connectTo(n2, null));
//
//		s.step();
//		s.step();

        SubSystem s = new SubSystem(null, 0.1);
        VoltageState n1, n2;
        CurrentSource cs1;
        Resistor r1;

        s.addState(n1 = new VoltageState());

        s.addComponent((cs1 = new CurrentSource("cs1")).setCurrent(0.01).connectTo(n1, null));
        s.addComponent((r1 = new Resistor()).setResistance(10).connectTo(n1, null));

        s.step();

        Eln.logger.info("R: U = " + r1.getVoltage() + ", I = " + r1.getCurrent());
        Eln.logger.info("CS: U = " + cs1.getVoltage());
    }

    public boolean containe(State state) {
        return states.contains(state);
    }

    public void setX(State s, double value) {
        s.state = value;
    }

    public double getX(State s) {
        return s.state;
    }

    public double getXSafe(State bPin) {
        return bPin == null ? 0 : getX(bPin);
    }

    public boolean breakSystem() {
        if (breaked) return false;
        while (!breakDestructor.isEmpty()) {
            breakDestructor.pop().destruct();
        }

        for (Component c : component) {
            c.quitSubSystem();
        }
        for (State s : states) {
            s.quitSubSystem();
        }

        if (root != null) {
            for (Component c : component) {
                c.returnToRootSystem(root);
            }
            for (State s : states) {
                s.returnToRootSystem(root);
            }
        }
        root.systems.remove(this);

        invalidate();

        breaked = true;
        return true;
    }

    public void addProcess(ISubSystemProcessFlush p) {
        processF.add(p);
    }

    public void removeProcess(ISubSystemProcessFlush p) {
        processF.remove(p);
    }

    public double getDt() {
        return dt;
    }

    static public class Thevenin {
        public double resistance, voltage;

        public boolean isHighImpedance() {
            return resistance > 1e8;
        }
    }

    public Thevenin getTh(State d, VoltageSource voltageSource) {
        Thevenin thevenin = new Thevenin();
        double originalVoltage = d.state;

        double testVoltage = originalVoltage + 5;
        voltageSource.setVoltage(testVoltage);
        double testCurrent = solve(voltageSource.getCurrentState());

        voltageSource.setVoltage(originalVoltage);
        double originalCurrent = solve(voltageSource.getCurrentState());

        double theveninResistance = (testVoltage - originalVoltage) / (originalCurrent - testCurrent);
        double theveninVoltage;
        if (theveninResistance > 10000000000000000000.0 || theveninResistance < 0) {
            theveninVoltage = 0;
            theveninResistance = 10000000000000000000.0;
        } else {
            theveninVoltage = testVoltage + theveninResistance * testCurrent;
        }
        voltageSource.setVoltage(originalVoltage);

        thevenin.resistance = theveninResistance;
        thevenin.voltage = theveninVoltage;

        if(Double.isNaN(thevenin.voltage)) {
            thevenin.voltage = originalVoltage;
            thevenin.resistance = MnaConst.highImpedance;
        }
        if (Double.isNaN(thevenin.resistance)) {
            thevenin.voltage = originalVoltage;
            thevenin.resistance = MnaConst.highImpedance;
        }

        return thevenin;
    }

    public String toString() {
        String str = "";
        for (Component c: component) {
            if (c != null)
                str += c.toString();
        }
        return str;
    }

    public int componentSize() {
        return component.size();
    }
}
