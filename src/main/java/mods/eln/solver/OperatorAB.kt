package mods.eln.solver

abstract class OperatorAB : IOperator {

    protected var a: IValue? = null
    protected var b: IValue? = null

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
        this.b = values[1]
    }
}
