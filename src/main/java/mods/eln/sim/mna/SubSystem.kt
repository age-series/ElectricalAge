package mods.eln.sim.mna

import mods.eln.Eln
import mods.eln.debug.DebugType
import mods.eln.misc.Profiler
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Delay
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.IDestructor
import mods.eln.sim.mna.misc.ISubSystemProcessFlush
import mods.eln.sim.mna.misc.ISubSystemProcessI
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.QRDecomposition
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.SingularValueDecomposition

import java.util.ArrayList
import java.util.LinkedList

class SubSystem(root: RootSystem?, dt: Double) {
    var component = ArrayList<Component>()
    var states: MutableList<State> = ArrayList()
    var breakDestructor = LinkedList<IDestructor>()
    var interSystemConnectivity = ArrayList<SubSystem>()
    internal var processI = ArrayList<ISubSystemProcessI>()
    internal var statesTab: Array<State?>? = null

    var root: RootSystem? = null
        internal set

    var dt: Double = 0.toDouble()
        internal set
    internal var matrixValid = false

    internal var stateCount: Int = 0
    internal var A: RealMatrix? = null
    //RealMatrix I;
    internal var singularMatrix: Boolean = false

    internal var AInvdata: Array<DoubleArray>? = null
    internal var Idata: DoubleArray? = null

    internal var XtempData: DoubleArray? = null

    internal var breaked = false

    internal var processF = ArrayList<ISubSystemProcessFlush>()

