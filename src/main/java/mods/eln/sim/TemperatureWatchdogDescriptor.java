package mods.eln.sim;

public interface TemperatureWatchdogDescriptor {
    double getUmax();

    double getUmin();

    double getBreakPropPerVoltOverflow();
}
