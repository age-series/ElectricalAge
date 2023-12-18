package mods.eln.solver

class OperatorMapperAB(private val key: String, private val operator: Class<*>) : IOperatorMapper {
    override fun newOperator(key: String, depthDelta: Int, arg: MutableList<Any>, argOffset: Int): IOperator? {
        if (depthDelta != 0) return null
        if (this.key != key) return null
        if (argOffset - 1 < 0 || arg[argOffset - 1] !is IValue) return null
        if (argOffset + 1 > arg.size - 1 || arg[argOffset + 1] !is IValue) return null

        val o: IOperator

        try {
            o = operator.newInstance() as IOperator
            o.setOperator(arrayOf(arg[argOffset - 1] as IValue, arg[argOffset + 1] as IValue))
            arg[argOffset - 1] = o
            arg.removeAt(argOffset)
            arg.removeAt(argOffset)
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
}
