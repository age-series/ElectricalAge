package mods.eln.solver

/**
 * Constant value - a constant that never changes
 * @param value the constant value (double)
 */
class Constant internal constructor(private val value: Double) : IValue {

    override fun getValue(): Double {
        return value
    }

    override fun toString(): String {
        return "Constant($value)"
    }
}
