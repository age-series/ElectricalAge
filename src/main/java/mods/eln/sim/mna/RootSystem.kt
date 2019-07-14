package mods.eln.sim.mna

import mods.eln.misc.Profiler
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.misc.ISubSystemProcessFlush
import mods.eln.sim.mna.process.InterSystemAbstraction
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState

import java.util.*

class RootSystem(internal var dt: Double, internal var interSystemOverSampling: Int) {

    var systems = ArrayList<SubSystem>()

    var addComponents: MutableSet<Component> = HashSet()
    var addStates = HashSet<State>()

    internal var processF = ArrayList<ISubSystemProcessFlush>()

    internal var processPre = ArrayList<IRootSystemPreStepProcess>()

    val subSystemCount: Int
        get() = systems.size

    fun addComponent(c: Component) {
        addComponents.add(c)
        c.onAddToRootSystem()

        for (s in c.getConnectedStates()) {
            if (s == null) continue
            if (s.subSystem != null) {
                breakSystems(s.subSystem!!)
            }
        }
    }

    fun removeComponent(c: Component) {
        val system = c.getSubSystem()
        if (system != null) {
            breakSystems(system)
        }

        addComponents.remove(c)
        c.onRemovefromRootSystem()
    }

    fun addState(s: State) {
        for (c in s.getConnectedComponentsNotAbstracted().clone() as ArrayList<Component>) {
            if (c.getSubSystem() != null)
                breakSystems(c.getSubSystem()!!)
        }
        addStates.add(s)
    }

    fun removeState(s: State) {
        val system = s.subSystem
        if (system != null) {
            breakSystems(system)
        }
        addStates.remove(s)
    }

    fun generate() {
        if (!addComponents.isEmpty() || !addStates.isEmpty()) {
            val p = Profiler()
            p.add("*** Generate ***")
            generateLine()
            generateSystems()
            generateInterSystems()

            var stateCnt = 0
            var componentCnt = 0

            for (s in systems) {
                stateCnt += s.states.size
                componentCnt += s.component.size
            }
            p.stop()
            System.out.println("Ran generate in " + p.time + " μs. States: $stateCnt Components: $componentCnt")
        }
    }

    private fun isValidForLine(s: State): Boolean {
        if (!s.canBeSimplifiedByLine()) return false
        val sc = s.getConnectedComponentsNotAbstracted()
        if (sc.size != 2) return false
        for (c in sc) {
            if (false == c is Resistor) {
                return false
            }
        }

        return true
    }

    private fun generateLine() {
        val stateScope = HashSet<State>()
        for (s in addStates) {
            if (isValidForLine(s)) {
                stateScope.add(s)
            }
        }

        while (!stateScope.isEmpty()) {
            val sRoot = stateScope.iterator().next()

            var sPtr = sRoot
            var rPtr = sPtr.getConnectedComponentsNotAbstracted()[0] as Resistor
            while (true) {
                for (c in sPtr.getConnectedComponentsNotAbstracted()) {
                    if (c !== rPtr) {
                        rPtr = c as Resistor
                        break
                    }
                }
                var sNext: State? = null

                if (sPtr !== rPtr.aPin)
                    sNext = rPtr.aPin
                else if (sPtr !== rPtr.bPin) sNext = rPtr.bPin

                if (sNext == null || sNext === sRoot || stateScope.contains(sNext) == false) break

                sPtr = sNext
            }

            val lineStates = LinkedList<State>()
            val lineResistors = LinkedList<Resistor>()

            lineResistors.add(rPtr)
            while (true) {
                lineStates.add(sPtr)
                stateScope.remove(sPtr)
                for (c in sPtr.getConnectedComponentsNotAbstracted()) {
                    if (c !== rPtr) {
                        rPtr = c as Resistor
                        break
                    }
                }
                lineResistors.add(rPtr)

                var sNext: State? = null

                if (sPtr !== rPtr.aPin)
                    sNext = rPtr.aPin
                else if (sPtr !== rPtr.bPin) sNext = rPtr.bPin

                if (sNext == null || stateScope.contains(sNext) == false) break

                sPtr = sNext
            }

            if (lineResistors.first === lineResistors.last) {
                lineResistors.pop()
                lineStates.pop()
            }

            Line.newLine(this, lineResistors, lineStates)
        }
    }

    private fun generateSystems() {
        val firstState = LinkedList<State>()
        for (s in addStates) {
            if (s.mustBeFarFromInterSystem()) {
                firstState.add(s)
            }
        }

        for (s in firstState) {
            if (s.subSystem == null) {
                buildSubSystem(s)
            }
        }

        while (!addStates.isEmpty()) {
            val root = addStates.iterator().next()
            buildSubSystem(root)
        }
    }

    fun generateInterSystems() {
        val ic = addComponents.iterator()
        while (ic.hasNext()) {
            val c = ic.next()

            if (!c.canBeReplacedByInterSystem()) {
                System.out.println("" + c + ": " + "\tInterSystemError!")
            } else {
                System.out.println(c)
            }

            if (c is Delay) {
                val r = c as Resistor
                // If a pin is disconnected, we can't be intersystem
                if (r.aPin == null || r.bPin == null) continue

                InterSystemAbstraction(this, r)
                ic.remove()
            }
        }
    }

