package mods.eln.sim.mna.passive

open class InterSystem : Resistor() {

    class InterSystemDestructor {
        internal var done = false
    }

    override fun canBeReplacedByInterSystem(): Boolean {
        return true
    }
}
