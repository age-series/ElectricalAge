package mods.eln.sim.mna.active

import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.passive.Component
import mods.eln.sim.mna.passive.Resistor
import mods.eln.sim.mna.misc.IAbstractor
import mods.eln.sim.mna.misc.IDestructor
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState

class InterSystemAbstraction(internal var root: RootSystem, internal var interSystemResistor: Resistor) : IAbstractor, IDestructor {

    internal var aNewState: VoltageState
    internal var aNewResistor: Resistor
    internal var aNewDelay: DelayInterSystem2
    internal var bNewState: VoltageState
    internal var bNewResistor: Resistor
    internal var bNewDelay: DelayInterSystem2
    internal var thevnaCalc: DelayInterSystem2.ThevnaCalculator

    internal var aState: State = interSystemResistor.aPin ?: throw Exception("aPin on InterSystemResistor cannot be null!")
    internal var bState: State = interSystemResistor.bPin ?: throw Exception("bPin on InterSystemResistor cannot be null!")
    override var abstractorSubSystem: SubSystem = aState.subSystem ?: throw Exception("subsystem connected to aPin cannot be null!")
    internal var bSystem: SubSystem = bState.subSystem ?: throw Exception("subsystem connected to bPin cannot be null!")

    init {
        abstractorSubSystem.interSystemConnectivity.add(bSystem)
        bSystem.interSystemConnectivity.add(abstractorSubSystem)

        aNewState = VoltageState()
        aNewResistor = Resistor()
        aNewDelay = DelayInterSystem2()
        bNewState = VoltageState()
        bNewResistor = Resistor()
        bNewDelay = DelayInterSystem2()

        aNewResistor.connectGhostTo(aState, aNewState)
        aNewDelay.connectTo(aNewState, null)
        bNewResistor.connectGhostTo(bState, bNewState)
        bNewDelay.connectTo(bNewState, null)

        calibrate()

        abstractorSubSystem.addComponent(aNewResistor)
        abstractorSubSystem.addState(aNewState)
        abstractorSubSystem.addComponent(aNewDelay)
        bSystem.addComponent(bNewResistor)
        bSystem.addState(bNewState)
        bSystem.addComponent(bNewDelay)

        abstractorSubSystem.breakDestructor.add(this)
        bSystem.breakDestructor.add(this)

        interSystemResistor.abstractedBy = this

        thevnaCalc = DelayInterSystem2.ThevnaCalculator(aNewDelay, bNewDelay)
        root.addProcess(thevnaCalc)
    }

    internal fun calibrate() {
        val u = (aState.state + bState.state) / 2
        aNewDelay.u = u
        bNewDelay.u = u

        val r = interSystemResistor.r / 2
        aNewResistor.r = r
        bNewResistor.r = r
    }

    override fun dirty(component: Component) {
        calibrate()
    }

    override fun destruct() {
        abstractorSubSystem.breakDestructor.remove(this)
        abstractorSubSystem.removeComponent(aNewDelay)
        abstractorSubSystem.removeComponent(aNewResistor)
        abstractorSubSystem.removeState(aNewState)
        bSystem.breakDestructor.remove(this)
        bSystem.removeComponent(bNewDelay)
        bSystem.removeComponent(bNewResistor)
        bSystem.removeState(bNewState)

        root.removeProcess(thevnaCalc)

        interSystemResistor.abstractedBy = null

        abstractorSubSystem.component.add(interSystemResistor)
    }
}
