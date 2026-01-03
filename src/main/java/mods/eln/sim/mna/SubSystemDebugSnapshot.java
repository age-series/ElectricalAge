package mods.eln.sim.mna;

/**
 * Immutable snapshot of a subsystem's MNA state used for debugging/logging.
 */
public class SubSystemDebugSnapshot {

    private final double[][] conductanceMatrix;
    private final double[] rhsVector;
    private final String[] stateLabels;
    private final String[] stateOwners;
    private final String[] componentLabels;
    private final String[] componentOwners;
    private final int[][] componentConnections;
    private final boolean singular;

    public SubSystemDebugSnapshot(
        double[][] conductanceMatrix,
        double[] rhsVector,
        String[] stateLabels,
        String[] stateOwners,
        String[] componentLabels,
        String[] componentOwners,
        int[][] componentConnections,
        boolean singular
    ) {
        this.conductanceMatrix = conductanceMatrix;
        this.rhsVector = rhsVector;
        this.stateLabels = stateLabels;
        this.stateOwners = stateOwners;
        this.componentLabels = componentLabels;
        this.componentOwners = componentOwners;
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

    public String[] getStateOwners() {
        return stateOwners;
    }

    public String[] getComponentLabels() {
        return componentLabels;
    }

    public String[] getComponentOwners() {
        return componentOwners;
    }

    public int[][] getComponentConnections() {
        return componentConnections;
    }

    public boolean isSingular() {
        return singular;
    }
}
