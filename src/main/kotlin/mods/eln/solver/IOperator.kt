package mods.eln.solver

interface IOperator : IValue {
    fun setOperator(values: Array<IValue>)
    val redstoneCost: Int
}
