package mods.eln.sixnode.electricalbreaker;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalBreakerCutProcess implements IProcess, INBTTReady {
    private static final double STARTUP_INRUSH_TRIP_TIME_SECONDS = 1.0;
    private static final double STARTUP_INRUSH_MULTIPLIER = 2.0;
    private static final double OVERLOAD_HEAT_THRESHOLD = STARTUP_INRUSH_TRIP_TIME_SECONDS *
        (STARTUP_INRUSH_MULTIPLIER * STARTUP_INRUSH_MULTIPLIER - 1.0);

    ElectricalBreakerElement breaker;

    double T = 0;

    public ElectricalBreakerCutProcess(ElectricalBreakerElement breaker) {
        this.breaker = breaker;
    }

    public void resetTripAccumulator() {
        T = 0.0;
    }

    @Override
    public void process(double time) {
        double U = breaker.getMonitoredVoltage();
        double I = breaker.getTripCurrent();
        double currentLimit = breaker.currantMax;

        if (currentLimit > 0.0) {
            double overloadRatio = I / currentLimit;
            if (overloadRatio > 1.0) {
                T += (overloadRatio * overloadRatio - 1.0) * time;
            } else {
                T = Math.max(0.0, T - time);
            }
        } else {
            T = 0.0;
        }

        if (U >= breaker.voltageMax || U < breaker.voltageMin || T > OVERLOAD_HEAT_THRESHOLD) {
            breaker.tripSwitch();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        T = nbt.getFloat(str + "T");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setFloat(str + "T", (float) T);
    }
}
