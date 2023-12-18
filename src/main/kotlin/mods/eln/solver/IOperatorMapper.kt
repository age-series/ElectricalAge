package mods.eln.solver

interface IOperatorMapper {
    fun newOperator(key: String, depthDelta: Int, arg: MutableList<Any>, argOffset: Int): IOperator?
}
