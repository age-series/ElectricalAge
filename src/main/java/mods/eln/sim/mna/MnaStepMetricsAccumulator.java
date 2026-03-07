package mods.eln.sim.mna;

public class MnaStepMetricsAccumulator {
    private int subSystemCount;
    private int inversionCount;
    private int singularMatrixCount;
    private long inversionTotalNanoseconds;
    private long inversionMaxNanoseconds;

    public void reset() {
        subSystemCount = 0;
        inversionCount = 0;
        singularMatrixCount = 0;
        inversionTotalNanoseconds = 0L;
        inversionMaxNanoseconds = 0L;
    }

    public void setSubSystemCount(int value) {
        subSystemCount = value;
    }

    public void addInversion(long nanoseconds) {
        inversionCount++;
        inversionTotalNanoseconds += nanoseconds;
        if (nanoseconds > inversionMaxNanoseconds) {
            inversionMaxNanoseconds = nanoseconds;
        }
    }

    public void addInversions(int count, long totalNanoseconds, long maxNanoseconds) {
        if (count <= 0) {
            return;
        }
        inversionCount += count;
        inversionTotalNanoseconds += totalNanoseconds;
        if (maxNanoseconds > inversionMaxNanoseconds) {
            inversionMaxNanoseconds = maxNanoseconds;
        }
    }

    public void addSingular(int count) {
        singularMatrixCount += count;
    }

    public void add(MnaStepMetricsAccumulator other) {
        inversionCount += other.inversionCount;
        singularMatrixCount += other.singularMatrixCount;
        inversionTotalNanoseconds += other.inversionTotalNanoseconds;
        if (other.inversionMaxNanoseconds > inversionMaxNanoseconds) {
            inversionMaxNanoseconds = other.inversionMaxNanoseconds;
        }
    }

    public int getSubSystemCount() {
        return subSystemCount;
    }

    public int getInversionCount() {
        return inversionCount;
    }

    public int getSingularMatrixCount() {
        return singularMatrixCount;
    }

    public long getInversionMaxNanoseconds() {
        return inversionMaxNanoseconds;
    }

    public double getInversionAverageNanoseconds() {
        if (inversionCount <= 0) {
            return 0.0;
        }
        return (double) inversionTotalNanoseconds / inversionCount;
    }
}
