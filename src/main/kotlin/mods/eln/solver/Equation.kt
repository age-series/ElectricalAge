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

    fun preProcess(exp: String) {
        var exp = exp
        var idx: Int
        exp = exp.replace(" ", "")

        stringList.clear()
        val list = LinkedList<Any>()
        var stack = ""
        idx = 0
        while (idx != exp.length) {
            if (separatorList.contains(exp.subSequence(idx, idx + 1))) {
                if (stack !== "") {
                    list.add(stack)
                    stringList.add(stack)
                    stack = ""
                }
                list.add(exp.substring(idx, idx + 1))
            } else {
                stack += exp[idx]
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
            var a: IValue
            var b: IValue
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
                list.add(OperatorMapperFunc("asin", 1, Asin::class.java))
                list.add(OperatorMapperFunc("acos", 1, Acos::class.java))
                list.add(OperatorMapperFunc("abs", 1, Abs::class.java))
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
