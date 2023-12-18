package mods.eln.solver

class OperatorMapperFunc(private val key: String, private val argCount: Int, private val operator: Class<*>) :
    IOperatorMapper {
    override fun newOperator(key: String, depthDelta: Int, arg: MutableList<Any>, argOffset: Int): IOperator? {
        if (depthDelta != -1) return null
        if (this.key != key) return null
        if (!isFuncReady(arg, argOffset)) return null

        val o: IOperator

        try {
            o = operator.newInstance() as IOperator

            val operatorArg = (0 until argCount).mapIndexed { index, i ->  arg[argOffset + 2 * (i + 1)] as IValue}.toTypedArray()
            o.setOperator(operatorArg)
            arg[argOffset] = o
            removeFunc(arg, argOffset)
            return o
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return null
    }

    private fun isFuncReady(list: List<Any?>, argOffset: Int): Boolean {
        var argOffset = argOffset
        var counter = 0

        argOffset++

        val end = argOffset + 2 + argCount * 2 - 1
        while (argOffset < end) {
            if (argOffset >= list.size) return false
            val o = list[argOffset]
            var str: String? = null
            if (o is String) str = o
            if (counter == 0) {
                if (str == null || str != "(") return false
            } else if (argOffset == end - 1) {
                if (str == null || str != ")") return false
            } else if ((counter % 2) == 1) {
                if (o !is IValue) return false
            } else {
                if (str == null || str != ",") return false
            }
            counter++
            argOffset++
        }
        return true
    }

    private fun removeFunc(list: MutableList<Any>, offset: Int) {
        for (idx in 0 until 2 + argCount * 2 - 1) {
            list.removeAt(offset + 1)
        }
    }
}
