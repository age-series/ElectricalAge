package mods.eln.solver

class ConstSymbole(private val name: String, private val value: Double) : ISymbole {
    override fun getValue(): Double {
        return value
    }

    override fun getName(): String {
        return name
    }
}
