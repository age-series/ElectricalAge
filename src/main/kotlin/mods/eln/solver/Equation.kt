package mods.eln.solver

import mods.eln.Eln
import mods.eln.misc.INBTTReady
import mods.eln.sim.IProcess
import net.minecraft.nbt.NBTTagCompound
import java.util.*
import kotlin.math.*

class Equation : IValue, INBTTReady {
    var stringList: LinkedList<String?> = LinkedList()

    var nbtList: ArrayList<INBTTReady> = ArrayList()

    var operatorList: HashMap<Int, ArrayList<IOperatorMapper>> = HashMap()

    var separatorList: String = ""

    var iterationLimit: Int = 0
    var symbolList: ArrayList<ISymbole> = ArrayList()

    var root: IValue? = null

    var processList: ArrayList<IProcess> = ArrayList()

    @JvmField
    var operatorCount: Int = 0 // Juste a counter for fun
    @JvmField
    var beepList: ArrayList<Beep> = ArrayList()

    fun setUpDefaultOperatorAndMapper() {
        operatorList.putAll(staticOperatorList)
        separatorList += staticSeparatorList
    }

    fun addMapper(priority: Int, mapper: IOperatorMapper) {
        var list = operatorList[priority]
        if (list == null) {
            list = ArrayList()
            operatorList[priority] = list
        }
        list.add(mapper)
    }

    fun addSymbol(symbolList: ArrayList<ISymbole>?) {
        this.symbolList.addAll(symbolList!!)
    }

    fun preProcess(expressionInput: String) {
        var expression = expressionInput
        var idx: Int
        expression = expression.replace(" ", "")

        stringList.clear()
        beepList.clear()
        val list = LinkedList<Any>()
        var stack = ""
        idx = 0
        while (idx != expression.length) {
            if (separatorList.contains(expression.subSequence(idx, idx + 1))) {
                if (stack !== "") {
                    list.add(stack)
                    stringList.add(stack)
                    stack = ""
                }
                list.add(expression.substring(idx, idx + 1))
            } else {
                stack += expression[idx]
            }

            idx++
        }
        if (stack !== "") {
            list.add(stack)
            stringList.add(stack)
        }

        var depthMax = getDepthMax(list)
        var depth: Int
        // Double str
        run {
            idx = 0
            val i: Iterator<Any> = list.iterator()
            while (i.hasNext()) {
                val o = i.next()
                if (o is String) {
                    val str = o
                    var find = false
                    if (!find) for (s in symbolList) {
                        if (s.getName() == str) {
                            list[idx] = s
                            find = true
                        }
                    }
                    if (!find) try {
                        val value = str.toDouble()
                        list[idx] = Constant(value)
                        find = true
                    } catch (e: NumberFormatException) {
                    }
                    if (!find) {
                        if (str == "PI" || str == "pi") {
                            list[idx] = Constant(Math.PI)
                        }
                    }
                }
                idx++
            }
        }

        var priority = -1

        while (list.size > 1 && iterationLimit != 0) {
            iterationLimit--
            idx = 0
            depth = 0
            val i: Iterator<Any> = list.iterator()
            priority++
            while (i.hasNext()) {
                val o = i.next()
                if (o is String) {
                    val str = o

                    if (operatorList.containsKey(priority)) {
                        val depthDelta = depth - depthMax
                        var resetPriority = false
                        for (mapper in operatorList[priority]!!) {
                            var operator: IOperator?
                            if ((mapper.newOperator(str, depthDelta, list, idx).also { operator = it }) != null) {
                                if (operator is IProcess) processList.add(operator as IProcess)
                                if (operator is INBTTReady) nbtList.add(operator as INBTTReady)
                                if (operator is Beep) beepList.add(operator as Beep)
                                operatorCount += operator!!.redstoneCost
                                resetPriority = true
                                break
                            }
                        }
                        if (resetPriority) {
                            depthMax = getDepthMax(list)
                            priority = -1
                            break
                        }
                    }

                    if (str == "(") depth++
                    if (str == ")") depth--
                }

                idx++
            }
        }

        if (list.size == 1) {
            root = if (list[0] is IValue) {
                list[0] as IValue
            } else null
        }
    }

