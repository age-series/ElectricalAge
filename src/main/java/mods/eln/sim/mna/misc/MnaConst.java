package mods.eln.sim.mna.misc;

public class MnaConst {
    // Note: We used have ultraImpedance, but it causes instability above 5 kV in grid tech. DO NOT use for anything
    // that may be in contact with high voltage.
    public static final double ultraImpedance = 1e16;
    public static final double highImpedance = 1e9;
    public static final double pullDown = 1e9;
    public static final double noImpedance = 1e-9;
}
