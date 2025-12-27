package mods.eln.sim.mna.misc;

public class MnaConst {
    // Note: We used to have ultraImpedance, 1e16, but that caused instability above 5 kV in grid tech.
    public static final double highImpedance = 1e9;
    public static final double pullDown = 1e9;
    public static final double noImpedance = 1e-9;
}
