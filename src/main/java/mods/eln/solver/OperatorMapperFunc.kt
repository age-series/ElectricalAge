package mods.eln.solver

class OperatorMapperFunc(private val key: String, private val argCount: Int, private val operator: Class<*>) : IOperatorMapper {

    override fun newOperator(key: String, depthDelta: Int, arg: MutableList<Any>, argOffset: Int): IOperator? {
        if (depthDelta != -1) return null
        if (this.key != key) return null
        if (!isFuncReady(arg, argOffset)) return null

        val o: IOperator

        try {
            o = operator.newInstance() as IOperator
            val operatorArg = arrayOfNulls<IValue>(argCount)
            for (i in 0 until argCount) {
                operatorArg[i] = arg[argOffset + 2 * (i + 1)] as IValue
            }
            val operatorArg2 = operatorArg.filterNotNull().toTypedArray()
            o.setOperator(operatorArg2)
            arg[argOffset] = o
            removeFunc(arg, argOffset)
            return o
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun isFuncReady(list: List<Any>, argOffset: Int): Boolean {
        var argOffset2 = argOffset
        var counter = 0

        argOffset2++

        val end = argOffset2 + 2 + argCount * 2 - 1
        while (argOffset2 < end) {
            if (argOffset2 >= list.size) return false
            val o = list[argOffset2]
            var str: String? = null
            if (o is String) str = o
            if (counter == 0) {
                if (str == null || str != "(") return false
            } else if (argOffset2 == end - 1) {
                if (str == null || str != ")") return false
            } else if (counter % 2 == 1) {
                if (o !is IValue) return false
            } else {
                if (str == null || str != ",") return false
            }
            counter++
            argOffset2++
        }
        return true
    }

    private fun removeFunc(list: MutableList<Any>, offset: Int) {
        for (idx in 0 until 2 + argCount * 2 - 1) {
            list.removeAt(offset + 1)
        }
    }

    override fun toString(): String {
        return "func[$key($argCount args)]"
    }
}
