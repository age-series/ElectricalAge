package mods.eln.sim.mna.misc;

public class MnaConst {
    // Note: Avoid using ultraImpedance in grid tech, as it causes instability above 5 kV. Use highImpedance instead.
    public static final double ultraImpedance = 1e16;
    public static final double highImpedance = 1e9;
    public static final double pullDown = 1e9;
    public static final double noImpedance = 1e-9;
}
