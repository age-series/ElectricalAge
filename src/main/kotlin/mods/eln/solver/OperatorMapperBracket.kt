package mods.eln.solver

class OperatorMapperBracket : IOperatorMapper {
    override fun newOperator(key: String, depthDelta: Int, arg: MutableList<Any>, argOffset: Int): IOperator? {
        if (depthDelta != -1) return null
        if (key != "(") return null
        if (argOffset > arg.size - 3) return null
        if ((arg[argOffset + 1] is IValue
                    && arg[argOffset + 2] is String) && (arg[argOffset + 2] as String?) == ")"
        ) {
            val o: IOperator = Equation.Bracket()
            o.setOperator(arrayOf(arg[argOffset + 1] as IValue))
            arg[argOffset] = o
            arg.removeAt(argOffset + 1)
            arg.removeAt(argOffset + 1)
            return o
        }

        return null
    }
}