    fun getDepthMax(list: LinkedList<Any>): Int {
        var depth: Int
        var depthMax: Int
        run {
            depthMax = 0
            depth = 0
            val i: Iterator<Any> = list.iterator()
            while (i.hasNext()) {
                val o = i.next()
                if (o is String) {
                    val str = o
                    if (str == "(") depth++
                    if (str == ")") depth--
                    depthMax = max(depthMax.toDouble(), depth.toDouble()).toInt()
                }
            }
        }
        return depthMax
    }

    override fun getValue(): Double {
        if (root == null) return 0.0
        return root!!.getValue()
    }

    fun getValue(deltaT: Double): Double {
        if (root == null) return 0.0
        for (p in processList) {
            p.process(deltaT)
        }
        return root!!.getValue()
    }

    val isValid: Boolean
        get() = root != null

    class Eguals : OperatorAB() {
        override fun getValue(): Double {
            return if ((a.getValue() > 0.5) == (b.getValue() > 0.5)) 1.0 else 0.0
        }

        override val redstoneCost: Int
            get() = 1
    }

    class NotEguals : OperatorAB() {
        override fun getValue(): Double {
            return if ((a.getValue() > 0.5) != (b.getValue() > 0.5)) 1.0 else 0.0
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Bigger : OperatorAB() {
        override fun getValue(): Double {
            return if (a.getValue() > b.getValue()) 1.0 else 0.0
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Smaller : OperatorAB() {
        override fun getValue(): Double {
            return if (a.getValue() < b.getValue()) 1.0 else 0.0
        }

        override val redstoneCost: Int
            get() = 1
    }

    class And : OperatorAB() {
        override fun getValue(): Double {
            return if (a.getValue() > 0.5 && b.getValue() > 0.5) 1.0 else 0.0
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Or : OperatorAB() {
        override fun getValue(): Double {
            return if (a.getValue() > 0.5 || b.getValue() > 0.5) 1.0 else 0.0
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Add : OperatorAB() {
        override fun getValue(): Double {
            return a.getValue() + b.getValue()
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Sub : OperatorAB() {
        override fun getValue(): Double {
            return a.getValue() - b.getValue()
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Mul : OperatorAB() {
        override fun getValue(): Double {
            return a.getValue() * b.getValue()
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Div : OperatorAB() {
        override fun getValue(): Double {
            return a.getValue() / b.getValue()
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Mod : OperatorAB() {
        override fun getValue(): Double {
            return a.getValue() % b.getValue()
        }

        override val redstoneCost: Int
            get() = 1
    }


    class Inv : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return -a!!.getValue()
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Not : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return 1.0 - a!!.getValue()
        }

        override fun setOperator(values: Array<IValue>) {
            a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Bracket : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return a!!.getValue()
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 0
    }

    class Abs : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return abs(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Sin : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return sin(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Cos : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return cos(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Tan : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return tan(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Asin : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return asin(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Atan : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return atan(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Atan2 : IOperator {
        var y: IValue? = null
        var x: IValue? = null

        override fun getValue(): Double {
            return atan2(y!!.getValue(), x!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.y = values[0]
            this.x = values[1]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Acos : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return acos(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Log : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return ln(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Log10 : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return log10(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Exp : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return exp(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Sqrt : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return sqrt(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }


    class Pow : IOperator {
        var a: IValue? = null
        var b: IValue? = null

        override fun getValue(): Double {
            return a!!.getValue().pow(b!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
            this.b = values[1]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Clamp : IOperator {
        var x: IValue? = null
        var min: IValue? = null
        var max: IValue? = null

        override fun getValue(): Double {
            return max(min!!.getValue(), min(max!!.getValue(), x!!.getValue()))
        }

        override fun setOperator(values: Array<IValue>) {
            this.x = values[0]
            this.min = values[1]
            this.max = values[2]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class Lerp : IOperator {
        var a: IValue? = null
        var b: IValue? = null
        var t: IValue? = null

        /**
         * Linear interpolation: a + (b - a) * t.
         */
        override fun getValue(): Double {
            val av = a!!.getValue()
            return av + (b!!.getValue() - av) * t!!.getValue()
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
            this.b = values[1]
            this.t = values[2]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class Step : IOperator {
        var edge: IValue? = null
        var x: IValue? = null

        /**
         * Hard threshold: 0 if x < edge, else 1.
         */
        override fun getValue(): Double {
            return if (x!!.getValue() < edge!!.getValue()) 0.0 else 1.0
        }

        override fun setOperator(values: Array<IValue>) {
            this.edge = values[0]
            this.x = values[1]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class SmoothStep : IOperator {
        var edge0: IValue? = null
        var edge1: IValue? = null
        var x: IValue? = null

        /**
         * Smooth transition between 0 and 1 with a cubic Hermite curve.
         */
        override fun getValue(): Double {
            val e0 = edge0!!.getValue()
            val e1 = edge1!!.getValue()
            val t = ((x!!.getValue() - e0) / (e1 - e0)).coerceIn(0.0, 1.0)
            return t * t * (3.0 - 2.0 * t)
        }

        override fun setOperator(values: Array<IValue>) {
            this.edge0 = values[0]
            this.edge1 = values[1]
            this.x = values[2]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class Round : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return round(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Floor : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return floor(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Ceil : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return ceil(a!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Fract : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            val v = a!!.getValue()
            return v - floor(v)
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Sign : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            val v = a!!.getValue()
            return if (v > 0.0) 1.0 else if (v < 0.0) -1.0 else 0.0
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Beep : IOperator {
        var value: IValue? = null
        var condition: IValue? = null
        var active: Boolean = false
        var pitch: Double = 1.0
        var volume: Double = 0.5

        /**
         * Pass-through value; updates active/pitch/volume based on condition and value.
         */
        override fun getValue(): Double {
            val v = value!!.getValue()
            active = condition!!.getValue() > 0.5
            pitch = 1.0
            volume = 1.0
            return v
        }

        override fun setOperator(values: Array<IValue>) {
            this.value = values[0]
            this.condition = values[1]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Saturate : IOperator {
        var a: IValue? = null

        override fun getValue(): Double {
            return a!!.getValue().coerceIn(0.0, 1.0)
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class DeadZone : IOperator {
        var x: IValue? = null
        var min: IValue? = null
        var max: IValue? = null

        /**
         * Zero output inside [min, max], passthrough otherwise.
         */
        override fun getValue(): Double {
            val v = x!!.getValue()
            return if (v >= min!!.getValue() && v <= max!!.getValue()) 0.0 else v
        }

        override fun setOperator(values: Array<IValue>) {
            this.x = values[0]
            this.min = values[1]
            this.max = values[2]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Pulse : IOperator {
        var edge0: IValue? = null
        var edge1: IValue? = null
        var x: IValue? = null

        /**
         * Returns 1 when x is within [edge0, edge1], else 0.
         */
        override fun getValue(): Double {
            val v = x!!.getValue()
            return if (v >= edge0!!.getValue() && v <= edge1!!.getValue()) 1.0 else 0.0
        }

        override fun setOperator(values: Array<IValue>) {
            this.edge0 = values[0]
            this.edge1 = values[1]
            this.x = values[2]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Square : IOperator {
        var phase: IValue? = null
        var duty: IValue? = null

        /**
         * Square wave from phase (0..1) and duty (0..1).
         */
        override fun getValue(): Double {
            val t = phase!!.getValue()
            val fract = t - floor(t)
            return if (fract < duty!!.getValue()) 1.0 else 0.0
        }

        override fun setOperator(values: Array<IValue>) {
            this.phase = values[0]
            this.duty = values[1]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Saw : IOperator {
        var phase: IValue? = null

        /**
         * Sawtooth wave: fract(phase).
         */
        override fun getValue(): Double {
            val t = phase!!.getValue()
            return t - floor(t)
        }

        override fun setOperator(values: Array<IValue>) {
            this.phase = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class Triangle : IOperator {
        var phase: IValue? = null

        /**
         * Triangle wave in [0,1].
         */
        override fun getValue(): Double {
            val t = phase!!.getValue()
            val f = t - floor(t)
            return 1.0 - abs(2.0 * f - 1.0)
        }

        override fun setOperator(values: Array<IValue>) {
            this.phase = values[0]
        }

        override val redstoneCost: Int
            get() = 1
    }

    class GenSquare : IOperator, IProcess {
        var freq: IValue? = null
        var duty: IValue? = null
        private var phase: Double = 0.0

        /**
         * Time-domain square generator: phase += freq * dt; output uses duty in [0,1].
         */
        override fun process(time: Double) {
            phase += freq!!.getValue() * time
        }

        override fun getValue(): Double {
            val f = phase - floor(phase)
            return if (f < duty!!.getValue()) 1.0 else 0.0
        }

        override fun setOperator(values: Array<IValue>) {
            this.freq = values[0]
            this.duty = values[1]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class GenSaw : IOperator, IProcess {
        var freq: IValue? = null
        private var phase: Double = 0.0

        /**
         * Time-domain saw generator: phase += freq * dt; output is fract(phase).
         */
        override fun process(time: Double) {
            phase += freq!!.getValue() * time
        }

        override fun getValue(): Double {
            return phase - floor(phase)
        }

        override fun setOperator(values: Array<IValue>) {
            this.freq = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class GenTriangle : IOperator, IProcess {
        var freq: IValue? = null
        private var phase: Double = 0.0

        /**
         * Time-domain triangle generator: phase += freq * dt; output in [0,1].
         */
        override fun process(time: Double) {
            phase += freq!!.getValue() * time
        }

        override fun getValue(): Double {
            val f = phase - floor(phase)
            return 1.0 - abs(2.0 * f - 1.0)
        }

        override fun setOperator(values: Array<IValue>) {
            this.freq = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Impulse : IOperator {
        var k: IValue? = null
        var x: IValue? = null

        /**
         * Smooth impulse response for 0..1 input: k*x*exp(1 - k*x).
         */
        override fun getValue(): Double {
            val xv = x!!.getValue()
            if (xv <= 0.0) return 0.0
            val kv = k!!.getValue()
            return kv * xv * exp(1.0 - kv * xv)
        }

        override fun setOperator(values: Array<IValue>) {
            this.k = values[0]
            this.x = values[1]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class LowPass : IOperator, IProcess {
        var input: IValue? = null
        var alpha: IValue? = null
        private var state: Double = 0.0

        /**
         * Single-pole low-pass: state += (input - state) * alpha.
         */
        override fun process(time: Double) {
            val a = alpha!!.getValue().coerceIn(0.0, 1.0)
            state += (input!!.getValue() - state) * a
        }

        override fun getValue(): Double {
            return state
        }

        override fun setOperator(values: Array<IValue>) {
            this.input = values[0]
            this.alpha = values[1]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class HighPass : IOperator, IProcess {
        var input: IValue? = null
        var alpha: IValue? = null
        private var lowState: Double = 0.0
        private var output: Double = 0.0

        /**
         * High-pass using internal low-pass state: output = input - lowState.
         */
        override fun process(time: Double) {
            val a = alpha!!.getValue().coerceIn(0.0, 1.0)
            val inputValue = input!!.getValue()
            lowState += (inputValue - lowState) * a
            output = inputValue - lowState
        }

        override fun getValue(): Double {
            return output
        }

        override fun setOperator(values: Array<IValue>) {
            this.input = values[0]
            this.alpha = values[1]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Hysteresis : IOperator {
        var input: IValue? = null
        var low: IValue? = null
        var high: IValue? = null
        private var state: Double = 0.0

        /**
         * Latches high when input >= high, low when input <= low.
         */
        override fun getValue(): Double {
            val v = input!!.getValue()
            if (v >= high!!.getValue()) state = 1.0
            if (v <= low!!.getValue()) state = 0.0
            return state
        }

        override fun setOperator(values: Array<IValue>) {
            this.input = values[0]
            this.low = values[1]
            this.high = values[2]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Debounce : IOperator, IProcess {
        var input: IValue? = null
        var tau: IValue? = null
        private var state: Double = 0.0
        private var counter: Double = 0.0
        private var lastHigh: Boolean? = null

        /**
         * Only changes output when input has been stable for tau seconds.
         */
        override fun process(time: Double) {
            val high = input!!.getValue() > 0.5
            if (lastHigh == null || high != lastHigh) {
                lastHigh = high
                counter = 0.0
            } else {
                counter += time
            }
            if (counter >= tau!!.getValue()) {
                state = if (high) 1.0 else 0.0
            }
        }

        override fun getValue(): Double {
            return state
        }

        override fun setOperator(values: Array<IValue>) {
            this.input = values[0]
            this.tau = values[1]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class Ramp : IOperator, INBTTReady, IProcess {
        var counter: Double = 0.0

        var periode: IValue? = null

        override fun getValue(): Double {
            return counter
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            counter = nbt.getDouble(str + "counter")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setDouble(str + "counter", counter)
        }

        override fun process(time: Double) {
            val p = periode!!.getValue()
            counter += time / p
            if (counter >= 1.0) counter -= 1.0
            if (counter >= 1.0) counter = 0.0
        }

        override fun setOperator(values: Array<IValue>) {
            this.periode = values[0]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class Integrator : IOperator, INBTTReady, IProcess {
        var counter: Double = 0.0
        var probe: IValue? = null
        var reset: IValue? = null

        override fun getValue(): Double {
            return counter
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            counter = nbt.getDouble(str + "counter")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setDouble(str + "counter", counter)
        }

        override fun process(time: Double) {
            counter += time * probe!!.getValue()
            if (reset!!.getValue() > 0.5) counter = 0.0
        }

        override fun setOperator(values: Array<IValue>) {
            this.probe = values[0]
            this.reset = values[1]
        }

        override val redstoneCost: Int
            get() = 4
    }

    class IntegratorMinMax : IOperator, INBTTReady, IProcess {
        var counter: Double = 0.0

        var probe: IValue? = null
        var min: IValue? = null
        var max: IValue? = null

        override fun getValue(): Double {
            return counter
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            counter = nbt.getDouble(str + "counter")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setDouble(str + "counter", counter)
        }

        override fun process(time: Double) {
            counter += time * probe!!.getValue()
            if (counter < min!!.getValue()) counter = min!!.getValue()
            if (counter > max!!.getValue()) counter = max!!.getValue()
        }

        override fun setOperator(values: Array<IValue>) {
            this.probe = values[0]
            this.min = values[1]
            this.max = values[2]
        }

        override val redstoneCost: Int
            get() = 4
    }

    class Derivator : IOperator, INBTTReady, IProcess {
        var old: Double = 0.0
        var lvalue: Double = 0.0
        var probe: IValue? = null

        override fun getValue(): Double {
            return lvalue
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            old = nbt.getDouble(str + "old")
            lvalue = nbt.getDouble(str + "value")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setDouble(str + "old", old)
            nbt.setDouble(str + "value", lvalue)
        }

        override fun process(time: Double) {
            val next = probe!!.getValue()
            lvalue = (next - old) / time
            old = next
        }

        override fun setOperator(values: Array<IValue>) {
            this.probe = values[0]
        }

        override val redstoneCost: Int
            get() = 3
    }

    open class Pid : IOperator, INBTTReady, IProcess {
        var iStack: Double = 0.0
        var oldError: Double = 0.0
        var dValue: Double = 0.0

        var target: IValue? = null
        var hit: IValue? = null
        var p: IValue? = null
        var i: IValue? = null
        var d: IValue? = null

        override fun getValue(): Double {
            val value = oldError * p!!.getValue() + iStack + dValue * d!!.getValue()
            return value
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            iStack = nbt.getDouble(str + "iStack")
            oldError = nbt.getDouble(str + "oldError")
            dValue = nbt.getDouble(str + "dValue")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setDouble(str + "iStack", iStack)
            nbt.setDouble(str + "oldError", oldError)
            nbt.setDouble(str + "dValue", dValue)
        }

        override fun process(time: Double) {
            val error = target!!.getValue() - hit!!.getValue()
            iStack += error * time * i!!.getValue()
            dValue = (error - oldError) / time

            if (iStack > 1) iStack = 1.0
            if (iStack < 0) iStack = 0.0
            oldError = error
        }

        override fun setOperator(values: Array<IValue>) {
            this.target = values[0]
            this.hit = values[1]
            this.p = values[2]
            this.i = values[3]
            this.d = values[4]
        }

        override val redstoneCost: Int
            get() = 12
    }

    class PidMinMax : Pid() {
        var min: IValue? = null
        var max: IValue? = null

        override fun getValue(): Double {
            return max(min!!.getValue(), min(max!!.getValue(), super.getValue()))
        }

        override fun setOperator(values: Array<IValue>) {
            super.setOperator(values)
            min = values[5]
            max = values[6]
        }

        override val redstoneCost: Int
            get() = super.redstoneCost + 2
    }

    class Min : IOperator {
        var a: IValue? = null
        var b: IValue? = null

        override fun getValue(): Double {
            return min(a!!.getValue(), b!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[1]
            this.b = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Max : IOperator {
        var a: IValue? = null
        var b: IValue? = null

        override fun getValue(): Double {
            return max(a!!.getValue(), b!!.getValue())
        }

        override fun setOperator(values: Array<IValue>) {
            this.a = values[1]
            this.b = values[0]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class Rs : IOperator, INBTTReady {
        var state: Boolean = false

        var set: IValue? = null
        var reset: IValue? = null

        override fun getValue(): Double {
            if (set!!.getValue() > 0.6) state = true
            if (reset!!.getValue() > 0.6) state = false
            return if (state) 1.0 else 0.0
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            state = nbt.getBoolean(str + "state")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setBoolean(str + "state", state)
        }

        override fun setOperator(values: Array<IValue>) {
            this.set = values[1]
            this.reset = values[0]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class RC : IOperator, INBTTReady, IProcess {
        var state: Double = 0.0

        var tao: IValue? = null
        var input: IValue? = null

        override fun getValue(): Double {
            return state
        }

        override fun process(time: Double) {
            val tao = max(time, tao!!.getValue())
            state += (input!!.getValue() - state) / tao * time
        }

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            state = nbt.getDouble(str + "state")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setDouble(str + "state", state)
        }

        override fun setOperator(values: Array<IValue>) {
            this.input = values[1]
            this.tao = values[0]
        }

        override val redstoneCost: Int
            get() = 3
    }

    class If : IOperator {
        var condition: IValue? = null
        var thenValue: IValue? = null
        var elseValue: IValue? = null

        override fun getValue(): Double {
            return if (condition!!.getValue() > 0.5) thenValue!!.getValue() else elseValue!!.getValue()
        }

        override fun setOperator(values: Array<IValue>) {
            this.condition = values[0]
            this.thenValue = values[1]
            this.elseValue = values[2]
        }

        override val redstoneCost: Int
            get() = 2
    }

    class BatteryCharge : IOperator {
        var eMax: Double
        var probe: IValue? = null

        init {
            val uFq = Eln.instance.batteryVoltageFunctionTable
            val dq = 0.001
            var q = 0.0
            eMax = 0.0
            while (q <= 1.0) {
                eMax += uFq.getValue(q) * dq
                q += dq
            }
        }

        override fun setOperator(values: Array<IValue>) {
            this.probe = values[0]
        }

        override val redstoneCost: Int
            get() = 8

        override fun getValue(): Double {
            val uFq = Eln.instance.batteryVoltageFunctionTable
            val probeU = probe!!.getValue()
            if (probeU > 1.5) return 1.0
            var q = 0.0
            val dq = 0.001
            var e = 0.0
            var u: Double

            while ((uFq.getValue(q).also { u = it }) < probeU) {
                e += u * dq
                q += dq
            }

            return e / eMax
        }
    }

    /**
     * Rescale input values.
     *
     *
     * scale(X, in0, in1, out0, out1) = (X - in0) / (in1 - in0) * (out1 - out0) + out0
     */
    class Scale : IOperator {
        private var x: IValue? = null
        private var in0: IValue? = null
        private var in1: IValue? = null
        private var out0: IValue? = null
        private var out1: IValue? = null

        override fun setOperator(values: Array<IValue>) {
            x = values[0]
            in0 = values[1]
            in1 = values[2]
            out0 = values[3]
            out1 = values[4]
        }

        override val redstoneCost: Int
            get() = 5

        override fun getValue(): Double {
            val xv = x!!.getValue()
            val in0v = in0!!.getValue()
            val in1v = in1!!.getValue()
            val out0v = out0!!.getValue()
            val out1v = out1!!.getValue()

            return (xv - in0v) / (in1v - in0v) * (out1v - out0v) + out0v
        }
    }

    fun isSymboleUsed(iSymbole: ISymbole): Boolean {
        if (!isValid) return false
        return stringList.contains(iSymbole.getName())
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        if (!isValid) return
        var idx = 0
        for (o in nbtList) {
            o.readFromNBT(nbt, str + idx)
            idx++
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        if (!isValid) return
        var idx = 0
        for (o in nbtList) {
            o.writeToNBT(nbt, str + idx)
            idx++
        }
    }

    companion object {
        val staticOperatorList: HashMap<Int, ArrayList<IOperatorMapper>> = HashMap()
        const val staticSeparatorList: String = "+-*&|/^,()<>=!"

        init {
            var priority = 0
            run {
                val list = ArrayList<IOperatorMapper>()
                list.add(OperatorMapperFunc("min", 2, Min::class.java))
                list.add(OperatorMapperFunc("max", 2, Max::class.java))
                list.add(OperatorMapperFunc("sin", 1, Sin::class.java))
                list.add(OperatorMapperFunc("cos", 1, Cos::class.java))
                list.add(OperatorMapperFunc("tan", 1, Tan::class.java))
                list.add(OperatorMapperFunc("asin", 1, Asin::class.java))
                list.add(OperatorMapperFunc("acos", 1, Acos::class.java))
                list.add(OperatorMapperFunc("atan", 1, Atan::class.java))
                list.add(OperatorMapperFunc("atan2", 2, Atan2::class.java))
                list.add(OperatorMapperFunc("abs", 1, Abs::class.java))
                list.add(OperatorMapperFunc("log", 1, Log::class.java))
                list.add(OperatorMapperFunc("log10", 1, Log10::class.java))
                list.add(OperatorMapperFunc("exp", 1, Exp::class.java))
                list.add(OperatorMapperFunc("sqrt", 1, Sqrt::class.java))
                list.add(OperatorMapperFunc("ramp", 1, Ramp::class.java))
                list.add(OperatorMapperFunc("integrate", 2, Integrator::class.java))
                list.add(OperatorMapperFunc("integrate", 3, IntegratorMinMax::class.java))
                list.add(OperatorMapperFunc("derivate", 1, Derivator::class.java))
                list.add(OperatorMapperFunc("pow", 2, Pow::class.java))
                list.add(OperatorMapperFunc("pid", 5, Pid::class.java))
                list.add(OperatorMapperFunc("pid", 7, PidMinMax::class.java))
                list.add(OperatorMapperFunc("batteryCharge", 1, BatteryCharge::class.java))
                list.add(OperatorMapperFunc("rs", 2, Rs::class.java))
                list.add(OperatorMapperFunc("rc", 2, RC::class.java))
                list.add(OperatorMapperFunc("if", 3, If::class.java))
                list.add(OperatorMapperFunc("scale", 5, Scale::class.java))
                list.add(OperatorMapperFunc("clamp", 3, Clamp::class.java))
                list.add(OperatorMapperFunc("lerp", 3, Lerp::class.java))
                list.add(OperatorMapperFunc("step", 2, Step::class.java))
                list.add(OperatorMapperFunc("smoothstep", 3, SmoothStep::class.java))
                list.add(OperatorMapperFunc("round", 1, Round::class.java))
                list.add(OperatorMapperFunc("floor", 1, Floor::class.java))
                list.add(OperatorMapperFunc("ceil", 1, Ceil::class.java))
                list.add(OperatorMapperFunc("fract", 1, Fract::class.java))
                list.add(OperatorMapperFunc("sign", 1, Sign::class.java))
                list.add(OperatorMapperFunc("beep", 2, Beep::class.java))
                list.add(OperatorMapperFunc("saturate", 1, Saturate::class.java))
                list.add(OperatorMapperFunc("deadzone", 3, DeadZone::class.java))
                list.add(OperatorMapperFunc("pulse", 3, Pulse::class.java))
                list.add(OperatorMapperFunc("square", 2, Square::class.java))
                list.add(OperatorMapperFunc("saw", 1, Saw::class.java))
                list.add(OperatorMapperFunc("triangle", 1, Triangle::class.java))
                list.add(OperatorMapperFunc("gensquare", 2, GenSquare::class.java))
                list.add(OperatorMapperFunc("gensaw", 1, GenSaw::class.java))
                list.add(OperatorMapperFunc("gentriangle", 1, GenTriangle::class.java))
                list.add(OperatorMapperFunc("impulse", 2, Impulse::class.java))
                list.add(OperatorMapperFunc("lowpass", 2, LowPass::class.java))
                list.add(OperatorMapperFunc("highpass", 2, HighPass::class.java))
                list.add(OperatorMapperFunc("hysteresis", 3, Hysteresis::class.java))
                list.add(OperatorMapperFunc("debounce", 2, Debounce::class.java))
                // Added mod here becuase % wasn't working. $%^&@#!
                list.add(OperatorMapperFunc("mod", 2, Mod::class.java))
                list.add(OperatorMapperBracket())
                staticOperatorList.put(priority++, list)
            }
            run {
                val list = ArrayList<IOperatorMapper>()
                staticOperatorList.put(priority++, list)
            }
            run {
                val list = ArrayList<IOperatorMapper>()
                list.add(OperatorMapperA("-", Inv::class.java))
                list.add(OperatorMapperA("!", Not::class.java))
                list.add(OperatorMapperAB("*", Mul::class.java))
                list.add(OperatorMapperAB("/", Div::class.java))
                // I had mod here but it's not working. FML.
                staticOperatorList.put(priority++, list)
            }
            run {
                val list = ArrayList<IOperatorMapper>()
                list.add(OperatorMapperAB("+", Add::class.java))
                list.add(OperatorMapperAB("-", Sub::class.java))
                staticOperatorList.put(priority++, list)
            }
            run {
                val list = ArrayList<IOperatorMapper>()
                list.add(OperatorMapperAB(">", Bigger::class.java))
                list.add(OperatorMapperAB("<", Smaller::class.java))
                staticOperatorList.put(priority++, list)
            }
            run {
                val list = ArrayList<IOperatorMapper>()
                list.add(OperatorMapperAB("=", Eguals::class.java))
                list.add(OperatorMapperAB("^", NotEguals::class.java))
                list.add(OperatorMapperAB("&", And::class.java))
                list.add(OperatorMapperAB("|", Or::class.java))
                staticOperatorList.put(priority++, list)
            }
        }
    }
}
