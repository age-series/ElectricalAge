package mods.eln.solver

interface IOperator : IValue {
    fun setOperator(values: Array<IValue>)
    fun getRedstoneCost(): Int
}

interface ISymbol : IValue {
    fun getName(): String
}

interface IValue {
    fun getValue(): Double
}

interface IOperatorMapper {
    fun newOperator(key: String, depthDelta: Int, arg: MutableList<Any>, argOffset: Int): IOperator?
}
