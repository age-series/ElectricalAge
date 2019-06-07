package mods.eln.sim.mna.process;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;

public class TransformerInterSystemProcess implements IRootSystemPreStepProcess {
    private State aState, bState;
    private VoltageSource aVoltgeSource, bVoltgeSource;

    private double ratio = 1;

    public TransformerInterSystemProcess(State aState, State bState, VoltageSource aVoltgeSource, VoltageSource bVoltgeSource) {
        this.aState = aState;
        this.bState = bState;
        this.aVoltgeSource = aVoltgeSource;
        this.bVoltgeSource = bVoltgeSource;
    }

    /**
     * rootSystemPreStepProcess
     *
     * This code is called from the MNA every tick for every isolated DC/DC converter, and needs to figure out
     * <insert smart comment here>
     *
     * It likely needs to be re-written to perform better. Currently, it causes a capacitance problem, likely because
     * of the call to getTh not taking into account the B side voltage. Not sure exactly yet...
     *
     */
    @Override
    public void rootSystemPreStepProcess() {
        // these two lines below probably are up for replacement.
        Th a = getTh(aState, aVoltgeSource);
        Th b = getTh(bState, bVoltgeSource);

        double aU = (a.U * b.R + ratio * b.U * a.R) / (b.R + ratio * ratio * a.R);
        if (Double.isNaN(aU)) {
            aU = 0;
        }

        aVoltgeSource.setU(aU);
        bVoltgeSource.setU(aU * ratio);
    }

    @Deprecated
    static class Th {
        double R, U;
    }

    /**
     * getTh
     *
     * @param d VoltageState
     * @param voltageSource a voltage source
     * @return instance of class Th (a pair equivelant)
     */
    @Deprecated
    private Th getTh(State d, VoltageSource voltageSource) {
        Th th = new Th();
        double originalU = d.state;

        double aU = 10;
        voltageSource.setU(aU);
        double aI = d.getSubSystem().solve(voltageSource.getCurrentState());

        double bU = 5;
        voltageSource.setU(bU);
        double bI = d.getSubSystem().solve(voltageSource.getCurrentState());

        th.R = (aU - bU) / (bI - aI);

        if (th.R > 10000000000000000000.0 || th.R < 0) {
            th.U = 0;
            th.R = 10000000000000000000.0;
        } else {
            th.U = aU + th.R * aI;
        }
        voltageSource.setU(originalU);

        /*
        Experimentally, lowering the resistance between two isolated transformers causes a bit of a capacitance bug.
        I can't fix it for some reason by increasing the resistnace here, but this function does some things that are
        not very good, and it should be combined with the function above the static Th class above.

        Before you try to comb the source code for anything usable about how this class works, I already checked all of
        the source code, and it was never re-written or commented. It just "came to be".

        It should be noted that lowering the resistance here does have some interesting effects, but none of the ones I
        was looking for. Namely, it caused a few things to asplode that were fine otherwise.
         */

        // Eln.dp.println(DebugType.MNA, "TISP Resistance: " + th.R);
        return th;
    }

    /**
     * setRatio - sets the ratio of the voltage, with regards to the side A voltage. (Ua * ratio = Ub)
     *
     * @param ratio the new ratio to use
     */
    public void setRatio(double ratio) {
        this.ratio = ratio;
    }
}
