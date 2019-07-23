package mods.eln.solver

import mods.eln.Eln
import mods.eln.misc.FunctionTable
import mods.eln.misc.INBTTReady
import mods.eln.sim.core.IProcess
import net.minecraft.nbt.NBTTagCompound


class Eguals : OperatorAB() {
    override fun getValue(): Double {
        return if (a!!.getValue() > 0.5 == b!!.getValue() > 0.5) 1.0 else 0.0
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class NotEguals : OperatorAB() {
    override fun getValue(): Double {
        return if (a!!.getValue() > 0.5 != b!!.getValue() > 0.5) 1.0 else 0.0
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Bigger : OperatorAB() {
    override fun getValue(): Double {
        return if (a!!.getValue() > b!!.getValue()) 1.0 else 0.0
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Smaller : OperatorAB() {
    override fun getValue(): Double {
        return if (a!!.getValue() < b!!.getValue()) 1.0 else 0.0
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class And : OperatorAB() {
    override fun getValue(): Double {
        return if (a!!.getValue() > 0.5 && b!!.getValue() > 0.5) 1.0 else 0.0
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Or : OperatorAB() {
    override fun getValue(): Double {
        return if (a!!.getValue() > 0.5 || b!!.getValue() > 0.5) 1.0 else 0.0
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Add : OperatorAB() {
    override fun getValue(): Double {
        return a!!.getValue() + b!!.getValue()
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Sub : OperatorAB() {
    override fun getValue(): Double {
        return a!!.getValue() - b!!.getValue()
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Mul : OperatorAB() {
    override fun getValue(): Double {
        return a!!.getValue() * b!!.getValue()
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Div : OperatorAB() {
    override fun getValue(): Double {
        return a!!.getValue() / b!!.getValue()
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Mod : OperatorAB() {
    override fun getValue(): Double {
        return a!!.getValue() % b!!.getValue()
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}


class Inv : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return -a!!.getValue()
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Not : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return 1.0 - a!!.getValue()
    }

    override fun setOperator(values: Array<IValue>) {
        a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Bracket : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return a!!.getValue()
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 0
    }
}

class Abs : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return Math.abs(a!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 1
    }
}

class Sin : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return Math.sin(a!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }
}

class Cos : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return Math.cos(a!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }
}

class Asin : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return Math.asin(a!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }
}


class Acos : IOperator {
    internal var a: IValue? = null

    override fun getValue(): Double {
        return Math.acos(a!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }
}


class Pow : IOperator {
    internal var a: IValue? = null
    internal var b: IValue? = null

    override fun getValue(): Double {
        return Math.pow(a!!.getValue(), b!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[0]
        this.b = values[1]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }
}

class Ramp : IOperator, INBTTReady, IProcess {
    var counter = 0.0

    private var periode: IValue? = null

    override fun getValue(): Double {
        return counter
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        counter = nbt.getDouble(str + "counter")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {

        nbt.setDouble(str + "counter", counter)
    }

    override fun process(dt: Double) {
        val p = periode!!.getValue()
        counter += dt / p
        if (counter >= 1.0) counter -= 1.0
        if (counter >= 1.0) counter = 0.0
    }

    override fun setOperator(values: Array<IValue>) {
        this.periode = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 3
    }
}

class Integrator : IOperator, INBTTReady, IProcess {
    var counter = 0.0
    var probe: IValue? = null
    var reset: IValue? = null

    override fun getValue(): Double {
        return counter
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        counter = nbt.getDouble(str + "counter")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "counter", counter)
    }

    override fun process(dt: Double) {
        counter += dt * probe!!.getValue()
        if (reset!!.getValue() > 0.5) counter = 0.0
    }

    override fun setOperator(values: Array<IValue>) {
        this.probe = values[0]
        this.reset = values[1]
    }

    override fun getRedstoneCost(): Int {
        return 4
    }
}

class IntegratorMinMax : IOperator, INBTTReady, IProcess {
    var counter = 0.0

    var probe: IValue? = null
    var min: IValue? = null
    var max: IValue? = null

    override fun getValue(): Double {
        return counter
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        counter = nbt.getDouble(str + "counter")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "counter", counter)
    }

    override fun process(dt: Double) {
        counter += dt * probe!!.getValue()
        if (counter < min!!.getValue()) counter = min!!.getValue()
        if (counter > max!!.getValue()) counter = max!!.getValue()
    }

    override fun setOperator(values: Array<IValue>) {
        this.probe = values[0]
        this.min = values[1]
        this.max = values[2]
    }

    override fun getRedstoneCost(): Int {
        return 4
    }
}

class Derivator : IOperator, INBTTReady, IProcess {
    private var old = 0.0
    private var valu = 0.0
    var probe: IValue? = null

    override fun getValue(): Double {
        return valu
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        old = nbt.getDouble(str + "old")
        valu = nbt.getDouble(str + "value")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "old", old)
        nbt.setDouble(str + "value", valu)
    }

    override fun process(dt: Double) {
        val next = probe!!.getValue()
        valu = (next - old) / dt
        old = next
    }

    override fun setOperator(values: Array<IValue>) {
        this.probe = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 3
    }
}

open class Pid : IOperator, INBTTReady, IProcess {
    private var iStack = 0.0
    private var oldError = 0.0
    private var dValue = 0.0

    var target: IValue? = null
    private var hit: IValue? = null
    var p: IValue? = null
    var i: IValue? = null
    var d: IValue? = null

    override fun getValue(): Double {
        return oldError * p!!.getValue() + iStack + dValue * d!!.getValue()
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        iStack = nbt.getDouble(str + "iStack")
        oldError = nbt.getDouble(str + "oldError")
        dValue = nbt.getDouble(str + "dValue")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "iStack", iStack)
        nbt.setDouble(str + "oldError", oldError)
        nbt.setDouble(str + "dValue", dValue)
    }

    override fun process(dt: Double) {
        val error = target!!.getValue() - hit!!.getValue()
        iStack += error * dt * i!!.getValue()
        dValue = (error - oldError) / dt

        if (iStack > 1) iStack = 1.0
        if (iStack < -1) iStack = -1.0
        oldError = error
    }

    override fun setOperator(values: Array<IValue>) {
        this.target = values[0]
        this.hit = values[1]
        this.p = values[2]
        this.i = values[3]
        this.d = values[4]
    }

    override fun getRedstoneCost(): Int {
        return 12
    }
}

class PidMinMax : Pid() {
    var min: IValue? = null
    var max: IValue? = null

    override fun getValue(): Double {
        return Math.max(min!!.getValue(), Math.min(max!!.getValue(), super.getValue()))
    }

    override fun setOperator(values: Array<IValue>) {
        super.setOperator(values)
        min = values[5]
        max = values[6]
    }

    override fun getRedstoneCost(): Int {
        return super.getRedstoneCost() + 2
    }
}

class Min : IOperator {
    var a: IValue? = null
    var b: IValue? = null

    override fun getValue(): Double {
        return Math.min(a!!.getValue(), b!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[1]
        this.b = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }
}

class Max : IOperator {
    var a: IValue? = null
    var b: IValue? = null

    override fun getValue(): Double {
        return Math.max(a!!.getValue(), b!!.getValue())
    }

    override fun setOperator(values: Array<IValue>) {
        this.a = values[1]
        this.b = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }
}

class Rs : IOperator, INBTTReady {
    var state = false

    var set: IValue? = null
    var reset: IValue? = null

    override fun getValue(): Double {

        if (set!!.getValue() > 0.6) state = true
        if (reset!!.getValue() > 0.6) state = false
        return if (state) 1.0 else 0.0
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        state = nbt.getBoolean(str + "state")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setBoolean(str + "state", state)
    }

    override fun setOperator(values: Array<IValue>) {
        this.set = values[1]
        this.reset = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 3
    }
}

class RC : IOperator, INBTTReady, IProcess {
    var state: Double = 0.toDouble()

    private var tao: IValue? = null
    var input: IValue? = null

    override fun getValue(): Double {
        return state
    }

    override fun process(dt: Double) {
        val tao = Math.max(dt, this.tao!!.getValue())
        state += (input!!.getValue() - state) / tao * dt
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        state = nbt.getDouble(str + "state")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "state", state)
    }

    override fun setOperator(values: Array<IValue>) {
        this.input = values[1]
        this.tao = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 3
    }
}

class If : IOperator {
    var condition: IValue? = null
    private var thenValue: IValue? = null
    private var elseValue: IValue? = null

    override fun getValue(): Double {
        return if (condition!!.getValue() > 0.5) thenValue!!.getValue() else elseValue!!.getValue()
    }

    override fun setOperator(values: Array<IValue>) {
        this.condition = values[0]
        this.thenValue = values[1]
        this.elseValue = values[2]
    }

    override fun getRedstoneCost(): Int {
        return 2
    }

}

class BatteryCharge : IOperator {

    private var eMax: Double = 0.toDouble()
    var probe: IValue? = null

    init {
        val uFq = Eln.batteryVoltageFunctionTable
        var q = 0.0
        val dq = 0.01
        eMax = 0.0
        while (q <= 1.0) {
            eMax += uFq.getValue(q) * dq
            q += dq
        }
    }

    override fun setOperator(values: Array<IValue>) {
        this.probe = values[0]
    }

    override fun getRedstoneCost(): Int {
        return 8
    }

    override fun getValue(): Double {
        val uFq: FunctionTable = Eln.batteryVoltageFunctionTable
        val probeU = probe!!.getValue()
        if (probeU > 1.5) return 1.0
        var q = 0.0
        val dq = 0.01
        var e = 0.0
        var u = uFq.getValue(q)
        while (u < probeU) {
            e += u * dq
            q += dq
            u = uFq.getValue(q)
        }

        return e / eMax
    }
}

/**
 * Rescale input values.
 *
 *
 * scale(X, in0, in1, out0, out1) = (X - in0) / (in1 - in0) * (out1 - out0) + out0
 */
class Scale : IOperator {
    private var x: IValue? = null
    private var in0: IValue? = null
    private var in1: IValue? = null
    private var out0: IValue? = null
    private var out1: IValue? = null

    override fun setOperator(values: Array<IValue>) {
        x = values[0]
        in0 = values[1]
        in1 = values[2]
        out0 = values[3]
        out1 = values[4]
    }

    override fun getRedstoneCost(): Int {
        return 5
    }

    override fun getValue(): Double {
        val xv = x!!.getValue()
        val in0v = in0!!.getValue()
        val in1v = in1!!.getValue()
        val out0v = out0!!.getValue()
        val out1v = out1!!.getValue()

        return (xv - in0v) / (in1v - in0v) * (out1v - out0v) + out0v
    }
}
