package mods.eln.sim.mna

import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.debug.Profiler
import mods.eln.sim.mna.passive.Component
import mods.eln.sim.mna.passive.Delay
import mods.eln.sim.mna.passive.Resistor
import mods.eln.sim.mna.passive.VoltageSource
import mods.eln.sim.mna.misc.IDestructor
import mods.eln.sim.mna.misc.ISubSystemProcessFlush
import mods.eln.sim.mna.misc.ISubSystemProcessI
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.QRDecomposition
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.SingularValueDecomposition

import java.util.ArrayList
import java.util.LinkedList

class SubSystem(root: RootSystem?, val dt: Double) {
    var component = ArrayList<Component>()
    var states: MutableList<State> = ArrayList()
    var breakDestructor = LinkedList<IDestructor>()
    var interSystemConnectivity = ArrayList<SubSystem>()
    private var processI = ArrayList<ISubSystemProcessI>()
    private var statesTab: Array<State?>? = null

    var root: RootSystem? = null
        private set

    private var matrixValid = false

    private var stateCount: Int = 0
    private var a: RealMatrix? = null
    private var singularMatrix: Boolean = false

    private var aInverseData: Array<DoubleArray>? = null
    private var iData: DoubleArray? = null
    private var xTempData: DoubleArray? = null

    private var breaked = false

    private var processF = ArrayList<ISubSystemProcessFlush>()

    private val matrixProfiler = Profiler()

    init {
        this.root = root
    }

    fun invalidate() {
        matrixValid = false
    }

    fun addComponent(c: Component) {
        component.add(c)
        c.addedTo(this)
        invalidate()
    }

    fun addState(s: State) {
        states.add(s)
        s.addedTo(this)
        invalidate()
    }

    fun removeComponent(c: Component) {
        component.remove(c)
        c.quitSubSystem()
        invalidate()
    }

    fun removeState(s: State) {
        states.remove(s)
        s.quitSubSystem()
        invalidate()
    }

    fun removeProcess(p: ISubSystemProcessI) {
        processI.remove(p)
        invalidate()
    }

    fun addComponent(i: Iterable<Component>) {
        for (c in i) {
            addComponent(c)
        }
    }

    fun addState(i: Iterable<State>) {
        for (s in i) {
            addState(s)
        }
    }

    fun addProcess(p: ISubSystemProcessI) {
        processI.add(p)
    }

    private fun generateMatrix() {
        stateCount = states.size

        matrixProfiler.reset()
        matrixProfiler.add("Inversse with $stateCount state")

        a = MatrixUtils.createRealMatrix(stateCount, stateCount)

        iData = DoubleArray(stateCount)
        xTempData = DoubleArray(stateCount)
        run {
            var idx = 0
            for (s in states) {
                s.id = idx++
            }
        }

        for (c in component) {
            c.applyTo(this)
        }

        val svd = SingularValueDecomposition(a)
        // Broken or large numbers are bad. Inverses are typically pretty ill-conditioned, but we're looking for egregious ones.
        // For every order of magnitude from 10^n, we get n more digits of error (apparently).
        // Some people say 10e8 or 10e12 may be more realistic? Not sure I want that much error. I set 10e4 for now.
        // Doubles have (roughly?) 15 decimal digits of precision. I can see 4 of them go away without too much trouble.
        if(svd.conditionNumber.isNaN() or (svd.conditionNumber > 10e4)) {
            DP.println(DPType.MNA, "Condition of Matrix: " + svd.conditionNumber)
            for (row in a!!.data) {
                for (i in row) {
                    DP.print(DPType.MNA, "$i, ")
                }
                DP.println(DPType.MNA, "")
            }
        }

        try {
            val aInverse = QRDecomposition(a).solver.inverse
            aInverseData = aInverse.data
            singularMatrix = false
        } catch (e: org.apache.commons.math3.linear.SingularMatrixException) {
            singularMatrix = true
            if (stateCount > 1) {
                DP.println(DPType.MNA,"//////////SingularMatrix////////////")
                for (row in a!!.data) {
                    for (i in row) {
                        DP.print(DPType.MNA, "$i, ")
                    }
                    DP.println(DPType.MNA, "")
                }
            }
        }

        statesTab = arrayOfNulls(stateCount)
        statesTab = states.toTypedArray()

        matrixValid = true

        matrixProfiler.stop()
        DP.println(DPType.MNA, matrixProfiler.toString())
    }