    init {
        this.dt = dt
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

    /*public void removeAll() {
		for (Component c : component) {
			c.disconnectFromSubSystem();
		}
		for (State s : states) {
			s.disconnectFromSubSystem();
		}
		invalidate();
	}*/

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

    //double[][] getDataRef()

    fun generateMatrix() {
        stateCount = states.size

        val p = Profiler()
        p.add("Inversse with $stateCount state : ")

        A = MatrixUtils.createRealMatrix(stateCount, stateCount)

        //Adata = ((Array2DRowRealMatrix) A).getDataRef();
        // X = MatrixUtils.createRealMatrix(stateCount, 1); Xdata =
        // ((Array2DRowRealMatrix)X).getDataRef();
        //I = MatrixUtils.createRealMatrix(stateCount, 1);
        //Idata = ((Array2DRowRealMatrix) I).getDataRef();
        Idata = DoubleArray(stateCount)
        XtempData = DoubleArray(stateCount)
        run {
            var idx = 0
            for (s in states) {
                s.id = idx++
            }
        }

        for (c in component) {
            c.applyTo(this)
        }

        //	org.apache.commons.math3.linear.

        val svd = SingularValueDecomposition(A)
        // Broken or large numbers are bad. Inverses are typically pretty ill-conditioned, but we're looking for egregious ones.
        // For every order of magnitude from 10^n, we get n more digits of error (apparently).
        // Some people say 10e8 or 10e12 may be more realistic? Not sure I want that much error. I set 10e4 for now.
        // Doubles have (roughly?) 15 decimal digits of precision. I can see 4 of them go away without too much trouble.
        if(svd.conditionNumber.isNaN() or (svd.conditionNumber > 10e4)) {
            System.out.println("Condition of Matrix: " + svd.conditionNumber)
            for (row in A!!.data) {
                for (i in row) {
                    System.out.print("" + i + ", ")
                }
                System.out.println()
            }
        }

        try {
            //FieldLUDecomposition QRDecomposition  LUDecomposition RRQRDecomposition
            val Ainv = QRDecomposition(A).solver.inverse
            AInvdata = Ainv.data
            singularMatrix = false
        } catch (e: Exception) {
            singularMatrix = true
            if (stateCount > 1) {
                var idx = 0
                idx++
                System.out.println("//////////SingularMatrix////////////")
                for (row in A!!.data) {
                    for (i in row) {
                        System.out.print("" + i + ", ")
                    }
                    System.out.println()
                }
            }
        }

        statesTab = arrayOfNulls(stateCount)
        statesTab = states.toTypedArray()

        matrixValid = true

        p.stop()
        System.out.println(p.toString())
    }

    fun addToA(a: State?, b: State?, v: Double) {
        if (a == null || b == null)
            return
        A?.addToEntry(a.id, b.id, v)
        //Adata[a.getId()][b.getId()] += v;
    }

    fun addToI(s: State?, v: Double) {
        if (s == null) return
        Idata?.set(s.id, v)
        //Idata[s.getId()][0] += v;
    }

    /*
	 * public void pushX(){
	 *
	 * }
	 */
    /*
	 * public void popX(){
	 *
	 * }
	 */

    fun step() {
        stepCalc()
        stepFlush()
    }

    fun stepCalc() {
        val profiler = Profiler()
        //	profiler.add("generateMatrix");
        if (!matrixValid) {
            generateMatrix()
        }

        if (!singularMatrix) {
            //profiler.add("generateMatrix");
            for (y in 0 until stateCount) {
                Idata?.set(y, 0.0)
            }
            //profiler.add("generateMatrix");
            for (p in processI) {
                p.simProcessI(this)
            }
            //	profiler.add("generateMatrix");

            for (idx2 in 0 until stateCount) {
                var stack = 0.0
                for (idx in 0 until stateCount) {
                    stack += AInvdata!![idx2][idx] * Idata?.get(idx)!!
                }
                XtempData!![idx2] = stack
            }
            //Xtemp = Ainv.multiply(I);
        }
        profiler.stop()
        //Utils.println(profiler);
    }

    fun solve(pin: State): Double {
        //Profiler profiler = new Profiler();
        if (!matrixValid) {
            generateMatrix()
        }

        if (!singularMatrix) {
            for (y in 0 until stateCount) {
                Idata?.set(y, 0.0)
            }
            for (p in processI) {
                p.simProcessI(this)
            }

            val idx2 = pin.id
            var stack = 0.0
            for (idx in 0 until stateCount) {
                stack += AInvdata!![idx2][idx] * Idata!![idx]
            }
            return stack
        }
        return 0.0
    }

    //RealMatrix Xtemp;
    fun stepFlush() {
        if (!singularMatrix) {
            for (idx in 0 until stateCount) {
                //statesTab[idx].state = Xtemp.getEntry(idx, 0);
                statesTab?.get(idx)?.state = XtempData!![idx]

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

    fun containe(state: State): Boolean {
        return states.contains(state)
    }

    fun setX(s: State, value: Double) {
        s.state = value
    }

    fun getX(s: State): Double {
        return s.state
    }

    fun getXSafe(bPin: State?): Double {
        return bPin?.let { getX(it) } ?: 0.0
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

    class Th {
        var R: Double = 0.toDouble()
        var U: Double = 0.toDouble()

        val isHighImpedance: Boolean
            get() = R > 1e8
    }

    fun getTh(d: State, voltageSource: VoltageSource): Th {
        val th = Th()
        val originalU = d.state

        if (originalU.isNaN()) {
            System.out.println("originalU NaN!")
        }

        val aU = originalU
        voltageSource.u = aU
        val aI = solve(voltageSource.currentState)

        val bU = originalU * 0.95
        voltageSource.u = bU
        val bI = solve(voltageSource.currentState)

        var Rth = (aU - bU) / (bI - aI)
        if (Rth.isNaN()) Rth = MnaConst.noImpedance
        //System.out.println("au ai bu bi r" + aU + " " + aI + " " + bU + " " + bI + " " + Rth)

        val Uth: Double
        //if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
        if (Rth > 10000000000000000000.0 || Rth < 0) {
            Uth = 0.0
            Rth = 10000000000000000000.0
        } else {
            Uth = originalU + Rth * aI
        }
        voltageSource.u = Uth // originanlU

        th.R = Rth
        th.U = Uth
        //System.out.println("" + th.R + " " + th.U)
        return th
    }

    override fun toString(): String {
        var str = ""
        for (c in component) {
            str += c.toString()
        }
        //str = component.size() + "components";
        return str
    }

    fun componentSize(): Int {
        return component.size
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            //		SubSystem s = new SubSystem(null, 0.1);
            //		VoltageState n1, n2;
            //		VoltageSource u1;
            //		Resistor r1, r2;
            //
            //		s.addState(n1 = new VoltageState());
            //		s.addState(n2 = new VoltageState());
            //
            //		//s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));
            //
            //		s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
            //		s.addComponent((r2 = new Resistor()).setR(20).connectTo(n2, null));
            //
            //		s.step();
            //		s.step();

            val s = SubSystem(null, 0.1)
            val n1 = VoltageState()
            val n2 = VoltageState()
            val n3 = VoltageState()
            val u1: VoltageSource
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

            u1 = VoltageSource("")
            u1.u = 1.0
            u1.connectTo(n1, null)
            s.addComponent(u1)

            s.addComponent(r1)
            s.addComponent(d1)
            s.addComponent(r2)

            for (idx in 0..99) {
                s.step()
            }

            System.out.println("END")

            s.step()
            s.step()
            System.out.println(s)
            s.step()
        }
    }
}