    fun step() {
        val profiler = Profiler()
        profiler.add("Generate")
        generate()
        profiler.add("interSystem")
        for (idx in 0 until interSystemOverSampling) {
            for (p in processPre) {
                p.rootSystemPreStepProcess()
            }
        }

        /*	for (SubSystem s : systems) {
			for (State state : s.states) {
				Utils.print(state.state + " ");
			}
		}
		Utils.println("");*/

        profiler.add("stepCalc")
        for (s in systems) {
            s.stepCalc()
        }
        profiler.add("stepFlush")
        for (s in systems) {
            s.stepFlush()
        }
        profiler.add("simProcessFlush")
        for (p in processF) {
            p.simProcessFlush()
        }

        /*	for (SubSystem s : systems) {
			for (State state : s.states) {
				Utils.print(state.state + " ");
			}
		}
		Utils.println("");*/

        profiler.stop()
        //Utils.println(profiler);
    }

    private fun buildSubSystem(root: State) {
        val componentSet = HashSet<Component>()
        val stateSet = HashSet<State>()

        val roots = LinkedList<State>()
        roots.push(root)
        buildSubSystem(roots, componentSet, stateSet)

        addComponents.removeAll(componentSet)
        addStates.removeAll(stateSet)

        val subSystem = SubSystem(this, dt)
        System.out.println(stateSet)
        System.out.println(componentSet)
        subSystem.addState(stateSet)
        subSystem.addComponent(componentSet)

        systems.add(subSystem)
    }

    private fun buildSubSystem(roots: LinkedList<State>, componentSet: MutableSet<Component>, stateSet: MutableSet<State>) {
        val privateSystem = roots.first.isPrivateSubSystem

        while (!roots.isEmpty()) {
            val sExplored = roots.pollFirst()
            stateSet.add(sExplored)

            for (c in sExplored!!.getConnectedComponentsNotAbstracted()) {
                if (privateSystem && roots.size + stateSet.size > maxSubSystemSize && c.canBeReplacedByInterSystem()) {
                    continue
                }
                if (componentSet.contains(c)) continue
                var noGo = false
                for (sNext in c.getConnectedStates()) {
                    if (sNext == null) continue
                    if (sNext.subSystem != null) {
                        noGo = true
                        break
                    }
                    if (sNext.isPrivateSubSystem != privateSystem) {
                        noGo = true
                        break
                    }
                }

                if (noGo) continue
                componentSet.add(c)
                for (sNext in c.getConnectedStates()) {
                    if (sNext == null) continue
                    if (stateSet.contains(sNext)) continue
                    roots.addLast(sNext)
                }
            }
        }
    }

    private fun findSubSystemWith(state: State): SubSystem? {
        for (s in systems) {
            if (s.containe(state)) return s
        }

        return null
    }

    fun breakSystems(sub: SubSystem) {
        if (sub.breakSystem()) {
            for (s in sub.interSystemConnectivity) {
                breakSystems(s)
            }
        }
    }

    fun addProcess(p: ISubSystemProcessFlush) {
        processF.add(p)
    }

    fun removeProcess(p: ISubSystemProcessFlush) {
        processF.remove(p)
    }

    fun addProcess(p: IRootSystemPreStepProcess) {
        System.out.println(p)
        processPre.add(p)
    }

    fun removeProcess(p: IRootSystemPreStepProcess) {
        processPre.remove(p)
    }

    fun isRegistred(load: ElectricalLoad): Boolean {
        return load.subSystem != null || addStates.contains(load)
    }

    companion object {

        internal val maxSubSystemSize = 100

        @JvmStatic
        fun main(args: Array<String>) {
            val s = RootSystem(0.1, 1)

            val n1= VoltageState("n1")
            val n2= VoltageState("n2")
            val u1 = VoltageSource("u1")
            val r1 = Resistor("r1")
            val r2 = Resistor("r2")

            u1.u = 1.0
            r1.r = 10.0
            r2.r = 20.0

            r1.connectTo(n1, n2)
            r2.connectTo(n2, null)
            u1.connectTo(n1, null)

            s.addState(n1)
            s.addState(n2)
            s.addComponent(u1)
            s.addComponent(r1)
            s.addComponent(r2)

            val n11 = VoltageState("n11")
            val n12 = VoltageState("n12")
            val u11 = VoltageSource("u11")
            val r11 = Resistor("r11")
            val r12 = Resistor("r12")
            val r13 = Resistor("r13")

            u11.u = 1.0
            r11.r = 10.0
            r12.r = 30.0
            r13.r = 30.0

            u11.connectTo(n11, null)
            r11.connectTo(n11, n12)
            r12.connectTo(n12, null)
            r13.connectTo(n12, null)

            s.addState(n11)
            s.addState(n12)
            s.addComponent(u11)
            s.addComponent(r11)
            s.addComponent(r12)


            val is1 = InterSystem()
            is1.r = 10.0
            is1.connectTo(n2, n12)

            s.addComponent(is1)

            for (i in 0..1) {
                s.step()
            }

            s.addComponent(r13)

            for (i in 0..1) {
                s.step()
            }

            s.step()

            for (d in s.systems) {
                System.out.println("system: " + d)
            }
        }
    }
}

//TODO: garbadge collector
//TODO: ghost suprresion
