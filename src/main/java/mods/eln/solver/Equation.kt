package mods.eln.solver

import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.misc.INBTTReady
import mods.eln.sim.core.IProcess
import net.minecraft.nbt.NBTTagCompound
import java.util.*

class Equation : IValue, INBTTReady {

    private var stringList = LinkedList<String>()

    private var nbtList = ArrayList<INBTTReady>()
    internal var operatorList: HashMap<Int, ArrayList<IOperatorMapper>> = HashMap()
    private var separatorList: String = ""

    private var iterationLimit: Int = 0
    private var symbolList: ArrayList<ISymbol> = ArrayList()

    internal var root: IValue? = null

    private var processList = ArrayList<IProcess>()

    var operatorCount: Int = 0
        internal set // Juste a counter for fun

    val isValid: Boolean
        get() = root != null

    fun setUpDefaultOperatorAndMapper() {
        operatorList.putAll(staticOperatorList)
        separatorList += staticSeparatorList
    }

    @Suppress("unused")
    fun addMapper(priority: Int, mapper: IOperatorMapper) {
        var list: ArrayList<IOperatorMapper>? = operatorList[priority]
        if (list == null) {
            list = ArrayList()
            operatorList[priority] = list
        }
        list.add(mapper)
    }

    fun setIterationLimit(iterationLimit: Int) {
        this.iterationLimit = iterationLimit
    }

    fun addSymbol(symbolList: ArrayList<ISymbol>) {
        this.symbolList.addAll(symbolList)
    }

    /**
     * preProcess - parses the string into a list of equations, evaluates them, and returns either a value or error.
     *
     * @param expression: a collection of symbols that create a valid expression
     */
    fun preProcess(expression: String) {
        val exp = expression.replace(" ", "")

        // this first part converts an expression string into many little expression strings, split by separators.
        var idx: Int
        stringList.clear()
        val list = LinkedList<Any>() // stores the strings to start, but eventually converts into functions, constants, etc, and then eventually converges (hopefully!) on a single value.
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

        //DP.println(DPType.CONSOLE, list.toString())

        // This second part detects constants and pulls them out. (Ex, values, variables, constants like PI)
        var depthMax = getDepthMax(list)
        var depth: Int
        // Double str
        run {
            idx = 0
            val i = list.iterator()
            while (i.hasNext()) {
                val o = i.next()
                if (o is String) {
                    var find = false
                    if (!find)
                        for (s in symbolList) {
                            if (s.getName() == o) {
                                list[idx] = s
                                find = true
                            }
                        }
                    if (!find)
                        try {
                            val value = o.toDouble()
                            list[idx] = Constant(value)
                            find = true
                        } catch (e: NumberFormatException) {}

                    if (!find) {
                        if (o.equals("pi", true)) list[idx] = Constant(Math.PI)
                    }
                }
                idx++
            }
        }

        // This final part goes through and tries to find the appropriate expressions and execute them
        var priority = -1

        var iterations = iterationLimit
        while (list.size > 1 && iterations != 0) {
            iterations--
            idx = 0
            depth = 0
            val i = list.iterator()
            priority++
            while (i.hasNext()) {
                val o = i.next()
                if (o is String) {
                    //DP.println(DPType.CONSOLE, "Considering operation $o")
                    if (operatorList.containsKey(priority)) {
                        val depthDelta = depth - depthMax
                        var resetPriority = false
                        for (mapper in operatorList[priority]!!) {
                            val operator: IOperator? = mapper.newOperator(o, depthDelta, list, idx)
                            if (operator != null) {
                                if (operator is IProcess)
                                    processList.add(operator as IProcess)
                                if (operator is INBTTReady)
                                    nbtList.add(operator as INBTTReady)
                                operatorCount += operator.getRedstoneCost()
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
                    if (o == "(")
                        depth++
                    if (o == ")")
                        depth--
                }
                idx++
            }
        }

        //DP.println(DPType.CONSOLE, list.toString())

        if (list.size == 1) {
            if (list[0] is IValue) {
                root = list[0] as IValue
            } else
                root = null
        }
    }

    private fun getDepthMax(list: LinkedList<Any>): Int {
        var depth = 0
        var depthMax = 0
        val i = list.iterator()
        while (i.hasNext()) {
            val o = i.next()
            if (o is String) {
                if (o == "(") depth++
                if (o == ")") depth--
                depthMax = Math.max(depthMax, depth)
            }
        }
        return depthMax
    }

    override fun getValue(): Double {
        return if (root == null) 0.0 else root!!.getValue()
    }

    fun getValue(deltaT: Double): Double {
        if (root == null)
            return 0.0
        for (p in processList) {
            p.process(deltaT)
        }
        return root!!.getValue()
    }


    fun isSymboleUsed(iSymbole: ISymbol): Boolean {
        return if (!isValid) false else stringList.contains(iSymbole.getName())
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        if (!isValid) return
        for ((idx, o) in nbtList.withIndex()) {
            o.readFromNBT(nbt, str + idx)
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        if (!isValid) return
        for ((idx, o) in nbtList.withIndex()) {
            o.writeToNBT(nbt, str + idx)
        }
    }

    companion object {

        internal val staticOperatorList: HashMap<Int, ArrayList<IOperatorMapper>> = HashMap()

        internal const val staticSeparatorList = "+-*&|/%^,()<>=!"

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
                list.add(OperatorMapperAB("%", Mod::class.java))
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

        @JvmStatic
        fun main(args: Array<String>) {
            val equation = Equation()
            equation.setIterationLimit(10)
            equation.setUpDefaultOperatorAndMapper()
            DP.println(DPType.CONSOLE, "Loaded operations: ${equation.operatorList}")
            while(true) {
                val expression = readLine()!!
                if (expression.equals("stop", true)) return
                equation.preProcess(expression)
                if (equation.isValid)
                    DP.println(DPType.CONSOLE, "Results: ${equation.getValue(1.0)}")
                else
                    DP.println(DPType.CONSOLE, "Failure to parse: $expression")
            }
        }
    }
}
