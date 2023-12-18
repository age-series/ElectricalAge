package mods.eln.solver

class Constant internal constructor(private val value: Double) : IValue {
    override fun getValue(): Double {
        return value
    }
}
