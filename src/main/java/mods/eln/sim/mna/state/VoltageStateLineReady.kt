package mods.eln.sim.mna.state

open class VoltageStateLineReady : VoltageState() {

    init {
        name = "VoltageStateLineReady"
    }

    internal var canBeSimplifiedByLine = false

    fun setCanBeSimplifiedByLine(v: Boolean) {
        this.canBeSimplifiedByLine = v
    }

    override fun canBeSimplifiedByLine(): Boolean {
        return canBeSimplifiedByLine
    }
}
