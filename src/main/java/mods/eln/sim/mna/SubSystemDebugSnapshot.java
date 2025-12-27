package mods.eln.sim.mna;

/**
 * Immutable snapshot of a subsystem's MNA state used for debugging/logging.
 */
public class SubSystemDebugSnapshot {

    private final double[][] conductanceMatrix;
    private final double[] rhsVector;
    private final String[] stateLabels;
    private final String[] componentLabels;
    private final int[][] componentConnections;
    private final boolean singular;

    public SubSystemDebugSnapshot(
        double[][] conductanceMatrix,
        double[] rhsVector,
        String[] stateLabels,
        String[] componentLabels,
        int[][] componentConnections,
        boolean singular
    ) {
        this.conductanceMatrix = conductanceMatrix;
        this.rhsVector = rhsVector;
        this.stateLabels = stateLabels;
        this.componentLabels = componentLabels;
        this.componentConnections = componentConnections;
        this.singular = singular;
    }

    public double[][] getConductanceMatrix() {
        return conductanceMatrix;
    }

    public double[] getRhsVector() {
        return rhsVector;
    }

    public String[] getStateLabels() {
        return stateLabels;
    }

    public String[] getComponentLabels() {
        return componentLabels;
    }

    public int[][] getComponentConnections() {
        return componentConnections;
    }

    public boolean isSingular() {
        return singular;
    }
}