    fun addToA(a: State?, b: State?, v: Double) {
        if (a == null || b == null)
            return
        this.a?.addToEntry(a.id, b.id, v)
    }

    fun addToI(s: State?, v: Double) {
        if (s == null) return
        iData?.set(s.id, v)
    }

    fun step() {
        stepCalc()
        stepFlush()
    }

    fun stepCalc() {
        if (!matrixValid) {
            generateMatrix()
        }
        if (!singularMatrix) {
            for (y in 0 until stateCount) {
                iData?.set(y, 0.0)
            }
            for (p in processI) {
                p.simProcessI(this)
            }
            for (idx2 in 0 until stateCount) {
                var stack = 0.0
                for (idx in 0 until stateCount) {
                    stack += aInverseData!![idx2][idx] * iData?.get(idx)!!
                }
                xTempData!![idx2] = stack
            }
        }
    }

    fun solve(pin: State): Double {
        if (!matrixValid) {
            generateMatrix()
        }

        if (!singularMatrix) {
            for (y in 0 until stateCount) {
                iData?.set(y, 0.0)
            }
            for (p in processI) {
                p.simProcessI(this)
            }

            val idx2 = pin.id
            var stack = 0.0
            for (idx in 0 until stateCount) {
                stack += aInverseData!![idx2][idx] * iData!![idx]
            }
            return stack
        }
        return 0.0
    }

    fun stepFlush() {
        if (!singularMatrix) {
            for (idx in 0 until stateCount) {
                statesTab?.get(idx)?.state = xTempData!![idx]
            }
        } else {
            for (idx in 0 until stateCount) {
                statesTab?.get(idx)?.state = 0.0
            }
        }

        for (p in processF) {
            p.simProcessFlush()
        }
    }

    fun contains(state: State): Boolean {
        return states.contains(state)
    }

    fun getXSafe(bPin: State?): Double {
        return bPin?.state ?: 0.0
    }

    fun breakSystem(): Boolean {
        if (breaked) return false
        while (!breakDestructor.isEmpty()) {
            breakDestructor.pop().destruct()
        }

        for (c in component) {
            c.quitSubSystem()
        }
        for (s in states) {
            s.quitSubSystem()
        }

        if (root != null) {
            for (c in component) {
                c.returnToRootSystem(root)
            }
            for (s in states) {
                s.returnToRootSystem(root!!)
            }
        }
        root!!.systems.remove(this)

        invalidate()

        breaked = true
        return true
    }

    fun addProcess(p: ISubSystemProcessFlush) {
        processF.add(p)
    }

    fun removeProcess(p: ISubSystemProcessFlush) {
        processF.remove(p)
    }

    override fun toString(): String {
        var str = ""
        for (c in component) {
            str += c.toString()
        }
        return str
    }

    fun matrixSize(): Int {
        return component.size
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val s = SubSystem(null, 0.1)
            val n1 = VoltageState()
            val n2 = VoltageState()
            val n3 = VoltageState()
            val u1 = VoltageSource("")
            val r1 = Resistor()
            r1.r = 10.0
            r1.connectTo(n1, n2)
            val r2 = Resistor()
            r2.r = 10.0
            r2.connectTo(n3, null)
            val d1 = Delay()
            d1.set(1.0)
            d1.connectTo(n2, n3)

            s.addState(n1)
            s.addState(n2)
            s.addState(n3)

            u1.u = 1.0
            u1.connectTo(n1, null)
            s.addComponent(u1)

            s.addComponent(r1)
            s.addComponent(d1)
            s.addComponent(r2)

            val p = Profiler()

            p.add("run")

            // as it turns out, the first step where we build the matrix is what takes the longest time.
            s.step()

            p.add("first")

            for (idx in 0..49) {
                s.step()
            }
            r1.r = 20.0
            for (idx in 0..49) {
                s.step()
            }
            p.stop()

            DP.println(DPType.CONSOLE, "$p ${p.list}")
            DP.println(DPType.CONSOLE, "$s")

            DP.println(DPType.CONSOLE, "first step finished in ${(p.list[1].nano - p.list.first.nano) / 1000}ps")
            DP.println(DPType.CONSOLE, "other steps finished in ${(p.list.last.nano - p.list[1].nano) / 100 / 1000}ps")
        }
    }
}
