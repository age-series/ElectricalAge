package mods.eln.sim.watchdogs;

import mods.eln.*;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;

public abstract class ValueWatchdog implements IProcess {

    IDestructible destructible;
    double min;
    double max;

    double timeoutReset = 2;

    double timeout = 0;
    boolean boot = true;
    boolean joker = true;

    double rand = Utils.rand(0.5, 1.5);

    @Override
    public void process(double time) {
        if (boot) {
            boot = false;
            timeout = timeoutReset;
        }
        double value = getValue();
        double overflow = Math.max(value - max, min - value);
        if (overflow > 0) {
            if (joker) {
                joker = false;
                overflow = 0;
            }
        } else {
            joker = true;
        }

        timeout -= time * overflow * rand;
        if (timeout > timeoutReset) {
            timeout = timeoutReset;
        }
        if (timeout < 0) {
            Utils.println("%s destroying %s",
                getClass().getName(),
                destructible.describe());
            if (!Eln.debugExplosions)
                destructible.destructImpl();
        }
    }

    public ValueWatchdog set(IDestructible d) {
        this.destructible = d;
        return this;
    }

    abstract double getValue();

    public void disable() {
        this.max = 100000000;
        this.min = -max;
        this.timeoutReset = 10000000;
    }

    public void reset() {
        boot = true;
    }
}
