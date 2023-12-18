package mods.eln.solver

abstract class OperatorAB : IOperator {
    lateinit var a: IValue
    lateinit var b: IValue

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
        this.b = values[1]
    }
}
