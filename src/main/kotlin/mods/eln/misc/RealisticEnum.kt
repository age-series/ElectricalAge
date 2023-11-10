package mods.eln.misc

enum class RealisticEnum(val color: String) {
    /**
     * May have a caveat or three as to why it's not perfect, but as good as it's going to get with the simulator
     *
     * E.g. Current cable - resistance not impacted by temperature and resistance high, but otherwise correct
     */
    REALISTIC("§a"),

    /**
     * An ideal component (also possibly with a caveat or two)
     *
     * E.g. Voltage source - ours are ideal but have a resistor on them to prevent singular matrices
     */
    IDEAL("§9"),

    /**
     * A component that doesn't really behave properly for one reason or another
     *
     * E.g. Inductor ... it's really bad.
     */
    UNREALISTIC("§c"),

    /**
     * A component that doesn't exist, so we can't really be sure...
     *
     * E.g. Experimental Teleporter
     */
    FANTASY("§d")
}